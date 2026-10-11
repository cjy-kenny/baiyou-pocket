package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g100 单词消消：字母盘里藏着单词，点首字母再点尾字母（同行或同列直线）即消除；
 *  找齐一盘换新盘，90 秒冲分。 */
public class WordFindGame extends CanvasGame {
    private static final int N = 6, CELL = 48, LEFT = 36, TOP = 150;
    private static final long ROUND_MS = 90000;

    private final Random rnd = new Random();
    private final char[][] grid = new char[N][N];
    private final List<String> targets = new ArrayList<>();
    private final List<String> found = new ArrayList<>();
    private int selR = -1, selC = -1, shownScore;
    private long roundEnd;
    private boolean settling;

    public WordFindGame() {
        super(360, 560);
        genBoard();
    }

    private void genBoard() {
        targets.clear();
        found.clear();
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                grid[r][c] = 0; // 0 = 未填
        String[] pool = {"CAT", "DOG", "SUN", "MAP", "BUS", "STAR", "MOON", "FISH", "CAKE", "RAIN"};
        java.util.Collections.shuffle(java.util.Arrays.asList(pool), rnd);
        int placed = 0;
        for (String w : pool) {
            if (placed >= 4) break;
            for (int tries = 0; tries < 25; tries++) {
                boolean horiz = rnd.nextBoolean();
                int len = w.length();
                int r = rnd.nextInt(horiz ? N : N - len + 1);
                int c = rnd.nextInt(horiz ? N - len + 1 : N);
                boolean ok = true;
                for (int k = 0; k < len; k++) {
                    char ch = horiz ? grid[r][c + k] : grid[r + k][c];
                    if (ch != 0 && ch != w.charAt(k)) {
                        ok = false;
                        break;
                    }
                }
                if (!ok) continue;
                for (int k = 0; k < len; k++) {
                    if (horiz) grid[r][c + k] = w.charAt(k);
                    else grid[r + k][c] = w.charAt(k);
                }
                targets.add(w);
                placed++;
                break;
            }
        }
        // 兜底：至少两个词
        while (targets.size() < 2) {
            targets.add(pool[targets.size()]);
        }
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (grid[r][c] == 0) grid[r][c] = (char) ('A' + rnd.nextInt(26));
        selR = selC = -1;
    }

    private void startRound() {
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
        if (selR < 0) {
            selR = r;
            selC = c;
            beep(0);
            dirty();
            return;
        }
        // 必须同行或同列
        if (selR == r && selC == c) {
            selR = selC = -1;
            dirty();
            return;
        }
        if (selR != r && selC != c) {
            selR = r;
            selC = c;
            beep(2);
            dirty();
            return;
        }
        StringBuilder sb = new StringBuilder();
        StringBuilder rev = new StringBuilder();
        if (selR == r) {
            int from = Math.min(selC, c), to = Math.max(selC, c);
            for (int k = from; k <= to; k++) sb.append(grid[r][k]);
        } else {
            int from = Math.min(selR, r), to = Math.max(selR, r);
            for (int k = from; k <= to; k++) sb.append(grid[k][c]);
        }
        rev.append(sb).reverse();
        String word = sb.toString();
        String wordRev = rev.toString();
        String hit2 = null;
        for (String t : targets) {
            if (!found.contains(t) && (t.equals(word) || t.equals(wordRev))) {
                hit2 = t;
                break;
            }
        }
        if (hit2 != null) {
            found.add(hit2);
            shownScore += 50 + hit2.length() * 10;
            host.setScore(shownScore);
            beep(1);
            burst(cellX(c) + CELL / 2f, cellY(r) + CELL / 2f, 0xFF7CFFB2, 12, 5f);
            popup("找到 " + hit2 + "！", lw / 2f, 110, 0xFF7CFFB2, 16);
            selR = selC = -1;
            if (found.size() == targets.size()) {
                shownScore += 200;
                host.setScore(shownScore);
                popup("清盘！ +200", lw / 2f, 140, 0xFFFFC24B, 18);
                beep(3);
                genBoard();
            }
        } else {
            selR = r;
            selC = c;
            beep(2);
        }
        dirty();
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
        rr(c, 0, 0, lw, lh, 0, 0xFF1A1E2A);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        // 目标词
        StringBuilder sb = new StringBuilder();
        for (String t : targets) {
            sb.append(found.contains(t) ? "✓" + t : t).append("  ");
        }
        label(c, sb.toString(), lw / 2f, 110, 14, 0xFF9AA0AC, true);
        for (int r = 0; r < N; r++)
            for (int col = 0; col < N; col++) {
                float l = cellX(col) + 2, t = cellY(r) + 2;
                rr(c, l, t, l + CELL - 4, t + CELL - 4, 7, 0xFF2A2E3C);
                boolean sel = r == selR && col == selC;
                label(c, String.valueOf(grid[r][col]), l + (CELL - 4) / 2f, t + (CELL - 4) / 2f,
                        20, sel ? 0xFFFFC24B : 0xFFB9C4E0, true);
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
