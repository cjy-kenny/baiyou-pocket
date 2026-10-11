package com.baiyou.pocket.desktop;

import java.awt.Color;

/** 桌面端配色（与 Android 端 values/values-night 同一套色值）。 */
public class Theme {
    private static final Theme INSTANCE = new Theme();

    public boolean dark = true;
    public Color bg, card, cardSub, text, textSub, accent;

    private Theme() {
        apply();
    }

    public static Theme get() {
        return INSTANCE;
    }

    public void apply() {
        if (dark) {
            bg = new Color(0x131419);
            card = new Color(0x1F2128);
            cardSub = new Color(0x2A2D37);
            text = new Color(0xECECF1);
            textSub = new Color(0x8B8E9C);
            accent = new Color(0x7B7FF5);
        } else {
            bg = new Color(0xF3F4F8);
            card = new Color(0xFFFFFF);
            cardSub = new Color(0xEDEEF4);
            text = new Color(0x1B1C22);
            textSub = new Color(0x7A7D8A);
            accent = new Color(0x5B5FEF);
        }
    }

    public static Color star() {
        return new Color(0xFFC24B);
    }

    public static Color danger() {
        return new Color(0xE8574F);
    }
}
