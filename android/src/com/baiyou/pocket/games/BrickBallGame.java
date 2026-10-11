package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g004 碎块弹球：拖动挡板弹球打砖。
 *  5 行 6 列砖块（越靠上分越高），3 条命；清空全部砖块进入下一关（球速 +8%，+100 分）；
 *  每次挡板命中球速微升（上限 1.35 倍）；砖块碎裂有碎屑与浮分，球带拖尾。 */
public class BrickBallGame extends CanvasGame {
    private static final int COLS = 6, ROWS = 5;
    private static final float PAD = 16, GAP = 6, BH = 20, TOP = 64;
    private static final float BW = (400 - PAD * 2 - GAP * (COLS - 1)) / COLS;
    private static final float PW = 80, PH = 13, PY = 600, R = 7;
    private static final float PLAY_TOP = 48;
    private static final float SPEED_CAP_K = 1.35f;
    private static final int[] ROW_COLOR = {
            0xFFE8574F, 0xFFF2A33C, 0xFFE3D24B, 0xFF2FA36B, 0xFF5B8DEF};

    private final boolean[][] alive = new boolean[ROWS][COLS];
    private final List<float[]> trail = new ArrayList<>();   // {x, y, born}
    private int trailTicks;
    private float px, bx, by, vx, vy;
    private int lives, level, score, remaining;
    private boolean stuck;
    private float speedBase = 5.0f;
    private long paddleFlashUntil;

    public BrickBallGame() {
        super(400, 640);
        px = lw / 2f;
    }

