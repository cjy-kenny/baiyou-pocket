package com.baiyou.pocket.data;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

/** 统一本地数据（全部离线，存应用私有目录）。
 *
 *  多进程分工，避免并发写坏文件：
 *  - store.json        —— 仅主进程读写：金币、成就、收藏、最近玩过、设置、累计时长/局数
 *  - scores/<id>.json  —— 仅游戏进程(:game)实时写（原子替换）：最高分、局数、上次游玩时间
 *    主进程对 scores 只读，游戏进程从不碰 store.json。
 *
 *  全部原子写：先写 tmp 再 rename，进程被杀也不会留下半截文件。 */
public final class Store {
    private Store() {
    }

    // ---------- store.json（主进程数据） ----------

    public static JSONObject loadStore(Context c) {
        JSONObject o = readJson(new File(c.getFilesDir(), "store.json"));
        if (o == null) o = new JSONObject();
        try {
            // 缺省字段兜底
            if (!o.has("coins")) o.put("coins", 0);          // 金币
            if (!o.has("timeSec")) o.put("timeSec", 0);      // 累计游玩秒数
            if (!o.has("playsTotal")) o.put("playsTotal", 0);// 累计局数
            if (!o.has("maxScore")) o.put("maxScore", 0);    // 历史单局最高分
            if (!o.has("favs")) o.put("favs", new JSONArray());      // 收藏的 gameId
            if (!o.has("recent")) o.put("recent", new JSONArray());  // 最近玩过 [{id,ts}]
            if (!o.has("playedIds")) o.put("playedIds", new JSONArray()); // 玩过的 gameId 去重
            if (!o.has("ach")) o.put("ach", new JSONArray());        // 已达成成就 id
            if (!o.has("sound")) o.put("sound", true);               // 音效开关
        } catch (Exception ignore) {
        }
        return o;
    }

    public static void saveStore(Context c, JSONObject o) {
        writeAtomic(new File(c.getFilesDir(), "store.json"), o.toString());
    }

    // ---------- scores/<id>.json（游戏进程数据，主进程只读） ----------

    public static JSONObject loadScore(Context c, String gameId) {
        JSONObject o = readJson(scoreFile(c, gameId));
        if (o == null) o = new JSONObject();
        try {
            if (!o.has("best")) o.put("best", 0);
            if (!o.has("plays")) o.put("plays", 0);
            if (!o.has("last")) o.put("last", 0L);
        } catch (Exception ignore) {
        }
        return o;
    }

    public static void saveScore(Context c, String gameId, JSONObject o) {
        File f = scoreFile(c, gameId);
        f.getParentFile().mkdirs();
        writeAtomic(f, o.toString());
    }

    private static File scoreFile(Context c, String gameId) {
        return new File(new File(c.getFilesDir(), "scores"), gameId + ".json");
    }

    // ---------- 底层读写 ----------

    private static JSONObject readJson(File f) {
        try {
            if (!f.exists()) return null;
            FileInputStream in = new FileInputStream(f);
            StringBuilder sb = new StringBuilder();
            InputStreamReader r = new InputStreamReader(in, StandardCharsets.UTF_8);
            char[] buf = new char[4096];
            int n;
            while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
            r.close();
            return new JSONObject(sb.toString());
        } catch (Exception e) {
            return null; // 文件损坏时按全新数据处理，不让坏档堵死入口
        }
    }

    private static void writeAtomic(File f, String s) {
        try {
            File tmp = new File(f.getParentFile(), f.getName() + ".tmp");
            Writer w = new OutputStreamWriter(new FileOutputStream(tmp), StandardCharsets.UTF_8);
            w.write(s);
            w.close();
            if (!tmp.renameTo(f)) {
                // 极端平台 rename 失败时退回直接覆盖
                FileOutputStream out = new FileOutputStream(f);
                out.write(s.getBytes(StandardCharsets.UTF_8));
                out.close();
                tmp.delete();
            }
        } catch (Exception ignore) {
        }
    }
}
