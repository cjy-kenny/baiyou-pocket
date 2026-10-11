# -*- coding: utf-8 -*-
"""检查 IconDrawable 是否有 colorOf，并给 WebServer 的 home 统计修 coins 引用。"""
import io

icon_p = r'C:\Users\Administrator\.zcode\workspace\default\baiyou-pocket\android\src\com\baiyou\pocket\ui\IconDrawable.java'
t = io.open(icon_p, encoding='utf-8').read()
if 'colorOf' not in t:
    anchor = '    @Override\n    public void draw(Canvas canvas) {'
    add = '''    /** 该游戏的图标底色（网页首页卡片描边用）。 */
    public static int colorOf(GameMeta m) {
        int i = Registry.indexOf(m.id);
        return BG[((i % BG.length) + BG.length) % BG.length];
    }

''' + anchor
    t = t.replace(anchor, add, 1)
    io.open(icon_p, 'w', encoding='utf-8', newline='').write(t)
    print('IconDrawable: colorOf added')
else:
    print('IconDrawable: colorOf exists')

# WebServer home 里的 coins(store) 改成局部变量
ws_p = r'C:\Users\Administrator\.zcode\workspace\default\baiyou-pocket\tools\home_ui_v2.py'
s = io.open(ws_p, encoding='utf-8').read()
s = s.replace('.append(coins(store)).append("</b><span>金币</span>")',
              '.append(coins).append("</b><span>金币</span>")')
io.open(ws_p, 'w', encoding='utf-8', newline='').write(s)
print('home_ui_v2 coins ref fixed:', 'coins(store)' not in s)
