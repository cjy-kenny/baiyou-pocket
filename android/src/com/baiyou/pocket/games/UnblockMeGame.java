package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.Random;

/** g070 挪车出库：水平/垂直车挪位，给红色小车让出右边出口；
 *  四关逐级难，步数越少分越高。 */
public class UnblockMeGame extends CanvasGame {
    // 车：{row, col, len, 横?}；关卡手工设计（6×6）
    private static final int[][][] LEVELS = {
            // 红: {2,1,2,true}
            {{2, 1, 2, 1}, {0, 3, 3, 0}, {4, 0, 2, 1}, {3, 4, 2, 0}, {1, 0, 2, 1}},
            {{2, 1, 2, 1}, {0, 0, 3, 1}, {1, 4, 2, 0}, {4, 2, 3, 1}, {5, 0, 2, 1}, {0, 4, 2, 0}},
            {{2, 0, 2, 1}, {0, 2, 3, 0}, {3, 3, 2, 0}, {5, 0, 2, 1}, {1, 5, 3, 0}, {4, 0, 2, 0}},
            {{2, 1, 2, 1}, {0, 0, 2, 0}, {1, 2, 3, 1}, {3, 4, 2, 1}, {4, 0, 3, 0}, {5, 3, 2, 1}, {0, 4, 2, 1}},
    };

    private int level;
    private int[][] cars;
    private int selCar = -1;
    private int moves, shownScore;
    private float downX = -1, downY = -1;
    private boolean settling;

    public UnblockMeGame() {
        super(360, 560);
        loadLevel(0);
    }

    private void loadLevel(int lv) {
        level = lv;
        cars = new int[LEVELS[lv].length][];
        for (int i = 0; i < cars.length; i++) cars[i] = LEVELS[lv][i].clone();
        moves = 0;
        selCar = -1;
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

    private float cell() {
        return (lw - 24) / 6f;
    }

    private float boardL() {
        return 12;
    }

    private float boardT() {
        return 110;
    }

    private boolean occupied(int r, int c, int ignore) {
        if (r < 0 || r >= 6 || c < 0 || c >= 6) return true;
        for (int i = 0; i < cars.length; i++) {
            if (i == ignore) continue;
            int[] car = cars[i];
            int len = car[2];
            if (car[3] == 1) {
                if (car[0] == r && c >= car[1] && c < car[1] + len) return true;
            } else {
                if (car[1] == c && r >= car[0] && r < car[0] + len) return true;
            }
        }
        return false;
    }

    /** 红车能否出库（右侧到出口无阻挡且红车在出口行）。 */
    private boolean escaped() {
        int[] red = cars[0];
        if (red[0] != 2) return false;
        int end = red[1] + red[2];
        for (int c = end; c < 6; c++) if (occupied(2, c, 0)) return false;
        return true;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling) return;
        float cs = cell();
        if (action == 0) {
            downX = x;
            downY = y;
            // 点车
            int hitCar = -1;
            for (int i = 0; i < cars.length; i++) {
                int[] car = cars[i];
                float cx = boardL() + car[1] * cs, cy = boardT() + car[0] * cs;
                float w = car[3] == 1 ? car[2] * cs : cs;
                float h = car[3] == 1 ? cs : car[2] * cs;
                if (hit(x, y, cx, cy, w, h)) hitCar = i;
            }
            selCar = hitCar;
            if (selCar >= 0) beep(0);
            dirty();
            return;
        }
        if (action == 2 && selCar >= 0 && downX >= 0) {
            float dx = x - downX, dy = y - downY;
            if (Math.abs(dx) < cs / 3f && Math.abs(dy) < cs / 3f) return;
            int d;
            if (Math.abs(dx) > Math.abs(dy)) d = dx > 0 ? 1 : 0;
            else d = dy > 0 ? 3 : 2;
            downX = x;
            downY = y;
            tryMove(d);
        }
    }

