package com.baiyou.pocket.desktop;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Handler;
import android.view.View;

import com.baiyou.pocket.CrashGuard;
import com.baiyou.pocket.data.Achievements;
import com.baiyou.pocket.data.GameMeta;
import com.baiyou.pocket.data.Registry;
import com.baiyou.pocket.data.ResultMerger;
import com.baiyou.pocket.data.Store;
import com.baiyou.pocket.game.Game;
import com.baiyou.pocket.ui.IconDrawable;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import org.json.JSONArray;
import org.json.JSONObject;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/** 网页壳：同一份共享游戏代码的第三块屏。
 *  - 游戏逻辑/渲染/输入全部串行在单条"游戏线程"上（Handler 网页驱动）；
 *  - /stream 以 MJPEG（multipart/x-mixed-replace）把当前帧推给浏览器 <img>；
 *  - /input 把网页上的指针事件回传为触摸事件（视图 1:1 逻辑坐标，零换算）；
 *  - 结算走与 PC/Android 完全相同的 ResultMerger/Achievements/Store，
 *    进度与 PC 端共用 desktop-data/ 目录。
 *  同一时间只有一台"游戏机"：后打开的游戏接管画面。 */
public final class WebServer {
    private WebServer() {
    }

    private static final int STREAM_FPS_MS = 45;

    // ---------------- 游戏线程（一切游戏代码在此串行） ----------------

