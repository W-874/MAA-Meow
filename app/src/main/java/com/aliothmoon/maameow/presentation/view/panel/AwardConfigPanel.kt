package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.AwardConfig
import com.aliothmoon.maameow.presentation.components.ExpressiveSwitch
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingRow

/**
 * 领取配置面板 - 迁移自 WPF AwardSettingsUserControl.xaml
 */
@Composable
fun AwardConfigPanel(
    config: AwardConfig,
    onConfigChange: (AwardConfig) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(top = 2.dp, bottom = 4.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SegmentedSettingsGroup {
            item {
                AwardSwitchRow(
                    title = stringResource(R.string.panel_award_daily_weekly),
                    checked = config.award,
                    onCheckedChange = { onConfigChange(config.copy(award = it)) },
                )
            }
            item {
                AwardSwitchRow(
                    title = stringResource(R.string.panel_award_mail),
                    checked = config.mail,
                    onCheckedChange = { onConfigChange(config.copy(mail = it)) },
                )
            }
            item {
                AwardSwitchRow(
                    title = stringResource(R.string.panel_award_free_gacha),
                    description = stringResource(R.string.panel_award_free_gacha_tip),
                    checked = config.freeGacha,
                    onCheckedChange = { onConfigChange(config.copy(freeGacha = it)) },
                )
            }
            item {
                AwardSwitchRow(
                    title = stringResource(R.string.panel_award_orundum),
                    checked = config.orundum,
                    onCheckedChange = { onConfigChange(config.copy(orundum = it)) },
                )
            }
            item {
                AwardSwitchRow(
                    title = stringResource(R.string.panel_award_mining),
                    checked = config.mining,
                    onCheckedChange = { onConfigChange(config.copy(mining = it)) },
                )
            }
            item {
                AwardSwitchRow(
                    title = stringResource(R.string.panel_award_special_access),
                    checked = config.specialAccess,
                    onCheckedChange = { onConfigChange(config.copy(specialAccess = it)) },
                )
            }
        }
    }
}

@Composable
private fun AwardSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    description: String? = null,
) {
    SettingRow(
        title = title,
        description = description,
        icon = null,
        onClick = { onCheckedChange(!checked) },
        trailing = {
            ExpressiveSwitch(
                checked = checked,
                onCheckedChange = onCheckedChange,
            )
        },
    )
}
