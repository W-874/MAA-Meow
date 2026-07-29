package com.aliothmoon.maameow.utils

import android.os.Build
import android.os.Trace
import com.aliothmoon.maameow.BuildConfig
import java.util.concurrent.atomic.AtomicInteger

object PerformanceTrace {
    private val nextCookie = AtomicInteger(1)

    inline fun <T> section(name: String, block: () -> T): T {
        if (!BuildConfig.BENCHMARK_BUILD) return block()
        Trace.beginSection(name)
        return try {
            block()
        } finally {
            Trace.endSection()
        }
    }

    fun beginAsync(name: String): Int {
        if (!BuildConfig.BENCHMARK_BUILD) return 0
        val cookie = nextCookie.getAndIncrement()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.beginAsyncSection(name, cookie)
        }
        return cookie
    }

    fun endAsync(name: String, cookie: Int) {
        if (!BuildConfig.BENCHMARK_BUILD) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            Trace.endAsyncSection(name, cookie)
        }
    }
}
