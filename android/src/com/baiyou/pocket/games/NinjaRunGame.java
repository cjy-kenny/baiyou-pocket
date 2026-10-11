package com.baiyou.pocket.games;

import com.baiyou.pocket.game.RunnerGame;

import android.graphics.Canvas;
import android.graphics.Path;

/** g037 蹦跳忍者：自动飞奔，点屏跳过屋檐间的杂物，跑得越远分越高。 */
public class NinjaRunGame extends RunnerGame {

    public NinjaRunGame() {
        super();
    }

    @Override
    protected void spawnObstacle() {
        float w = rand(22, 46);
        float h = rand(34, 62);
        obstacles.add(new float[]{360, w, h, randi(2)});
    }

    @Override
    protected void drawObstacle(Canvas c, float[] o) {
        if ((int) o[3] == 0) {
            // 木箱
            rr(c, o[0], GROUND_Y - o[2], o[0] + o[1], GROUND_Y, 4, 0xFF8A5A2B);
            stroke.setColor(0x66302010);
            stroke.setStrokeWidth(2);
            c.drawLine(o[0], GROUND_Y - o[2], o[0] + o[1], GROUND_Y, stroke);
        } else {
            // 竹刺
            Path p = new Path();
            p.moveTo(o[0], GROUND_Y);
            p.lineTo(o[0] + o[1] / 2f, GROUND_Y - o[2]);
            p.lineTo(o[0] + o[1], GROUND_Y);
            p.close();
            fill.setColor(0xFF9ACDAA);
            c.drawPath(p, fill);
        }
    }

    @Override
    protected float jumpV() {
        return -14.2f;
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1A1420);
        rr(c, 0, GROUND_Y, lw, lh - GROUND_Y, 0, 0xFF2E2438);
        // 屋檐剪影
        fill.setColor(0xFF241C30);
        int seg = 70;
        for (int i = 0; i < 6; i++) {
            float bx = -((int) (score * 0.5f) % seg) + i * seg;
            rr(c, bx, GROUND_Y - 90, bx + seg - 10, GROUND_Y - 84, 3, 0xFF241C30);
        }
        label(c, "月夜疾行", lw / 2f, 40, 13, 0xFF6B5E8F, false);
    }

    @Override
    protected void drawPlayer(Canvas c) {
        // 忍者（蹲跑姿态）
        rr(c, PX - 15, py - 40, PX + 15, py, 7, 0xFF2B2F3A);
        fill.setColor(0xFF7CFFB2);
        rr(c, PX - 15, py - 40, PX + 15, py - 26, 7, 0xFF7CFFB2);
        fill.setColor(0xFF1B1C22);
        c.drawCircle(PX + 6, py - 33, 2.5f, fill);
        // 围巾
        stroke.setColor(0xFFE8574F);
        stroke.setStrokeWidth(3);
        c.drawLine(PX - 14, py - 32, PX - 30, py - 36 + (onGround ? 0 : -6), stroke);
    }
}
