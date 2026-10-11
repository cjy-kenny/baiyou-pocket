# -*- coding: utf-8 -*-
"""修复 home() 里被真实换行打断的 JS 字符串：把“字符串内换行”去掉（拼接为单行）。"""
import io
import re

P = r'C:\Users\Administrator\.zcode\workspace\default\baiyou-pocket\desktop\src\com\baiyou\pocket\desktop\WebServer.java'
lines = io.open(P, encoding='utf-8').read().splitlines(True)

# 找出形如 .append("...（行尾未闭合引号） 的断行字符串，与下一行 ") 拼接
out = []
i = 0
fixed = 0
while i < len(lines):
    l = lines[i]
    m = re.search(r'\.append\("(.*)$', l)
    if m and not l.rstrip().endswith('");') and not l.rstrip().endswith('")') and i + 1 < len(lines):
        nxt = lines[i + 1]
        if nxt.strip() == '")':
            # 断行字符串：把下一行的 ") 并进来
            out.append(l.rstrip('\n').rstrip() + nxt.strip())
            i += 2
            fixed += 1
            continue
    out.append(l)
    i += 1

io.open(P, 'w', encoding='utf-8', newline='').write(''.join(out))
print('fixed broken strings:', fixed)
