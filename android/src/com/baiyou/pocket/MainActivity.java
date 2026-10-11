package com.baiyou.pocket;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.baiyou.pocket.data.Achievements;
import com.baiyou.pocket.data.GameMeta;
import com.baiyou.pocket.data.Registry;
import com.baiyou.pocket.data.ResultMerger;
import com.baiyou.pocket.data.Store;
import com.baiyou.pocket.ui.IconDrawable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** 首页：100 款游戏入口 + 分类 + 搜索 + 收藏 + 最近玩过 + 成就 + 设置。
 *  纯程序化布局（无布局 XML），颜色走日/夜主题 attr，自动适配深色模式；
 *  网格列数随屏幕宽度自适应（手机 2-3 列，平板 4-5 列）。 */
public class MainActivity extends Activity {
    private static final int REQ_GAME = 1;

    private LinearLayout content;
    private LinearLayout chipsRow;
    private EditText etSearch;
    private TextView tvCoins, tvSubTitle;
    private int cBg, cCard, cCardSub, cText, cTextSub, cAccent;
    private int cols = 3;
    private float den;

    private String cat = "全部";
    private String query = "";
    private JSONObject store;
    private final Map<String, JSONObject> scoreCache = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        den = getResources().getDisplayMetrics().density;
        cBg = attr(R.attr.cBg);
        cCard = attr(R.attr.cCard);
        cCardSub = attr(R.attr.cCardSub);
        cText = attr(R.attr.cText);
        cTextSub = attr(R.attr.cTextSub);
        cAccent = attr(R.attr.cAccent);

        int wdp = getResources().getConfiguration().screenWidthDp;
        cols = Math.max(2, Math.min(5, wdp / 165));

        setContentView(buildUi());
        etSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void onTextChanged(CharSequence s, int a, int b, int c) {
            }

            public void afterTextChanged(Editable s) {
                query = s.toString().trim();
                rebuild();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        rebuild();
    }

    private int attr(int id) {
        android.content.res.TypedArray a = obtainStyledAttributes(new int[]{id});
        int c = a.getColor(0, 0xFF888888);
        a.recycle();
        return c;
    }

    private int dp(float v) {
        return Math.round(v * den);
    }

    // ================= 界面搭建 =================

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(cBg);

