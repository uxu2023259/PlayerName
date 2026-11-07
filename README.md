# NameTagSync（PlayerName）

NameTagSync 是一个面向 Paper 的玩家头顶名牌同步插件。它把 LuckPerms 的前缀、玩家名和后缀组合成头顶显示名称，并同步到服务器使用的计分板队伍中。插件不修改聊天消息或玩家列表名称。

## 环境与可选插件

- Paper 1.20.4 API；插件描述文件的 `api-version` 为 1.20。
- Java 17。
- LuckPerms 可选：提供玩家前缀和后缀；未检测到时只显示玩家名。
- PlaceholderAPI 可选：若存在，优先解析 `%luckperms_prefix%` 和 `%luckperms_suffix%`；解析不可用时回退到 LuckPerms API。

## 安装与行为

将构建产物 `target/PlayerName-1.0.5.jar` 放入服务器的 `plugins` 目录并重启。插件无需配置文件或命令。玩家加入、离开或切换世界，以及 LuckPerms/PlaceholderAPI 启用或停用时，插件会重新同步名牌；正常运行期间每 40 tick（约 2 秒）刷新一次。

插件会为在线玩家在主计分板和玩家当前使用的计分板上创建队伍，并将名称可见性设为始终显示。无论插件能否获取到前后缀，玩家名称仍会显示。插件使用 `pnm_` 前缀命名自己创建的队伍，并会在启动和停用时清理该前缀的队伍；其他插件或服务器配置不要将 `pnm_` 用作队伍名前缀。

## 从源码构建

在项目目录执行：

```bash
mvn package
```

Maven 的 Java 编译目标为 17，产物输出到 `target/PlayerName-1.0.5.jar`。

## 版本信息

当前 `pom.xml` 版本为 `1.0.5`，但 `src/main/resources/plugin.yml` 中仍标记为 `1.0.4`。这会造成构建文件名与服务器插件信息显示的版本不一致。
