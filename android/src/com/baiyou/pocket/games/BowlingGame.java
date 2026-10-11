package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g080 保龄球道：上滑掷球（快慢=力度，斜偏=弧线），撞倒瓶阵；
 *  五格计分，全中有奖。 */
public class BowlingGame extends CanvasGame {
    private static final float LANE_TOP = 120;

    private final Random rnd = new Random();
    private final float[][] pins = new float[10][2]; // 剩余瓶位置
    private final boolean[] pinUp = new boolean[10];
    private float bx = 180, by = 470, bvx = 0, bvy = 0;
    private boolean rolling;
    private int frame, shownScore, knocked;
    private float downX = -1, downY = -1;
    private long downAt;
    private boolean settling;

    public BowlingGame() {
        super(360, 560);
        rack();
    }

    private void rack() {
        int i = 0;
        for (int row = 0; row < 4; row++)
            for (int c = 0; c <= row; c++) {
                pins[i][0] = 180 - row * 17 + c * 34;
                pins[i][1] = LANE_TOP + 60 + row * 40;
                pinUp[i] = true;
                i++;
            }
    }

    private void startRound() {
        frame = 1;
        shownScore = 0;
        knocked = 0;
        rack();
        resetBall();
        settling = false;
        over = false;
        host.setScore(0);
        startLoop(16);
        dirty();
    }

    private void resetBall() {
        bx = 180;
        by = 470;
        bvx = bvy = 0;
        rolling = false;
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
        if (over || settling || rolling) return;
        if (action == 0) {
            downX = x;
            downY = y;
            downAt = now();
        } else if (action == 1 && downX >= 0) {
            float dy = downY - y;
            float dx = x - downX;
            long dt = Math.max(80, now() - downAt);
            if (dy > 40) {
                float power = Math.min(13f, dy / (float) dt * 260f);
                bvy = -power;
                bvx = Math.max(-4f, Math.min(4f, dx * 0.05f));
                bx = Math.max(30, Math.min(lw - 30, downX));
                rolling = true;
                beep(0);
            }
            downX = downY = -1;
        }
        dirty();
    }

    private void settle() {
        settling = true;
        int down = 0;
        for (boolean b : pinUp) if (!b) down++;
        int thisRound = down - knocked;
        knocked = down;
        boolean strike = thisRound == 10;
        shownScore += thisRound * 10 + (strike ? 100 : 0);
        host.setScore(shownScore);
        if (strike) {
            popup("全中！ +100", lw / 2f, 240, 0xFFFFC24B, 22);
            burst(180, LANE_TOP + 130, 0xFFFFC24B, 20, 8f);
            beep(3);
        } else {
            beep(1);
            popup("倒 " + thisRound + " 瓶", lw / 2f, 240, 0xFFECECF1, 17);
        }
        dirty();
        postDelayedSafe(() -> {
            settling = false;
            if (frame >= 5) {
                over = true;
                stopLoop();
                popup("五格 " + knocked + " 瓶", lw / 2f, lh / 2f, 0xFFECECF1, 18);
                dirty();
                postDelayedSafe(() -> host.gameOver(shownScore), 850);
                return;
            }
            frame++;
            knocked = 0;
            rack();
            resetBall();
            dirty();
        }, 1000);
    }

    @Override
    protected void onTick() {
        if (rolling) {
            bx += bvx;
            by += bvy;
            bvy += 0.02f; // 微加速
            if (bx < 40 || bx > lw - 40) {
                bvx = -bvx * 0.7f;
                bx = Math.max(40, Math.min(lw - 40, bx));
                beep(0);
            }
            // 撞瓶
            boolean hitAny = false;
            for (int i = 0; i < 10; i++) {
                if (!pinUp[i]) continue;
                float dx = bx - pins[i][0], dy = by - pins[i][1];
                if (dx * dx + dy * dy < 24 * 24) {
                    pinUp[i] = false;
                    hitAny = true;
                    burst(pins[i][0], pins[i][1], 0xFFF4F5F9, 8, 5f);
                }
            }
            if (hitAny) beep(1);
            if (by < LANE_TOP + 10) {
                rolling = false;
                settle();
                return;
            }
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF26180F);
        // 球道
        rr(c, 36, LANE_TOP, lw - 36, 500, 6, 0xFFB4763C);
        stroke.setColor(0x663A2410);
        stroke.setStrokeWidth(2);
        c.drawLine(60, LANE_TOP, 60, 500, stroke);
        c.drawLine(lw - 60, LANE_TOP, lw - 60, 500, stroke);
        // 瓶
        for (int i = 0; i < 10; i++) {
            if (!pinUp[i]) continue;
            fill.setColor(0xFFF4F5F9);
            c.drawCircle(pins[i][0], pins[i][1], 11, fill);
            stroke.setColor(0xFFE8574F);
            stroke.setStrokeWidth(2);
            c.drawLine(pins[i][0] - 8, pins[i][1] + 4, pins[i][0] + 8, pins[i][1] + 4, stroke);
        }
        // 球
        fill.setColor(0xFF5B3A8F);
        c.drawCircle(bx, by, 15, fill);
        fill.setColor(0x66FFFFFF);
        c.drawCircle(bx - 5, by - 5, 4, fill);
        label(c, "第 " + frame + "/5 格 · 倒 " + knocked + " 瓶", lw / 2f, 80, 14, 0xFFE8C4A0, true);
        label(c, "上滑掷球", lw / 2f, 530, 13, 0xFFC9B896, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
