package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g076 羽毛球挑高：判断来球落点，球进击球圈时点屏回击；
 *  连续回击越多分越高，漏球即结束。 */
public class BadmintonGame extends CanvasGame {
    private static final float HIT_Y = 430;
    private static final float RALLY_Y = 80;

    private final Random rnd = new Random();
    private float sx = 180, sy = RALLY_Y, svx = 2f, svy = 3f;
    private boolean incoming;
    private int score, streak;
    private boolean dying;

    public BadmintonGame() {
        super(360, 560);
        serveShuttle();
    }

    private void serveShuttle() {
        sx = rand(60, lw - 60);
        sy = RALLY_Y;
        svx = (180 - sx) / 60f + rand(-0.6f, 0.6f);
        svy = (HIT_Y - RALLY_Y) / 60f;
        incoming = true;
    }

    private void startRound() {
        score = 0;
        streak = 0;
        serveShuttle();
        dying = false;
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
        if (over || dying || !incoming || action != 0) return;
        if (sy > HIT_Y - 70) {
            // 回击
            float dx = Math.abs(x - sx);
            if (dx < 90) {
                streak++;
                score += 20 * Math.min(5, streak);
                host.setScore(score);
                beep(1);
                burst(sx, HIT_Y, 0xFF7CFFB2, 10, 5f);
                popup("× " + streak, sx, HIT_Y - 30, 0xFF7CFFB2, 16);
                serveShuttle();
            } else {
                miss();
            }
        }
        dirty();
    }

    private void miss() {
        dying = true;
        over = true;
        stopLoop();
        beep(2);
        shake(240);
        popup("漏球了 · 连击 " + streak, lw / 2f, lh / 2f, 0xFFE8574F, 18);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 800);
    }

    @Override
    protected void onTick() {
        if (incoming) {
            sx += svx;
            sy += svy;
            if (sx < 20 || sx > lw - 20) svx = -svx;
            if (sy > HIT_Y + 40) {
                miss();
                return;
            }
        } else {
            // 回击飞回画面上方的过程（视觉过渡）
            sy -= 12;
            if (sy < RALLY_Y) serveShuttle();
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF204020);
        rr(c, 0, 470, lw, lh - 470, 0, 0xFF2FA36B);
        // 网
        stroke.setColor(0xFFECECF1);
        stroke.setStrokeWidth(3);
        c.drawLine(0, 240, lw, 240, stroke);
        // 击球圈
        stroke.setColor(0x88FFC24B);
        stroke.setStrokeWidth(3);
        rf.set(lw / 2f - 110, HIT_Y - 70, lw / 2f + 110, HIT_Y + 20);
        c.drawRoundRect(rf, 20, 20, stroke);
        // 球拍
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(180, HIT_Y + 6, 22, fill);
        // 羽毛球
        if (incoming) {
            fill.setColor(0xFFF4F5F9);
            c.drawCircle(sx, sy, 8, fill);
            stroke.setColor(0xFFECECF1);
            stroke.setStrokeWidth(2);
            c.drawLine(sx, sy, sx - svx * 3, sy - svy * 3, stroke);
        }
        label(c, streak > 0 ? "连击 ×" + streak : "球进框时点屏",
                lw / 2f, 40, 15, 0xFFECECF1, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
