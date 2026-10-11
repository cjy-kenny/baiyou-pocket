package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g029 快刀削削：水果抛起，手指划过切开（+分），漏接扣命，切到炸弹直接结束。 */
public class FruitSlashGame extends CanvasGame {
    private static class Fruit {
        float x, y, vx, vy, r;
        boolean bomb;
        boolean cut;

        Fruit(float x, float y, float vx, float vy, float r, boolean bomb) {
            this.x = x;
            this.y = y;
            this.vx = vx;
            this.vy = vy;
            this.r = r;
            this.bomb = bomb;
        }
    }

    private final List<Fruit> fruits = new ArrayList<>();
    private final Random rnd = new Random();
    private float lastX = -1, lastY = -1;
    private int score, lives = 3, missed;
    private long lastSpawn;
    private boolean dying;

    public FruitSlashGame() {
        super(360, 560);
    }

    private void startRound() {
        fruits.clear();
        score = 0;
        lives = 3;
        missed = 0;
        lastSpawn = 0;
        lastX = lastY = -1;
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

    private void toss() {
        int n = 1 + (score > 150 ? 1 : 0);
        for (int i = 0; i < n; i++) {
            float x = rand(60, lw - 60);
            boolean bomb = rnd.nextInt(100) < 18;
            fruits.add(new Fruit(x, lh + 20, rand(-2.2f, 2.2f), rand(-15.5f, -13f),
                    20, bomb));
        }
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || dying) return;
        if (action == 0) {
            lastX = x;
            lastY = y;
            return;
        }
        if (action != 2) {
            if (action == 1) lastX = lastY = -1;
            return;
        }
        if (lastX < 0) {
            lastX = x;
            lastY = y;
            return;
        }
        // 线段切割检测
        for (Fruit f : fruits) {
            if (f.cut) continue;
            if (segHitsCircle(lastX, lastY, x, y, f.x, f.y, f.r)) {
                f.cut = true;
                if (f.bomb) {
                    die();
                    return;
                }
                score += 10;
                host.setScore(score);
                beep(1);
                burst(f.x, f.y, 0xFFE0973F, 10, 5f);
                popup("+10", f.x, f.y - 18, 0xFFE0973F, 14);
            }
        }
        lastX = x;
        lastY = y;
        dirty();
    }

    private boolean segHitsCircle(float x1, float y1, float x2, float y2, float cx, float cy, float r) {
        float dx = x2 - x1, dy = y2 - y1;
        float len2 = dx * dx + dy * dy;
        float t = len2 == 0 ? 0 : ((cx - x1) * dx + (cy - y1) * dy) / len2;
        t = Math.max(0f, Math.min(1f, t));
        float px = x1 + dx * t, py = y1 + dy * t;
        float ddx = px - cx, ddy = py - cy;
        return ddx * ddx + ddy * ddy <= r * r;
    }

    private void die() {
        dying = true;
        over = true;
        stopLoop();
        shake(360);
        burst(lastX, lastY, 0xFFE8574F, 24, 8f);
        beep(2);
        popup("砍到炸弹了！", lw / 2f, lh / 2f, 0xFFE8574F, 18);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 800);
    }

    @Override
    protected void onTick() {
        long now = now();
        if (now - lastSpawn > (fruits.size() > 2 ? 1300 : 800)) {
            lastSpawn = now;
            toss();
        }
        Iterator<Fruit> it = fruits.iterator();
        while (it.hasNext()) {
            Fruit f = it.next();
            f.vy += 0.28f;
            f.x += f.vx;
            f.y += f.vy;
            if (f.y > lh + 40) {
                it.remove();
                if (!f.bomb && !f.cut) {
                    lives--;
                    missed++;
                    beep(2);
                    if (lives <= 0) {
                        over = true;
                        dying = true;
                        stopLoop();
                        popup("漏了 " + missed + " 个", lw / 2f, lh / 2f, 0xFFECECF1, 17);
                        dirty();
                        postDelayedSafe(() -> host.gameOver(score), 800);
                        return;
                    }
                }
            }
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF20261E);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, "切开水果 · 别碰炸弹", 50, 30, 13, 0xFF9ACDAA, false);
        for (Fruit f : fruits) {
            if (f.cut && !f.bomb) continue;
            fill.setColor(f.bomb ? 0xFF3A3F4C : 0xFFE8574F);
            c.drawCircle(f.x, f.y, f.r, fill);
            if (f.bomb) {
                label(c, "✸", f.x, f.y, f.r * 1.6f, 0xFFFFC24B, true);
            } else {
                fill.setColor(0xFFE0973F);
                c.drawCircle(f.x - f.r * 0.25f, f.y - f.r * 0.25f, f.r * 0.35f, fill);
                stroke.setColor(0x88000000);
                stroke.setStrokeWidth(2);
                c.drawLine(f.x - f.r * 0.7f, f.y - f.r * 0.2f, f.x + f.r * 0.7f, f.y + f.r * 0.2f, stroke);
            }
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
