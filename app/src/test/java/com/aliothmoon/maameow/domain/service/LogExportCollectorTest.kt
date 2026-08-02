package com.aliothmoon.maameow.domain.service

import com.aliothmoon.maameow.constant.LogConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.RandomAccessFile

/**
 * 锁住日志导出挑选契约：
 * - gui / schedule / error_logs / crash_logs：仅近 [LogConfig.EXPORT_ROLLING_LOG_DAYS] 天
 * - asst / logcat / screenshots / 其它：完整入选
 * - export/ 排除
 */
class LogExportCollectorTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var debugDir: File

    private val dayMs = 24L * 60 * 60 * 1000

    @Before
    fun setUp() {
        debugDir = tempFolder.newFolder("debug")
    }

    private fun fileAt(
        relativePath: String,
        sizeBytes: Long = 16,
        lastModified: Long = System.currentTimeMillis(),
    ): File {
        val file = File(debugDir, relativePath)
        file.parentFile?.mkdirs()
        if (sizeBytes <= 0L) {
            file.writeText("")
        } else {
            RandomAccessFile(file, "rw").use { randomAccessFile ->
                randomAccessFile.setLength(sizeBytes)
            }
        }
        file.setLastModified(lastModified)
        return file
    }

    @Test
    fun select_rollingDirs_onlyKeepRecentDays() {
        val now = System.currentTimeMillis()
        val recentGui = fileAt("gui/meow_recent.log", lastModified = now - dayMs)
        val oldGui = fileAt(
            "gui/meow_old.log",
            lastModified = now - (LogConfig.EXPORT_ROLLING_LOG_DAYS + 2) * dayMs,
        )
        val recentErr = fileAt("error_logs/error.log", lastModified = now - 2 * dayMs)
        val oldCrash = fileAt(
            "crash_logs/crash_1.log",
            lastModified = now - (LogConfig.EXPORT_ROLLING_LOG_DAYS + 1) * dayMs,
        )
        val recentSched = fileAt("schedule/s.log", lastModified = now)

        val selected = LogExportCollector.select(
            listOf(recentGui, oldGui, recentErr, oldCrash, recentSched),
        )

        assertEquals(setOf(recentGui, recentErr, recentSched), selected.toSet())
    }

    @Test
    fun select_rollingDirs_noSizeOrCountCap() {
        val now = System.currentTimeMillis()
        val big = fileAt("gui/big.log", sizeBytes = 20L * 1024 * 1024, lastModified = now)
        val many = (1..60).map { index ->
            fileAt("gui/meow_$index.log", sizeBytes = 8, lastModified = now - index)
        }

        val selected = LogExportCollector.select(many + big)

        assertEquals(61, selected.size)
        assertTrue(selected.contains(big))
    }

    @Test
    fun select_asstAndLogcatAndScreenshots_fullNoAgeOrSizeCap() {
        val now = System.currentTimeMillis()
        val asst = fileAt("asst.log", sizeBytes = 80L * 1024 * 1024)
        val backup = fileAt("asst.bak.log", sizeBytes = 60L * 1024 * 1024)
        val logcat = fileAt(
            "logcat/core/c.log",
            sizeBytes = 30L * 1024 * 1024,
            lastModified = now - 30 * dayMs,
        )
        val oldScreenshot = fileAt(
            "screenshots/old.png",
            sizeBytes = 2L * 1024 * 1024,
            lastModified = now - 30 * dayMs,
        )

        val selected = LogExportCollector.select(listOf(asst, backup, logcat, oldScreenshot))

        assertEquals(setOf(asst, backup, logcat, oldScreenshot), selected.toSet())
    }

    @Test
    fun select_debugRootMisc_fullPack() {
        val now = System.currentTimeMillis()
        val files = (1..40).map { index ->
            fileAt("misc_$index.txt", sizeBytes = 10, lastModified = now + index)
        }

        assertEquals(40, LogExportCollector.select(files).size)
    }

    @Test
    fun collect_skipsExportDirAndWalksTree() {
        val asst = fileAt("asst.log", sizeBytes = 50)
        fileAt("${LogExportCollector.EXPORT_DIR_NAME}/maa_logs_old.zip", sizeBytes = 200)
        val gui = fileAt("gui/meow_log_1.log", sizeBytes = 30)

        val collected = LogExportCollector.collect(debugDir)

        assertEquals(setOf(asst, gui), collected.toSet())
    }

    @Test
    fun collect_emptyWhenDebugMissing() {
        val missing = File(tempFolder.root, "no_such_debug")
        assertTrue(LogExportCollector.collect(missing).isEmpty())
    }
}
