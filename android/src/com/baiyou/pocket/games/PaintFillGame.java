package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g063 涂色小格：一笔涂满 4×6 格（可以任意起点，重复涂整盘重来），
 *  用时越短分越高。 */
public class PaintFillGame extends CanvasGame {
    private static final int COLS = 6, ROWS = 4, CELL = 50, LEFT = 30, TOP = 140;

    private final boolean[][] painted = new boolean[ROWS][COLS];
    private final Random rnd = new Random();
    private int pr, pc, pathLen;
    private long startAt;
    private boolean settling, started;

    public PaintFillGame() {
        super(360, 560);
        resetBoard();
    }

    private void resetBoard() {
        for (boolean[] row : painted) java.util.Arrays.fill(row, false);
        pr = rnd.nextInt(ROWS);
        pc = rnd.nextInt(COLS);
        painted[pr][pc] = true;
        pathLen = 1;
        started = false;
    }

    private void startRound() {
        resetBoard();
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
        if (over || settling) return;
        if (action == 0) {
            started = true;
            return;
        }
        if (action != 2) return;
        int c = Math.round((x - LEFT) / CELL), r = Math.round((y - TOP) / CELL);
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return;
        if (Math.abs(pr - r) + Math.abs(pc - c) != 1) return;
        if (painted[r][c]) {
            // 重复涂 → 整盘重来
            beep(2);
            shake(200);
            resetBoard();
            dirty();
            return;
        }
        painted[r][c] = true;
        pr = r;
        pc = c;
        pathLen++;
        beep(0);
        if (pathLen == ROWS * COLS) {
            settling = true;
            int sec = (int) ((now() - startAt) / 1000);
            int finalScore = Math.max(100, 1200 - sec * 4);
            host.setScore(finalScore);
            popup("涂满了！", lw / 2f, lh / 2f, 0xFFFFC24B, 22);
            beep(3);
            dirty();
            postDelayedSafe(() -> host.gameOver(finalScore), 850);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1A2226);
        int remain = ROWS * COLS - pathLen;
        label(c, "还差 " + remain + " 格，别走回头路", lw / 2f, 95, 13, 0xFF8BC5B0, false);
        for (int r = 0; r < ROWS; r++)
            for (int col = 0; col < COLS; col++) {
                float l = cellX(col) + 3, t = cellY(r) + 3;
                rr(c, l, t, l + CELL - 6, t + CELL - 6, 8,
                        painted[r][col] ? 0xFF2FA36B : 0xFF243230);
                if (r == pr && col == pc) {
                    fill.setColor(0xFFECECF1);
                    c.drawCircle(l + (CELL - 6) / 2f, t + (CELL - 6) / 2f, 7, fill);
                }
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
