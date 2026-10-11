package com.baiyou.pocket.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

/** 游戏结算回传合并（双端共享）。
 *  游戏进程退出时回传 best/coins/plays/timeSec，由平台壳调用本类合并进 store.json：
 *  金币/时长/局数累加、单游戏最高分取大、playedIds 去重、最近玩过置顶（截断 20 条）。 */
public final class ResultMerger {
    private ResultMerger() {
    }

    public static void merge(Context c, String gameId,
                             int coins, int plays, int best, int timeSec) {
        JSONObject store = Store.loadStore(c);
        try {
            store.put("coins", store.optInt("coins", 0) + coins);
            store.put("playsTotal", store.optInt("playsTotal", 0) + plays);
            store.put("timeSec", store.optInt("timeSec", 0) + timeSec);
            store.put("maxScore", Math.max(store.optInt("maxScore", 0), best));

            // playedIds 去重
            JSONArray played = store.optJSONArray("playedIds");
            boolean seen = false;
            for (int i = 0; i < played.length(); i++)
                if (played.optString(i).equals(gameId)) seen = true;
            if (!seen) played.put(gameId);

            // recent 置顶
            JSONArray recent = store.optJSONArray("recent");
            JSONArray nr = new JSONArray();
            JSONObject entry = new JSONObject();
            entry.put("id", gameId);
            entry.put("ts", System.currentTimeMillis());
            nr.put(entry);
            for (int i = 0; i < recent.length(); i++) {
                JSONObject o = recent.optJSONObject(i);
                if (o != null && !gameId.equals(o.optString("id"))) nr.put(o);
            }
            while (nr.length() > 20) nr.remove(nr.length() - 1);
            store.put("recent", nr);
        } catch (Exception ignore) {
        }
        Store.saveStore(c, store);
    }
}
