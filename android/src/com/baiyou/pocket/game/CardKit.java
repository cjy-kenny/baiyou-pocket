package com.baiyou.pocket.game;

import android.graphics.Canvas;

/** 纸牌绘制小工具（牌类家族共用）：花色 ♠♥♦♣ 为公有领域符号，程序化绘制。 */
public final class CardKit {
    private CardKit() {
    }

    public static final String SUITS = "♠♥♦♣";
    public static final int RED = 0xFFD14B3F;
    public static final int BLACK = 0xFF2B2F3A;

    /** rank: 1=A … 11=J 12=Q 13=K；suit: 0♠ 1♥ 2♦ 3♣ */
    public static String rankStr(int rank) {
        switch (rank) {
            case 1: return "A";
            case 11: return "J";
            case 12: return "Q";
            case 13: return "K";
            default: return String.valueOf(rank);
        }
    }

    public static boolean isRed(int suit) {
        return suit == 1 || suit == 2;
    }

    /** 画一张牌（白底圆角 + 左上角点数花色 + 中央大花色）。faceUp=false 画牌背。 */
    public static void draw(CanvasGame g, Canvas c, int rank, int suit,
                            float l, float t, float w, float h, boolean faceUp) {
        if (!faceUp) {
            g.rr(c, l, t, l + w, t + h, 5, 0xFF3E5C9A);
            // 牌背斜纹点
            g.fill.setColor(0x55FFFFFF);
            for (int i = 0; i < 3; i++)
                for (int j = 0; j < 4; j++)
                    c.drawCircle(l + w * (0.25f + i * 0.25f), t + h * (0.2f + j * 0.2f), 2, g.fill);
            return;
        }
        g.rr(c, l, t, l + w, t + h, 5, 0xFFF7F7F2);
        int color = isRed(suit) ? RED : BLACK;
        String rankS = rankStr(rank);
        String suitS = String.valueOf(SUITS.charAt(suit));
        g.label(c, rankS, l + 10, t + 11, h * 0.19f, color, true);
        g.label(c, suitS, l + 10, t + 25, h * 0.15f, color, true);
        g.label(c, suitS, l + w / 2f, t + h / 2f + 4, h * 0.34f, color, true);
    }

    /** 画一张占位/空槽。 */
    public static void slot(CanvasGame g, Canvas c, float l, float t, float w, float h, int color) {
        g.rr(c, l, t, l + w, t + h, 5, color);
    }

    public static void shuffle(int[] a, java.util.Random rnd) {
        for (int i = a.length - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int t = a[i];
            a[i] = a[j];
            a[j] = t;
        }
    }
}
