package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g018 数字链消：6×6 数字盘，点两个相邻（上下左右）和为 10 的数字成对消除；
 *  无对可消或时间到结算，对数越多分越高。 */
public class NumberChainGame extends CanvasGame {
    private static final int N = 6, CELL = 48, LEFT = 36, TOP = 120;
    private static final long ROUND_MS = 90000;
    private static final int TARGET = 10;

    private final int[][] board = new int[N][N];
    private final boolean[][] gone = new boolean[N][N];
    private final Random rnd = new Random();
    private int shownScore, pairs;
    private int selR = -1, selC = -1;
    private long roundEnd;
    private boolean settling;

    public NumberChainGame() {
        super(360, 560);
        genBoard();
    }

    private void genBoard() {
        for (boolean[] row : gone) java.util.Arrays.fill(row, false);
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                board[r][c] = 1 + rnd.nextInt(9);
        // 保证至少一些可消对：随机补若干和为 10 的相邻对
        for (int k = 0; k < 6; k++) {
            int r = rnd.nextInt(N), c = rnd.nextInt(N);
            int v = board[r][c];
            int need = TARGET - v;
            if (need < 1 || need > 9) continue;
            int dr = rnd.nextBoolean() ? (r + 1 < N ? 1 : -1) : (r > 0 ? -1 : 1);
            board[r + dr][c] = need;
        }
    }

    private void startRound() {
        shownScore = 0;
        pairs = 0;
        roundEnd = now() + ROUND_MS;
        genBoard();
        selR = selC = -1;
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

    private boolean alive(int r, int c) {
        return !gone[r][c];
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        int c = (int) ((x - LEFT) / CELL), r = (int) ((y - TOP) / CELL);
        if (r < 0 || r >= N || c < 0 || c >= N || !alive(r, c)) return;
        if (selR < 0) {
            selR = r;
            selC = c;
            beep(0);
            dirty();
            return;
        }
        if (selR == r && selC == c) {
            selR = selC = -1;
            dirty();
            return;
        }
        boolean adjacent = Math.abs(selR - r) + Math.abs(selC - c) == 1;
        int sum = board[selR][selC] + board[r][c];
        if (adjacent && sum == TARGET) {
            gone[selR][selC] = true;
            gone[r][c] = true;
            pairs++;
            shownScore += 20;
            host.setScore(shownScore);
            burst(cellX(c) + CELL / 2f, cellY(r) + CELL / 2f, 0xFF7CFFB2, 10, 5f);
            popup("+20", cellX(c) + CELL / 2f, cellY(r) - 10, 0xFF7CFFB2, 16);
            beep(1);
            selR = selC = -1;
            if (!hasPair()) {
                settling = true;
                popup("无可消对 · 消除 " + pairs + " 对", lw / 2f, lh / 2f, 0xFFECECF1, 17);
                dirty();
                postDelayedSafe(this::finishNow, 900);
                return;
            }
        } else {
            selR = r;
            selC = c;
            beep(adjacent ? 2 : 0);
        }
        dirty();
    }

    private boolean hasPair() {
        int[] drs = {1, 0};
        int[] dcs = {0, 1};
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                if (!alive(r, c)) continue;
                for (int d = 0; d < 2; d++) {
                    int nr = r + drs[d], nc = c + dcs[d];
                    if (nr < N && nc < N && alive(nr, nc)
                            && board[r][c] + board[nr][nc] == TARGET) return true;
                }
            }
        return false;
    }

    private void finishNow() {
        over = true;
        stopLoop();
        beep(3);
        dirty();
        postDelayedSafe(() -> host.gameOver(shownScore), 400);
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到 · 消除 " + pairs + " 对", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1A2028);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        label(c, "相邻凑十，一对一对消 · 已消 " + pairs + " 对",
                lw - 30, 30, 12, 0xFF9AA0AC, true);
        for (int r = 0; r < N; r++)
            for (int col = 0; col < N; col++) {
                float l = cellX(col), t = cellY(r);
                if (!alive(r, col)) continue;
                boolean sel = r == selR && col == selC;
                rr(c, l + 2, t + 2, l + CELL - 2, t + CELL - 2, 8,
                        sel ? 0xFFFFC24B : 0xFF2E3648);
                label(c, String.valueOf(board[r][col]), l + CELL / 2f, t + CELL / 2f, 22,
                        sel ? 0xFF1B1C22 : 0xFFB9C4E0, true);
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
