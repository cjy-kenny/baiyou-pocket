package com.baiyou.pocket.games;

import android.graphics.Canvas;
import android.graphics.Path;

import com.baiyou.pocket.game.DodgeGame;

/** g034 星屑闪避：飞船横移躲陨石带，捡到金星能加分，撞上即毁。 */
public class StarfieldDodgeGame extends DodgeGame {

    public StarfieldDodgeGame() {
        super();
    }

    private int tickCount;

    @Override
    protected void spawn(long tick) {
        tickCount = (int) tick;
        if (tick % 30 == 0) {
            float r = rand(14, 30);
            addEnt(rand(r, lw - r), -r, rand(3.2f, 5.8f), r * 2, r * 2,
                    0xFF8A7B6B, 0);
        }
        if (tick % 240 == 120) {
            addEnt(rand(24, lw - 24), -16, rand(3.6f, 4.4f), 22, 22, 0xFFFFC24B, 0);
        }
    }

    @Override
    protected void onPass(float[] e) {
        if ((int) e[5] == 0xFFFFC24B) {
            score += 30;
            host.setScore(score);
            popup("+30", e[0], lh - 60, 0xFFFFC24B, 15);
        }
    }

    @Override
    protected void drawEnt(Canvas c, float[] e) {
        if ((int) e[5] == 0xFFFFC24B) {
            fill.setColor(0xFFFFC24B);
            c.drawCircle(e[0], e[1], e[3] / 2f, fill);
            label(c, "★", e[0], e[1], e[3] * 0.7f, 0xFFFFFFFF, true);
            return;
        }
        float r = e[3] / 2f;
        fill.setColor(0xFF6E6257);
        c.drawCircle(e[0], e[1], r, fill);
        fill.setColor(0xFF574D44);
        c.drawCircle(e[0] - r * 0.3f, e[1] + r * 0.2f, r * 0.4f, fill);
        fill.setColor(0xFF8A7B6B);
        c.drawCircle(e[0] + r * 0.35f, e[1] - r * 0.3f, r * 0.25f, fill);
    }

    @Override
    protected void drawPlayer(Canvas c) {
        Path p = new Path();
        p.moveTo(px, PY);
        p.lineTo(px - PW / 2f, PY + PH + 6);
        p.lineTo(px + PW / 2f, PY + PH + 6);
        p.close();
        fill.setColor(0xFF7CFFB2);
        c.drawPath(p, fill);
        fill.setColor(0xFFFF9D5C);
        c.drawCircle(px, PY + PH + 4, 4, fill);
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF0C0F1A);
        fill.setColor(0xFF3A4266);
        for (int i = 0; i < 24; i++) {
            float sx = (i * 97) % lw;
            float sy = ((i * 53) + tickCount * (0.5f + i % 3)) % lh;
            c.drawCircle(sx, sy, i % 3 == 0 ? 1.6f : 1f, fill);
        }
    }
}