    private void buildBricks() {
        remaining = 0;
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) {
                alive[r][c] = true;
                remaining++;
            }
    }

    private void resetBall() {
        stuck = true;
        bx = px;
        by = PY - R - 3;
        trail.clear();
    }

    private void startRound() {
        level = 1;
        lives = 3;
        score = 0;
        speedBase = 5.0f;
        px = lw / 2f;
        over = false;
        buildBricks();
        resetBall();
        host.setScore(0);
        startLoop(16);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(16);
    }

    private float curSpeed() {
        return (float) Math.sqrt(vx * vx + vy * vy);
    }

    // ---------------- 交互 ----------------

    @Override
    protected void touch(float x, float y, int action) {
        if (over) return;
        if (action == 0 && stuck) {
            stuck = false;
            float ang = (rnd.nextFloat() * 50 - 25) / 180f * (float) Math.PI;
            vx = speedBase * (float) Math.sin(ang);
            vy = -speedBase * (float) Math.cos(ang);
            beep(0);
        }
        if (action == 0 || action == 2) {
            px = Math.max(PAD + PW / 2, Math.min(lw - PAD - PW / 2, x));
        }
        dirty();
    }

    // ---------------- 逻辑帧：物理步进 ----------------

    @Override
    protected void onTick() {
        setNextTick(16);
        long now = System.currentTimeMillis();
        // 拖尾采样（每 3 帧记一点，180ms 淡出，硬上限 10 点保证尾巴紧凑）
        trailTicks++;
        if (!stuck && trailTicks % 3 == 0) trail.add(new float[]{bx, by, now});
        if (trail.size() > 10) trail.remove(0);
        Iterator<float[]> ti = trail.iterator();
        while (ti.hasNext()) if (now - ti.next()[2] > 180) ti.remove();

        if (stuck) {
            bx = px;
            by = PY - R - 3;
            dirty();
            return;
        }

        bx += vx;
        by += vy;

        // 墙与顶
        if (bx < PAD + R) {
            bx = PAD + R;
            vx = Math.abs(vx);
            beep(0);
        }
        if (bx > lw - PAD - R) {
            bx = lw - PAD - R;
            vx = -Math.abs(vx);
            beep(0);
        }
        if (by < PLAY_TOP + R) {
            by = PLAY_TOP + R;
            vy = Math.abs(vy);
        }

        // 挡板：反弹角随命中点变化，且每次命中球速微升
        if (vy > 0 && by + R >= PY && by + R <= PY + PH + 10 && Math.abs(bx - px) <= PW / 2 + R) {
            float rel = Math.max(-1f, Math.min(1f, (bx - px) / (PW / 2)));
            float ang = rel * 60f / 180f * (float) Math.PI;
            float sp = Math.min(speedBase * SPEED_CAP_K, curSpeed() * 1.012f);
            vx = sp * (float) Math.sin(ang);
            vy = -sp * (float) Math.cos(ang);
            by = PY - R - 1;
            paddleFlashUntil = now + 130;
            burst(bx, PY, 0xFFF4F5F9, 4, 2.5f);
            beep(0);
        }

        // 砖块
        for (int r = 0; r < ROWS; r++) {
            for (int col = 0; col < COLS; col++) {
                if (!alive[r][col]) continue;
                float bl = PAD + col * (BW + GAP);
                float bt = TOP + r * (BH + GAP);
                float cxr = Math.max(bl, Math.min(bx, bl + BW));
                float cyr = Math.max(bt, Math.min(by, bt + BH));
                float dx = bx - cxr, dy = by - cyr;
                if (dx * dx + dy * dy <= R * R) {
                    alive[r][col] = false;
                    remaining--;
                    int gain = 10 + (ROWS - 1 - r) * 5;
                    score += gain;
                    host.setScore(score);
                    float bcx = bl + BW / 2f, bcy = bt + BH / 2f;
                    burst(bcx, bcy, ROW_COLOR[r], 12, 5f);
                    popup("+" + gain, bcx, bcy, ROW_COLOR[r], 16);
                    beep(1);
                    // 反弹轴：取穿透更浅的轴翻转
                    if (Math.abs(dx) > Math.abs(dy)) vx = -vx;
                    else vy = -vy;
                    // 防水平死循环
                    if (Math.abs(vy) < 1.5f) vy = vy < 0 ? -1.5f : 1.5f;
                    if (remaining == 0) {
                        level++;
                        score += 100;
                        speedBase *= 1.08f;
                        host.setScore(score);
                        host.toast("进入第 " + level + " 关，球速提升！");
                        popup("第 " + level + " 关！", lw / 2f, lh / 2f, 0xFFFFC24B, 26);
                        burst(lw / 2f, TOP + ROWS * 13, 0xFFFFC24B, 26, 8f);
                        buildBricks();
                        resetBall();
                        beep(3);
                    }
                    dirty();
                    return;
                }
            }
        }

        // 掉落
        if (by - R > lh) {
            lives--;
            shake(280);
            burst(bx, lh - 10, 0xFFE8574F, 14, 6f);
            beep(2);
            if (lives <= 0) {
                over = true;
                stopLoop();
                popup("球用完了", lw / 2f, lh / 2f, 0xFFE8574F, 22);
                dirty();
                postDelayedSafe(() -> host.gameOver(score), 800);
                return;
            }
            resetBall();
        }
        dirty();
    }

    // ---------------- 渲染 ----------------

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF171A21);
        label(c, "第 " + level + " 关", 60, 24, 14, 0xFF9AA0AC, true);
        for (int i = 0; i < 3; i++) {
            fill.setColor(i < lives ? 0xFFFF6B6B : 0xFF3A3F4C);
            c.drawCircle(lw - 60 + i * 18, 24, 5.5f, fill);
        }

        for (int r = 0; r < ROWS; r++)
            for (int col = 0; col < COLS; col++) {
                if (!alive[r][col]) continue;
                float bl = PAD + col * (BW + GAP);
                float bt = TOP + r * (BH + GAP);
                rr(c, bl, bt, bl + BW, bt + BH, 4, ROW_COLOR[r]);
            }

        // 球拖尾
        long now = System.currentTimeMillis();
        for (float[] t : trail) {
            float k = 1 - (now - t[2]) / 180f;
            int alpha = (int) (90 * k);
            fill.setColor((alpha << 24) | 0x00FFFFFF);
            c.drawCircle(t[0], t[1], R * (0.4f + 0.6f * k), fill);
        }

        // 挡板（命中瞬间提亮）
        boolean flash = now < paddleFlashUntil;
        rr(c, px - PW / 2, PY, px + PW / 2, PY + PH, 6, flash ? 0xFFFFFFFF : 0xFFF4F5F9);
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(bx, by, R, fill);

        if (stuck && !over) {
            label(c, "点按发射 · 拖动挡板", lw / 2f, PY + 34, 13, 0xFF8B8E9C, false);
        }
    }
}
