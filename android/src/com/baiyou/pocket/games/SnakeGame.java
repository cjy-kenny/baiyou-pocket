package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayDeque;

/** g002 光速贪吃蛇：15×21 网格，滑动转向。
 *  每吃一粒提速一档（150ms → 70ms/帧）；每吃 5 粒出现一枚金色限时奖励（+30 分，
 *  约 6 秒内不吃即消失，临期急促闪烁）；撞墙/撞己震屏后结算。 */
public class SnakeGame extends CanvasGame {
    private static final int COLS = 15, ROWS = 21, CELL = 24;
    private static final int PADX = 16, PADY = 16;
    private static final long GOLD_LIFE = 6500;

    private final ArrayDeque<int[]> body = new ArrayDeque<>(); // {col,row} 头在首
    private final boolean[][] occupied = new boolean[COLS][ROWS];
    private final int[] dir = {1, 0};
    private final int[] pending = {1, 0};
    private final float[] lastTouch = new float[2];
    private final int[] food = {10, 10};
    private boolean foodGold;
    private long foodDeadline;
    private int score, foods;

    public SnakeGame() {
        super(392, 536);
    }

    private long speed() {
        return Math.max(70, 150 - foods * 3);
    }

    private void startRound() {
        body.clear();
        for (boolean[] row : occupied) java.util.Arrays.fill(row, false);
        int cy = ROWS / 2;
        for (int i = 0; i < 3; i++) {
            int[] p = {7 - i, cy};
            body.addLast(p);
            occupied[p[0]][p[1]] = true;
        }
        dir[0] = 1; dir[1] = 0;
        pending[0] = 1; pending[1] = 0;
        score = 0;
        foods = 0;
        over = false;
        spawnFood();
        host.setScore(0);
        startLoop(150);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(speed());
    }

    /** 每吃 5 粒后刷新出一枚金色奖励；普通食物永久存在。 */
    private void spawnFood() {
        int free = COLS * ROWS - body.size();
        if (free <= 0) return;
        int idx = rnd.nextInt(free);
        for (int c = 0; c < COLS; c++)
            for (int r = 0; r < ROWS; r++) {
                if (occupied[c][r]) continue;
                if (idx-- == 0) {
                    food[0] = c;
                    food[1] = r;
                    foodGold = foods > 0 && foods % 5 == 0;
                    foodDeadline = System.currentTimeMillis() + GOLD_LIFE;
                    return;
                }
            }
    }

    // ---------------- 交互：滑动转向 ----------------

    @Override
    protected void touch(float x, float y, int action) {
        if (over) return;
        if (action == 0) {
            lastTouch[0] = x;
            lastTouch[1] = y;
            return;
        }
        if (action == 1 || action == 2) {
            float dx = x - lastTouch[0], dy = y - lastTouch[1];
            if (Math.max(Math.abs(dx), Math.abs(dy)) < 18) return;
            int ndx = Math.abs(dx) > Math.abs(dy) ? (dx > 0 ? 1 : -1) : 0;
            int ndy = ndx == 0 ? (dy > 0 ? 1 : -1) : 0;
            // 不允许直接掉头
            if (!(ndx == -dir[0] && ndy == -dir[1])) {
                pending[0] = ndx;
                pending[1] = ndy;
            }
            lastTouch[0] = x;
            lastTouch[1] = y;
        }
    }

    // ---------------- 逻辑帧 ----------------

    @Override
    protected void onTick() {
        setNextTick(speed());
        // 应用待定方向（触摸层已拦截直接掉头）
        if (pending[0] != -dir[0] || pending[1] != -dir[1]) {
            dir[0] = pending[0];
            dir[1] = pending[1];
        }
        // 金色奖励超时消失
        if (foodGold && System.currentTimeMillis() > foodDeadline) {
            burst(foodX(), foodY(), 0xFFFFC24B, 8, 3.5f);
            spawnFood();
        }

        int[] head = body.peekFirst();
        int nc = head[0] + dir[0], nr = head[1] + dir[1];
        if (nc < 0 || nc >= COLS || nr < 0 || nr >= ROWS || occupied[nc][nr]) {
            die(head);
            return;
        }
        int[] nh = new int[]{nc, nr};
        body.addFirst(nh);
        occupied[nc][nr] = true;

        if (nc == food[0] && nr == food[1]) {
            int gain = foodGold ? 30 : 10;
            score += gain;
            foods++;
            host.setScore(score);
            popup("+" + gain, foodX(), foodY(), foodGold ? 0xFFFFC24B : 0xFFFFFFFF, foodGold ? 24 : 18);
            burst(foodX(), foodY(), foodGold ? 0xFFFFC24B : 0xFFFF6B6B, 12, 5f);
            beep(1);
            if (foodGold) beep(3);
            spawnFood();
        } else {
            int[] tail = body.removeLast();
            occupied[tail[0]][tail[1]] = false;
        }
        dirty();
    }

