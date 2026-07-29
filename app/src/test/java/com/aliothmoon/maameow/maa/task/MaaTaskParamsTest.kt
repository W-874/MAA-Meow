package com.aliothmoon.maameow.maa.task

import org.junit.Assert.assertEquals
import org.junit.Test

class MaaTaskParamsTest {

    @Test
    fun visibleTaskCountGroupsParamsFromSameNode() {
        val tasks = listOf(
            MaaTaskParams(MaaTaskType.START_UP, "{}", slot = TaskSlot("wake-up")),
            MaaTaskParams(MaaTaskType.OPER_BOX, "{}", slot = TaskSlot("user-data", 0)),
            MaaTaskParams(MaaTaskType.DEPOT, "{}", slot = TaskSlot("user-data", 1)),
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
            MaaTaskParams(MaaTaskType.START_UP, "{}", slot = TaskSlot("wake-up")),
            MaaTaskParams(MaaTaskType.COPILOT, "{}"),
        )

        assertEquals(2, tasks.visibleTaskCount())
    }
}
