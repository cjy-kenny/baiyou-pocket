package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g061 泡泡射击：点屏朝指向发射彩泡，同色三连即爆，爆空的下坠再加分；
 *  泡泡压过警戒线就结束。 */
public class BubbleShootGame extends CanvasGame {
    private static final int COLS = 8;
    private static final float R = 18, TOP = 90;
    private static final int COLORS = 4;
    private static final int[] PALETTE = {0xFFE8574F, 0xFF5B8DEF, 0xFF2FA36B, 0xFFFFC24B};

    // grid[r][c]：-1 空，否则颜色；奇数行右移半格
    private final int[][] grid = new int[12][COLS];
    private final Random rnd = new Random();
    private float shotX, shotY, shotVx, shotVy;
    private boolean flying;
    private int nextColor;
    private int score;
    private boolean dying;

    public BubbleShootGame() {
        super(360, 560);
        fillTop();
        nextColor = rnd.nextInt(COLORS);
    }

    private float cellX(int r, int c) {
        return 22 + c * (R * 2 + 1) + (r % 2 == 1 ? R : 0);
    }

    private float cellY(int r) {
        return TOP + r * (R * 2 - 5);
    }

    private void fillTop() {
        for (int r = 0; r < 4; r++)
            for (int c = 0; c < COLS; c++)
                grid[r][c] = rnd.nextInt(COLORS);
    }

    private void startRound() {
        for (int[] row : grid) java.util.Arrays.fill(row, -1);
        fillTop();
        score = 0;
        flying = false;
        dying = false;
        over = false;
        nextColor = rnd.nextInt(COLORS);
        host.setScore(0);
        startLoop(16);
        dirty();
    }

