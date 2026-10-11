package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g032 节奏连击：圆圈不断收拢到目标环，收紧的一瞬点它；
 *  Perfect +15 / Good +8 / Miss 扣命，3 命尽结算。 */
public class BeatTapGame extends CanvasGame {
    private static final float RING_R = 86;

    private static class Beat {
        float x, y, r;
        float speed;
        boolean judged;

        Beat(float x, float y, float r, float speed) {
            this.x = x;
            this.y = y;
            this.r = r;
            this.speed = speed;
        }
    }

    private final List<Beat> beats = new ArrayList<>();
    private final Random rnd = new Random();
    private int score, lives = 3, combo;
    private long lastSpawn;
    private boolean dying;

    public BeatTapGame() {
        super(360, 560);
    }

    private void startRound() {
        beats.clear();
        score = 0;
        lives = 3;
        combo = 0;
        lastSpawn = 0;
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
        if (over || dying || action != 0) return;
        Beat best = null;
        float bestD = 1e9f;
        for (Beat b : beats) {
            if (b.judged) continue;
            float dx = x - b.x, dy = y - b.y;
            float d = (float) Math.sqrt(dx * dx + dy * dy);
            if (d < bestD && Math.abs(d - RING_R) < 120) {
                bestD = d;
                best = b;
            }
        }
        if (best == null) return;
        best.judged = true;
        float d = Math.abs(bestD - RING_R);
        if (d < 10) {
            combo++;
            int gain = 15 + Math.min(20, combo * 2);
            score += gain;
            popup("Perfect +" + gain, best.x, best.y - 40, 0xFFFFC24B, 16);
            burst(best.x, best.y, 0xFFFFC24B, 12, 5f);
            beep(1);
        } else if (d < 26) {
            combo++;
            score += 8;
            popup("Good +8", best.x, best.y - 40, 0xFF7CFFB2, 14);
            beep(1);
        } else {
            miss(best);
        }
        host.setScore(score);
        dirty();
    }

    private void miss(Beat b) {
        combo = 0;
        lives--;
        beep(2);
        popup("Miss", b.x, b.y - 40, 0xFFE8574F, 15);
        if (lives <= 0) {
            dying = true;
            over = true;
            stopLoop();
            shake(260);
            popup("断了连击", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 800);
        }
    }

    @Override
    protected void onTick() {
        long now = now();
        // 一次只有一个音符合中心环，收紧到判定线
        if (beats.isEmpty() && now - lastSpawn > 500) {
            lastSpawn = now;
            beats.add(new Beat(lw / 2f, lh / 2f, 190, Math.min(3.6f, 2.6f + score / 300f)));
        }
        Iterator<Beat> it = beats.iterator();
        while (it.hasNext()) {
            Beat b = it.next();
            b.r -= b.speed;
            if (!b.judged && b.r < RING_R - 34) {
                it.remove();
                miss(b);
                if (over) return;
                continue;
            }
            if (b.judged && b.r < 20) it.remove();
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF181420);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, combo > 1 ? "连击 ×" + combo : "跟准节奏", 40, 30, 14, 0xFFB9AED0, true);
        // 目标环
        stroke.setColor(0xFF7B7FF5);
        stroke.setStrokeWidth(3);
        c.drawCircle(lw / 2f, lh / 2f, RING_R, stroke);
        // 十字定位
        stroke.setColor(0x447B7FF5);
        stroke.setStrokeWidth(2);
        c.drawLine(lw / 2f - RING_R - 8, lh / 2f, lw / 2f + RING_R + 8, lh / 2f, stroke);
        c.drawLine(lw / 2f, lh / 2f - RING_R - 8, lw / 2f, lh / 2f + RING_R + 8, stroke);
        for (Beat b : beats) {
            if (b.judged) continue;
            float d = Math.abs(b.r - RING_R);
            stroke.setColor(d < 10 ? 0xFFFFC24B : (d < 26 ? 0xFF7CFFB2 : 0xFF8B93A7));
            stroke.setStrokeWidth(d < 10 ? 5 : 3);
            c.drawCircle(b.x, b.y, b.r, stroke);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
