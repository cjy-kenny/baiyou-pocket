package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g039 圆环穿梭：同心圆环各有一个缺口并反向旋转，点屏让小球往外跳一格；
 *  只有对准缺口才能穿过，撞环即结束，穿过几环得几分。 */
public class RingDashGame extends CanvasGame {
    private static final int RINGS = 5;
    private static final float CX = 180, CY = 290, GAP_DEG = 52;

    private final Random rnd = new Random();
    private final float[] ringAngle = new float[RINGS];
    private final float[] ringSpeed = new float[RINGS];
    private int at = 0;   // 0=中心，1..RINGS-1=所在环，RINGS=出太空
    private float hopK;
    private boolean settling;

    public RingDashGame() {
        super(360, 560);
        startRound();
    }

    private void startRound() {
        for (int i = 0; i < RINGS; i++) {
            ringAngle[i] = rand(0, 360);
            ringSpeed[i] = (1.1f + i * 0.35f) * (i % 2 == 0 ? 1 : -1);
        }
        at = 0;
        hopK = 0;
        settling = false;
        over = false;
        startLoop(16);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
        host.setScore(0);
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(16);
    }

    private float ringR(int i) {
        return 46 + i * 34;
    }

    /** 球当前显示半径（跳跃插值）。 */
    private float ballR() {
        float from = at == 0 ? 14 : ringR(at - 1);
        float to = at >= RINGS ? ringR(RINGS - 1) + 60 : ringR(at);
        return from + (to - from) * easeOut(hopK);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0 || hopK > 0) return;
        // 目标环
        int target = at + 1;
        if (target <= RINGS - 1) {
            float gapCenter = ringAngle[target];
            float diff = Math.abs(((angleOf(x, y) - gapCenter + 540) % 360) - 180);
            float within = GAP_DEG / 2f + (target == 0 ? 20 : 14) - at * 1.5f;
            if (diff > within) {
                over = true;
                stopLoop();
                shake(300);
                burst(CX, CY, 0xFFE8574F, 18, 7f);
                beep(2);
                dirty();
                postDelayedSafe(() -> host.gameOver(at * 20), 800);
                return;
            }
        }
        at = target;
        hopK = 0.001f;
        score();
        beep(at >= RINGS ? 3 : 1);
        if (at >= RINGS) {
            settling = true;
            popup("冲出星环！", lw / 2f, 120, 0xFFFFC24B, 22);
            dirty();
            postDelayedSafe(() -> {
                over = true;
                stopLoop();
                dirty();
                postDelayedSafe(() -> host.gameOver(RINGS * 20), 700);
            }, 900);
        }
        dirty();
    }

    private void score() {
        host.setScore(Math.min(at, RINGS) * 20);
    }

    private float angleOf(float x, float y) {
        double a = Math.toDegrees(Math.atan2(y - CY, x - CX));
        return (float) ((a + 360) % 360);
    }

    @Override
    protected void onTick() {
        for (int i = 0; i < RINGS; i++) {
            ringAngle[i] = (ringAngle[i] + ringSpeed[i] + 360) % 360;
        }
        if (hopK > 0) {
            hopK += 0.08f;
            if (hopK >= 1) hopK = at >= RINGS ? 1 : 0;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF0B0E18);
        label(c, "对准缺口 · 点屏外跳", lw / 2f, 40, 13, 0xFF8B93A7, false);
        for (int i = 0; i < RINGS; i++) {
            float r = ringR(i);
            stroke.setColor(i == at - 1 ? 0xFF7CFFB2 : 0xFF4A5568);
            stroke.setStrokeWidth(i == at - 1 ? 5 : 3);
            rf.set(CX - r, CY - r, CX + r, CY + r);
            float gap0 = ringAngle[i] - 90 - GAP_DEG / 2f;
            c.drawArc(rf, gap0 + GAP_DEG, 360 - GAP_DEG, false, stroke);
        }
        // 小球
        double a = Math.toRadians(ringAngle[Math.max(0, Math.min(at, RINGS - 1))] - 90);
        float br = ballR();
        float bx = CX + (float) Math.cos(a) * (at == 0 ? 0 : br);
        float by = CY + (float) Math.sin(a) * (at == 0 ? 0 : br);
        fill.setColor(0xFFFFC24B);
        c.drawCircle(at == 0 ? CX : bx, at == 0 ? CY : by, 10, fill);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
