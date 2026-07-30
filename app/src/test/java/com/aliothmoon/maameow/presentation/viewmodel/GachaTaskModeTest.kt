package com.aliothmoon.maameow.presentation.viewmodel

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class GachaTaskModeTest {

    @Test
    fun `once mode uses MaaCore one-pull task`() {
        assertEquals("GachaOnce", gachaTaskName(once = true))
    }

    @Test
    fun `ten-pull mode uses MaaCore ten-pull task`() {
        assertEquals("GachaTenTimes", gachaTaskName(once = false))
    }

    @Test
    fun `foreground mode does not expose gacha`() {
        assertFalse(ToolboxTab.visibleFor(com.aliothmoon.maameow.domain.models.RunMode.FOREGROUND)
            .contains(ToolboxTab.GACHA))
    }
}
