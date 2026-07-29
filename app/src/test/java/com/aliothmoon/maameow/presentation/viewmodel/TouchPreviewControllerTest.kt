package com.aliothmoon.maameow.presentation.viewmodel

import android.view.MotionEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchPreviewControllerTest {

    @Test
    fun multipleMovesPublishOnlyLatestPositionPerFrame() {
        val fixture = fixture()

        fixture.controller.acceptTouchEvent(1, 1, MotionEvent.ACTION_MOVE)
        fixture.controller.acceptTouchEvent(2, 2, MotionEvent.ACTION_MOVE)
        fixture.controller.acceptTouchEvent(3, 3, MotionEvent.ACTION_MOVE)

        assertEquals(1, fixture.scheduler.pendingCount)
        assertTrue(fixture.controller.markers.value.isEmpty())

        fixture.scheduler.runFrame()

        assertEquals(listOf(3 to 3), fixture.controller.markers.value.map { it.x to it.y })
        fixture.close()
    }

    @Test
    fun upFlushesPendingMoveBeforeUpAndStaleFrameDoesNothing() {
        val fixture = fixture()

        fixture.controller.acceptTouchEvent(1, 1, MotionEvent.ACTION_DOWN)
        fixture.controller.acceptTouchEvent(2, 2, MotionEvent.ACTION_MOVE)
        fixture.controller.acceptTouchEvent(3, 3, MotionEvent.ACTION_UP)

        assertEquals(
            listOf(MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP),
            fixture.controller.markers.value.map { it.action },
        )

        fixture.scheduler.runFrame()

        assertEquals(3, fixture.controller.markers.value.size)
        fixture.close()
    }

    @Test
    fun unsupportedEventsDoNotScheduleFrames() {
        val fixture = fixture()

        fixture.controller.acceptTouchEvent(1, 1, MotionEvent.ACTION_CANCEL)

        assertEquals(0, fixture.scheduler.pendingCount)
        assertTrue(fixture.controller.markers.value.isEmpty())
        fixture.close()
    }

    private fun fixture(): Fixture {
        val scheduler = FakeFrameScheduler()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val controller = TouchPreviewController(
            scope = scope,
            frameScheduler = scheduler,
            elapsedRealtime = { 1_000L },
        )
        return Fixture(controller, scheduler, scope)
    }

    private data class Fixture(
        val controller: TouchPreviewController,
        val scheduler: FakeFrameScheduler,
        val scope: CoroutineScope,
    ) {
        fun close() = scope.cancel()
    }

    private class FakeFrameScheduler : TouchFrameScheduler {
        private val callbacks = ArrayDeque<() -> Unit>()
        val pendingCount: Int get() = callbacks.size

        override fun schedule(callback: () -> Unit) {
            callbacks += callback
        }

        fun runFrame() {
            callbacks.removeFirst()()
        }
    }
}
