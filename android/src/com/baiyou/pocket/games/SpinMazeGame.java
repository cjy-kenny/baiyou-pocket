package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g068 转盘迷宫：三重环各带缺口，左右两键旋转对应环，
 *  缺口连成一条通路时点中心小球冲出去；90 秒多关。 */
public class SpinMazeGame extends CanvasGame {
    private static final int RINGS = 3;
    private static final float CX = 180, CY = 310;
    private static final float[] RADII = {58, 104, 150};
    private static final float GAP = 56;

    private final Random rnd = new Random();
    private final float[] angle = new float[RINGS];  // 缺口中心角
    private int level, shownScore;
    private long roundEnd;
    private boolean settling;

    public SpinMazeGame() {
        super(360, 560);
        genLevel();
    }

    private void genLevel() {
        for (int i = 0; i < RINGS; i++) angle[i] = rand(0, 360);
    }

    private void startRound() {
        level = 1;
        shownScore = 0;
        roundEnd = now() + 90000;
        genLevel();
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

    /** 三环缺口是否都大致朝上（-90°±24°）。 */
    private boolean aligned() {
        for (int i = 0; i < RINGS; i++) {
            float diff = Math.abs(((angle[i] + 90) % 360 + 360) % 360);
            if (diff > 180) diff = 360 - diff;
            if (diff > 24) return false;
        }
        return true;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        // 左下/右下按钮旋转内外环
        if (hit(x, y, 30, 480, 130, 56)) {
            angle[0] = (angle[0] + 12) % 360;
            angle[1] = (angle[1] - 8) % 360;
            beep(0);
            dirty();
            return;
        }
        if (hit(x, y, 200, 480, 130, 56)) {
            angle[1] = (angle[1] + 12) % 360;
            angle[2] = (angle[2] - 9) % 360;
            beep(0);
            dirty();
            return;
        }
        // 点中心发射
        float dx = x - CX, dy = y - CY;
        if (dx * dx + dy * dy < 40 * 40 && aligned()) {
            shownScore += 250;
            host.setScore(shownScore);
            level++;
            popup("冲出去了！ +250", CX, CY - 40, 0xFF7CFFB2, 18);
            burst(CX, CY, 0xFF7CFFB2, 18, 7f);
            beep(3);
            genLevel();
            dirty();
        }
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到 · 逃出 " + (level - 1) + " 次", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF10141E);
        float remain = Math.max(0f, (roundEnd - now()) / 90000f);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        label(c, "第 " + level + " 迷宫", 40, 30, 14, 0xFF9AA0AC, true);
        boolean ok = aligned();
        for (int i = 0; i < RINGS; i++) {
            stroke.setColor(ok ? 0xFF7CFFB2 : 0xFF4A5568);
            stroke.setStrokeWidth(9);
            rf.set(CX - RADII[i], CY - RADII[i], CX + RADII[i], CY + RADII[i]);
            float gap0 = angle[i] - 90 - GAP / 2f;
            c.drawArc(rf, gap0 + GAP, 360 - GAP, false, stroke);
        }
        // 中心球
        fill.setColor(ok ? 0xFFFFC24B : 0xFF6B7280);
        c.drawCircle(CX, CY, 20, fill);
        fill.setColor(0x66FFFFFF);
        c.drawCircle(CX - 6, CY - 6, 6, fill);
        // 按钮
        rr(c, 30, 480, 160, 536, 10, 0xFF2A2D37);
        label(c, "转 内环", 95, 508, 17, 0xFFECECF1, true);
        rr(c, 200, 480, 330, 536, 10, 0xFF2A2D37);
        label(c, "转 外环", 265, 508, 17, 0xFFECECF1, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
