# Agent 工作规约

本 fork 基于 `Aliothmoon/MAA-Meow` 的 `upstream/main`。所有 agent 必须遵守以下规则：

- 同步上游前先 `fetch`，比较双方的 merge base；合并时保留本 fork 的性能优化：startup defer、资源归档/延迟加载、UI state isolation，以及 benchmark/baseline profiles。
- 后台任务页必须保留 Material3 Expressive 风格、`TaskProfileSelectorPanel` 和圆形主操作；不得直接用 upstream 的 `BackgroundTaskView`、`TaskListDetailLayout` 或底部动作栏覆盖当前后台页。
- 小工具启动必须经过启动门禁；仓库识别必须保持 baseline/delta/profile 会话路由语义。任何数据迁移都必须配套测试。
- 中英文 README 必须同步更新；改动后运行相关的定向测试或编译检查。
- 保留现有用户改动；禁止修改 generated/build outputs 或 local configs。
