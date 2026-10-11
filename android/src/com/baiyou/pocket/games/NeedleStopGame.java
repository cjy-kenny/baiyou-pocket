package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g031 刻度停针：指针绕圈转，在亮起的扇区里按停；命中 +10 且扇区变小、
 *  转速加快；脱靶扣命，3 命尽结算。 */
public class NeedleStopGame extends CanvasGame {
    private static final float CX = 180, CY = 280, R = 120;

    private final Random rnd = new Random();
    private float angle;
    private float speed = 4.2f;
    private float targetStart, targetSpan = 70;
    private int lives = 3, shownScore, hits;
    private boolean settling;

    public NeedleStopGame() {
        super(360, 560);
        newTarget();
    }

    private void newTarget() {
        targetStart = rand(0, 340);
        targetSpan = Math.max(26f, 70f - hits * 5f);
        speed = 4.2f + hits * 0.5f;
    }

    private void startRound() {
        lives = 3;
        shownScore = 0;
        hits = 0;
        angle = 0;
        newTarget();
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
        if (over || settling || action != 0) return;
        // 指针角度（0=正上，顺时针）
        float deg = (angle % 360 + 360) % 360;
        float t0 = targetStart % 360, t1 = (targetStart + targetSpan) % 360;
        boolean in;
        if (t0 < t1) in = deg >= t0 && deg <= t1;
        else in = deg >= t0 || deg <= t1;
        if (in) {
            hits++;
            shownScore += 10 + hits;
            host.setScore(shownScore);
            beep(1);
            popup("+ " + (10 + hits), CX, CY - R - 24, 0xFF7CFFB2, 16);
            burst(CX, CY, 0xFF7CFFB2, 10, 5f);
            newTarget();
        } else {
            lives--;
            beep(2);
            shake(200);
            popup("脱靶！", CX, CY - R - 24, 0xFFE8574F, 15);
            if (lives <= 0) {
                settling = true;
                over = true;
                stopLoop();
                popup("命中 " + hits + " 次", lw / 2f, lh / 2f, 0xFFECECF1, 17);
                dirty();
                postDelayedSafe(() -> host.gameOver(shownScore), 850);
                return;
            }
        }
        dirty();
    }

    @Override
    protected void onTick() {
        angle += speed;
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF181420);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, "命中 " + hits, 40, 30, 14, 0xFFECECF1, true);
        // 表盘
        fill.setColor(0xFF241C30);
        c.drawCircle(CX, CY, R + 16, fill);
        // 目标扇区
        float t0 = targetStart - 90, t1 = t0 + targetSpan;
        fill.setColor(0x887CFFB2);
        rf.set(CX - R, CY - R, CX + R, CY + R);
        c.drawArc(rf, t0, targetSpan, true, fill);
        // 刻度
        for (int d = 0; d < 12; d++) {
            double a = Math.toRadians(d * 30 - 90);
            float x1 = CX + (float) Math.cos(a) * (R - 8), y1 = CY + (float) Math.sin(a) * (R - 8);
            float x2 = CX + (float) Math.cos(a) * R, y2 = CY + (float) Math.sin(a) * R;
            stroke.setColor(0xFF566079);
            stroke.setStrokeWidth(2);
            c.drawLine(x1, y1, x2, y2, stroke);
        }
        // 指针
        double a = Math.toRadians(angle - 90);
        float nx = CX + (float) Math.cos(a) * (R - 14), ny = CY + (float) Math.sin(a) * (R - 14);
        stroke.setColor(0xFFFFC24B);
        stroke.setStrokeWidth(5);
        c.drawLine(CX, CY, nx, ny, stroke);
        fill.setColor(0xFFFFC24B);
        c.drawCircle(CX, CY, 10, fill);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
