package com.aliothmoon.maameow.domain.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import android.os.Bundle
import android.graphics.Color
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.IconCompat
import com.aliothmoon.maameow.MainActivity
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.manager.RemoteServiceManager
import com.xzakota.hyper.notification.focus.FocusNotification
import com.aliothmoon.maameow.maa.callback.TaskChainStatusTracker
import com.aliothmoon.maameow.maa.callback.TaskRunInfo
import com.aliothmoon.maameow.maa.callback.TaskRunStatus
import com.aliothmoon.maameow.maa.callback.groupByVisibleTask
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.koin.android.ext.android.inject
import timber.log.Timber

class TaskExecutionService : Service() {

    companion object {
        private const val TASK_CHANNEL_ID = "task_execution_live"
        private const val NOTIFICATION_ID = 9003
        private const val MIN_UPDATE_INTERVAL_MS = 1000L
        private const val PROGRESS_STYLE_MAX = 1000
        private const val PERMISSION_POST_PROMOTED_NOTIFICATIONS =
            "android.permission.POST_PROMOTED_NOTIFICATIONS"
        private const val ACTION_COMPLETE = "com.aliothmoon.maameow.action.TASK_NOTIFICATION_COMPLETE"
        private const val EXTRA_COMPLETION_SUMMARY = "completion_summary"

        private const val PROGRESS_COLOR_COMPLETED = 0xFF4CAF50.toInt()
        private const val PROGRESS_COLOR_ACTIVE = 0xFF2196F3.toInt()
        private const val PROGRESS_COLOR_PENDING = 0xFF9E9E9E.toInt()
        private const val PROGRESS_COLOR_ERROR = 0xFFD32F2F.toInt()

        private val VISIBLE_TASK_TITLE_RES = mapOf(
            "Fight" to R.string.maa_fight,
            "Recruit" to R.string.maa_recruit,
            "Infrast" to R.string.maa_infrast,
            "Mall" to R.string.maa_mall,
            "Award" to R.string.maa_award,
            "Roguelike" to R.string.maa_roguelike,
            "Copilot" to R.string.maa_copilot,
            "SSSCopilot" to R.string.maa_sss_copilot,
            "Reclamation" to R.string.maa_reclamation,
            "Custom" to R.string.maa_custom,
            "CloseDown" to R.string.maa_close_down,
            "StartUp" to R.string.maa_start_up,
            "Depot" to R.string.maa_depot,
            "OperBox" to R.string.maa_oper_box,
        )

        fun start(context: Context) {
            val intent = Intent(context, TaskExecutionService::class.java)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, TaskExecutionService::class.java))
        }

        fun complete(context: Context, summary: String) {
            val intent = Intent(context, TaskExecutionService::class.java).apply {
                action = ACTION_COMPLETE
                putExtra(EXTRA_COMPLETION_SUMMARY, summary)
            }
            context.startService(intent)
        }
    }

    private val compositionService: MaaCompositionService by inject()
    private val sessionLogger: MaaSessionLogger by inject()
    private val taskChainStatusTracker: TaskChainStatusTracker by inject()
    private val appSettingsManager: AppSettingsManager by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val miIslandNetworkMutex = Mutex()
    @Volatile private var notificationSessionActive = false
    private var stoppingRequested = false
    private var keepTerminalNotification = false
    private var completionFinalizing = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_COMPLETE) {
            val summary = intent.getStringExtra(EXTRA_COMPLETION_SUMMARY)
                ?: getString(R.string.notification_task_completed)
            finalizeMiIslandCompletion(summary)
        }
        return START_NOT_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        notificationSessionActive = true
        ensureNotificationChannel()
        val initial = TaskNotificationSnapshot(
            state = compositionService.state.value,
            statusText = sessionLogger.logs.value.lastOrNull()?.content,
            tasks = taskChainStatusTracker.tasks.value,
        )
        startAsForeground(buildNotification(initial))
        observeProgress()
    }

    override fun onDestroy() {
        notificationSessionActive = false
        // 外部 stopService 与 StateFlow 收集存在竞态；此处兜底确保 Live Update 通知被清除。
        // observeProgress 的 collector 由 serviceScope.cancel() 结构化取消。
        if (!keepTerminalNotification) removeActiveNotification()
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun observeProgress() {
        serviceScope.launch {
            var lastUpdateTime = 0L
            var pending: TaskNotificationSnapshot? = null
            var scheduledJob: Job? = null

            fun resetSchedule() {
                scheduledJob?.cancel()
                scheduledJob = null
                pending = null
            }

            fun forceUpdate(snapshot: TaskNotificationSnapshot) {
                resetSchedule()
                lastUpdateTime = SystemClock.elapsedRealtime()
                updateNotification(snapshot)
            }

            fun throttledUpdate(snapshot: TaskNotificationSnapshot) {
                val now = SystemClock.elapsedRealtime()
                val elapsed = now - lastUpdateTime
                if (elapsed >= MIN_UPDATE_INTERVAL_MS) {
                    lastUpdateTime = now
                    updateNotification(snapshot)
                    return
                }
                pending = snapshot
                if (scheduledJob?.isActive == true) return
                scheduledJob = launch {
                    delay((MIN_UPDATE_INTERVAL_MS - elapsed).coerceAtLeast(0L))
                    pending?.let {
                        lastUpdateTime = SystemClock.elapsedRealtime()
                        updateNotification(it)
                    }
                    pending = null
                    scheduledJob = null
                }
            }

            // 三个数据源合并为单一 Snapshot 流，distinctUntilChanged 避免无意义刷新。
            combine(
                compositionService.state,
                taskChainStatusTracker.tasks,
                sessionLogger.logs
                    .map { it.lastOrNull()?.content }
                    .distinctUntilChanged(),
            ) { state, tasks, lastLog ->
                TaskNotificationSnapshot(state, lastLog, tasks)
            }
                .distinctUntilChanged()
                .collect { snapshot ->
                    when (snapshot.state) {
                        MaaExecutionState.IDLE,
                        MaaExecutionState.ERROR -> {
                            resetSchedule()
                            handleTerminalState(snapshot)
                        }

                        MaaExecutionState.STARTING -> forceUpdate(
                            snapshot.copy(
                                statusText = getString(R.string.notification_task_starting)
                            )
                        )

                        MaaExecutionState.STOPPING -> forceUpdate(
                            snapshot.copy(
                                statusText = getString(R.string.notification_task_stopping)
                            ).also { stoppingRequested = true }
                        )

                        MaaExecutionState.RUNNING -> throttledUpdate(
                            snapshot.copy(
                                statusText = snapshot.statusText
                                    ?: getString(R.string.notification_task_running)
                            )
                        )
                    }
                }
        }
    }

    private fun handleTerminalState(snapshot: TaskNotificationSnapshot) {
        if (snapshot.state == MaaExecutionState.IDLE &&
            !stoppingRequested &&
            appSettingsManager.taskNotificationStyle.value == AppSettingsManager.TaskNotificationStyle.MI_ISLAND
        ) {
            val summary = sessionLogger.logs.value.lastOrNull()?.content
                ?: getString(R.string.notification_task_completed)
            finalizeMiIslandCompletion(summary, snapshot)
            return
        }
        notificationSessionActive = false
        Timber.i("TaskExecutionService: state=%s, stopping", snapshot.state)
        stopForeground(STOP_FOREGROUND_REMOVE)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.cancel(NOTIFICATION_ID)
        } catch (e: SecurityException) {
            Timber.w(e, "cancel blocked by POST_NOTIFICATIONS denial")
        }
        stopSelf()
    }

    private fun finalizeMiIslandCompletion(
        summary: String,
        snapshot: TaskNotificationSnapshot = TaskNotificationSnapshot(
            state = MaaExecutionState.IDLE,
            statusText = summary,
            tasks = taskChainStatusTracker.tasks.value,
        ),
    ) {
        if (completionFinalizing) return
        completionFinalizing = true
        keepTerminalNotification = true
        serviceScope.launch {
            delay(300L)
            val completed = snapshot.copy(
                state = MaaExecutionState.IDLE,
                statusText = summary,
            )
            notifyMiIslandWithBypass(completed)
            stopForeground(STOP_FOREGROUND_DETACH)
            notificationSessionActive = false
            stopSelf()
        }
    }

    private fun ensureNotificationChannel() {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val taskChannelName = getString(R.string.notification_channel_task_execution_live)
        val channelDescription = getString(R.string.notification_channel_task_execution_desc)

        // Live updates must use DEFAULT+ importance to remain eligible for promoted ongoing
        // notifications / Live Updates on supported devices.
        val taskChannel = NotificationChannel(
            TASK_CHANNEL_ID,
            taskChannelName,
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = channelDescription
            setShowBadge(false)
        }

        manager.createNotificationChannel(taskChannel)
    }

    private fun startAsForeground(notification: Notification) {
        val shouldBypass = appSettingsManager.taskNotificationStyle.value == AppSettingsManager.TaskNotificationStyle.MI_ISLAND &&
            appSettingsManager.miIslandBypassRestriction.value
        val remote = (RemoteServiceManager.state.value as? RemoteServiceManager.ServiceState.Connected)?.service
        val blocked = shouldBypass && remote?.setPackageNetworkingEnabled("com.xiaomi.xmsf", false) == true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
        if (blocked) {
            serviceScope.launch(Dispatchers.IO) {
                delay(100L)
                withContext(NonCancellable) {
                    runCatching { remote?.setPackageNetworkingEnabled("com.xiaomi.xmsf", true) }
                        .onFailure { Timber.e(it, "Failed to restore XMSF networking after startForeground") }
                }
            }
        }
    }

    private fun buildNotification(snapshot: TaskNotificationSnapshot): Notification {
        val statusText = snapshot.statusText ?: defaultStatusText(snapshot.state)
        val progressInfo = buildProgressInfo(snapshot)
        val contentText = buildContentText(statusText, progressInfo)
        val activeName = activeTaskName(snapshot)
        val title = activeName ?: getString(R.string.notification_task_running_title)

        return if (appSettingsManager.taskNotificationStyle.value == AppSettingsManager.TaskNotificationStyle.MI_ISLAND) {
            buildMiIslandNotification(snapshot, progressInfo, activeName)
        } else {
            buildCompatProgressNotification(
                title = title,
                contentText = contentText,
                progressInfo = progressInfo,
                activeTaskName = activeName,
            )
        }
    }

    private fun buildMiIslandNotification(
        snapshot: TaskNotificationSnapshot,
        progressInfo: TaskProgressInfo,
        activeName: String?,
    ): Notification {
        val title = if (snapshot.state == MaaExecutionState.IDLE) {
            getString(R.string.notification_task_completed)
        } else activeName ?: getString(R.string.notification_task_running_title)
        val currentContent = snapshot.statusText ?: getString(R.string.notification_task_running)
        val isTerminal = snapshot.state == MaaExecutionState.IDLE
        val builder = NotificationCompat.Builder(this, TASK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_maa_logo)
            .setContentTitle(title)
            .setContentText(currentContent)
            .setContentIntent(buildContentIntent())
            .setOngoing(!isTerminal)
            .setAutoCancel(isTerminal)
            .setOnlyAlertOnce(!isTerminal)
            .setSilent(!isTerminal)
            .setCategory(if (isTerminal) NotificationCompat.CATEGORY_STATUS else NotificationCompat.CATEGORY_PROGRESS)
        val extras = FocusNotification.buildV3 {
            val logo = Icon.createWithResource(this@TaskExecutionService, R.mipmap.ic_launcher)
            val logoKey = createPicture("maa_task_logo", logo)
            business = "maa_task"
            enableFloat = isTerminal
            islandFirstFloat = isTerminal
            updatable = true
            ticker = title
            tickerPic = logoKey
            island {
                islandProperty = 1
                bigIslandArea {
                    imageTextInfoLeft {
                        type = 1
                        picInfo { type = 1; pic = logoKey }
                        textInfo { this.title = title }
                    }
                    progressTextInfo {
                        progressInfo {
                            progress = (progressInfo.progress * 100 / PROGRESS_STYLE_MAX)
                            isCCW = false
                        }
                        textInfo {
                            this.title = progressInfo.progressLabel ?: "0/0"
                        }
                    }
                }
                smallIslandArea { picInfo { type = 1; pic = logoKey } }
            }
            baseInfo {
                type = 2
                this.title = title
                subTitle = progressInfo.progressLabel ?: "0/0"
                colorSubTitle = "#2196F3"
                colorSubTitleDark = "#2196F3"
                content = currentContent
            }
            picInfo {
                type = 1
                pic = logoKey
                picDark = logoKey
            }
            progressInfo {
                progress = (progressInfo.progress * 100 / PROGRESS_STYLE_MAX)
                colorProgress = "#2196F3"
                colorProgressEnd = "#2196F3"
            }
        }
        builder.addExtras(extras)
        return builder.build()
    }

    private fun defaultStatusText(state: MaaExecutionState): String = when (state) {
        MaaExecutionState.STARTING -> getString(R.string.notification_task_starting)
        MaaExecutionState.STOPPING -> getString(R.string.notification_task_stopping)
        MaaExecutionState.RUNNING -> getString(R.string.notification_task_running)
        MaaExecutionState.IDLE -> getString(R.string.notification_task_completed)
        MaaExecutionState.ERROR -> getString(R.string.notification_task_error)
    }

    private fun buildCompatProgressNotification(
        title: String,
        contentText: String,
        progressInfo: TaskProgressInfo,
        activeTaskName: String?,
    ): Notification {
        val style = NotificationCompat.ProgressStyle()
            .setStyledByProgress(true)
            .setProgressIndeterminate(progressInfo.totalCount == 0)
            .setProgressTrackerIcon(
                IconCompat.createWithResource(this, R.drawable.ic_progress_tracker)
            )

        if (progressInfo.totalCount > 0) {
            style.setProgress(progressInfo.progress)
        }

        progressInfo.segments.forEach { segment ->
            style.addProgressSegment(
                NotificationCompat.ProgressStyle.Segment(segment.length)
                    .setColor(segment.color)
            )
        }

        val shortCritical = when {
            progressInfo.progressLabel != null && activeTaskName != null ->
                "${progressInfo.progressLabel} $activeTaskName"

            progressInfo.progressLabel != null -> progressInfo.progressLabel
            activeTaskName != null -> activeTaskName
            else -> null
        }

        return NotificationCompat.Builder(this, TASK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_maa_logo)
            .setColor(progressInfo.barColor)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(style)
            .setContentIntent(buildContentIntent())
            .setOngoing(true)
            .setRequestPromotedOngoing(canRequestPromotedOngoing())
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .apply {
                if (shortCritical != null) {
                    setShortCriticalText(shortCritical)
                }
            }
            .build()
    }

    private fun buildProgressInfo(snapshot: TaskNotificationSnapshot): TaskProgressInfo {
        // IDLE/ERROR 分支仅用于 onCreate fast-fail 时构建初始通知的兜底。
        val tasks = snapshot.tasks.groupByVisibleTask()
        val total = tasks.size
        if (total == 0) {
            return TaskProgressInfo(
                max = PROGRESS_STYLE_MAX,
                progress = 0,
                completedCount = 0,
                totalCount = 0,
                barColor = PROGRESS_COLOR_ACTIVE,
                progressLabel = null,
                segments = listOf(
                    ProgressSegmentInfo(PROGRESS_STYLE_MAX, PROGRESS_COLOR_PENDING)
                ),
            )
        }

        val completedCount = tasks.count { it.status == TaskRunStatus.COMPLETED }
        val activeIndex = tasks.indexOfFirst { it.status == TaskRunStatus.IN_PROGRESS }
            .takeIf { it >= 0 }
        val taskErrorIndex = tasks.indexOfFirst { it.status == TaskRunStatus.ERROR }
            .takeIf { it >= 0 }

        fun progressFor(finishedCount: Int): Int =
            (finishedCount.toLong() * PROGRESS_STYLE_MAX / total).toInt()

        val progress = when {
            taskErrorIndex != null -> progressFor(taskErrorIndex + 1)
            snapshot.state == MaaExecutionState.ERROR -> progressFor(completedCount)
            snapshot.state == MaaExecutionState.IDLE -> PROGRESS_STYLE_MAX
            snapshot.state == MaaExecutionState.STOPPING -> {
                val idx = completedCount.coerceAtLeast(activeIndex ?: completedCount)
                    .coerceIn(0, total)
                progressFor(idx)
            }

            activeIndex != null -> {
                progressFor(activeIndex) + (PROGRESS_STYLE_MAX / total) / 2
            }

            completedCount > 0 -> progressFor(completedCount)
            else -> 0
        }.coerceIn(0, PROGRESS_STYLE_MAX)

        val barColor = when {
            taskErrorIndex != null || snapshot.state == MaaExecutionState.ERROR ->
                PROGRESS_COLOR_ERROR

            snapshot.state == MaaExecutionState.IDLE -> PROGRESS_COLOR_COMPLETED
            else -> PROGRESS_COLOR_ACTIVE
        }

        return TaskProgressInfo(
            max = PROGRESS_STYLE_MAX,
            progress = progress,
            completedCount = completedCount,
            totalCount = total,
            barColor = barColor,
            progressLabel = "$completedCount/$total",
            segments = listOf(ProgressSegmentInfo(PROGRESS_STYLE_MAX, barColor)),
        )
    }

    private fun buildContentText(statusText: String, progressInfo: TaskProgressInfo): String {
        val label = progressInfo.progressLabel ?: return statusText
        return "$label · $statusText"
    }

    private fun activeTaskName(snapshot: TaskNotificationSnapshot): String? =
        snapshot.tasks
            .firstOrNull { it.status == TaskRunStatus.IN_PROGRESS }
            ?.taskChain
            ?.trim()
            ?.let { taskChain ->
                VISIBLE_TASK_TITLE_RES[taskChain]?.let(::getString)
            }

    private fun canRequestPromotedOngoing(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            ContextCompat.checkSelfPermission(
                this,
                PERMISSION_POST_PROMOTED_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun updateNotification(snapshot: TaskNotificationSnapshot) {
        if (appSettingsManager.taskNotificationStyle.value == AppSettingsManager.TaskNotificationStyle.MI_ISLAND &&
            appSettingsManager.miIslandBypassRestriction.value
        ) {
            serviceScope.launch(Dispatchers.IO) { notifyMiIslandWithBypass(snapshot) }
            return
        }
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.notify(NOTIFICATION_ID, buildNotification(snapshot))
        } catch (e: SecurityException) {
            // API 33+ 用户拒绝 POST_NOTIFICATIONS 时 notify 会抛 SecurityException；
            // FGS 主线程不应被通知权限异常拖垮，对齐 MaaEventNotifier 的处理。
            Timber.w(e, "notify blocked by POST_NOTIFICATIONS denial")
        }
    }

    private suspend fun notifyMiIslandWithBypass(snapshot: TaskNotificationSnapshot) {
        miIslandNetworkMutex.withLock {
            if (!notificationSessionActive) return@withLock
            val remote = (RemoteServiceManager.state.value as? RemoteServiceManager.ServiceState.Connected)?.service
            var blocked = false
            try {
                blocked = remote?.setPackageNetworkingEnabled("com.xiaomi.xmsf", false) == true
                val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (notificationSessionActive) manager.notify(NOTIFICATION_ID, buildNotification(snapshot))
                if (blocked) delay(100L)
            } catch (e: Exception) {
                Timber.w(e, "Mi Island bypass failed; notification sent without bypass when possible")
                runCatching {
                    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    if (notificationSessionActive) manager.notify(NOTIFICATION_ID, buildNotification(snapshot))
                }
            } finally {
                if (blocked) {
                    withContext(NonCancellable) {
                        runCatching { remote?.setPackageNetworkingEnabled("com.xiaomi.xmsf", true) }
                            .onFailure { Timber.e(it, "Failed to restore XMSF networking") }
                    }
                }
            }
        }
    }

    private fun removeActiveNotification() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        try {
            manager.cancel(NOTIFICATION_ID)
        } catch (e: SecurityException) {
            Timber.w(e, "cancel blocked by POST_NOTIFICATIONS denial")
        }
    }

    private fun buildContentIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private data class TaskNotificationSnapshot(
        val state: MaaExecutionState,
        val statusText: String?,
        val tasks: List<TaskRunInfo>,
    )

    private data class TaskProgressInfo(
        val max: Int,
        val progress: Int,
        val completedCount: Int,
        val totalCount: Int,
        val barColor: Int,
        val progressLabel: String?,
        val segments: List<ProgressSegmentInfo>,
    )

    private data class ProgressSegmentInfo(
        val length: Int,
        val color: Int,
    )
}
