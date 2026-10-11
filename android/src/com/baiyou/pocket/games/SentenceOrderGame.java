package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g097 词语排序：把打乱的词块按顺序点成通顺句子；
 *  排错重排不扣分但费时，完成 6 句结算，用时越短分越高。 */
public class SentenceOrderGame extends CanvasGame {
    private static final String[][] SENTENCES = {
            {"今天", "天气", "真好"}, {"小猫", "在", "追", "蝴蝶"},
            {"我", "每天", "坚持", "跑步"}, {"妈妈", "做了", "一桌", "好菜"},
            {"秋天", "树叶", "变黄了"}, {"他", "静静地", "看着", "大海"},
            {"春天", "来了", "花开", "满山"}, {"我们", "一起", "去看", "日出"},
    };
    private static final int TOTAL = 6;

    private final Random rnd = new Random();
    private final List<Integer> used = new ArrayList<>();
    private String[] parts = new String[0];
    private final List<Integer> chipOrder = new ArrayList<>(); // 槽位 → parts 下标（打乱）
    private final List<Integer> order = new ArrayList<>();     // 已点槽位序列
    private int round, wrongTaps;
    private long roundStart, totalTime;
    private boolean showing, settling;
    private long showUntil;

    public SentenceOrderGame() {
        super(360, 560);
        nextRound();
    }

    private void nextRound() {
        round++;
        if (used.size() >= SENTENCES.length) used.clear();
        int i;
        do {
            i = rnd.nextInt(SENTENCES.length);
        } while (used.contains(i));
        used.add(i);
        parts = SENTENCES[i];
        chipOrder.clear();
        for (int k = 0; k < parts.length; k++) chipOrder.add(k);
        // 打乱槽位（避免恰好原序）
        do {
            java.util.Collections.shuffle(chipOrder, rnd);
        } while (isIdentity(chipOrder) && parts.length > 1);
        order.clear();
        wrongTaps = 0;
        showing = true;
        showUntil = now() + 1200;
        roundStart = now();
    }

    private boolean isIdentity(List<Integer> l) {
        for (int k = 0; k < l.size(); k++) if (l.get(k) != k) return false;
        return true;
    }

    private void startGame() {
        round = 0;
        totalTime = 0;
        settling = false;
        over = false;
        nextRound();
        host.setScore(0);
        startLoop(60);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(60);
    }

    /** 按已点槽位序列拼出的前缀是否正确。 */
    private boolean correctSoFar() {
        for (int k = 0; k < order.size(); k++) {
            if (!parts[chipOrder.get(order.get(k))].equals(parts[k])) return false;
        }
        return true;
    }

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || showing || action != 0) return;
        for (int slot = 0; slot < chipOrder.size(); slot++) {
            if (order.contains(slot)) continue;
            float cx = chipX(slot), cy = 340;
            if (hit(x, y, cx, cy, 90, 56)) {
                order.add(slot);
                if (!correctSoFar()) {
                    wrongTaps++;
                    beep(2);
                    order.remove(order.size() - 1);
                    shake(150);
                    dirty();
                    return;
                }
                beep(0);
                if (order.size() == parts.length) {
                    long dt = now() - roundStart;
                    totalTime += dt;
                    roundDone(dt);
                }
                dirty();
                return;
            }
        }
    }

    private void roundDone(long dt) {
        settling = true;
        beep(1);
        popup("通顺！", lw / 2f, 260, 0xFF7CFFB2, 17);
        int gained = Math.max(60, 300 - (int) (dt / 1000) * 10 - wrongTaps * 40);
        shownScore += gained;
        host.setScore(shownScore);
        dirty();
        postDelayedSafe(() -> {
            settling = false;
            if (over) return;
            if (round >= TOTAL) {
                over = true;
                stopLoop();
                int sec = (int) (totalTime / 1000);
                popup("六句连成！用时 " + sec + " 秒", lw / 2f, lh / 2f, 0xFFFFC24B, 19);
                dirty();
                postDelayedSafe(() -> host.gameOver(shownScore), 900);
            } else {
                nextRound();
                dirty();
            }
        }, 700);
    }

    private int shownScore;

    private float chipX(int i) {
        return 30 + i * 76;
    }

    @Override
    protected void onTick() {
        if (showing && now() >= showUntil) showing = false;
        dirty();
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1E2430);
        label(c, "第 " + Math.min(round, TOTAL) + "/" + TOTAL + " 句",
                lw / 2f, 90, 14, 0xFF9AA0AC, false);
        if (showing) {
            StringBuilder sb = new StringBuilder();
            for (String p : parts) sb.append(p);
            label(c, sb.toString(), lw / 2f, 200, 24, 0xFFFFC24B, true);
            label(c, "记住顺序…", lw / 2f, 250, 13, 0xFF8B93A7, false);
        } else {
            // 已拼
            StringBuilder sb = new StringBuilder();
            for (int slot : order) sb.append(parts[chipOrder.get(slot)]);
            label(c, sb.toString().isEmpty() ? "＿" : sb.toString(), lw / 2f, 200, 24,
                    0xFF7CFFB2, true);
            // 词块（打乱后的槽位）
            for (int slot = 0; slot < chipOrder.size(); slot++) {
                if (order.contains(slot)) continue;
                float cx = chipX(slot), cy = 340;
                rr(c, cx, cy, cx + 90, cy + 56, 10, 0xFF2C3648);
                label(c, parts[chipOrder.get(slot)], cx + 45, cy + 28, 17, 0xFFECECF1, true);
            }
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }
}
