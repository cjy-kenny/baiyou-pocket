# -*- coding: utf-8 -*-
"""生成启动图标（纯标准库，零依赖）：深蓝底 + 2x2 彩色圆角块，代表「游戏合集」。
   4x 超采样抗锯齿，输出五档 mipmap PNG。"""
import os
import struct
import zlib

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
RES = os.path.join(BASE, 'android', 'res')

BG_TOP = (30, 34, 53)      # #1E2235
BG_BOT = (58, 47, 99)      # #3A2F63
TILES = [(255, 107, 107), (255, 194, 75), (78, 205, 196), (91, 143, 239)]
SS = 4  # 超采样倍数


def lerp(a, b, t):
    return tuple(int(a[i] + (b[i] - a[i]) * t) for i in range(3))


def rounded_mask(x, y, w, h, r):
    """点 (x,y) 是否在圆角矩形内（局部坐标，左上角原点）。"""
    if not (0 <= x <= w and 0 <= y <= h):
        return False
    cx = min(max(x, r), w - r)
    cy = min(max(y, r), h - r)
    return (x - cx) ** 2 + (y - cy) ** 2 <= r ** 2


def render(size):
    n = size * SS
    px = bytearray(n * n * 4)
    m = n * 0.22          # 外框圆角
    gap = n * 0.075       # 块间距
    pad = n * 0.19        # 内边距
    cell = (n - 2 * pad - gap) / 2
    tr = cell * 0.30      # 彩块圆角
    for y in range(n):
        for x in range(n):
            inside = rounded_mask(x + .5, y + .5, n, n, m)
            if not inside:
                continue
            c = lerp(BG_TOP, BG_BOT, y / n)
            # 2x2 彩块
            gx, gy = x - pad, y - pad
            if 0 <= gx <= n - 2 * pad and 0 <= gy <= n - 2 * pad:
                ix = int(gx // (cell + gap))
                iy = int(gy // (cell + gap))
                lx = gx - ix * (cell + gap)
                ly = gy - iy * (cell + gap)
                if 0 <= ix < 2 and 0 <= iy < 2 and lx <= cell and ly <= cell:
                    if rounded_mask(lx, ly, cell, cell, tr):
                        # 彩块带轻微高光（顶部亮 8%）
                        hl = 1.08 if ly < cell * 0.35 else 1.0
                        t = TILES[iy * 2 + ix]
                        c = tuple(min(255, int(v * hl)) for v in t)
            i = (y * n + x) * 4
            px[i] = c[0]; px[i + 1] = c[1]; px[i + 2] = c[2]; px[i + 3] = 255
    # 降采样
    out = bytearray(size * size * 4)
    for y in range(size):
        for x in range(size):
            r = g = b = a = 0
            for dy in range(SS):
                for dx in range(SS):
                    j = ((y * SS + dy) * n + (x * SS + dx)) * 4
                    r += px[j]; g += px[j + 1]; b += px[j + 2]; a += px[j + 3]
            k = (y * size + x) * 4
            out[k] = r // (SS * SS); out[k + 1] = g // (SS * SS)
            out[k + 2] = b // (SS * SS); out[k + 3] = a // (SS * SS)
    return bytes(out)


def write_png(path, size, rgba):
    def chunk(tag, data):
        c = tag + data
        return struct.pack('>I', len(data)) + c + struct.pack('>I', zlib.crc32(c))
    raw = b''.join(b'\x00' + rgba[y * size * 4:(y + 1) * size * 4] for y in range(size))
    png = (b'\x89PNG\r\n\x1a\n'
           + chunk(b'IHDR', struct.pack('>IIBBBBB', size, size, 8, 6, 0, 0, 0))
           + chunk(b'IDAT', zlib.compress(raw, 9))
           + chunk(b'IEND', b''))
    with open(path, 'wb') as f:
        f.write(png)


DENS = {'mdpi': 48, 'hdpi': 72, 'xhdpi': 96, 'xxhdpi': 144, 'xxxhdpi': 192}
for d, s in DENS.items():
    dst = os.path.join(RES, 'mipmap-' + d)
    os.makedirs(dst, exist_ok=True)
    write_png(os.path.join(dst, 'ic_launcher.png'), s, render(s))
    print('ic_launcher.png', s, 'x', s, '->', d)
print('done')
