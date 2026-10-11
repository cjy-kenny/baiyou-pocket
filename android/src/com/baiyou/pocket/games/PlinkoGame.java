package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g064 弹珠台：点屏落珠，撞钉弹跳，落进不同分值的槽里；
 *  五颗珠，总分定输赢。 */
public class PlinkoGame extends CanvasGame {
    private static final int ROWS = 6, PEG_GAP = 52;
    private static final float TOP = 110;

    private static class Ball {
        float x, y, vx, vy;

        Ball(float x, float y) {
            this.x = x;
            this.y = y;
        }
    }

    private final Random rnd = new Random();
    private final List<Ball> balls = new ArrayList<>();
    private int ballsLeft = 5, score;
    private final int[] bucketScore = {5, 10, 50, 10, 5, 20, 5};
    private boolean settling;

    public PlinkoGame() {
        super(360, 560);
    }

    private void startRound() {
        balls.clear();
        ballsLeft = 5;
        score = 0;
        settling = false;
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

    private float pegX(int r, int c) {
        return 40 + c * PEG_GAP + (r % 2 == 1 ? PEG_GAP / 2f : 0);
    }

    private float pegY(int r) {
        return TOP + r * 52;
    }

    private int pegCols(int r) {
        return r % 2 == 1 ? 6 : 7;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0 || ballsLeft <= 0) return;
        if (y > 90) return;
        balls.add(new Ball(Math.max(24, Math.min(lw - 24, x)), 70));
        ballsLeft--;
        beep(0);
        dirty();
    }

    @Override
    protected void onTick() {
        Iterator<Ball> it = balls.iterator();
        while (it.hasNext()) {
            Ball b = it.next();
            b.vy += 0.22f;
            b.x += b.vx;
            b.y += b.vy;
            // 撞钉
            for (int r = 0; r < ROWS; r++)
                for (int cI = 0; cI < pegCols(r); cI++) {
                    float pegxx = pegX(r, cI), pegyy = pegY(r);
                    float dx = b.x - pegxx, dy = b.y - pegyy;
                    if (dx * dx + dy * dy < 12 * 12 && b.y < pegyy) {
                        float len = (float) Math.sqrt(dx * dx + dy * dy);
                        if (len < 0.01f) len = 1;
                        b.vx = dx / len * Math.abs(b.vx + 1.2f) + rand(-0.6f, 0.6f);
                        b.vy = Math.abs(b.vy) * 0.4f;
                        b.y = pegyy - 12;
                        beep(0);
                    }
                }
            // 壁
            if (b.x < 14) {
                b.x = 14;
                b.vx = Math.abs(b.vx);
            }
            if (b.x > lw - 14) {
                b.x = lw - 14;
                b.vx = -Math.abs(b.vx);
            }
            // 入槽
            if (b.y > 480) {
                int slot = Math.max(0, Math.min(6, (int) (b.x / (lw / 7f))));
                score += bucketScore[slot];
                host.setScore(score);
                beep(1);
                popup("+" + bucketScore[slot], 20 + slot * (lw / 7f) + lw / 14f, 470,
                        0xFFFFC24B, 16);
                burst(b.x, 470, 0xFFFFC24B, 10, 4f);
                it.remove();
            }
        }
        if (ballsLeft == 0 && balls.isEmpty() && !over) {
            settling = true;
            over = true;
            stopLoop();
            popup("总分 " + score, lw / 2f, lh / 2f, 0xFFECECF1, 20);
            beep(3);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 900);
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1A1E2E);
        label(c, "剩余弹珠 " + ballsLeft, 40, 30, 14, 0xFF9AA0AC, true);
        for (int r = 0; r < ROWS; r++)
            for (int cI = 0; cI < pegCols(r); cI++) {
                fill.setColor(0xFF8B93A7);
                c.drawCircle(pegX(r, cI), pegY(r), 5, fill);
            }
        // 槽
        for (int s = 0; s < 7; s++) {
            float l = s * (lw / 7f);
            rr(c, l + 2, 480, l + lw / 7f - 2, 545, 4,
                    bucketScore[s] >= 50 ? 0xFF7B7FF5 : 0xFF2A2D37);
            label(c, String.valueOf(bucketScore[s]), l + lw / 14f, 512, 15, 0xFFECECF1, true);
        }
        for (Ball b : balls) {
            fill.setColor(0xFFFFC24B);
            c.drawCircle(b.x, b.y, 9, fill);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
