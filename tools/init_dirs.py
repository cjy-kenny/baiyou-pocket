# -*- coding: utf-8 -*-
"""初始化 baiyou-pocket 项目目录树（可重复运行）。"""
import os
import shutil

BASE = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))  # baiyou-pocket/
DIRS = [
    'android/res/values', 'android/res/values-night',
    'android/res/mipmap-mdpi', 'android/res/mipmap-hdpi', 'android/res/mipmap-xhdpi',
    'android/res/mipmap-xxhdpi', 'android/res/mipmap-xxxhdpi',
    'android/src/com/baiyou/pocket/data', 'android/src/com/baiyou/pocket/game',
    'android/src/com/baiyou/pocket/games', 'android/src/com/baiyou/pocket/ui',
    'android/build', 'dist', 'tools',
]


def main():
    for d in DIRS:
        os.makedirs(os.path.join(BASE, d), exist_ok=True)
    # 之前的 cd 链残留（android/cd、android/dist、android/src 空壳）
    for junk in ['android/cd', 'android/dist']:
        p = os.path.join(BASE, junk)
        if os.path.isdir(p):
            shutil.rmtree(p)
    for root, dnames, fnames in os.walk(BASE):
        print(root.replace(BASE, '.'))


if __name__ == '__main__':
    main()
