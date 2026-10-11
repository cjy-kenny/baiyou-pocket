package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

/** g014 木箱推推：滑动移动搬运工，把箱子推上货点；三个仓库逐步加大，
 *  全部归位进入下一关，用时与步数越少分越高。 */
public class SokobanGame extends CanvasGame {
    // 关卡：# 墙 . 地 B 箱 T 货点 P 人（行等长）
    private static final String[][] LEVELS = {
            {
                    "######",
                    "#....#",
                    "#.PB.#",
                    "#...T#",
                    "######",
            },
            {
                    "#######",
                    "#.T...#",
                    "#.B...#",
                    "#P.B..#",
                    "#..T..#",
                    "#.....#",
                    "#######",
            },
            {
                    "########",
                    "#..T...#",
                    "#.BB...#",
                    "#..P...#",
                    "#.T.B.T#",
                    "#......#",
                    "########",
            },
    };

    private int level;
    private char[][] grid;
    private int rows, cols, pr, pc;
    private int moves, shownScore;
    private long levelStart;
    private float downX = -1, downY = -1;
    private boolean settling;

    public SokobanGame() {
        super(360, 560);
        loadLevel(0);
    }

    private void loadLevel(int lv) {
        level = lv;
        String[] def = LEVELS[lv];
        rows = def.length;
        cols = def[0].length();
        grid = new char[rows][];
        for (int r = 0; r < rows; r++) grid[r] = def[r].toCharArray();
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c] == 'P') {
                    pr = r;
                    pc = c;
                }
        moves = 0;
        levelStart = now();
    }

    private void startGame() {
        shownScore = 0;
        settling = false;
        over = false;
        loadLevel(0);
        host.setScore(0);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    private float cellSize() {
        return Math.min((lw - 32) / (float) cols, 380f / rows);
    }

    private float boardLeft() {
        return (lw - cellSize() * cols) / 2f;
    }

    private float boardTop() {
        return 120 + (380 - cellSize() * rows) / 2f;
    }

    private int boxesLeft() {
        int n = 0;
        for (int r = 0; r < rows; r++)
            for (int c = 0; c < cols; c++)
                if (grid[r][c] == 'B' || grid[r][c] == 'b') n++;
        return n;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling) return;
        if (action == 0) {
            downX = x;
            downY = y;
            return;
        }
        if (action != 1 && action != 2) return;
        if (downX < 0) return;
        float dx = x - downX, dy = y - downY;
        if (Math.max(Math.abs(dx), Math.abs(dy)) < 30) return;
        int dr = 0, dc = 0;
        if (Math.abs(dx) > Math.abs(dy)) dc = dx > 0 ? 1 : -1;
        else dr = dy > 0 ? 1 : -1;
        downX = x;
        downY = y;
        step(dr, dc);
    }

    private void step(int dr, int dc) {
        char cur = grid[pr][pc];
        int nr = pr + dr, nc = pc + dc;
        char ahead = grid[nr][nc];
        if (ahead == '#') return;
        if (ahead == 'B' || ahead == 'b') {
            int br = nr + dr, bc = nc + dc;
            char beyond = grid[br][bc];
            if (beyond == '#' || beyond == 'B' || beyond == 'b') return;
            // 推箱：箱入目标点变 b；玩家落到目标点变 p
            grid[br][bc] = beyond == 'T' ? 'b' : 'B';
            grid[nr][nc] = ahead == 'b' ? 'p' : 'P';
        } else if (ahead == 'T') {
            grid[nr][nc] = 'p'; // 人站在货点上
        } else {
            grid[nr][nc] = 'P';
        }
        grid[pr][pc] = cur == 'p' ? 'T' : '.';
        pr = nr;
        pc = nc;
        moves++;
        beep(0);
        if (boxesLeft() == 0) {
            settling = true;
            int sec = (int) ((now() - levelStart) / 1000);
            shownScore += Math.max(100, 800 - moves * 10 - sec * 4);
            host.setScore(shownScore);
            beep(3);
            burst(boardLeft() + cellSize() * pc, boardTop() + cellSize() * pr, 0xFF7CFFB2, 14, 6f);
            dirty();
            postDelayedSafe(this::nextLevel, 900);
            return;
        }
        dirty();
    }

    private void nextLevel() {
        settling = false;
        if (level >= LEVELS.length - 1) {
            over = true;
            stopLoop();
            popup("三仓完毕！总分 " + shownScore, lw / 2f, lh / 2f, 0xFFFFC24B, 20);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 850);
            return;
        }
        popup("进入第 " + (level + 2) + " 仓", lw / 2f, lh / 2f - 30, 0xFFECECF1, 16);
        loadLevel(level + 1);
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF22201B);
        label(c, "第 " + (level + 1) + "/" + LEVELS.length + " 仓 · 步数 " + moves,
                lw / 2f, 70, 14, 0xFFC9B896, false);
        float cs = cellSize(), bl = boardLeft(), bt = boardTop();
        for (int r = 0; r < rows; r++)
            for (int col = 0; col < cols; col++) {
                float l = bl + col * cs, t = bt + r * cs;
                char ch = grid[r][col];
                if (ch == '#') {
                    rr(c, l, t, l + cs, t + cs, 3, 0xFF5A4A38);
                    continue;
                }
                rr(c, l, t, l + cs, t + cs, 3, 0xFF2E2A22);
                if (ch == 'T' || ch == 'p' || ch == 'b') {
                    stroke.setColor(0xFFC9B896);
                    stroke.setStrokeWidth(2);
                    rf.set(l + 8, t + 8, l + cs - 8, t + cs - 8);
                    c.drawRoundRect(rf, 4, 4, stroke);
                }
                if (ch == 'B' || ch == 'b') {
                    rr(c, l + 5, t + 5, l + cs - 5, t + cs - 5, 5, ch == 'b' ? 0xFF7CFFB2 : 0xFFE0973F);
                    stroke.setColor(0x66302010);
                    stroke.setStrokeWidth(2);
                    c.drawLine(l + 8, t + 8, l + cs - 8, t + cs - 8, stroke);
                    c.drawLine(l + cs - 8, t + 8, l + 8, t + cs - 8, stroke);
                }
                if (ch == 'P' || ch == 'p') {
                    fill.setColor(0xFF7CFFB2);
                    c.drawCircle(l + cs / 2f, t + cs / 2f, cs * 0.3f, fill);
                }
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
