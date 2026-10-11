package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;
/** g023 递增序列：盘中数字点出一条严格递增的链（点过的数字消失），
 *  链断扣命；盘面无更大数字自动换盘，链越长加分越多。45 秒。 */
public class RisingChainGame extends CanvasGame {
    private static final int COLS = 5, ROWS = 4, CELL = 60, LEFT = 30, TOP = 130;
    private static final long ROUND_MS = 45000;

    private final int[][] board = new int[ROWS][COLS];
    private final boolean[][] used = new boolean[ROWS][COLS];
    private final Random rnd = new Random();
    private int last = 0, chain, lives = 3, shownScore;
    private long roundEnd;
    private boolean settling;

    public RisingChainGame() {
        super(360, 560);
        genBoard();
    }

    private void genBoard() {
        int n = 0;
        int[] pool = new int[ROWS * COLS];
        while (n < ROWS * COLS) {
            int v = 1 + rnd.nextInt(99);
            boolean dup = false;
            for (int i = 0; i < n; i++) if (pool[i] == v) dup = true;
            if (!dup) pool[n++] = v;
        }
        for (int i = pool.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = pool[i];
            pool[i] = pool[j];
            pool[j] = t;
        }
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++) {
                board[r][c] = pool[r * COLS + c];
                used[r][c] = false;
            }
        last = 0;
        chain = 0;
    }

    private void startGame() {
        lives = 3;
        shownScore = 0;
        roundEnd = now() + ROUND_MS;
        genBoard();
        settling = false;
        over = false;
        host.setScore(0);
        startLoop(100);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
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
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS || used[r][c]) return;
        int v = board[r][c];
        if (v > last) {
            last = v;
            chain++;
            used[r][c] = true;
            shownScore += 5 * chain;
            host.setScore(shownScore);
            beep(1);
            popup("×" + chain, cellX(c) + CELL / 2f, cellY(r) - 8, 0xFF7CFFB2, 14);
            burst(cellX(c) + CELL / 2f, cellY(r) + CELL / 2f, 0xFF7CFFB2, 6, 4f);
            if (!hasBigger()) {
                int bonus = chain >= 8 ? 200 : 50;
                shownScore += bonus;
                host.setScore(shownScore);
                popup("换盘 +" + bonus, lw / 2f, lh / 2f - 40, 0xFFFFC24B, 16);
                genBoard();
            }
        } else {
            lives--;
            chain = 0;
            last = 0;
            beep(2);
            shake(180);
            popup("要更大的数！", lw / 2f, lh / 2f - 40, 0xFFE8574F, 15);
            if (lives <= 0) {
                finishNow();
                return;
            }
        }
        dirty();
    }

    private boolean hasBigger() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                if (!used[r][c] && board[r][c] > last) return true;
        return false;
    }

    private void finishNow() {
        settling = true;
        over = true;
        stopLoop();
        popup("链条总长 " + chain, lw / 2f, lh / 2f, 0xFFECECF1, 17);
        dirty();
        postDelayedSafe(() -> host.gameOver(shownScore), 850);
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF20242E);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, "链 ×" + chain, 40, 30, 14, 0xFF7CFFB2, true);
        for (int r = 0; r < ROWS; r++)
            for (int col = 0; col < COLS; col++) {
                float l = cellX(col), t = cellY(r);
                if (used[r][col]) {
                    rr(c, l + 3, t + 3, l + CELL - 3, t + CELL - 3, 8, 0xFF23272F);
                    continue;
                }
                boolean ok = board[r][col] > last;
                rr(c, l + 3, t + 3, l + CELL - 3, t + CELL - 3, 8, ok ? 0xFF2E3E52 : 0xFF3A2A32);
                label(c, String.valueOf(board[r][col]), l + CELL / 2f, t + CELL / 2f, 21,
                        ok ? 0xFFB9E0C4 : 0xFFB0848C, true);
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
