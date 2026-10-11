package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g009 迷宫滚球：滑动一步步滚球走迷宫，抵达出口进入下一张（90 秒内多过关）。
 *  每关 +150 分。迷宫为深度优先随机生成，必有通路。 */
public class MazeRollGame extends CanvasGame {
    private static final int GW = 11, GH = 15, CELL = 26, LEFT = 37, TOP = 120;
    private static final long ROUND_MS = 90000;

    private final boolean[][] wall = new boolean[GH][GW];
    private final Random rnd = new Random();
    private int pr, pc, level, shownScore;
    private long roundEnd;
    private float downX = -1, downY = -1;
    private boolean settling;

    public MazeRollGame() {
        super(360, 560);
        genMaze();
    }

    private void genMaze() {
        for (boolean[] row : wall) java.util.Arrays.fill(row, true);
        carve(1, 1);
        pr = 1;
        pc = 1;
    }

    private void carve(int r, int c) {
        wall[r][c] = false;
        int[] drs = {-2, 2, 0, 0};
        int[] dcs = {0, 0, -2, 2};
        int s = rnd.nextInt(4);
        for (int k = 0; k < 4; k++) {
            int i = (s + k) % 4;
            int nr = r + drs[i], nc = c + dcs[i];
            if (nr < 1 || nr >= GH - 1 || nc < 1 || nc >= GW - 1 || !wall[nr][nc]) continue;
            wall[r + drs[i] / 2][c + dcs[i] / 2] = false;
            carve(nr, nc);
        }
    }

    private void startRound() {
        genMaze();
        roundEnd = now() + ROUND_MS;
        level = 1;
        shownScore = 0;
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
        int nr = pr + dr, nc = pc + dc;
        if (nr < 0 || nr >= GH || nc < 0 || nc >= GW || wall[nr][nc]) return;
        pr = nr;
        pc = nc;
        beep(0);
        if (pr == GH - 2 && pc == GW - 2) {
            shownScore += 150;
            host.setScore(shownScore);
            level++;
            popup("过关！新迷宫…", lw / 2f, lh / 2f - 30, 0xFF7CFFB2, 17);
            burst(cellX(GW - 2), cellY(GH - 2), 0xFFFFC24B, 16, 6f);
            genMaze();
        }
        dirty();
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到 · 通过 " + (level - 1) + " 关", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF171B24);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF7B7FF5);
        label(c, "第 " + level + " 关", lw - 40, 30, 14, 0xFF9AA0AC, true);
        for (int r = 0; r < GH; r++)
            for (int col = 0; col < GW; col++)
                if (wall[r][col])
                    rr(c, cellX(col), cellY(r), cellX(col) + CELL, cellY(r) + CELL, 3, 0xFF5663A8);
        rr(c, cellX(GW - 2) + 4, cellY(GH - 2) + 4, cellX(GW - 2) + CELL - 4, cellY(GH - 2) + CELL - 4,
                6, 0xFFFFC24B);
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(cellX(pc) + CELL / 2f, cellY(pr) + CELL / 2f, CELL / 2f - 3, fill);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
