# 更新日志

## 3.0.2

- **修复未选择 Module 时代码生成到错误目录**：当项目没有明确 module（或对话框中未选中 module）时，之前会把 package 目录创建在项目根目录下（与 `src` 同级）。现在会自动回退到第一个含源码根的 module，正确生成到 `src/main/java` 下。

## 3.0.1

- **兼容最新版 IntelliJ IDEA 2026**：全面迁移至 IntelliJ IDEA 2026.1 平台，支持 2026.1 及以后的所有版本。
- **构建系统升级**：
  - 构建插件由 `org.jetbrains.intellij` 1.17.0 升级到全新的 `org.jetbrains.intellij.platform` 2.18.1。
  - Gradle 由 8.10.2 升级到 9.6.1。
  - 编译目标由 Java 17 升级到 Java 21。
- **内部/废弃 API 重构**（提升对新版本的兼容性与稳定性）：
  - 编辑器只读切换改用公开接口 `EditorEx.setViewer`（原 `EditorImpl`）。
  - 文件文本读取改用公开接口 `VfsUtilCore.loadText`（原 `LoadTextUtil`）。
  - 资源文本读取改用标准 Java 实现（原内部类 `UrlUtil`）。
  - 所有右键菜单动作补充 `getActionUpdateThread`，避免新版本下的线程告警。
  - 通知改用注册的通知组 + `NotificationGroupManager`（替代已废弃的 `SYSTEM_MESSAGES_GROUP_ID`）。
- **上传市场告警修复**：移除 `plugin.xml` 中对 `com.intellij.modules.java` 的重复依赖，去除内部 API `IdeBundle` 的使用。
- **兼容范围调整**：因按 2026 平台 + Java 21 编译，本版本不再支持 2023/2024 等旧版 IDEA。

## 2.0.3

- **记住模板勾选状态**：上次生成代码时勾选的模板（Controller/Service/Entity/Dao/ServiceImpl 等）会自动恢复选中，无需重新勾选。

## 2.0.2

- **记住上次生成代码的选择**：打开"Generate Code"对话框时，自动填充上次成功生成时的选择（Module、Package、removePre、Template Group、复选框），避免重复选择。已配置过的表仍使用其自身保存的设置。
