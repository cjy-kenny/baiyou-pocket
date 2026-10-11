# -*- coding: utf-8 -*-
"""百游口袋 · 网页壳全量自测：对 100 款游戏逐个"开局 → 注入随机操作 → 查状态"，
   用崩溃日志（desktop-data-demo/logs/）揪出运行时 bug。
   用法: python tools/selftest_web.py [起始序号]  （服务需已在 8082 以 --demo --web 启动）"""
import json
import os
import time
import urllib.request
import urllib.error

BASE = 'http://127.0.0.1:8082'
LOGS = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                    'desktop-data-demo', 'logs')
START = int(os.environ.get('START', '1')) if len(os.sys.argv) < 2 else int(os.sys.argv[1])


def get(path, timeout=6, read_bytes=0):
    """read_bytes=0 读整个响应体；>0 读固定字节（MJPEG 流必须限制，否则永久阻塞）。"""
    try:
        with urllib.request.urlopen(BASE + path, timeout=timeout) as r:
            body = r.read(read_bytes) if read_bytes else r.read()
            return r.status, body.decode('utf-8', 'replace')
    except Exception as e:
        return 0, str(e)


def poke(gid, action, x, y):
    get('/input?game=%s&action=%d&x=%d&y=%d' % (gid, action, x, y), timeout=3)


def logs_set():
    if not os.path.isdir(LOGS):
        return set()
    return set(os.listdir(LOGS))


before = logs_set()
results = {}
ids = ['g%03d' % n for n in range(START, 101)]

for gid in ids:
    # 开局：stream 连接触发 switchTo（读一小块即断开）
    get('/stream?game=%s' % gid, timeout=2.2, read_bytes=600)
    # 解除玩法介绍挂起，否则输入会被丢弃
    get('/ctl?game=%s&op=begin' % gid, timeout=3)
    time.sleep(0.25)
    # 通用探测输入：点按 + 滑动 + 多点触探
    for act, x, y in [(0, 180, 300), (1, 180, 300),
                      (0, 180, 430), (2, 180, 260), (1, 180, 260),
                      (0, 90, 200), (1, 90, 200),
                      (0, 270, 200), (1, 270, 200),
                      (0, 120, 460), (2, 260, 460), (1, 260, 460)]:
        poke(gid, act, x, y)
        time.sleep(0.08)
    time.sleep(0.5)
    st, body = get('/meta?game=%s' % gid, timeout=3)
    over = None
    try:
        over = json.loads(body).get('over')
    except Exception:
        pass
    results[gid] = over
    print(gid, 'over=', over, flush=True)

time.sleep(1.0)
after = logs_set()
crashes = sorted(after - before)
print('==== 崩溃日志 (%d) ====' % len(crashes))
for c in crashes:
    print(c)
print('==== over=true（需人工甄别是否合理结束）====')
for gid, over in results.items():
    if over:
        print(gid)
print('==== 自测完成 ====')
