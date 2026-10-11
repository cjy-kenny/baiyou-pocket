package android.graphics.drawable;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;

/** 桌面端 Drawable：bounds 管理 + 绘制入口（Android 端用真实现）。 */
public abstract class Drawable {
    private final Rect bounds = new Rect();

    public abstract void draw(Canvas canvas);

    public void setBounds(int l, int t, int r, int b) {
        bounds.set(l, t, r, b);
    }

    public void setBounds(Rect b) {
        bounds.set(b);
    }

    public Rect getBounds() {
        return bounds;
    }

    public void setAlpha(int alpha) {
    }

    public void setColorFilter(ColorFilter cf) {
    }

    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }

    public void invalidateSelf() {
    }
}
