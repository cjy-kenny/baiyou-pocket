package com.baiyou.pocket;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.media.AudioManager;
import android.media.ToneGenerator;
import android.os.Bundle;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.baiyou.pocket.data.GameMeta;
import com.baiyou.pocket.data.Registry;
import com.baiyou.pocket.data.Store;
import com.baiyou.pocket.game.Game;

import org.json.JSONObject;

/** 游戏宿主：所有游戏共用这一个 Activity（Manifest 里声明为独立进程 :game）。
 *
 *  职责：
 *  1. 顶栏 HUD：退出 | 游戏名+最高分 | 实时分数 | 暂停 | 重开
 *  2. 生命周期接线：系统/用户暂停 → game.pauseGame()，恢复 → resumeGame()
 *  3. 结算：弹窗（再来一局/退出），会话内多局数据合并后一次性回传主进程
 *  4. 崩溃兜底：SafeFrame 捕触摸异常、循环异常由 Game.runSafe 上抛到这里；
 *     兜底失败还有 :game 进程隔离 + CrashGuard 落盘（见 App/CrashGuard）
 *  5. 数据回传：setResult(best/coins/plays/timeSec)，主进程合并进 store.json；
 *     最高分由本进程实时写 scores/<id>.json，崩溃也不丢。 */
public class GameHostActivity extends Activity implements Game.Host {
    public static final String EXTRA_GAME = "game_id";

    private String gameId;
    private GameMeta meta;
    private Game game;

    private SafeFrame container;
    private TextView tvName, tvSub, tvScore;
    private ViewGroup pauseOverlay;

    private boolean userPaused;      // 用户点了暂停（区别于系统暂停）
    private boolean settling;        // 结算弹窗期间
    private boolean finishing;
    private boolean roundStarted;    // 第一局是否已开始（玩法弹窗关闭后才开）
    private boolean introShowing;    // 玩法弹窗显示中

    // 计时（游玩时长）
    private boolean clockOn = true;
    private long lastResumeAt;
    private long activeMs;

    // 本会话（可多次重开）累计，退出时一次性回传
    private int accBest, accCoins, accPlays;
    private boolean roundAwarded;
    private boolean newBestThisRound;

    private int best;                       // 历史最高分（scores/<id>.json）
    private JSONObject scoreObj;            // 本游戏 scores 文件缓存
    private ToneGenerator tg;
    private boolean soundOn = true;

    // ---------------- 生命周期 ----------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        gameId = getIntent().getStringExtra(EXTRA_GAME);
        meta = Registry.get(gameId);
        if (meta == null || !meta.playable()) {
            finish();
            return;
        }

        soundOn = Store.loadStore(this).optBoolean("sound", true);
        scoreObj = Store.loadScore(this, gameId);
        best = scoreObj.optInt("best", 0);
        lastResumeAt = SystemClock.elapsedRealtime();

        buildUi();

        game = Registry.create(gameId);
        if (game == null) {
            Toast.makeText(this, "游戏加载失败", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        game.bind(this, this);
        container.addView(game.createView(),
                new FrameLayout.LayoutParams(-1, -1));
        container.addView(pauseOverlay,
                new FrameLayout.LayoutParams(-1, -1));
        showIntro(true); // 先看玩法介绍，点"开始"才开第一局
    }

    /** 玩法介绍弹窗。first=true 时按钮为"开始"，关闭后才开第一局。 */
    private void showIntro(boolean first) {
        introShowing = true;
        syncClock(false);
        if (game != null) game.pauseGame();
        pauseOverlay.setVisibility(View.GONE);
        new AlertDialog.Builder(this)
                .setTitle("玩法 · " + meta.name)
                .setMessage(meta.tutorial)
                .setCancelable(false)
                .setPositiveButton(first ? "开始" : "继续", (d, w) -> {
                    introShowing = false;
                    if (first) {
                        startRound();
                    } else {
                        syncClock(true);
                        game.resumeGame();
                    }
                })
                .show();
    }

    private void buildUi() {
        int cBg = attr(R.attr.cBg), cCard = attr(R.attr.cCard);
        int cText = attr(R.attr.cText), cTextSub = attr(R.attr.cTextSub);
        int cAccent = attr(R.attr.cAccent);
        float den = getResources().getDisplayMetrics().density;
        int dp8 = Math.round(8 * den), dp12 = Math.round(12 * den);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(cBg);

        // ---- 顶栏 ----
        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setBackgroundColor(cCard);
        bar.setGravity(Gravity.CENTER_VERTICAL);
        bar.setPadding(dp12, dp8, dp12, dp8);

        TextView btnExit = barText("退出", cTextSub);
        btnExit.setOnClickListener(v -> exitToMain());

        LinearLayout nameCol = new LinearLayout(this);
        nameCol.setOrientation(LinearLayout.VERTICAL);
        nameCol.setPadding(dp12, 0, 0, 0);
        tvName = new TextView(this);
        tvName.setText(meta.name);
        tvName.setTextColor(cText);
        tvName.setTextSize(16);
        tvName.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvSub = new TextView(this);
        tvSub.setText("最高 " + best);
        tvSub.setTextColor(cTextSub);
        tvSub.setTextSize(11);
        nameCol.addView(tvName);
        nameCol.addView(tvSub);

        tvScore = new TextView(this);
        tvScore.setText("0");
        tvScore.setTextColor(cAccent);
        tvScore.setTextSize(24);
        tvScore.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        tvScore.setPadding(0, 0, dp12, 0);

        TextView btnPause = barText("暂停", cText);
        btnPause.setOnClickListener(v -> userPause());
        TextView btnRestart = barText("重开", cText);
        btnRestart.setOnClickListener(v -> userRestart());
        TextView btnHow = barText("玩法", cTextSub);
        btnHow.setOnClickListener(v -> showIntro(roundStarted));

        LinearLayout.LayoutParams lpName = new LinearLayout.LayoutParams(0, -2, 1);
        bar.addView(btnExit);
        bar.addView(nameCol, lpName);
        bar.addView(tvScore);
        bar.addView(btnPause);
        bar.addView(btnRestart);
        bar.addView(btnHow);

        // ---- 游戏容器（触摸兜底）----
        container = new SafeFrame(this);
        container.setBackgroundColor(cBg);

        // ---- 暂停遮罩 ----
        pauseOverlay = buildPauseOverlay(cCard, cText, cTextSub, cAccent, den);

        root.addView(bar, new LinearLayout.LayoutParams(-1, -2));
        root.addView(container, new LinearLayout.LayoutParams(-1, 0, 1));
        setContentView(root, new ViewGroup.LayoutParams(-1, -1));
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private TextView barText(String s, int color) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(15);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setPadding(10, 8, 10, 8);
        return t;
    }

    private ViewGroup buildPauseOverlay(int cCard, int cText, int cTextSub, int cAccent, float den) {
        FrameLayout ov = new FrameLayout(this);
        ov.setBackgroundColor(0xCC101218);
        ov.setVisibility(View.GONE);
        ov.setClickable(true);

        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER);

        TextView title = new TextView(this);
        title.setText("已暂停");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(26);
        title.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);

