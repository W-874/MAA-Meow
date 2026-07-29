package com.aliothmoon.maameow.data.datasource

import android.content.Context
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.AssetArchiveDescriptor
import com.aliothmoon.maameow.data.model.AssetManifest
import com.aliothmoon.maameow.utils.JsonUtils
import com.aliothmoon.maameow.utils.i18n.LocalizedException
import com.aliothmoon.maameow.utils.i18n.uiTextOf
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.BufferedInputStream
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.InputStream
import java.security.MessageDigest
import java.util.HashSet
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream

class AssetExtractor(private val context: Context) {

    companion object {
        const val CORE_ARCHIVE_ID = "core"
        private const val MANIFEST_FILE_NAME = "MaaSync/asset_manifest.json"
        private const val BUFFER_SIZE = 128 * 1024
        private const val PROGRESS_INTERVAL_MS = 100L
        private const val FILE_OPEN_ATTEMPTS = 3
        private const val FILE_OPEN_RETRY_DELAY_MS = 40L
    }

    class ExtractFailedException(
        failedFile: String,
        attempts: Int,
        cause: Throwable,
    ) : LocalizedException(uiTextOf(R.string.resource_extract_failed, failedFile, attempts), cause)

    data class ExtractProgress(
        val extractedCount: Int,
        val totalCount: Int,
        val currentFile: String,
    )

