package com.baiyou.pocket.game;

import android.graphics.Canvas;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/** 翻牌对战家族基类（人机）：翻两张同图案即收归己方并继续行动，否则换人；
 *  AI 带可调"记忆力"（以概率记住看过的牌）。全部配对后按持有数结算，
 *  胜利 = 持有×100 + 300，失败 = 持有×100。 */
public abstract class MemoryDuelGame extends CanvasGame {
    protected final Random rnd = new Random();
    private final Map<Integer, Integer> aiMem = new HashMap<>(); // AI 记住的 idx→id
    private int cols, rows, kinds;
    private float aiMemChance;
    private int[] ids;        // 每格图案 id 0..kinds-1
    private int[] owner;      // 0 无主 1 你 2 AI
    private boolean[] up;     // 翻开中
    private int first = -1, turn = 1;
    private long lockUntil;
    private int mine, yours;
    private boolean aiBusy;

    /** kinds：图案种数（=格数/2）；aiMemChance：0~1 AI 记牌概率。 */
    protected MemoryDuelGame(float w, float h, int cols, int rows, float aiMemChance) {
        super(w, h);
        this.cols = cols;
        this.rows = rows;
        this.kinds = cols * rows / 2;
        this.aiMemChance = aiMemChance;
    }

    /** 格子几何（子类按画布尺寸摆放）。 */
    protected abstract float cellX(int i);

    protected abstract float cellY(int i);

    protected abstract float cellW();

    protected abstract float cellH();

    /** 背景色（游戏内自成一体的配色）。 */
    protected abstract int bgColor();

    @Override
    public void restartGame() {
        int n = cols * rows;
        ids = new int[n];
        owner = new int[n];
        up = new boolean[n];
        for (int i = 0; i < n; i++) ids[i] = i / 2;
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = ids[i];
            ids[i] = ids[j];
            ids[j] = t;
        }
        aiMem.clear();
        first = -1;
        turn = 1;
        lockUntil = 0;
        mine = 0;
        yours = 0;
        aiBusy = false;
        over = false;
        host.setScore(0);
        startLoop(80);
        dirty();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(80);
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || action != 0 || turn != 1 || lockUntil > 0 || aiBusy) return;
        for (int i = 0; i < ids.length; i++) {
            if (owner[i] != 0 || up[i]) continue;
            if (hit(x, y, cellX(i), cellY(i), cellW(), cellH())) {
                flip(i);
                return;
            }
        }
    }

    private void flip(int i) {
        up[i] = true;
        beep(0);
        if (aiBusy || turn == 2) aiRemember(i);
        if (first < 0) {
            first = i;
        } else {
            boolean match = ids[first] == ids[i];
            if (match) {
                owner[first] = owner[i] = turn;
                if (turn == 1) {
                    mine += 2;
                    host.setScore(mine * 100);
                    popup("收对！", cellX(i) + cellW() / 2f, cellY(i) - 8, 0xFF7CFFB2, 15);
                    beep(1);
                } else {
                    yours += 2;
                    popup("对手收对", cellX(i) + cellW() / 2f, cellY(i) - 8, 0xFFE8574F, 14);
                    beep(2);
                }
                first = -1;
                if (mine + yours == ids.length) {
                    finish();
                    return;
                }
                dirty();
                return; // 配对成功继续行动
            }
            // 不配对：短暂展示后换人
            final int a = first, b = i;
            first = -1;
            lockUntil = now() + 800;
            postDelayedSafe(() -> {
                up[a] = false;
                up[b] = false;
                turn = turn == 1 ? 2 : 1;
                lockUntil = 0;
                dirty();
            }, 800);
        }
        dirty();
    }

    private void aiRemember(int i) {
        if (rnd.nextFloat() < aiMemChance) aiMem.put(i, ids[i]);
    }

    @Override
    protected void onTick() {
        if (over || turn != 2 || lockUntil > 0 || aiBusy) return;
        aiBusy = true;
        postDelayedSafe(this::aiMove, 550);
    }

    private void aiMove() {
        try {
            if (over || turn != 2) return;
            // 先找记住的配对
            for (Integer a : aiMem.keySet()) {
                if (owner[a] != 0 || up[a]) continue;
                for (Integer b : aiMem.keySet()) {
                    if (b.equals(a) || owner[b] != 0 || up[b]) continue;
                    if (aiMem.get(a).equals(aiMem.get(b))) {
                        flip(a);
                        if (up[a] && up[b]) flip(b);
                        return;
                    }
                }
            }
            // 否则随机翻
            int n = ids.length, a = -1, b = -1;
            while (a < 0 || owner[a] != 0 || up[a]) a = randi(n);
            flip(a);
            while (b < 0 || owner[b] != 0 || up[b] || b == a) b = randi(n);
            flip(b);
        } finally {
            aiBusy = false;
        }
    }

    private void finish() {
        over = true;
        stopLoop();
        boolean win = mine > yours;
        int finalScore = mine * 100 + (win ? 300 : 0);
        host.setScore(finalScore);
        popup(win ? "你赢了！" : (mine == yours ? "平局" : "惜败"), lw / 2f, lh / 2f - 20,
                win ? 0xFFFFC24B : 0xFFECECF1, 26);
        beep(win ? 3 : 2);
        dirty();
        postDelayedSafe(() -> host.gameOver(finalScore), 900);
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, bgColor());
        label(c, turn == 1 ? "你的回合" : "对手思考中…", lw / 2f, 22, 14,
                turn == 1 ? 0xFF7CFFB2 : 0xFF9AA0AC, true);
        label(c, "你 " + mine / 2 + " : " + yours / 2 + " 对手", lw / 2f, 44, 13, 0xFFECECF1, true);
        for (int i = 0; i < ids.length; i++) {
            float l = cellX(i), t = cellY(i), w = cellW(), h = cellH();
            if (owner[i] == 1) {
                rr(c, l, t, l + w, t + h, 8, 0xFF2E5244);
                drawKind(c, ids[i], l + w / 2f, t + h / 2f, 0x997CFFB2);
            } else if (owner[i] == 2) {
                rr(c, l, t, l + w, t + h, 8, 0xFF52303A);
                drawKind(c, ids[i], l + w / 2f, t + h / 2f, 0x99E8848C);
            } else if (up[i]) {
                rr(c, l, t, l + w, t + h, 8, 0xFFF4F5F9);
                drawKind(c, ids[i], l + w / 2f, t + h / 2f, 0xFF33363F);
            } else {
                rr(c, l, t, l + w, t + h, 8, 0xFF4C566F);
                label(c, "?", l + w / 2f, t + h / 2f, h * 0.4f, 0xFF8B93A7, true);
            }
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    /** 图案绘制：色彩 + 星/圆/方/三角/菱形 循环（家族默认实现）。 */
    protected void drawKind(Canvas c, int kind, float cx, float cy, int color) {
        fill.setColor(color);
        float r = Math.min(cellW(), cellH()) * 0.26f;
        switch (kind % 5) {
            case 0:
                c.drawCircle(cx, cy, r, fill);
                break;
            case 1:
                rr(c, cx - r, cy - r, cx + r, cy + r, 4, color);
                break;
            case 2:
                c.drawCircle(cx, cy, r, fill);
                fill.setColor(bgColor());
                c.drawCircle(cx, cy, r * 0.5f, fill);
                break;
            case 3:
                label(c, "▲", cx, cy, r * 2.2f, color, true);
                break;
            default:
                label(c, "◆", cx, cy, r * 2.2f, color, true);
                break;
        }
    }
}
