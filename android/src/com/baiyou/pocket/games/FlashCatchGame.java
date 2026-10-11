package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.PopTargetGame;

/** g026 惊现捕手：目标在随机位置一闪而过，出现瞬间点中它；
 *  漏掉扣命，越来越快。 */
public class FlashCatchGame extends PopTargetGame {

    public FlashCatchGame() {
        super();
    }

    @Override
    protected long spawnGapMs() {
        return Math.max(420, 900 - score * 6);
    }

    @Override
    protected long lifeMs() {
        return Math.max(760, 1200 - score * 6);
    }

    @Override
    protected int maxLives() {
        return 4;
    }

    @Override
    protected int maxOnScreen() {
        return 3;
    }

    @Override
    protected long roundMs() {
        return 45000;
    }

    @Override
    protected int hitScore(int kind) {
        return 15;
    }

    @Override
    protected boolean missCostsLife() {
        return true;
    }

    @Override
    protected void drawEnt(Canvas c, float[] e) {
        long age = now() - (long) e[3];
        float k = Math.min(1f, age / 90f);
        float r = e[2] * (0.3f + 0.7f * easeOut(k));
        fill.setColor(0xFFFFC24B);
        c.drawCircle(e[0], e[1], r, fill);
        fill.setColor(0xFFFFFFFF);
        c.drawCircle(e[0], e[1], r * 0.4f, fill);
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF161A24);
        label(c, "手快有，手慢无", lw / 2f, 30, 13, 0xFF8B93A7, false);
    }
}
