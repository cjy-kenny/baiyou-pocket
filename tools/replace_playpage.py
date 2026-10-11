# -*- coding: utf-8 -*-
"""替换 WebServer 的 playPage 方法（DOM 版弹层 + 玩法介绍 + 玩法按钮），
   并在 switchTask 里加 1.5x 超采样、stream 里提升 JPEG 质量。"""
import io
import re

P = r'C:\Users\Administrator\.zcode\workspace\default\baiyou-pocket\desktop\src\com\baiyou\pocket\desktop\WebServer.java'

NEW_PLAYPAGE = '''    private static void playPage(HttpExchange ex, String gid) throws IOException {
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
                .append("<div><button class='btn' id='bPause' onclick='ctl(\\"pause\\")'>暂停</button> ")
                .append("<button class='btn' onclick='ctl(\\"restart\\")'>重开</button> ")
                .append("<button class='btn' onclick='howto()'>玩法</button> ")
                .append("<button class='btn gray' onclick='ctl(\\"exit\\")'>退出</button></div></div>");
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
                .append("function addBtn(label,js){var b=document.createElement('button');b.className='btn';b.textContent=label;b.addEventListener('click',function(){eval(js);});ov.appendChild(b);}")
                .append("function ovTitle(t){var h=document.createElement('div');h.className='big';h.textContent=t;ov.appendChild(h);}")
                .append("function ovText(t){var d=document.createElement('div');d.className='tut';d.textContent=t;ov.appendChild(d);}")
                .append("function showIntro(text,first,infoOnly){ov.className='show';ov.innerHTML='';")
                .append("  ovTitle('玩法 · '+gname);ovText(text);")
                .append("  if(infoOnly){addBtn('知道了',\\"manualInfo=false;hideOv();\\");}")
                .append("  else{addBtn(first?'开始':'继续',\\"manualInfo=false;fetch('/ctl?game='+gid+'&op='+(first?'begin':'resume')).then(function(){hideOv();refresh();});\\");}")
                .append("}")
                .append("function showPause(){ov.className='show';ov.innerHTML='';ovTitle('已暂停');")
                .append("  addBtn('继续游戏',\\"ctl('resume')\\");addBtn('重新开始',\\"ctl('restart')\\");addBtn('退出游戏',\\"ctl('exit')\\");}")
                .append("function showSettle(s){ov.className='show';ov.innerHTML='';ovTitle('本局结束');")
                .append("  var d=document.createElement('div');d.innerHTML='得分 <b>'+s.finalScore+'</b> · 最高 <b>'+s.best+'</b>'+(s.newBest?'<br>★ 新纪录！':'')+(s.coins?'<br>金币 +'+s.coins:'');ov.appendChild(d);")
                .append("  if(s.ach){var a=document.createElement('div');a.className='ach';a.textContent='达成成就：'+s.ach;ov.appendChild(a);}")
                .append("  addBtn('再来一局',\\"ctl('restart')\\");addBtn('退出',\\"ctl('exit')\\");}")
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
'''

src = io.open(P, encoding='utf-8').read()

start = src.index('    private static void playPage')
end = src.index('    // ---------------- MJPEG 实时流 ----------------')
src = src[:start] + NEW_PLAYPAGE + '\n' + src[end:]

# switchTask：1.5x 超采样（宽不超过 640）
old_fit = '''        if (g instanceof com.baiyou.pocket.game.CanvasGame) {
            com.baiyou.pocket.game.CanvasGame cg = (com.baiyou.pocket.game.CanvasGame) g;
            session.fw = Math.round(cg.viewWidth());
            session.fh = Math.round(cg.viewHeight());
        } else {
            session.fw = 360;
            session.fh = 480;
        }'''
new_fit = '''        if (g instanceof com.baiyou.pocket.game.CanvasGame) {
            com.baiyou.pocket.game.CanvasGame cg = (com.baiyou.pocket.game.CanvasGame) g;
            // 1.5x 超采样：MJPEG 帧更清晰，触摸仍按渲染像素换算回逻辑坐标
            float q = Math.min(1.5f, 640f / cg.viewWidth());
            session.fw = Math.round(cg.viewWidth() * q);
            session.fh = Math.round(cg.viewHeight() * q);
        } else {
            session.fw = 540;
            session.fh = 720;
        }'''
assert old_fit in src, 'switchTask fit 块未找到'
src = src.replace(old_fit, new_fit)

# stream：JPEG 质量 0.9
old_jpg = '''                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                javax.imageio.ImageIO.write(f, "jpg", bo);'''
new_jpg = '''                ByteArrayOutputStream bo = new ByteArrayOutputStream();
                javax.imageio.ImageWriter iw = javax.imageio.ImageIO.getImageWritersByFormatName("jpg").next();
                javax.imageio.ImageWriteParam wp = iw.getDefaultWriteParam();
                wp.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
                wp.setCompressionQuality(0.9f);
                javax.imageio.stream.ImageOutputStream ios = javax.imageio.ImageIO.createImageOutputStream(bo);
                iw.setOutput(ios);
                iw.write(null, new javax.imageio.IIOImage(f, null, null), wp);
                ios.flush();
                ios.close();
                iw.dispose();'''
assert old_jpg in src, 'stream JPEG 块未找到'
src = src.replace(old_jpg, new_jpg)

io.open(P, 'w', encoding='utf-8', newline='').write(src)
print('playPage/switchTask/stream 已更新')
