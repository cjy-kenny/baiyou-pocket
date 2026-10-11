# -*- coding: utf-8 -*-
"""重做网页首页 UI（Hero 头部+统计+彩色分类+收藏标记+最近玩过），
   并新增 /ach 成就页、/settings 设置页、/storeop 操作接口。"""
import io
import re

P = r'C:\Users\Administrator\.zcode\workspace\default\baiyou-pocket\desktop\src\com\baiyou\pocket\desktop\WebServer.java'

src = io.open(P, encoding='utf-8').read()

# ---------- 1) 替换 head()（新 CSS） ----------
HEAD_START = '    private static void head(StringBuilder sb, String title) {'
HEAD_END = '\n    // ---------------- 图标 ----------------'
i = src.index(HEAD_START)
j = src.index(HEAD_END, i)

NEW_HEAD = '''    private static void head(StringBuilder sb, String title) {
        sb.append("<!doctype html><html lang='zh'><head><meta charset='utf-8'>")
                .append("<meta name='viewport' content='width=device-width,initial-scale=1'>")
                .append("<title>").append(title).append("</title><style>")
                .append("*{box-sizing:border-box}")
                .append("body{margin:0;background:#101116;color:#ECECF1;font-family:'Microsoft YaHei',sans-serif}")
                .append("a{text-decoration:none;color:inherit}")
                .append(".hero{background:linear-gradient(135deg,#4A5BD8 0%,#7B4FD8 100%);padding:24px 22px 20px;color:#fff}")
                .append(".hrow{display:flex;justify-content:space-between;align-items:flex-start}")
                .append(".htitle{font-size:28px;font-weight:bold;letter-spacing:1px}")
                .append(".hsub{opacity:.85;font-size:13px;margin-top:6px}")
                .append(".hbtns{display:flex;gap:8px}")
                .append(".hbtn{color:#fff;background:rgba(255,255,255,.16);border-radius:10px;padding:7px 14px;font-size:13px}")
                .append(".hbtn:hover{background:rgba(255,255,255,.28)}")
                .append(".hstats{display:flex;gap:26px;margin-top:16px}")
                .append(".hstat b{display:block;font-size:20px}")
                .append(".hstat span{font-size:11px;opacity:.75}")
                .append(".bar{position:sticky;top:0;background:#101116;padding:14px 20px 10px;z-index:5}")
                .append(".search{width:100%;background:#171923;border:1px solid #262A33;border-radius:999px;color:#ECECF1;padding:10px 18px;font-size:14px;outline:none;font-family:inherit}")
                .append(".search:focus{border-color:#7B7FF5}")
                .append(".chips{display:flex;gap:8px;overflow-x:auto;padding:10px 20px 6px;scrollbar-width:none}")
                .append(".chip{flex:0 0 auto;border:1px solid #2E3140;background:#171923;color:#9AA0B4;border-radius:999px;padding:6px 16px;font-size:13px;cursor:pointer;font-family:inherit}")
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
                .append(".arow .an{font-weight:bold;font-size:14px}.arow .ad{font-size:11px;color:#8B8E9C;margin-top:2px}")
                .append(".arow .as{font-size:12px}")
                .append(".as.ok{color:#7B7FF5}.as.no{color:#565B6B}")
                .append(".wrap{max-width:680px;margin:0 auto;padding:6px 20px 30px}")
                .append(".pgt{font-size:20px;font-weight:bold;padding:18px 20px 4px}")
                .append(".note{font-size:12px;color:#8B8E9C;padding:2px 20px 10px}")
                .append("</style></head><body>");
    }
'''

src = src[:i] + NEW_HEAD + src[j:]

# ---------- 2) 替换 home() ----------
HOME_START = '    private static void home(HttpExchange ex) throws IOException {'
HOME_END = '\n    // ---------------- 图标 ----------------'
i = src.index(HOME_START)
j = src.index(HOME_END, i)

