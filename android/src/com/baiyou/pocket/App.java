package com.baiyou.pocket;

import android.app.Application;

/** 应用入口。主进程与游戏进程(:game)都会执行到这里，因此必须保持进程无关：
 *  只安装崩溃守卫，不做任何"仅主进程"的初始化。 */
public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        CrashGuard.install(this);
    }
}
