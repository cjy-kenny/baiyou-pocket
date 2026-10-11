# -*- coding: utf-8 -*-
"""针对性重扫：只测指定游戏，验证 sweep 标记是否可复现。"""
import json
import os
import time
import urllib.request

BASE = 'http://127.0.0.1:8082'
LOGS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'desktop-data-demo', 'logs')
IDS = ['g066', 'g079', 'g027', 'g031', 'g033', 'g038', 'g042', 'g045', 'g057']


def get(path, timeout=6, read_bytes=0):
    try:
        with urllib.request.urlopen(BASE + path, timeout=timeout) as r:
            body = r.read(read_bytes) if read_bytes else r.read()
            return r.status, body.decode('utf-8', 'replace')
    except Exception as e:
        return 0, str(e)


before = set(os.listdir(LOGS)) if os.path.isdir(LOGS) else set()
for gid in IDS:
    get('/stream?game=%s' % gid, timeout=2.2, read_bytes=600)
    time.sleep(0.25)
    for act, x, y in [(0, 180, 300), (1, 180, 300),
                      (0, 180, 430), (2, 180, 260), (1, 180, 260),
                      (0, 90, 200), (1, 90, 200),
                      (0, 270, 200), (1, 270, 200),
                      (0, 120, 460), (2, 260, 460), (1, 260, 460)]:
        get('/input?game=%s&action=%d&x=%d&y=%d' % (gid, act, x, y), timeout=3)
        time.sleep(0.08)
    time.sleep(0.5)
    st, body = get('/meta?game=%s' % gid, timeout=3)
    try:
        over = json.loads(body).get('over')
    except Exception:
        over = '?'
    print(gid, 'over=', over, flush=True)

time.sleep(1.0)
after = set(os.listdir(LOGS)) if os.path.isdir(LOGS) else set()
crashes = sorted(after - before)
print('==== 新增崩溃日志 (%d) ====' % len(crashes))
for c in crashes:
    print(c)
