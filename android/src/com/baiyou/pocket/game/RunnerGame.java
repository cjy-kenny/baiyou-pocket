package com.baiyou.pocket.game;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 横版跑酷家族基类：自动前进、点按跳跃、躲开障碍；
 *  距离即得分，撞到障碍结算（子类可定义障碍与皮肤）。 */
public abstract class RunnerGame extends CanvasGame {
    protected static final float GROUND_Y = 470, PX = 90;

    protected final List<float[]> obstacles = new ArrayList<>(); // {x, w, h, kind}
    protected float speed = 6f;
    protected float py = GROUND_Y, vy;
    protected boolean onGround = true;
    protected int score;
    protected long tick;
    private boolean dying;

    protected RunnerGame() {
        super(360, 560);
    }

    /** 障碍生成间隔（帧）；子类可随速度调整。 */
    protected int spawnGapFrames() {
        return Math.max(34, 80 - (int) (speed * 3));
    }

    /** 生成一个障碍 {x=360, w, h, kind}。 */
    protected abstract void spawnObstacle();

    /** 障碍绘制。 */
    protected abstract void drawObstacle(Canvas c, float[] o);

    /** 跳跃力度（负方向速度）。 */
    protected float jumpV() {
        return -13.5f;
    }

    /** 背景（子类可覆写）。 */
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF151A24);
        rr(c, 0, GROUND_Y, lw, lh - GROUND_Y, 0, 0xFF2A3040);
    }

    /** 玩家绘制（默认圆头方块人，子类可覆写）。 */
    protected void drawPlayer(Canvas c) {
        rr(c, PX - 16, py - 46, PX + 16, py, 6, 0xFF7CFFB2);
        fill.setColor(0xFF14342B);
        c.drawCircle(PX + 6, py - 36, 2.5f, fill);
    }

    @Override
    public void restartGame() {
        obstacles.clear();
        speed = 6f;
        py = GROUND_Y;
        vy = 0;
        onGround = true;
        score = 0;
        tick = 0;
        dying = false;
        over = false;
        host.setScore(0);
        startLoop(16);
        dirty();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(16);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || dying) return;
        if (action == 0 && onGround) {
            vy = jumpV();
            onGround = false;
            beep(0);
        }
    }

    @Override
    protected void onTick() {
        tick++;
        if (tick % spawnGapFrames() == 0) spawnObstacle();
        speed += 0.0018f;

        // 跳跃物理
        if (!onGround) {
            vy += 0.55f;
            py += vy;
            if (py >= GROUND_Y) {
                py = GROUND_Y;
                vy = 0;
                onGround = true;
            }
        }
        // 障碍移动与碰撞
        Iterator<float[]> it = obstacles.iterator();
        while (it.hasNext()) {
            float[] o = it.next();
            o[0] -= speed;
            if (o[0] + o[1] < -10) {
                it.remove();
                continue;
            }
            boolean overlapX = PX + 14 > o[0] && PX - 14 < o[0] + o[1];
            boolean overlapY = py > GROUND_Y - o[2] + 2;
            if (overlapX && overlapY) {
                die();
                return;
            }
        }
        // 计分：距离
        score += Math.round(speed / 3f);
        host.setScore(score);
        dirty();
    }

    private void die() {
        dying = true;
        over = true;
        stopLoop();
        shake(300);
        burst(PX, py - 24, 0xFFE8574F, 18, 7f);
        beep(2);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 750);
    }

    @Override
    protected void render(Canvas c) {
        drawBg(c);
        for (float[] o : obstacles) drawObstacle(c, o);
        if (!dying) drawPlayer(c);
        label(c, score + " m", lw - 50, 28, 16, 0xFF9AA0AC, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