    @Override
    public void restartGame() {
        startRound();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(16);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || dying || flying || action != 0 || y > 430) return;
        float dx = x - 180, dy = y - 500;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 10) return;
        shotX = 180;
        shotY = 500;
        shotVx = dx / len * 11;
        shotVy = dy / len * 11;
        flying = true;
        beep(0);
        dirty();
    }

    @Override
    protected void onTick() {
        if (flying) {
            shotX += shotVx;
            shotY += shotVy;
            if (shotX < R + 6 || shotX > lw - R - 6) {
                shotVx = -shotVx;
                shotX = Math.max(R + 6, Math.min(lw - R - 6, shotX));
            }
            if (shotY <= TOP + R) {
                land();
                return;
            }
            // 撞上已有泡泡
            for (int r = 0; r < 12; r++)
                for (int c = 0; c < COLS; c++) {
                    if (grid[r][c] < 0) continue;
                    float bx = cellX(r, c), by = cellY(r);
                    float dx = shotX - bx, dy = shotY - by;
                    if (dx * dx + dy * dy < (R * 2 - 6) * (R * 2 - 6)) {
                        land();
                        return;
                    }
                }
            if (shotY > 560) {
                flying = false; // 飞出底线（防御）
            }
        }
        dirty();
    }

    /** 泡泡落格：吸附到最近的空格，检查消除。 */
    private void land() {
        flying = false;
        int bestR = -1, bestC = -1;
        float bestD = Float.MAX_VALUE;
        for (int r = 0; r < 12; r++)
            for (int c = 0; c < COLS; c++) {
                if (grid[r][c] >= 0) continue;
                if (r > 0 && grid[r - 1][c] < 0) continue;
                float dx = shotX - cellX(r, c), dy = shotY - cellY(r);
                float d = dx * dx + dy * dy;
                if (d < bestD) {
                    bestD = d;
                    bestR = r;
                    bestC = c;
                }
            }
        if (bestR < 0) {
            gameOverNow();
            return;
        }
        grid[bestR][bestC] = nextColor;
        nextColor = rnd.nextInt(COLORS);
        popFrom(bestR, bestC);
        dropFloaters();
        // 压线判定
        for (int c = 0; c < COLS; c++)
            if (grid[10][c] >= 0 || grid[11][c] >= 0) {
                gameOverNow();
                return;
            }
        dirty();
    }

    private void popFrom(int r, int c) {
        int color = grid[r][c];
        boolean[][] seen = new boolean[12][COLS];
        List<int[]> stack = new ArrayList<>();
        stack.add(new int[]{r, c});
        seen[r][c] = true;
        List<int[]> group = new ArrayList<>();
        while (!stack.isEmpty()) {
            int[] p = stack.remove(stack.size() - 1);
            group.add(p);
            int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
            for (int[] d : dirs) {
                int nr = p[0] + d[0], nc = p[1] + d[1];
                if (nr < 0 || nr >= 12 || nc < 0 || nc >= COLS || seen[nr][nc]) continue;
                if (grid[nr][nc] == color) {
                    seen[nr][nc] = true;
                    stack.add(new int[]{nr, nc});
                }
            }
        }
        if (group.size() >= 3) {
            int gain = group.size() * group.size() * 2;
            score += gain;
            host.setScore(score);
            beep(1);
            for (int[] p : group) {
                burst(cellX(p[0], p[1]), cellY(p[0]), PALETTE[color], 4, 3.5f);
                grid[p[0]][p[1]] = -1;
            }
            popup("+" + gain, cellX(r, c), cellY(r), 0xFFECECF1, 16);
        } else {
            beep(0);
        }
    }

    /** 清掉悬空泡泡。 */
    private void dropFloaters() {
        boolean[][] reach = new boolean[12][COLS];
        List<int[]> stack = new ArrayList<>();
        for (int c = 0; c < COLS; c++)
            if (grid[0][c] >= 0) {
                reach[0][c] = true;
                stack.add(new int[]{0, c});
            }
        int[][] dirs = {{0, -1}, {0, 1}, {-1, 0}, {1, 0}, {-1, -1}, {-1, 1}, {1, -1}, {1, 1}};
        while (!stack.isEmpty()) {
            int[] p = stack.remove(stack.size() - 1);
            for (int[] d : dirs) {
                int nr = p[0] + d[0], nc = p[1] + d[1];
                if (nr < 0 || nr >= 12 || nc < 0 || nc >= COLS || reach[nr][nc]) continue;
                if (grid[nr][nc] >= 0) {
                    reach[nr][nc] = true;
                    stack.add(new int[]{nr, nc});
                }
            }
        }
        int dropped = 0;
        for (int r = 0; r < 12; r++)
            for (int c = 0; c < COLS; c++)
                if (grid[r][c] >= 0 && !reach[r][c]) {
                    grid[r][c] = -1;
                    dropped++;
                }
        if (dropped > 0) {
            int gain = dropped * dropped * 3;
            score += gain;
            host.setScore(score);
            popup("塌落 +" + gain, lw / 2f, 300, 0xFFFFC24B, 16);
        }
    }

    private void gameOverNow() {
        dying = true;
        over = true;
        stopLoop();
        shake(300);
        beep(2);
        popup("泡泡压境了", lw / 2f, lh / 2f, 0xFFE8574F, 18);
        dirty();
        postDelayedSafe(() -> host.gameOver(score), 800);
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF181E2C);
        // 警戒线
        stroke.setColor(0x66E8574F);
        stroke.setStrokeWidth(2);
        c.drawLine(0, cellY(10), lw, cellY(10), stroke);
        for (int r = 0; r < 12; r++)
            for (int col = 0; col < COLS; col++) {
                if (grid[r][col] < 0) continue;
                fill.setColor(PALETTE[grid[r][col]]);
                c.drawCircle(cellX(r, col), cellY(r), R - 2, fill);
                fill.setColor(0x55FFFFFF);
                c.drawCircle(cellX(r, col) - R * 0.3f, cellY(r) - R * 0.3f, R * 0.25f, fill);
            }
        if (flying) {
            fill.setColor(PALETTE[nextColor]);
            c.drawCircle(shotX, shotY, R - 2, fill);
        }
        // 炮台
        fill.setColor(0xFF3A4266);
        c.drawCircle(180, 500, 22, fill);
        fill.setColor(PALETTE[nextColor]);
        c.drawCircle(180, 500, 13, fill);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
