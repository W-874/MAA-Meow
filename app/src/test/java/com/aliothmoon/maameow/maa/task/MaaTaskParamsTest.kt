package com.aliothmoon.maameow.maa.task

import org.junit.Assert.assertEquals
import org.junit.Test

class MaaTaskParamsTest {

    @Test
    fun visibleTaskCountGroupsParamsFromSameNode() {
        val tasks = listOf(
            MaaTaskParams(MaaTaskType.START_UP, "{}", nodeId = "wake-up"),
            MaaTaskParams(MaaTaskType.OPER_BOX, "{}", nodeId = "user-data"),
            MaaTaskParams(MaaTaskType.DEPOT, "{}", nodeId = "user-data"),
        )

        assertEquals(2, tasks.visibleTaskCount())
    }

    @Test
    fun visibleTaskCountFallsBackToCoreTaskCountWithoutNodes() {
        val tasks = listOf(
            MaaTaskParams(MaaTaskType.COPILOT, "{}"),
            MaaTaskParams(MaaTaskType.COPILOT, "{}"),
        )

        assertEquals(2, tasks.visibleTaskCount())
    }

    @Test
    fun visibleTaskCountKeepsUnassociatedTasksSeparate() {
        val tasks = listOf(
            MaaTaskParams(MaaTaskType.START_UP, "{}", nodeId = "wake-up"),
            MaaTaskParams(MaaTaskType.COPILOT, "{}"),
        )

        assertEquals(2, tasks.visibleTaskCount())
    }
}
