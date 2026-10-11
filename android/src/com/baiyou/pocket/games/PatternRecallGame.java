package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g043 位置回想：亮出的方格图案看 2 秒后复原；复原正确进入下一轮
 *  （格子更多），点错即结束。 */
public class PatternRecallGame extends CanvasGame {
    private static final int N = 5, CELL = 56, LEFT = 40, TOP = 130;
    private static final int SHOW_MS = 2000;

    private final boolean[][] pattern = new boolean[N][N];
    private final boolean[][] input = new boolean[N][N];
    private final Random rnd = new Random();
    private int round, litCount;
    private boolean showing;
    private long showUntil;
    private boolean settling;

    public PatternRecallGame() {
        super(360, 560);
        genRound();
    }

    private void genRound() {
        litCount = Math.min(N * N - 4, 2 + round);
        for (boolean[] row : pattern) java.util.Arrays.fill(row, false);
        for (boolean[] row : input) java.util.Arrays.fill(row, false);
        int placed = 0;
        while (placed < litCount) {
            int r = rnd.nextInt(N), c = rnd.nextInt(N);
            if (!pattern[r][c]) {
                pattern[r][c] = true;
                placed++;
            }
        }
        showing = true;
        showUntil = now() + SHOW_MS;
    }

    private void startGame() {
        round = 1;
        settling = false;
        over = false;
        genRound();
        host.setScore(0);
        startLoop(60);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(60);
    }

    private float cellX(int c) {
        return LEFT + c * CELL;
    }

    private float cellY(int r) {
        return TOP + r * CELL;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || showing || action != 0) return;
        int c = (int) ((x - LEFT) / CELL), r = (int) ((y - TOP) / CELL);
        if (r < 0 || r >= N || c < 0 || c >= N) return;
        if (pattern[r][c] && !input[r][c]) {
            input[r][c] = true;
            beep(1);
            burst(cellX(c) + CELL / 2f, cellY(r) + CELL / 2f, 0xFF7CFFB2, 6, 4f);
            if (allFound()) {
                settling = true;
                round++;
                shownScore += 100;
                host.setScore(shownScore);
                popup("第 " + (round - 1) + " 轮复原！", lw / 2f, 100, 0xFF7CFFB2, 18);
                beep(3);
                postDelayedSafe(() -> {
                    settling = false;
                    if (over) return;
                    genRound();
                    dirty();
                }, 900);
            }
        } else if (!input[r][c]) {
            over = true;
            stopLoop();
            beep(2);
            shake(240);
            popup("点错了！通过 " + (round - 1) + " 轮", lw / 2f, lh / 2f, 0xFFE8574F, 17);
            int finalScore = (round - 1) * 100;
            host.setScore(finalScore);
            postDelayedSafe(() -> host.gameOver(finalScore), 850);
        }
        dirty();
    }

    private boolean allFound() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (pattern[r][c] != input[r][c]) return false;
        return true;
    }

    private int shownScore;

    @Override
    protected void onTick() {
        if (showing && now() >= showUntil) showing = false;
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1A1E2A);
        label(c, showing ? "记住亮格位置…" : "第 " + round + " 轮 · 复原图案",
                lw / 2f, 90, 14, showing ? 0xFFFFC24B : 0xFFECECF1, true);
        for (int r = 0; r < N; r++)
            for (int col = 0; col < N; col++) {
                float l = cellX(col) + 3, t = cellY(r) + 3;
                boolean lit = showing ? pattern[r][col] : input[r][col];
                rr(c, l, t, l + CELL - 6, t + CELL - 6, 8, lit ? 0xFF7CFFB2 : 0xFF2A2E3C);
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
