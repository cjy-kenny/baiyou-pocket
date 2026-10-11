package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g049 数字追踪：数字按顺序在方格间闪现，闪完后按同样顺序点格子；
 *  点错即结束，轮数即分数。 */
public class NumberTraceGame extends CanvasGame {
    private static final int N = 4, CELL = 62, LEFT = 52, TOP = 140;

    private final Random rnd = new Random();
    private final List<Integer> order = new ArrayList<>(); // 格子下标序列
    private int showIdx, inputPos, round;
    private boolean showing, settling;
    private long showUntil;
    private int litCell = -1;
    private int shownScore;

    public NumberTraceGame() {
        super(360, 560);
        genRound();
    }

    private void genRound() {
        int len = 3 + round;
        int cells = N * N;
        order.clear();
        int last = -1;
        for (int i = 0; i < len; i++) {
            int cell;
            do {
                cell = rnd.nextInt(cells);
            } while (cell == last);
            order.add(cell);
            last = cell;
        }
        showIdx = 0;
        inputPos = 0;
        showing = true;
        litCell = order.get(0);
        showUntil = now() + 520;
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
        int cell = r * N + c;
        if (cell == order.get(inputPos)) {
            inputPos++;
            beep(1);
            burst(cellX(c) + CELL / 2f, cellY(r) + CELL / 2f, 0xFF7CFFB2, 6, 4f);
            if (inputPos >= order.size()) {
                round++;
                shownScore += 120;
                host.setScore(shownScore);
                popup("第 " + (round - 1) + " 轮达成！", lw / 2f, 100, 0xFF7CFFB2, 18);
                beep(3);
                settling = true;
                postDelayedSafe(() -> {
                    settling = false;
                    if (over) return;
                    genRound();
                    dirty();
                }, 800);
            }
        } else {
            over = true;
            stopLoop();
            beep(2);
            shake(240);
            popup("跟丢了！完成 " + (round - 1) + " 轮", lw / 2f, lh / 2f, 0xFFE8574F, 17);
            int finalScore = (round - 1) * 120;
            host.setScore(finalScore);
            postDelayedSafe(() -> host.gameOver(finalScore), 850);
        }
        dirty();
    }

    @Override
    protected void onTick() {
        if (showing) {
            long now = now();
            if (now >= showUntil) {
                showIdx++;
                if (showIdx >= order.size()) {
                    showing = false;
                    litCell = -1;
                } else {
                    litCell = order.get(showIdx);
                    showUntil = now + 520;
                }
            }
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1A1E26);
        label(c, "第 " + round + " 轮 · " + (showing ? "记住顺序…" : "按顺序点！"),
                lw / 2f, 95, 14, showing ? 0xFFFFC24B : 0xFFECECF1, true);
        for (int r = 0; r < N; r++)
            for (int col = 0; col < N; col++) {
                float l = cellX(col) + 3, t = cellY(r) + 3;
                int idx = r * N + col;
                boolean lit = showing && idx == litCell;
                rr(c, l, t, l + CELL - 6, t + CELL - 6, 10, lit ? 0xFFFFC24B : 0xFF2A2E3C);
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
