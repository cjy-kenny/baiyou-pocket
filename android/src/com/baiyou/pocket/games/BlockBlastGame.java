package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/** g001 方块消消：8×8 同色消除。
 *  点连 ≥2 个同色方块消除（n² × 连击系数计分），方块平滑下落/左移；
 *  3 秒内连续消除触发连击（每次 +25%）；无可消组合时提示后结算，全清 +200。 */
public class BlockBlastGame extends CanvasGame {
    private static final int N = 8;
    private static final int CELL = 44, PAD = 4, TOP = 6;
    private static final int[] PALETTE = {
            0xFF5B8DEF, 0xFFE8574F, 0xFFF2A33C, 0xFF2FA36B, 0xFF9B59F6};
    private static final long POP_MS = 140, FALL_MS = 150;

    private final int[][] grid = new int[N][N];
    /** 爆裂退场：{r, c, deadline, color} */
    private final List<long[]> popping = new ArrayList<>();
    /** 下落动画：{value, fx, fy, tx, ty, start, destIdx=r*N+c}（格左上角逻辑坐标） */
    private final List<float[]> falling = new ArrayList<>();
    private int score;
    private int combo;
    private long lastClearAt;
    private long fallStart;
    private boolean settling;

    public BlockBlastGame() {
        super(360, 430);
        fillBoard();
    }