        TextView hint = new TextView(this);
        hint.setText("休息一下，回来继续");
        hint.setTextColor(0xFF9AA0AC);
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);

        box.addView(title);
        box.addView(hint);
        int pad = Math.round(20 * den);
        box.setPadding(pad, pad, pad, pad);

        LinearLayout btns = new LinearLayout(this);
        btns.setOrientation(LinearLayout.VERTICAL);
        btns.setGravity(Gravity.CENTER_HORIZONTAL);
        btns.addView(overlayBtn("继续游戏", cAccent, v -> userResume()));
        btns.addView(overlayBtn("重新开始", 0xFFFFFFFF, v -> userRestart()));
        btns.addView(overlayBtn("退出游戏", 0xFF9AA0AC, v -> exitToMain()));
        box.addView(btns, new LinearLayout.LayoutParams(-2, -2));

        ov.addView(box, lp);
        return ov;
    }

    private TextView overlayBtn(String s, int color, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextColor(color);
        t.setTextSize(18);
        t.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(40, 24, 40, 24);
        t.setOnClickListener(l);
        return t;
    }

    private int attr(int id) {
        android.content.res.TypedArray a = obtainStyledAttributes(new int[]{id});
        int c = a.getColor(0, 0xFF888888);
        a.recycle();
        return c;
    }

    // ---------------- 暂停 / 重开 / 退出 ----------------

    private void userPause() {
        if (settling || finishing || userPaused) return;
        userPaused = true;
        syncClock(false);
        game.pauseGame();
        pauseOverlay.setVisibility(View.VISIBLE);
    }

    private void userResume() {
        userPaused = false;
        pauseOverlay.setVisibility(View.GONE);
        syncClock(true);
        game.resumeGame();
    }

    private void userRestart() {
        if (settling || finishing) return;
        userPaused = false;
        pauseOverlay.setVisibility(View.GONE);
        startRound();
    }

    private void startRound() {
        accBest = Math.max(accBest, 0); // accBest 保留会话最高
        newBestThisRound = false;
        roundAwarded = false;
        roundStarted = true;
        tvScore.setText("0");
        syncClock(true);
        game.restartGame();             // 游戏自行 startLoop / setScore(0)
    }

    private void exitToMain() {
        if (finishing) return;
        settling = true;
        finishing = true;
        syncClock(false);
        // 半局退出也按当前分结算金币（一次性）
        if (!roundAwarded && game != null) {
            roundAwarded = true;
            accCoins += Math.min(50, Math.max(accBest, curScore()) / 10);
        }
        setResult(RESULT_OK, resultIntent());
        try {
            game.pauseGame();
        } catch (Throwable ignore) {
        }
        finish();
    }

    private int curScore() {
        try {
            return Integer.parseInt(tvScore.getText().toString());
        } catch (Exception e) {
            return 0;
        }
    }

    private void syncClock(boolean on) {
        long now = SystemClock.elapsedRealtime();
        if (on && !clockOn) {
            lastResumeAt = now;
            clockOn = true;
        } else if (!on && clockOn) {
            activeMs += now - lastResumeAt;
            clockOn = false;
        }
    }

    private Intent resultIntent() {
        Intent i = new Intent();
        i.putExtra("game_id", gameId);
        i.putExtra("best", accBest);
        i.putExtra("coins", accCoins);
        i.putExtra("plays", accPlays);
        i.putExtra("timeSec", (int) (activeMs / 1000));
        return i;
    }

    // ---------------- Game.Host 实现 ----------------

    @Override
    public void setScore(int score) {
        tvScore.setText(String.valueOf(score));
        if (score > best) {
            best = score;
            newBestThisRound = true;
            tvSub.setText("最高 " + best);
            try {
                scoreObj.put("best", best);
                Store.saveScore(this, gameId, scoreObj); // 游戏进程负责写，实时落盘
            } catch (Throwable ignore) {
            }
        }
    }

    @Override
    public void gameOver(int finalScore) {
        if (settling || finishing) return;
        settling = true;
        syncClock(false);
        game.pauseGame();

        if (finalScore > accBest) accBest = finalScore;
        if (!roundAwarded) {
            roundAwarded = true;
            accCoins += Math.min(50, Math.max(0, finalScore) / 10);
        }
        accPlays++;
        try {
            scoreObj.put("plays", scoreObj.optInt("plays", 0) + 1);
            scoreObj.put("last", System.currentTimeMillis());
            scoreObj.put("best", Math.max(best, finalScore));
            Store.saveScore(this, gameId, scoreObj);
        } catch (Throwable ignore) {
        }
        if (newBestThisRound && finalScore > 0) beep(3);

        String msg = "本局得分  " + finalScore
                + "\n历史最高  " + Math.max(best, finalScore)
                + (newBestThisRound && finalScore > 0 ? "\n\n★ 新纪录！金币 +" + Math.min(50, finalScore / 10)
                : "\n\n金币 +" + Math.min(50, Math.max(0, finalScore) / 10));
        new AlertDialog.Builder(this)
                .setTitle(meta.name + " · 本局结束")
                .setMessage(msg)
                .setCancelable(false)
                .setPositiveButton("再来一局", (d, w) -> {
                    settling = false;
                    startRound();
                })
                .setNegativeButton("退出", (d, w) -> exitToMain())
                .show();
    }

    @Override
    public void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    @Override
    public boolean soundOn() {
        return soundOn;
    }

    @Override
    public void beep(int kind) {
        if (!soundOn) return;
        try {
            if (tg == null) tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 70);
            int tone;
            switch (kind) {
                case 1: tone = ToneGenerator.TONE_PROP_BEEP2; break;
                case 2: tone = ToneGenerator.TONE_CDMA_PIP; break;
                case 3: tone = ToneGenerator.TONE_PROP_ACK; break;
                default: tone = ToneGenerator.TONE_PROP_BEEP; break;
            }
            tg.startTone(tone, 120);
        } catch (Throwable ignore) {
        }
    }

    @Override
    public void crash(Throwable t) {
        crashExit(t);
    }

    // ---------------- 系统生命周期 ----------------

    @Override
    protected void onResume() {
        super.onResume();
        if (introShowing) return; // 玩法弹窗期间计时暂停
        syncClock(true);
        if (!userPaused && !settling && !finishing && roundStarted && game != null) {
            game.resumeGame();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        syncClock(false);
        if (game != null) {
            try {
                game.pauseGame();
            } catch (Throwable ignore) {
            }
        }
    }

    @Override
    public void onBackPressed() {
        exitToMain();
    }

    @Override
    protected void onDestroy() {
        if (game != null) {
            try {
                game.destroy();
            } catch (Throwable ignore) {
            }
        }
        if (tg != null) {
            try {
                tg.release();
            } catch (Throwable ignore) {
            }
            tg = null;
        }
        super.onDestroy();
    }

    // ---------------- 崩溃兜底 ----------------

    private void crashExit(Throwable t) {
        if (finishing) return;
        finishing = true;
        CrashGuard.dump(this, "game-" + gameId, t);
        // 半局崩溃也按当前分数结算金币，避免玩家因崩溃吃亏
        if (!roundAwarded) {
            roundAwarded = true;
            accCoins += Math.min(50, Math.max(accBest, curScore()) / 10);
        }
        Toast.makeText(this, "游戏出现异常，已安全退出（其它内容不受影响）", Toast.LENGTH_LONG).show();
        setResult(RESULT_OK, resultIntent());
        finish();
    }

    /** 触摸事件兜底：游戏视图内任何触摸处理异常都收敛到这里。 */
    private class SafeFrame extends FrameLayout {
        SafeFrame(android.content.Context c) {
            super(c);
        }

        @Override
        public boolean dispatchTouchEvent(MotionEvent ev) {
            try {
                return super.dispatchTouchEvent(ev);
            } catch (Throwable t) {
                crashExit(t);
                return true;
            }
        }
    }
}
