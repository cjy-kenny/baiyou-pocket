package com.baiyou.pocket.games;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

import com.baiyou.pocket.game.CanvasGame;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g013 形状归位：点选散落的形状，再点对应凹槽归位；三回合累计，
 *  放错扣分、用时越短分越高。 */
public class ShapeMatchGame extends CanvasGame {
    private static final int KINDS = 4;

    private final Random rnd = new Random();
    private final float[] sx = new float[KINDS], sy = new float[KINDS];
    private final int[] kinds = new int[KINDS];   // 每个散落物形状
    private final boolean[] placed = new boolean[KINDS];
    private final boolean[] slotFilled = new boolean[KINDS];
    private int selected = -1, round, wrong;
    private long roundStart;
    private boolean settling;

    public ShapeMatchGame() {
        super(360, 560);
        genRound();
    }

    private void genRound() {
        List<Integer> deck = new ArrayList<>();
        for (int i = 0; i < KINDS; i++) deck.add(i);
        java.util.Collections.shuffle(deck, rnd);
        for (int i = 0; i < KINDS; i++) {
            kinds[i] = deck.get(i);
            sx[i] = rand(50, lw - 50);
            sy[i] = rand(150, 300);
            placed[i] = false;
            slotFilled[i] = false;
        }
        // 简单防重叠：两两太近就重掷一次
        for (int k = 0; k < 8; k++)
            for (int i = 0; i < KINDS; i++)
                for (int j = i + 1; j < KINDS; j++)
                    if (Math.abs(sx[i] - sx[j]) < 60 && Math.abs(sy[i] - sy[j]) < 60) {
                        sx[j] = rand(50, lw - 50);
                        sy[j] = rand(150, 300);
                    }
        selected = -1;
        roundStart = now();
    }

    private void startGame() {
        round = 1;
        wrong = 0;
        settling = false;
        over = false;
        genRound();
        host.setScore(0);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    private float slotX(int i) {
        return 32 + i * 78;
    }

    private static final float SLOT_Y = 400, SLOT_W = 64, SLOT_H = 64;

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        // 先看凹槽
        for (int i = 0; i < KINDS; i++) {
            if (hit(x, y, slotX(i), SLOT_Y, SLOT_W, SLOT_H)) {
                if (selected >= 0 && !slotFilled[i]) {
                    if (kinds[selected] == i) {
                        placed[selected] = true;
                        slotFilled[i] = true;
                        beep(1);
                        burst(slotX(i) + SLOT_W / 2f, SLOT_Y + SLOT_H / 2f, 0xFF7CFFB2, 10, 5f);
                        if (allPlaced()) {
                            roundDone();
                            return;
                        }
                    } else {
                        wrong++;
                        beep(2);
                        shake(150);
                        popup("形状不对", slotX(i) + SLOT_W / 2f, SLOT_Y - 12, 0xFFE8574F, 14);
                    }
                    selected = -1;
                }
                dirty();
                return;
            }
        }
        // 再看形状
        for (int i = 0; i < KINDS; i++) {
            if (placed[i]) continue;
            if (hit(x, y, sx[i] - 30, sy[i] - 30, 60, 60)) {
                selected = (selected == i) ? -1 : i;
                beep(0);
                dirty();
                return;
            }
        }
    }

    private boolean allPlaced() {
        for (boolean b : placed) if (!b) return false;
        return true;
    }

    private void roundDone() {
        settling = true;
        int sec = (int) ((now() - roundStart) / 1000);
        int gained = Math.max(100, 600 - wrong * 50 - sec * 3);
        shownScore += gained;
        host.setScore(shownScore);
        popup("第 " + round + " 轮 + " + gained, lw / 2f, 360, 0xFF7CFFB2, 18);
        beep(3);
        dirty();
        postDelayedSafe(() -> {
            settling = false;
            if (round >= 3) {
                over = true;
                stopLoop();
                popup("完成！总分 " + shownScore, lw / 2f, lh / 2f, 0xFFFFC24B, 20);
                dirty();
                postDelayedSafe(() -> host.gameOver(shownScore), 850);
            } else {
                round++;
                genRound();
                dirty();
            }
        }, 900);
    }

    private int shownScore;

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF20283A);
        label(c, "第 " + round + "/3 轮 · 点形状再点凹槽", lw / 2f, 70, 14, 0xFF9AAFD0, false);
        for (int i = 0; i < KINDS; i++) {
            if (placed[i]) continue;
            drawShape(c, kinds[i], sx[i], sy[i], 26,
                    i == selected ? 0xFFFFC24B : 0xFF7C9BF0, true);
        }
        for (int i = 0; i < KINDS; i++) {
            // 凹槽（描边轮廓）
            stroke.setColor(slotFilled[i] ? 0xFF7CFFB2 : 0xFF55688F);
            stroke.setStrokeWidth(3);
            drawShapeStroke(c, i, slotX(i) + SLOT_W / 2f, SLOT_Y + SLOT_H / 2f, 24, stroke);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private void drawShape(Canvas c, int kind, float cx, float cy, float r, int color, boolean filled) {
        if (filled) fill.setColor(color);
        switch (kind) {
            case 0:
                c.drawCircle(cx, cy, r, filled ? fill : stroke);
                break;
            case 1:
                rr(c, cx - r, cy - r, cx + r, cy + r, 6, color);
                break;
            case 2:
                Path p = new Path();
                p.moveTo(cx, cy - r);
                p.lineTo(cx + r, cy + r * 0.8f);
                p.lineTo(cx - r, cy + r * 0.8f);
                p.close();
                fill.setColor(color);
                c.drawPath(p, fill);
                break;
            default:
                Path h = new Path();
                for (int k = 0; k < 6; k++) {
                    double a = Math.PI / 3 * k - Math.PI / 2;
                    float px = cx + (float) Math.cos(a) * r;
                    float py = cy + (float) Math.sin(a) * r;
                    if (k == 0) h.moveTo(px, py);
                    else h.lineTo(px, py);
                }
                h.close();
                fill.setColor(color);
                c.drawPath(h, fill);
                break;
        }
    }

    private void drawShapeStroke(Canvas c, int kind, float cx, float cy, float r, Paint st) {
        switch (kind) {
            case 0:
                c.drawCircle(cx, cy, r, st);
                break;
            case 1:
                rf.set(cx - r, cy - r, cx + r, cy + r);
                c.drawRoundRect(rf, 6, 6, st);
                break;
            case 2: {
                Path p = new Path();
                p.moveTo(cx, cy - r);
                p.lineTo(cx + r, cy + r * 0.8f);
                p.lineTo(cx - r, cy + r * 0.8f);
                p.close();
                c.drawPath(p, st);
                break;
            }
            default: {
                Path h = new Path();
                for (int k = 0; k < 6; k++) {
                    double a = Math.PI / 3 * k - Math.PI / 2;
                    float px = cx + (float) Math.cos(a) * r;
                    float py = cy + (float) Math.sin(a) * r;
                    if (k == 0) h.moveTo(px, py);
                    else h.lineTo(px, py);
                }
                h.close();
                c.drawPath(h, st);
                break;
            }
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
