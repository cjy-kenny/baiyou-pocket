package android.graphics;

/** 桌面端 Paint：属性集与游戏代码用到的子集对齐（Android 端用真实现）。 */
public class Paint {
    public static final int ANTI_ALIAS_FLAG = 0x01;

    public enum Style {FILL, STROKE, FILL_AND_STROKE}

    public enum Align {LEFT, CENTER, RIGHT}

    public enum Cap {BUTT, ROUND, SQUARE}

    public static class FontMetrics {
        public float ascent;      // 负值（基线以上），与 Android 语义一致
        public float descent;
        public float leading;
        public float top;
        public float bottom;
    }

    private boolean antiAlias;
    private int color = 0xFF000000;
    private Style style = Style.FILL;
    private float strokeWidth;
    private float textSize;
    private boolean fakeBold;
    private Align align = Align.LEFT;
    private Cap cap = Cap.BUTT;

    public Paint() {
    }

    public Paint(int flags) {
        antiAlias = (flags & ANTI_ALIAS_FLAG) != 0;
    }

    public boolean isAntiAlias() {
        return antiAlias;
    }

    public void setAntiAlias(boolean aa) {
        antiAlias = aa;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public Style getStyle() {
        return style;
    }

    public void setStyle(Style style) {
        this.style = style;
    }

    public float getStrokeWidth() {
        return strokeWidth;
    }

    public void setStrokeWidth(float strokeWidth) {
        this.strokeWidth = strokeWidth;
    }

    public float getTextSize() {
        return textSize;
    }

    public void setTextSize(float textSize) {
        this.textSize = textSize;
    }

    public boolean isFakeBoldText() {
        return fakeBold;
    }

    public void setFakeBoldText(boolean fakeBold) {
        this.fakeBold = fakeBold;
    }

    public Align getTextAlign() {
        return align;
    }

    public void setTextAlign(Align align) {
        this.align = align;
    }

    public Cap getStrokeCap() {
        return cap;
    }

    public void setStrokeCap(Cap cap) {
        this.cap = cap;
    }

    public FontMetrics getFontMetrics() {
        return ShimFont.metrics(this);
    }

    /** Android Paint.measureText 的垫片等价（取字形宽和）。 */
    public float measureText(String text) {
        return ShimFont.measure(this, text);
    }
}
