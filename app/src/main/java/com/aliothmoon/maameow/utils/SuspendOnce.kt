package com.aliothmoon.maameow.utils

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Shares one in-flight initialization and retries after a failed attempt. */
internal class SuspendOnce {
    private val mutex = Mutex()
    private var inFlight: CompletableDeferred<Unit>? = null
    private var completed = false

    suspend fun run(block: suspend () -> Unit) {
        var ownsAttempt = false
        val attempt = mutex.withLock {
            if (completed) return
            inFlight ?: CompletableDeferred<Unit>().also {
                inFlight = it
                ownsAttempt = true
            }
        }

        if (!ownsAttempt) {
            attempt.await()
            return
        }

        try {
            block()
            mutex.withLock {
                completed = true
                if (inFlight === attempt) inFlight = null
            }
            attempt.complete(Unit)
        } catch (error: Throwable) {
            mutex.withLock {
                if (inFlight === attempt) inFlight = null
            }
            attempt.completeExceptionally(error)
            throw error
        }
    }
}
