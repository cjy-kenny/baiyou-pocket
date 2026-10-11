package android.view;

import android.content.Context;
import android.graphics.Canvas;

/** 桌面端 View：承载 CanvasGame 创建的匿名子类（onSizeChanged/onDraw/触摸）。
 *  Android 上由系统回调；桌面端由 ViewHostPanel 通过 perform* 驱动。 */
public class View {
    public interface OnTouchListener {
        boolean onTouch(View v, MotionEvent event);
    }

    private final int[] size = new int[2];
    private OnTouchListener touchListener;
    /** 桌面端由宿主注入：postInvalidateOnAnimation 时触发重绘。 */
    public Runnable invalidateHook;

    public View(Context context) {
    }

    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    }

    protected void onDraw(Canvas canvas) {
    }

    public final int getWidth() {
        return size[0];
    }

    public final int getHeight() {
        return size[1];
    }

    public void setOnTouchListener(OnTouchListener l) {
        touchListener = l;
    }

    public void postInvalidateOnAnimation() {
        if (invalidateHook != null) invalidateHook.run();
    }

    // ---- 桌面宿主驱动入口 ----

    public final void performSizeChanged(int w, int h, int oldw, int oldh) {
        size[0] = w;
        size[1] = h;
        onSizeChanged(w, h, oldw, oldh);
    }

    public final void performDraw(Canvas canvas) {
        onDraw(canvas);
    }

    public final boolean performTouch(MotionEvent ev) {
        return touchListener != null && touchListener.onTouch(this, ev);
    }
}
