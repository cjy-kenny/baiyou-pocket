package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

/** g024 手速连点：10 秒内疯狂点屏，点出多少下就是多少分；
 *  最后 3 秒画面变红催命。 */
public class TapRushGame extends CanvasGame {
    private static final long ROUND_MS = 10000;

    private int taps;
    private long roundEnd;
    private boolean settling;

    public TapRushGame() {
        super(360, 560);
    }

    private void startRound() {
        taps = 0;
        roundEnd = now() + ROUND_MS;
        settling = false;
        over = false;
        host.setScore(0);
        startLoop(60);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(60);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        taps++;
        host.setScore(taps);
        beep(0);
        burst(x, y, taps % 7 < 3 ? 0xFF7CFFB2 : (taps % 7 < 5 ? 0xFF5B8DEF : 0xFFFFC24B), 4, 3f);
        dirty();
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            settling = true;
            stopLoop();
            beep(3);
            popup("手速 " + taps * 6 + " 点/分", lw / 2f, lh / 2f, 0xFFFFC24B, 20);
            dirty();
            postDelayedSafe(() -> host.gameOver(taps), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        long remain = roundEnd - now();
        boolean panic = remain < 3000;
        rr(c, 0, 0, lw, lh, 0, panic ? 0xFF301318 : 0xFF1E222B);
        float r = 90 + (panic ? (float) Math.sin(now() / 80.0) * 10 : 0);
        label(c, String.valueOf(taps), lw / 2f, lh / 2f - 40, 110, 0xFFECECF1, true);
        label(c, "点它！点它！", lw / 2f, lh / 2f + 60, 26, panic ? 0xFFE8574F : 0xFF8B8E9C, true);
        // 倒计时环
        float k = Math.max(0f, remain / (float) ROUND_MS);
        stroke.setColor(panic ? 0xFFE8574F : 0xFF7B7FF5);
        stroke.setStrokeWidth(8);
        rf.set(lw / 2f - r, 70, lw / 2f + r, 70 + r * 2);
        c.drawArc(rf, -90, 360 * k, false, stroke);
        label(c, String.format("%.1f", remain / 1000.0) + "s", lw / 2f, 70 + r, 20,
                panic ? 0xFFE8574F : 0xFF9AA0AC, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