        // ---- 头部 ----
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(16), dp(12), dp(16), dp(4));

        LinearLayout titleCol = new LinearLayout(this);
        titleCol.setOrientation(LinearLayout.VERTICAL);

        TextView tvTitle = new TextView(this);
        tvTitle.setText("百游口袋");
        tvTitle.setTextColor(cText);
        tvTitle.setTextSize(24);
        tvTitle.setTypeface(Typeface.DEFAULT_BOLD);

        tvSubTitle = new TextView(this);
        tvSubTitle.setTextColor(cTextSub);
        tvSubTitle.setTextSize(12);

        titleCol.addView(tvTitle);
        titleCol.addView(tvSubTitle);

        tvCoins = new TextView(this);
        tvCoins.setTextColor(cAccent);
        tvCoins.setTextSize(14);
        tvCoins.setTypeface(Typeface.DEFAULT_BOLD);
        tvCoins.setPadding(dp(12), dp(6), dp(12), dp(6));
        tvCoins.setBackground(round(cCardSub, dp(16)));
        tvCoins.setOnClickListener(v -> showAchievements());

        TextView btnAch = chipText("成就");
        btnAch.setOnClickListener(v -> showAchievements());
        TextView btnSet = chipText("设置");
        btnSet.setOnClickListener(v -> showSettings());

        header.addView(titleCol, new LinearLayout.LayoutParams(0, -2, 1));
        header.addView(tvCoins);
        header.addView(btnAch);
        header.addView(btnSet);

        // ---- 搜索框 ----
        etSearch = new EditText(this);
        etSearch.setHint("搜索 100 款小游戏…");
        etSearch.setHintTextColor(cTextSub);
        etSearch.setTextColor(cText);
        etSearch.setTextSize(14);
        etSearch.setSingleLine(true);
        etSearch.setBackground(round(cCard, dp(12)));
        etSearch.setPadding(dp(14), dp(10), dp(14), dp(10));
        LinearLayout.LayoutParams lpSearch = new LinearLayout.LayoutParams(-1, -2);
        lpSearch.setMargins(dp(16), dp(8), dp(16), dp(4));
        root.addView(header, new LinearLayout.LayoutParams(-1, -2));
        root.addView(etSearch, lpSearch);

        // ---- 分类 chips ----
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        chipsRow = new LinearLayout(this);
        chipsRow.setOrientation(LinearLayout.HORIZONTAL);
        chipsRow.setPadding(dp(12), dp(6), dp(12), dp(6));
        hs.addView(chipsRow);
        root.addView(hs, new LinearLayout.LayoutParams(-1, -2));

        // ---- 内容区 ----
        ScrollView scroll = new ScrollView(this);
        scroll.setFillViewport(true);
        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(12), dp(4), dp(12), dp(24));
        scroll.addView(content, new ViewGroup.LayoutParams(-1, -2));
        root.addView(scroll, new LinearLayout.LayoutParams(-1, 0, 1));

        rebuildChips();
        return root;
    }

    private TextView chipText(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(cText);
        t.setTextSize(14);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(10), dp(6), dp(10), dp(6));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.setMargins(dp(4), 0, 0, 0);
        t.setLayoutParams(lp);
        return t;
    }

    private GradientDrawable round(int color, float radiusPx) {
        GradientDrawable g = new GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radiusPx);
        return g;
    }

    private void rebuildChips() {
        chipsRow.removeAllViews();
        List<String> labels = new ArrayList<>();
        labels.add("全部");
        labels.add("收藏");
        for (String c : Registry.categories()) labels.add(c);
        for (String label : labels) {
            boolean on = label.equals(cat);
            TextView t = new TextView(this);
            t.setText(label);
            t.setTextSize(13);
            t.setTypeface(on ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT);
            t.setPadding(dp(14), dp(7), dp(14), dp(7));
            t.setBackground(round(on ? cAccent : cCard, dp(16)));
            t.setTextColor(on ? 0xFFFFFFFF : cTextSub);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
            lp.setMargins(dp(4), 0, dp(4), 0);
            t.setLayoutParams(lp);
            t.setOnClickListener(v -> {
                cat = label;
                rebuildChips();
                rebuild();
            });
            chipsRow.addView(t);
        }
    }

    // ================= 列表重建 =================

    private void rebuild() {
        store = Store.loadStore(this);
        scoreCache.clear();
        for (GameMeta m : Registry.ALL) {
            scoreCache.put(m.id, Store.loadScore(this, m.id)); // 主进程对 scores 只读
        }

        // 头部
        JSONArray recent = store.optJSONArray("recent");
        tvCoins.setText("金币 " + store.optInt("coins", 0));
        tvSubTitle.setText("100 款全开放 · 累计 " + store.optInt("playsTotal", 0)
                + " 局 · " + store.optInt("timeSec", 0) / 60 + " 分钟");

        content.removeAllViews();

        boolean browse = cat.equals("全部") && query.isEmpty();

        // ---- 最近玩过 ----
        if (browse && recent != null && recent.length() > 0) {
            TextView lt = sectionTitle("最近玩过");
            content.addView(lt);
            HorizontalScrollView hs = new HorizontalScrollView(this);
            hs.setHorizontalScrollBarEnabled(false);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(6), 0, dp(6));
            int shown = 0;
            for (int i = 0; i < recent.length() && shown < 10; i++) {
                String id = recent.optJSONObject(i).optString("id");
                GameMeta m = Registry.get(id);
                if (m == null) continue;
                row.addView(miniCard(m));
                shown++;
            }
            hs.addView(row);
            content.addView(hs, new LinearLayout.LayoutParams(-1, -2));
        }

        // ---- 过滤 ----
        List<GameMeta> list = new ArrayList<>();
        boolean favOnly = cat.equals("收藏");
        for (GameMeta m : Registry.ALL) {
            boolean ok;
            if (favOnly) ok = isFav(m.id);
            else if (browse) ok = true;
            else ok = m.category.equals(cat);
            if (ok && !query.isEmpty()) {
                ok = m.name.contains(query) || m.category.contains(query);
            }
            if (ok) list.add(m);
        }

        TextView gt = sectionTitle((favOnly ? "收藏" : (browse ? "全部游戏" : cat))
                + " · " + list.size() + " 款");
        content.addView(gt);

        // ---- 网格 ----
        if (list.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText(query.isEmpty() ? "这里还什么都没有" : "没有找到 \"" + query + "\"");
            empty.setTextColor(cTextSub);
            empty.setTextSize(14);
            empty.setGravity(Gravity.CENTER);
            empty.setPadding(0, dp(40), 0, dp(40));
            content.addView(empty, new LinearLayout.LayoutParams(-1, -2));
        } else {
            for (int i = 0; i < list.size(); i += cols) {
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                for (int j = 0; j < cols; j++) {
                    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
                    lp.setMargins(dp(3), dp(3), dp(3), dp(3));
                    if (i + j < list.size()) row.addView(cell(list.get(i + j)), lp);
                    else row.addView(new View(this), lp);
                }
                content.addView(row, new LinearLayout.LayoutParams(-1, -2));
            }
        }

        // ---- 页脚 ----
        TextView footer = new TextView(this);
        footer.setText("长按图标，收藏喜欢的游戏");
        footer.setTextColor(cTextSub);
        footer.setTextSize(11);
        footer.setGravity(Gravity.CENTER);
        footer.setPadding(0, dp(18), 0, 0);
        content.addView(footer, new LinearLayout.LayoutParams(-1, -2));
    }

    private int playableCount() {
        int n = 0;
        for (GameMeta m : Registry.ALL) if (m.playable()) n++;
        return n;
    }

    private TextView sectionTitle(String s) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(cTextSub);
        t.setTextSize(13);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setPadding(dp(4), dp(10), 0, dp(2));
        return t;
    }

    private boolean isFav(String id) {
        JSONArray favs = store.optJSONArray("favs");
        if (favs == null) return false;
        for (int i = 0; i < favs.length(); i++) if (favs.optString(i).equals(id)) return true;
        return false;
    }

    // ---- 网格单元 ----

    private View cell(final GameMeta m) {
        JSONObject sc = scoreCache.get(m.id);
        int best = sc == null ? 0 : sc.optInt("best", 0);

        FrameLayout cell = new FrameLayout(this);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(8), dp(12), dp(8), dp(10));
        card.setBackground(round(cCard, dp(14)));
        card.setClickable(true);

        View icon = new View(this);
        icon.setBackground(new IconDrawable(m));
        LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(52), dp(52));
        lpIcon.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(icon, lpIcon);

        TextView name = new TextView(this);
        name.setText(m.name);
        name.setTextColor(m.playable() ? cText : cTextSub);
        name.setTextSize(13);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setSingleLine(true);
        name.setGravity(Gravity.CENTER);
        card.addView(name, new LinearLayout.LayoutParams(-1, -2));

        TextView sub = new TextView(this);
        sub.setText(m.playable() ? (best > 0 ? "最高 " + best : "来挑战") : "敬请期待");
        sub.setTextColor(cTextSub);
        sub.setTextSize(10);
        sub.setSingleLine(true);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub, new LinearLayout.LayoutParams(-1, -2));

        cell.addView(card, new FrameLayout.LayoutParams(-1, -2));
        card.setOnClickListener(v -> {
            if (m.playable()) launch(m);
            else Toast.makeText(this, "《" + m.name + "》正在开发中，敬请期待", Toast.LENGTH_SHORT).show();
        });
        card.setOnLongClickListener(v -> {
            toggleFav(m.id, m.name);
            return true;
        });

        if (isFav(m.id)) {
            TextView star = new TextView(this);
            star.setText("★");
            star.setTextColor(0xFFFFC24B);
            star.setTextSize(14);
            FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.TOP | Gravity.END);
            lp.setMargins(0, dp(4), dp(6), 0);
            cell.addView(star, lp);
        }
        return cell;
    }

    // ---- 最近玩过的小卡 ----

    private View miniCard(final GameMeta m) {
        JSONObject sc = scoreCache.get(m.id);
        int best = sc == null ? 0 : sc.optInt("best", 0);
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);
        card.setPadding(dp(10), dp(10), dp(10), dp(8));
        card.setBackground(round(cCard, dp(12)));
        card.setClickable(true);

        View icon = new View(this);
        icon.setBackground(new IconDrawable(m));
        icon.setMinimumWidth(dp(44));
        LinearLayout.LayoutParams lpIcon = new LinearLayout.LayoutParams(dp(44), dp(44));
        lpIcon.gravity = Gravity.CENTER_HORIZONTAL;
        card.addView(icon, lpIcon);

        TextView name = new TextView(this);
        name.setText(m.name);
        name.setTextColor(cText);
        name.setTextSize(12);
        name.setTypeface(Typeface.DEFAULT_BOLD);
        name.setSingleLine(true);
        name.setGravity(Gravity.CENTER);
        card.addView(name, new LinearLayout.LayoutParams(-1, -2));

        TextView sub = new TextView(this);
        sub.setText(best > 0 ? "最高 " + best : "未挑战");
        sub.setTextColor(cTextSub);
        sub.setTextSize(10);
        sub.setGravity(Gravity.CENTER);
        card.addView(sub, new LinearLayout.LayoutParams(-1, -2));

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(96), -2);
        lp.setMargins(dp(4), 0, dp(4), 0);
        card.setLayoutParams(lp);
        card.setOnClickListener(v -> launch(m));
        return card;
    }

    // ================= 进入游戏与数据回传 =================

    private void launch(GameMeta m) {
        Intent i = new Intent(this, GameHostActivity.class);
        i.putExtra(GameHostActivity.EXTRA_GAME, m.id);
        startActivityForResult(i, REQ_GAME);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_GAME || data == null) return;
        String id = data.getStringExtra("game_id");
        if (id == null) return;
        GameMeta m = Registry.get(id);
        if (m == null) return;

        int coins = data.getIntExtra("coins", 0);
        int plays = data.getIntExtra("plays", 0);
        int best = data.getIntExtra("best", 0);
        int timeSec = data.getIntExtra("timeSec", 0);

        store = Store.loadStore(this);
        ResultMerger.merge(this, id, coins, plays, best, timeSec);
        store = Store.loadStore(this);

        List<String> newly = Achievements.checkAndSave(this, store);
        if (coins > 0) Toast.makeText(this, "《" + m.name + "》获得金币 +" + coins, Toast.LENGTH_SHORT).show();
        if (!newly.isEmpty()) showAchievementToast(newly);
        rebuild();
    }

    private void toggleFav(String id, String name) {
        JSONArray favs = store.optJSONArray("favs");
        boolean has = false;
        for (int i = 0; i < favs.length(); i++) if (favs.optString(i).equals(id)) has = true;
        if (has) {
            for (int i = favs.length() - 1; i >= 0; i--) if (favs.optString(i).equals(id)) favs.remove(i);
            Toast.makeText(this, "已取消收藏《" + name + "》", Toast.LENGTH_SHORT).show();
        } else {
            favs.put(id);
            Toast.makeText(this, "已收藏《" + name + "》", Toast.LENGTH_SHORT).show();
        }
        Store.saveStore(this, store);
        rebuild();
    }

    // ================= 成就 =================

    private void showAchievementToast(List<String> names) {
        StringBuilder sb = new StringBuilder("达成成就：");
        for (int i = 0; i < names.size(); i++) {
            if (i > 0) sb.append("、");
            sb.append(names.get(i));
        }
        Toast.makeText(this, sb.toString(), Toast.LENGTH_LONG).show();
    }

    private void showAchievements() {
        store = Store.loadStore(this);
        JSONArray ach = store.optJSONArray("ach");
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        box.setPadding(pad, dp(6), pad, dp(6));

        JSONArray played = store.optJSONArray("playedIds");
        TextView stat = new TextView(this);
        stat.setText("累计 " + store.optInt("playsTotal", 0) + " 局 · 玩过 " + played.length()
                + " 款 · 时长 " + (store.optInt("timeSec", 0) / 60) + " 分钟 · 金币 " + store.optInt("coins", 0));
        stat.setTextColor(cTextSub);
        stat.setTextSize(12);
        stat.setPadding(0, 0, 0, dp(10));
        box.addView(stat);

        for (String[] d : Achievements.DEFS) {
            boolean got = Achievements.has(store, d[0]);
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, dp(8), 0, dp(8));

            LinearLayout col = new LinearLayout(this);
            col.setOrientation(LinearLayout.VERTICAL);
            TextView n = new TextView(this);
            n.setText(d[1]);
            n.setTextColor(got ? cAccent : cText);
            n.setTextSize(14);
            n.setTypeface(Typeface.DEFAULT_BOLD);
            TextView de = new TextView(this);
            de.setText(d[2]);
            de.setTextColor(cTextSub);
            de.setTextSize(11);
            col.addView(n);
            col.addView(de);
            row.addView(col, new LinearLayout.LayoutParams(0, -2, 1));

            TextView st = new TextView(this);
            st.setText(got ? "已达成" : "未达成");
            st.setTextColor(got ? cAccent : cTextSub);
            st.setTextSize(12);
            row.addView(st, new LinearLayout.LayoutParams(-2, -2));

            View line = new View(this);
            line.setBackgroundColor(cCardSub);
            box.addView(row, new LinearLayout.LayoutParams(-1, -2));
            box.addView(line, new LinearLayout.LayoutParams(-1, dp(1)));
        }

        new AlertDialog.Builder(this)
                .setTitle("我的成就")
                .setView(wrapInScroll(box))
                .setPositiveButton("好的", null)
                .show();
    }

    private ScrollView wrapInScroll(View v) {
        ScrollView s = new ScrollView(this);
        s.addView(v, new ViewGroup.LayoutParams(-1, -2));
        return s;
    }

    // ================= 设置 =================

    private void showSettings() {
        store = Store.loadStore(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(20);
        box.setPadding(pad, dp(8), pad, dp(4));

        LinearLayout soundRow = new LinearLayout(this);
        soundRow.setOrientation(LinearLayout.HORIZONTAL);
        soundRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView soundLabel = new TextView(this);
        soundLabel.setText("音效（系统合成音）");
        soundLabel.setTextColor(cText);
        soundLabel.setTextSize(15);
        soundRow.addView(soundLabel, new LinearLayout.LayoutParams(0, -2, 1));
        final Switch sw = new Switch(this);
        sw.setChecked(store.optBoolean("sound", true));
        soundRow.addView(sw, new LinearLayout.LayoutParams(-2, -2));
        box.addView(soundRow, new LinearLayout.LayoutParams(-1, -2));

        TextView clear = new TextView(this);
        clear.setText("清除全部数据（金币/成就/最高分）");
        clear.setTextColor(0xFFE8574F);
        clear.setTextSize(15);
        clear.setPadding(0, dp(22), 0, dp(10));
        clear.setOnClickListener(v -> new AlertDialog.Builder(this)
                .setTitle("确认清除")
                .setMessage("将删除本机全部游戏数据（最高分、金币、成就、收藏），且无法恢复。")
                .setPositiveButton("清除", (d, w) -> wipeAll())
                .setNegativeButton("取消", null)
                .show());
        box.addView(clear, new LinearLayout.LayoutParams(-1, -2));

        TextView about = new TextView(this);
        about.setText("百游口袋 v1.0\n100 款小游戏合集 · 完全离线\n数据仅保存在本机");
        about.setTextColor(cTextSub);
        about.setTextSize(11);
        about.setGravity(Gravity.CENTER);
        box.addView(about, new LinearLayout.LayoutParams(-1, -2));

        AlertDialog dlg = new AlertDialog.Builder(this)
                .setTitle("设置")
                .setView(box)
                .setPositiveButton("完成", null)
                .show();
        sw.setOnCheckedChangeListener((b, v) -> {
            store = Store.loadStore(this);
            try {
                store.put("sound", v);
            } catch (Exception ignore) {
            }
            Store.saveStore(this, store);
        });
    }

    private void wipeAll() {
        new File(getFilesDir(), "store.json").delete();
        deleteDir(new File(getFilesDir(), "scores"));
        deleteDir(new File(getFilesDir(), "logs"));
        Toast.makeText(this, "已清除全部数据", Toast.LENGTH_SHORT).show();
        recreate();
    }

    private void deleteDir(File dir) {
        if (dir == null || !dir.exists()) return;
        File[] fs = dir.listFiles();
        if (fs != null) for (File f : fs) {
            if (f.isDirectory()) deleteDir(f);
            else f.delete();
        }
        dir.delete();
    }
}
