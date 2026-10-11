package com.baiyou.pocket.games;

import com.baiyou.pocket.game.DodgeGame;

import android.graphics.Canvas;
import android.graphics.Path;

/** g077 滑雪竞速：左右滑行躲树障，雪道飞速下退，坚持越久滑得越远。 */
public class SkiDodgeGame extends DodgeGame {

    public SkiDodgeGame() {
        super();
    }

    private int tickCount;

    @Override
    protected void spawn(long tick) {
        tickCount = (int) tick;
        if (tick % 36 == 0) {
            float w = rand(22, 40);
            addEnt(rand(w, lw - w), -30, rand(4.4f, 6.4f), w, w, 0xFF2E5C3A, 0);
        }
        if (tick % 150 == 75) {
            // 岩石
            addEnt(rand(30, lw - 30), -26, rand(4f, 5.4f), 40, 30, 0xFF6E6257, 1);
        }
    }

    @Override
    protected void drawEnt(Canvas c, float[] e) {
        if ((int) e[6] == 0) {
            // 松树
            float w = e[3], top = e[1] - w;
            fill.setColor(0xFF6B4A2B);
            rr(c, e[0] - 4, e[1] - 4, e[0] + 4, e[1] + 10, 2, 0xFF6B4A2B);
            Path p = new Path();
            p.moveTo(e[0], top);
            p.lineTo(e[0] - w / 2f, e[1] + 6);
            p.lineTo(e[0] + w / 2f, e[1] + 6);
            p.close();
            fill.setColor(0xFF2E6C42);
            c.drawPath(p, fill);
            fill.setColor(0x66FFFFFF);
            c.drawCircle(e[0], top + w * 0.2f, w * 0.12f, fill);
        } else {
            rr(c, e[0] - e[3] / 2f, e[1] - e[4] / 2f, e[0] + e[3] / 2f, e[1] + e[4] / 2f,
                    6, 0xFF6E6257);
        }
    }

    @Override
    protected void drawPlayer(Canvas c) {
        // 滑雪者
        fill.setColor(0xFFE8574F);
        rr(c, px - 9, PY - 6, px + 9, PY + 22, 5, 0xFFE8574F);
        fill.setColor(0xFFF4F5F9);
        c.drawCircle(px, PY - 2, 8, fill);
        stroke.setColor(0xFFFFC24B);
        stroke.setStrokeWidth(4);
        c.drawLine(px - 14, PY + 24, px + 14, PY + 24, stroke);
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFFDDE6EE);
        // 雪道痕迹
        stroke.setColor(0xFFC8D4E0);
        stroke.setStrokeWidth(2);
        int off = (int) (System.currentTimeMillis() / 10 % 46);
        for (int y = -46 + off; y < lh; y += 46) {
            c.drawLine(0, y, lw, y, stroke);
        }
        label(c, "滑行 " + score + " m", lw / 2f, 34, 15, 0xFF3A4A5A, true);
    }
}
