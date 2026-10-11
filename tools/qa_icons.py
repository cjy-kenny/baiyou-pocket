# -*- coding: utf-8 -*-
"""下载全部 100 款图标到 desktop/qa/ 并拼审查大图。"""
import os
import time
import urllib.request

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
QA = os.path.join(BASE, 'desktop', 'qa')

for n in range(1, 101):
    gid = 'g%03d' % n
    for attempt in (1, 2, 3):
        try:
            r = urllib.request.urlopen('http://127.0.0.1:8082/icon?game=' + gid, timeout=5)
            data = r.read()
            open(os.path.join(QA, 'icon_' + gid + '.png'), 'wb').write(data)
            break
        except Exception as e:
            if attempt == 3:
                print(gid, 'FAIL', e)
            time.sleep(0.4)
print('downloaded')

# 拼大图
from PIL import Image, ImageDraw
COLS = 8
CELL = 104
ids = ['g%03d' % n for n in range(1, 101)]
rows = (len(ids) + COLS - 1) // COLS
sheet = Image.new('RGB', (COLS * CELL + 16, rows * (CELL + 18) + 8), (24, 24, 30))
d = ImageDraw.Draw(sheet)
for k, gid in enumerate(ids):
    cx = 8 + (k % COLS) * CELL
    cy = 8 + (k // COLS) * (CELL + 18)
    p = os.path.join(QA, 'icon_' + gid + '.png')
    if os.path.exists(p):
        im = Image.open(p).convert('RGB').resize((88, 88))
        sheet.paste(im, (cx + 8, cy + 4))
    d.text((cx + 30, cy + 94), gid, fill=(220, 220, 230))
sheet.save(os.path.join(QA, 'icons_all.png'))
print('sheet saved')
