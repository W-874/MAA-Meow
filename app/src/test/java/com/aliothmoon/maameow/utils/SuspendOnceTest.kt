package com.aliothmoon.maameow.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class SuspendOnceTest {

    @Test
    fun concurrentCallersWaitForSameAttempt() = runBlocking {
        val once = SuspendOnce()
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val calls = AtomicInteger()

        val first = async {
            once.run {
                calls.incrementAndGet()
                entered.complete(Unit)
                release.await()
            }
        }
        entered.await()
        val second = async { once.run { calls.incrementAndGet() } }
        yield()

        assertFalse(second.isCompleted)
        assertEquals(1, calls.get())

        release.complete(Unit)
        first.await()
        second.await()
        once.run { calls.incrementAndGet() }
        assertEquals(1, calls.get())
    }

    @Test
    fun failedAttemptCanBeRetried() = runBlocking {
        val once = SuspendOnce()
        val calls = AtomicInteger()

        runCatching {
            once.run {
                calls.incrementAndGet()
                error("first attempt")
            }
        }
        once.run { calls.incrementAndGet() }

        assertEquals(2, calls.get())
    }
}
