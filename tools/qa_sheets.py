# -*- coding: utf-8 -*-
"""把 desktop/qa/ 的 100 张截图拼成审查用大图（每张 20 格，带 id 标签）。"""
import os
from PIL import Image, ImageDraw

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
QA = os.path.join(BASE, 'desktop', 'qa')
OUT = os.path.join(BASE, 'desktop', 'qa')

COLS, ROWS = 4, 5
TW, TH = 180, 280
PER = COLS * ROWS


def sheet(names, idx):
    W = COLS * (TW + 8) + 8
    H = ROWS * (TH + 26) + 8
    img = Image.new('RGB', (W, H), (18, 18, 24))
    d = ImageDraw.Draw(img)
    for k, gid in enumerate(names):
        cx = 8 + (k % COLS) * (TW + 8)
        cy = 8 + (k // COLS) * (TH + 26)
        p = os.path.join(QA, gid + '.jpg')
        if os.path.exists(p):
            try:
                im = Image.open(p).convert('RGB').resize((TW, TH))
                img.paste(im, (cx, cy))
            except Exception:
                d.rectangle([cx, cy, cx + TW, cy + TH], fill=(60, 0, 0))
        else:
            d.rectangle([cx, cy, cx + TW, cy + TH], fill=(60, 0, 0))
        d.text((cx + 4, cy + TH + 4), gid, fill=(230, 230, 230))
    out = os.path.join(OUT, 'sheet%d.png' % idx)
    img.save(out)
    print(out)


def main():
    ids = ['g%03d' % n for n in range(1, 101)]
    for s in range(5):
        sheet(ids[s * PER:(s + 1) * PER], s)


if __name__ == '__main__':
    main()
