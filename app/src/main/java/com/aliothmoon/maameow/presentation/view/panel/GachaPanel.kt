package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.presentation.components.AdaptiveTaskPromptDialog
import com.aliothmoon.maameow.presentation.components.InfoCard
import com.aliothmoon.maameow.presentation.components.RainbowFlowText
import com.aliothmoon.maameow.presentation.navigation.LocalMainBottomBarPadding
import com.aliothmoon.maameow.presentation.viewmodel.ToolboxViewModel
import com.aliothmoon.maameow.theme.MaaDesignTokens
import com.aliothmoon.maameow.utils.i18n.asString

/**
 * 牛牛抽卡内容区（对齐 MaaWpfGui Toolbox Gacha）。
 * 寻访次数在此选择，仍由后台任务页的圆形主按钮统一启动。
 */
@Composable
fun GachaPanel(
    modifier: Modifier = Modifier,
    viewModel: ToolboxViewModel,
) {
    val disclaimerAccepted by viewModel.gachaDisclaimerAccepted.collectAsStateWithLifecycle()
    val gachaOnce by viewModel.gachaOnce.collectAsStateWithLifecycle()
    val tip by viewModel.gachaTip.collectAsStateWithLifecycle()
    val status by viewModel.statusMessage.collectAsStateWithLifecycle()
    val mainBottomBarPadding = LocalMainBottomBarPadding.current
    val statusText = status.asString()
    var showWarning by remember { mutableStateOf(false) }

    AdaptiveTaskPromptDialog(
        visible = showWarning,
        title = stringResource(R.string.gacha_warning_title),
        message = stringResource(R.string.gacha_warning),
        onDismissRequest = {
            showWarning = false
            viewModel.onGachaCancel()
        },
        onConfirm = {
            showWarning = false
            viewModel.onGachaAgreeDisclaimer()
        },
        dismissText = stringResource(R.string.common_cancel),
    )

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = MaaDesignTokens.Spacing.listHorizontal,
            top = MaaDesignTokens.Spacing.xs,
            end = MaaDesignTokens.Spacing.listHorizontal,
            bottom = mainBottomBarPadding + MaaDesignTokens.Spacing.xs,
        ),
        verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.sm),
    ) {
        item {
            Text(
                text = stringResource(R.string.toolbox_tab_gacha),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
        }

        if (!disclaimerAccepted) {
            item {
                InfoCard(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.lg),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(MaaDesignTokens.Spacing.xs),
                        ) {
                            Text(
                                text = stringResource(R.string.gacha_disclaimer_head),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                textAlign = TextAlign.Center,
                            )
                            RainbowFlowText(
                                text = stringResource(R.string.gacha_disclaimer_emphasize),
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.ExtraBold,
                                textAlign = TextAlign.Center,
                            )
                        }
                        Button(
                            onClick = { showWarning = true },
                            shape = RoundedCornerShape(MaaDesignTokens.CornerRadius.button),
                        ) {
                            Text(
                                text = stringResource(R.string.gacha_agree_disclaimer),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                    }
                }
            }
        } else {
            item {
                InfoCard(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Text(
                        text = tip.asString(),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }

            if (statusText.isNotBlank()) {
                item {
                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = MaaDesignTokens.Spacing.md),
                    )
                }
            }

            item {
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    GachaMode.entries.forEachIndexed { index, mode ->
                        SegmentedButton(
                            selected = gachaOnce == mode.once,
                            onClick = { viewModel.onGachaModeChange(mode.once) },
                            shape = SegmentedButtonDefaults.itemShape(
                                index = index,
                                count = GachaMode.entries.size,
                                baseShape = RoundedCornerShape(MaaDesignTokens.CornerRadius.button),
                            ),
                        ) {
                            Text(stringResource(mode.labelRes))
                        }
                    }
                }
            }
        }
    }
}

private enum class GachaMode(val once: Boolean, val labelRes: Int) {
    ONCE(once = true, labelRes = R.string.gacha_once),
    TEN_TIMES(once = false, labelRes = R.string.gacha_ten_times),
}
