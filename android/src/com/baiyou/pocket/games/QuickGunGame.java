package com.baiyou.pocket.games;

import com.baiyou.pocket.game.PopTargetGame;

import android.graphics.Canvas;

/** g083 疾速神枪：靶子随机闪现，出现即刻开枪；漏靶扣命，越往后闪越快。 */
public class QuickGunGame extends PopTargetGame {

    public QuickGunGame() {
        super();
    }

    @Override
    protected long spawnGapMs() {
        return Math.max(620, 1100 - score * 7);
    }

    @Override
    protected long lifeMs() {
        return Math.max(1300, 1800 - score * 6);
    }

    @Override
    protected int maxOnScreen() {
        return 2;
    }

    @Override
    protected long roundMs() {
        return 45000;
    }

    @Override
    protected int maxLives() {
        return 4;
    }

    @Override
    protected int hitScore(int kind) {
        return 18;
    }

    @Override
    protected boolean missCostsLife() {
        return true;
    }

    @Override
    protected void drawEnt(Canvas c, float[] e) {
        long age = now() - (long) e[3];
        float k = Math.min(1f, age / 80f);
        float r = e[2] * (0.5f + 0.5f * easeOut(k));
        // 靶心
        fill.setColor(0xFFF4F5F9);
        c.drawCircle(e[0], e[1], r, fill);
        fill.setColor(0xFFE8574F);
        c.drawCircle(e[0], e[1], r * 0.66f, fill);
        fill.setColor(0xFFFFC24B);
        c.drawCircle(e[0], e[1], r * 0.3f, fill);
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF26221C);
        // 靶场条纹
        stroke.setColor(0xFF3A3226);
        stroke.setStrokeWidth(2);
        for (int i = 0; i < 6; i++) {
            c.drawLine(0, 120 + i * 70, lw, 130 + i * 70, stroke);
        }
        label(c, "指哪打哪", lw / 2f, 34, 13, 0xFFC9B896, false);
    }
}
