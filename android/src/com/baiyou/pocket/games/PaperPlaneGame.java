package com.baiyou.pocket.games;

import android.graphics.Canvas;
import android.graphics.Path;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g069 纸飞机：按住上扬、松手俯冲，穿圈得分，撞地/撞顶即结束。 */
public class PaperPlaneGame extends CanvasGame {
    private static class Ring {
        float x, y, r;
        boolean passed;

        Ring(float x, float y) {
            this.x = x;
            this.y = y;
            this.r = 42;
        }
    }

    private final List<Ring> rings = new ArrayList<>();
    private final Random rnd = new Random();
    private float py, vy;
    private int score;
    private float scrollX;
    private boolean dying;

    public PaperPlaneGame() {
        super(360, 560);
    }

    private void startRound() {
        rings.clear();
        py = 280;
        vy = 0;
        score = 0;
        scrollX = 0;
        float lastX = 0;
        for (int i = 0; i < 6; i++) {
            lastX += rand(200, 280);
            rings.add(new Ring(lastX, rand(140, 460)));
        }
        dying = false;
        over = false;
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

    @Override
    protected void touch(float x, float y, int action) {
        if (over || dying) return;
        vy = action == 0 || action == 2 ? -5.4f : vy;
        if (action == 1) vy = Math.max(vy, 2f);
        dirty();
    }

    @Override
    protected void onTick() {
        vy += 0.24f;
        vy = Math.max(-7f, Math.min(8f, vy));
        py += vy;
        scrollX += 6f;
        // 出圈/进圈判定
        Iterator<Ring> it = rings.iterator();
        while (it.hasNext()) {
            Ring ring = it.next();
            float sx = ring.x - scrollX;
            if (sx < -60) {
                it.remove();
                continue;
            }
            if (!ring.passed && Math.abs(sx - 90) < 8) {
                ring.passed = true;
                if (Math.abs(py - ring.y) < ring.r) {
                    score += 10;
                    host.setScore(score);
                    beep(1);
                    burst(sx, ring.y, 0xFF7CFFB2, 12, 5f);
                    popup("+10", sx, ring.y - 30, 0xFF7CFFB2, 16);
                } else {
                    beep(2);
                }
            }
        }
        // 补圈
        float lastX = 0;
        for (Ring ring : rings) lastX = Math.max(lastX, ring.x);
        while (lastX < scrollX + lw + 200) {
            lastX += rand(200, 280);
            rings.add(new Ring(lastX, rand(140, 460)));
        }
        // 撞地/顶
        if (py < 30 || py > 540) {
            dying = true;
            over = true;
            stopLoop();
            shake(280);
            burst(90, py, 0xFFE8574F, 16, 6f);
            beep(2);
            popup("撞上了", lw / 2f, lh / 2f, 0xFFE8574F, 18);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF18202E);
        for (Ring ring : rings) {
            float sx = ring.x - scrollX;
            if (sx < -60 || sx > lw + 60) continue;
            stroke.setColor(ring.passed ? 0xFF2E5244 : 0xFFFFC24B);
            stroke.setStrokeWidth(6);
            c.drawCircle(sx, ring.y, ring.r, stroke);
        }
        // 纸飞机
        fill.setColor(0xFFF4F5F9);
        Path p = new Path();
        p.moveTo(110, py);
        p.lineTo(74, py + 10);
        p.lineTo(104, py + 16);
        p.close();
        fill.setColor(0xFFF4F5F9);
        c.drawPath(p, fill);
        stroke.setColor(0xFF9AA0AC);
        stroke.setStrokeWidth(1.5f);
        c.drawLine(110, py, 74, py + 10, stroke);
        label(c, String.valueOf(score), lw - 44, 34, 18, 0xFF9AA0AC, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
