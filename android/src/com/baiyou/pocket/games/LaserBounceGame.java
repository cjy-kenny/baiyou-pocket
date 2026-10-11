package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g089 激光折角：点镜面旋转 45°，让激光经反射命中右上角的目标；
 *  三关谜面固定，关卡越后镜越多。 */
public class LaserBounceGame extends CanvasGame {
    // 关卡：镜子 {col,row,type}（3×3 网格），type: 0=/ 1=\（按反射规则均可解）
    private static final int[][][] LEVELS = {
            {{2, 1, 0}},
            {{1, 1, 1}, {1, 2, 1}, {2, 2, 0}},
            {{1, 1, 0}, {1, 0, 1}, {2, 2, 0}},
    };

    private static final float CELL = 84, LEFT = 60, TOP = 140;

    private int level;
    private final int[][] mirrors = new int[9][3]; // {col,row,type} 有效条目
    private int mirrorCount;
    private int shownScore;
    private boolean settling;

    public LaserBounceGame() {
        super(360, 560);
        loadLevel(0);
    }

    private void loadLevel(int lv) {
        level = lv;
        mirrorCount = LEVELS[lv].length;
        for (int i = 0; i < mirrorCount; i++) {
            mirrors[i][0] = LEVELS[lv][i][0];
            mirrors[i][1] = LEVELS[lv][i][1];
            mirrors[i][2] = LEVELS[lv][i][2];
        }
        // 开局不应该是答案状态：把第一面镜转到错误朝向，让玩家有得拧
        if (solvedNow()) mirrors[0][2] = (mirrors[0][2] + 1) % 2;
        settling = false;
        over = false;
        dirty();
    }

    private void startGame() {
        shownScore = 0;
        loadLevel(0);
        host.setScore(0);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    private int mirrorAt(int col, int row) {
        for (int i = 0; i < mirrorCount; i++)
            if (mirrors[i][0] == col && mirrors[i][1] == row) return i;
        return -1;
    }

    private float cellX(int c) {
        return LEFT + c * CELL;
    }

    private float cellY(int r) {
        return TOP + r * CELL;
    }

    /** 从激光源出发追踪光路，返回是否命中目标（右上角）。 */
    private boolean solvedNow() {
        int dir = 1; // 0上 1右 2下 3左
        int col = -1, row = 1; // 从左缘第 2 行射入
        for (int step = 0; step < 12; step++) {
            col += dir == 1 ? 1 : dir == 3 ? -1 : 0;
            row += dir == 0 ? -1 : dir == 2 ? 1 : 0;
            if (col < 0 || col > 2 || row < 0 || row > 2) return false;
            if (col == 2 && row == 0) return dir == 1 || dir == 0; // 目标格
            int m = mirrorAt(col, row);
            if (m >= 0) {
                int type = mirrors[m][2];
                boolean slash = type == 0; // /
                switch (dir) {
                    case 0: dir = slash ? 3 : 1; break;
                    case 1: dir = slash ? 0 : 2; break;
                    case 2: dir = slash ? 1 : 3; break;
                    case 3: dir = slash ? 2 : 0; break;
                }
            }
        }
        return false;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        int c = (int) ((x - LEFT) / CELL), r = (int) ((y - TOP) / CELL);
        if (c < 0 || c > 2 || r < 0 || r > 2) return;
        int m = mirrorAt(c, r);
        if (m < 0) return;
        mirrors[m][2] = (mirrors[m][2] + 1) % 2;
        beep(0);
        if (solvedNow()) {
            shownScore += 300;
            host.setScore(shownScore);
            settling = true;
            popup("激光贯通！ +300", lw / 2f, 110, 0xFF7CFFB2, 19);
            burst(cellX(2), cellY(0), 0xFF7CFFB2, 16, 6f);
            beep(3);
            postDelayedSafe(() -> {
                settling = false;
                if (over) return;
                if (level >= LEVELS.length - 1) {
                    over = true;
                    stopLoop();
                    popup("光学大师！", lw / 2f, lh / 2f, 0xFFFFC24B, 22);
                    dirty();
                    postDelayedSafe(() -> host.gameOver(shownScore), 900);
                } else {
                    loadLevel(level + 1);
                    dirty();
                }
            }, 850);
        }
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF101820);
        label(c, "第 " + (level + 1) + "/" + LEVELS.length + " 关 · 点镜面旋转",
                lw / 2f, 100, 14, 0xFF9AA0AC, false);
        // 网格
        stroke.setColor(0x444A5568);
        stroke.setStrokeWidth(1);
        for (int i = 0; i <= 3; i++) {
            c.drawLine(cellX(0) - 20, cellY(i), cellX(2) + 20, cellY(i), stroke);
            c.drawLine(cellX(i), cellY(0) - 20, cellX(i), cellY(2) + 20, stroke);
        }
        // 激光源
        fill.setColor(0xFFE8574F);
        c.drawCircle(LEFT - 30, cellY(1), 10, fill);
        // 目标
        fill.setColor(0xFF7CFFB2);
        c.drawCircle(cellX(2), cellY(0), 14, fill);
        label(c, "目标", cellX(2), cellY(0) - 26, 11, 0xFF9ACDAA, false);
        // 光路
        boolean solved = solvedNow();
        if (solved) {
            stroke.setColor(0xFF7CFFB2);
            stroke.setStrokeWidth(4);
            c.drawLine(LEFT - 30, cellY(1), cellX(2), cellY(0), stroke);
        }
        // 镜面
        for (int i = 0; i < mirrorCount; i++) {
            float mx = cellX(mirrors[i][0]), my = cellY(mirrors[i][1]);
            stroke.setColor(0xFFB9C4E0);
            stroke.setStrokeWidth(5);
            if (mirrors[i][2] == 0) {
                c.drawLine(mx - 26, my + 26, mx + 26, my - 26, stroke); // /
            } else {
                c.drawLine(mx - 26, my - 26, mx + 26, my + 26, stroke); // \
            }
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
