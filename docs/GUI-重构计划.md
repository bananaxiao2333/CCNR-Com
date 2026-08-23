# CCNR-Com GUI 重构计划（移植 E33Chat 聊天 APP 风格）

> 参考仓库：[E33EPUS/E33Chat](https://github.com/E33EPUS/E33Chat)（MIT，v2.3.7，Forge 1.20.1）
> 目标：把 CCNR-Com 的通讯界面（对讲机消息、管理通讯、OOC、说话者 HUD、设置）全部重做成 E33Chat 的聊天 APP 风格。

---

## 0. 参考架构（从 E33Chat 移植什么）

| E33Chat 组件 | 用途 | 移植方式 |
| --- | --- | --- |
| `RoundRectRenderer` + `rendertype_round_rect` shader | SDF 圆角矩形渲染（带缩放兼容、普通矩形 fallback） | 直接移植（MIT，保留版权头） |
| `ChatBubbleTheme` / `UiTokens` | light/dark 主题 token（颜色、间距、圆角半径） | 精简为 CCNR-Com 的 `CommsTheme` |
| `UiLayout` / `ChatLayout` | 布局计算（纯数学，可单测） | 移植核心方法 |
| `ChatScrollbar` / `SmoothScrollPane` | 滚动条（thumb 高度/位置纯计算） | 直接移植 |
| `ChatMessageRenderer` | 气泡渲染：头像（`SkinResolver`）、名字、时间、换行 | 改写为四类消息样式 |
| `ChatBubbleHudOverlay` | 游戏内悬浮窗（HUD 层绘制） | 改写为"对讲机窗口 + 说话者面板" |
| `ChatSidebar` | 私聊侧边栏 | 改写为"频道成员侧边栏"（数据源 = 现有 ChannelSync + PV 源状态） |
| `ChatContextMenus` / `ChatSearchPanel` / `ChatQuickChatPanel` | 右键菜单 / 搜索 / 常用语 | 搜索+菜单移植，常用语改为"频道快速切换" |
| `ChatBubbleConfigScreen` / `ChatSettingsMenu` | APP 风格设置界面 | 改写为 CCNR-Com 设置屏（写回 ForgeConfigSpec） |
| `ChatMessageStore` / `HistoryStore` | 本地消息历史（分桶、上限、持久化） | 移植，按频道分桶 |
| `Animation` / `AnimationStyle` | 出入场动画 | 移植（插值纯函数可单测） |
| JUnit5 测试（`-PrunTests` 显式开关） | 纯逻辑单测 | 沿用该模式 |

**合规**：E33Chat 是 MIT —— 移植文件保留原版权头，在 `LICENSE`/NOTICE 与 mods.toml `credits` 注明 "GUI 框架参考 E33Chat (E33EPUS)"。

---

## 1. 任务阶段与验收

### 阶段 0：GUI 基座移植（圆角渲染 + 主题 + 布局）

任务：
1. 移植 `RoundRectRenderer` 与 `rendertype_round_rect` shader（vertex/fragment），注册到 `RegisterShadersEvent`
2. 建 `CommsTheme`（light/dark 两套 token：主色/消息色/背景/圆角/间距）与 `CommsThemeRegistry`（`/ccnr theme dark|light` 命令或跟随游戏主题）
3. 移植 `UiLayout` 基础布局工具与 `SmoothScrollPane`、`ChatScrollbar`
4. 新增消息数据模型 `CommsMessage`（type: RADIO/ADMIN/OOC/SYSTEM，频道，发送者 UUID/名字，时间戳，文本）

验收：
- [ ] 游戏内（开发环境 runClient）画一个圆角矩形：圆角正确、带 alpha 混合、缩放（PoseStack scale）下不变形
- [ ] 删掉 shader 文件后 fallback 为直角矩形，无崩溃（异常路径）
- [ ] light/dark 主题切换后 token 生效
- [ ] 自动：`RoundRectParamsTest`（半径钳制、缩放换算）、`ChatScrollbarTest`（thumb 高度/位置，移植 E33Chat 用例）、`CommsThemeTest`（token 完整性：两套主题键集合一致）

### 阶段 1：消息存储与接入（无渲染）

任务：
1. 移植 `ChatMessageStore` → `CommsMessageStore`：按频道分桶、每桶上限（配置 `historyLimit`）、最近消息优先
2. 持久化：会话内 `HistoryStore`（JSON 到 config 目录，可选开关 `historyPersist`）
3. 现有网络包（RadioReceivePacket / CommsChatPacket）改写入 store，同时继续走旧聊天栏（双写，便于回滚）
4. 新增配置：`bubbleChat`（气泡界面总开关，默认 false，本阶段可手动开）

验收：
- [ ] 服务器发三类消息后，store 按频道/类型正确分桶且顺序正确
- [ ] 重启游戏后（开持久化）历史仍在
- [ ] 旧聊天栏显示不受影响（双写验证）
- [ ] 自动：`CommsMessageStoreTest`（CRUD、分桶、上限裁剪、序列化往返）、`CommsMessageTest`（类型/颜色 token 映射）

### 阶段 2：气泡渲染（核心视觉）

任务：
1. 移植 `ChatMessageRenderer` → `CommsMessageRenderer`：气泡 + 头像（先用 MC 自带皮肤贴图 `SkinResolver` 简化版）+ 名字色（对讲=青蓝、管理=红、OOC=灰）+ 时间戳 + 自动换行
2. 新增 `CommsBubbleScreen`（全屏，仿 ChatBubbleScreen：标题栏、滚动区、输入框）
3. `bubbleChat` 默认开；旧聊天栏可通过配置 `legacyChat` 保留
4. 出入场动画（`Animation` 移植：淡入/滑入，插值可单测）

验收：
- [ ] 三类消息 + 系统消息（频道变更提示）各自气泡样式正确、头像可加载
- [ ] 长消息换行、超高滚动、100 条消息滚动流畅（>60fps）
- [ ] 发送消息从输入框发出后回显为右侧气泡
- [ ] 自动：`CommsMessageRendererTest`（消息→行拆分/高度计算/颜色映射，纯函数化渲染布局）、`AnimationTest`（插值端点与单调性）

### 阶段 3：悬浮窗 + 频道侧边栏（HUD 层）

任务：
1. 移植 `ChatBubbleHudOverlay` → `CommsHudWindow`：可拖拽、可折叠、透明背景（BlurRenderer 简化版可后置）、滚动
2. 说话者面板并入悬浮窗：近场/对讲区分（现有 `ClientChannelState` + PV 源状态），音频电平驱动保留现状
3. `CommsSidebar`：频道成员列表（数据来自现有 ChannelStatePacket + 玩家进出事件），说话者高亮（PV isActivated），点击成员→右键菜单
4. 悬浮窗位置/大小记忆（`hudWindowX/Y/W/H` 配置或本地 JSON）

验收：
- [ ] 拖拽/折叠/滚动手感到位；位置记忆重启后生效
- [ ] 成员增删（上线、切频道）实时反映；说话者名字高亮且随 `ChannelSync` 更新
- [ ] 与阶段 2 全屏屏互不冲突（开全屏时悬浮窗隐藏）
- [ ] 自动：`CommsSidebarTest`（成员排序/过滤/高亮判定逻辑）、`HudWindowGeometryTest`（拖动边界钳制、折叠状态机）

### 阶段 4：面板与交互

任务：
1. `CommsChannelPanel`（仿 ChatQuickChatPanel）：频道列表 + 一键切换（调现有 `/channel` 逻辑）+ 常用频道收藏
2. `CommsSearchPanel`：按频道/发送者/文本搜历史
3. 右键菜单 `CommsContextMenus`：复制消息 / 跳转频道 / 私聊（对讲）/ 屏蔽（BlockList 移植）
4. 通知：@或名字被提及时悬浮提示（`MentionNotificationBanner` 简化版：仅"你被 @ 时"+ 频道内呼叫）

验收：
- [ ] 切换频道→侧边栏/气泡配色即时更新；收藏持久化
- [ ] 搜索三种条件组合正确；结果点击滚动定位
- [ ] 菜单三项动作全部生效（复制到剪贴板、频道跳转、屏蔽后该玩家消息折叠）
- [ ] 自动：`ChannelPanelTest`（收藏/排序）、`SearchQueryTest`（解析与匹配）、`ContextMenuActionTest`（动作分发）

### 阶段 5：设置界面

任务：
1. `CommsConfigScreen`（仿 ChatBubbleConfigScreen）：分页（频道 / HUD / 消息样式 / 语音 / 高级）
2. 读写现有 ForgeConfigSpec：`maxMessageLength`、`hud*`、`voice*`、`bubbleChat`、`history*` 等
3. 服务端配置界面仅 OP 可见（复用 `Permissions.canAdmin`）

验收：
- [ ] 每个设置项改完即时生效；重启保留；与 toml 文件双向一致（改 toml 后 UI 刷新）
- [ ] OP 才看到"服务器设置"页
- [ ] 自动：`ConfigScreenBinderTest`（UI 值↔配置值绑定映射表测试，纯映射可测）、`ServerConfigSyncTest`（服务端配置包往返）

### 阶段 6：资源、性能与发布

任务：
1. light/dark 全套纹理（参照 E33Chat 资源布局，本 mod 可先用纯色 + 圆角 shader 减少贴图依赖）
2. i18n 补齐新界面文案（en/zh）
3. 性能：渲染节流（消息追加批次）、悬浮窗合帧、历史上限
4. CI 全绿 + 打 v1.1.0 标签发布（自动构建已有）

验收：
- [ ] 双主题下所有界面无贴图缺失/错位
- [ ] 半小时游玩：内存稳定（历史受上限约束）、无异常日志
- [ ] 服务器端无改动可正常连接（纯客户端 GUI 不影响协议）
- [ ] CI：build + test + spotless 全绿；Release 带 jar

---

## 2. 自动化测试怎么做

**框架**：JUnit 5（同 E33Chat：`testImplementation platform('org.junit:junit-bom:5.11.4')` + `org.junit.jupiter:junit-jupiter`）。
**运行开关**（沿用 E33Chat 做法，离线开发环境不自动跑）：

```gradle
tasks.withType(Test).configureEach {
    useJUnitPlatform()
    onlyIf { project.hasProperty('runTests') }
}
```

本地：`./gradlew test -PrunTests`；CI：build.yml 增加 `./gradlew test -PrunTests`。

**原则**：能抽成纯逻辑的绝不碰 MC 运行时 ——
- 布局/滚动/几何/动画插值/主题 token/消息模型/存储/编解码 → 普通 JUnit（无 Minecraft 依赖，秒级）
- 需要 MC 类的客户端渲染逻辑 → 拆"布局计算纯函数"与"绘制"两层，只测前者
- 服务端频道/命令逻辑 → Forge GameTest（`forge.enabledGameTestNamespaces=ccnrcom`，已在 run 配置），或先用现有生产服务器冒烟 + 断言日志

**测试矩阵**（每阶段的自动测试已在上面验收清单中列明）。

## 3. 代码风格检查怎么做

**工具**：Spotless + palantir-java-format（或 google-java-format，二选一，仓库统一）。

```gradle
plugins {
    id 'com.diffplug.spotless' version '6.25.0'
}
spotless {
    java {
        target 'src/main/java/**/*.java', 'src/test/java/**/*.java'
        palantirJavaFormat()
        licenseHeaderFile 'config/spotless/license-header.txt'  // MIT 头
        trimTrailingWhitespace()
        endWithNewline()
    }
}
```

- 提交前：`./gradlew spotlessApply`
- CI 门禁：`./gradlew build spotlessCheck test -PrunTests`
- 另加 `.editorconfig`（UTF-8、4 空格缩进、LF）与 `checkstyle`（可选，规则严格度低：导入顺序/未用导入）

---

## 4. 里程碑（建议节奏）

| 里程碑 | 内容 | 预计 |
| --- | --- | --- |
| M1 | 阶段 0 + 1（基座 + 存储） | 2-3 天 |
| M2 | 阶段 2（气泡渲染，可发布预览版） | 3-5 天 |
| M3 | 阶段 3 + 4（悬浮窗/侧边栏/交互） | 4-6 天 |
| M4 | 阶段 5 + 6（设置/资源/发布 v1.1.0） | 3-4 天 |

> 每个阶段独立可验收、可回滚（配置开关保留旧界面）；阶段 2 完成后即可发一个"气泡预览版"给玩家试用收集反馈。
