# 百游口袋 · 游戏扩展规范（从 5 款到 100 款）

本规范回答一个问题：**如何把第 6 款到第 100 款游戏安全地塞进这套架构**。
任何新游戏严格照此流程，不改框架代码。

## 一、扩展一个游戏的完整步骤（以 g006 滑块拼图为例）

### 第 1 步：写游戏类

新建 `android/src/com/baiyou/pocket/games/SlidePuzzleGame.java`：

```java
package com.baiyou.pocket.games;

import android.graphics.Canvas;
import com.baiyou.pocket.game.CanvasGame;

/** g006 滑块拼图：<一句话玩法>。计分规则：<公式>。 */
public class SlidePuzzleGame extends CanvasGame {

    public SlidePuzzleGame() {
        super(360, 560);   // 逻辑坐标系宽高（等比缩放适配一切屏幕，勿在渲染外读真实像素）
    }

    // ---------- 必须实现 ----------

    @Override
    protected void render(Canvas c) {
        // 背景：rr(c, 0, 0, lw, lh, 0, 0x??????) 自带配色，不随日夜主题
    }

    @Override
    protected void touch(float x, float y, int action) {
        // action：0=DOWN 1=UP 2=MOVE 3=CANCEL；坐标已换算为逻辑坐标
    }

    @Override
    public void restartGame() {
        // 重置全部状态 → host.setScore(0) → 需要逻辑帧则 startLoop(间隔ms)
    }

    // ---------- 按需实现 ----------

    @Override
    protected void onTick() {
        setNextTick(16);            // 默认 16ms，变速游戏在此改下一帧间隔
        // 逻辑…结束后 dirty() 触发重绘
    }

    @Override
    public void resumeGame() {
        if (!over) startLoop(/*与 restartGame 相同的间隔*/);   // 有循环的游戏必须覆写
    }

    @Override
    public void pauseGame() {
        super.pauseGame();          // 停逻辑帧（默认已实现）
        // 如有 Handler 自定义任务需一并暂停
    }
}
```

**硬性约定**（违反会破坏崩溃隔离或结算）：

1. 构造函数只做与 host 无关的初始化（host 在构造后才注入）；首局由宿主调
   `restartGame()` 启动；
2. 逻辑只允许跑在三类受保护入口：`touch()`（宿主 SafeFrame 兜底）、
   `onTick()`（循环兜底）、其余代码用 `runSafe(() -> …)` 包一层；
3. 分数变化必须经 `host.setScore(int)`（HUD 刷新 + 实时落盘），不要自己存档；
4. 结束一局必须调 `host.gameOver(finalScore)`，不许自己 finish Activity；
5. 游戏内不 new Thread、不碰网络/文件（除 `host.*` 提供的能力）、不申请权限；
6. 素材合规：图形程序化绘制（Canvas/Paint），音效走 `host.beep(0~3)`，
   名称原创、不用任何现有商业游戏的名称/美术/术语组合。

### 第 2 步：注册

`data/Registry.java` 找到对应分类区块，把占位行改成实现：

```java
g("g006", "滑块拼图", "益智", "移动滑块还原完整图片", SlidePuzzleGame.class),
```

只改最后一个参数（`null` → 类名），id/名称/分类/文案保持不变（名称与
GAMELIST.md 是同一来源）。

### 第 3 步：同步清单并构建

```bash
python tools/gen_gamelist.py     # GAMELIST.md 从 Registry 重新生成
python android/build_apk.py      # 编译、签名、出 dist/baiyou-pocket-vX.Y.apk
```

### 第 4 步：版本与提交检查单

- [ ] `javac` 零错误零新增警告；APK 体积增幅合理（每款游戏 ≈ 2~6 KB）
- [ ] 首页：新游戏不再置灰；图标/名称/一句话玩法正确；长按可收藏
- [ ] 一局完整流程：进入 → 计分 → 结算弹窗（分数/最高/金币）→ 再来一局 → 退出
- [ ] 暂停（顶栏按钮 + 切后台）→ 继续 → 重开；退出后首页"最近玩过/最高分"正确
- [ ] 深色模式切换后首页正常（游戏画面自成配色，不受影响）
- [ ] 平板宽度（≥600dp）下列数与画面居中正常
- [ ] 杀进程重进：最高分还在（scores 文件已实时落盘）
- [ ] 构造函数没碰 host；没有绕过 host 的存档/结算

## 二、批量节奏建议

- 每批开放 **5~10 款**，每批一个版本号（v0.2、v0.3…versionCode 同步 +1）；
- 批内优先做同分类（共用交互模式，写起来快）：推荐顺序
  反应类 → 数字类 → 记忆类 → 休闲类 → 益智类 → 牌类 → 体育/射击 → 文字类；
- 复杂度高的（数独、纸牌接龙、找茬）单独成批，预留双倍工时。

## 三、涉及框架改动时的注意事项（原则上禁止，确需时）

