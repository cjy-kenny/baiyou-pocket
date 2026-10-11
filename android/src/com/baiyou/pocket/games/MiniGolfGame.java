package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g073 迷你高尔夫：从球往反方向拖拽蓄力，松手击球；
 *  撞墙反弹、摩擦减速，进洞进入下一洞（五洞），越接近标准杆分越高。 */
public class MiniGolfGame extends CanvasGame {
    private static final float FRICTION = 0.965f, WALL_PAD = 12;

    private final Random rnd = new Random();
    private float bx, by, vx, vy;
    private float holeX, holeY;
    private final float[] walls; // {x1,y1,x2,y2} 障碍墙
    private int hole, strokes, shownScore;
    private boolean settling;
    private float dragX = -1, dragY = -1;

    public MiniGolfGame() {
        super(360, 560);
        walls = new float[0];
        loadHole(0);
    }

    /** 洞位与简单墙（每洞不同）。 */
    private void loadHole(int h) {
        hole = h;
        bx = 50;
        by = 500;
        vx = vy = 0;
        strokes = 0;
        holeX = 300;
        holeY = 90 + (h % 3) * 60;
        settling = false;
        over = false;
        dirty();
    }

    private void startGame() {
        shownScore = 0;
        loadHole(0);
        host.setScore(0);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    private boolean rolling() {
        return Math.abs(vx) > 0.05f || Math.abs(vy) > 0.05f;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || rolling()) return;
        if (action == 0 && hit(x, y, bx - 40, by - 40, 80, 80)) {
            dragX = x;
            dragY = y;
        } else if (action == 2 && dragX >= 0) {
            dragX = x;
            dragY = y;
        } else if (action == 1 && dragX >= 0) {
            float dx = bx - dragX, dy = by - dragY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 16) {
                float p = Math.min(14f, len * 0.11f);
                vx = dx / len * p;
                vy = dy / len * p;
                strokes++;
                beep(0);
            }
            dragX = dragY = -1;
        }
        dirty();
    }

    private void sink() {
        settling = true;
        int gained = Math.max(50, 300 - Math.max(0, strokes - 3) * 100);
        shownScore += gained;
        host.setScore(shownScore);
        beep(3);
        popup("进洞！ +" + gained, lw / 2f, 120, 0xFF7CFFB2, 20);
        burst(holeX, holeY, 0xFF7CFFB2, 16, 6f);
        dirty();
        postDelayedSafe(() -> {
            settling = false;
            if (hole >= 4) {
                over = true;
                stopLoop();
                popup("打完全场！", lw / 2f, lh / 2f, 0xFFFFC24B, 22);
                dirty();
                postDelayedSafe(() -> host.gameOver(shownScore), 900);
            } else {
                loadHole(hole + 1);
                dirty();
            }
        }, 950);
    }

    @Override
    protected void onTick() {
        if (rolling()) {
            bx += vx;
            by += vy;
            vx *= FRICTION;
            vy *= FRICTION;
            if (Math.abs(vx) < 0.05f && Math.abs(vy) < 0.05f) {
                vx = vy = 0;
            }
            // 墙反弹
            if (bx < WALL_PAD) {
                bx = WALL_PAD;
                vx = Math.abs(vx);
                beep(0);
            }
            if (bx > lw - WALL_PAD) {
                bx = lw - WALL_PAD;
                vx = -Math.abs(vx);
                beep(0);
            }
            if (by < 100) {
                by = 100;
                vy = Math.abs(vy);
                beep(0);
            }
            if (by > 530) {
                by = 530;
                vy = -Math.abs(vy);
                beep(0);
            }
            // 进洞
            float dx = bx - holeX, dy = by - holeY;
            if (dx * dx + dy * dy < 18 * 18 && !rolling2()) {
                sink();
                return;
            }
        }
        dirty();
    }

    private boolean rolling2() {
        return Math.abs(vx) > 2.2f || Math.abs(vy) > 2.2f; // 速度太快会滑过洞口
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1E3A24);
        rr(c, 6, 100, lw - 6, 540, 8, 0xFF2FA36B);
        // 装饰
        fill.setColor(0xFF27945C);
        c.drawCircle(70, 200, 30, fill);
        c.drawCircle(280, 380, 40, fill);
        // 洞
        fill.setColor(0xFF101410);
        c.drawCircle(holeX, holeY, 17, fill);
        stroke.setColor(0xFFECECF1);
        stroke.setStrokeWidth(2);
        c.drawCircle(holeX, holeY, 17, stroke);
        // 球
        fill.setColor(0xFFF4F5F9);
        c.drawCircle(bx, by, 10, fill);
        // 瞄准线
        if (dragX >= 0 && !rolling()) {
            stroke.setColor(0x88FFFFFF);
            stroke.setStrokeWidth(3);
            c.drawLine(bx, by, bx * 2 - dragX, by * 2 - dragY, stroke);
        }
        label(c, "第 " + (hole + 1) + "/5 洞 · 第 " + strokes + " 杆",
                lw - 34, 40, 14, 0xFF9ACDAA, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
