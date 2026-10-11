# -*- coding: utf-8 -*-
"""修复构造函数碰 host 的 NPE：init 里删 host.setScore(0)，restartGame 里补回。"""
import os

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
G = os.path.join(BASE, 'android', 'src', 'com', 'baiyou', 'pocket', 'games')
FILES = ['RingDashGame.java', 'JellyJumpGame.java', 'KlondikeGame.java', 'FreeCellGame.java',
         'NumericChainGame.java', 'ThreePileGame.java', 'RankChainGame.java',
         'SuitSortGame.java', 'PyramidGame.java', 'PenaltyKickGame.java']

for f in FILES:
    p = os.path.join(G, f)
    src = open(p, encoding='utf-8').read()
    assert src.count('host.setScore(0);') == 1, f + ' 出现次数异常'
    src = src.replace('        host.setScore(0);\n', '', 1)
    key = 'public void restartGame() {\n'
    i = src.index(key)
    j = src.index('\n', i + len(key))
    # restartGame 第一行是 init 调用，在其后补 host.setScore(0);
    src = src[:j] + '\n        host.setScore(0);' + src[j:]
    open(p, 'w', encoding='utf-8', newline='').write(src)
    print('fixed', f)
print('done')
