package com.baiyou.pocket.games;

import android.graphics.Canvas;
import android.graphics.Path;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g062 切绳送糖：点绳索剪断，让糖果掉进下面的口袋；
 *  五个关卡逐级多绳，剪错落空本关重开。 */
public class CutRopeGame extends CanvasGame {
    private static final float GROUND = 470;

    // 关卡：扁平 (锚x, 锚y)*绳数 + 末尾两位糖果坐标
    private static final float[][] LEVELS = {
            {180, 60, 180, 420},                                  // 1 绳
            {100, 60, 260, 60, 180, 420},                          // 2 绳
            {80, 50, 180, 50, 280, 50, 120, 420},                  // 3 绳
            {70, 60, 180, 40, 290, 60, 240, 420},                  // 3 绳
            {60, 50, 130, 40, 230, 40, 300, 50, 60, 420},          // 4 绳
    };

    private final Random rnd = new Random();
    private int level;
    private boolean[] cut;          // 每根绳是否已剪
    private float candyX, candyY, vx, vy;
    private boolean falling;
    private int score;
    private boolean settling;

    public CutRopeGame() {
        super(360, 560);
        loadLevel(0);
    }

    private int ropeCount() {
        return LEVELS[level].length / 2 - 1;
    }

    private float anchorX(int i) {
        return LEVELS[level][i * 2];
    }

    private float anchorY(int i) {
        return LEVELS[level][i * 2 + 1];
    }

    private void loadLevel(int lv) {
        level = lv;
        cut = new boolean[ropeCount()];
        candyX = LEVELS[lv][LEVELS[lv].length - 2];
        candyY = LEVELS[lv][LEVELS[lv].length - 1];
        vx = vy = 0;
        falling = false;
        settling = false;
        over = false;
        dirty();
    }

    private void startGame() {
        score = 0;
        loadLevel(0);
        host.setScore(0);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    private float bucketX() {
        return LEVELS[level][LEVELS[level].length - 2];
    }

    private boolean allCut() {
        for (boolean b : cut) if (!b) return false;
        return true;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || falling || action != 0) return;
        for (int i = 0; i < ropeCount(); i++) {
            if (cut[i]) continue;
            // 点到绳中段（糖果上方）
            float ax = anchorX(i), ay = anchorY(i);
            if (Math.abs(x - ax) < 26 && y > ay && y < candyY - 10) {
                cut[i] = true;
                beep(1);
                burst(ax, (ay + candyY) / 2f, 0xFFC9B896, 8, 4f);
                if (allCut()) {
                    falling = true;
                    vx = 0;
                    vy = 0;
                }
                dirty();
                return;
            }
        }
    }

    @Override
    protected void onTick() {
        if (falling) {
            vy += 0.3f;
            candyX += vx;
            candyY += vy;
            if (candyY >= GROUND - 26) {
                settling = true;
                if (Math.abs(candyX - bucketX()) < 44) {
                    score += 100 * (level + 1);
                    host.setScore(score);
                    beep(3);
                    popup("进袋！ + " + 100 * (level + 1), lw / 2f, 200, 0xFF7CFFB2, 20);
                    burst(candyX, GROUND - 30, 0xFF7CFFB2, 16, 6f);
                    postDelayedSafe(() -> {
                        settling = false;
                        if (level >= LEVELS.length - 1) {
                            over = true;
                            stopLoop();
                            popup("糖果大师！", lw / 2f, lh / 2f, 0xFFFFC24B, 22);
                            dirty();
                            postDelayedSafe(() -> host.gameOver(score), 900);
                        } else {
                            loadLevel(level + 1);
                            dirty();
                        }
                    }, 900);
                } else {
                    beep(2);
                    popup("落空了…本关重来", lw / 2f, 200, 0xFFE8574F, 17);
                    postDelayedSafe(() -> {
                        settling = false;
                        loadLevel(level);
                        dirty();
                    }, 900);
                }
                dirty();
                return;
            }
        } else {
            // 挂着的糖果轻微摆动
            candyX += (float) Math.sin(now() / 500.0 + level) * 0.3f;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF20261E);
        rr(c, 0, GROUND, lw, lh - GROUND, 0, 0xFF2E3A28);
        label(c, "第 " + (level + 1) + "/" + LEVELS.length + " 关", lw - 44, 30, 14, 0xFF9ACDAA, true);
        // 绳
        for (int i = 0; i < ropeCount(); i++) {
            if (cut[i]) continue;
            stroke.setColor(0xFFC9B896);
            stroke.setStrokeWidth(3);
            c.drawLine(anchorX(i), anchorY(i), candyX, candyY - 10, stroke);
            fill.setColor(0xFF8A9A4B);
            c.drawCircle(anchorX(i), anchorY(i), 6, fill);
        }
        // 糖果
        fill.setColor(0xFFE85FA8);
        c.drawCircle(candyX, candyY, 15, fill);
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(candyX - 5, candyY - 5, 4, fill);
        // 口袋
        float bx = bucketX();
        fill.setColor(0xFF8A5A2B);
        Path p = new Path();
        p.moveTo(bx - 40, GROUND - 40);
        p.lineTo(bx + 40, GROUND - 40);
        p.lineTo(bx + 28, GROUND);
        p.lineTo(bx - 28, GROUND);
        p.close();
        fill.setColor(0xFF8A5A2B);
        c.drawPath(p, fill);
        stroke.setColor(0xFF5A3A1B);
        stroke.setStrokeWidth(3);
        c.drawLine(bx - 40, GROUND - 40, bx + 40, GROUND - 40, stroke);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
