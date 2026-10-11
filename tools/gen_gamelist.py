# -*- coding: utf-8 -*-
"""从 Registry.java 解析 100 款游戏定义，生成 GAMELIST.md（保证代码与文档一致）。"""
import os
import re

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
REG = os.path.join(BASE, 'android', 'src', 'com', 'baiyou', 'pocket', 'data', 'Registry.java')
OUT = os.path.join(BASE, 'GAMELIST.md')

LINE = re.compile(
    r'g\("(\w+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]+)",\s*"([^"]*)",\s*(null|\w+Game)(?:\.class)?\)')


def main():
    src = open(REG, encoding='utf-8').read()
    games = []
    for m in LINE.finditer(src):
        gid, name, cat, brief, tut, impl = m.groups()
        games.append((gid, name, cat, brief, impl != 'null'))

    assert len(games) == 100, 'Registry 应有 100 款，实际 %d' % len(games)
    cats = []
    for g in games:
        if g[2] not in cats:
            cats.append(g[2])
    playable = sum(1 for g in games if g[4])

    lines = [
        '# 百游口袋 · 游戏清单（100 款）',
        '',
        '> 本文件由 `tools/gen_gamelist.py` 从 `Registry.java` 自动生成，勿手改；',
        '> 新增游戏请先改 Registry，再运行 `python tools/gen_gamelist.py` 重新生成。',
        '',
        '- 共 **%d 款**（10 分类 × 10 款），v1.0 全部开放' % (len(games),),
        '- 状态说明：✅ 已开放可玩 · ⬜ 规划中（首页置灰"敬请期待"）',
        '',
    ]
    for cat in cats:
        lines.append('## %s' % cat)
        lines.append('')
        lines.append('| ID | 游戏名 | 玩法 | 状态 |')
        lines.append('| --- | --- | --- | --- |')
        for gid, name, c, brief, ok in games:
            if c != cat:
                continue
            lines.append('| %s | %s | %s | %s |' % (gid, name, brief, '✅' if ok else '⬜'))
        lines.append('')

    open(OUT, 'w', encoding='utf-8').write('\n'.join(lines))
    print('已生成 %s（%d 款，%d 分类，已开放 %d）' % (OUT, len(games), len(cats), playable))


if __name__ == '__main__':
    main()
