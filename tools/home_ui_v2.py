# -*- coding: utf-8 -*-
"""网页首页 UI 重做 + 成就页/设置页：
   1) 重写 head() CSS 与 home()（Hero+统计+彩色分类+收藏标记+最近玩过+客户端过滤）
   2) 新增 /ach /settings /storeop 端点
   幂等：按标记替换，重复运行安全。"""
import io

P = r'C:\Users\Administrator\.zcode\workspace\default\baiyou-pocket\desktop\src\com\baiyou\pocket\desktop\WebServer.java'

src = io.open(P, encoding='utf-8').read()
changed = []


def replace_between(src, start_marker, end_marker, new_block, tag):
    i = src.index(start_marker)
    j = src.index(end_marker, i)
    changed.append(tag)
    return src[:i] + new_block + src[j:]


# ---------- 1) head() ----------
NEW_HEAD = '''    private static void head(StringBuilder sb, String title) {
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
'''

if 'private static void head(' in src:
    i = src.index('    private static void head(')
    j = src.index('\n    // ---------------- 图标 ----------------', i)
    src = src[:i] + NEW_HEAD + src[j + 1:]
    changed.append('head')

# ---------- 2) home() ----------
NEW_HOME = '''    private static void home(HttpExchange ex) throws IOException {
        JSONObject store = Store.loadStore(ctx());
        JSONArray rec = store.optJSONArray("recent");

        StringBuilder sb = new StringBuilder();
        sb.append("<div class='hero'><div class='hrow'><div>")
                .append("<div class='htitle'>百游口袋</div><div class='hsub'>100 款小游戏 · 点开即玩 · 全离线</div></div>")
                .append("<div class='hbtns'><a class='hbtn' href='/ach'>成就</a><a class='hbtn' href='/settings'>设置</a></div></div>")
                .append("<div class='hstats'>")
                .append("<div class='hstat'><b>").append(coins(store)).append("</b><span>金币</span></div>")
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
                .append("var fcat='全部',fq='';\n")
                .append("var chips=document.getElementById('chips');\n")
                .append("['全部','收藏'].concat([")
                .append("'益智','数字','反应','街机','记忆','牌类','休闲','体育','射击','文字']").append(").forEach(function(cname,i){\n")
                .append("  var b=document.createElement('button');b.className='chip'+(i===0?' on':'');\n")
                .append("  b.style.setProperty('--c',['#7B7FF5','#FFC24B','#5B8DEF','#E8574F','#9B59F6','#2FA36B','#12B5A5','#E85FA8','#F2784B','#6C5CE7','#D46A9E','#4A90E2'][i]||'#7B7FF5');\n")
                .append("  b.textContent=cname;b.onclick=function(){fcat=cname;\n")
                .append("    document.querySelectorAll('.chip').forEach(function(x){x.classList.remove('on');});\n")
                .append("    b.classList.add('on');applyF();};\n")
                .append("  chips.appendChild(b);});\n")
                .append("function applyF(){var q=document.getElementById('q').value.trim();\n")
                .append("  document.querySelectorAll('#grid .card').forEach(function(c){\n")
                .append("    var okc=fcat==='全部'||c.dataset.cat===fcat||(fcat==='收藏'&&c.dataset.fav==='1');\n")
                .append("    var okq=!q||c.dataset.name.indexOf(q)>=0||c.dataset.cat.indexOf(q)>=0;\n")
                .append("    c.style.display=(okc&&okq)?'':'none';});}\n")
                .append("</script>");
        bytes(ex, 200, "text/html; charset=utf-8", sb.toString());
    }
'''

if 'private static void home(' in src:
    i = src.index('    private static void home(')
    j = src.index('\n    // ---------------- 图标 ----------------', i)
    src = src[:i] + NEW_HOME + src[j + 1:]
    changed.append('home')

# ---------- 3) route() 增加端点 ----------
anchor = '            else if (path.equals("/ctl")) ctl(ex, q.get("game"), q.get("op"));'
if anchor in src:
    src = src.replace(anchor, anchor +
            '\n            else if (path.equals("/ach")) achPage(ex);' +
            '\n            else if (path.equals("/settings")) settingsPage(ex);' +
            '\n            else if (path.equals("/storeop")) storeop(ex, q);', 1)
    changed.append('route')

# ---------- 4) 新页面与操作方法（插在 图标 区前） ----------
NEW_METHODS = '''    // ---------------- 成就页 / 设置页 / 存档操作 ----------------

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
        sb.append("<button class='btn' onclick=\\"fetch('/storeop?op=sound&v=1').then(()=>location='/settings')\\">音效 开</button> ")
                .append("<button class='btn gray' onclick=\\"fetch('/storeop?op=sound&v=0').then(()=>location='/settings')\\">音效 关</button>");
        sb.append("<p class='note'>清除数据将删除本机的金币、成就、最高分与收藏，且无法恢复。</p>");
        sb.append("<button class='btn red' onclick=\\"if(confirm('确定清除全部数据？不可恢复！')){fetch('/storeop?op=wipe').then(()=>location='/');}\\">清除全部数据</button>");
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
'''

MARK = '\n    // ---------------- 图标 ----------------'
if 'favOf(JSONObject store, String id)' not in src:
    i = src.index(MARK)
    src = src[:i] + '\n' + NEW_METHODS + src[i:]
    changed.append('methods')

io.open(P, 'w', encoding='utf-8', newline='').write(src)
print('完成:', changed)
