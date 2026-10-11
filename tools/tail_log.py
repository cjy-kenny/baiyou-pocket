# -*- coding: utf-8 -*-
"""打印构建日志尾部（自动识别 GBK/UTF-8）。用法: python tools/tail_log.py [行数=60]"""
import io
import os
import sys

LOG = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))),
                   'android', 'build', 'last_build.log')
N = int(sys.argv[1]) if len(sys.argv) > 1 else 60

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
err = [l for l in lines if ('错误' in l or 'error' in l or 'ERROR' in l or '失败' in l)]
print('--- 命中错误/关键行 (%d) ---' % len(err))
for l in err[-30:]:
    print(l)
print('--- 尾部 %d 行 ---' % min(N, len(lines)))
for l in lines[-N:]:
    print(l)
