package com.aliothmoon.maameow.maa.callback

import com.aliothmoon.maameow.maa.task.TaskSlot
import org.junit.Assert.assertEquals
import org.junit.Test

class TaskChainStatusTrackerTest {

    @Test
    fun groupedProgressCountsExpandedNodeOnce() {
        val tasks = listOf(
            TaskRunInfo(1, "StartUp", TaskRunStatus.COMPLETED, TaskSlot("wake-up")),
            TaskRunInfo(2, "OperBox", TaskRunStatus.COMPLETED, TaskSlot("user-data", 0)),
            TaskRunInfo(3, "Depot", TaskRunStatus.IN_PROGRESS, TaskSlot("user-data", 1)),
        )

        val grouped = tasks.groupByVisibleTask()

        assertEquals(2, grouped.size)
        assertEquals(TaskRunStatus.COMPLETED, grouped[0].status)
        assertEquals(TaskRunStatus.IN_PROGRESS, grouped[1].status)
        assertEquals("Depot", grouped[1].taskChain)
    }

    @Test
    fun groupedProgressRemainsActiveBetweenExpandedTasks() {
        val tasks = listOf(
            TaskRunInfo(1, "OperBox", TaskRunStatus.COMPLETED, TaskSlot("user-data", 0)),
            TaskRunInfo(2, "Depot", TaskRunStatus.PENDING, TaskSlot("user-data", 1)),
        )

        assertEquals(TaskRunStatus.IN_PROGRESS, tasks.groupByVisibleTask().single().status)
    }

    @Test
    fun groupedProgressKeepsTasksWithoutNodeIdsSeparate() {
        val tasks = listOf(
            TaskRunInfo(1, "Copilot", TaskRunStatus.COMPLETED),
            TaskRunInfo(2, "Copilot", TaskRunStatus.PENDING),
        )

        assertEquals(2, tasks.groupByVisibleTask().size)
    }
}
