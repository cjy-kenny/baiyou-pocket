package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g025 色觉快答：一个颜色名字用另一种颜色写出，判断"字义"与"字色"是否一致。
 *  30 秒内答对越多越好，答错扣命。 */
public class ColorStroopGame extends CanvasGame {
    private static final long ROUND_MS = 30000;
    private static final String[] NAMES = {"红", "橙", "黄", "绿", "蓝", "紫"};
    private static final int[] COLORS = {
            0xFFE8574F, 0xFFF2913C, 0xFFE3D24B, 0xFF2FA36B, 0xFF4A90E2, 0xFF9B59F6};

    private final Random rnd = new Random();
    private String word;
    private int inkColor;   // 字的颜色
    private boolean consistent;
    private int lives = 3, shownScore;
    private long roundEnd;
    private long flashUntil;
    private boolean flashOk;

    public ColorStroopGame() {
        super(360, 560);
        nextQ();
    }

    private void nextQ() {
        int w = rnd.nextInt(NAMES.length);
        word = NAMES[w];
        boolean same = rnd.nextInt(100) < 40;
        int ink;
        if (same) {
            ink = COLORS[w];
        } else {
            do {
                ink = rnd.nextInt(NAMES.length);
            } while (ink == w);
            ink = COLORS[ink];
        }
        inkColor = ink;
        consistent = same;
    }

    private void startRound() {
        lives = 3;
        shownScore = 0;
        roundEnd = now() + ROUND_MS;
        nextQ();
        flashUntil = 0;
        over = false;
        host.setScore(0);
        startLoop(60);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(60);
    }

    private void answer(boolean yes) {
        boolean ok = yes == consistent;
        if (ok) {
            shownScore += 12;
            host.setScore(shownScore);
            beep(1);
            burst(180, 240, 0xFF7CFFB2, 10, 5f);
        } else {
            lives--;
            beep(2);
            shake(200);
        }
        flashOk = ok;
        flashUntil = now() + 260;
        if (lives <= 0) {
            over = true;
            stopLoop();
            popup("分还行 " + shownScore, lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        postDelayedSafe(() -> {
            if (over) return;
            nextQ();
            dirty();
        }, 240);
        dirty();
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || action != 0 || now() < flashUntil) return;
        if (hit(x, y, 26, 380, 140, 84)) answer(true);
        else if (hit(x, y, 194, 380, 140, 84)) answer(false);
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        long remain = roundEnd - now();
        rr(c, 0, 0, lw, lh, 0, flashUntil > 0 ? (flashOk ? 0xFF1E3328 : 0xFF331B20) : 0xFF20242E);
        float k = Math.max(0f, remain / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * k, 60, 4, 0xFF7B7FF5);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, "字的意思和颜色一样吗？", lw / 2f, 150, 16, 0xFF8B8E9C, false);
        label(c, word, lw / 2f, 250, 84, inkColor, true);
        rr(c, 26, 380, 166, 464, 12, 0xFF2FA36B);
        rr(c, 194, 380, 334, 464, 12, 0xFFE8574F);
        label(c, "一致", 96, 422, 26, 0xFFFFFFFF, true);
        label(c, "不同", 264, 422, 26, 0xFFFFFFFF, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
