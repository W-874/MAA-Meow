package com.aliothmoon.maameow.presentation.viewmodel

import android.content.Context
import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.achievement.AchievementIds
import com.aliothmoon.maameow.data.achievement.AchievementRepository
import com.aliothmoon.maameow.data.preferences.AppSettingsManager
import com.aliothmoon.maameow.data.preferences.TaskChainState
import com.aliothmoon.maameow.data.repository.DepotRepository
import com.aliothmoon.maameow.data.repository.OperBoxRepository
import com.aliothmoon.maameow.data.repository.toSortedItems
import com.aliothmoon.maameow.data.resource.ActivityManager
import com.aliothmoon.maameow.data.resource.ItemHelper
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.domain.service.MaaCompositionService
import com.aliothmoon.maameow.domain.state.MaaExecutionState
import com.aliothmoon.maameow.domain.usecase.CheckGameReadinessUseCase
import com.aliothmoon.maameow.domain.usecase.GameReadiness
import com.aliothmoon.maameow.domain.usecase.TaskStartContext
import com.aliothmoon.maameow.domain.usecase.TaskStartMode
import com.aliothmoon.maameow.data.model.toolbox.OperBoxExportFormatter
import com.aliothmoon.maameow.data.model.toolbox.OperBoxOperator
import com.aliothmoon.maameow.maa.callback.ToolboxResultCollector
import com.aliothmoon.maameow.maa.task.MaaTaskParams
import com.aliothmoon.maameow.maa.task.MaaTaskType
import com.aliothmoon.maameow.presentation.view.panel.PanelDialogConfirmAction
import com.aliothmoon.maameow.presentation.view.panel.PanelDialogUiState
import com.aliothmoon.maameow.utils.i18n.UiText
import com.aliothmoon.maameow.utils.i18n.uiTextOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlin.random.Random

enum class ToolboxTab(@field:StringRes val labelRes: Int) {
    MINI_GAME(R.string.toolbox_tab_mini_game),
    RECRUIT_CALC(R.string.toolbox_tab_recruit_calc),
    DEPOT(R.string.maa_depot),
    OPER_BOX(R.string.panel_operbox_title),
    GACHA(R.string.toolbox_tab_gacha),
    ;

    companion object {
        /** 前台模式不展示牛牛抽卡 */
        fun visibleFor(runMode: RunMode): List<ToolboxTab> =
            if (runMode == RunMode.FOREGROUND) {
                entries.filter { it != GACHA }
            } else {
                entries.toList()
            }
    }
}

internal fun gachaTaskName(once: Boolean): String = if (once) "GachaOnce" else "GachaTenTimes"

internal fun canStopToolboxExecution(executionState: MaaExecutionState): Boolean =
    executionState == MaaExecutionState.RUNNING

data class RecruitCalcConfig(
    val chooseLevel3: Boolean = true,
    val chooseLevel4: Boolean = true,
    val chooseLevel5: Boolean = true,
    val chooseLevel6: Boolean = true,
    val autoSetTime: Boolean = true,
    val level3Time: Int = 540,
    val level4Time: Int = 540,
    val level5Time: Int = 540,
)

