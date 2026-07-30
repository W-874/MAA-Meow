package com.aliothmoon.maameow.presentation.viewmodel

import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.domain.models.RunMode
import com.aliothmoon.maameow.domain.usecase.TaskStartContext
import com.aliothmoon.maameow.presentation.view.panel.PanelDialogUiState
import com.aliothmoon.maameow.utils.i18n.UiText
import com.aliothmoon.maameow.utils.i18n.uiTextOf

/** Immutable start target captured before the readiness check begins. */
data class ToolboxStartRequest(
    val tab: ToolboxTab,
    val gachaOnce: Boolean,
    val context: TaskStartContext,
)

/** Single observable state for the toolbox shell and its global controls. */
data class ToolboxUiState(
    val visibleTabs: List<ToolboxTab> = ToolboxTab.visibleFor(RunMode.BACKGROUND),
    val currentTab: ToolboxTab = ToolboxTab.MINI_GAME,
    val gachaDisclaimerAccepted: Boolean = false,
    val gachaOnce: Boolean = true,
    val gachaTip: UiText = uiTextOf(R.string.gacha_init_tip),
    val statusMessage: UiText = UiText.Empty,
    val dialog: PanelDialogUiState? = null,
    val isStarting: Boolean = false,
    val canStart: Boolean = true,
    val canStop: Boolean = false,
    val pendingStartRequest: ToolboxStartRequest? = null,
)

sealed interface ToolboxAction {
    data class SelectTab(val tab: ToolboxTab) : ToolboxAction
    data class SelectGachaMode(val once: Boolean) : ToolboxAction
    data object AgreeGachaDisclaimer : ToolboxAction
    data object CancelGacha : ToolboxAction
    data object Start : ToolboxAction
    data object Stop : ToolboxAction
    data object ConfirmDialog : ToolboxAction
    data object DismissDialog : ToolboxAction
}

internal fun ToolboxUiState.clearGachaTransientState(): ToolboxUiState = copy(
    gachaOnce = true,
    gachaTip = uiTextOf(R.string.gacha_init_tip),
    statusMessage = UiText.Empty,
)
