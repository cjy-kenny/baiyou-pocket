# -*- coding: utf-8 -*-
"""从构建日志提取 javac 错误行（文件:行号 + 首行错误描述）。"""
import io
import os
import re

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LOG = os.path.join(BASE, 'android', 'build', 'last_build.log')

raw = open(LOG, 'rb').read()
for enc in ('utf-8', 'gbk'):
    try:
        text = raw.decode(enc)
        break
    except UnicodeDecodeError:
        continue
else:
    text = raw.decode('utf-8', 'replace')

lines = text.splitlines()
pat = re.compile(r'\.java:\d+')
shown = set()
for i, l in enumerate(lines):
    if pat.search(l):
        key = l[:120]
        if key in shown:
            continue
        shown.add(key)
        print(l.strip())
        # 后续 4 行通常是符号/位置详情
        for j in range(i + 1, min(len(lines), i + 5)):
            if pat.search(lines[j]):
                break
            if lines[j].strip():
                print('   ', lines[j].strip()[:150])
print('--- 共', len(shown), '处错误位置 ---')
