package android.graphics;

/** 桌面端 Rect（整型）。 */
public class Rect {
    public int left, top, right, bottom;

    public Rect() {
    }

    public Rect(int left, int top, int right, int bottom) {
        set(left, top, right, bottom);
    }

    public void set(int l, int t, int r, int b) {
        left = l;
        top = t;
        right = r;
        bottom = b;
    }

    public void set(Rect r) {
        set(r.left, r.top, r.right, r.bottom);
    }

    public void set(RectF r) {
        set((int) r.left, (int) r.top, (int) r.right, (int) r.bottom);
    }

    public final int width() {
        return right - left;
    }

    public final int height() {
        return bottom - top;
    }

    public final int centerX() {
        return (left + right) / 2;
    }

    public final int centerY() {
        return (top + bottom) / 2;
    }

    public final boolean isEmpty() {
        return left >= right || top >= bottom;
    }
}
