package android.view;

/** 桌面端 MotionEvent：只承载游戏用到的三个量（动作、逻辑面板坐标）。 */
public class MotionEvent {
    public static final int ACTION_DOWN = 0;
    public static final int ACTION_UP = 1;
    public static final int ACTION_MOVE = 2;
    public static final int ACTION_CANCEL = 3;

    private final int action;
    private final float x, y;

    public MotionEvent(int action, float x, float y) {
        this.action = action;
        this.x = x;
        this.y = y;
    }

    public int getActionMasked() {
        return action;
    }

    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }
}