    private void fillBoard() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                grid[r][c] = rnd.nextInt(PALETTE.length);
    }

    private float cellX(int c) {
        return PAD + c * CELL;
    }

    private float cellY(int r) {
        return TOP + r * CELL;
    }

    @Override
    public void restartGame() {
        fillBoard();
        popping.clear();
        falling.clear();
        over = false;
        settling = false;
        score = 0;
        combo = 0;
        host.setScore(0);
        dirty();
    }

    // ---------------- 交互 ----------------

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0 || !popping.isEmpty() || !falling.isEmpty()) return;
        int c = (int) ((x - PAD) / CELL);
        int r = (int) ((y - TOP) / CELL);
        if (r < 0 || r >= N || c < 0 || c >= N) return;
        int color = grid[r][c];
        if (color < 0) return;

        List<int[]> group = flood(r, c, color);
        if (group.size() < 2) {
            beep(2);
            return;
        }

        // 连击计分：3 秒内连续消除，每次 +25%
        long now = System.currentTimeMillis();
        combo = (now - lastClearAt < 3000) ? combo + 1 : 1;
        lastClearAt = now;
        int base = group.size() * group.size();
        int gained = Math.round(base * (1f + 0.25f * (combo - 1)));
        score += gained;
        host.setScore(score);

        // 特效：组中心弹分 + 连击提示 + 每格碎屑
        float cx = 0, cy = 0;
        long deadline = now + POP_MS;
        for (int[] p : group) {
            popping.add(new long[]{p[0], p[1], deadline, PALETTE[color]});
            cx += cellX(p[1]) + CELL / 2f;
            cy += cellY(p[0]) + CELL / 2f;
            burst(cellX(p[1]) + CELL / 2f, cellY(p[0]) + CELL / 2f, PALETTE[color], 3, 4.5f);
        }
        cx /= group.size();
        cy /= group.size();
        popup("+" + gained, cx, cy, 0xFFFFFFFF, 22);
        if (combo >= 2) popup("连击 ×" + combo, cx, cy - 26, 0xFFFFC24B, 15);
        beep(1);
        startLoop(16);
        dirty();
    }

    private List<int[]> flood(int sr, int sc, int color) {
        List<int[]> out = new ArrayList<>();
        boolean[][] seen = new boolean[N][N];
        List<int[]> stack = new ArrayList<>();
        stack.add(new int[]{sr, sc});
        seen[sr][sc] = true;
        while (!stack.isEmpty()) {
            int[] p = stack.remove(stack.size() - 1);
            out.add(p);
            int[][] dirs = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
            for (int[] d : dirs) {
                int nr = p[0] + d[0], nc = p[1] + d[1];
                if (nr < 0 || nr >= N || nc < 0 || nc >= N || seen[nr][nc]) continue;
                if (grid[nr][nc] == color) {
                    seen[nr][nc] = true;
                    stack.add(new int[]{nr, nc});
                }
            }
        }
        return out;
    }

    // ---------------- 逻辑帧（爆裂 → 下落 → 结算检测） ----------------

    @Override
    protected void onTick() {
        long now = System.currentTimeMillis();
        for (int i = popping.size() - 1; i >= 0; i--) {
            if (now >= popping.get(i)[2]) {
                long[] p = popping.remove(i);
                grid[(int) p[0]][(int) p[1]] = -1;
            }
        }
        boolean busy = !popping.isEmpty();
        if (!falling.isEmpty()) {
            if (now - fallStart >= FALL_MS) falling.clear();
            else busy = true;
        }
        if (busy) {
            dirty();
            return;
        }
        stopLoop();
        afterSettle();
        dirty();
    }

    /** 消除动画全部结束：结算塌落动画、全清奖励与死局检测。 */
    private void afterSettle() {
        if (over || settling) return;
        collapse();
        boolean full = isEmptyBoard();
        if (full) {
            score += 200;
            host.setScore(score);
            popup("+200 全清！", lw / 2f, lh / 2f, 0xFFFFC24B, 24);
            beep(3);
        }
        if (full || !hasMoves()) {
            settling = true; // 拦截输入，缓冲一拍再结算，让玩家看清局面
            popup(full ? "完美清盘！" : "没有可消的组合了", lw / 2f, lh / 2f - 34, 0xFFECECF1, 15);
            postDelayedSafe(() -> {
                over = true;
                host.gameOver(score);
            }, 900);
        }
        dirty();
    }

    private boolean isEmptyBoard() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                if (grid[r][c] >= 0) return false;
        return true;
    }

    /** 计算塌落并生成平滑下落动画（纵向落底 + 空列左移）。 */
    private void collapse() {
        // 每列纵向压实
        int[][] tmp = new int[N][N];        for (int c = 0; c < N; c++) {
            int write = N - 1;
            for (int r = N - 1; r >= 0; r--) {
                if (grid[r][c] >= 0) {
                    tmp[write][c] = grid[r][c];
                    write--;
                }
            }
        }
        // 非空列左移映射
        int[] colMap = new int[N];
        int wc = 0;
        for (int c = 0; c < N; c++) {
            boolean empty = true;
            for (int r = 0; r < N; r++) if (tmp[r][c] >= 0) empty = false;
            colMap[c] = empty ? -1 : wc++;
        }
        // 应用到新盘并记录位移
        int[][] ng = new int[N][N];
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++)
                ng[r][c] = -1;
        falling.clear();
        for (int c = 0; c < N; c++) {
            if (colMap[c] < 0) continue;
            int write = N - 1;
            for (int r = N - 1; r >= 0; r--) {
                if (tmp[r][c] < 0) continue;
                int v = tmp[r][c];
                int nr = write--, nc = colMap[c];
                ng[nr][nc] = v;
                if (nr != r || nc != c) {
                    falling.add(new float[]{v, cellX(c), cellY(r), cellX(nc), cellY(nr),
                            System.currentTimeMillis(), nr * N + nc});
                }
            }
        }
        for (int r = 0; r < N; r++) System.arraycopy(ng[r], 0, grid[r], 0, N);
        if (!falling.isEmpty()) fallStart = System.currentTimeMillis();
    }

    private boolean hasMoves() {
        for (int r = 0; r < N; r++)
            for (int c = 0; c < N; c++) {
                int v = grid[r][c];
                if (v < 0) continue;
                if (c + 1 < N && grid[r][c + 1] == v) return true;
                if (r + 1 < N && grid[r + 1][c] == v) return true;
            }
        return false;
    }

    // ---------------- 渲染 ----------------

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF262A34);
        long now = System.currentTimeMillis();
        Set<Integer> animDest = new HashSet<>();
        for (float[] f : falling) animDest.add((int) f[6]);
        Set<Integer> poppingCells = new HashSet<>();
        for (long[] p : popping) poppingCells.add((int) p[0] * N + (int) p[1]);

        for (int r = 0; r < N; r++) {
            for (int col = 0; col < N; col++) {
                int v = grid[r][col];
                if (v < 0 || animDest.contains(r * N + col) || poppingCells.contains(r * N + col))
                    continue;
                rr(c, cellX(col) + 2, cellY(r) + 2, cellX(col) + CELL - 2, cellY(r) + CELL - 2,
                        8, PALETTE[v]);
            }
        }
        // 爆裂中的方块缩小淡出
        for (long[] p : popping) {
            float k = Math.max(0f, Math.min(1f, (p[2] - now) / (float) POP_MS));
            float l = cellX((int) p[1]), t = cellY((int) p[0]);
            float inset = (1f - k) * CELL / 2f;
            int color = (int) p[3];
            rr(c, l + 2 + inset, t + 2 + inset, l + CELL - 2 - inset, t + CELL - 2 - inset,
                    8, (((int) (0xFF * k)) << 24) | (0x00FFFFFF & color));
        }
        // 下落/左移中的方块（缓出插值）
        for (float[] f : falling) {
            float k = easeOut((now - f[5]) / (float) FALL_MS);
            float l = f[1] + (f[3] - f[1]) * k;
            float t = f[2] + (f[4] - f[2]) * k;
            rr(c, l + 2, t + 2, l + CELL - 2, t + CELL - 2, 8, PALETTE[(int) f[0]]);
        }
        label(c, "同色连成片，点大片更划算",
                lw / 2f, TOP + N * CELL + 26, 11, 0xFF8B8E9C, false);
    }
}
