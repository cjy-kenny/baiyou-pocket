package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

/** g011 孔明棋阵：英式 33 孔跳棋，跳过相邻棋子吃掉它，剩子越少分越高，
 *  只剩 1 子且在中心有额外大奖。无子可跳即结算。 */
public class PegSolitaireGame extends CanvasGame {
    private static final int N = 7, CELL = 40, LEFT = 46, TOP = 130;
    // 英式棋盘布局（1=有孔）
    private static final boolean[][] MASK = {
            {false, false, true, true, true, false, false},
            {false, false, true, true, true, false, false},
            {true, true, true, true, true, true, true},
            {true, true, true, true, true, true, true},
            {true, true, true, true, true, true, true},
            {false, false, true, true, true, false, false},
            {false, false, true, true, true, false, false},
    };

    private final boolean[][] peg = new boolean[N][N];
    private int selR = -1, selC = -1, pegs, moves;

    public PegSolitaireGame() {
        super(360, 560);
        startBoard();
    }

    private void startBoard() {
        pegs = 0;
        moves = 0;
        selR = selC = -1;
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                peg[r][c] = MASK[r][c];
                if (peg[r][c]) pegs++;
            }
        peg[3][3] = false; // 中心留空
        pegs--;
        over = false;
        dirty();
    }

    @Override
    public void restartGame() {
        startBoard();
        host.setScore(0);
    }

    private float cellX(int c) {
        return LEFT + c * CELL;
    }

    private float cellY(int r) {
        return TOP + r * CELL;
    }

    private boolean inMask(int r, int c) {
        return r >= 0 && r < N && c >= 0 && c < N && MASK[r][c];
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || action != 0) return;
        int c = (int) ((x - LEFT) / CELL), r = (int) ((y - TOP) / CELL);
        if (!inMask(r, c)) return;

        if (selR >= 0 && peg[selR][selC]) {
            // 尝试跳
            int dr = r - selR, dc = c - selC;
            if ((Math.abs(dr) == 2 && dc == 0) || (Math.abs(dc) == 2 && dr == 0)) {
                int mr = selR + dr / 2, mc = selC + dc / 2;
                if (!peg[r][c] && peg[mr][mc]) {
                    peg[selR][selC] = false;
                    peg[mr][mc] = false;
                    peg[r][c] = true;
                    pegs--;
                    moves++;
                    selR = selC = -1;
                    beep(1);
                    burst(cellX(c) + CELL / 2f, cellY(r) + CELL / 2f, 0xFF7CFFB2, 8, 4f);
                    if (pegs == 1 && r == 3 && c == 3) {
                        popup("完美收局！", lw / 2f, 90, 0xFFFFC24B, 22);
                        beep(3);
                    }
                    if (!hasMoves()) {
                        finish();
                        return;
                    }
                    dirty();
                    return;
                }
            }
        }
        // 选中/取消
        if (peg[r][c]) {
            selR = (selR == r && selC == c) ? -1 : r;
            selC = selR < 0 ? -1 : c;
            beep(0);
        } else {
            selR = selC = -1;
        }
        dirty();
    }

    private boolean hasMoves() {
        int[][] dirs = {{-2, 0}, {2, 0}, {0, -2}, {0, 2}};
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                if (!peg[r][c]) continue;
                for (int[] d : dirs) {
                    int mr = r + d[0] / 2, mc = c + d[1] / 2;
                    int tr = r + d[0], tc = c + d[1];
                    if (inMask(tr, tc) && !peg[tr][tc] && peg[mr][mc]) return true;
                }
            }
        return false;
    }

    private void finish() {
        over = true;
        int finalScore = (32 - pegs) * 50 + (pegs == 1 && peg[3][3] ? 500 : 0);
        host.setScore(finalScore);
        popup("剩 " + pegs + " 子 · " + finalScore + " 分", lw / 2f, 90, 0xFFECECF1, 17);
        beep(3);
        dirty();
        postDelayedSafe(() -> host.gameOver(finalScore), 900);
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1C1420);
        label(c, "跳子吃子 · 剩 " + pegs + " 子 · 步数 " + moves, lw / 2f, 90, 14, 0xFFC9B8E8, false);
        for (int r = 0; r < N; r++)
            for (int col = 0; col < N; col++) {
                if (!inMask(r, col)) continue;
                float cx = cellX(col) + CELL / 2f, cy = cellY(r) + CELL / 2f;
                fill.setColor(0xFF2E2438);
                c.drawCircle(cx, cy, 15, fill);
                if (peg[r][col]) {
                    boolean sel = r == selR && col == selC;
                    fill.setColor(sel ? 0xFFFFC24B : 0xFF9B59F6);
                    c.drawCircle(cx, cy, 13, fill);
                    fill.setColor(0x66FFFFFF);
                    c.drawCircle(cx - 4, cy - 4, 4, fill);
                }
                if (selR >= 0 && peg[selR][selC] && canJumpTo(selR, selC, r, col)) {
                    stroke.setColor(0x887CFFB2);
                    stroke.setStrokeWidth(2);
                    c.drawCircle(cx, cy, 17, stroke);
                }
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private boolean canJumpTo(int sr, int sc, int tr, int tc) {
        int dr = tr - sr, dc = tc - sc;
        if (!((Math.abs(dr) == 2 && dc == 0) || (Math.abs(dc) == 2 && dr == 0))) return false;
        if (!inMask(tr, tc) || peg[tr][tc]) return false;
        return peg[sr + dr / 2][sc + dc / 2];
    }
}
