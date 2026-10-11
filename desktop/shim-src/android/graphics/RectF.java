package android.graphics;

/** 桌面端 RectF（浮点）。 */
public class RectF {
    public float left, top, right, bottom;

    public RectF() {
    }

    public RectF(float left, float top, float right, float bottom) {
        set(left, top, right, bottom);
    }

    public void set(float l, float t, float r, float b) {
        left = l;
        top = t;
        right = r;
        bottom = b;
    }

    public void set(RectF r) {
        set(r.left, r.top, r.right, r.bottom);
    }

    public void set(Rect r) {
        set(r.left, r.top, r.right, r.bottom);
    }

    public final float width() {
        return right - left;
    }

    public final float height() {
        return bottom - top;
    }
}
