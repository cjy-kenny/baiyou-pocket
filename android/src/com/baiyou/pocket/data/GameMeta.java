package com.baiyou.pocket.data;

/** 一款游戏的元数据。注册表见 {@link Registry}。 */
public class GameMeta {
    public final String id;       // g001
    public final String name;     // 方块消消
    public final String category; // 益智
    public final String brief;    // 一句话玩法
    public final String tutorial; // 玩法介绍（进游戏时的引导文案）
    /** 已实现的玩法类；未开放游戏为 null（首页置灰"敬请期待"）。 */
    public final Class<?> impl;

    public GameMeta(String id, String name, String category, String brief,
                    String tutorial, Class<?> impl) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.brief = brief;
        this.tutorial = tutorial;
        this.impl = impl;
    }

    public boolean playable() {
        return impl != null;
    }
}
