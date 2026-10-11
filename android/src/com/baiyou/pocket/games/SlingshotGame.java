package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

/** g082 弹弓打靶：从弹弓往后拖瞄准，松手弹射，击落飞行标靶；
 *  弹数有限，靶越来越快。 */
public class SlingshotGame extends CanvasGame {
    private static final float SLX = 70, SLY = 430;

    private final Random rnd = new Random();
    private final List<float[]> targets = new ArrayList<>(); // {x,y,r,vx,kind} kind0靶 1风标
    private float projX, projY, projVx, projVy;
    private boolean flying;
    private float dragX = -1, dragY = -1;
    private int shots = 8, score;
    private boolean settling;

    public SlingshotGame() {
        super(360, 560);
        spawnTarget();
    }

    private void spawnTarget() {
        float y = rand(110, 260);
        targets.add(new float[]{rand(150, lw - 60), y, 24, (rnd.nextBoolean() ? 1 : -1) * rand(1.2f, 2.4f), 0});
    }

    private void startRound() {
        targets.clear();
        shots = 8;
        score = 0;
        spawnTarget();
        flying = false;
        settling = false;
        over = false;
        host.setScore(0);
        startLoop(16);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(16);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling) return;
        if (action == 0 && !flying && hit(x, y, SLX - 70, SLY - 70, 140, 140)) {
            dragX = x;
            dragY = y;
        } else if (action == 2 && dragX >= 0) {
            dragX = x;
            dragY = y;
        } else if (action == 1 && dragX >= 0) {
            float dx = SLX - dragX, dy = SLY - dragY;
            float len = (float) Math.sqrt(dx * dx + dy * dy);
            if (len > 22 && shots > 0) {
                projX = SLX;
                projY = SLY;
                projVx = dx / len * Math.min(14f, len * 0.13f);
                projVy = dy / len * Math.min(14f, len * 0.13f);
                flying = true;
                shots--;
                beep(0);
            }
            dragX = dragY = -1;
        }
        dirty();
    }

    private void finish(String why) {
        settling = true;
        over = true;
        stopLoop();
        popup(why + " 得分 " + score, lw / 2f, lh / 2f, 0xFFECECF1, 18);
        beep(3);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 900);
    }

    @Override
    protected void onTick() {
        // 靶移动
        for (float[] t : targets) {
            t[0] += t[3];
            if (t[0] < 40 || t[0] > lw - 40) t[3] = -t[3];
        }
        // 弹丸
        if (flying) {
            projX += projVx;
            projY += projVy;
            Iterator<float[]> it = targets.iterator();
            while (it.hasNext()) {
                float[] t = it.next();
                float dx = projX - t[0], dy = projY - t[1];
                if (dx * dx + dy * dy < t[2] * t[2]) {
                    it.remove();
                    score += 50;
                    host.setScore(score);
                    beep(1);
                    burst(t[0], t[1], 0xFFFFC24B, 14, 6f);
                    popup("+50", t[0], t[1] - 24, 0xFFFFC24B, 17);
                    flying = false;
                    if (targets.isEmpty()) spawnTarget();
                    break;
                }
            }
            if (flying && (projX < -20 || projX > lw + 20 || projY < -20 || projY > lh + 20)) {
                flying = false;
            }
        }
        if (shots <= 0 && !flying) {
            finish("弹弓用完");
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF24301E);
        for (float[] t : targets) {
            fill.setColor(0xFFFFC24B);
            c.drawCircle(t[0], t[1], t[2], fill);
            stroke.setColor(0xFFE8574F);
            stroke.setStrokeWidth(3);
            rf.set(t[0] - t[2] * 0.5f, t[1] - t[2] * 0.5f, t[0] + t[2] * 0.5f, t[1] + t[2] * 0.5f);
            c.drawRoundRect(rf, 4, 4, stroke);
        }
        // 弹弓
        stroke.setColor(0xFF8A5A2B);
        stroke.setStrokeWidth(5);
        c.drawLine(SLX - 12, SLY + 10, SLX - 4, SLY - 26, stroke);
        c.drawLine(SLX + 12, SLY + 10, SLX + 4, SLY - 26, stroke);
        if (dragX >= 0 && !flying) {
            stroke.setColor(0xFF7CFFB2);
            stroke.setStrokeWidth(2);
            c.drawLine(SLX - 4, SLY - 26, dragX, dragY, stroke);
            c.drawLine(SLX + 4, SLY - 26, dragX, dragY, stroke);
            fill.setColor(0xFF33363F);
            c.drawCircle(dragX, dragY, 9, fill);
        }
        if (flying) {
            fill.setColor(0xFF33363F);
            c.drawCircle(projX, projY, 9, fill);
        }
        label(c, "弹药 " + shots, lw - 44, 32, 15, 0xFF9ACDAA, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
