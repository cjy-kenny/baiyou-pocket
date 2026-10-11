# -*- coding: utf-8 -*-
"""urllib 连通性小诊断。"""
import time
import urllib.request

t = time.time()
try:
    r = urllib.request.urlopen('http://127.0.0.1:8082/meta?game=g001', timeout=3)
    print('meta', r.status, round(time.time() - t, 2), r.read(80))
except Exception as e:
    print('meta ERR', round(time.time() - t, 2), type(e).__name__, e)

t = time.time()
try:
    r = urllib.request.urlopen('http://127.0.0.1:8082/stream?game=g002', timeout=2)
    d = r.read(100)
    print('stream read100 OK', round(time.time() - t, 2), d[:24])
except Exception as e:
    print('stream ERR', round(time.time() - t, 2), type(e).__name__, str(e)[:100])

t = time.time()
try:
    r = urllib.request.urlopen('http://127.0.0.1:8082/input?game=g002&action=0&x=100&y=200', timeout=3)
    print('input', r.status, round(time.time() - t, 2))
except Exception as e:
    print('input ERR', round(time.time() - t, 2), type(e).__name__, str(e)[:100])