    private val json = JsonUtils.common
    private val extractionDispatcher = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "maa-resource-extractor").apply { isDaemon = true }
    }.asCoroutineDispatcher()

    suspend fun extractArchive(
        archiveId: String,
        destDir: File,
        onProgress: (ExtractProgress) -> Unit,
    ): Result<Int> = withContext(extractionDispatcher) {
        runCatching {
            val descriptor = loadAssetManifest().archives.firstOrNull { it.id == archiveId }
                ?: error("Assets 归档不存在: $archiveId")
            val expectedDestination = if (archiveId == CORE_ARCHIVE_ID) "" else "global/$archiveId"
            check(descriptor.destination == expectedDestination) {
                "归档目标目录不匹配: ${descriptor.destination}/$expectedDestination"
            }
            destDir.mkdirs()
            extractVerifiedArchive(
                openArchive = { context.assets.open(descriptor.assetPath) },
                descriptor = descriptor,
                destDir = destDir,
                onProgress = onProgress,
            )
        }.onFailure { Timber.e(it, "资源归档提取失败: $archiveId") }
    }

    internal fun extractVerifiedArchive(
        openArchive: () -> InputStream,
        descriptor: AssetArchiveDescriptor,
        destDir: File,
        onProgress: (ExtractProgress) -> Unit = {},
    ): Int {
        require(descriptor.fileCount >= 0) { "归档文件数非法" }
        require(descriptor.uncompressedSize >= 0L) { "归档展开大小非法" }
        require(descriptor.sha256.matches(Regex("[0-9a-f]{64}"))) { "归档摘要格式非法" }
        require(descriptor.versionSha256.matches(Regex("[0-9a-f]{64}"))) { "资源版本摘要格式非法" }
        check(openArchive().use(::sha256) == descriptor.sha256) { "归档摘要校验失败" }

        val extractedFiles = HashSet<String>(descriptor.fileCount)
        val destinationRoot = destDir.canonicalFile
        var extractedCount = 0
        var extractedBytes = 0L
        var lastProgressAt = 0L
        var lastPercent = -1
        val buffer = ByteArray(BUFFER_SIZE)

        onProgress(ExtractProgress(0, descriptor.fileCount, ""))
        openArchive().use { rawInput ->
            ZipInputStream(BufferedInputStream(rawInput, BUFFER_SIZE)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    if (entry.isDirectory) continue
                    val relativePath = entry.name.replace('\\', '/')
                    val pathSegments = relativePath.split('/')
                    require(
                        relativePath.isNotBlank() &&
                            !relativePath.startsWith('/') &&
                            pathSegments.none { it.isBlank() || it == "." || it == ".." } &&
                            extractedFiles.add(relativePath)
                    ) {
                        "归档包含非法路径或重复文件: $relativePath"
                    }
                    val targetFile = File(destinationRoot, relativePath).canonicalFile
                    require(targetFile.path.startsWith(destinationRoot.path + File.separator)) {
                        "非法资源路径: $relativePath"
                    }
                    try {
                        openTargetFile(targetFile, relativePath).use { output ->
                            while (true) {
                                val count = zip.read(buffer)
                                if (count < 0) break
                                output.write(buffer, 0, count)
                                extractedBytes += count
                                check(extractedBytes <= descriptor.uncompressedSize) {
                                    "归档展开大小超过清单"
                                }
                            }
                        }
                    } catch (error: Exception) {
                        targetFile.delete()
                        throw if (error is ExtractFailedException) {
                            error
                        } else {
                            ExtractFailedException(relativePath, 1, error)
                        }
                    }
                    extractedCount++
                    check(extractedCount <= descriptor.fileCount) { "归档文件数超过清单" }
                    val now = System.currentTimeMillis()
                    val percent = if (descriptor.fileCount == 0) 100
                    else extractedCount * 100 / descriptor.fileCount
                    if (now - lastProgressAt >= PROGRESS_INTERVAL_MS || percent != lastPercent) {
                        onProgress(
                            ExtractProgress(extractedCount, descriptor.fileCount, relativePath)
                        )
                        lastProgressAt = now
                        lastPercent = percent
                    }
                }
            }
        }

        check(extractedCount == descriptor.fileCount) {
            "归档文件数不匹配: $extractedCount/${descriptor.fileCount}"
        }
        check(extractedBytes == descriptor.uncompressedSize) {
            "归档展开大小不匹配: $extractedBytes/${descriptor.uncompressedSize}"
        }
        val missingFile = extractedFiles.firstOrNull { relativePath ->
            !File(destinationRoot, relativePath).isFile
        }
        check(missingFile == null) { "归档文件提取后缺失: $missingFile" }
        if (descriptor.id == CORE_ARCHIVE_ID) {
            val versionFile = File(destDir, "version.json")
            check(versionFile.isFile && versionFile.sha256() == descriptor.versionSha256) {
                "核心资源版本校验失败"
            }
        }
        File(destDir, ".maa-archive-complete").writeText(descriptor.sha256)
        onProgress(ExtractProgress(extractedCount, descriptor.fileCount, ""))
        return extractedCount
    }

    private fun openTargetFile(targetFile: File, relativePath: String): FileOutputStream {
        var lastError: FileNotFoundException? = null
        repeat(FILE_OPEN_ATTEMPTS) { attempt ->
            try {
                val parent = requireNotNull(targetFile.parentFile)
                if (!parent.isDirectory && !parent.mkdirs() && !parent.isDirectory) {
                    throw FileNotFoundException("无法创建资源目录: ${parent.absolutePath}")
                }
                return FileOutputStream(targetFile)
            } catch (error: FileNotFoundException) {
                lastError = error
                if (attempt < FILE_OPEN_ATTEMPTS - 1) {
                    Thread.sleep(FILE_OPEN_RETRY_DELAY_MS * (attempt + 1))
                }
            }
        }
        throw ExtractFailedException(
            relativePath,
            FILE_OPEN_ATTEMPTS,
            requireNotNull(lastError),
        )
    }

    private fun loadAssetManifest(): AssetManifest {
        return context.assets.open(MANIFEST_FILE_NAME).use { input ->
            json.decodeFromString(input.bufferedReader().readText())
        }
    }

    private fun File.sha256(): String = inputStream().buffered().use(::sha256)

    private fun sha256(input: InputStream): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(BUFFER_SIZE)
        while (true) {
            val count = input.read(buffer)
            if (count < 0) break
            digest.update(buffer, 0, count)
        }
        return digest.digest().toHex()
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }
}
