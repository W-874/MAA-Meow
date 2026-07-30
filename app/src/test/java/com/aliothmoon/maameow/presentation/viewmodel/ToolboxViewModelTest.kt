package com.aliothmoon.maameow.presentation.viewmodel

import com.aliothmoon.maameow.domain.usecase.TaskStartContext
import com.aliothmoon.maameow.domain.usecase.TaskStartMode
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.utils.i18n.UiText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolboxViewModelTest {

    @Test
    fun `start request keeps its original tab and gacha mode`() {
        val request = ToolboxStartRequest(
            tab = ToolboxTab.GACHA,
            gachaOnce = false,
            context = TaskStartContext(TaskStartMode.MANUAL),
        )

        assertEquals(ToolboxTab.GACHA, request.tab)
        assertFalse(request.gachaOnce)
        assertEquals(TaskStartMode.MANUAL, request.context.mode)
    }

    @Test
    fun `clearing gacha transient state preserves accepted disclaimer`() {
        val state = ToolboxUiState(
            gachaDisclaimerAccepted = true,
            gachaOnce = false,
            statusMessage = UiText.Empty,
        )

        val cleared = state.clearGachaTransientState()

        assertTrue(cleared.gachaDisclaimerAccepted)
        assertTrue(cleared.gachaOnce)
        assertEquals(UiText.Empty, cleared.statusMessage)
    }

    @Test
    fun `stop is unavailable until toolbox execution is running`() {
        assertFalse(canStopToolboxExecution(MaaExecutionState.STARTING))
        assertTrue(canStopToolboxExecution(MaaExecutionState.RUNNING))
    }
}
