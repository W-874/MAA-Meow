package com.aliothmoon.maameow.domain.service

import android.content.Context
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.MaaFiles.ASSET_DIR_NAME
import com.aliothmoon.maameow.constant.MaaFiles.OVERRIDES_ASSET_TASKS
import com.aliothmoon.maameow.constant.MaaFiles.VERSION_FILE
import com.aliothmoon.maameow.data.config.MaaPathConfig
import com.aliothmoon.maameow.data.datasource.AssetExtractor
import com.aliothmoon.maameow.domain.state.ResourceInitState
import com.aliothmoon.maameow.utils.i18n.LocalizedException
import com.aliothmoon.maameow.utils.i18n.uiTextDynamicOr
import com.aliothmoon.maameow.utils.PerformanceTrace
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File

class ResourceInitService(
    private val context: Context,
    private val assetExtractor: AssetExtractor,
    private val pathConfig: MaaPathConfig
) {
    private val _state = MutableStateFlow<ResourceInitState>(ResourceInitState.NotChecked)
    val state: StateFlow<ResourceInitState> = _state.asStateFlow()
    private val _clientStates = MutableStateFlow<Map<String, ResourceInitState>>(emptyMap())
    val clientStates: StateFlow<Map<String, ResourceInitState>> = _clientStates.asStateFlow()
    private val extractionMutex = Mutex()
    private val supportedGlobalClients = setOf("YoStarEN", "YoStarJP", "YoStarKR", "txwy")

    suspend fun ensureClientResources(clientType: String): Result<Unit> {
        if (clientType.isBlank() || clientType == "Official" || clientType == "Bilibili") {
            return Result.success(Unit)
        }
        require(clientType in supportedGlobalClients) { "Unsupported global client: $clientType" }
        val clientDir = File(pathConfig.resourceDir, "global/$clientType")
        if (File(clientDir, "resource").isDirectory) return Result.success(Unit)

        return extractionMutex.withLock {
            if (File(clientDir, "resource").isDirectory) return@withLock Result.success(Unit)
            val stagingDir = File(pathConfig.rootDir, ".resource-client-staging-$clientType")
            val trace = PerformanceTrace.beginAsync("ResourceInit.client.$clientType")
            try {
                withContext(Dispatchers.IO) {
                    if (stagingDir.exists() && !stagingDir.deleteRecursively()) {
                        error("无法清理客户端资源临时目录")
                    }
                    stagingDir.mkdirs()
                }
                updateClientState(clientType, ResourceInitState.Extracting(0, 0, ""))
                val result = assetExtractor.extractArchive(clientType, stagingDir) { progress ->
                    updateClientState(
                        clientType,
                        ResourceInitState.Extracting(
                            progress.extractedCount,
                            progress.totalCount,
                            progress.currentFile,
                        ),
                    )
                }
                result.getOrThrow()
                withContext(Dispatchers.IO) {
                    clientDir.parentFile?.mkdirs()
                    commitStagedDirectory(
                        stagingDir = stagingDir,
                        destinationDir = clientDir,
                        backupDir = File(pathConfig.rootDir, ".resource-client-backup-$clientType"),
                    )
                }
                updateClientState(clientType, ResourceInitState.Ready)
                Result.success(Unit)
            } catch (error: Exception) {
                Timber.e(error, "客户端资源初始化失败: $clientType")
                updateClientState(
                    clientType,
                    ResourceInitState.Failed(
                        uiTextDynamicOr(error.message, R.string.resource_init_error_copy_failed)
                    ),
                )
                Result.failure(error)
            } finally {
                withContext(Dispatchers.IO) { stagingDir.deleteRecursively() }
                PerformanceTrace.endAsync("ResourceInit.client.$clientType", trace)
            }
        }
    }

    private fun updateClientState(clientType: String, state: ResourceInitState) {
        _clientStates.value = _clientStates.value + (clientType to state)
    }

    suspend fun checkAndInit() {
        _state.value = ResourceInitState.Checking

        if (pathConfig.isResourceReady) {
            _state.value = ResourceInitState.Ready
            return
        }

        doExtractFromAssets()
    }

    suspend fun reInitialize() {
        doExtractFromAssets()
    }

    suspend fun doExtractFromAssets() = extractionMutex.withLock {
        _state.value = ResourceInitState.Extracting(0, 0, context.getString(R.string.resource_init_preparing))

        val stagingDir = File(pathConfig.rootDir, ".resource-staging")
        val trace = PerformanceTrace.beginAsync("ResourceInitService.extract")
        try {
            withContext(Dispatchers.IO) {
                pathConfig.ensureDirectories()
                if (stagingDir.exists() && !stagingDir.deleteRecursively()) {
                    throw IllegalStateException("无法清理资源临时目录: ${stagingDir.absolutePath}")
                }
                stagingDir.mkdirs()
            }

            val result = assetExtractor.extractArchive(
                archiveId = AssetExtractor.CORE_ARCHIVE_ID,
                destDir = stagingDir,
                onProgress = { progress ->
                    _state.value = ResourceInitState.Extracting(
                        extractedCount = progress.extractedCount,
                        totalCount = progress.totalCount,
                        currentFile = progress.currentFile
                    )
                }
            )

            val failure = result.exceptionOrNull()
            if (failure == null) {
                withContext(Dispatchers.IO) {
                    check(File(stagingDir, VERSION_FILE).isFile) {
                        "资源缺少 $VERSION_FILE"
                    }
                    commitStagedDirectory(
                        stagingDir = stagingDir,
                        destinationDir = File(pathConfig.resourceDir),
                        backupDir = File(pathConfig.rootDir, ".resource-backup"),
                        preserveRelativePaths = listOf("global"),
                    )
                }
                withContext(Dispatchers.IO) { doForceSyncOverridesTemplate() }
                Timber.i("资源初始化完成")
                _state.value = ResourceInitState.Ready
            } else {
                _state.value = ResourceInitState.Failed(
                    (failure as? LocalizedException)?.uiText
                        ?: uiTextDynamicOr(failure.message, R.string.resource_init_error_copy_failed)
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "资源初始化失败")
            _state.value = ResourceInitState.Failed(
                (e as? LocalizedException)?.uiText
                ?: uiTextDynamicOr(e.message, R.string.resource_init_error_unknown)
            )
        } finally {
            withContext(Dispatchers.IO) {
                if (stagingDir.exists()) {
                    stagingDir.deleteRecursively()
                }
            }
            PerformanceTrace.endAsync("ResourceInitService.extract", trace)
        }
    }

    private fun commitStagedDirectory(
        stagingDir: File,
        destinationDir: File,
        backupDir: File,
        preserveRelativePaths: List<String> = emptyList(),
    ) {
        if (backupDir.exists() && !backupDir.deleteRecursively()) {
            throw IllegalStateException("无法清理旧资源备份")
        }

        val hadExistingResource = destinationDir.exists()
        if (hadExistingResource && !destinationDir.renameTo(backupDir)) {
            throw IllegalStateException("无法保留旧资源")
        }

        val movedPreservedPaths = mutableListOf<String>()
        try {
            check(stagingDir.renameTo(destinationDir)) { "无法提交新资源" }
            preserveRelativePaths.forEach { relativePath ->
                val preservedSource = File(backupDir, relativePath)
                if (!preservedSource.exists()) return@forEach
                val preservedDestination = File(destinationDir, relativePath)
                check(!preservedDestination.exists()) { "新资源与保留目录冲突: $relativePath" }
                preservedDestination.parentFile?.mkdirs()
                check(preservedSource.renameTo(preservedDestination)) {
                    "无法保留旧资源目录: $relativePath"
                }
                movedPreservedPaths += relativePath
            }
            if (backupDir.exists() && !backupDir.deleteRecursively()) {
                Timber.w("旧资源备份清理失败: ${backupDir.absolutePath}")
            }
        } catch (e: Exception) {
            movedPreservedPaths.asReversed().forEach { relativePath ->
                val movedPath = File(destinationDir, relativePath)
                val originalPath = File(backupDir, relativePath)
                originalPath.parentFile?.mkdirs()
                if (movedPath.exists() && !movedPath.renameTo(originalPath)) {
                    Timber.e("恢复保留目录失败: $relativePath")
                }
            }
            if (destinationDir.exists() && !destinationDir.deleteRecursively()) {
                Timber.e("回滚时无法删除未完成的新资源")
            }
            if (backupDir.exists() && !backupDir.renameTo(destinationDir)) {
                Timber.e("回滚旧资源失败: ${backupDir.absolutePath}")
            }
            throw e
        }
    }

    private fun doForceSyncOverridesTemplate() {
        val dest = pathConfig.overrideTasksFile
        runCatching {
            dest.parentFile?.mkdirs()
            context.assets.open(OVERRIDES_ASSET_TASKS).use { src ->
                dest.outputStream().use { src.copyTo(it) }
            }
            Timber.d("overrides 模板已同步: ${dest.absolutePath}")
        }.onFailure {
            Timber.w(it, "overrides 模板同步失败，跳过")
        }
    }
}
