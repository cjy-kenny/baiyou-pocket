package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g074 桌球新手：拖母球瞄准击球，把彩球撞进四个袋口；
 *  进球 +100，母球落袋扣 50 并重置；限时 75 秒。 */
public class BilliardGame extends CanvasGame {
    private static final long ROUND_MS = 75000;
    private static final float L = 20, T = 110, R = 340, B = 530;

    private static class Ball {
        float x, y, vx, vy;
        int color;
        boolean sunk;

        Ball(float x, float y, int color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    private final Random rnd = new Random();
    private final List<Ball> balls = new ArrayList<>();
    private Ball cue;
    private int score, sunk;
    private long roundEnd;
    private boolean settling;
    private float aimX = -1, aimY = -1;

    public BilliardGame() {
        super(360, 560);
        rack();
    }

    private static final int[] BALL_COLORS = {
            0xFFE8574F, 0xFFF2A33C, 0xFF2FA36B, 0xFF4A90E2, 0xFF9B59F6, 0xFFE8848C};

    private void rack() {
        balls.clear();
        cue = new Ball(90, (T + B) / 2f, 0xFFF4F5F9);
        int n = 0;
        for (int col = 0; col < 3; col++)
            for (int row = 0; row <= col; row++) {
                if (n >= 6) break;
                balls.add(new Ball(240 + col * 26, (T + B) / 2f - col * 13 + row * 26,
                        BALL_COLORS[n]));
                n++;
            }
    }

    private void startRound() {
        rack();
        score = 0;
        sunk = 0;
        roundEnd = now() + ROUND_MS;
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
        if (action == 0 || action == 2) {
            if (hit(x, y, 0, 0, lw, lh)) {
                aimX = x;
                aimY = y;
            }
        } else if (action == 1) {
            if (aimX >= 0 && !moving()) {
                float dx = cue.x - aimX, dy = cue.y - aimY;
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len > 18) {
                    float p = Math.min(11f, len * 0.09f);
                    cue.vx = dx / len * p;
                    cue.vy = dy / len * p;
                    beep(0);
                }
            }
            aimX = aimY = -1;
        }
        dirty();
    }

    private boolean moving() {
        for (Ball b : balls) {
            if (!b.sunk && (Math.abs(b.vx) > 0.02f || Math.abs(b.vy) > 0.02f)) return true;
        }
        return Math.abs(cue.vx) > 0.02f || Math.abs(cue.vy) > 0.02f;
    }

    private void pocket(Ball b) {
        b.sunk = true;
        if (b == cue) {
            score = Math.max(0, score - 50);
            cue.x = 90;
            cue.y = (T + B) / 2f;
            cue.vx = cue.vy = 0;
            beep(2);
            popup("母球落袋 -50", lw / 2f, T + 40, 0xFFE8574F, 15);
        } else {
            score += 100;
            sunk++;
            beep(1);
            popup("+100", b.x, b.y - 20, 0xFF7CFFB2, 16);
        }
        host.setScore(score);
    }

    @Override
    protected void onTick() {
        // 物理
        List<Ball> live = new ArrayList<>();
        if (!cue.sunk) live.add(cue);
        for (Ball b : balls) if (!b.sunk) live.add(b);
        for (Ball b : live) {
            b.x += b.vx;
            b.y += b.vy;
            b.vx *= 0.985f;
            b.vy *= 0.985f;
            if (Math.abs(b.vx) < 0.02f) b.vx = 0;
            if (Math.abs(b.vy) < 0.02f) b.vy = 0;
            // 库边
            if (b.x < L + 10) {
                b.x = L + 10;
                b.vx = Math.abs(b.vx) * 0.8f;
                beep(0);
            }
            if (b.x > R - 10) {
                b.x = R - 10;
                b.vx = -Math.abs(b.vx) * 0.8f;
                beep(0);
            }
            if (b.y < T + 10) {
                b.y = T + 10;
                b.vy = Math.abs(b.vy) * 0.8f;
                beep(0);
            }
            if (b.y > B - 10) {
                b.y = B - 10;
                b.vy = -Math.abs(b.vy) * 0.8f;
                beep(0);
            }
            // 袋口（四角）
            float[][] pockets = {{L, T}, {R, T}, {L, B}, {R, B}};
            for (float[] pk : pockets) {
                float dx = b.x - pk[0], dy = b.y - pk[1];
                if (dx * dx + dy * dy < 20 * 20) {
                    pocket(b);
                    break;
                }
            }
        }
        // 球球碰撞
        for (int i = 0; i < live.size(); i++)
            for (int j = i + 1; j < live.size(); j++) {
                Ball a = live.get(i), b = live.get(j);
                float dx = b.x - a.x, dy = b.y - a.y;
                float d2 = dx * dx + dy * dy;
                if (d2 < 20 * 20 && d2 > 0.001f) {
                    float d = (float) Math.sqrt(d2);
                    float nx = dx / d, ny = dy / d;
                    float rel = (a.vx - b.vx) * nx + (a.vy - b.vy) * ny;
                    if (rel > 0) {
                        a.vx -= nx * rel;
                        a.vy -= ny * rel;
                        b.vx += nx * rel;
                        b.vy += ny * rel;
                        beep(0);
                    }
                    float overlap = 20 - d;
                    a.x -= nx * overlap / 2f;
                    a.y -= ny * overlap / 2f;
                    b.x += nx * overlap / 2f;
                    b.y += ny * overlap / 2f;
                }
            }
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到 · 进 " + sunk + " 球", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(score), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF3A2A20);
        rr(c, L, T, R, B, 6, 0xFF1E5C38);
        // 袋口
        fill.setColor(0xFF101010);
        c.drawCircle(L, T, 14, fill);
        c.drawCircle(R, T, 14, fill);
        c.drawCircle(L, B, 14, fill);
        c.drawCircle(R, B, 14, fill);
        // 球
        if (!cue.sunk) {
            fill.setColor(0xFFF4F5F9);
            c.drawCircle(cue.x, cue.y, 10, fill);
        }
        for (Ball b : balls) {
            if (b.sunk) continue;
            fill.setColor(b.color);
            c.drawCircle(b.x, b.y, 10, fill);
            fill.setColor(0x66FFFFFF);
            c.drawCircle(b.x - 3, b.y - 3, 3, fill);
        }
        // 瞄准线
        if (aimX >= 0 && !cue.sunk) {
            stroke.setColor(0x88FFFFFF);
            stroke.setStrokeWidth(2);
            c.drawLine(cue.x, cue.y, cue.x * 2 - aimX, cue.y * 2 - aimY, stroke);
        }
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        label(c, "进 " + sunk + " 球", 40, 30, 14, 0xFFECECF1, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
