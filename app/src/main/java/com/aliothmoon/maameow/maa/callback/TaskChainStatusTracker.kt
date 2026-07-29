package com.aliothmoon.maameow.maa.callback

import com.aliothmoon.maameow.maa.task.TaskSlot
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class TaskRunStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    ERROR
}

data class TaskRunInfo(
    val taskId: Int,
    val taskChain: String,
    val status: TaskRunStatus,
    val slot: TaskSlot? = null,
)

/**
 * Aggregate MaaCore tasks by the configuration item visible to the user.
 * Container items can expand into several Core tasks but still count as one item.
 */
internal fun List<TaskRunInfo>.groupByVisibleTask(): List<TaskRunInfo> {
    return groupBy { info -> info.slot?.nodeId?.let { "node:$it" } ?: "task:${info.taskId}" }
        .values
        .map { group ->
            val status = when {
                group.any { it.status == TaskRunStatus.ERROR } -> TaskRunStatus.ERROR
                group.all { it.status == TaskRunStatus.COMPLETED } -> TaskRunStatus.COMPLETED
                group.any { it.status == TaskRunStatus.IN_PROGRESS } ||
                    group.any { it.status == TaskRunStatus.COMPLETED } -> TaskRunStatus.IN_PROGRESS
                else -> TaskRunStatus.PENDING
            }
            val representative = group.firstOrNull { it.status == TaskRunStatus.IN_PROGRESS }
                ?: group.first()
            representative.copy(status = status)
        }
}

/**
 * 跟踪每条任务链的运行状态（taskId → status）。
 *
 * - 任务 append 时 [register] 注册为 PENDING
 * - 回调到达时通过 [updateStatus] 推进状态
 * - 全部完成或停止时 [clear]
 *
 * [tasks] StateFlow 供 UI 消费。
 */
class TaskChainStatusTracker {

    private val _tasks = MutableStateFlow<List<TaskRunInfo>>(emptyList())
    val tasks: StateFlow<List<TaskRunInfo>> = _tasks.asStateFlow()

    private val registry = LinkedHashMap<Int, TaskRunInfo>()

    fun register(taskId: Int, taskChain: String, slot: TaskSlot? = null) {
        registry[taskId] = TaskRunInfo(taskId, taskChain, TaskRunStatus.PENDING, slot)
        emit()
    }

    fun updateStatus(taskId: Int, status: TaskRunStatus) {
        registry.computeIfPresent(taskId) { _, info -> info.copy(status = status) }
        emit()
    }

    fun getNodeId(taskId: Int): String? = registry[taskId]?.slot?.nodeId

    fun clear() {
        registry.clear()
        emit()
    }

    private fun emit() {
        _tasks.value = registry.values.toList()
    }
}
