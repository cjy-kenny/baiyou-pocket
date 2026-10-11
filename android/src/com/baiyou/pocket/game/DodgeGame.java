package com.baiyou.pocket.game;

import android.graphics.Canvas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/** 躲避类家族基类：玩家随手指横向移动，上方/下方掉落物碰到即结束，
 *  存活时间即得分（子类可加穿越加分）。 */
public abstract class DodgeGame extends CanvasGame {
    public static final float PW = 34, PH = 34, PY = 480;

    protected final List<float[]> ents = new ArrayList<>(); // {x,y,vy,w,h,color,shape}
    protected float px = 180;
    protected int score;
    private long lastScoreAt;
    private boolean dying;

    protected DodgeGame() {
        super(360, 560);
    }

    /** 每帧调用：子类按难度生成掉落物（addEnt）。 */
    protected abstract void spawn(long tick);

    /** 背景装饰（可覆写）。 */
    protected void drawBg(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1B1E26);
    }

    /** 掉落物绘制。shape：0 圆 1 方。 */
    protected void drawEnt(Canvas c, float[] e) {
        fill.setColor((int) e[5]);
        if ((int) e[6] == 0) c.drawCircle(e[0], e[1], e[3] / 2f, fill);
        else rr(c, e[0] - e[3] / 2f, e[1] - e[4] / 2f, e[0] + e[3] / 2f, e[1] + e[4] / 2f, 4, (int) e[5]);
    }

    /** 玩家绘制（默认圆形，子类可覆写）。 */
    protected void drawPlayer(Canvas c) {
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(px, PY + PH / 2f, PW / 2f, fill);
        stroke.setColor(0xFFFFFFFF);
        stroke.setStrokeWidth(2);
        c.drawCircle(px, PY + PH / 2f, PW / 2f + 3, stroke);
    }

    /** 生存加分速率：每 500ms 加多少（默认 1）。 */
    protected int scoreStep() {
        return 1;
    }

    protected final void addEnt(float x, float y, float vy, float w, float h, int color, int shape) {
        ents.add(new float[]{x, y, vy, w, h, color, shape});
    }

    @Override
    public void restartGame() {
        ents.clear();
        px = lw / 2f;
        score = 0;
        lastScoreAt = now();
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
        if (over) return;
        if (action == 0 || action == 2) {
            px = Math.max(PW / 2 + 4, Math.min(lw - PW / 2 - 4, x));
            dirty();
        }
    }

    @Override
    protected void onTick() {
        long now = now();
        if (!dying && now - lastScoreAt >= 500) {
            lastScoreAt = now;
            score += scoreStep();
            host.setScore(score);
        }
        spawn(now / 16);
        Iterator<float[]> it = ents.iterator();
        while (it.hasNext()) {
            float[] e = it.next();
            e[1] += e[2];
            if (e[1] > lh + 40) {
                it.remove();
                onPass(e);
                continue;
            }
            // 碰撞（近似圆）
            float dx = Math.abs(e[0] - px), dy = Math.abs(e[1] - (PY + PH / 2f));
            if (dx < (e[3] + PW) / 2f - 4 && dy < (e[4] + PH) / 2f - 4) {
                it.remove();
                die(e);
                return;
            }
        }
        dirty();
    }

    /** 掉落物安全飞出（子类可用于穿行加分）。 */
    protected void onPass(float[] e) {
    }

    private void die(float[] e) {
        dying = true;
        over = true;
        stopLoop();
        shake(320);
        burst(px, PY + PH / 2f, 0xFFE8574F, 20, 7f);
        beep(2);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 750);
    }

    @Override
    protected void render(Canvas c) {
        drawBg(c);
        for (float[] e : ents) drawEnt(c, e);
        if (!dying) drawPlayer(c);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
