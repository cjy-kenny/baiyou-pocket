package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g017 数独小栈：4×4 迷你数独（2×2 宫），点格子循环填 1-4，
 *  行/列/宫不重复即完成。90 秒内多解几盘，每盘 +300。 */
public class SudokuMiniGame extends CanvasGame {
    private static final int N = 4, CELL = 62, LEFT = 52, TOP = 130;
    private static final long ROUND_MS = 90000;

    private final int[][] sol = new int[N][N];   // 解
    private final int[][] board = new int[N][N]; // 当前（0=空）
    private final boolean[][] given = new boolean[N][N];
    private final Random rnd = new Random();
    private int shownScore, solvedCount;
    private long roundEnd;
    private int selR = -1, selC = -1;
    private boolean settling;

    public SudokuMiniGame() {
        super(360, 560);
        genPuzzle();
    }

    private boolean canPlace(int r, int c, int v) {
        for (int i = 0; i < N; i++) {
            if (sol[r][i] == v || sol[i][c] == v) return false;
        }
        int br = r / 2 * 2, bc = c / 2 * 2;
        for (int i = br; i < br + 2; i++)
            for (int j = bc; j < bc + 2; j++)
                if (sol[i][j] == v) return false;
        return true;
    }

    private boolean genSol(int pos) {
        if (pos == N * N) return true;
        int r = pos / N, c = pos % N;
        int start = 1 + rnd.nextInt(N);
        for (int k = 0; k < N; k++) {
            int v = (start + k - 1) % N + 1;
            if (canPlace(r, c, v)) {
                sol[r][c] = v;
                if (genSol(pos + 1)) return true;
                sol[r][c] = 0;
            }
        }
        return false;
    }

    private void genPuzzle() {
        for (int[] row : sol) java.util.Arrays.fill(row, 0);
        genSol(0);
        for (int[] row : board) java.util.Arrays.fill(row, 0);
        for (boolean[] row : given) java.util.Arrays.fill(row, false);
        int holes = 0;
        while (holes < 8) {
            int r = rnd.nextInt(N), c = rnd.nextInt(N);
            if (given[r][c]) continue;
            given[r][c] = true;
            board[r][c] = sol[r][c];
            holes++;
        }
    }

    private void startRound() {
        shownScore = 0;
        solvedCount = 0;
        roundEnd = now() + ROUND_MS;
        genPuzzle();
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
        if (r < 0 || r >= N || c < 0 || c >= N || given[r][c]) return;
        board[r][c] = board[r][c] % N + 1;
        selR = r;
        selC = c;
        beep(0);
        if (board[r][c] != sol[r][c]) beep(2);
        if (solvedBoard()) {
            shownScore += 300;
            host.setScore(shownScore);
            solvedCount++;
            popup("完成！ +300", lw / 2f, lh / 2f - 30, 0xFF7CFFB2, 19);
            burst(lw / 2f, lh / 2f, 0xFF7CFFB2, 18, 7f);
            beep(3);
            genPuzzle();
        }
        dirty();
    }

    private boolean solvedBoard() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (board[r][c] != sol[r][c]) return false;
        return true;
    }

    private boolean conflict(int r, int c) {
        int v = board[r][c];
        if (v == 0) return false;
        for (int i = 0; i < N; i++) {
            if (i != c && board[r][i] == v) return true;
            if (i != r && board[i][c] == v) return true;
        }
        int br = r / 2 * 2, bc = c / 2 * 2;
        for (int i = br; i < br + 2; i++)
            for (int j = bc; j < bc + 2; j++)
                if ((i != r || j != c) && board[i][j] == v) return true;
        return false;
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到 · 完成 " + solvedCount + " 盘", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1E2430);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        label(c, "已完成 " + solvedCount + " 盘 · 点格填到不重复", lw - 30, 30, 12, 0xFF9AA0AC, true);
        for (int r = 0; r < N; r++)
            for (int col = 0; col < N; col++) {
                float l = cellX(col), t = cellY(r);
                rr(c, l + 1, t + 1, l + CELL - 1, t + CELL - 1, 4,
                        given[r][col] ? 0xFF2E3648 : 0xFF232B3A);
                int v = board[r][col];
                if (v > 0) {
                    boolean bad = conflict(r, col);
                    label(c, String.valueOf(v), l + CELL / 2f, t + CELL / 2f, 26,
                            bad ? 0xFFE8574F : (given[r][col] ? 0xFFB9C4E0 : 0xFF7CFFB2), true);
                }
                if (r == selR && col == selC) {
                    stroke.setColor(0xFFFFC24B);
                    stroke.setStrokeWidth(2);
                    rf.set(l + 2, t + 2, l + CELL - 2, t + CELL - 2);
                    c.drawRoundRect(rf, 4, 4, stroke);
                }
            }
        // 宫线
        stroke.setColor(0xFF566079);
        stroke.setStrokeWidth(2);
        c.drawLine(cellX(2) - 2, cellY(0), cellX(2) - 2, cellY(N), stroke);
        c.drawLine(cellX(0), cellY(2) - 2, cellX(N), cellY(2) - 2, stroke);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
