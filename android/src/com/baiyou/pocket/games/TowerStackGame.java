package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g065 吊塔堆塔：吊臂左右摆动，点屏放下方块；偏得太多整块倾塌。
 *  叠得越高分越高。 */
public class TowerStackGame extends CanvasGame {
    private static final float TOP_Y = 120, GROUND = 500;

    private final Random rnd = new Random();
    private float blockW = 90;
    private float swing;          // 吊臂摆角（-1..1）
    private float swingV = 0.02f;
    private final float[] heights = new float[64];   // 已叠每层中心 x
    private final float[] widths = new float[64];
    private int stacked;
    private float dropX, dropY, dropVx, dropVy;
    private boolean dropping;
    private int score;
    private boolean settling;

    public TowerStackGame() {
        super(360, 560);
    }

    private void startRound() {
        stacked = 0;
        heights[0] = lw / 2f;
        widths[0] = 110;
        stacked = 1;
        blockW = 100;
        swing = 0;
        swingV = 0.02f;
        dropping = false;
        settling = false;
        over = false;
        score = 0;
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

    private float layerY(int i) {
        return GROUND - 24 - i * 24;
    }

    private float swingX() {
        return lw / 2f + swing * 130;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || dropping || action != 0) return;
        dropping = true;
        dropX = swingX();
        dropY = TOP_Y;
        dropVx = swingV * 130 * 0.5f;
        dropVy = 0;
        beep(0);
        dirty();
    }

    private void topple() {
        settling = true;
        over = true;
        stopLoop();
        shake(320);
        burst(dropX, dropY, 0xFFE8574F, 18, 7f);
        beep(2);
        popup("塌了！叠到 " + (stacked - 1) + " 层", lw / 2f, lh / 2f, 0xFFE8574F, 18);
        dirty();
        postDelayedSafe(() -> host.gameOver((stacked - 1) * 100), 850);
    }

    @Override
    protected void onTick() {
        if (!dropping) {
            swing += swingV;
            if (swing > 1) {
                swing = 1;
                swingV = -Math.abs(swingV);
            }
            if (swing < -1) {
                swing = -1;
                swingV = Math.abs(swingV);
            }
        } else {
            dropVy += 0.3f;
            dropX += dropVx;
            dropY += dropVy;
            float targetY = layerY(stacked) + 12;
            if (dropY >= targetY) {
                dropY = targetY;
                // 与下层对齐检查
                float prevX = heights[stacked - 1];
                float prevW = widths[stacked - 1];
                float off = Math.abs(dropX - prevX);
                if (off > prevW / 2f + blockW / 2f - 8) {
                    topple();
                    return;
                }
                // 有效宽度收缩
                float remain = Math.min(prevW / 2f + blockW / 2f - off, blockW);
                blockW = Math.max(24, Math.min(blockW, remain * 1.6f));
                heights[stacked] = dropX;
                widths[stacked] = remain * 1.6f;
                stacked++;
                score += 100;
                host.setScore(score);
                beep(1);
                burst(dropX, targetY, 0xFF7CFFB2, 8, 4f);
                dropping = false;
                swingV = 0.02f + stacked * 0.0015f;
            }
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1A2028);
        rr(c, 0, GROUND, lw, lh - GROUND, 0, 0xFF2A3240);
        // 已叠层
        for (int i = 0; i < stacked; i++) {
            float y = layerY(i);
            int color = i % 2 == 0 ? 0xFF5B8DEF : 0xFF7B7FF5;
            rr(c, heights[i] - widths[i] / 2f, y - 12, heights[i] + widths[i] / 2f, y + 12,
                    4, color);
        }
        // 吊臂
        float sx = swingX();
        stroke.setColor(0xFF8B93A7);
        stroke.setStrokeWidth(3);
        c.drawLine(lw / 2f, 40, sx, TOP_Y - 20, stroke);
        if (!dropping) {
            rr(c, sx - blockW / 2f, TOP_Y - 20, sx + blockW / 2f, TOP_Y, 4, 0xFFFFC24B);
            stroke.setColor(0xFFFFC24B);
            c.drawLine(sx, TOP_Y, sx, TOP_Y - 20, stroke);
        } else {
            rr(c, dropX - blockW / 2f, dropY - 12, dropX + blockW / 2f, dropY + 12,
                    4, 0xFFFFC24B);
        }
        label(c, "叠了 " + (stacked - 1) + " 层", 44, 30, 15, 0xFF9AA0AC, true);
        label(c, "点屏放下", lw / 2f, 70, 13, 0xFF8B93A7, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
