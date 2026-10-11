package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/** g005 数字合并：4×4 滑动合并（经典 merge 玩法）。
 *  方块滑动/被合并有完整动画（滑入→淡出→合并弹跳→新生弹入）；
 *  每步合并得分等于合并后数字之和；无路可走时画面渐暗后结算；达成 2048 后可继续冲分。 */
public class NumMergeGame extends CanvasGame {
    private static final int PAD = 12, GAP = 8, CELL = 80, TOP = 16;
    private static final long SLIDE_MS = 120, POP_MS = 150;

    private final int[] board = new int[16];   // 0 = 空
    private final Random rnd = new Random();
    private final float[] downPt = new float[2];
    private int score;
    private boolean winShown;

    /** 滑动动画：{value, fx, fy, tx, ty, destIdx}（配 animStart），ghost 用 alpha 渐隐 */
    private List<float[]> slides = new ArrayList<>();
    private List<float[]> ghosts = new ArrayList<>();
    /** 合并目标格在滑动期间显示的旧值 */
    private Map<Integer, Integer> mergedShow = new HashMap<>();
    /** 本步发生合并的目标格（finishMove 弹跳用） */
    private final List<Integer> mergedTargets = new ArrayList<>();
    private List<long[]> mergePops = new ArrayList<>();   // {idx, start}
    private List<long[]> spawnPops = new ArrayList<>();   // {idx, start}
    private long animStart = -1, overAt;

    public NumMergeGame() {
        super(368, 412);
    }

    private float cellX(int col) {
        return PAD + col * (CELL + GAP);
    }

    private float cellY(int row) {
        return TOP + row * (CELL + GAP);
    }

