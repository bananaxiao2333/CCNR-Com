# CCNR-Com 对讲机模组（Forge 1.20.1）

一个对讲机（Walkie Talkie）模组：玩家设定自己的频道后，用 `/r` 和同频道玩家通话；
联动 [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice)，**按住说话时同频道玩家可以远程听到你的语音**。

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

- 只有**频道相同**的玩家才能收发，由服务端强制校验，客户端无法绕过。
- **语音联动（需安装 Plasmo Voice）**：按住说话键（PV 的按键）时，同频道玩家**无视距离**以全音量听到你的声音；不在频道里则保持 PV 原有的近距语音。
- **管理通讯**：`/a` 消息只有 OP（≥2 级）或拥有权限节点 `ccnrcom.admin.chat` 的玩家能收到（可用 LuckPerms 等权限插件授予，默认拒绝）。
- **场外通讯**：`/o` 无视频道和距离，广播给服务器所有在线玩家。
- **说话者 HUD**：屏幕侧边显示当前谁在说话（对讲机与附近都显示），蓝色背景，饱和度/透明度随音量电平变化，全部可配置。
- 收到文字消息时客户端播放"哔"提示音（可配置）。
- **开麦反馈**：按住说话时 ActionBar 显示频道与对讲状态，并播放"滴"声（可配置关闭）。
- 支持中英文界面语言（游戏语言自动切换）。
- 频道在服务器重启后会清空。

## 安装（服务端 + 客户端都要装）

1. 客户端：安装 Forge 1.20.1，把 `build/libs/ccnrcom-1.0.0.jar` 放进 `mods` 文件夹。
2. 服务端：同样使用 Forge 1.20.1 服务端，放同一个 jar。
3. **语音功能**：服务端和客户端都需额外安装 [Plasmo Voice](https://modrinth.com/plugin/plasmo-voice)（Forge 1.20.1 版本）；不装也能用文字对讲。

## 配置（serverconfig/ccnrcom-common.toml）

| 配置项 | 默认值 | 说明 |
| --- | --- | --- |
| `maxMessageLength` | 256 | `/r` 消息最大长度 |
| `channelPattern` | `^\\d{1,4}\\.\\d{1,4}$` | 频道格式正则 |
| `crossDimension` | true | 是否允许跨维度收发 |
| `chatRange` | -1 | `/r` 文字消息距离限制（方块），-1 无限制 |
| `echoToSender` | true | 发送者是否回显自己的消息 |
| `voiceEnabled` | true | 是否启用 Plasmo Voice 语音联动 |
| `voiceRange` | -1 | 语音转发距离限制（方块），-1 无限制 |
| `beepOnReceive` | true | 收到消息时是否播放提示音 |

## 构建

已配置国内镜像：Gradle 发行版走腾讯云，Maven Central 走阿里云。

需要 JDK 17 或更高（本机示例使用 brew 的 JDK 21）：

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
./gradlew build
```

产物：`build/libs/ccnrcom-1.0.0.jar`
开发运行：`./gradlew runServer` / `./gradlew runClient`（开发环境会自动加载 `libs/` 下的 Plasmo Voice 用于联调）

## 依赖说明

- `libs/plasmovoice-forge-1.20.1-2.1.13.jar`：Plasmo Voice 官方 jar，仅用于编译期 API，**不会**被打进产物；运行时需玩家自行安装 Plasmo Voice。
- 模组图标：`src/main/resources/icon.png`（128×128）。
- Plasmo Voice 是可选依赖：没装时语音功能自动禁用，文字对讲不受影响。