package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.DodgeGame;

/** g030 红区闪避：拖住绿点躲开不断砸下的红色区域，
 *  坚持越久分越高（每 0.5 秒 +1）。 */
public class RedZoneDodgeGame extends DodgeGame {

    public RedZoneDodgeGame() {
        super();
    }

    private int spawnTick;

    @Override
    protected void spawn(long tick) {
        spawnTick = (int) tick;
        if (tick % 34 == 0) {
            float w = rand(46, 110);
            addEnt(rand(w / 2, lw - w / 2), -30, rand(3.4f, 5.6f), w, w,
                    0xFFE8574F, 1);
        }
        if (tick % 110 == 55) {
            // 偶发横向漂移的小方块
            addEnt(rand(20, lw - 20), -20, rand(4f, 6f), 26, 26, 0xFFE8848C, 0);
        }
    }

    @Override
    protected void drawEnt(Canvas c, float[] e) {
        fill.setColor((int) e[5]);
        rr(c, e[0] - e[3] / 2f, e[1] - e[4] / 2f, e[0] + e[3] / 2f, e[1] + e[4] / 2f, 6, (int) e[5]);
        stroke.setColor(0x66FFFFFF);
        stroke.setStrokeWidth(1.5f);
        rf.set(e[0] - e[3] / 2f + 4, e[1] - e[4] / 2f + 4, e[0] + e[3] / 2f - 4, e[1] + e[4] / 2f - 4);
        c.drawRoundRect(rf, 4, 4, stroke);
    }

    @Override
    protected void drawPlayer(Canvas c) {
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(px, PY + PH / 2f, PW / 2f, fill);
        fill.setColor(0xFF14342B);
        c.drawCircle(px, PY + PH / 2f, 4, fill);
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF17141C);
        label(c, "拖住绿点 · 躲红块", lw / 2f, 30, 12, 0xFF6B5E8F, false);
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
