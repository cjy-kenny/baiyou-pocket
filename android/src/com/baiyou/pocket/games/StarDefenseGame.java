package com.baiyou.pocket.games;

import com.baiyou.pocket.game.PopTargetGame;

import android.graphics.Canvas;
import android.graphics.Path;

/** g081 星际防线：敌舰沿轨道逼近星门，点敌舰开火拦截；
 *  敌舰撞门扣护盾，护盾归零结算。 */
public class StarDefenseGame extends PopTargetGame {

    public StarDefenseGame() {
        super();
    }

    @Override
    protected long spawnGapMs() {
        return Math.max(700, 1200 - score * 5);
    }

    @Override
    protected long lifeMs() {
        return 3200;
    }

    @Override
    protected int maxLives() {
        return 5;
    }

    @Override
    protected int maxOnScreen() {
        return 5;
    }

    @Override
    protected long roundMs() {
        return 0;
    }

    @Override
    protected boolean missCostsLife() {
        return true;
    }

    @Override
    protected void spawnOne() {
        // 敌舰从上方缓降（生命周期长）
        spawnEnt(rand(50, lw - 50), rand(120, lh - 140), 27, lifeMs(), KIND_TARGET);
    }

    @Override
    protected int hitScore(int kind) {
        return 20;
    }

    @Override
    protected void drawEnt(Canvas c, float[] e) {
        float r = e[2];
        fill.setColor(0xFF9B59F6);
        Path p = new Path();
        p.moveTo(e[0], e[1] + r * 0.8f);
        p.lineTo(e[0] - r, e[1] - r * 0.6f);
        p.lineTo(e[0] + r, e[1] - r * 0.6f);
        p.close();
        c.drawPath(p, fill);
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(e[0], e[1], r * 0.3f, fill);
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF0E1420);
        // 星门
        fill.setColor(0xFF3E4B66);
        rr(c, lw / 2f - 70, lh - 70, lw / 2f + 70, lh - 30, 8, 0xFF3E4B66);
        fill.setColor(0xFF7CFFB2);
        rr(c, lw / 2f - 50, lh - 58, lw / 2f + 50, lh - 42, 4, 0xFF7CFFB2);
        label(c, "护盾", 40, 30, 13, 0xFF9AA0AC, false);
        hearts(c, lives, 3, lw - 24, 30, 8);
    }
}
