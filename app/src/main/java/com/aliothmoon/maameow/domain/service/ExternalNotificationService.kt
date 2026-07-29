package com.aliothmoon.maameow.domain.service

import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.notification.NotificationSettingsManager
import com.aliothmoon.maameow.data.notification.provider.NotificationProvider
import com.aliothmoon.maameow.utils.i18n.UiText
import com.aliothmoon.maameow.utils.i18n.uiTextOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import timber.log.Timber

class ExternalNotificationService(
    private val settingsManager: NotificationSettingsManager,
    private val sessionLogger: MaaSessionLogger,
    providerList: List<NotificationProvider>,
) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val providers = providerList.associateBy(NotificationProvider::id)
    private val _feedbackMessages = MutableSharedFlow<UiText>(extraBufferCapacity = 16)
    val feedbackMessages: SharedFlow<UiText> = _feedbackMessages.asSharedFlow()

    fun send(title: String, content: String) {
        scope.launch {
            dispatchToProviders(title, content, isTest = false)
        }
    }

    fun sendWithLogs(title: String, content: String) {
        scope.launch {
            settingsManager.awaitLoaded()
            val body = if (settingsManager.includeLogDetails.value) {
                val logs = sessionLogger.logs.value
                    .joinToString("\n") { "[${it.formattedTime}] ${it.content}" }
                if (logs.isNotEmpty()) "$logs\n$content" else content
            } else {
                content
            }
            dispatchToProviders(title, body, isTest = false)
        }
    }

    fun sendTest(title: String, content: String) {
        scope.launch {
            dispatchToProviders(title, content, isTest = true)
        }
    }

    private suspend fun dispatchToProviders(title: String, content: String, isTest: Boolean) {
        settingsManager.awaitLoaded()
        val enabledIds = settingsManager.enabledProviderIds.value

        if (enabledIds.isEmpty()) {
            if (isTest) {
                _feedbackMessages.tryEmit(uiTextOf(R.string.notification_feedback_no_channel))
            }
            return
        }

        val prefixedTitle = "[MAA] $title"

        for (id in enabledIds) {
            val provider = providers[id]
            val result = if (provider == null) {
                Timber.w("未知通知渠道: $id")
                false
            } else {
                runCatching { provider.send(prefixedTitle, content) }
                    .onFailure { Timber.e(it, "通知渠道 $id 发送失败") }
                    .getOrDefault(false)
            }

            if (isTest || !result) {
                _feedbackMessages.tryEmit(
                    if (result) uiTextOf(R.string.notification_feedback_send_success, id)
                    else uiTextOf(R.string.notification_feedback_send_failed, id)
                )
            }
        }
    }
}
