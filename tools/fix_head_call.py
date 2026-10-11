# -*- coding: utf-8 -*-
"""home() 开头补 head() 调用。"""
import io

P = r'C:\Users\Administrator\.zcode\workspace\default\baiyou-pocket\desktop\src\com\baiyou\pocket\desktop\WebServer.java'
src = io.open(P, encoding='utf-8').read()

anchor = 'StringBuilder sb = new StringBuilder();'
i = src.find(anchor)
# home() 里的第一个 StringBuilder 后插入 head 调用（跳过已存在的 head 调用行）
j = src.find('\n', i)
seg = src[i:j]
if 'head(sb' in seg:
    print('already calls head')
else:
    src = src[:j] + '\n        head(sb, "百游口袋");' + src[j:]
    io.open(P, 'w', encoding='utf-8', newline='').write(src)
    print('head call inserted at line', src[:j].count('\n') + 2)
