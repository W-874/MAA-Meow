package com.aliothmoon.maameow.data.datasource

import android.content.Context
import com.aliothmoon.maameow.data.model.AssetArchiveDescriptor
import io.mockk.mockk
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.security.MessageDigest
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

class AssetExtractorTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private val extractor = AssetExtractor(mockk<Context>())

    @Test
    fun extractsValidCoreArchiveAndPublishesFinalProgress() {
        val version = "{\"version\":1}".encodeToByteArray()
        val task = "task".encodeToByteArray()
        val archive = zipOf("version.json" to version, "resource/tasks.json" to task)
        val progress = mutableListOf<AssetExtractor.ExtractProgress>()
        val destination = temporaryFolder.newFolder("core")

        val count = extractor.extractVerifiedArchive(
            openArchive = { ByteArrayInputStream(archive) },
            descriptor = descriptor(
                id = AssetExtractor.CORE_ARCHIVE_ID,
                archive = archive,
                files = listOf(version, task),
                versionSha256 = version.sha256(),
            ),
            destDir = destination,
            onProgress = progress::add,
        )

        assertEquals(2, count)
        assertArrayEquals(version, destination.resolve("version.json").readBytes())
        assertArrayEquals(task, destination.resolve("resource/tasks.json").readBytes())
        assertEquals(2, progress.last().extractedCount)
        assertEquals("", progress.last().currentFile)
        assertTrue(destination.resolve(".maa-archive-complete").isFile)
    }

    @Test
    fun extractsValidClientArchiveWithoutCoreVersionFile() {
        val payload = "client resource".encodeToByteArray()
        val archive = zipOf("resource/client.json" to payload)
        val destination = temporaryFolder.newFolder("client")

        extractor.extractVerifiedArchive(
            openArchive = { ByteArrayInputStream(archive) },
            descriptor = descriptor("YoStarEN", archive, listOf(payload)),
            destDir = destination,
        )

        assertArrayEquals(payload, destination.resolve("resource/client.json").readBytes())
    }

    @Test
    fun rejectsCorruptedArchiveBeforeWritingFiles() {
        val payload = "payload".encodeToByteArray()
        val archive = zipOf("resource/file" to payload)
        val destination = temporaryFolder.newFolder("digest")
        val descriptor = descriptor("YoStarEN", archive, listOf(payload)).copy(sha256 = ZERO_SHA)

        assertFails { extract(archive, descriptor, destination) }

        assertTrue(destination.listFiles().orEmpty().isEmpty())
    }

    @Test
    fun rejectsPathTraversal() {
        val payload = "escape".encodeToByteArray()
        val archive = zipOf("../escape" to payload)
        val destination = temporaryFolder.newFolder("traversal")
        val escaped = destination.parentFile.resolve("escape")

        assertFails { extract(archive, descriptor("YoStarEN", archive, listOf(payload)), destination) }

        assertFalse(escaped.exists())
    }

    @Test
    fun rejectsDuplicateEntries() {
        val archive = storedZipWithDuplicateEntries("same", "first".encodeToByteArray(), "second".encodeToByteArray())
        val destination = temporaryFolder.newFolder("duplicate")

        assertFails {
            extract(
                archive,
                descriptor("YoStarEN", archive, listOf("first".encodeToByteArray(), "second".encodeToByteArray())),
                destination,
            )
        }
    }

    @Test
    fun rejectsFileCountMismatch() {
        val payload = "payload".encodeToByteArray()
        val archive = zipOf("one" to payload)
        val destination = temporaryFolder.newFolder("count")
        val descriptor = descriptor("YoStarEN", archive, listOf(payload)).copy(fileCount = 2)

        assertFails { extract(archive, descriptor, destination) }
    }

    @Test
    fun rejectsExpandedSizeMismatch() {
        val payload = "payload".encodeToByteArray()
        val archive = zipOf("one" to payload)
        val destination = temporaryFolder.newFolder("size")
        val descriptor = descriptor("YoStarEN", archive, listOf(payload)).copy(
            uncompressedSize = payload.size.toLong() + 1,
        )

        assertFails { extract(archive, descriptor, destination) }
    }

    @Test
    fun rejectsFileMissingAfterExtraction() {
        val payload = "payload".encodeToByteArray()
        val archive = zipOf("one" to payload)
        val destination = temporaryFolder.newFolder("missing")

        assertFails {
            extractor.extractVerifiedArchive(
                openArchive = { ByteArrayInputStream(archive) },
                descriptor = descriptor("YoStarEN", archive, listOf(payload)),
                destDir = destination,
                onProgress = { progress ->
                    if (progress.currentFile == "one") {
                        destination.resolve("one").delete()
                    }
                },
            )
        }
    }

    @Test
    fun rejectsCoreVersionMismatch() {
        val version = "version".encodeToByteArray()
        val archive = zipOf("version.json" to version)
        val destination = temporaryFolder.newFolder("version")
        val descriptor = descriptor(
            AssetExtractor.CORE_ARCHIVE_ID,
            archive,
            listOf(version),
            versionSha256 = ZERO_SHA,
        )

        assertFails { extract(archive, descriptor, destination) }
    }

    private fun extract(
        archive: ByteArray,
        descriptor: AssetArchiveDescriptor,
        destination: java.io.File,
    ) {
        extractor.extractVerifiedArchive(
            openArchive = { ByteArrayInputStream(archive) },
            descriptor = descriptor,
            destDir = destination,
        )
    }

    private fun descriptor(
        id: String,
        archive: ByteArray,
        files: List<ByteArray>,
        versionSha256: String = ZERO_SHA,
    ) = AssetArchiveDescriptor(
        id = id,
        assetPath = "unused.zip",
        destination = if (id == AssetExtractor.CORE_ARCHIVE_ID) "" else "global/$id",
        sha256 = archive.sha256(),
        fileCount = files.size,
        uncompressedSize = files.sumOf { it.size.toLong() },
        versionSha256 = versionSha256,
    )

    private fun zipOf(vararg files: Pair<String, ByteArray>): ByteArray {
        return ByteArrayOutputStream().also { bytes ->
            ZipOutputStream(bytes).use { zip ->
                files.forEach { (name, content) ->
                    zip.putNextEntry(ZipEntry(name))
                    zip.write(content)
                    zip.closeEntry()
                }
            }
        }.toByteArray()
    }

    private fun storedZipWithDuplicateEntries(
        name: String,
        first: ByteArray,
        second: ByteArray,
    ): ByteArray = ByteArrayOutputStream().also { output ->
        listOf(first, second).forEach { content ->
            val nameBytes = name.encodeToByteArray()
            val crc = CRC32().apply { update(content) }.value
            output.writeLittleEndian(0x04034b50, 4)
            output.writeLittleEndian(20, 2)
            output.writeLittleEndian(0, 2)
            output.writeLittleEndian(0, 2)
            output.writeLittleEndian(0, 2)
            output.writeLittleEndian(0, 2)
            output.writeLittleEndian(crc, 4)
            output.writeLittleEndian(content.size.toLong(), 4)
            output.writeLittleEndian(content.size.toLong(), 4)
            output.writeLittleEndian(nameBytes.size.toLong(), 2)
            output.writeLittleEndian(0, 2)
            output.write(nameBytes)
            output.write(content)
        }
    }.toByteArray()

    private fun ByteArrayOutputStream.writeLittleEndian(value: Long, byteCount: Int) {
        repeat(byteCount) { offset -> write((value ushr (offset * 8)).toInt() and 0xff) }
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(it) }

    private fun assertFails(block: () -> Unit) {
        var failed = false
        try {
            block()
        } catch (_: Exception) {
            failed = true
        }
        assertTrue("Expected extraction to fail", failed)
    }

    private companion object {
        const val ZERO_SHA = "0000000000000000000000000000000000000000000000000000000000000000"
    }
}