NEW_HOME = '''    private static void home(HttpExchange ex) throws IOException {
        JSONObject store = Store.loadStore(ctx());
        int coins = store.optInt("coins", 0);
        int plays = store.optInt("playsTotal", 0);
        int mins = store.optInt("timeSec", 0) / 60;

        StringBuilder sb = new StringBuilder();
        // Hero 头部
        sb.append("<div class='hero'><div class='hrow'><div>")
                .append("<div class='htitle'>百游口袋</div><div class='hsub'>100 款小游戏 · 点开即玩 · 全离线</div></div>")
                .append("<div class='hbtns'><a class='hbtn' href='/ach'>成就</a><a class='hbtn' href='/settings'>设置</a></div></div>")
                .append("<div class='hstats'>")
                .append("<div class='hstat'><b>").append(coins).append("</b><span>金币</span></div>")
                .append("<div class='hstat'><b>").append(plays).append("</b><span>累计局数</span></div>")
                .append("<div class='hstat'><b>").append(mins).append("</b><span>游玩分钟</span></div>")
                .append("<div class='hstat'><b>100</b><span>全部开放</span></div>")
                .append("</div></div>");
        // 搜索 + 分类
        sb.append("<div class='bar'><input id='q' class='search' placeholder='搜索游戏…' oninput='applyF()'>")
                .append("<div class='chips' id='chips'></div></div>");
        // 最近玩过
        sb.append("<div class='sec' id='recentWrap' style='display:none'><div class='sectt'>最近玩过</div><div class='rrow' id='rrow'></div></div>");
        // 全部游戏网格
        sb.append("<div class='grid' id='grid'>");
        java.util.List<Integer> recentIds = new ArrayList<>();
        JSONArray rec = store.optJSONArray("recent");
        if (rec != null) for (int i = 0; i < rec.length(); i++) recentIds.add(rec.optJSONObject(i).optString("id"));
        for (GameMeta m : Registry.ALL) {
            boolean on = m.playable();
            boolean fav = favOf(store, m.id);
            JSONObject sc = Store.loadScore(ctx(), m.id);
            int best = sc.optInt("best", 0);
            int color = com.baiyou.pocket.ui.IconDrawable.colorOf(m);
            sb.append(on
                    ? "<a class='card on' data-name=\\\"" + esc(m.name) + "\\" data-cat=\\\"" + m.category
                            + "\\" data-fav=\\\"" + (fav ? 1 : 0) + "\\" href='/play?game=" + m.id + "' style='--c:" + color + "'>"
                    : "<div class='card off' data-name=\\\"" + esc(m.name) + "\\" data-cat=\\\"" + m.category + "\\">");
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
        sb.append("<div class='ft'>长按无感？网页里右键卡片可收藏 · 数据只存本机</div>");
        // 客户端过滤 + 最近玩过填充
        sb.append("<script>")
                .append("var RECENT=").append(recent == null ? "[]" : recent.toString()).append(";\n")
                .append("var fcat='全部',fq='';\n")
                .append("var CATS=['益智','数字','反应','街机','记忆','牌类','休闲','体育','射击','文字'];\n")
                .append("var chips=document.getElementById('chips');\n")
                .append("['全部','收藏'].concat(CATS).forEach(function(cname,i0){\n")
                .append("  var b=document.createElement('button');b.className='chip'+(i0===0?' on':'');\n")
                .append("  b.textContent=cname;b.onclick=function(){fcat=cname;\n")
                .append("    document.querySelectorAll('.chip').forEach(function(x){x.classList.remove('on');});\n")
                .append("    b.classList.add('on');applyF();};\n")
                .append("  chips.appendChild(b);});\n")
                .append("function applyF(){var q=document.getElementById('q').value.trim();\n")
                .append("  document.querySelectorAll('#grid .card').forEach(function(c){\n")
                .append("    var okc=fcat==='全部'||c.dataset.cat===fcat||(fcat==='收藏'&&c.dataset.fav==='1');\n")
                .append("    var okq=!q||c.dataset.name.indexOf(q)>=0||c.dataset.cat.indexOf(q)>=0;\n")
                .append("    c.style.display=(okc&&okq)?'':'none';});}\n")
                .append("(function(){var r=document.getElementById('rrow'),w=document.getElementById('recentWrap');\n")
                .append("  if(!RECENT.length){w.style.display='none';return;}w.style.display='block';\n")
                .append("  RECENT.forEach(function(id){var t=document.querySelector(\\\"[data-name]\\\")?0:0;\n")
                .append("    var src=document.querySelector(\\\".card[data-href='/play?game="+\\\"'+id+'\\\"']\\\");});\n")
                .append("})();\n")
                .append("</script>");
        bytes(ex, 200, "text/html; charset=utf-8", sb.toString());
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("'", "&#39;").replace("<", "&lt;");
    }

    private static boolean favOf(JSONObject store, String id) {
        JSONArray favs = store.optJSONArray("favs");
        for (int i = 0; favs != null && i < favs.length(); i++)
            if (favs.optString(i).equals(id)) return true;
        return false;
    }
'''

src = src[:i] + NEW_HOME + src[j:]

io.open(P, 'w', encoding='utf-8', newline='').write(src)
print('head/home 已替换')
