package com.aliothmoon.maameow.data.notification

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NotificationSettingsManager(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    companion object {
        val Context.notificationDataStore: DataStore<Preferences> by preferencesDataStore(name = "notification_settings")
    }

    private val initialSettings: NotificationSettings = NotificationSettings()

    val settings: Flow<NotificationSettings> = with(NotificationSettingsSchema) {
        context.notificationDataStore.flow
    }
    private val _loadedSettings = MutableStateFlow<NotificationSettings?>(null)
    private val loadedSettings = _loadedSettings.asStateFlow()
    val isLoaded: StateFlow<Boolean> = loadedSettings
        .map { it != null }
        .stateIn(scope, SharingStarted.Eagerly, false)
    private val settingsState: StateFlow<NotificationSettings> = loadedSettings
        .map { it ?: initialSettings }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, initialSettings)

    init {
        scope.launch {
            settings.collect { _loadedSettings.value = it }
        }
    }

    suspend fun awaitLoaded(): NotificationSettings = loadedSettings.filterNotNull().first()

    suspend fun updateSettings(new: NotificationSettings) {
        with(NotificationSettingsSchema) { context.notificationDataStore.update(new) }
    }

    val sendOnComplete: StateFlow<Boolean> = settingsState
        .map { it.sendOnComplete.toBooleanStrictOrNull() ?: true }
        .distinctUntilChanged()
        .stateIn(
            scope, SharingStarted.Eagerly,
            initialSettings.sendOnComplete.toBooleanStrictOrNull() ?: true
        )

    val sendOnError: StateFlow<Boolean> = settingsState
        .map { it.sendOnError.toBooleanStrictOrNull() ?: true }
        .distinctUntilChanged()
        .stateIn(
            scope, SharingStarted.Eagerly,
            initialSettings.sendOnError.toBooleanStrictOrNull() ?: true
        )

    val sendOnServiceDied: StateFlow<Boolean> = settingsState
        .map { it.sendOnServiceDied.toBooleanStrictOrNull() ?: false }
        .distinctUntilChanged()
        .stateIn(
            scope, SharingStarted.Eagerly,
            initialSettings.sendOnServiceDied.toBooleanStrictOrNull() ?: false
        )

    val includeLogDetails: StateFlow<Boolean> = settingsState
        .map { it.includeLogDetails.toBooleanStrictOrNull() ?: false }
        .distinctUntilChanged()
        .stateIn(
            scope, SharingStarted.Eagerly,
            initialSettings.includeLogDetails.toBooleanStrictOrNull() ?: false
        )

    val enabledProviderIds: StateFlow<List<String>> = settingsState
        .map { it.enabledProviders.split(",").filter { id -> id.isNotEmpty() } }
        .distinctUntilChanged()
        .stateIn(
            scope, SharingStarted.Eagerly,
            initialSettings.enabledProviders.split(",").filter { it.isNotEmpty() }
        )
}
