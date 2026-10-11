package android.graphics;

import java.awt.Font;
import java.awt.FontMetrics;

/** 桌面端字体工具：Paint → AWT Font/FontMetrics（缓存度量用 1×1 图）。 */
final class ShimFont {
    private static final java.awt.image.BufferedImage METER =
            new java.awt.image.BufferedImage(1, 1, java.awt.image.BufferedImage.TYPE_INT_ARGB);

    private ShimFont() {
    }

    static Font font(Paint p) {
        int style = p.isFakeBoldText() ? Font.BOLD : Font.PLAIN;
        return new Font("SansSerif", style, 12).deriveFont(Math.max(0.1f, p.getTextSize()));
    }

    static Paint.FontMetrics metrics(Paint p) {
        Font f = font(p);
        FontMetrics fm = METER.getGraphics().getFontMetrics(f);
        Paint.FontMetrics m = new Paint.FontMetrics();
        m.ascent = -fm.getAscent();
        m.descent = fm.getDescent();
        m.leading = fm.getLeading();
        m.top = m.ascent - m.leading;
        m.bottom = m.descent + m.leading;
        return m;
    }

    static float measure(Paint p, String text) {
        return METER.getGraphics().getFontMetrics(font(p)).stringWidth(text);
    }
}
