# -*- coding: utf-8 -*-
"""在源码里找含指定中文的行。用法: python tools/find_cn.py 目录 关键词 [关键词...]"""
import os
import sys

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
root = os.path.join(BASE, sys.argv[1]) if len(sys.argv) > 1 else BASE
keys = sys.argv[2:] if len(sys.argv) > 2 else ['已开放']

for dirpath, _, files in os.walk(root):
    for f in files:
        if not f.endswith(('.java', '.xml', '.py')):
            continue
        p = os.path.join(dirpath, f)
        try:
            lines = open(p, encoding='utf-8').read().splitlines()
        except Exception:
            continue
        for i, l in enumerate(lines):
            if any(k in l for k in keys):
                print('%s:%d: %s' % (os.path.relpath(p, BASE), i + 1, l.strip()[:130]))
