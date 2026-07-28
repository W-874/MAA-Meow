package com.aliothmoon.maameow.maa.callback

import org.junit.Assert.assertEquals
import org.junit.Test

class TaskChainStatusTrackerTest {

    @Test
    fun groupedProgressCountsExpandedNodeOnce() {
        val tasks = listOf(
            TaskRunInfo(1, "StartUp", TaskRunStatus.COMPLETED, "wake-up"),
            TaskRunInfo(2, "OperBox", TaskRunStatus.COMPLETED, "user-data"),
            TaskRunInfo(3, "Depot", TaskRunStatus.IN_PROGRESS, "user-data"),
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
            TaskRunInfo(1, "OperBox", TaskRunStatus.COMPLETED, "user-data"),
            TaskRunInfo(2, "Depot", TaskRunStatus.PENDING, "user-data"),
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
