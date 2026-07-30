package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.presentation.viewmodel.ToolboxTab
import com.aliothmoon.maameow.presentation.viewmodel.ToolboxViewModel
import com.aliothmoon.maameow.theme.MaaDesignTokens
import org.koin.compose.koinInject

@Composable
fun ToolboxPanel(
    modifier: Modifier = Modifier,
    viewModel: ToolboxViewModel = koinInject()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.sm),
    ) {
        ToolboxNavigation(
            tabs = uiState.visibleTabs,
            currentTab = uiState.currentTab,
            onSelect = viewModel::onTabChange,
        )

        when (uiState.currentTab) {
            ToolboxTab.MINI_GAME -> MiniGamePanel(
                delegate = viewModel.miniGame,
                modifier = Modifier.weight(1f),
            )
            ToolboxTab.GACHA -> GachaPanel(
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
            )
            ToolboxTab.RECRUIT_CALC -> RecruitCalcPanel(
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
            )
            ToolboxTab.DEPOT -> DepotRecognitionPanel(
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
            )
            ToolboxTab.OPER_BOX -> OperBoxPanel(
                viewModel = viewModel,
                modifier = Modifier.weight(1f),
            )
        }
    }
}
