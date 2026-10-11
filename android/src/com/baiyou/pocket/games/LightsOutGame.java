package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g010 点亮灯塔：5×5 点灯谜题，点一格翻转十字五格，全部点亮过关（90 秒多关卡）。
 *  谜面由"全亮状态随机按灯"生成，必有解。每关 +200 分。 */
public class LightsOutGame extends CanvasGame {
    private static final int N = 5, CELL = 54, LEFT = 45, TOP = 120;
    private static final long ROUND_MS = 90000;

    private final boolean[][] lit = new boolean[N][N];
    private final Random rnd = new Random();
    private int level, shownScore;
    private long roundEnd;
    private boolean settling;

    public LightsOutGame() {
        super(360, 560);
        genLevel();
    }

    private void genLevel() {
        for (boolean[] row : lit) java.util.Arrays.fill(row, true);
        int taps = 6 + Math.min(10, level * 2);
        for (int k = 0; k < taps; k++) toggle(rnd.nextInt(N), rnd.nextInt(N));
    }

    private void toggle(int r, int c) {
        lit[r][c] = !lit[r][c];
        if (r > 0) lit[r - 1][c] = !lit[r - 1][c];
        if (r < N - 1) lit[r + 1][c] = !lit[r + 1][c];
        if (c > 0) lit[r][c - 1] = !lit[r][c - 1];
        if (c < N - 1) lit[r][c + 1] = !lit[r][c + 1];
    }

    private void startRound() {
        level = 1;
        shownScore = 0;
        roundEnd = now() + ROUND_MS;
        genLevel();
        settling = false;
        over = false;
        host.setScore(0);
        startLoop(100);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(100);
    }

    private float cellX(int c) {
        return LEFT + c * CELL;
    }

    private float cellY(int r) {
        return TOP + r * CELL;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        int c = (int) ((x - LEFT) / CELL), r = (int) ((y - TOP) / CELL);
        if (r < 0 || r >= N || c < 0 || c >= N) return;
        toggle(r, c);
        beep(0);
        if (allLit()) {
            shownScore += 200;
            host.setScore(shownScore);
            level++;
            popup("第 " + (level - 1) + " 关点亮！", lw / 2f, lh / 2f - 30, 0xFFFFC24B, 18);
            burst(lw / 2f, lh / 2f, 0xFFFFC24B, 20, 7f);
            beep(3);
            genLevel();
        }
        dirty();
    }

    private boolean allLit() {
        for (boolean[] row : lit)
            for (boolean b : row) if (!b) return false;
        return true;
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到 · 点亮 " + (level - 1) + " 关", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF141821);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        label(c, "第 " + level + " 关 · 点一格翻十字", lw - 30, 30, 13, 0xFF9AA0AC, true);
        for (int r = 0; r < N; r++)
            for (int col = 0; col < N; col++) {
                float l = cellX(col) + 3, t = cellY(r) + 3;
                rr(c, l, t, l + CELL - 6, t + CELL - 6, 10, lit[r][col] ? 0xFFFFC24B : 0xFF2A2D37);
                if (lit[r][col]) {
                    // 灯塔光晕
                    fill.setColor(0x66FFFFFF);
                    c.drawCircle(l + (CELL - 6) / 2f, t + (CELL - 6) / 2f, 10, fill);
                }
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
