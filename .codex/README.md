# Codex 多 Agent 工作流

本项目采用分层模型路由，在保证 Android、Shizuku/Root、MaaCore 与 native bridge 改动质量的同时控制成本。

| 角色 | 模型 | 用途 |
| --- | --- | --- |
| 主代理 | GPT-5.6 Terra / medium | 日常调度、小改动、汇总结果 |
| explorer | GPT-5.6 Luna / low | 只读检索、调用链与测试定位 |
| mechanical-worker | GPT-5.6 Luna / medium | 文档、资源、测试骨架、机械修改 |
| implementer | GPT-5.6 Terra / medium | 常规 Kotlin、Compose、Gradle、服务与 native 实现 |
| planner | GPT-5.6 Sol / medium | 跨模块、高风险或架构规划 |
| reviewer | GPT-5.6 Sol / medium | 最终差异与测试证据验收 |

## VSCode 使用方式

修改配置后执行 `Developer: Reload Window` 并新建 Codex 对话。明确要求使用项目工作流：

```text
请使用项目的多 Agent 工作流处理这个任务。
先按成本路由：简单检索交给 explorer，机械修改交给 mechanical-worker，
正常实现交给 implementer；仅在跨模块或高风险时调用 planner。
实施完成后由 reviewer 检查最终 diff 和测试证据。
并行任务必须拥有互不重叠的文件。
```

对于单文件、低风险且需求明确的改动，直接让 Terra 主代理完成通常更省，不需要启动子代理。

## 验证策略

按成本从低到高执行：

1. 受影响测试类或测试方法；
2. `./gradlew :app:testDebugUnitTest`；
3. 受影响模块的检查任务；
4. 只有发布、构建系统、资源或 native 改动才运行 `./gradlew assembleDebug`。

运行构建需要 JDK 25。仅在任务明确需要 MaaCore 产物时运行 `python scripts/setup_maa_core.py`。

## 调整建议

- 如果 Luna 在当前子代理入口不可用，把两个 Luna 角色临时改为 `gpt-5.6-terra`。
- 如果任务涉及权限、Shizuku/Root 双进程、后台生命周期或 native 内存安全，可将对应 Sol 角色的 `model_reasoning_effort` 临时提高到 `high`。
- 不建议把主代理长期设为 Sol，也不建议让 Luna 独立承担没有明确边界的生产代码修改。