    private static final ScheduledExecutorService GAME = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "web-game");
        t.setDaemon(true);
        return t;
    });

    /** 网页版 Handler 驱动：全部投递到游戏线程。 */
    private static void installDriver() {
        final Map<Runnable, ScheduledFuture<?>> tasks = new ConcurrentHashMap<>();
        Handler.setDriver(new Handler.Driver() {
            @Override
            public void post(Runnable r, long delayMs) {
                cancel(r);
                tasks.put(r, GAME.schedule(r, Math.max(0, delayMs), TimeUnit.MILLISECONDS));
            }

            @Override
            public void cancel(Runnable r) {
                ScheduledFuture<?> f = tasks.remove(r);
                if (f != null) f.cancel(false);
            }
        });
    }

    // ---------------- 会话状态 ----------------

    private static final Session session = new Session();

    private static final class Session {
        volatile String gameId;
        volatile GameMeta meta;
        volatile Game game;
        volatile View view;
        volatile int fw, fh;
        volatile BufferedImage frame;          // 最新一帧（游戏线程写，HTTP 线程读）
        volatile byte[] frameJpg;              // 最新一帧的 JPEG（编码一次，多路流共享）
        volatile long jpgVer;                  // 帧版本号（画面没变不重复编码）
        volatile int score, best, finalScore;
        volatile boolean over, paused;
        volatile boolean pendingIntro;         // 玩法介绍挂起中（点"开始"前不开局）
        volatile String toast;
        volatile long toastAt;
        volatile List<String> newAch = new ArrayList<>(); // 网页线程读后即清
        volatile long sessionStart, pausedTotal, pausedAt;
        volatile JSONObject scoreObj;
        int accBest, accCoins, accPlays;
        boolean roundAwarded, newBestThisRound;
    }

    private static Context ctx() {
        return new Context();
    }

    // ---------------- 启动 ----------------

    public static void start(int port) {
        installDriver();
        try {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/", WebServer::route);
            // 必须给线程池：默认实现是单线程串行处理，MJPEG 长连接会把其它请求全部饿死
            server.setExecutor(Executors.newCachedThreadPool());
            server.start();
            System.out.println("百游口袋网页版已启动: http://127.0.0.1:" + port
                    + "  （同一局域网的手机可访问 http://<电脑IP>:" + port + "）");
        } catch (IOException e) {
            System.out.println("端口 " + port + " 启动失败：" + e);
        }
    }

    private static void route(HttpExchange ex) {
        try {
            String path = ex.getRequestURI().getPath();
            Map<String, String> q = query(ex.getRequestURI().getRawQuery());
            if (path.equals("/") || path.equals("/index.html")) home(ex);
            else if (path.equals("/icon")) icon(ex, q.get("game"));
            else if (path.equals("/play")) playPage(ex, q.get("game"));
            else if (path.equals("/stream")) stream(ex, q.get("game"));
            else if (path.equals("/meta")) meta(ex, q.get("game"));
            else if (path.equals("/input")) input(ex, q.get("game"), q.get("action"), q.get("x"), q.get("y"));
            else if (path.equals("/ctl")) ctl(ex, q.get("game"), q.get("op"));
            else if (path.equals("/ach")) achPage(ex);
            else if (path.equals("/settings")) settingsPage(ex);
            else if (path.equals("/storeop")) storeop(ex, q);
            else if (path.equals("/ach")) achPage(ex);
            else if (path.equals("/settings")) settingsPage(ex);
            else if (path.equals("/storeop")) storeop(ex, q);
            else notFound(ex);
        } catch (Throwable t) {
            CrashGuard.dump(ctx(), "web", t);
            try {
                byte[] msg = "服务内部错误".getBytes(StandardCharsets.UTF_8);
                ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                ex.sendResponseHeaders(500, msg.length);
                ex.getResponseBody().write(msg);
                ex.close();
            } catch (IOException ignore) {
            }
        }
    }

    private static Map<String, String> query(String raw) {
        Map<String, String> out = new HashMap<>();
        if (raw == null || raw.isEmpty()) return out;
        for (String kv : raw.split("&")) {
            int i = kv.indexOf('=');
            if (i <= 0) continue;
            out.put(URLDecoder.decode(kv.substring(0, i), StandardCharsets.UTF_8),
                    URLDecoder.decode(kv.substring(i + 1), StandardCharsets.UTF_8));
        }
        return out;
    }

    private static void notFound(HttpExchange ex) throws IOException {
        bytes(ex, 404, "text/plain; charset=utf-8", "404");
    }

    private static void bytes(HttpExchange ex, int code, String type, String body) {
        try {
            ex.getResponseHeaders().set("Content-Type", type);
            byte[] b = body.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(code, b.length == 0 ? -1 : b.length);
            if (b.length > 0) ex.getResponseBody().write(b);
            ex.close();
        } catch (IOException ignore) {
            // 客户端提前断开（页面跳转/刷新）属正常现象
        }
    }

    // ---------------- 页面：首页 ----------------

    private static void head(StringBuilder sb, String title) {
        sb.append("<!doctype html><html lang='zh'><head><meta charset='utf-8'>")
                .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
                .append("<title>").append(title).append("</title><style>")
                .append("*{box-sizing:border-box}")
                .append("body{margin:0;background:#101116;color:#ECECF1;font-family:'Microsoft YaHei',sans-serif}")
                .append("a{text-decoration:none;color:inherit}")
                .append("button{font-family:inherit}")
                .append(".hero{background:linear-gradient(135deg,#4A5BD8 0%,#7B4FD8 100%);padding:24px 22px 18px;color:#fff}")
                .append(".hrow{display:flex;justify-content:space-between;align-items:flex-start}")
                .append(".htitle{font-size:28px;font-weight:bold;letter-spacing:1px}")
                .append(".hsub{opacity:.85;font-size:13px;margin-top:6px}")
                .append(".hbtns{display:flex;gap:8px}")
                .append(".hbtn{color:#fff;background:rgba(255,255,255,.16);border-radius:10px;padding:7px 14px;font-size:13px}")
                .append(".hbtn:hover{background:rgba(255,255,255,.3)}")
                .append(".hstats{display:flex;gap:26px;margin-top:16px}")
                .append(".hstat b{display:block;font-size:20px}")
                .append(".hstat span{font-size:11px;opacity:.75}")
                .append(".bar{position:sticky;top:0;background:#101116;padding:14px 20px 10px;z-index:5}")
                .append(".search{width:100%;background:#171923;border:1px solid #262A33;border-radius:999px;color:#ECECF1;padding:10px 18px;font-size:14px;outline:none;font-family:inherit}")
                .append(".search:focus{border-color:#7B7FF5}")
                .append(".chips{display:flex;gap:8px;overflow-x:auto;padding:10px 20px 6px;scrollbar-width:none}")
                .append(".chip{flex:0 0 auto;border:1px solid #2E3140;background:#171923;color:#9AA0B4;border-radius:999px;padding:6px 16px;font-size:13px;cursor:pointer}")
                .append(".chip.on{background:var(--c,#7B7FF5);border-color:var(--c,#7B7FF5);color:#fff;font-weight:bold}")
                .append(".sec{padding:8px 20px 0}")
                .append(".sectt{font-size:13px;color:#9AA0B4;font-weight:bold;margin-bottom:8px}")
                .append(".rrow{display:flex;gap:10px;overflow-x:auto;padding-bottom:6px;scrollbar-width:none}")
                .append(".grid{display:grid;grid-template-columns:repeat(auto-fill,minmax(108px,1fr));gap:14px;padding:10px 20px 24px;max-width:920px}")
                .append(".card{position:relative;background:#171923;border:1px solid #23262F;border-radius:16px;padding:14px 6px 12px;text-align:center;display:block;transition:transform .15s,border-color .15s}")
                .append(".card img{width:60px;height:60px}")
                .append(".card .nm{font-size:13px;font-weight:bold;margin-top:9px;color:#ECECF1}")
                .append(".card .bs{font-size:10px;margin-top:3px;color:#7B7FF5}")
                .append(".card .ct{font-size:10px;color:#8B8E9C;margin-top:2px}")
                .append(".card.off{opacity:.4}")
                .append(".card.on:hover{border-color:var(--c);transform:translateY(-3px)}")
                .append(".fav{position:absolute;top:8px;right:10px;color:#FFC24B;font-size:12px}")
                .append(".ft{color:#565B6B;font-size:11px;text-align:center;padding:14px 0 26px}")
                .append(".btn{background:#7B7FF5;color:#fff;border:0;border-radius:10px;padding:8px 16px;font-size:14px;cursor:pointer;font-family:inherit;margin:2px}")
                .append(".btn.gray{background:#2A2D37;color:#8B8E9C}")
                .append(".btn.red{background:#3A2226;color:#E8848C}")
                .append(".arow{display:flex;justify-content:space-between;align-items:center;padding:12px 4px;border-bottom:1px solid #23262F}")
                .append(".an{font-weight:bold;font-size:14px}.ad{font-size:11px;color:#8B8E9C;margin-top:2px}")
                .append(".as{font-size:12px}.as.ok{color:#7B7FF5}.as.no{color:#565B6B}")
                .append(".wrap{max-width:680px;margin:0 auto;padding:6px 20px 30px}")
                .append(".pgt{font-size:20px;font-weight:bold;padding:18px 20px 4px}")
                .append("</style></head><body>");
    }

    private static void home(HttpExchange ex) throws IOException {
        JSONObject store = Store.loadStore(ctx());
        JSONArray rec = store.optJSONArray("recent");

        StringBuilder sb = new StringBuilder();
        head(sb, "百游口袋");
        sb.append("<div class='hero'><div class='hrow'><div>")
                .append("<div class='htitle'>百游口袋</div><div class='hsub'>100 款小游戏 · 点开即玩 · 全离线</div></div>")
                .append("<div class='hbtns'><a class='hbtn' href='/ach'>成就</a><a class='hbtn' href='/settings'>设置</a></div></div>")
                .append("<div class='hstats'>")
                .append("<div class='hstat'><b>").append(store.optInt("coins", 0)).append("</b><span>金币</span></div>")
                .append("<div class='hstat'><b>").append(store.optInt("playsTotal", 0)).append("</b><span>累计局数</span></div>")
                .append("<div class='hstat'><b>").append(store.optInt("timeSec", 0) / 60).append("</b><span>游玩分钟</span></div>")
                .append("<div class='hstat'><b>100</b><span>全部开放</span></div>")
                .append("</div></div>");
        sb.append("<div class='bar'><input id='q' class='search' placeholder='搜索游戏…' oninput='applyF()'>")
                .append("<div class='chips' id='chips'></div></div>");
        // 最近玩过
        sb.append("<div class='sec' id='recentWrap' style='display:").append(
                rec != null && rec.length() > 0 ? "block" : "none").append("'>")
                .append("<div class='sectt'>最近玩过</div><div class='rrow'>");
        if (rec != null) {
            for (int i = 0; i < rec.length(); i++) {
                String rid = rec.optJSONObject(i).optString("id");
                GameMeta rm = Registry.get(rid);
                if (rm == null) continue;
                int rbest = Store.loadScore(ctx(), rid).optInt("best", 0);
                sb.append("<a class='rcard' href='/play?game=").append(rid).append("'>")
                        .append("<img loading='lazy' src='/icon?game=").append(rid).append("'>")
                        .append("<div class='nm'>").append(rm.name).append("</div>")
                        .append("<div class='bs'>").append(rbest > 0 ? "最高 " + rbest : "未挑战").append("</div>")
                        .append("</a>");
            }
        }
        sb.append("</div></div>");
        // 全部游戏网格
        sb.append("<div class='grid' id='grid'>");
        for (GameMeta m : Registry.ALL) {
            boolean on = m.playable();
            boolean fav = favOf(store, m.id);
            int best = Store.loadScore(ctx(), m.id).optInt("best", 0);
            int color = com.baiyou.pocket.ui.IconDrawable.colorOf(m);
            String open = on
                    ? "<a class='card on' style='--c:" + color + "' data-name='" + m.name
                            + "' data-cat='" + m.category + "' data-fav='" + (fav ? 1 : 0)
                            + "' href='/play?game=" + m.id + "'>"
                    : "<div class='card off' data-name='" + m.name + "' data-cat='" + m.category + "'>";
            sb.append(open);
            sb.append("<img loading='lazy' src='/icon?game=").append(m.id).append("'>");
            sb.append("<div class='nm'>").append(m.name).append("</div>");
            if (on) {
                sb.append(best > 0
                        ? "<div class='bs'>最高 " + best + "</div>"
                        : "<div class='bs' style='color:#565B6B'>未挑战</div>");
            }
            sb.append("<div class='ct'>").append(m.category).append(on ? "" : " · 敬请期待").append("</div>");
            if (fav) sb.append("<span class='fav'>★</span>");
            sb.append(on ? "</a>" : "</div>");
        }
        sb.append("</div>");
        sb.append("<div class='ft'>右键卡片可收藏 · 数据只存本机</div>");
        sb.append("<script>")
                .append("var fcat='全部',fq='';")                .append("var chips=document.getElementById('chips');")                .append("['全部','收藏'].concat([")
                .append("'益智','数字','反应','街机','记忆','牌类','休闲','体育','射击','文字']").append(").forEach(function(cname,i){")                .append("  var b=document.createElement('button');b.className='chip'+(i===0?' on':'');")                .append("  b.style.setProperty('--c',['#7B7FF5','#FFC24B','#5B8DEF','#E8574F','#9B59F6','#2FA36B','#12B5A5','#E85FA8','#F2784B','#6C5CE7','#D46A9E','#4A90E2'][i]||'#7B7FF5');")                .append("  b.textContent=cname;b.onclick=function(){fcat=cname;")                .append("    document.querySelectorAll('.chip').forEach(function(x){x.classList.remove('on');});")                .append("    b.classList.add('on');applyF();};")                .append("  chips.appendChild(b);});")                .append("function applyF(){var q=document.getElementById('q').value.trim();")                .append("  document.querySelectorAll('#grid .card').forEach(function(c){")                .append("    var okc=fcat==='全部'||c.dataset.cat===fcat||(fcat==='收藏'&&c.dataset.fav==='1');")                .append("    var okq=!q||c.dataset.name.indexOf(q)>=0||c.dataset.cat.indexOf(q)>=0;")                .append("    c.style.display=(okc&&okq)?'':'none';});}")                .append("</script>");
        bytes(ex, 200, "text/html; charset=utf-8", sb.toString());
    }
    // ---------------- 成就页 / 设置页 / 存档操作 ----------------

    private static void achPage(HttpExchange ex) throws IOException {
        JSONObject store = Store.loadStore(ctx());
        StringBuilder sb = new StringBuilder();
        head(sb, "成就 · 百游口袋");
        sb.append("<div class='pgt'>我的成就</div>");
        int got = 0;
        for (String[] d : com.baiyou.pocket.data.Achievements.DEFS)
            if (com.baiyou.pocket.data.Achievements.has(store, d[0])) got++;
        sb.append("<div class='note'>已达成 ").append(got).append(" / ").append(com.baiyou.pocket.data.Achievements.DEFS.length)
                .append(" · 累计 ").append(store.optInt("playsTotal", 0)).append(" 局 · 金币 ").append(store.optInt("coins", 0)).append("</div>");
        sb.append("<div class='wrap'>");
        for (String[] d : com.baiyou.pocket.data.Achievements.DEFS) {
            boolean ok = com.baiyou.pocket.data.Achievements.has(store, d[0]);
            sb.append("<div class='arow'><div><div class='an'>").append(d[1])
                    .append("</div><div class='ad'>").append(d[2]).append("</div></div>")
                    .append("<div class='as ").append(ok ? "ok" : "no").append("'>")
                    .append(ok ? "已达成" : "未达成").append("</div></div>");
        }
        sb.append("</div><div class='ft'>完成对局与收藏即可解锁成就</div>");
        bytes(ex, 200, "text/html; charset=utf-8", sb.toString());
    }

    private static void settingsPage(HttpExchange ex) throws IOException {
        JSONObject store = Store.loadStore(ctx());
        StringBuilder sb = new StringBuilder();
        head(sb, "设置 · 百游口袋");
        sb.append("<div class='pgt'>设置</div>");
        sb.append("<div class='note'>音效开关用于安卓 / PC 端；网页端暂无声音</div>");
        sb.append("<div class='wrap'>");
        sb.append("<button class='btn' onclick=\"fetch('/storeop?op=sound&v=1').then(()=>location='/settings')\">音效 开</button> ")
                .append("<button class='btn gray' onclick=\"fetch('/storeop?op=sound&v=0').then(()=>location='/settings')\">音效 关</button>");
        sb.append("<p class='note'>清除数据将删除本机的金币、成就、最高分与收藏，且无法恢复。</p>");
        sb.append("<button class='btn red' onclick=\"if(confirm('确定清除全部数据？不可恢复！')){fetch('/storeop?op=wipe').then(()=>location='/');}\">清除全部数据</button>");
        sb.append("</div><div class='ft'>百游口袋 v1.0 · 全离线</div>");
        bytes(ex, 200, "text/html; charset=utf-8", sb.toString());
    }

    private static void storeop(HttpExchange ex, java.util.Map<String, String> q) throws IOException {
        String op = q.get("op");
        android.content.Context c = ctx();
        if ("sound".equals(op)) {
            JSONObject s = Store.loadStore(c);
            s.put("sound", "1".equals(q.get("v")));
            Store.saveStore(c, s);
        } else if ("wipe".equals(op)) {
            new java.io.File(c.getFilesDir(), "store.json").delete();
            delDir(new java.io.File(c.getFilesDir(), "scores"));
            delDir(new java.io.File(c.getFilesDir(), "logs"));
        }
        bytes(ex, 200, "text/plain", "ok");
    }

    private static void delDir(java.io.File dir) {
        java.io.File[] fs = dir.listFiles();
        if (fs != null) for (java.io.File f : fs) {
            if (f.isDirectory()) delDir(f);
            else f.delete();
        }
        dir.delete();
    }

    private static boolean favOf(JSONObject store, String id) {
        JSONArray favs = store.optJSONArray("favs");
        for (int i = 0; favs != null && i < favs.length(); i++)
            if (favs.optString(i).equals(id)) return true;
        return false;
    }

    // ---------------- 图标 ----------------

    private static void icon(HttpExchange ex, String gid) throws IOException {
        GameMeta m = Registry.get(gid);
        BufferedImage img = new BufferedImage(96, 96, BufferedImage.TYPE_INT_ARGB);
        if (m != null) {
            Graphics2D g = img.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            IconDrawable d = new IconDrawable(m);
            d.setBounds(new Rect(0, 0, 96, 96));
            d.draw(new Canvas(g));
            g.dispose();
        }
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(img, "png", bo);
        ex.getResponseHeaders().set("Content-Type", "image/png");
        ex.getResponseHeaders().set("Cache-Control", "no-cache");
        ex.sendResponseHeaders(200, bo.size());
        ex.getResponseBody().write(bo.toByteArray());
        ex.close();
    }

    // ---------------- 页面：游戏 ----------------

    private static void playPage(HttpExchange ex, String gid) throws IOException {
        GameMeta m = Registry.get(gid);
        if (m == null || !m.playable()) {
            bytes(ex, 200, "text/html; charset=utf-8",
                    "<meta http-equiv='refresh' content='0;/'>该游戏暂未开放，正在返回首页…");
            return;
        }
        StringBuilder sb = new StringBuilder();
        head(sb, m.name + " · 百游口袋");
        sb.append("<div class='hdr'><div><h1 id='nm'>").append(m.name).append("</h1>")
                .append("<div class='sub'>分数 <b id='score' style='color:#7B7FF5'>0</b> · 最高 <b id='best'>0</b></div></div>")
                .append("<div><button class='btn' id='bPause' onclick='ctl(\"pause\")'>暂停</button> ")
                .append("<button class='btn' onclick='ctl(\"restart\")'>重开</button> ")
                .append("<button class='btn' onclick='howto()'>玩法</button> ")
                .append("<button class='btn gray' onclick='ctl(\"exit\")'>退出</button></div></div>");
        sb.append("<div id='stage'><img id='screen' src='/stream?game=").append(gid).append("' alt='加载中…'>")
                .append("<div id='overlay' class='hide'></div></div>");
        sb.append("<div class='ops'>手指就是手柄</div>");
        sb.append("<script>")
                .append("var gid='").append(gid).append("',gname='").append(m.name)
                .append("',ovKey='',manualInfo=false,down=false,lastSend=0;")
                .append("var ov=document.getElementById('overlay'),scr=document.getElementById('screen');")
                .append("function ctl(op){fetch('/ctl?game='+gid+'&op='+op).then(r=>r.text()).then(t=>{if(t==='exit')location='/';else refresh();});}")
                .append("function send(a,e){var r=scr.getBoundingClientRect();")
                .append("var x=(e.clientX-r.left)*scr.naturalWidth/r.width,y=(e.clientY-r.top)*scr.naturalHeight/r.height;")
                .append("fetch('/input?game='+gid+'&action='+a+'&x='+Math.round(x)+'&y='+Math.round(y),{method:'POST'});}")
                .append("scr.addEventListener('pointerdown',function(e){down=true;send(0,e);e.preventDefault();});")
                .append("scr.addEventListener('pointermove',function(e){if(!down)return;var n=Date.now();if(n-lastSend<40)return;lastSend=n;send(2,e);e.preventDefault();});")
                .append("window.addEventListener('pointerup',function(e){if(!down)return;down=false;send(1,e);});")
                .append("scr.addEventListener('touchmove',function(e){e.preventDefault();},{passive:false});")
                .append("function hideOv(){ov.className='hide';ov.innerHTML='';ovKey='';}")
                .append("function addBtn(label,fn){var b=document.createElement('button');b.className='btn';b.textContent=label;b.addEventListener('click',function(){fn();});ov.appendChild(b);}")
                .append("function ovTitle(t){var h=document.createElement('div');h.className='big';h.textContent=t;ov.appendChild(h);}")
                .append("function ovText(t){var d=document.createElement('div');d.className='tut';d.textContent=t;ov.appendChild(d);}")
                .append("function showIntro(text,first,infoOnly){ov.className='show';ov.innerHTML='';")
                .append("  ovTitle('玩法 · '+gname);ovText(text);")
                .append("  if(infoOnly){addBtn('知道了',function(){manualInfo=false;hideOv();});}")
                .append("  else{addBtn(first?'开始':'继续',function(){manualInfo=false;fetch('/ctl?game='+gid+'&op='+(first?'begin':'resume')).then(function(){hideOv();refresh();});});}")
                .append("}")
                .append("function showPause(){ov.className='show';ov.innerHTML='';ovTitle('已暂停');")
                .append("  addBtn('继续游戏',function(){ctl('resume');});addBtn('重新开始',function(){ctl('restart');});addBtn('退出游戏',function(){ctl('exit');});}")
                .append("function showSettle(s){ov.className='show';ov.innerHTML='';ovTitle('本局结束');")
                .append("  var d=document.createElement('div');d.innerHTML='得分 <b>'+s.finalScore+'</b> · 最高 <b>'+s.best+'</b>'+(s.newBest?'<br>★ 新纪录！':'')+(s.coins?'<br>金币 +'+s.coins:'');ov.appendChild(d);")
                .append("  if(s.ach){var a=document.createElement('div');a.className='ach';a.textContent='达成成就：'+s.ach;ov.appendChild(a);}")
                .append("  addBtn('再来一局',function(){ctl('restart');});addBtn('退出',function(){ctl('exit');});}")
                .append("function howto(){if(ovKey==='over')return;")
                .append("  fetch('/ctl?game='+gid+'&op=pause').then(function(){")
                .append("    fetch('/meta?game='+gid).then(r=>r.json()).then(function(s){manualInfo=!s.over;showIntro(s.tutorial,false,!s.over);});")
                .append("  });}")
                .append("function refresh(){fetch('/meta?game='+gid).then(r=>r.json()).then(function(s){")
                .append("if(!s.ok){location='/';return;}")
                .append("document.getElementById('score').textContent=s.score;")
                .append("document.getElementById('best').textContent=s.best;")
                .append("document.getElementById('bPause').textContent=s.paused?'继续':'暂停';")
                .append("if(manualInfo)return;")
                .append("var k=s.over?'over':(s.intro?'intro':(s.paused?'paused':''));")
                .append("if(k===ovKey)return;ovKey=k;")
                .append("if(k==='intro')showIntro(s.tutorial,true,false);")
                .append("else if(k==='paused')showPause();")
                .append("else if(k==='over')showSettle(s);")
                .append("else hideOv();")
                .append("});}")
                .append("setInterval(refresh,700);refresh();")
                .append("</script>");
        sb.append("<style>#stage{position:relative;width:min(94vw,420px);margin:4px auto}")
                .append("#screen{width:100%;display:block;border-radius:14px;background:#20242E;touch-action:none;user-select:none}")
                .append(".btn{background:#7B7FF5;color:#fff;border:0;border-radius:10px;padding:8px 14px;font-size:14px;cursor:pointer;font-family:inherit;margin:2px}")
                .append(".btn.gray{background:#2A2D37;color:#8B8E9C}")
                .append(".ops{color:#8B8E9C;font-size:12px;text-align:center;padding:8px 0 24px}")
                .append("#overlay{position:absolute;inset:0;background:rgba(16,18,24,.88);border-radius:14px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px;text-align:center;padding:0 18px}")
                .append("#overlay.hide{display:none}.big{font-size:22px;font-weight:bold}")
                .append(".tut{color:#C9CBDA;font-size:15px;line-height:1.7;max-width:320px}")
                .append(".ach{color:#FFC24B;font-size:13px;margin:4px 0}</style></body></html>");
        bytes(ex, 200, "text/html; charset=utf-8", sb.toString());
    }

    // ---------------- MJPEG 实时流 ----------------

    private static void stream(HttpExchange ex, String gid) throws IOException {
        GameMeta m = Registry.get(gid);
        if (m == null || !m.playable()) {
            notFound(ex);
            return;
        }
        switchTo(gid);
        ex.getResponseHeaders().set("Content-Type", "multipart/x-mixed-replace; boundary=frame");
        ex.getResponseHeaders().set("Cache-Control", "no-store");
        ex.sendResponseHeaders(200, 0);
        OutputStream out = ex.getResponseBody();
        long sentVer = -1;
        try {
            while (true) {
                // 只在画面版本变化时写出（renderFrame 已编码缓存，这里零编码）
                byte[] jpg = session.frameJpg;
                if (jpg != null && session.jpgVer != sentVer) {
                    sentVer = session.jpgVer;
                    out.write(("--frame\r\nContent-Type: image/jpeg\r\nContent-Length: " + jpg.length + "\r\n\r\n")
                            .getBytes(StandardCharsets.US_ASCII));
                    out.write(jpg);
                    out.write("\r\n".getBytes(StandardCharsets.US_ASCII));
                    out.flush();
                }
                Thread.sleep(STREAM_FPS_MS);
            }
        } catch (IOException | InterruptedException ignore) {
            // 客户端断开/页面关闭
        } finally {
            try {
                ex.close();
            } catch (Throwable ignore) {
            }
        }
    }

    private static void switchTo(String gid) {
        if (gid.equals(session.gameId)) return;
        final GameMeta m = Registry.get(gid);
        if (m == null || !m.playable()) return;
        final GameMeta old = session.meta;
        GAME.execute(() -> {
            try {
                switchTask(gid, m, old);
            } catch (Throwable t) {
                CrashGuard.dump(ctx(), "web-switch-" + gid, t);
                session.toast = "切换失败：" + t;
                session.toastAt = System.currentTimeMillis();
            }
        });
        // 等游戏线程完成切换（含玩法介绍挂起），避免首帧取到旧画面
        for (int i = 0; i < 80 && (!gid.equals(session.gameId) || session.pendingIntro); i++) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                break;
            }
        }
    }

    /** 会话切换的真实工作（游戏线程上执行）。 */
    private static void switchTask(String gid, GameMeta m, GameMeta old) {
        // 离开旧游戏：把未结算的半局按当前分入库（与 PC/Android 一致）
        if (session.game != null && old != null) {
            settleRound(false, 0);
            try {
                session.game.destroy();
            } catch (Throwable ignore) {
            }
        }
        session.gameId = gid;
        session.meta = m;
        session.scoreObj = Store.loadScore(ctx(), gid);
        session.best = session.scoreObj.optInt("best", 0);
        session.score = 0;
        session.finalScore = 0;
        session.accBest = 0;
        session.accCoins = 0;
        session.accPlays = 0;
        session.roundAwarded = false;
        session.newBestThisRound = false;
        session.over = false;
        session.paused = false;
        session.sessionStart = System.currentTimeMillis();
        session.pausedTotal = 0;
        session.pausedAt = 0;
        Game g = Registry.create(gid);
        if (g == null) {
            session.toast = "游戏加载失败";
            session.toastAt = System.currentTimeMillis();
            return;
        }
        session.game = g;
        g.bind(ctx(), host);
        View v = g.createView();
        session.view = v;
        // 游戏标脏 → 才重画重编码（画面静止时几乎零开销）
        v.invalidateHook = () -> frameDirty = true;
        frameDirty = true;
        if (g instanceof com.baiyou.pocket.game.CanvasGame) {
            com.baiyou.pocket.game.CanvasGame cg = (com.baiyou.pocket.game.CanvasGame) g;
            // 1.5x 超采样：MJPEG 帧更清晰，触摸仍按渲染像素换算回逻辑坐标
            float q = Math.min(1.5f, 640f / cg.viewWidth());
            session.fw = Math.round(cg.viewWidth() * q);
            session.fh = Math.round(cg.viewHeight() * q);
        } else {
            session.fw = 540;
            session.fh = 720;
        }
        v.performSizeChanged(session.fw, session.fh, 0, 0);
        g.restartGame(); // 首局由宿主启动（契约与两端一致）
        // 网页端先展示玩法介绍：暂停计时与逻辑，点"开始"再进入
        session.pendingIntro = true;
        session.paused = true;
        session.pausedAt = System.currentTimeMillis();
        g.pauseGame();
    }

    // ---------------- 帧渲染（游戏线程） ----------------

    static volatile boolean frameDirty = true;   // 画面有变化才重画+重编码
    static final javax.imageio.ImageWriter JPG_WRITER = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
    static final javax.imageio.ImageWriteParam JPG_PARAM = JPG_WRITER.getDefaultWriteParam();

    static {
        JPG_PARAM.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        JPG_PARAM.setCompressionQuality(0.85f);
    }

    static {
        GAME.scheduleWithFixedDelay(WebServer::renderFrame, 0, STREAM_FPS_MS, TimeUnit.MILLISECONDS);
    }

    private static void renderFrame() {
        try {
            Session s = session;
            int w = s.fw > 0 ? s.fw : 360, h = s.fh > 0 ? s.fh : 480;
            // 画面没变化时不重画也不重新编码（静止游戏几乎零开销）
            if (s.view != null && !frameDirty && s.frame != null && s.frameJpg != null) return;
            BufferedImage img;
            if (s.view == null) {
                img = placeholder();
            } else {
                img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g = img.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
                s.view.performDraw(new Canvas(g)); // 游戏 render + 基类特效自动叠加
                g.dispose();
            }
            s.frame = img;
            s.frameJpg = encodeJpg(img);
            s.jpgVer++;
            frameDirty = false;
        } catch (Throwable t) {
            // 帧渲染异常不拖垮游戏线程；若是游戏内异常已由兜底上报
        }
    }

    /** JPEG 编码（质量 0.85，静态画面走缓存）。仅在游戏线程调用，失败返回 null 沿用旧帧。 */
    private static byte[] encodeJpg(java.awt.image.BufferedImage img) {
        try {
            ByteArrayOutputStream bo = new ByteArrayOutputStream();
            javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(bo);
            JPG_WRITER.setOutput(ios);
            JPG_WRITER.write(null, new javax.imageio.IIOImage(img, null, null), JPG_PARAM);
            ios.flush();
            ios.close();
            return bo.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    private static BufferedImage placeholder() {
        BufferedImage img = new BufferedImage(360, 480, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(new java.awt.Color(0x20242E));
        g.fillRect(0, 0, 360, 480);
        g.setColor(new java.awt.Color(0x8B8E9C));
        g.setFont(g.getFont().deriveFont(java.awt.Font.BOLD, 18f));
        String s = "从首页选择一款游戏开始";
        g.drawString(s, 180 - g.getFontMetrics().stringWidth(s) / 2f, 240);
        g.dispose();
        return img;
    }

    // ---------------- 输入回传 ----------------

    private static void input(HttpExchange ex, String gid, String action, String xs, String ys) throws IOException {
        GameMeta m = Registry.get(gid);
        if (m == null || !m.playable()) {
            bytes(ex, 404, "text/plain", "no game");
            return;
        }
        switchTo(gid);
        final int a = parse(action, 0), x = parse(xs, 0), y = parse(ys, 0);
        GAME.execute(() -> {
            // 会话已切换/拆除的陈旧请求直接丢弃
            if (session.game == null || session.view == null || !gid.equals(session.gameId)) return;
            if (session.over || session.paused) return;
            try {
                session.view.performTouch(new android.view.MotionEvent(a, x, y));
            } catch (Throwable t) {
                host.crash(t);
            }
        });
        bytes(ex, 200, "text/plain", "");
    }

    private static int parse(String s, int def) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            return def;
        }
    }

    // ---------------- 控制 ----------------

    private static void ctl(HttpExchange ex, String gid, String op) throws IOException {
        GameMeta m = Registry.get(gid);
        if (m == null || !m.playable()) {
            bytes(ex, 404, "text/plain", "no game");
            return;
        }
        switchTo(gid);
        final String o = op == null ? "" : op;
        GAME.execute(() -> {
            // 会话已拆除/已切换时丢弃陈旧控制请求
            if (session.game == null || !gid.equals(session.gameId)) {
                if (o.equals("exit")) session.gameId = "";
                return;
            }
            try {
                switch (o) {
                    case "pause":
                        if (!session.over && !session.paused && !session.pendingIntro) {
                            session.paused = true;
                            session.pausedAt = System.currentTimeMillis();
                            session.game.pauseGame();
                        }
                        break;
                    case "resume":
                        if (session.paused && !session.pendingIntro) {
                            session.paused = false;
                            session.pausedTotal += System.currentTimeMillis() - session.pausedAt;
                            session.game.resumeGame();
                        }
                        break;
                    case "begin":
                        // 玩法介绍点"开始"：解除开局挂起
                        if (session.pendingIntro) {
                            session.pendingIntro = false;
                            session.paused = false;
                            session.pausedTotal += System.currentTimeMillis() - session.pausedAt;
                            session.pausedAt = 0;
                            session.game.resumeGame();
                        }
                        break;
                    case "restart":
                        session.pendingIntro = false;
                        session.paused = false;
                        if (session.pausedAt > 0)
                            session.pausedTotal += System.currentTimeMillis() - session.pausedAt;
                        session.pausedAt = 0;
                        session.newBestThisRound = false;
                        session.roundAwarded = false;
                        session.score = 0;
                        session.over = false;
                        session.game.restartGame();
                        break;
                    case "exit":
                        if (!session.over) settleRound(true, session.score);
                        try {
                            session.game.destroy();
                        } catch (Throwable ignore) {
                        }
                        session.game = null;
                        session.view = null;
                        session.gameId = "";
                        session.meta = null;
                        break;
                    default:
                        break;
                }
            } catch (Throwable t) {
                host.crash(t);
            }
        });
        bytes(ex, 200, "text/plain", o.equals("exit") ? "exit" : "ok");
    }

    // ---------------- 结算（与 PC/Android 同一套公式与入库） ----------------

    /** 一次完整的 gameOver；exit 时按当前分半局结算。在游戏线程上调用。 */
    private static void settleRound(boolean halfRound, int finalScore) {
        Session s = session;
        if (s.meta == null) return;
        long now = System.currentTimeMillis();
        long active = now - s.sessionStart - s.pausedTotal
                - (s.pausedAt > 0 ? now - s.pausedAt : 0);
        int timeSec = (int) Math.max(0, active / 1000);
        if (finalScore > s.accBest) s.accBest = finalScore;
        if (!s.roundAwarded) {
            s.roundAwarded = true;
            s.accCoins += Math.min(50, Math.max(0, finalScore) / 10);
        }
        if (!halfRound) s.accPlays++;
        try {
            s.scoreObj.put("best", Math.max(s.best, finalScore));
            s.scoreObj.put("plays", s.scoreObj.optInt("plays", 0) + (halfRound ? 0 : 1));
            s.scoreObj.put("last", now);
            Store.saveScore(ctx(), s.meta.id, s.scoreObj);
        } catch (Throwable ignore) {
        }
        if (s.accBest > 0 || s.accCoins > 0 || s.accPlays > 0) {
            ResultMerger.merge(ctx(), s.meta.id, s.accCoins, s.accPlays, s.accBest, timeSec);
        }
        if (!s.newAch.isEmpty()) s.newAch = new ArrayList<>();
        List<String> ach = Achievements.checkAndSave(ctx(), Store.loadStore(ctx()));
        if (!ach.isEmpty()) s.newAch = ach;
    }

    private static final Game.Host host = new Game.Host() {
        @Override
        public void setScore(int score) {
            Session s = session;
            s.score = score;
            if (score > s.best) {
                s.best = score;
                s.newBestThisRound = true;
                try {
                    s.scoreObj.put("best", s.best);
                    Store.saveScore(ctx(), s.meta.id, s.scoreObj); // 实时落盘
                } catch (Throwable ignore) {
                }
            }
        }

        @Override
        public void gameOver(int finalScore) {
            Session s = session;
            if (s.over) return;
            s.over = true;
            s.finalScore = finalScore;
            s.game.pauseGame();
            settleRound(false, finalScore);
            if (s.newBestThisRound && finalScore > 0) Beep.play(3);
        }

        @Override
        public void toast(String msg) {
            session.toast = msg;
            session.toastAt = System.currentTimeMillis();
        }

        @Override
        public boolean soundOn() {
            return false; // 网页端暂不出声
        }

        @Override
        public void beep(int kind) {
        }

        @Override
        public void crash(Throwable t) {
            CrashGuard.dump(ctx(), "web-game-" + session.gameId, t);
            if (!session.over) {
                if (!session.roundAwarded) {
                    session.roundAwarded = true;
                    session.accCoins += Math.min(50, Math.max(session.accBest, session.score) / 10);
                }
                settleRound(true, session.score);
            }
            session.over = true;
            session.finalScore = session.score;
            session.toast = "游戏出现异常，已安全退出";
            session.toastAt = System.currentTimeMillis();
        }
    };

    // ---------------- /meta ----------------

    private static void meta(HttpExchange ex, String gid) throws IOException {
        Session s = session;
        JSONObject o = new JSONObject();
        if (s.meta == null || s.gameId == null || s.gameId.isEmpty()) {
            o.put("ok", false);
        } else {
            o.put("ok", true);
            o.put("game", s.gameId);
            o.put("name", s.meta.name);
            o.put("tutorial", s.meta.tutorial);
            o.put("intro", s.pendingIntro);
            o.put("score", s.score);
            o.put("best", s.best);
            o.put("over", s.over);
            o.put("finalScore", s.finalScore);
            o.put("newBest", s.newBestThisRound && s.finalScore > 0 && s.finalScore >= s.best);
            o.put("coins", Math.min(50, Math.max(0, s.finalScore) / 10));
            o.put("paused", s.paused);
            long now = System.currentTimeMillis();
            if (s.toast != null && now - s.toastAt < 2600) o.put("toast", s.toast);
            if (!s.newAch.isEmpty()) {
                o.put("ach", String.join("、", s.newAch));
                s.newAch = new ArrayList<>();
            }
        }
        bytes(ex, 200, "application/json; charset=utf-8", o.toString());
    }
}