    private void startRound() {
        java.util.Arrays.fill(board, 0);
        score = 0;
        over = false;
        winShown = false;
        slides.clear();
        ghosts.clear();
        mergedShow.clear();
        mergedTargets.clear();
        mergePops.clear();
        spawnPops.clear();
        animStart = -1;
        spawn();
        spawn();
        host.setScore(0);
        startLoop(16); // 驱动开局生成弹入动画，结束后自动停
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    private void spawn() {
        int empty = 0;
        for (int v : board) if (v == 0) empty++;
        if (empty == 0) return;
        int idx = rnd.nextInt(empty);
        for (int i = 0; i < 16; i++) {
            if (board[i] != 0) continue;
            if (idx-- == 0) {
                board[i] = rnd.nextInt(10) < 9 ? 2 : 4;
                spawnPops.add(new long[]{i, System.currentTimeMillis()});
                return;
            }
        }
    }

    /** dir：0上 1下 2左 3右 → (行 i, 位 j) 的棋盘下标。 */
    private int idxOf(int dir, int i, int j) {
        switch (dir) {
            case 0: return j * 4 + i;
            case 1: return (3 - j) * 4 + i;
            case 2: return i * 4 + j;
            default: return i * 4 + (3 - j);
        }
    }

    private boolean animating() {
        return animStart >= 0 && System.currentTimeMillis() - animStart < SLIDE_MS;
    }

    // ---------------- 交互：滑动四向 ----------------

    @Override
    protected void touch(float x, float y, int action) {
        if (over || animating()) return;
        if (action == 0) {
            downPt[0] = x;
            downPt[1] = y;
            return;
        }
        if (action == 1) {
            float dx = x - downPt[0], dy = y - downPt[1];
            if (Math.max(Math.abs(dx), Math.abs(dy)) < 26) return;
            int dir;
            if (Math.abs(dx) > Math.abs(dy)) dir = dx > 0 ? 3 : 2;
            else dir = dy > 0 ? 1 : 0;
            move(dir);
        }
    }

    // ---------------- 合并逻辑（带动画映射） ----------------

    private void move(int dir) {
        List<float[]> mv = new ArrayList<>();
        List<float[]> gh = new ArrayList<>();
        Map<Integer, Integer> show = new HashMap<>();
        int[] nb = new int[16];
        int gained = 0;
        float sumMx = 0, sumMy = 0;
        int mergeCount = 0;

        for (int i = 0; i < 4; i++) {
            int[] line = new int[4];
            int[] fromIdx = new int[4];
            for (int j = 0; j < 4; j++) {
                fromIdx[j] = idxOf(dir, i, j);
                line[j] = board[fromIdx[j]];
            }
            int[] target = new int[4];
            boolean[] slotMerged = new boolean[4];
            int w = 0;
            for (int j = 0; j < 4; j++) {
                int v = line[j];
                if (v == 0) continue;
                if (w > 0 && target[w - 1] == v && !slotMerged[w - 1]) {
                    // 滑入前一个槽并消失（ghost），槽位翻倍
                    target[w - 1] = v * 2;
                    slotMerged[w - 1] = true;
                    gained += v * 2;
                    int dst = idxOf(dir, i, w - 1);
                    gh.add(new float[]{v, cellX(fromIdx[j] % 4), cellY(fromIdx[j] / 4),
                            cellX(dst % 4), cellY(dst / 4), dst});
                    show.put(dst, v);
                    mergedTargets.add(dst);
                    sumMx += cellX(dst % 4) + CELL / 2f;
                    sumMy += cellY(dst / 4) + CELL / 2f;
                    mergeCount++;
                } else {
                    target[w] = v;
                    int dst = idxOf(dir, i, w);
                    if (dst != fromIdx[j]) {
                        mv.add(new float[]{v, cellX(fromIdx[j] % 4), cellY(fromIdx[j] / 4),
                                cellX(dst % 4), cellY(dst / 4), dst});
                    }
                    w++;
                }
            }
            for (int j = 0; j < 4; j++) nb[idxOf(dir, i, j)] = target[j];
        }

        boolean changed = false;
        for (int i = 0; i < 16; i++) if (nb[i] != board[i]) changed = true;
        if (!changed) return;

        for (int i = 0; i < 16; i++) board[i] = nb[i];
        score += gained;
        host.setScore(score);
        beep(gained > 0 ? 1 : 0);
        if (mergeCount > 0) {
            popup("+" + gained, sumMx / mergeCount, sumMy / mergeCount - 10,
                    0xFF7CFFB2, 22);
        }

        slides = mv;
        ghosts = gh;
        mergedShow = show;
        animStart = System.currentTimeMillis();
        startLoop(16);
        dirty();
    }

    /** 滑动动画结束：生成新块、合并弹跳、里程碑与死局检测。 */
    private void finishMove() {
        slides.clear();
        ghosts.clear();
        mergedShow.clear();
        animStart = -1;
        spawn();
        long now = System.currentTimeMillis();
        for (int idx : mergedTargets) mergePops.add(new long[]{idx, now});
        mergedTargets.clear();

        if (!winShown) {
            for (int v : board) {
                if (v >= 2048) {
                    winShown = true;
                    host.toast("达成 2048！继续挑战更高分");
                    popup("2048！", lw / 2f, lh / 2f - 40, 0xFFEDC22E, 30);
                    beep(3);
                }
            }
        }
        if (!canMove()) {
            over = true;
            overAt = now;
            beep(2);
            postDelayedSafe(() -> host.gameOver(score), 950);
        }
        dirty();
    }

    @Override
    protected void onTick() {
        long now = System.currentTimeMillis();
        if (animStart >= 0) {
            if (now - animStart >= SLIDE_MS) finishMove();
            else dirty();
            return;
        }
        boolean busy = false;
        Iterator<long[]> mi = mergePops.iterator();
        while (mi.hasNext()) {
            if (now - mi.next()[1] > POP_MS) mi.remove();
            else busy = true;
        }
        Iterator<long[]> si = spawnPops.iterator();
        while (si.hasNext()) {
            if (now - si.next()[1] > POP_MS) si.remove();
            else busy = true;
        }
        if (busy) dirty();
        else stopLoop();
    }

    private boolean canMove() {
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < 4; c++) {
                int v = board[r * 4 + c];
                if (v == 0) return true;
                if (c + 1 < 4 && board[r * 4 + c + 1] == v) return true;
                if (r + 1 < 4 && board[(r + 1) * 4 + c] == v) return true;
            }
        return false;
    }

    // ---------------- 渲染 ----------------

