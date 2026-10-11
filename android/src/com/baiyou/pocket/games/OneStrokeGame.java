package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g008 一笔画成：5×5 点阵，从起点出发一笔走遍所有点（每个点只经一次），
 *  走到走过的点=回退一步。全部点亮即胜，用时越短分越高。 */
public class OneStrokeGame extends CanvasGame {
    private static final int N = 5, CELL = 52, LEFT = 55, TOP = 140;

    private final boolean[][] visited = new boolean[N][N];
    private final int[] pathR = new int[N * N];
    private final int[] pathC = new int[N * N];
    private int pathLen;
    private final Random rnd = new Random();
    private long startAt;
    private boolean settling;

    public OneStrokeGame() {
        super(360, 560);
        genPuzzle();
    }

    /** 随机自避走样生成一条覆盖全图的哈密顿路径 → 谜面必有解。 */
    private void genPuzzle() {
        boolean[][] seen = new boolean[N][N];
        int[] pr = new int[N * N], pc = new int[N * N];
        if (!dfsGen(seen, pr, pc, 0, 0, 0) || pathLen == 0) {
            // 防御回退：蛇形路径
            for (boolean[] row : visited) java.util.Arrays.fill(row, false);
            pathLen = 0;
            for (int r = 0; r < N; r++) {
                for (int cI = 0; cI < N; cI++) {
                    int c = r % 2 == 0 ? cI : N - 1 - cI;
                    visited[r][c] = true;
                    pathR[pathLen] = r;
                    pathC[pathLen] = c;
                    pathLen++;
                }
            }
            // 只保留起点，其余留白（谜面仍是全图）
            for (boolean[] row : visited) java.util.Arrays.fill(row, false);
            visited[0][0] = true;
            pathLen = 1;
        }
    }

    private boolean dfsGen(boolean[][] seen, int[] pr, int[] pc, int r, int c, int depth) {
        seen[r][c] = true;
        pr[depth] = r;
        pc[depth] = c;
        if (depth == N * N - 1) {
            seen[r][c] = false;
            // 谜面：起点固定左上角（5×5 角点必有哈密顿路径），玩家自寻全图路径
            for (boolean[] row : visited) java.util.Arrays.fill(row, false);
            visited[0][0] = true;
            pathR[0] = 0;
            pathC[0] = 0;
            pathLen = 1;
            return true;
        }
        int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
        int s = rnd.nextInt(4);
        for (int k = 0; k < 4; k++) {
            int[] d = dirs[(s + k) % 4];
            int nr = r + d[0], nc = c + d[1];
            if (nr < 0 || nr >= N || nc < 0 || nc >= N || seen[nr][nc]) continue;
            if (dfsGen(seen, pr, pc, nr, nc, depth + 1)) {
                seen[r][c] = false;
                return true;
            }
        }
        seen[r][c] = false;
        return false;
    }

    private void startRound() {
        genPuzzle();
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

    private int curR() {
        return pathR[pathLen - 1];
    }

    private int curC() {
        return pathC[pathLen - 1];
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling) return;
        if (action != 0 && action != 2) return;
        // 视觉点在格心：直接找最近的点，距离超过半格忽略
        int cr = pathLen > 0 ? curR() : 0, cc = pathLen > 0 ? curC() : 0;
        int bestR = -1, bestC = -1, bestD = Integer.MAX_VALUE;
        for (int r = 0; r < N; r++)
            for (int c2 = 0; c2 < N; c2++) {
                float dx = x - (cellX(c2) + CELL / 2f), dy = y - (cellY(r) + CELL / 2f);
                int d = (int) (dx * dx + dy * dy);
                if (d < bestD) {
                    bestD = d;
                    bestR = r;
                    bestC = c2;
                }
            }
        if (bestR < 0 || bestD > (CELL / 2f) * (CELL / 2f)) return;
        int r = bestR, c = bestC;
        if (Math.abs(cr - r) + Math.abs(cc - c) != 1) return;
        if (!visited[r][c]) {
            visited[r][c] = true;
            pathR[pathLen] = r;
            pathC[pathLen] = c;
            pathLen++;
            beep(0);
            if (pathLen == N * N) {
                settling = true;
                int sec = (int) ((now() - startAt) / 1000);
                int finalScore = Math.max(100, 1200 - sec * 4);
                host.setScore(finalScore);
                popup("一笔完成！", lw / 2f, lh / 2f, 0xFFFFC24B, 22);
                beep(3);
                dirty();
                postDelayedSafe(() -> host.gameOver(finalScore), 850);
            }
        } else if (pathLen > 1 && pathR[pathLen - 2] == r && pathC[pathLen - 2] == c) {
            // 回退一步
            visited[cr][cc] = false;
            pathLen--;
            beep(2);
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1E222B);
        label(c, "从绿点出发一笔走遍 · 走回头路=撤销", lw / 2f, 90, 13, 0xFF8B8E9C, false);
        // 连线（点在格心，触点即所见）
        stroke.setColor(0xFF7CFFB2);
        stroke.setStrokeWidth(5);
        for (int i = 1; i < pathLen; i++) {
            c.drawLine(cellX(pathC[i - 1]) + CELL / 2f, cellY(pathR[i - 1]) + CELL / 2f,
                    cellX(pathC[i]) + CELL / 2f, cellY(pathR[i]) + CELL / 2f, stroke);
        }
        for (int r = 0; r < N; r++)
            for (int col = 0; col < N; col++) {
                boolean isCur = pathLen > 0 && curR() == r && curC() == col;
                if (visited[r][col]) {
                    fill.setColor(0xFF7CFFB2);
                    c.drawCircle(cellX(col) + CELL / 2f, cellY(r) + CELL / 2f, isCur ? 12 : 8, fill);
                } else {
                    fill.setColor(0xFF566079);
                    c.drawCircle(cellX(col) + CELL / 2f, cellY(r) + CELL / 2f, 6, fill);
                }
            }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
