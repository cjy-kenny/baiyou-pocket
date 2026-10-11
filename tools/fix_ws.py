# -*- coding: utf-8 -*-
"""修复 WebServer：补 JSONArray 导入、恢复 head()、修 coins 引用。"""
import io

P = r'C:\Users\Administrator\.zcode\workspace\default\baiyou-pocket\desktop\src\com\baiyou\pocket\desktop\WebServer.java'
src = io.open(P, encoding='utf-8').read()

# 1) JSONArray 导入
if 'import org.json.JSONArray;' not in src:
    src = src.replace('import org.json.JSONObject;', 'import org.json.JSONArray;\nimport org.json.JSONObject;', 1)
    print('import JSONArray added')

# 2) coins(store) 坏引用
if 'coins(store)' in src:
    src = src.replace('.append(coins(store)).append("</b><span>金币</span>")',
                      '.append(store.optInt("coins", 0)).append("</b><span>金币</span>")')
    print('coins ref fixed')

# 3) 恢复 head()
if 'private static void head(' not in src:
    HEAD = '''    private static void head(StringBuilder sb, String title) {
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
    anchor = '    private static void home('
    i = src.index(anchor)
    src = src[:i] + HEAD + '\n' + src[i:]
    print('head restored')

io.open(P, 'w', encoding='utf-8', newline='').write(src)
print('done')
