package com.aliothmoon.maameow.presentation.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.DisplayMode
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.domain.models.OverlayControlMode
import com.aliothmoon.maameow.domain.models.RemoteBackend
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.domain.service.ResourceInitService
import com.aliothmoon.maameow.domain.service.MaaResourceLoader
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.domain.state.MaaExecutionStateStore
import com.aliothmoon.maameow.domain.state.MaaResourceLoadStateStore
import com.aliothmoon.maameow.domain.state.OverlayStateStore
import com.aliothmoon.maameow.domain.state.ResourceInitState
import com.aliothmoon.maameow.manager.PermissionManager
import com.aliothmoon.maameow.manager.RemoteServiceManager
import com.aliothmoon.maameow.manager.RemoteServiceManager.useRemoteService
import com.aliothmoon.maameow.manager.ShizukuInstallHelper
import com.aliothmoon.maameow.overlay.OverlayController
import com.aliothmoon.maameow.presentation.state.HomeInteractionUiState
import com.aliothmoon.maameow.presentation.state.HomeServiceUiState
import com.aliothmoon.maameow.presentation.state.StatusColorType
import com.aliothmoon.maameow.presentation.state.UiEffect
import com.aliothmoon.maameow.utils.Misc
import com.aliothmoon.maameow.utils.i18n.remoteBackendPermissionLabel
import com.aliothmoon.maameow.utils.i18n.uiTextOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.math.abs

