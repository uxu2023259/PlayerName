# NameTagSync｜头顶称号同步 - 让玩家名牌自动显示 LuckPerms 前后缀

## 一句话介绍
一款轻量、免配置的 Paper 玩家头顶名牌同步插件，可自动把 LuckPerms 的前缀、玩家名与后缀显示到玩家头顶，支持 PlaceholderAPI、热重载兼容与实时刷新。

## 推荐 MineBBS 发布标题
[管理/显示] NameTagSync｜头顶称号同步 - 自动显示 LuckPerms 前后缀｜轻量免配置｜支持 PlaceholderAPI｜Paper 1.20.4-1.21.11

## 备选标题
1. [轻量工具] NameTagSync｜玩家头顶名牌同步 LuckPerms 称号/后缀，开箱即用
2. [服务器管理] NameTagSync - 让 LuckPerms 称号显示在玩家头顶｜免配置自动同步
3. [Paper] NameTagSync｜头顶名称美化插件，自动同步权限组前后缀

## 插件页短介绍
NameTagSync 是一款专注于“玩家头顶名称显示”的轻量插件。安装后无需配置，即可自动读取 LuckPerms 的玩家前缀和后缀，并同步到玩家头顶名牌中。适合生存服、群组服、RPG 服、小游戏大厅等需要展示身份、称号、职位或权限组的服务器。

## 插件页详细介绍
### 插件简介
你是否希望玩家头顶不只是普通 ID，而是能直接显示权限组、称号、身份或后缀？

NameTagSync 可以自动将 LuckPerms 中的玩家前缀、玩家名和后缀组合成头顶名牌显示，例如：

```text
[管理员] Steve [建筑师]
[VIP] Alex
```

插件以轻量和稳定为核心，不需要复杂配置，不需要数据库，不会强制接管聊天系统，只处理玩家头顶名称显示。

### 核心功能
- 自动同步 LuckPerms 前缀、玩家名、后缀到玩家头顶名牌
- 支持 PlaceholderAPI，优先解析 `%luckperms_prefix%` 和 `%luckperms_suffix%`
- 未安装 PlaceholderAPI 时，可直接读取 LuckPerms 数据
- 支持 `&` 颜色代码与 `&#RRGGBB` 十六进制颜色
- 玩家进服、切换世界、权限数据更新时自动刷新
- LuckPerms 或 PlaceholderAPI 启停后自动重新同步
- 自动清理插件创建的计分板队伍，降低残留数据风险
- 免配置，放入插件目录即可使用

### 适用场景
- 生存服：显示玩家所在权限组、称号或身份
- RPG 服：显示职业、阵营、爵位、等级称号
- 大厅服：显示 VIP、管理员、主播、赞助者等身份
- 小型服务器：不想安装复杂名牌系统，只需要稳定显示前后缀

### 依赖与兼容
#### 必需
- Paper 1.20.4 至 1.21.11 或兼容 Paper API 的服务端
- Java 17 及以上；运行 Paper 1.21.11 服务端时需使用 Java 21

#### 推荐
- LuckPerms：用于提供前缀和后缀数据

#### 可选
- PlaceholderAPI：用于解析 LuckPerms 变量

### 安装方法
1. 将插件 jar 文件放入服务器的 `plugins` 目录。
2. 确保服务器已安装 LuckPerms。
3. 重启服务器或使用插件管理工具加载插件。
4. 在 LuckPerms 中设置玩家或权限组的 prefix/suffix。
5. 玩家头顶名牌会自动显示对应内容。

### LuckPerms 示例
```text
/lp group admin meta setprefix 100 "&c[管理员]&r"
/lp group vip meta setprefix 100 "&6[VIP]&r"
/lp user Steve meta setsuffix 100 "&b[建筑师]&r"
```

### 常见问题
#### 为什么头顶没有显示前后缀？
请先确认 LuckPerms 中已经设置了 prefix 或 suffix，并且玩家拥有对应权限组或元数据。

#### 必须安装 PlaceholderAPI 吗？
不必须。安装 PlaceholderAPI 后插件会优先解析变量；未安装时插件会直接读取 LuckPerms 数据。

#### 支持彩色前后缀吗？
支持。插件支持常见的 `&a`、`&c` 等颜色代码，也支持 `&#66ccff` 这类十六进制颜色写法。

#### 会影响聊天格式吗？
不会。插件只处理玩家头顶名牌，不修改聊天消息。

#### 可以热重载吗？
插件对 LuckPerms、PlaceholderAPI 以及常见插件管理器做了软依赖声明，并会在相关插件启停时重新同步。仍建议正式环境优先使用重启方式更新插件。

### 推荐配图文案
- 主标题：NameTagSync
- 副标题：自动同步 LuckPerms 前后缀到玩家头顶
- 卖点：轻量免配置｜支持颜色｜实时刷新｜Paper 1.20.4-1.21.11

## 更新日志模板
### 1.0.4
- 编译基线调整为 Paper 1.20.4，兼容 Paper 1.20.4 至 1.21.11
- 使用 Java 17 编译；插件兼容 Java 17 及以上，Paper 1.21.11 服务端需 Java 21

### 1.0.3
- 升级 Paper API 至 1.21.11
- 使用 Java 21 编译，适配 Paper 1.21.11

### 1.0.2
- 优化发布信息与插件元数据
- 整理 MineBBS 发布文案与图标方案
- 重新打包发布产物

## 标签建议
LuckPerms、头顶名称、名牌、称号、前缀、后缀、Paper、PlaceholderAPI、服务器管理、轻量插件
