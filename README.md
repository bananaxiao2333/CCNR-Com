<div align="center">

<img src="src/main/resources/icon.png" width="128" height="128" alt="CCNR-Com 图标">

# CCNR-Com 对讲机模组（Forge 1.20.1）

一个对讲机（Walkie Talkie）模组：玩家设定自己的频道后，用 `/r` 和同频道玩家通话；
联动 [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice)，**按住说话时同频道玩家可以远程听到你的语音**。

[![Release](https://img.shields.io/github/v/release/bananaxiao2333/CCNR-Com?label=Release&color=brightgreen)](https://github.com/bananaxiao2333/CCNR-Com/releases)
[![Build](https://img.shields.io/github/actions/workflow/status/bananaxiao2333/CCNR-Com/build.yml?branch=main&label=Build)](https://github.com/bananaxiao2333/CCNR-Com/actions)
[![License](https://img.shields.io/github/license/bananaxiao2333/CCNR-Com?label=License)](LICENSE)
[![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-important)](https://www.minecraft.net)
[![Forge](https://img.shields.io/badge/Forge-47.2.0%2B-orange)](https://files.minecraftforge.net)
[![Plasmo Voice](https://img.shields.io/badge/Plasmo%20Voice-2.1.13-blueviolet)](https://modrinth.com/plugin/plasmo-voice)

</div>

## 功能

| 命令 | 说明 |
| --- | --- |
| `/channel <频道>` | 设定自己的频道（默认格式 数字.数字，如 `123.4`、`462.5625`） |
| `/channel` | 查看当前频道 |
| `/channel clear` | 退出频道 |
| `/channel set <玩家> <频道>` | 管理员：批量设定频道（支持 `@a` `@p` `@r` 等选择器） |
| `/channel clear <玩家>` | 管理员：批量清除频道（支持选择器） |
| `/channel get <玩家>` | 管理员：查看频道（支持选择器） |
| `/r <消息>` | 向同频道所有在线玩家发送对讲机消息 |
| `/a <消息>` | 管理通讯：仅 OP 或拥有 `ccnrcom.admin.chat` 权限节点的玩家可收发 |
| `/o <消息>` | 场外通讯（OOC）：灰色`[场外通讯]`前缀 + 黄色玩家名，无视距离全员可见 |
| `/ccnr chat` | 打开气泡对话面板（快捷键 G） |
| `/ccnr hud <on\|off\|toggle>` | 切换说话者语音 HUD |
| `/ccnr theme <auto\|light\|dark>` | 切换界面主题 |

- 只有**频道相同**的玩家才能收发，由服务端强制校验，客户端无法绕过。
- **语音联动（需安装 Plasmo Voice）**：按住说话键（PV 的按键）时，同频道玩家**无视距离**以全音量听到你的声音；不在频道里则保持 PV 原有的近距语音。
- **管理通讯**：`/a` 消息只有 OP（≥2 级）或拥有权限节点 `ccnrcom.admin.chat` 的玩家能收到（可用 LuckPerms 等权限插件授予，默认拒绝）。
- **场外通讯**：`/o` 无视频道和距离，广播给服务器所有在线玩家。
- **气泡对话界面**：全屏气泡消息面板（头像 / 名字色 / 时间 / 自动换行）、消息过滤（全部 / 对讲 / 管理 / 场外）、频道快速切换、历史搜索、右键菜单（复制 / 切频道 / 屏蔽）；
- **设置界面**：APP 风格设置屏（主题 / 气泡界面 / 语音 HUD / 历史上限 / 提示音 / ActionBar 反馈等，即时生效）。
- **说话者 HUD**：屏幕侧边显示当前谁在说话（对讲机与附近都显示），蓝色背景，饱和度/透明度随音量电平变化，全部可配置。
- 收到文字消息时客户端播放"哔"提示音（可配置）。
- **开麦反馈**：按住说话时 ActionBar 显示频道与对讲状态，并播放"滴"声（可配置关闭）。
- 支持中英文界面语言（游戏语言自动切换）。
- 频道在服务器重启后会清空。

## 安装（服务端 + 客户端都要装）

1. 客户端：安装 Forge 1.20.1，把 `ccnrcom-1.1.0.jar` 放进 `mods` 文件夹。
2. 服务端：同样使用 Forge 1.20.1 服务端，放同一个 jar。
3. **语音功能**：服务端和客户端都需额外安装 [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice)（Forge 1.20.1 版本）；不装也能用文字对讲。

## 配置（serverconfig/ccnrcom-common.toml）

服务端与客户端共用一份 COMMON 配置；单机/客户端侧对应 `config/ccnrcom-common.toml`。

| 分类 | 配置项 | 默认值 | 说明 |
| --- | --- | --- | --- |
| chat | `maxMessageLength` | 256 | `/r` 消息最大长度 |
| chat | `channelPattern` | `^\\d{1,4}\\.\\d{1,4}$` | 频道格式正则 |
| chat | `crossDimension` | true | 是否允许跨维度收发 |
| chat | `chatRange` | -1 | `/r` 文字消息距离限制（方块），-1 无限制 |
| chat | `echoToSender` | true | 发送者是否回显自己的消息 |
| voice | `voiceEnabled` | true | 是否启用 Plasmo Voice 语音联动 |
| voice | `voiceRange` | -1 | 语音转发距离限制（方块），-1 无限制 |
| voice | `beepOnReceive` | true | 收到消息时是否播放提示音 |
| voice | `actionBarFeedback` | true | 开麦/关麦时 ActionBar 状态反馈 |
| gui | `themeMode` | auto | 界面主题：auto / light / dark |
| gui | `bubbleChat` | true | 气泡对话界面总开关 |
| gui | `historyLimit` | 100 | 每桶本地消息历史上限 |
| hud | `hudEnabled` | true | 语音说话者 HUD 总开关 |
| hud | `hudPosition` | left | HUD 位置：left / right（默认左上角） |
| hud | `hudOffsetX` / `hudOffsetY` | 4 / 8 | HUD 水平/垂直偏移（像素） |
| hud | `hudScale` | 1.0 | HUD 缩放（0.5~2.0） |
| hud | `hudMaxEntries` | 8 | HUD 最大显示条数 |
| hud | `hudColor` | 1F6EFF | 背景基础颜色（RRGGBB，音量越高越饱和） |
| hud | `hudMinAlpha` | 0.25 | 背景最小透明度（0~1，音量越高越高） |

## 构建

GitHub Actions 已配置自动构建（`push main` / 打 `v*` 标签 / PR），产物自动上传；
打 `v*` 标签时自动把 jar 附加到对应 GitHub Release（文件名取 `mod_version`）。

本地构建已配置国内镜像：Gradle 发行版走腾讯云，Maven Central 走阿里云。

需要 JDK 17 或更高（本机示例使用 brew 的 JDK 21）：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew build
```

产物：`build/libs/ccnrcom-1.1.0.jar`
开发运行：`./gradlew runServer` / `./gradlew runClient`（开发环境会自动加载 `libs/` 下的 Plasmo Voice 用于联调）
运行全部单元测试：`./gradlew build test -PrunTests`

## 依赖说明

- `libs/plasmovoice-forge-1.20.1-2.1.13.jar`：Plasmo Voice 官方 jar，仅用于编译期 API，**不会**被打进产物；运行时需玩家自行安装 Plasmo Voice。
- 模组图标：`src/main/resources/icon.png`（128×128）。
- Plasmo Voice 是可选依赖：没装时语音功能自动禁用，文字对讲不受影响。

## 致谢

- 气泡对话界面（全屏面板 / 侧边栏 / 消息横幅 / 圆角渲染等）参考并部分移植自 [E33Chat](https://github.com/E33EPUS/E33Chat)（MIT 协议，[@E33EPUS](https://github.com/E33EPUS)）——感谢作者的开源与分享。
