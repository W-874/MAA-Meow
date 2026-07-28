package com.aliothmoon.maameow.data.config

import android.content.Context
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files

class MaaPathConfigTest {

    @Test
    fun `resource remains ready after app upgrade`() {
        val externalFilesDir = Files.createTempDirectory("maa-path-config-test").toFile()
        try {
            val context = mockk<Context> {
                every { getExternalFilesDir(null) } returns externalFilesDir
            }
            val pathConfig = MaaPathConfig(context)
            val versionFile = File(pathConfig.resourceDir, "version.json")
            versionFile.parentFile?.mkdirs()
            versionFile.writeText("{}")
            File(pathConfig.rootDir, ".version").writeText("1")

            assertTrue(pathConfig.isResourceReady)
        } finally {
            externalFilesDir.deleteRecursively()
        }
    }

    @Test
    fun `resource is not ready without version file`() {
        val externalFilesDir = Files.createTempDirectory("maa-path-config-test").toFile()
        try {
            val context = mockk<Context> {
                every { getExternalFilesDir(null) } returns externalFilesDir
            }

            assertFalse(MaaPathConfig(context).isResourceReady)
        } finally {
            externalFilesDir.deleteRecursively()
        }
    }
}
