package com.baiyou.pocket.games;

import com.baiyou.pocket.game.RunnerGame;

import android.graphics.Canvas;

/** g078 跨栏小将：自动冲刺，点屏起跳跨栏，撞栏减速仍要跑，
 *  距离就是成绩。 */
public class HurdleRunGame extends RunnerGame {

    public HurdleRunGame() {
        super();
    }

    @Override
    protected void spawnObstacle() {
        float w = 30;
        float h = 44;
        obstacles.add(new float[]{360, w, h, 0});
    }

    @Override
    protected void drawObstacle(Canvas c, float[] o) {
        // 栏架
        rr(c, o[0], GROUND_Y - o[2], o[0] + o[1], GROUND_Y, 3, 0xFF5B8DEF);
        stroke.setColor(0xFFECECF1);
        stroke.setStrokeWidth(3);
        c.drawLine(o[0] - 3, GROUND_Y - o[2] + 6, o[0] + o[1] + 3, GROUND_Y - o[2] + 6, stroke);
        stroke.setColor(0xFFE8574F);
        c.drawLine(o[0] - 3, GROUND_Y - o[2] + 18, o[0] + o[1] + 3, GROUND_Y - o[2] + 18, stroke);
    }

    @Override
    protected float jumpV() {
        return -12.8f;
    }

    @Override
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF2A3038);
        // 跑道
        rr(c, 0, GROUND_Y, lw, lh - GROUND_Y, 0, 0xFFB4553C);
        stroke.setColor(0x66FFFFFF);
        stroke.setStrokeWidth(3);
        int off = (int) (score * 0.8f % 60);
        for (int x = -60 + off; x < lw; x += 60) {
            c.drawLine(x, GROUND_Y + 26, x + 28, GROUND_Y + 26, stroke);
        }
        // 看台
        fill.setColor(0xFF3A4250);
        rr(c, 0, 60, lw, GROUND_Y - 90, 4, 0xFF3A4250);
        fill.setColor(0xFF566079);
        for (int i = 0; i < 40; i++) {
            c.drawCircle((i * 83) % lw, 80 + (i * 37) % 180, 3, fill);
        }
        label(c, "节奏起跳", lw / 2f, 40, 13, 0xFF9AA0AC, false);
    }

    @Override
    protected void drawPlayer(Canvas c) {
        // 跨栏选手（前倾）
        rr(c, PX - 12, py - 42, PX + 14, py, 7, 0xFFFFC24B);
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(PX + 4, py - 34, 7, fill);
        stroke.setColor(0xFF1B1C22);
        stroke.setStrokeWidth(3);
        c.drawLine(PX + 8, py - 14, PX + 20, py + (onGround ? 0 : -8), stroke);
        c.drawLine(PX - 6, py - 14, PX - 16, py + (onGround ? 0 : -10), stroke);
    }
}
