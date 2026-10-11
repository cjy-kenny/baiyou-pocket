package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g067 挖矿寻宝：从井口一路挖到宝石，岩石挡路要绕；限定步数内到达即过关，
 *  越关石头越密。 */
public class MineDigGame extends CanvasGame {
    private static final int COLS = 6, ROWS = 8, CELL = 50, LEFT = 30, TOP = 120;
    private static final int STEPS = 34;

    private final Random rnd = new Random();
    private final boolean[][] rock = new boolean[ROWS][COLS];
    private int pr, pc, gemR, gemC, stepsLeft, level, shownScore;
    private boolean settling;

    public MineDigGame() {
        super(360, 560);
        genLevel();
    }

    private void genLevel() {
        for (boolean[] row : rock) java.util.Arrays.fill(row, false);
        pr = 0;
        pc = COLS / 2;
        gemR = ROWS - 1;
        gemC = rnd.nextInt(COLS);
        int rocks = 10 + level * 3;
        while (rocks > 0) {
            int r = rnd.nextInt(ROWS), c = rnd.nextInt(COLS);
            if ((r == 0 && c == pc) || (r == gemR && c == gemC) || rock[r][c]) continue;
            rock[r][c] = true;
            rocks--;
        }
        stepsLeft = STEPS;
    }

    private void startGame() {
        level = 1;
        shownScore = 0;
        over = false;
        genLevel();
        host.setScore(0);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
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
        if (r < 0 || r >= ROWS || c < 0 || c >= COLS) return;
        if (Math.abs(pr - r) + Math.abs(pc - c) != 1) return;
        if (rock[r][c]) {
            beep(2);
            popup("是岩石", cellX(c) + CELL / 2f, cellY(r) - 10, 0xFFE8848C, 13);
            dirty();
            return;
        }
        pr = r;
        pc = c;
        stepsLeft--;
        beep(0);
        if (pr == gemR && pc == gemC) {
            shownScore += 200;
            host.setScore(shownScore);
            level++;
            popup("挖到宝石！ +200", lw / 2f, 100, 0xFFFFC24B, 19);
            burst(cellX(gemC) + CELL / 2f, cellY(gemR) + CELL / 2f, 0xFFFFC24B, 16, 6f);
            beep(3);
            genLevel();
            dirty();
            return;
        }
        if (stepsLeft <= 0) {
            over = true;
            stopLoop();
            beep(2);
            popup("镐头磨平了", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 850);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF26201B);
        label(c, "第 " + level + " 矿 · 步数 " + stepsLeft, lw - 30, 30, 13, 0xFFC9B896, true);
        for (int r = 0; r < ROWS; r++)
            for (int col = 0; col < COLS; col++) {
                float l = cellX(col) + 2, t = cellY(r) + 2;
                rr(c, l, t, l + CELL - 4, t + CELL - 4, 5,
                        rock[r][col] ? 0xFF8A7B6B : 0xFF2A241C);
                if (rock[r][col]) {
                    // 岩石灰块 + 高光棱面，一眼能认
                    fill.setColor(0xFF6E6257);
                    rf.set(l + 6, t + 6, l + CELL - 10, t + CELL - 10);
                    c.drawRoundRect(rf, 4, 4, fill);
                    fill.setColor(0xFF9A8E80);
                    c.drawCircle(l + 14, t + 14, 3.5f, fill);
                }
                if (r == gemR && col == gemC) {
                    fill.setColor(0xFFFFC24B);
                    rf.set(l + 12, t + 12, l + CELL - 16, t + CELL - 16);
                    c.drawRoundRect(rf, 4, 4, fill);
                    stroke.setColor(0xFFFFFFFF);
                    stroke.setStrokeWidth(2);
                    c.drawLine(l + 14, t + 20, l + CELL - 18, t + 20, stroke);
                }
            }
        // 矿工
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(cellX(pc) + CELL / 2f, cellY(pr) + CELL / 2f, 16, fill);
        fill.setColor(0xFF1B1C22);
        c.drawCircle(cellX(pc) + CELL / 2f - 5, cellY(pr) + CELL / 2f - 4, 2.5f, fill);
        c.drawCircle(cellX(pc) + CELL / 2f + 5, cellY(pr) + CELL / 2f - 4, 2.5f, fill);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
