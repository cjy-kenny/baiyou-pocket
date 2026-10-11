# -*- coding: utf-8 -*-
"""把最终成品打成发布压缩包 dist/百游口袋-v1.0.zip"""
import os, zipfile, shutil

ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DIST = os.path.join(ROOT, 'dist')
STAGE = os.path.join(DIST, '_pkg')
TOP = '百游口袋-v1.0'
ZIP = os.path.join(DIST, TOP + '.zip')

FILES = [
    ('dist/baiyou-pocket-v1.0.apk',            'baiyou-pocket-v1.0.apk'),
    ('dist/baiyou-pocket-desktop-v1.0.jar',    'dist/baiyou-pocket-desktop-v1.0.jar'),
    ('百游口袋-PC.bat',                        '百游口袋-PC.bat'),
    ('百游口袋-网页.bat',                       '百游口袋-网页.bat'),
    ('README.md',                              'README.md'),
    ('GAMELIST.md',                            'GAMELIST.md'),
    ('EXTENSION.md',                           'EXTENSION.md'),
]

GUIDE = '''# 百游口袋 v1.0 · 使用说明

一个口袋装下 100 款小游戏。全离线 · 零权限 · 所有数据只存本机。

## 一、手机安装（Android 7.0 及以上）
1. 把 `baiyou-pocket-v1.0.apk` 传到手机（数据线 / 微信文件传输助手 / 网盘均可）；
2. 在手机上点开安装，系统提示"未知来源应用"时选择允许本次安装；
3. 装好打开即玩：首页按分类或搜索找游戏 → 点开先看玩法介绍 → 点「开始」。
- 应用不申请任何权限、不联网；最高分 / 金币 / 成就 / 收藏全部只保存在手机本地。

## 二、电脑上游玩（需 Java 11 及以上）
- 双击「百游口袋-PC.bat」，弹出游戏窗口直接玩；
- 也可以命令行运行：`javaw -jar dist\\baiyou-pocket-desktop-v1.0.jar`。

## 三、手机浏览器游玩（与电脑同一 WiFi）
- 电脑双击「百游口袋-网页.bat」，电脑浏览器自动打开 http://127.0.0.1:8080 ；
- 手机浏览器访问 `http://<电脑IP>:8080`，即可实时同步画面游玩。

## 四、包内清单
| 文件 | 说明 |
| --- | --- |
| baiyou-pocket-v1.0.apk | 安卓安装包（v1.0 / versionCode 10） |
| dist/baiyou-pocket-desktop-v1.0.jar | PC 与网页版共用运行包（同码三端） |
| 百游口袋-PC.bat / 百游口袋-网页.bat | 一键启动脚本 |
| GAMELIST.md | 100 款游戏清单（分类 + 玩法一句话） |
| README.md / EXTENSION.md | 项目说明与扩展规格 |

## 五、数据与隐私
- 不联网、不要权限；金币 / 成就 / 最高分 / 设置仅存本机文件；
- 想清档：网页版「设置 → 清空存档」，或直接卸载重装。

## 六、常见问题
- **双击 bat 一闪而过**：电脑没装 Java 11+，装好后重试；
- **手机安装被拦截**：属系统对未知来源的正常提示，允许本次安装即可；
- **手机打不开网页版**：确认与电脑同一 WiFi、电脑防火墙放行 8080 端口；
- **某款游戏点开黑屏/异常**：游戏在独立进程运行，出问题只影响该局，返回首页重进即可。
'''

# 1) 准备暂存目录
if os.path.exists(STAGE):
    shutil.rmtree(STAGE)
base = os.path.join(STAGE, TOP)
os.makedirs(base)

# 使用说明
with open(os.path.join(base, '使用说明.md'), 'w', encoding='utf-8') as f:
    f.write(GUIDE)

# 2) 拷贝成品
for src_rel, dst_rel in FILES:
    src = os.path.join(ROOT, src_rel.replace('/', os.sep))
    dst = os.path.join(base, dst_rel.replace('/', os.sep))
    os.makedirs(os.path.dirname(dst), exist_ok=True)
    shutil.copy2(src, dst)
    print('copy', src_rel, '->', TOP + '/' + dst_rel)

# 3) 压缩
if os.path.exists(ZIP):
    os.remove(ZIP)
with zipfile.ZipFile(ZIP, 'w', zipfile.ZIP_DEFLATED, compresslevel=9) as z:
    for dirpath, _, names in os.walk(base):
        for n in sorted(names):
            p = os.path.join(dirpath, n)
            arc = os.path.relpath(p, STAGE)
            z.write(p, arc)

shutil.rmtree(STAGE)

print()
print('=== ZIP:', ZIP)
print('size: %.1f MB' % (os.path.getsize(ZIP) / 1048576))
with zipfile.ZipFile(ZIP) as z:
    for i in z.infolist():
        print('  %8.1f KB  %s' % (i.file_size / 1024, i.filename))
