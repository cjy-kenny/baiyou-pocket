package com.baiyou.pocket.games;

import android.graphics.Canvas;
import android.graphics.Path;

import com.baiyou.pocket.game.DodgeGame;

/** g038 无尽坠落：持续下坠，拖住落者躲开自下而上的尖刺与断桥，
 *  下得越深分越高。 */
public class EndlessFallGame extends DodgeGame {

    public EndlessFallGame() {
        super();
    }

    @Override
    protected void spawn(long tick) {
        if (tick % 26 == 0) {
            float w = rand(30, 70);
            addEnt(rand(w / 2, lw - w / 2), lh + 24, -rand(4.6f, 7.2f), w, 16, 0xFF9AA0AC, 1);
        }
        if (tick % 90 == 45) {
            float w = rand(60, 110);
            addEnt(rand(w / 2, lw - w / 2), lh + 30, -rand(4.2f, 5.4f), w, 22, 0xFF6B7280, 1);
        }
    }

    @Override
    protected void drawEnt(Canvas c, float[] e) {
        float w = e[3], h = e[4];
        if ((int) e[6] == 0) {
            // 尖刺朝上
            Path p = new Path();
            int n = 3;
            p.moveTo(e[0] - w / 2f, e[1] + h / 2f);
            for (int i = 0; i < n; i++) {
                p.lineTo(e[0] - w / 2f + w * (i + 0.5f) / n, e[1] - h / 2f);
                p.lineTo(e[0] - w / 2f + w * (i + 1f) / n, e[1] + h / 2f);
            }
            p.close();
            fill.setColor(0xFFB9C4E0);
            c.drawPath(p, fill);
        } else {
            rr(c, e[0] - w / 2f, e[1] - h / 2f, e[0] + w / 2f, e[1] + h / 2f, 4, 0xFF6B7280);
            stroke.setColor(0x88303646);
            stroke.setStrokeWidth(2);
            c.drawLine(e[0] - w / 2f, e[1], e[0] + w / 2f, e[1], stroke);
        }
    }

    @Override
    protected void drawPlayer(Canvas c) {
        // 落者（张开手臂）
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(px, PY + PH / 2f - 4, 12, fill);
        rr(c, px - 8, PY + PH / 2f + 6, px + 8, PY + PH / 2f + 16, 4, 0xFF5BAF8F);
        stroke.setColor(0xFF7CFFB2);
        stroke.setStrokeWidth(3);
        c.drawLine(px - 10, PY + PH / 2f, px - 20, PY + PH / 2f - 8, stroke);
        c.drawLine(px + 10, PY + PH / 2f, px + 20, PY + PH / 2f - 8, stroke);
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF101018);
        // 深度刻度线向上滚动的感觉
        stroke.setColor(0xFF2A2D3E);
        stroke.setStrokeWidth(1);
        int off = (int) (System.currentTimeMillis() / 12 % 40);
        for (int y = -40 + off; y < lh; y += 40) {
            c.drawLine(0, y, lw, y, stroke);
        }
        label(c, "越深越危险", lw / 2f, 30, 12, 0xFF566079, false);
    }
}
