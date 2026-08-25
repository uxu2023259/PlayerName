# Learnings

Corrections, insights, and knowledge gaps captured during development.

**Categories**: correction | insight | knowledge_gap | best_practice

---
## [LRN-20260517-001] correction

**Logged**: 2026-05-17T19:49:00+08:00
**Priority**: high
**Status**: pending
**Area**: docs

### Summary
用户提供外部模板文件作为参考时，不应直接修改源文件，应复制到当前插件目录后再改。

### Details
本次误将 `C:\Users\hyx\Desktop\hyx823894.github.io-main\plugin-template.html` 作为修改目标。用户纠正要求“不要改源文件，放到自己的目录改”。已将 NameTagSync 页面保存到当前插件目录，并把外部模板恢复为填坑球页面。

### Suggested Action
以后遇到用户提到外部模板或参考文件时，先复制到当前项目的 `release` 或其他自有目录，再对副本进行编辑；除非用户明确要求，否则不要写入外部源文件。

### Metadata
- Source: user_feedback
- Related Files: C:\Users\hyx\Desktop\PlayerName\release\NameTagSync-插件介绍页.html
- Tags: file-safety, docs, html

---
