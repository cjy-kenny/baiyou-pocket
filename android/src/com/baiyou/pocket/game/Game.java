package com.baiyou.pocket.game;

import android.content.Context;
import android.view.View;

/** 一款游戏 = Game 的一个子类。所有游戏运行在 GameHostActivity（独立进程 :game）里，
 *  由宿主提供 HUD（分数）、暂停/重开/退出、结算、音效与崩溃兜底。
 *
 *  生命周期约定：
 *  - bind() → createView() → 宿主调用 restartGame() 开第一局 →
 *    [pauseGame()/resumeGame() 随系统与用户暂停] → gameOver() 由游戏主动调用结算 →
 *    restartGame() 重开 → destroy() 离开。
 *  - 构造函数只做与 host 无关的初始化（此时 host 尚未注入）。
 *  - 游戏内部只允许三类执行入口，全部有崩溃兜底：
 *    1) 触摸回调（宿主 SafeFrame.dispatchTouchEvent 兜底）
 *    2) onTick 逻辑帧（宿主 Handler 循环兜底）
 *    3) 其它主动调用尽量经过 Game.runSafe() 包装。 */
public abstract class Game {
    public interface Host {
        /** HUD 实时刷新分数（写盘由宿主负责）。 */
        void setScore(int score);

        /** 本局结束，进入结算（宿主弹窗、写最高分、累计金币）。 */
        void gameOver(int finalScore);

        void toast(String msg);

        boolean soundOn();

        /** 合成音效：0=点按 1=得分 2=失误 3=胜利 */
        void beep(int kind);

        /** 游戏逻辑抛出异常时上抛给宿主（记日志 + 安全退出，不影响其它内容）。 */
        void crash(Throwable t);
    }

    protected Context ctx;
    protected Host host;

    public final void bind(Context ctx, Host host) {
        this.ctx = ctx;
        this.host = host;
    }

    /** 创建游戏主视图（只调用一次；重开不重建视图）。 */
    public abstract View createView();

    /** 暂停：停掉逻辑与计时（系统 onPause 或用户点暂停都会调用）。 */
    public abstract void pauseGame();

    /** 恢复。 */
    public abstract void resumeGame();

    /** 重开一局（分数清零由游戏自行回调 host.setScore(0)）。 */
    public abstract void restartGame();

    public void destroy() {
    }

    /** runSafe：包装可能抛异常的游戏代码。 */
    protected final void runSafe(Runnable r) {
        try {
            r.run();
        } catch (Throwable t) {
            host.crash(t);
        }
    }
}
