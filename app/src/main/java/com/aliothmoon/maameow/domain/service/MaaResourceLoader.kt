package com.aliothmoon.maameow.domain.service

import android.os.Process
import com.aliothmoon.maameow.RemoteService
import com.aliothmoon.maameow.MaaCoreService
import com.aliothmoon.maameow.data.config.MaaPathConfig
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.data.resource.ActivityManager
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.data.resource.ResourceDataManager
import com.aliothmoon.maameow.manager.LogcatServiceManager
import com.aliothmoon.maameow.manager.RemoteServiceManager.useRemoteService
import com.aliothmoon.maameow.utils.PerformanceTrace
import com.aliothmoon.maameow.domain.state.MaaResourceLoadStateStore
import com.aliothmoon.maameow.utils.i18n.LocaleBootstrap.resolveSelectedLanguage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

class MaaResourceLoader(
    private val pathConfig: MaaPathConfig,
    private val appSettings: AppSettingsManager,
    private val chainState: TaskChainState,
    private val itemHelper: ItemHelper,
    private val resourceDataManager: ResourceDataManager,
    private val activityManager: ActivityManager,
    private val resourceInitService: ResourceInitService,
    private val stateStore: MaaResourceLoadStateStore,
) {
    private val fullReloadInProgress = AtomicBoolean(false)
    private val loadMutex = Mutex()
    private val metadataDispatcher = Dispatchers.Default.limitedParallelism(1)
    private val metadataScope = CoroutineScope(SupervisorJob() + metadataDispatcher)
    private val metadataJobs = mutableMapOf<String, Deferred<Result<Unit>>>()
    private var metadataPreloadJob: Job? = null
    @Volatile
    private var loadedClientType: String? = null

    sealed class State {
        data object NotLoaded : State()
        data class Loading(val message: String = "") : State()
        data class Reloading(val message: String = "") : State()
        data object Ready : State()
        /**
         * @param permanent true = 资源文件缺失，重试无意义，需用户手动重新初始化；
         *                  false = IPC/IO 临时失败，ensureLoaded() 可再次尝试加载。
         */
        data class Failed(val message: String, val permanent: Boolean = false) : State()
    }

    val state: StateFlow<State> = stateStore.state

    suspend fun load(clientType: String = chainState.getClientType()): Result<Unit> =
        loadMutex.withLock {
            loadLocked(clientType)
        }

    private suspend fun loadLocked(clientType: String): Result<Unit> {
        appSettings.awaitLoaded()
        loadedClientType = null
        stateStore.set(State.Loading())
        val isGlobal = clientType !in listOf("", "Official", "Bilibili")
        if (isGlobal) {
            val result = resourceInitService.ensureClientResources(clientType)
            if (result.isFailure) {
                stateStore.set(State.Failed("资源未就绪，请重新初始化", permanent = false))
                return result
            }
        }
        if (!pathConfig.isResourceReady) {
            Timber.e("MaaResourceLoader.load() aborted: resource not ready (version.json missing or app version mismatch)")
            stateStore.set(State.Failed("资源未就绪，请重新初始化", permanent = true))
            return Result.failure(Exception("Resource not ready"))
        }
        Timber.i("MaaCore resources loading, client type=$clientType")
        val coreTrace = PerformanceTrace.beginAsync("MaaResourceLoader.maaCore")
        val coreResult = try {
            withContext(Dispatchers.IO) {
                useRemoteService { srv ->
                    srv.setup(pathConfig.rootDir, appSettings.debugMode.value)
                    srv.setForceFullscreenOnVirtualDisplay(appSettings.forceFullscreenOnVirtualDisplay.value)

                    val maa = srv.maaCoreService
                    copyTasksJson(pathConfig.cacheResourceDir)

                    if (!loadResIfExists(maa, pathConfig.rootDir)) {
                        stateStore.set(State.Failed("Failed to load main resource"))
                        Timber.e("LoadResource failed: ${pathConfig.rootDir}")
                        return@useRemoteService Result.failure(Exception("Failed to load main resource"))
                    }

                    val followUps = buildList {
                        add(pathConfig.cacheDir)
                        if (isGlobal) {
                            pathConfig.globalResourceDir(clientType).parent?.let(::add)
                            pathConfig.globalCacheResourceDir(clientType).parent?.let(::add)
                        }
                    }

                    if (isGlobal) {
                        copyTasksJson(pathConfig.globalCacheResourceDir(clientType).absolutePath)
                    }

                    followUps.forEach { loadResIfExists(maa, it) }

                    if (appSettings.tasksOverrideEnabled.value) {
                        loadResIfExists(maa, pathConfig.overridesDir)
                    }

                    if (appSettings.debugMode.value) {
                        startDebugLogCapture(srv)
                    }
                    Result.success(Unit)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "MaaResourceLoader error")
            stateStore.set(State.Failed(e.message ?: "Resource loading exception"))
            Result.failure(e)
        } finally {
            PerformanceTrace.endAsync("MaaResourceLoader.maaCore", coreTrace)
        }

        if (coreResult.isFailure) return coreResult
        loadedClientType = clientType
        stateStore.set(State.Ready)
        scheduleTaskMetadataPreload(clientType)
        return Result.success(Unit)
    }

    private fun startDebugLogCapture(srv: RemoteService) {
        val appPid = Process.myPid()
        val servicePid = srv.pid()
        CoroutineScope(Dispatchers.IO).async {
            runCatching {
                LogcatServiceManager.bind()
                LogcatServiceManager.startCapture(
                    appPid,
                    servicePid,
                    pathConfig.rootDir
                )
            }.onFailure { Timber.w(it, "LogcatService startCapture failed") }
        }
    }

    private suspend fun doLoadDepsInfo(clientType: String) {
        val displayLanguage = ResourceDataManager.displayLanguageCode(
            resolveSelectedLanguage(appSettings.language.value)
        )
        withTimeout(30_000) {
            withContext(metadataDispatcher) {
                resourceDataManager.load(clientType, displayLanguage)
                itemHelper.load()
                activityManager.load(clientType)
            }
        }
    }

    private fun scheduleTaskMetadataPreload(clientType: String) {
        synchronized(metadataJobs) {
            metadataPreloadJob?.cancel()
            metadataPreloadJob = metadataScope.launch {
                delay(TASK_METADATA_PRELOAD_DELAY_MS)
                ensureTaskMetadataReady(clientType)
            }
        }
    }

    suspend fun ensureTaskMetadataReady(
        clientType: String? = null,
    ): Result<Unit> {
        val resolvedClientType = clientType ?: chainState.getClientType()
        val job = synchronized(metadataJobs) {
            metadataJobs.getOrPut(resolvedClientType) {
                metadataScope.async { loadTaskMetadata(resolvedClientType) }
            }
        }
        return job.await()
    }

    private suspend fun loadTaskMetadata(clientType: String): Result<Unit> {
        val trace = PerformanceTrace.beginAsync("MaaResourceLoader.taskMetadata")
        return try {
            doLoadDepsInfo(clientType)
            Result.success(Unit)
        } catch (error: Exception) {
            Timber.e(error, "Task metadata loading failed")
            synchronized(metadataJobs) { metadataJobs.remove(clientType) }
            Result.failure(error)
        } finally {
            PerformanceTrace.endAsync("MaaResourceLoader.taskMetadata", trace)
        }
    }

    private fun loadResIfExists(maa: MaaCoreService, parentDir: String): Boolean {
        val resDir = File(parentDir, "resource")
        if (!resDir.exists()) {
            Timber.d("Resource directory not found, skipping: ${resDir.absolutePath}")
            return true
        }
        return maa.LoadResource(parentDir).also { ok ->
            if (ok) Timber.i("LoadResource succeeded: $parentDir")
            else Timber.w("LoadResource failed: $parentDir")
        }
    }

    suspend fun ensureLoaded(): Result<Unit> = ensureLoaded(chainState.getClientType())

    suspend fun ensureLoaded(clientType: String): Result<Unit> {
        return when (val s = state.value) {
            is State.Ready -> if (loadedClientType == clientType) {
                Result.success(Unit)
            } else {
                load(clientType)
            }
            is State.Failed -> if (s.permanent) {
                // 资源文件缺失，重试无意义
                Result.failure(Exception(s.message))
            } else {
                // 临时失败（IPC/IO），重新尝试加载
                load(clientType)
            }
            is State.Loading, is State.Reloading -> {
                // 等待当前加载结束，避免并发启动时误报失败
                val terminal = state.first { it is State.Ready || it is State.Failed }
                if (terminal is State.Failed) {
                    Result.failure(Exception(terminal.message))
                } else if (loadedClientType == clientType) {
                    Result.success(Unit)
                } else {
                    load(clientType)
                }
            }
            else -> load(clientType)
        }
    }

    fun reset() {
        if (fullReloadInProgress.get()) {
            Timber.i("Skip resource reset while full reload is in progress")
            return
        }
        loadedClientType = null
        stateStore.set(State.NotLoaded)
        synchronized(metadataJobs) {
            metadataPreloadJob?.cancel()
            metadataPreloadJob = null
            metadataJobs.values.forEach { it.cancel() }
            metadataJobs.clear()
        }
    }

    /**
     * Copy tasks.json to tasks/tasks.json (compatible with new directory structure)
     */
    private fun copyTasksJson(resourcePath: String) {
        try {
            val src = File(resourcePath, "tasks.json")
            if (!src.exists()) return
            val destDir = File(resourcePath, "tasks").apply { mkdirs() }
            val dest = File(destDir, "tasks.json")
            if (dest.exists() && dest.length() == src.length() && dest.lastModified() >= src.lastModified()) {
                return
            }
            src.copyTo(dest, overwrite = true)
            Timber.d("copyTasksJson: ${src.absolutePath} -> ${dest.absolutePath}")
        } catch (e: Exception) {
            Timber.w(e, "copyTasksJson failed: $resourcePath")
        }
    }

    private companion object {
        const val TASK_METADATA_PRELOAD_DELAY_MS = 5_000L
    }
}