| 想做的事 | 影响面 | 正确姿势 |
| --- | --- | --- |
| 改结算/金币规则 | GameHostActivity（游戏进程） | 只改 `gameOver/exitToMain`；主进程合并逻辑不动 |
| 加全局设置项 | store.json schema | 在 `Store.loadStore` 加缺省兜底；只在主进程写 |
| 加新成就 | MainActivity.ACHS + evalAch | 只加 id/名称/描述/判定，勿改存储结构 |
| 改 Registry 行格式 | tools/gen_gamelist.py 正则 | 行格式是"代码↔文档"同步契约，改必须两边一起 |
| 加第三方库 | 免 Gradle 流水线 | 优先不加；确需纯 Java 库则 jar 放 `libs/` 并在脚本加 classpath |

## 四、数据与计分口径（保持全局一致）

- 最高分：游戏进程实时写 `scores/<id>.json`，主进程只读展示；
- 金币：`每局 = min(50, 该局得分 / 10)`，会话内多局累加，退出一次性回传；
- 局数：`host.gameOver()` 计 1 局；半局退出不计局数（金币按当前分结算）；
- 得分分数量级请设计在 **0 ~ 3000** 区间（成就阈值 500 分才有统一意义），
  计时惩罚类游戏务必设下限（参考记忆翻牌 `max(100, …)`）。

## 五、命名与合规红线

1. 游戏名不用任何现有商业游戏名称、别名、商标词（含"消消乐""大作战"等组合）；
   提交前在应用商店搜索一遍确认无同名；
2. 玩法可实现为"同类机制"但代码、数值、美术全部原创；
3. 不引入：网络、广告 SDK、统计 SDK、动态加载、外部存储 —— 保持零权限离线；
4. 文字类游戏题库（成语/诗词/谜语）使用公有领域内容，且注明来源分类。

## 六、双端平台规则（写 1 款 = 自动得 Android + PC 两端）

新游戏**天然同时跑在 APK 和 PC 桌面版**上——因为游戏代码只依赖
`desktop/shim-src/` 垫片覆盖的那一小块 Android API。务必遵守：

**游戏代码只许用这些 API（垫片白名单）：**

- `android.graphics`：Canvas（save/restore/translate/scale/drawRoundRect/
  drawCircle/drawPath/drawArc/drawText）、Paint（颜色/样式/字色/对齐/描边/
  仿粗/字距）、Path、RectF（新增垫片需同步改两端）；
- `android.os`：Handler.postDelayed / removeCallbacks（PC 端在 Swing EDT 执行，
  与 Android 主线程模型一致，游戏逻辑无需加锁）；
- `android.view`：View（匿名子类重写 onSizeChanged/onDraw）、
  MotionEvent（getActionMasked/getX/getY）；
- 其余一概不用：**禁止** import android.app.*、android.widget.*、
  android.content.Context（游戏不该碰存储）、Toast/Dialog（用 host.toast）。

**被禁止行为的双端后果速查：**

| 违规写法 | Android 端 | PC 端 |
| --- | --- | --- |
| import android.graphics.Color 等未垫片类 | 能编译 | 桌面构建直接失败 |
| 自己开 Thread / new Handler(自建 Looper) | 破坏单线程模型 | PC 端崩或渲染撕裂 |
| 直接读文件存档 | 绕过实时落盘，崩溃丢分 | 读写到错误目录 |
| 自己 finish()/弹窗 | 绕过结算回传 | 结算金币丢失 |

**扩展检查单补充（双端）：**

- [ ] `python desktop/build_desktop.py` 编译通过（垫片白名单校验的硬门槛）；
- [ ] `java -jar dist/…jar --demo` 截图正常（桌面端实跑）；
- [ ] PC 窗口里鼠标左键=触摸按下/抬起、拖动=MOVE，操作可达（无"必须多点触控"的交互）；
- [ ] 深浅主题下首页正常，游戏画面自成配色不受影响。

**给游戏加 PC 端演示脚本（可选但推荐）**：在
`desktop/src/…/DemoRunner.java` 的 `SCRIPTS` 里加一条该 id 的操作序列
`{action, fx, fy, delayMs}`，`--demo` 就能自动游玩并截图，直观验证新游戏。

## 七、全量运行时自测（每次批量加游戏后必跑）

网页壳可以直接当自动化测试台：`--demo --web 8082`（演示目录 + 网页服务，
不污染真档），再用 `tools/selftest_web.py` 对 100 款逐个
"开局 → 注入随机操作 → 查状态"：

```bash
python desktop/build_desktop.py
start javaw -jar dist/baiyou-pocket-desktop-v1.0.jar --demo --web 8082
python tools/selftest_web.py          # 全量扫描（约 3 分钟）
```

判定口径：

- **`desktop-data-demo/logs/` 出现新 `web-game-*.log` = 真崩溃，必须修**；
- over=true ≠ bug：时机/躲避类游戏被随机乱点打死是正常机制——
  单独复测（`tools/retest_suspects.py`）可复现且符合玩法即放行，
  不可复现的也放行；
- 谜题类（一笔画/激光/词链等）改生成逻辑时必须重验"可解性"；
- 脚本坑：MJPEG 流必须 `read(固定字节)`，无参 `read()` 会永久阻塞
  （流数据持续到达，socket 超时永不触发）。