    private static int tileColor(int v) {
        switch (v) {
            case 2: return 0xFFEDE6DB;
            case 4: return 0xFFEAE0C8;
            case 8: return 0xFFF2B179;
            case 16: return 0xFFF59563;
            case 32: return 0xFFF67C5F;
            case 64: return 0xFFF65E3B;
            case 128: return 0xFFEDCF72;
            case 256: return 0xFFEDCC61;
            case 512: return 0xFFEDC850;
            case 1024: return 0xFFEDC53F;
            case 2048: return 0xFFEDC22E;
            default: return v == 0 ? 0xFF333846 : 0xFF3C3A32;
        }
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF20242E);
        rr(c, PAD, TOP, PAD + 4 * CELL + 3 * GAP, TOP + 4 * CELL + 3 * GAP, 10, 0xFF2B2F3A);
        long now = System.currentTimeMillis();
        boolean anim = animStart >= 0;

        // 空槽
        for (int r = 0; r < 4; r++)
            for (int col = 0; col < 4; col++) {
                rr(c, cellX(col), cellY(r), cellX(col) + CELL, cellY(r) + CELL, 7, 0xFF333846);
            }

        // 静态方块（滑动中的来源/目标格与合并目标格单独处理）
        for (int r = 0; r < 4; r++) {
            for (int col = 0; col < 4; col++) {
                int idx = r * 4 + col;
                int v = board[idx];
                if (v == 0) continue;
                float scale = 1f;
                if (anim) {
                    if (mergedShow.containsKey(idx)) {
                        drawTile(c, mergedShow.get(idx), cellX(col), cellY(r), 1f);
                        continue;
                    }
                    if (isSlideDest(idx)) continue;
                } else {
                    scale = popScale(mergePops, idx, now, true);
                    if (scale == 1f) scale = popScale(spawnPops, idx, now, false);
                }
                drawTile(c, v, cellX(col), cellY(r), scale);
            }
        }

        if (anim) {
            float k = easeOut((now - animStart) / (float) SLIDE_MS);
            for (float[] s : slides) {
                drawTile(c, (int) s[0],
                        s[1] + (s[3] - s[1]) * k, s[2] + (s[4] - s[2]) * k, 1f);
            }
            for (float[] g : ghosts) {
                int alpha = (int) (255 * (1 - k));
                drawTileAlpha(c, (int) g[0],
                        g[1] + (g[3] - g[1]) * k, g[2] + (g[4] - g[2]) * k, alpha);
            }
        }

        // 死局渐暗
        if (over) {
            float k = Math.min(1f, (now - overAt) / 500f);
            fill.setColor(((int) (0xB3 * k)) << 24);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
            label(c, "无路可走了", lw / 2f, lh / 2f - 8, 26, 0xFFECECF1, true);
        }

        label(c, "往哪滑，哪边合",
                lw / 2f, lh - 18, 12, 0xFF8B8E9C, false);
    }

    private boolean isSlideDest(int idx) {
        for (float[] s : slides) if ((int) s[5] == idx) return true;
        return false;
    }

    private float popScale(List<long[]> pops, int idx, long now, boolean mergeStyle) {
        for (long[] p : pops) {
            if ((int) p[0] != idx) continue;
            float t = Math.min(1f, (now - p[1]) / (float) POP_MS);
            return mergeStyle ? 1.14f - 0.14f * t : 0.35f + 0.65f * easeOut(t);
        }
        return 1f;
    }

    private void drawTile(Canvas c, int v, float l, float t, float scale) {
        drawTileAlpha(c, v, l, t, 255, scale);
    }

    private void drawTileAlpha(Canvas c, int v, float l, float t, int alpha) {
        drawTileAlpha(c, v, l, t, alpha, 1f);
    }

    private void drawTileAlpha(Canvas c, int v, float l, float t, int alpha, float scale) {
        float cx = l + CELL / 2f, cy = t + CELL / 2f;
        float half = CELL / 2f * scale;
        int bg = (alpha << 24) | (0x00FFFFFF & tileColor(v));
        fill.setColor(bg);
        c.drawRoundRect(new android.graphics.RectF(cx - half, cy - half, cx + half, cy + half),
                7 * scale, 7 * scale, fill);
        if (v == 0) return;
        boolean dark = v == 2 || v == 4;
        String s = String.valueOf(v);
        float size = (s.length() >= 4 ? 26 : (s.length() == 3 ? 32 : 38)) * scale;
        int tc = ((alpha) << 24) | (0x00FFFFFF & (dark ? 0xFF5A5347 : 0xFFF9F6F2));
        label(c, s, cx, cy, size, tc, true);
    }
}
