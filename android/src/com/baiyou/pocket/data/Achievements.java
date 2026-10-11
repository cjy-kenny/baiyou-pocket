package com.baiyou.pocket.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/** 成就系统（双端共享：Android 主页与 PC 桌面端同一套判定）。
 *  数据存 store.json 的 ach 数组；判定只依赖 store 内字段。 */
public final class Achievements {
    private Achievements() {
    }

    /** id / 名称 / 描述 */
    public static final String[][] DEFS = {
            {"a_first", "初次启程", "完成第 1 局游戏"},
            {"a_play10", "小试身手", "累计完成 10 局"},
            {"a_play50", "游戏常客", "累计完成 50 局"},
            {"a_try5", "五湖四海", "玩过 5 款不同的游戏"},
            {"a_try20", "百游探索家", "玩过 20 款不同的游戏"},
            {"a_score500", "高分新星", "单局得分达到 500"},
            {"a_coin100", "小金库", "累计获得 100 金币"},
            {"a_fav1", "收藏家", "收藏 1 款喜欢的游戏"},
    };

    public static boolean eval(String id, JSONObject store) {
        switch (id) {
            case "a_first": return store.optInt("playsTotal", 0) >= 1;
            case "a_play10": return store.optInt("playsTotal", 0) >= 10;
            case "a_play50": return store.optInt("playsTotal", 0) >= 50;
            case "a_try5": return store.optJSONArray("playedIds").length() >= 5;
            case "a_try20": return store.optJSONArray("playedIds").length() >= 20;
            case "a_score500": return store.optInt("maxScore", 0) >= 500;
            case "a_coin100": return store.optInt("coins", 0) >= 100;
            case "a_fav1": return store.optJSONArray("favs").length() >= 1;
            default: return false;
        }
    }

    /** 检查并写入新达成的成就（有新增则保存 store），返回新成就名称列表。 */
    public static List<String> checkAndSave(Context c, JSONObject store) {
        List<String> newly = new ArrayList<>();
        JSONArray ach = store.optJSONArray("ach");
        for (String[] d : DEFS) {
            boolean got = false;
            for (int i = 0; i < ach.length(); i++) if (ach.optString(i).equals(d[0])) got = true;
            if (!got && eval(d[0], store)) {
                ach.put(d[0]);
                newly.add(d[1]);
            }
        }
        if (!newly.isEmpty()) Store.saveStore(c, store);
        return newly;
    }

    public static boolean has(JSONObject store, String id) {
        JSONArray ach = store.optJSONArray("ach");
        for (int i = 0; i < ach.length(); i++) if (ach.optString(i).equals(id)) return true;
        return false;
    }
}
