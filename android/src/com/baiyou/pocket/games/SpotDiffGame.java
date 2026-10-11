package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g050 细节侦探：左右两幅符号图只有一处不同，点右边不同的那格；
 *  限时 45 秒找得越多分越高。 */
public class SpotDiffGame extends CanvasGame {
    private static final int N = 4, CELL = 56, LEFT_L = 34, LEFT_R = 190, TOP = 130;
    private static final String GLYPHS = "●■▲◆★✚";
    private static final long ROUND_MS = 45000;

    private final int[][] left = new int[N][N];
    private final Random rnd = new Random();
    private int diffR, diffC, solved;
    private long roundEnd;
    private boolean settling;

    public SpotDiffGame() {
        super(360, 560);
        genPuzzle();
    }

    private void genPuzzle() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                left[r][c] = rnd.nextInt(GLYPHS.length());
        diffR = rnd.nextInt(N);
        diffC = rnd.nextInt(N);
        int other;
        do {
            other = rnd.nextInt(GLYPHS.length());
        } while (other == left[diffR][diffC]);
        // 右图 = 左图仅在 (diffR,diffC) 不同
    }

    private int rightAt(int r, int c) {
        if (r == diffR && c == diffC) {
            int other;
            do {
                other = rnd.nextInt(GLYPHS.length());
            } while (other == left[r][c]);
            return other;
        }
        return left[r][c];
    }

    private void startRound() {
        shownScore = 0;
        solved = 0;
        roundEnd = now() + ROUND_MS;
        genPuzzle();
        settling = false;
        over = false;
        host.setScore(0);
        startLoop(60);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(60);
    }

    private int shownScore;

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        int c = (int) ((x - LEFT_R) / CELL), r = (int) ((y - TOP) / CELL);
        if (r < 0 || r >= N || c < 0 || c >= N) return;
        if (r == diffR && c == diffC) {
            solved++;
            shownScore += 100;
            host.setScore(shownScore);
            beep(1);
            burst(LEFT_R + c * CELL + CELL / 2f, TOP + r * CELL + CELL / 2f, 0xFF7CFFB2, 10, 5f);
            genPuzzle();
        } else {
            beep(2);
            shake(160);
        }
        dirty();
    }

    private void drawBoard(Canvas c, float gx, boolean isRight) {
        for (int r = 0; r < N; r++)
            for (int col = 0; col < N; col++) {
                float l = gx + col * CELL, t = TOP + r * CELL;
                rr(c, l + 2, t + 2, l + CELL - 2, t + CELL - 2, 8, 0xFF2A2E3C);
                int g = isRight ? rightAt(r, col) : left[r][col];
                label(c, String.valueOf(GLYPHS.charAt(g)), l + CELL / 2f, t + CELL / 2f,
                        24, 0xFFECECF1, true);
            }
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("找到 " + solved + " 处", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1A1E2A);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        label(c, "已找 " + solved + " 处 · 右图哪块不对？点它", lw - 30, 30, 12, 0xFF9AA0AC, true);
        drawBoard(c, LEFT_L, false);
        drawBoard(c, LEFT_R, true);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
