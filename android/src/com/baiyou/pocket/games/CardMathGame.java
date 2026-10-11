package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;
import com.baiyou.pocket.game.CardKit;

import java.util.Random;

/** g055 算牌速配：两张牌与一个运算符，让结果等于目标数；
 *  60 秒内配对越多越好，算错扣命。 */
public class CardMathGame extends CanvasGame {
    private static final long ROUND_MS = 60000;
    private static final String OPS = "+-×";

    private final Random rnd = new Random();
    private int a, b, target, opAns;
    private final boolean[] optOk = new boolean[3];
    private int lives = 3, score;
    private long roundEnd;
    private boolean settling;

    public CardMathGame() {
        super(360, 560);
        nextPuzzle();
    }

    private void nextPuzzle() {
        a = 1 + rnd.nextInt(10);
        b = 1 + rnd.nextInt(10);
        int op = rnd.nextInt(3);
        switch (op) {
            case 0: opAns = a + b; break;
            case 1: opAns = Math.max(a, b) - Math.min(a, b); break;
            default: opAns = a * b; break;
        }
        target = opAns;
        // 三个运算符按钮，其中至少一个能得到目标
        for (int i = 0; i < 3; i++) {
            int r;
            switch (OPS.charAt(i)) {
                case '+': r = a + b; break;
                case '-': r = Math.max(a, b) - Math.min(a, b); break;
                default: r = a * b; break;
            }
            optOk[i] = r == target;
        }
    }

    private void startRound() {
        lives = 3;
        score = 0;
        roundEnd = now() + ROUND_MS;
        nextPuzzle();
        settling = false;
        over = false;
        host.setScore(0);
        startLoop(100);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(100);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        for (int i = 0; i < 3; i++) {
            float ox = 30 + i * 104;
            if (hit(x, y, ox, 380, 92, 84)) {
                if (optOk[i]) {
                    score += 15;
                    host.setScore(score);
                    beep(1);
                    burst(ox + 46, 422, 0xFF7CFFB2, 10, 5f);
                    nextPuzzle();
                } else {
                    lives--;
                    beep(2);
                    shake(200);
                    if (lives <= 0) {
                        over = true;
                        stopLoop();
                        popup("算岔了", lw / 2f, lh / 2f, 0xFFECECF1, 17);
                        dirty();
                        postDelayedSafe(() -> host.gameOver(score), 850);
                        return;
                    }
                }
                dirty();
                return;
            }
        }
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF26181E);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, "让两张牌凑出目标", lw / 2f, 100, 15, 0xFFE8C4CE, false);
        // 两张牌 + 问号
        CardKit.draw(this, c, a, 0, 40, 140, 72, 96, true);
        label(c, "?", 180, 190, 40, 0xFFFFC24B, true);
        CardKit.draw(this, c, b, 1, 248, 140, 72, 96, true);
        // 目标
        rr(c, 90, 270, 270, 340, 14, 0xFF3A2430);
        label(c, "= " + target, 180, 306, 34, 0xFFFFC24B, true);
        // 运算符
        for (int i = 0; i < 3; i++) {
            float ox = 30 + i * 104;
            rr(c, ox, 380, ox + 92, 464, 12, 0xFF3A2A34);
            label(c, String.valueOf(OPS.charAt(i)), ox + 46, 422, 34, 0xFFECECF1, true);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
