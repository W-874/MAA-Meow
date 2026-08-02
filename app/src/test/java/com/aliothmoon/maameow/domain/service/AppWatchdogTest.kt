package com.aliothmoon.maameow.domain.service

import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.remote.AppAliveStatus
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppWatchdogTest {

    @Test
    fun nullDisplayStatusDoesNotIntervene() = runBlocking {
        val fixture = fixture()
        fixture.checker.onDisplay = null

        repeat(5) {
            fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
            fixture.advance(10_000L)
        }

        assertTrue(fixture.checker.moveCalls.isEmpty())
        assertTrue(fixture.events.isEmpty())
        fixture.close()
    }

    @Test
    fun repinWaitsForGracePeriodToExpire() = runBlocking {
        val fixture = fixture(delaySec = 5)
        fixture.checker.onDisplay = false

        fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        fixture.advance(4_999L)
        fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        assertTrue(fixture.checker.moveCalls.isEmpty())

        fixture.advance(1L)
        fixture.checker.moveResult = true
        fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)

        assertEquals(listOf(GAME_PACKAGE), fixture.checker.moveCalls)
        fixture.close()
    }

    @Test
    fun successfulRepinResetsGraceForNextDrift() = runBlocking {
        val fixture = fixture(delaySec = 5)
        fixture.checker.onDisplay = false
        fixture.checker.moveResult = true

        fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        fixture.advance(5_000L)
        fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        assertEquals(1, fixture.checker.moveCalls.size)

        fixture.advance(5_000L)
        fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        assertEquals(1, fixture.checker.moveCalls.size)

        fixture.advance(5_000L)
        fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        assertEquals(2, fixture.checker.moveCalls.size)
        fixture.close()
    }

    @Test
    fun repinFailuresCapNotifyOnceAndResetOnReturn() = runBlocking {
        val fixture = fixture(delaySec = 5)
        fixture.checker.onDisplay = false

        fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        repeat(AppWatchdog.MAX_REPIN_ATTEMPTS) {
            fixture.advance(5_000L)
            fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        }
        yield()

        assertEquals(AppWatchdog.MAX_REPIN_ATTEMPTS, fixture.checker.moveCalls.size)
        assertEquals(listOf(GAME_PACKAGE), fixture.events)

        fixture.checker.onDisplay = true
        fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        fixture.checker.onDisplay = false
        fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        repeat(AppWatchdog.MAX_REPIN_ATTEMPTS) {
            fixture.advance(5_000L)
            fixture.watchdog.checkDisplayPinned(GAME_PACKAGE)
        }
        yield()

        assertEquals(AppWatchdog.MAX_REPIN_ATTEMPTS * 2, fixture.checker.moveCalls.size)
        assertEquals(listOf(GAME_PACKAGE, GAME_PACKAGE), fixture.events)
        fixture.close()
    }

    private fun fixture(enabled: Boolean = true, delaySec: Int = 5): Fixture {
        val settings = mockk<AppSettingsManager>()
        every { settings.driftAutoRepinEnabled } returns MutableStateFlow(enabled)
        every { settings.driftAutoRepinDelaySec } returns MutableStateFlow(delaySec)
        val checker = FakeAppAliveChecker()
        val watchdog = AppWatchdog(mockk<TaskChainState>(), checker, settings)
        val fixture = Fixture(checker, watchdog)
        watchdog.clock = { fixture.now }
        fixture.scope.launch { watchdog.displayDriftEvent.collect { fixture.events += it } }
        return fixture
    }

    private class Fixture(
        val checker: FakeAppAliveChecker,
        val watchdog: AppWatchdog,
    ) {
        var now = 100_000L
        val events = mutableListOf<String>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)

        fun advance(ms: Long) {
            now += ms
        }

        fun close() = scope.cancel()
    }

    private class FakeAppAliveChecker : AppAliveChecker {
        var onDisplay: Boolean? = true
        var moveResult: Boolean? = false
        val moveCalls = mutableListOf<String>()

        override suspend fun isAppAlive(packageName: String): Int = AppAliveStatus.ALIVE

        override suspend fun isAppOnBackgroundDisplay(packageName: String): Boolean? = onDisplay

        override suspend fun moveAppToBackgroundDisplay(packageName: String): Boolean? {
            moveCalls += packageName
            return moveResult
        }
    }

    private companion object {
        const val GAME_PACKAGE = "com.hypergryph.arknights"
    }
}
