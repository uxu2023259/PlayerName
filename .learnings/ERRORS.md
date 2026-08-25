# Errors

Command failures and integration errors.

---
## [ERR-20260505-001] ripgrep_files_no_output

**Logged**: 2026-05-05T20:24:38.9912036+08:00
**Priority**: low
**Status**: pending
**Area**: infra

### Summary
在项目根目录执行 g --files 返回退出码 1 且无输出。

### Error
``
Exit code: 1; stdout/stderr 均为空。
``

### Context
- Command: & 'C:\ripgrep\rg.exe' --files
- Working directory: C:\Users\hyx\Desktop\PlayerName
- 可能原因：项目目录为空、所有文件被忽略，或当前工作区尚未初始化。

### Suggested Fix
先使用 PowerShell 目录枚举确认工作区内容；后续代码/内容搜索仍按项目要求使用 C:\ripgrep\rg.exe。

### Metadata
- Reproducible: unknown
- Related Files: .learnings/ERRORS.md

---
## [ERR-20260505-002] maven_compile_handlerlist_overload

**Logged**: 2026-05-05T20:31:40.1713855+08:00
**Priority**: medium
**Status**: resolved
**Area**: backend

### Summary
首次构建时 HandlerList.unregisterAll(this) 在 Java 编译阶段出现重载歧义。

### Error
``
对 unregisterAll 的引用不明确：Plugin 与 Listener 两个重载都匹配。
``

### Context
- Command: mvn -q clean package
- File: src/main/java/cn/hyx/playername/PlayerNamePlugin.java
- Cause: 主类同时继承 JavaPlugin 并实现 Listener。

### Suggested Fix
显式转型为 Listener 或改用 HandlerList.unregisterAll((Listener) this)。

### Metadata
- Reproducible: yes
- Related Files: src/main/java/cn/hyx/playername/PlayerNamePlugin.java

### Resolution
- **Resolved**: 2026-05-05T20:31:40.1751765+08:00
- **Notes**: 准备将调用改为显式 Listener 转型后重新构建。

---
## [ERR-20260505-003] maven_compile_reflective_exception

**Logged**: 2026-05-05T20:32:09.4094529+08:00
**Priority**: medium
**Status**: resolved
**Area**: backend

### Summary
第二次构建时反射辅助方法声明抛出 ReflectiveOperationException，调用处捕获范围不完整。

### Error
``
未报告的异常错误 java.lang.ReflectiveOperationException；必须对其进行捕获或声明以便抛出。
``

### Context
- Command: mvn -q clean package
- File: src/main/java/cn/hyx/playername/PlayerNamePlugin.java
- Cause: 调用处只捕获了部分反射异常子类。

### Suggested Fix
将调用处捕获范围改为 ReflectiveOperationException。

### Metadata
- Reproducible: yes
- Related Files: src/main/java/cn/hyx/playername/PlayerNamePlugin.java

### Resolution
- **Resolved**: 2026-05-05T20:32:09.4118802+08:00
- **Notes**: 准备扩大捕获范围后重新构建。

---
## [ERR-20260517-001] powershell_same_file_pipeline

**Logged**: 2026-05-17T00:00:00+08:00
**Priority**: low
**Status**: pending
**Area**: docs

### Summary
PowerShell 管道中对同一个文件同时 Get-Content 与 Set-Content 时发生文件占用。

### Error
```
The process cannot access the file because it is being used by another process.
```

### Context
- 操作：读取网页模板后用 -replace 直接管道写回同一路径。
- 文件：C:\Users\hyx\Desktop\hyx823894.github.io-main\plugin-template.html

### Suggested Fix
先将文件内容读入变量并完成替换，再调用 Set-Content 写回，避免同一管道同时持有读写句柄。

### Metadata
- Reproducible: yes
- Related Files: C:\Users\hyx\Desktop\hyx823894.github.io-main\plugin-template.html

---
## [ERR-20260724-001] portable_jdk_download_blocked

**Logged**: 2026-07-24T18:00:00+08:00
**Priority**: low
**Status**: resolved
**Area**: infra

### Summary
准备便携 Java 21 的下载、解压和执行组合命令被终端安全策略拦截。

### Error
```
command rejected: blocked by policy
```

### Context
- 目的：为 Paper 1.21.11 Maven 构建准备 Java 21。
- 原因：终端已存在可用的 Java 21，只需改用现有运行时即可完成构建。

### Suggested Fix
构建前先使用 `C:\ripgrep\rg.exe` 定位本机已有 `java.exe`，避免不必要的下载操作。

### Metadata
- Source: error
- Related Files: pom.xml
- Tags: java-21, build, environment

### Resolution
- **Resolved**: 2026-07-24T18:00:00+08:00
- **Notes**: 使用 VS Code Java 扩展内置的 Java 21 成功完成 `mvn clean verify`。

---
## [ERR-20260724-002] powershell_maven_property_argument

**Logged**: 2026-07-24T18:00:00+08:00
**Priority**: low
**Status**: resolved
**Area**: infra

### Summary
PowerShell 执行 Maven `-Dexpression=maven.compiler.release` 时未引用参数，点号被解析为生命周期阶段分隔符。

### Error
```
Unknown lifecycle phase ".compiler.release"
```

### Context
- 操作：构建后查询 Maven 编译 release 属性。
- 影响：只影响查询命令，不影响已成功完成的构建。

### Suggested Fix
PowerShell 中将 Maven 系统属性参数写为 `'-Dexpression=maven.compiler.release'`。

### Metadata
- Source: error
- Related Files: pom.xml
- Tags: maven, powershell, verification

### Resolution
- **Resolved**: 2026-07-24T18:00:00+08:00
- **Notes**: 已从 POM 结构和构建产物主版本号完成等价验证。

---
## [ERR-20260517-002] powershell_bash_heredoc

**Logged**: 2026-05-17T00:00:00+08:00
**Priority**: low
**Status**: pending
**Area**: infra

### Summary
在 PowerShell 中使用 Bash 风格的 `python - <<'PY'` here-doc 导致解析错误。

### Error
```
Missing file specification after redirection operator.
```

### Context
- 操作：验证 HTML 文件 UTF-8 时误用 Bash here-doc 语法。

### Suggested Fix
在 PowerShell 中使用 here-string：`@' ... '@ | python -`。

### Metadata
- Reproducible: yes
- Related Files: C:\Users\hyx\Desktop\hyx823894.github.io-main\plugin-template.html

---
