package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g012 管道拧转：点管件旋转 90°，把水源接到出水口（90 秒多关卡）。
 *  谜面由一条随机管路生成，必有解；其余格子随机管件作干扰。每关 +200。 */
public class PipeGame extends CanvasGame {
    private static final int N = 5, CELL = 56, GX = 40, GY = 120;
    private static final long ROUND_MS = 90000;
    // 连接位：0上 1右 2下 3左（位掩码）
    private static final int UP = 1, RIGHT = 2, DOWN = 4, LEFT = 8;

    private final int[][] conns = new int[N][N];
    private final Random rnd = new Random();
    private int level, shownScore, moves, sinkR;
    private long roundEnd;
    private boolean settling;

    public PipeGame() {
        super(360, 560);
        genLevel();
    }

    private void genLevel() {
        for (int[] row : conns) java.util.Arrays.fill(row, 0);
        // 随机自避路径：左中 → 右缘（记下终点行）
        int r = N / 2, c = 0;
        List<int[]> path = new ArrayList<>();
        path.add(new int[]{r, c});
        boolean[][] used = new boolean[N][N];
        used[r][c] = true;
        while (c < N - 1) {
            int[][] cand = {{r - 1, c}, {r + 1, c}, {r, c + 1}};
            List<int[]> ok = new ArrayList<>();
            for (int[] p : cand) {
                int nr = p[0], nc = p[1];
                if (nr < 0 || nr >= N || nc < 0 || nc >= N || used[nr][nc]) continue;
                ok.add(p);
            }
            if (ok.isEmpty()) break;
            int[] pick = ok.get(rnd.nextInt(ok.size()));
            r = pick[0];
            c = pick[1];
            used[r][c] = true;
            path.add(new int[]{r, c});
        }
        sinkR = r;
        // 依路径设置连接
        for (int i = 0; i < path.size(); i++) {
            int[] cell = path.get(i);
            int m = 0;
            if (i > 0) m |= dirMask(cell, path.get(i - 1));
            if (i < path.size() - 1) m |= dirMask(cell, path.get(i + 1));
            conns[cell[0]][cell[1]] = m;
        }
        // 打乱旋转 + 干扰管件
        for (int i = 0; i < N; i++)
            for (int j = 0; j < N; j++) {
                if (conns[i][j] == 0) {
                    if (rnd.nextInt(3) == 0)
                        conns[i][j] = UP | DOWN; // 干扰直管
                    continue;
                }
                int rot = rnd.nextInt(4);
                for (int k = 0; k < rot; k++) conns[i][j] = rotate(conns[i][j]);
            }
    }

    private int dirMask(int[] a, int[] b) {
        if (b[0] == a[0] - 1) return UP;
        if (b[0] == a[0] + 1) return DOWN;
        if (b[1] == a[1] + 1) return RIGHT;
        return LEFT;
    }

    private int rotate(int m) {
        int out = 0;
        if ((m & UP) != 0) out |= RIGHT;
        if ((m & RIGHT) != 0) out |= DOWN;
        if ((m & DOWN) != 0) out |= LEFT;
        if ((m & LEFT) != 0) out |= UP;
        return out;
    }

    private void startRound() {
        level = 1;
        shownScore = 0;
        moves = 0;
        roundEnd = now() + ROUND_MS;
        genLevel();
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
        return GX + c * CELL;
    }

    private float cellY(int r) {
        return GY + r * CELL;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        int c = (int) ((x - GX) / CELL), r = (int) ((y - GY) / CELL);
        if (r < 0 || r >= N || c < 0 || c >= N || conns[r][c] == 0) return;
        conns[r][c] = rotate(conns[r][c]);
        moves++;
        beep(0);
        if (connected()) {
            shownScore += 200;
            host.setScore(shownScore);
            level++;
            popup("水路贯通！", lw / 2f, lh / 2f - 30, 0xFF7CFFB2, 18);
            burst(cellX(N - 1) + CELL / 2f, cellY(N / 2) + CELL / 2f, 0xFF5B8DEF, 16, 6f);
            beep(3);
            genLevel();
        }
        dirty();
    }

    /** 从水源（第 N/2 行左缘）BFS，看是否到达出水口（第 N/2 行右缘）。 */
    private boolean connected() {
        int sr = N / 2, sc = 0;
        if ((conns[sr][sc] & LEFT) == 0) return false;
        boolean[][] seen = new boolean[N][N];
        List<int[]> stack = new ArrayList<>();
        stack.add(new int[]{sr, sc});
        seen[sr][sc] = true;
        int[] drs = {-1, 0, 1, 0};
        int[] dcs = {0, 1, 0, -1};
        int[] masks = {UP, RIGHT, DOWN, LEFT};
        while (!stack.isEmpty()) {
            int[] p = stack.remove(stack.size() - 1);
            if (p[0] == sinkR && p[1] == N - 1 && (conns[p[0]][p[1]] & RIGHT) != 0) return true;
            for (int d = 0; d < 4; d++) {
                int nr = p[0] + drs[d], nc = p[1] + dcs[d];
                if (nr < 0 || nr >= N || nc < 0 || nc >= N || seen[nr][nc]) continue;
                int needIn = masks[(d + 2) % 4];
                if ((conns[p[0]][p[1]] & masks[d]) != 0 && (conns[nr][nc] & needIn) != 0) {
                    seen[nr][nc] = true;
                    stack.add(new int[]{nr, nc});
                }
            }
        }
        return false;
    }

    @Override
    protected void onTick() {
        if (now() >= roundEnd) {
            over = true;
            stopLoop();
            beep(3);
            popup("时间到 · 贯通 " + (level - 1) + " 关", lw / 2f, lh / 2f, 0xFFECECF1, 17);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 800);
            return;
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF101820);
        float remain = Math.max(0f, (roundEnd - now()) / (float) ROUND_MS);
        rr(c, 16, 52, lw - 16, 60, 4, 0xFF2A2D37);
        rr(c, 16, 52, 16 + (lw - 32) * remain, 60, 4, 0xFF5B8DEF);
        label(c, "第 " + level + " 关 · 点管件旋转", lw - 30, 30, 13, 0xFF9AA0AC, true);        for (int r = 0; r < N; r++)
            for (int col = 0; col < N; col++) {
                float l = cellX(col) + 3, t = cellY(r) + 3, s = CELL - 6;                rr(c, l, t, l + s, t + s, 8, 0xFF1B2530);
                int m = conns[r][col];
                if (m == 0) continue;
                stroke.setColor(0xFF5B8DEF);
                stroke.setStrokeWidth(9);
                float cx = l + s / 2f, cy = t + s / 2f;
                if ((m & UP) != 0) c.drawLine(cx, cy, cx, t, stroke);
                if ((m & DOWN) != 0) c.drawLine(cx, cy, cx, t + s, stroke);
                if ((m & LEFT) != 0) c.drawLine(cx, cy, l, cy, stroke);
                if ((m & RIGHT) != 0) c.drawLine(cx, cy, l + s, cy, stroke);
                fill.setColor(0xFF7CFFB2);
                c.drawCircle(cx, cy, 5, fill);
            }
        // 水源/出水口
        fill.setColor(0xFF5B8DEF);
        c.drawCircle(GX - 8, cellY(N / 2) + CELL / 2f, 9, fill);
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(GX + N * CELL + 8, cellY(sinkR) + CELL / 2f, 9, fill);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