    private void tryMove(int d) {
        // d: 0左 1右 2上 3下
        if (selCar < 0 || settling) return;
        int[] car = cars[selCar];
        boolean horiz = car[3] == 1;
        if (horiz && d >= 2) return;
        if (!horiz && d < 2) return;
        int step = d == 1 || d == 3 ? 1 : -1;
        int nr = car[0], nc = car[1];
        if (horiz) nc += step;
        else nr += step;
        if (horiz) {
            int check = step == 1 ? car[1] + car[2] : car[1] - 1;
            if (occupied(car[0], check, selCar)) {
                beep(2);
                return;
            }
        } else {
            int check = step == 1 ? car[0] + car[2] : car[0] - 1;
            if (occupied(check, car[1], selCar)) {
                beep(2);
                return;
            }
        }
        car[0] = nr;
        car[1] = nc;
        moves++;
        beep(0);
        if (escaped()) {
            int gained = Math.max(150, 600 - moves * 15);
            shownScore += gained;
            host.setScore(shownScore);
            settling = true;
            stopLoop();
            beep(3);
            popup("出库！ +" + gained, lw / 2f, lh / 2f, 0xFF7CFFB2, 20);
            burst(boardL() + 5.5f * cell(), boardT() + 2.5f * cell(), 0xFF7CFFB2, 20, 7f);
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
            popup("车场大师！", lw / 2f, lh / 2f, 0xFFFFC24B, 22);
            dirty();
            postDelayedSafe(() -> host.gameOver(shownScore), 900);
            return;
        }
        popup("第 " + (level + 2) + " 关", lw / 2f, lh / 2f - 30, 0xFFECECF1, 16);
        loadLevel(level + 1);
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF241E1C);
        float cs = cell();
        float bl = boardL(), bt = boardT();
        // 地板
        rr(c, bl - 4, bt - 4, bl + 6 * cs + 4, bt + 6 * cs + 4, 8, 0xFF3A302C);
        for (int r = 0; r < 6; r++)
            for (int col = 0; col < 6; col++)
                rr(c, bl + col * cs + 2, bt + r * cs + 2, bl + (col + 1) * cs - 2,
                        bt + (r + 1) * cs - 2, 3, 0xFF2E2724);
        // 出口标记
        fill.setColor(0x667CFFB2);
        rr(c, bl + 6 * cs, bt + 2 * cs + 4, bl + 6 * cs + 10, bt + 3 * cs - 4, 2, 0x667CFFB2);
        // 车
        for (int i = 0; i < cars.length; i++) {
            int[] car = cars[i];
            boolean red = i == 0;
            int color = red ? 0xFFE8574F : (car[3] == 1 ? 0xFF5B8DEF : 0xFF2FA36B);
            float cx = bl + car[1] * cs + 3, cy = bt + car[0] * cs + 3;
            float w = (car[3] == 1 ? car[2] * cs : cs) - 6;
            float h = (car[3] == 1 ? cs : car[2] * cs) - 6;
            rr(c, cx, cy, cx + w, cy + h, 6, color);
            // 车窗
            fill.setColor(0x66FFFFFF);
            rr(c, cx + w * 0.3f, cy + h * 0.3f, cx + w * 0.7f, cy + h * 0.7f, 3, 0x66FFFFFF);
            if (i == selCar) {
                stroke.setColor(0xFFFFC24B);
                stroke.setStrokeWidth(2);
                rf.set(cx - 2, cy - 2, cx + w + 2, cy + h + 2);
                c.drawRoundRect(rf, 6, 6, stroke);
            }
        }
        label(c, "第 " + (level + 1) + "/" + LEVELS.length + " 关 · " + moves + " 步",
                lw / 2f, 80, 14, 0xFFC9B896, false);
        label(c, "点车选中 → 滑动方向挪车", lw / 2f, 105, 11, 0xFF8B8E9C, false);
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
