package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g006 滑块拼图：3×3 数字滑块，点空格旁的块滑动，复原 1-8 即胜。
 *  打乱方式为随机合法移动，天然可解。步数越少、用时越短分越高。 */
public class SlidePuzzleGame extends CanvasGame {
    private static final int N = 3, CELL = 86, LEFT = 51, TOP = 120;

    private final int[] board = new int[N * N]; // 0 = 空格
    private final Random rnd = new Random();
    private int moves;
    private long startAt;
    private boolean settling;

    public SlidePuzzleGame() {
        super(360, 560);
        shuffle();
    }

    private void shuffle() {
        for (int i = 0; i < N * N; i++) board[i] = (i + 1) % (N * N);
        int blank = N * N - 1;
        for (int k = 0; k < 200; k++) {
            int d = rnd.nextInt(4);
            int dr = d == 0 ? -1 : d == 1 ? 1 : 0;
            int dc = d == 2 ? -1 : d == 3 ? 1 : 0;
            int r = blank / N + dr, c = blank % N + dc;
            if (r < 0 || r >= N || c < 0 || c >= N) continue;
            int nb = r * N + c;
            board[blank] = board[nb];
            board[nb] = 0;
            blank = nb;
        }
    }

    private void startRound() {
        shuffle();
        moves = 0;
        startAt = now();
        settling = false;
        over = false;
        host.setScore(0);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
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
        int blank = -1;
        for (int i = 0; i < N * N; i++) if (board[i] == 0) blank = i;
        int br = blank / N, bc = blank % N;
        if (Math.abs(br - r) + Math.abs(bc - c) != 1) return;
        board[blank] = board[r * N + c];
        board[r * N + c] = 0;
        moves++;
        beep(0);
        if (solved()) {
            settling = true;
            int sec = (int) ((now() - startAt) / 1000);
            int finalScore = Math.max(100, 1500 - moves * 15 - sec * 4);
            host.setScore(finalScore);
            popup("复原！ " + finalScore + " 分", lw / 2f, lh / 2f, 0xFFFFC24B, 22);
            beep(3);
            dirty();
            postDelayedSafe(() -> host.gameOver(finalScore), 850);
            return;
        }
        dirty();
    }

    private boolean solved() {
        for (int i = 0; i < N * N; i++) if (board[i] != (i + 1) % (N * N)) return false;
        return true;
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF20242E);
        label(c, "步数 " + moves + " · 点空格旁的数字块滑动", lw / 2f, 70, 14, 0xFF8B8E9C, false);
        rr(c, LEFT - 8, TOP - 8, LEFT + N * CELL + 8, TOP + N * CELL + 8, 12, 0xFF2B2F3A);
        for (int i = 0; i < N * N; i++) {
            int v = board[i];
            if (v == 0) continue;
            int r = i / N, col = i % N;
            int color = 0xFF000000 | ((0x50 + v * 20) << 16) | ((0x80 + v * 8) << 8) | (0xFF - v * 6);
            rr(c, cellX(col) + 3, cellY(r) + 3, cellX(col) + CELL - 3, cellY(r) + CELL - 3,
                    10, color);
            label(c, String.valueOf(v), cellX(col) + CELL / 2f, cellY(r) + CELL / 2f, 34,
                    0xFFFFFFFF, true);
        }
    }
}