    private float foodX() {
        return PADX + food[0] * CELL + CELL / 2f;
    }

    private float foodY() {
        return PADY + food[1] * CELL + CELL / 2f;
    }

    /** 死亡：震屏 + 碎裂特效，缓冲一拍让玩家看清死因再结算。 */
    private void die(int[] head) {
        over = true;
        stopLoop();
        shake(360);
        float hx = PADX + head[0] * CELL + CELL / 2f;
        float hy = PADY + head[1] * CELL + CELL / 2f;
        burst(hx, hy, 0xFFE8574F, 18, 7f);
        popup("撞住了！", lw / 2f, lh / 2f, 0xFFE8574F, 22);
        beep(2);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 750);
    }

    // ---------------- 渲染 ----------------

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1E222B);
        rr(c, PADX, PADY, PADX + COLS * CELL, PADY + ROWS * CELL, 10, 0xFF262B36);
        // 棋盘格暗纹
        for (int col = 0; col < COLS; col++)
            for (int r = 0; r < ROWS; r++)
                if ((col + r) % 2 == 0)
                    rr(c, PADX + col * CELL, PADY + r * CELL,
                            PADX + (col + 1) * CELL, PADY + (r + 1) * CELL, 0, 0xFF2A303C);

        // 食物（普通红色脉动 / 金色限时闪烁）
        long now = System.currentTimeMillis();
        float cx = foodX(), cy = foodY();
        if (foodGold) {
            long left = foodDeadline - now;
            boolean blinkFast = left < 2200;
            boolean visible = blinkFast
                    ? (now / 160) % 2 == 0
                    : (now / 420) % 2 == 0 || left > GOLD_LIFE - 2600;
            if (visible) {
                fill.setColor(0xFFFFC24B);
                c.drawCircle(cx, cy, 8.5f, fill);
                stroke.setColor(0x88FFFFFF);
                stroke.setStrokeWidth(2);
                c.drawCircle(cx, cy, 12, stroke);
            }
        } else {
            long t = now % 800;
            float pulse = 0.5f + 0.5f * (float) Math.sin(t / 800.0 * Math.PI * 2);
            fill.setColor(0xFFFF6B6B);
            c.drawCircle(cx, cy, 7 + pulse * 2, fill);
        }

        // 蛇身
        boolean head = true;
        for (int[] p : body) {
            float l = PADX + p[0] * CELL, tp = PADY + p[1] * CELL;
            rr(c, l + 1.5f, tp + 1.5f, l + CELL - 1.5f, tp + CELL - 1.5f, 7,
                    head ? 0xFF7CFFB2 : 0xFF3DBFA8);
            if (head) {
                // 头部眼睛（随方向）
                fill.setColor(0xFF14342B);
                float ox = dir[1] * 4.5f, oy = dir[0] * 4.5f;   // 垂直于前进方向偏移
                float fx = dir[0] * 4f, fy = dir[1] * 4f;       // 沿前进方向偏移
                c.drawCircle(cxOf(l) + fx + ox, cyOf(tp) + fy + oy, 2.2f, fill);
                c.drawCircle(cxOf(l) + fx - ox, cyOf(tp) + fy - oy, 2.2f, fill);
                head = false;
            }
        }
        label(c, "滑动屏幕转向" + (foodGold ? " · 金色奖励限时！ " + Math.max(0, (int) ((foodDeadline - System.currentTimeMillis()) / 1000)) + "s" : ""),
                lw / 2f, lh - 14, 12, 0xFF8B8E9C, false);
    }

    private float cxOf(float l) {
        return l + CELL / 2f;
    }

    private float cyOf(float t) {
        return t + CELL / 2f;
    }
}