class ToolboxViewModel(
    private val appContext: Context,
    private val compositionService: MaaCompositionService,
    val collector: ToolboxResultCollector,
    activityManager: ActivityManager,
    private val checkGameReadiness: CheckGameReadinessUseCase,
    private val chainState: TaskChainState,
    private val achievementRepository: AchievementRepository,
    val depotRepository: DepotRepository,
    val operBoxRepository: OperBoxRepository,
    private val itemHelper: ItemHelper,
    private val appSettingsManager: AppSettingsManager,
) : ViewModel() {

    val miniGame = MiniGameDelegate(appContext, activityManager, compositionService, viewModelScope, achievementRepository)

    private val _uiState = MutableStateFlow(
        ToolboxUiState(visibleTabs = ToolboxTab.visibleFor(appSettingsManager.runMode.value))
    )
    val uiState: StateFlow<ToolboxUiState> = _uiState.asStateFlow()

    /** Compatibility projections for panels migrating to [uiState]. */
    val currentTab: StateFlow<ToolboxTab> = uiState.map { it.currentTab }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.currentTab)
    val visibleTabs: StateFlow<List<ToolboxTab>> = uiState.map { it.visibleTabs }
        .stateIn(viewModelScope, SharingStarted.Eagerly, _uiState.value.visibleTabs)
    val statusMessage: StateFlow<UiText> = uiState.map { it.statusMessage }
        .stateIn(viewModelScope, SharingStarted.Eagerly, UiText.Empty)
    val dialog: StateFlow<PanelDialogUiState?> = uiState.map { it.dialog }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val gachaOnce: StateFlow<Boolean> = uiState.map { it.gachaOnce }
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    private var gachaTipJob: Job? = null
    private val startMutex = Mutex()
    private var readinessInProgress = false
    private var stopJob: Job? = null

    // ==================== 牛牛抽卡（对齐 WPF Toolbox Gacha）====================

    val gachaDisclaimerAccepted: StateFlow<Boolean> = uiState.map { it.gachaDisclaimerAccepted }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)
    val gachaTip: StateFlow<UiText> = uiState.map { it.gachaTip }
        .stateIn(viewModelScope, SharingStarted.Eagerly, uiTextOf(R.string.gacha_init_tip))

    init {
        // 切到前台时若正停在抽卡 Tab，回退到牛杂
        viewModelScope.launch {
            appSettingsManager.runMode.collect { mode ->
                if (mode == RunMode.FOREGROUND && _uiState.value.currentTab == ToolboxTab.GACHA) {
                    _uiState.update {
                        it.copy(
                            visibleTabs = ToolboxTab.visibleFor(mode),
                            currentTab = ToolboxTab.MINI_GAME,
                            dialog = null,
                            pendingStartRequest = null,
                        ).clearGachaTransientState()
                    }
                } else {
                    _uiState.update { it.copy(visibleTabs = ToolboxTab.visibleFor(mode)) }
                }
            }
        }
        viewModelScope.launch {
            compositionService.state.collect { state ->
                updateExecutionControls(state)
                if (state == MaaExecutionState.IDLE || state == MaaExecutionState.ERROR) {
                    stopGachaTipRotation()
                }
            }
        }
    }

    fun onGachaAgreeDisclaimer() {
        viewModelScope.launch {
            achievementRepository.unlock(AchievementIds.REAL_GACHA)
            _uiState.update {
                it.copy(
                    gachaDisclaimerAccepted = true,
                    gachaTip = uiTextOf(R.string.gacha_init_tip),
                )
            }
        }
    }

    fun onGachaModeChange(once: Boolean) {
        _uiState.update { it.copy(gachaOnce = once) }
    }

    fun onGachaCancel() {
        clearGachaUiState()
    }

    // ==================== 公招识别配置 ====================

    private val _recruitConfig = MutableStateFlow(RecruitCalcConfig())
    val recruitConfig: StateFlow<RecruitCalcConfig> = _recruitConfig.asStateFlow()

    fun onRecruitConfigChange(config: RecruitCalcConfig) {
        _recruitConfig.value = config
    }

    fun onTabChange(tab: ToolboxTab) {
        if (tab == ToolboxTab.GACHA &&
            appSettingsManager.runMode.value == RunMode.FOREGROUND
        ) {
            return
        }
        if (_uiState.value.currentTab == ToolboxTab.GACHA || tab == ToolboxTab.GACHA) {
            clearGachaUiState()
        }
        _uiState.update { it.copy(currentTab = tab) }
    }

    // ==================== 统一启动/停止 ====================

    fun onAction(action: ToolboxAction) {
        when (action) {
            is ToolboxAction.SelectTab -> onTabChange(action.tab)
            is ToolboxAction.SelectGachaMode -> onGachaModeChange(action.once)
            ToolboxAction.AgreeGachaDisclaimer -> onGachaAgreeDisclaimer()
            ToolboxAction.CancelGacha -> onGachaCancel()
            ToolboxAction.Start -> onStart()
            ToolboxAction.Stop -> onStop()
            ToolboxAction.ConfirmDialog -> onDialogConfirm()
            ToolboxAction.DismissDialog -> onDialogDismiss()
        }
    }

    fun onStart() {
        val snapshot = _uiState.value
        if (!snapshot.canStart || !startMutex.tryLock()) return
        val request = ToolboxStartRequest(
            tab = snapshot.currentTab,
            gachaOnce = snapshot.gachaOnce,
            context = TaskStartContext(TaskStartMode.MANUAL),
        )
        if (request.tab == ToolboxTab.GACHA && !snapshot.gachaDisclaimerAccepted) {
            _uiState.update { it.copy(statusMessage = uiTextOf(R.string.gacha_need_disclaimer)) }
            startMutex.unlock()
            return
        }
        onStart(request)
    }

    private fun onStart(request: ToolboxStartRequest) {
        viewModelScope.launch {
            setReadinessInProgress(true)
            try {
                when (val readiness = checkGameReadiness(
                    clientType = chainState.clientType,
                    launchesGame = false,
                    context = request.context,
                )) {
                    is GameReadiness.RequiresConfirmation -> {
                        val pending = request.copy(
                            context = request.context.acknowledged(readiness.acknowledgement)
                        )
                        _uiState.update {
                            it.copy(
                                pendingStartRequest = pending,
                                dialog = appContext.createStartWarningDialog(
                                    appContext.resolveTaskStartConfirmationMessage(readiness.acknowledgement)
                                ),
                            )
                        }
                        return@launch
                    }

                    is GameReadiness.Blocked -> {
                        _uiState.update {
                            it.copy(
                                pendingStartRequest = null,
                                dialog = appContext.createStartBlockedDialog(
                                    appContext.resolveTaskStartBlockedMessage(readiness.reason)
                                ),
                            )
                        }
                        return@launch
                    }

                    is GameReadiness.Ready -> _uiState.update { it.copy(pendingStartRequest = null) }
                }
                if (request.tab == ToolboxTab.GACHA &&
                    appSettingsManager.runMode.value == RunMode.FOREGROUND
                ) {
                    return@launch
                }
                doStart(request)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update {
                    it.copy(
                        pendingStartRequest = null,
                        statusMessage = uiTextOf(R.string.task_start_error_start_failed),
                    )
                }
            } finally {
                setReadinessInProgress(false)
                startMutex.unlock()
            }
        }
    }

    private suspend fun doStart(request: ToolboxStartRequest) {
        when (request.tab) {
            ToolboxTab.MINI_GAME -> {
                miniGame.onStart()
            }
            ToolboxTab.GACHA -> {
                doStartGacha(request.gachaOnce)
            }
            ToolboxTab.RECRUIT_CALC -> {
                onStartRecruitCalc()
            }
            ToolboxTab.DEPOT -> {
                onStartDepot()
            }
            ToolboxTab.OPER_BOX -> {
                onStartOperBox()
            }
        }
    }

    private suspend fun doStartGacha(once: Boolean) {
        _uiState.update { it.copy(statusMessage = uiTextOf(R.string.toolbox_status_starting_gacha)) }
        val params = buildJsonObject {
            put("task_names", buildJsonArray { add(JsonPrimitive(gachaTaskName(once))) })
        }.toString()
        val result = compositionService.startCopilot(
            listOf(MaaTaskParams(MaaTaskType.CUSTOM, params)),
        )
        handleStartResult(result, uiTextOf(R.string.toolbox_status_gacha_started))
        if (result is MaaCompositionService.StartResult.Success) {
            startGachaTipRotation()
        }
    }

    private fun startGachaTipRotation() {
        gachaTipJob?.cancel()
        gachaTipJob = viewModelScope.launch {
            while (isActive) {
                val tipRes = GACHA_TIP_RES_IDS[Random.nextInt(GACHA_TIP_RES_IDS.size)]
                _uiState.update { it.copy(gachaTip = uiTextOf(tipRes)) }
                delay(5_000)
            }
        }
    }

    private fun stopGachaTipRotation() {
        gachaTipJob?.cancel()
        gachaTipJob = null
        _uiState.update { it.copy(gachaTip = uiTextOf(R.string.gacha_init_tip)) }
    }

    private fun clearGachaUiState() {
        _uiState.update { it.clearGachaTransientState() }
        stopGachaTipRotation()
    }

    fun onDialogConfirm() {
        when (_uiState.value.dialog?.confirmAction) {
            PanelDialogConfirmAction.CONFIRM_PENDING_START -> {
                val pending = _uiState.value.pendingStartRequest
                _uiState.update { it.copy(dialog = null, pendingStartRequest = null) }
                if (pending != null && startMutex.tryLock()) onStart(pending)
            }

            else -> _uiState.update { it.copy(dialog = null) }
        }
    }

    fun onDialogDismiss() {
        val pending = _uiState.value.pendingStartRequest
        _uiState.update { it.copy(dialog = null, pendingStartRequest = null) }
        if (pending?.tab == ToolboxTab.GACHA) clearGachaUiState()
    }

    fun onStop() {
        if (!_uiState.value.canStop || stopJob?.isActive == true) return
        stopGachaTipRotation()
        stopJob = viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = uiTextOf(R.string.toolbox_status_stopping)) }
            try {
                compositionService.stop()
                _uiState.update { it.copy(statusMessage = uiTextOf(R.string.toolbox_status_stopped)) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(statusMessage = uiTextOf(R.string.task_start_error_start_failed)) }
            }
        }
    }

    // ==================== 公招识别 ====================

    private suspend fun onStartRecruitCalc() {
        collector.clearRecruit()
        _uiState.update { it.copy(statusMessage = uiTextOf(R.string.toolbox_status_starting_recruit_calc)) }
        val cfg = _recruitConfig.value
        val selectList = buildJsonArray {
            if (cfg.chooseLevel3) add(3)
            if (cfg.chooseLevel4) add(4)
            if (cfg.chooseLevel5) add(5)
            if (cfg.chooseLevel6) add(6)
        }
        val params = buildJsonObject {
            put("select", selectList)
            put("confirm", buildJsonArray { add(JsonPrimitive(-1)) })
            put("times", 0)
            put("set_time", cfg.autoSetTime)
            put("expedite", false)
            if (cfg.autoSetTime) {
                put("recruitment_time", buildJsonObject {
                    put("3", cfg.level3Time)
                    put("4", cfg.level4Time)
                    put("5", cfg.level5Time)
                })
            }
        }
        handleStartResult(
            compositionService.startCopilot(listOf(MaaTaskParams(MaaTaskType.RECRUIT, params.toString())))
        )
    }

    // ==================== 仓库识别 ====================

    private suspend fun onStartDepot() {
        // 不 clear 持久化快照：识别失败时仍可看历史；成功 set 后自动刷新
        _uiState.update { it.copy(statusMessage = uiTextOf(R.string.toolbox_status_starting_depot)) }
        handleStartResult(
            compositionService.startCopilot(listOf(MaaTaskParams(MaaTaskType.DEPOT, "{}")))
        )
    }

    // ==================== 干员识别 ====================

    private suspend fun onStartOperBox() {
        _uiState.update { it.copy(statusMessage = uiTextOf(R.string.toolbox_status_starting_oper_box)) }
        handleStartResult(
            compositionService.startCopilot(listOf(MaaTaskParams(MaaTaskType.OPER_BOX, "{}")))
        )
    }

    // ==================== 导出（与屏幕同源：Repository 快照）====================

    fun exportDepotArkPlanner(): String {
        val items = depotRepository.snapshot.value.toSortedItems(itemHelper.items.value)
        val itemsJson = items.joinToString(",") { """{"id":"${it.id}","have":${it.count}}""" }
        return """{"@type":"@penguin-statistics/depot","items":[$itemsJson]}"""
    }

    fun exportDepotLolicon(): String {
        val items = depotRepository.snapshot.value.toSortedItems(itemHelper.items.value)
        return "{${items.joinToString(",") { "\"${it.id}\":${it.count}" }}}"
    }

    /** 干员识别导出列表：owned + notOwned（全部可用干员）。 */
    fun exportOperBoxList(): List<OperBoxOperator> {
        val snap = operBoxRepository.snapshot.value
        if (!snap.hasSynced) return emptyList()
        return snap.owned + snap.notOwned
    }

    /** 干员识别导出为 JSON（剪贴板与 .json 文件共用）。 */
    fun exportOperBox(): String = OperBoxExportFormatter.toJson(exportOperBoxList())

    private fun handleStartResult(
        result: MaaCompositionService.StartResult,
        successMessage: UiText = uiTextOf(R.string.toolbox_status_started),
    ) {
        _uiState.update { it.copy(statusMessage = appContext.formatStartResult(result, successMessage)) }
    }

    private fun setReadinessInProgress(inProgress: Boolean) {
        readinessInProgress = inProgress
        updateExecutionControls(compositionService.state.value)
    }

    private fun updateExecutionControls(executionState: MaaExecutionState) {
        _uiState.update {
            it.copy(
                isStarting = readinessInProgress || executionState == MaaExecutionState.STARTING,
                canStart = !readinessInProgress &&
                    (executionState == MaaExecutionState.IDLE || executionState == MaaExecutionState.ERROR),
                canStop = canStopToolboxExecution(executionState),
            )
        }
    }

    companion object {
        /** 对齐 WPF GachaTip1..17 */
        private val GACHA_TIP_RES_IDS = intArrayOf(
            R.string.gacha_tip_1,
            R.string.gacha_tip_2,
            R.string.gacha_tip_3,
            R.string.gacha_tip_4,
            R.string.gacha_tip_5,
            R.string.gacha_tip_6,
            R.string.gacha_tip_7,
            R.string.gacha_tip_8,
            R.string.gacha_tip_9,
            R.string.gacha_tip_10,
            R.string.gacha_tip_11,
            R.string.gacha_tip_12,
            R.string.gacha_tip_13,
            R.string.gacha_tip_14,
            R.string.gacha_tip_15,
            R.string.gacha_tip_16,
            R.string.gacha_tip_17,
        )
    }
}