class HomeViewModel(
    private val application: Context,
    private val appSettingsManager: AppSettingsManager,
    private val overlayController: Lazy<OverlayController>,
    private val permissionManager: PermissionManager,
    private val resourceLoadStateStore: MaaResourceLoadStateStore,
    private val executionStateStore: MaaExecutionStateStore,
    private val resourceInitService: ResourceInitService,
    private val overlayStateStore: OverlayStateStore,
) : ViewModel() {

    private val _serviceUiState = MutableStateFlow(
        HomeServiceUiState(serviceStatusText = uiTextOf(R.string.home_status_disconnected))
    )
    val serviceUiState: StateFlow<HomeServiceUiState> = _serviceUiState

    val resourceInitState: StateFlow<ResourceInitState> = resourceInitService.state

    private val _interactionUiState = MutableStateFlow(HomeInteractionUiState())
    val interactionUiState: StateFlow<HomeInteractionUiState> = _interactionUiState

    private val _effects = Channel<UiEffect>(Channel.BUFFERED)
    val effects = _effects.receiveAsFlow()

    init {
        observeServiceStatus()
        observeRunMode()
        observeFloatWindowMode()
        observeIsGranting()
        observeOverlayActive()
    }

    private fun observeServiceStatus() {
        viewModelScope.launch {
            combine(
                RemoteServiceManager.state,
                resourceLoadStateStore.state,
                executionStateStore.state,
            ) { serviceState, resourceState, executionState ->
                val remoteServiceActive =
                    serviceState is RemoteServiceManager.ServiceState.Connected || serviceState is RemoteServiceManager.ServiceState.Connecting
                val status = when {
                    serviceState is RemoteServiceManager.ServiceState.Died || serviceState is RemoteServiceManager.ServiceState.Error -> Triple(
                        uiTextOf(R.string.home_status_service_error), StatusColorType.ERROR, false
                    )

                    serviceState is RemoteServiceManager.ServiceState.Connecting -> Triple(
                        uiTextOf(R.string.home_status_service_connecting),
                        StatusColorType.WARNING,
                        true
                    )

                    serviceState is RemoteServiceManager.ServiceState.Disconnected -> Triple(
                        uiTextOf(R.string.home_status_disconnected), StatusColorType.NEUTRAL, false
                    )

                    resourceState is MaaResourceLoader.State.Loading || resourceState is MaaResourceLoader.State.Reloading -> Triple(
                        uiTextOf(R.string.home_status_resource_loading),
                        StatusColorType.WARNING,
                        true
                    )

                    resourceState is MaaResourceLoader.State.Failed -> Triple(
                        uiTextOf(R.string.home_status_resource_failed), StatusColorType.ERROR, false
                    )

                    resourceState is MaaResourceLoader.State.NotLoaded -> Triple(
                        uiTextOf(R.string.home_status_resource_not_loaded),
                        StatusColorType.NEUTRAL,
                        false
                    )

                    executionState == MaaExecutionState.ERROR -> Triple(
                        uiTextOf(R.string.home_status_task_error), StatusColorType.ERROR, false
                    )

                    executionState == MaaExecutionState.STARTING -> Triple(
                        uiTextOf(R.string.home_status_task_starting), StatusColorType.WARNING, true
                    )

                    executionState == MaaExecutionState.RUNNING -> Triple(
                        uiTextOf(R.string.home_status_task_running), StatusColorType.PRIMARY, true
                    )

                    else -> Triple(
                        uiTextOf(R.string.home_status_ready),
                        StatusColorType.PRIMARY,
                        false
                    )
                }
                Pair(status, remoteServiceActive)
            }
                .distinctUntilChanged()
                .collect { (status, remoteServiceActive) ->
                val (text, color, loading) = status
                _serviceUiState.update {
                    it.copy(
                        serviceStatusText = text,
                        serviceStatusColor = color,
                        serviceStatusLoading = loading,
                        remoteServiceActive = remoteServiceActive
                    )
                }
            }
        }
    }

    private fun observeFloatWindowMode() {
        viewModelScope.launch {
            appSettingsManager.overlayControlMode.collect { mode ->
                _interactionUiState.update { it.copy(overlayControlMode = mode) }
            }
        }
    }

    private fun observeIsGranting() {
        viewModelScope.launch {
            permissionManager.isGranting.collect { granting ->
                _interactionUiState.update { it.copy(isGranting = granting) }
            }
        }
    }

    private fun observeOverlayActive() {
        viewModelScope.launch {
            overlayStateStore.active.collect { active ->
                _interactionUiState.update { it.copy(isShowControlOverlay = active) }
            }
        }
    }

    private fun observeRunMode() {
        viewModelScope.launch {
            appSettingsManager.runMode.collect { mode ->
                _interactionUiState.update { it.copy(runMode = mode) }
            }
        }
    }

    fun checkAndInitResource() {
        viewModelScope.launch {
            resourceInitService.checkAndInit()
        }
    }

    fun onTryResourceInit() {
        viewModelScope.launch {
            resourceInitService.doExtractFromAssets()
        }
    }

    fun onRequestRemoteAccess() {
        viewModelScope.launch {
            val backend = permissionManager.permissions.startupBackend
            if (!permissionManager.permissions.isStartupBackendAvailable(backend)) {
                _effects.send(
                    UiEffect.toast(
                        R.string.home_toast_backend_unavailable, backend.display
                    )
                )
                return@launch
            }
            val granted = permissionManager.requestRemoteAccess()
            if (!granted) {
                _effects.send(
                    UiEffect.toast(
                        R.string.home_toast_backend_auth_failed, backend.display
                    )
                )
            }
        }
    }

    fun onRequestShizukuAccess() {
        viewModelScope.launch {
            if (!permissionManager.permissions.shizukuAvailable) {
                _effects.send(
                    UiEffect.toast(
                        R.string.home_toast_backend_unavailable,
                        RemoteBackend.SHIZUKU.display,
                    )
                )
                return@launch
            }
            if (!permissionManager.requestShizuku()) {
                _effects.send(
                    UiEffect.toast(
                        R.string.home_toast_backend_auth_failed,
                        RemoteBackend.SHIZUKU.display,
                    )
                )
            }
        }
    }

    fun onRequestOverlay(context: Context) {
        viewModelScope.launch {
            permissionManager.requestOverlay(context)
        }
    }

    fun onRequestStorage(context: Context) {
        viewModelScope.launch {
            permissionManager.requestStorage(context)
        }
    }

    fun onRequestBatteryWhitelist(context: Context) {
        viewModelScope.launch {
            permissionManager.requestBatteryWhitelist(context)
        }
    }

    fun onRequestNotification(context: Context) {
        viewModelScope.launch {
            permissionManager.requestNotification(context)
        }
    }

    fun onRequestAccessibility(context: Context) {
        viewModelScope.launch {
            if (permissionManager.permissions.remoteAccessGranted) {
                val success = permissionManager.quickGrantAccessibility()
                if (success) return@launch
            }
            permissionManager.requestAccessibility(context)
        }
    }

    fun onControlOverlayModeChanged(mode: OverlayControlMode) {
        viewModelScope.launch {
            appSettingsManager.setFloatWindowMode(mode)
            if (_interactionUiState.value.isShowControlOverlay) {
                overlayController.value.applyMode(mode)
            }
        }
    }

    fun onStartControlOverlay() {
        viewModelScope.launch {
            try {
                _serviceUiState.update { it.copy(isLoading = true) }

                // 刷新权限状态
                permissionManager.refresh()
                val state = permissionManager.permissions

                // 检查必要权限
                val currentMode = appSettingsManager.overlayControlMode.value
                if (!state.remoteAccessGranted) {
                    _serviceUiState.update { it.copy(isLoading = false) }
                    _effects.send(
                        UiEffect.toast(
                            R.string.home_toast_grant_permission,
                            application.remoteBackendPermissionLabel(state.startupBackend)
                        )
                    )
                    return@launch
                }

                val missingPermissions = buildList {
                    if (!state.overlay) add(application.getString(R.string.home_permission_overlay))
                    if (!state.storage) add(application.getString(R.string.home_permission_storage))
                    if (currentMode == OverlayControlMode.ACCESSIBILITY && !state.accessibility) {
                        add(application.getString(R.string.home_permission_accessibility))
                    }
                }

                if (missingPermissions.isNotEmpty()) {
                    _serviceUiState.update { it.copy(isLoading = false) }
                    val separator =
                        application.getString(R.string.home_toast_missing_permissions_separator)
                    _effects.send(
                        UiEffect.toast(
                            R.string.home_toast_missing_permissions,
                            missingPermissions.joinToString(separator)
                        )
                    )
                    return@launch
                }

                // 检查分辨率是否为 16:9
                if (!checkResolution()) {
                    _serviceUiState.update { it.copy(isLoading = false) }
                    return@launch
                }

                overlayController.value.show(currentMode)
                _serviceUiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error starting floating window")
                _serviceUiState.update { it.copy(isLoading = false) }
                _effects.send(
                    UiEffect.toast(
                        R.string.home_toast_start_overlay_failed, e.message.orEmpty()
                    )
                )
            }
        }
    }

    fun onStopControlOverlay() {
        viewModelScope.launch {
            try {
                _serviceUiState.update { it.copy(isLoading = true) }
                overlayController.value.hideAll()
                Timber.i("onStopFloatingWindow: Floating window hidden")
                _serviceUiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error stopping floating window")
                _serviceUiState.update { it.copy(isLoading = false) }
                _effects.send(
                    UiEffect.toast(
                        R.string.home_toast_stop_overlay_failed, e.message.orEmpty()
                    )
                )
            }
        }
    }

    fun onOpenShizuku() {
        val opened = if (!appSettingsManager.shizukuShortcutEnabled.value) {
            false
        } else {
            ShizukuInstallHelper.openShizuku(
                application, appSettingsManager.shizukuLaunchPackage.value
            )
        }
        if (!opened) {
            _effects.trySend(UiEffect.toast(R.string.home_toast_open_shizuku_failed))
        }
    }

    fun onToggleRemoteService() {
        if (_serviceUiState.value.remoteServiceActive) {
            onCloseRemoteService()
        } else {
            onOpenRemoteService()
        }
    }

    private fun onOpenRemoteService() {
        viewModelScope.launch {
            try {
                _serviceUiState.update { it.copy(isLoading = true) }

                permissionManager.refresh()
                val state = permissionManager.permissions
                val backend = state.startupBackend
                if (!state.isStartupBackendAvailable(backend)) {
                    _serviceUiState.update { it.copy(isLoading = false) }
                    _effects.send(
                        UiEffect.toast(
                            R.string.home_toast_backend_unavailable, backend.display
                        )
                    )
                    return@launch
                }

                if (!state.remoteAccessGranted) {
                    val granted = permissionManager.requestRemoteAccess()
                    if (!granted) {
                        _serviceUiState.update { it.copy(isLoading = false) }
                        _effects.send(
                            UiEffect.toast(
                                R.string.home_toast_backend_auth_failed, backend.display
                            )
                        )
                        return@launch
                    }
                }

                RemoteServiceManager.bind()

                Timber.i("onOpenRemoteService: Service binding started")
                _serviceUiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error opening remote service")
                _serviceUiState.update { it.copy(isLoading = false) }
                _effects.send(
                    UiEffect.toast(
                        R.string.home_toast_open_service_failed, e.message.orEmpty()
                    )
                )
            }
        }
    }

    private fun onCloseRemoteService() {
        viewModelScope.launch {
            try {
                _serviceUiState.update { it.copy(isLoading = true) }

                // 先关闭依赖远程服务的入口，避免继续操作已断开的 Binder。
                overlayController.value.hideAll()
                RemoteServiceManager.unbind()

                Timber.i("onCloseRemoteService: Service unbound")
                _serviceUiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error closing remote service")
                _serviceUiState.update { it.copy(isLoading = false) }
                _effects.send(
                    UiEffect.toast(
                        R.string.home_toast_close_service_failed, e.message.orEmpty()
                    )
                )
            }
        }
    }

    fun onChangeTo16x9Resolution(ctx: Context) {
        viewModelScope.launch {
            try {
                val label =
                    application.remoteBackendPermissionLabel(permissionManager.permissions.startupBackend)
                if (!permissionManager.permissions.remoteAccessGranted) {
                    val ret = permissionManager.requestRemoteAccess()
                    if (!ret) {
                        _effects.send(
                            UiEffect.toast(
                                R.string.home_toast_backend_not_acquired, label
                            )
                        )
                        return@launch
                    }
                }
                _serviceUiState.update { it.copy(isLoading = true) }
                val (width, height) = Misc.getPhysicalSize(ctx)

                val (targetWidth, targetHeight) = Misc.calculate16x9Resolution(
                    width, height
                )
                val ret = withContext(Dispatchers.IO) {
                    useRemoteService { service ->
                        service.setForcedDisplaySize(targetWidth, targetHeight)
                    }
                }
                Timber.i("onChangeTo16x9Resolution: setForcedDisplaySize result: %s", ret)

                _serviceUiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "onChangeTo16x9Resolution: Error changing resolution")
                _serviceUiState.update { it.copy(isLoading = false) }
                _effects.send(
                    UiEffect.toast(
                        R.string.home_toast_change_resolution_failed, e.message.orEmpty()
                    )
                )
            }
        }
    }

    fun onResetResolution() {
        Timber.i("onResetResolution: Resetting resolution to default")
        viewModelScope.launch {
            try {
                val label =
                    application.remoteBackendPermissionLabel(permissionManager.permissions.startupBackend)
                if (!permissionManager.permissions.remoteAccessGranted) {
                    val ret = permissionManager.requestRemoteAccess()
                    if (!ret) {
                        _effects.send(
                            UiEffect.toast(
                                R.string.home_toast_backend_not_acquired, label
                            )
                        )
                        return@launch
                    }
                }
                _serviceUiState.update { it.copy(isLoading = true) }
                val ret = withContext(Dispatchers.IO) {
                    useRemoteService { it.clearForcedDisplaySize() }
                }
                Timber.i("onResetResolution: %s", ret)
                _serviceUiState.update { it.copy(isLoading = false) }
            } catch (e: Exception) {
                Timber.e(e, "Error resetting resolution")
                _serviceUiState.update { it.copy(isLoading = false) }
                _effects.send(
                    UiEffect.toast(
                        R.string.home_toast_reset_resolution_failed, e.message.orEmpty()
                    )
                )
            }
        }
    }

    private fun checkResolution(): Boolean {
        // 不能用 application.resources.displayMetrics：application context 的 DisplayMetrics
        // 不反映 setForcedDisplaySize 修改后的 forced size（在 Android 9 上持续返回原生物理分辨率
        // 减去系统栏的值）。改用 Misc.getScreenSize，其底层走 Display.getRealMetrics /
        // WindowMetrics.getBounds，能正确读到 IWindowManager 层的 forced size。
        val (width, height) = Misc.getScreenSize(application)

        val longSide = maxOf(width, height)
        val shortSide = minOf(width, height)

        val ratio = longSide.toFloat() / shortSide.toFloat()
        val targetRatio = 16f / 9f
        val tolerance = 0.05f
        val isValid = abs(ratio - targetRatio) <= targetRatio * tolerance

        if (!isValid) {
            _effects.trySend(UiEffect.toast(R.string.home_toast_not_16_9_resolution, long = true))
            Timber.w("resolution check failed: ${longSide}x${shortSide}, required 16:9")
        }

        return isValid
    }

    fun onRunModeChange(isBackground: Boolean) {
        viewModelScope.launch {
            if (isBackground && Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
                _interactionUiState.update {
                    it.copy(
                        showRunModeUnsupportedDialog = true,
                        runModeUnsupportedMessage = uiTextOf(R.string.dialog_run_mode_unsupported_message_pre_q)
                    )
                }
                return@launch
            }

            appSettingsManager.setRunMode(
                if (isBackground) RunMode.BACKGROUND
                else RunMode.FOREGROUND
            )
            if (!isBackground) {
                overlayController.value.setup()
            }
            val mode = if (isBackground) {
                DisplayMode.BACKGROUND
            } else {
                DisplayMode.PRIMARY
            }
            if (permissionManager.permissions.remoteAccessGranted) {
                useRemoteService {
                    it.setVirtualDisplayMode(mode)
                }
            }
        }
    }

    fun onDismissRunModeUnsupportedDialog() {
        _interactionUiState.update { it.copy(showRunModeUnsupportedDialog = false) }
    }

    fun checkRunModeChangeEnabled(): Boolean {
        val value = executionStateStore.state.value
        return !(value == MaaExecutionState.RUNNING || value == MaaExecutionState.STARTING || value == MaaExecutionState.STOPPING)
    }


}
