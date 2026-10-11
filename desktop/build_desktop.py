# -*- coding: utf-8 -*-
"""百游口袋 PC 端构建（无需 Gradle）：
   共享核心（android/src 中的 data/game/games/ui + CrashGuard）
   + Android API 垫片（desktop/shim-src）
   + PC 端 Swing 壳（desktop/src）
   → javac → 与 org.json 合并成 fat jar（dist/baiyou-pocket-desktop-vX.Y.jar）。"""
import glob
import os
import shutil
import subprocess
import sys
import zipfile

HERE = os.path.dirname(os.path.abspath(__file__))            # baiyou-pocket/desktop
ROOT = os.path.dirname(HERE)                                 # baiyou-pocket
ANDROID_SRC = os.path.join(ROOT, 'android', 'src')
SHIM = os.path.join(HERE, 'shim-src')
DESK = os.path.join(HERE, 'src')
JSON_JAR = os.path.join(HERE, 'libs', 'json-20240303.jar')
OUT = os.path.join(HERE, 'build')
DIST = os.path.join(ROOT, 'dist')

VERSION = '1.0'   # 与 Android 端版本保持一致
JAR = os.path.join(DIST, 'baiyou-pocket-desktop-v%s.jar' % VERSION)

# 共享核心：仅这些目录/文件（Android 壳 App/MainActivity/GameHostActivity 不参与桌面编译）
SHARED = [
    os.path.join(ANDROID_SRC, 'com', 'baiyou', 'pocket', 'CrashGuard.java'),
    os.path.join(ANDROID_SRC, 'com', 'baiyou', 'pocket', 'data'),
    os.path.join(ANDROID_SRC, 'com', 'baiyou', 'pocket', 'game'),
    os.path.join(ANDROID_SRC, 'com', 'baiyou', 'pocket', 'games'),
    os.path.join(ANDROID_SRC, 'com', 'baiyou', 'pocket', 'ui'),
]


def run(cmd):
    print('>>', ' '.join(cmd))
    r = subprocess.run(cmd)
    if r.returncode != 0:
        sys.exit('命令失败（退出码 %d）: %s' % (r.returncode, ' '.join(cmd)))


def collect_src():
    srcs = []
    for p in SHARED:
        if os.path.isfile(p):
            srcs.append(p)
        else:
            srcs += glob.glob(os.path.join(p, '**', '*.java'), recursive=True)
    srcs += glob.glob(os.path.join(SHIM, '**', '*.java'), recursive=True)
    srcs += glob.glob(os.path.join(DESK, '**', '*.java'), recursive=True)
    return srcs


def main():
    if not os.path.exists(JSON_JAR):
        sys.exit('缺少 %s（先执行 tools 下载或手动放置 org.json jar）' % JSON_JAR)
    cls = os.path.join(OUT, 'classes')
    shutil.rmtree(OUT, ignore_errors=True)
    os.makedirs(cls)

    srcs = collect_src()
    run(['javac', '--release', '11', '-encoding', 'UTF-8',
         '-cp', JSON_JAR, '-d', cls] + srcs)
    print('[1/2] 编译完成（%d 个源文件，含垫片与共享核心）' % len(srcs))

    os.makedirs(DIST, exist_ok=True)
    with zipfile.ZipFile(JAR, 'w', zipfile.ZIP_DEFLATED) as zout:
        # fat jar：把 org.json 一并打入
        with zipfile.ZipFile(JSON_JAR) as zj:
            for info in zj.infolist():
                if info.filename.endswith(('.class',)) or info.filename.startswith('META-INF/LICENSE'):
                    zout.writestr(info.filename, zj.read(info.filename))
        manifest = 'Manifest-Version: 1.0\r\nMain-Class: com.baiyou.pocket.desktop.DesktopMain\r\n'
        zout.writestr('META-INF/MANIFEST.MF', manifest)
        for path in glob.glob(os.path.join(cls, '**', '*.class'), recursive=True):
            rel = os.path.relpath(path, cls).replace('\\', '/')
            zout.write(path, rel)
    print('[2/2] fat jar 完成')
    print('=' * 46)
    print('PC 端产物: %s (%.1f KB)' % (JAR, os.path.getsize(JAR) / 1024))
    print('运行: java -jar %s        （或双击 百游口袋-PC.bat）' % JAR)
    print('演示: java -jar %s --demo  （自动游玩并截图）' % JAR)
    print('=' * 46)


if __name__ == '__main__':
    main()
