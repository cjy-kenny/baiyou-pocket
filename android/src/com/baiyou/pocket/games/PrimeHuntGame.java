package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.PopTargetGame;

/** g021 素数猎手：数字冒出来，只捕质数（+15）；合数是雷（点了扣命），
 *  质数漏掉也扣命；合数到时自己消失。45 秒猎捕。 */
public class PrimeHuntGame extends PopTargetGame {

    public PrimeHuntGame() {
        super();
    }

    private static boolean isPrime(int n) {
        if (n < 2) return false;
        for (int i = 2; i * i <= n; i++) if (n % i == 0) return false;
        return true;
    }

    @Override
    protected long spawnGapMs() {
        return Math.max(560, 1000 - score * 4);
    }

    @Override
    protected int maxLives() {
        return 4;
    }

    @Override
    protected long lifeMs() {
        return 1500;
    }

    @Override
    protected int maxOnScreen() {
        return 5;
    }

    @Override
    protected long roundMs() {
        return 45000;
    }

    @Override
    protected void spawnOne() {
        int n = 2 + randi(48);
        int kind = isPrime(n) ? KIND_TARGET : KIND_BOMB;
        spawnEnt(rand(40, lw - 40), rand(120, lh - 90), 26, lifeMs(), kind);
        lastEnt()[6] = n;
    }

    @Override
    protected int hitScore(int kind) {
        return kind == KIND_TARGET ? 15 : 0;
    }

    @Override
    protected boolean missCostsLife() {
        return true; // 质数漏掉扣命（合数是 BOMB，超时无害）
    }

    @Override
    protected void drawEnt(Canvas c, float[] e) {
        long age = now() - (long) e[3];
        float k = Math.min(1f, age / 140f);
        float r = e[2] * (0.6f + 0.4f * easeOut(k));
        boolean prime = (int) e[5] == KIND_TARGET;
        fill.setColor(prime ? 0xFF2FA36B : 0xFF4A3B52);
        c.drawCircle(e[0], e[1], r, fill);
        stroke.setColor(prime ? 0xFF7CFFB2 : 0xFFE8848C);
        stroke.setStrokeWidth(2);
        c.drawCircle(e[0], e[1], r, stroke);
        label(c, String.valueOf((int) e[6]), e[0], e[1], r * 0.9f, 0xFFF4F5F9, true);
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF14201A);
        label(c, "捕质数 · 别碰合数", lw / 2f, 30, 13, 0xFF9ACDAA, false);
    }
}
