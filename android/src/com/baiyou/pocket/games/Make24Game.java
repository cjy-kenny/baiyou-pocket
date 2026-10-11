package com.baiyou.pocket.games;

import android.graphics.Canvas;

import com.baiyou.pocket.game.CanvasGame;
import com.baiyou.pocket.game.CardKit;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** g019 廿四快斗：四张牌两两用 + - × ÷ 逐步合并，凑出 24。
 *  先点一张牌、再点运算符、再点另一张牌；除法只能整除；
 *  凑成 +500 并换新局，无法凑出可换牌（-1 命），3 命尽结算。 */
public class Make24Game extends CanvasGame {
    private final Random rnd = new Random();
    private final List<Integer> cards = new ArrayList<>();
    private int selCard = -1, selOp = -1;
    private int lives = 3, shownScore, solved;
    private boolean settling;

    private static final String OPS = "+-×÷";

    public Make24Game() {
        super(360, 560);
        deal();
    }

    private void deal() {
        cards.clear();
        for (int i = 0; i < 4; i++) cards.add(1 + rnd.nextInt(13));
        selCard = selOp = -1;
    }

    private void startGame() {
        lives = 3;
        shownScore = 0;
        solved = 0;
        settling = false;
        over = false;
        deal();
        host.setScore(0);
        dirty();
    }

    @Override
    public void restartGame() {
        startGame();
    }

    private float cardX(int i) {
        return 24 + i * 80;
    }

    private static final float CARD_Y = 150, CARD_W = 68, CARD_H = 92;

    private float opX(int i) {
        return 40 + i * 72;
    }

    private static final float OP_Y = 300, OP_W = 60, OP_H = 56;

    @Override
    protected void touch(float x, float y, int action) {
        if (over || settling || action != 0) return;
        // 牌
        for (int i = 0; i < cards.size(); i++) {
            if (hit(x, y, cardX(i), CARD_Y, CARD_W, CARD_H)) {
                if (selOp >= 0 && selCard >= 0 && i != selCard) {
                    combine(selCard, i, selOp);
                    return;
                }
                selCard = (selCard == i) ? -1 : i;
                selOp = -1;
                beep(0);
                dirty();
                return;
            }
        }
        // 运算符
        for (int i = 0; i < 4; i++) {
            if (hit(x, y, opX(i), OP_Y, OP_W, OP_H)) {
                selOp = (selOp == i) ? -1 : i;
                beep(0);
                dirty();
                return;
            }
        }
    }

    private void combine(int a, int b, int op) {
        int va = cards.get(a), vb = cards.get(b);
        int r;
        switch (OPS.charAt(op)) {
            case '+': r = va + vb; break;
            case '-': r = va - vb; if (r < 0) r = vb - va; break;
            case '×': r = va * vb; break;
            default:
                int hi = Math.max(va, vb), lo = Math.min(va, vb);
                if (lo == 0 || hi % lo != 0) {
                    beep(2);
                    popup("除不尽", 180, 280, 0xFFE8574F, 14);
                    return;
                }
                r = hi / lo;
                break;
        }
        int keep = Math.max(a, b), drop = Math.min(a, b);
        cards.set(keep, r);
        cards.remove(drop);
        selCard = selOp = -1;
        beep(1);
        if (cards.size() == 1) {
            if (cards.get(0) == 24) {
                solved++;
                shownScore += 500;
                host.setScore(shownScore);
                popup("廿四！ +500", lw / 2f, lh / 2f - 40, 0xFFFFC24B, 24);
                burst(lw / 2f, CARD_Y + 40, 0xFFFFC24B, 22, 8f);
                beep(3);
                postDelayedSafe(() -> {
                    if (over || settling) return;
                    deal();
                    dirty();
                }, 1000);
            } else {
                lives--;
                beep(2);
                popup("结果 " + cards.get(0) + " · 换一副", lw / 2f, lh / 2f - 40, 0xFFE8574F, 16);
                if (lives <= 0) {
                    finishGame();
                    return;
                }
                postDelayedSafe(() -> {
                    if (over || settling) return;
                    deal();
                    dirty();
                }, 1000);
            }
        }
        dirty();
    }

    private void finishGame() {
        settling = true;
        over = true;
        stopLoop();
        popup("完成 " + solved + " 副 · " + shownScore + " 分", lw / 2f, lh / 2f, 0xFFECECF1, 17);
        dirty();
        postDelayedSafe(() -> host.gameOver(shownScore), 850);
    }

    @Override
    protected void render(Canvas c) {
        rr(c, 0, 0, lw, lh, 0, 0xFF1C2218);
        label(c, "凑出 24 · 点牌→点运算符→点牌", lw / 2f, 70, 13, 0xFFAFC9A0, false);
        hearts(c, lives, 3, lw - 24, 30, 8);
        label(c, "已解 " + solved + " 副", 40, 30, 13, 0xFFECECF1, true);
        for (int i = 0; i < cards.size(); i++) {
            boolean sel = i == selCard;
            CardKit.draw(this, c, Math.min(13, cards.get(i)), 0,
                    cardX(i), CARD_Y, CARD_W, CARD_H, false);
            // 数字大字覆盖
            label(c, String.valueOf(cards.get(i)), cardX(i) + CARD_W / 2f, CARD_Y + CARD_H / 2f,
                    30, 0xFF2B2F3A, true);
            if (sel) {
                stroke.setColor(0xFFFFC24B);
                stroke.setStrokeWidth(3);
                rf.set(cardX(i) - 2, CARD_Y - 2, cardX(i) + CARD_W + 2, CARD_Y + CARD_H + 2);
                c.drawRoundRect(rf, 6, 6, stroke);
            }
        }
        for (int i = 0; i < 4; i++) {
            boolean sel = i == selOp;
            rr(c, opX(i), OP_Y, opX(i) + OP_W, OP_Y + OP_H, 10,
                    sel ? 0xFFFFC24B : 0xFF31402C);
            label(c, String.valueOf(OPS.charAt(i)), opX(i) + OP_W / 2f, OP_Y + OP_H / 2f,
                    28, sel ? 0xFF1B1C22 : 0xFFECECF1, true);
        }
        if (over) {
            fill.setColor(0xB3101218);
            c.drawRoundRect(new android.graphics.RectF(0, 0, lw, lh), 0, 0, fill);
        }
    }

    private final android.graphics.RectF rf = new android.graphics.RectF();
}
