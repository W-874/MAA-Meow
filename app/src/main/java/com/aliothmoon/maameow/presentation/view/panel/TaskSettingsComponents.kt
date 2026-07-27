package com.aliothmoon.maameow.presentation.view.panel

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Loop
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.AwardConfig
import com.aliothmoon.maameow.data.model.DepotMaintainConfig
import com.aliothmoon.maameow.data.model.FightConfig
import com.aliothmoon.maameow.data.model.InfrastConfig
import com.aliothmoon.maameow.data.model.MallConfig
import com.aliothmoon.maameow.data.model.ReclamationConfig
import com.aliothmoon.maameow.data.model.RecruitConfig
import com.aliothmoon.maameow.data.model.RoguelikeConfig
import com.aliothmoon.maameow.data.model.TaskParamProvider
import com.aliothmoon.maameow.data.model.TaskTypeInfo
import com.aliothmoon.maameow.data.model.UserDataUpdateConfig
import com.aliothmoon.maameow.data.model.WakeUpConfig

@Composable
fun TaskSettingsSectionTitle(
    title: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Normal,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 16.dp, top = 8.dp, bottom = 4.dp),
    )
}

@Composable
fun TaskSettingsBlock(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    androidx.compose.foundation.layout.Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(2.dp),
        content = content,
    )
}

fun taskConfigIcon(config: TaskParamProvider): ImageVector = when (config) {
    is WakeUpConfig -> Icons.Default.PlayArrow
    is RecruitConfig -> Icons.Default.Search
    is InfrastConfig -> Icons.Default.Home
    is FightConfig -> Icons.Default.Loop
    is MallConfig -> Icons.Default.ShoppingCart
    is AwardConfig -> Icons.Default.Star
    is RoguelikeConfig -> Icons.Default.Settings
    is ReclamationConfig -> Icons.Default.Build
    is UserDataUpdateConfig -> Icons.Default.Refresh
    is DepotMaintainConfig -> Icons.Default.Inventory2
}

fun taskTypeIcon(typeInfo: TaskTypeInfo): ImageVector = taskConfigIcon(typeInfo.defaultConfig())

@Composable
fun taskTypeDescription(typeInfo: TaskTypeInfo): String = stringResource(
    when (typeInfo) {
        TaskTypeInfo.WAKE_UP -> R.string.task_type_wake_up_description
        TaskTypeInfo.RECRUITING -> R.string.task_type_recruiting_description
        TaskTypeInfo.BASE -> R.string.task_type_base_description
        TaskTypeInfo.COMBAT -> R.string.task_type_combat_description
        TaskTypeInfo.MALL -> R.string.task_type_mall_description
        TaskTypeInfo.MISSION -> R.string.task_type_mission_description
        TaskTypeInfo.AUTO_ROGUELIKE -> R.string.task_type_auto_roguelike_description
        TaskTypeInfo.RECLAMATION -> R.string.task_type_reclamation_description
        TaskTypeInfo.USER_DATA_UPDATE -> R.string.task_type_user_data_update_description
        TaskTypeInfo.DEPOT_MAINTAIN -> R.string.task_type_depot_maintain_description
    },
)

@Composable
fun taskConfigSummary(config: TaskParamProvider): String = when (config) {
    is WakeUpConfig -> stringResource(
        R.string.panel_task_summary_wakeup,
        config.clientType,
    )
    is RecruitConfig -> stringResource(
        R.string.panel_task_summary_recruit,
        config.maxRecruitTimes,
    )
    is InfrastConfig -> stringResource(
        R.string.panel_task_summary_infrast,
        config.mode.name,
    )
    is FightConfig -> if (config.hasTimesLimited) {
        stringResource(R.string.panel_task_summary_fight_limited, config.maxTimes)
    } else {
        stringResource(R.string.panel_task_summary_fight_unlimited)
    }
    is MallConfig -> stringResource(
        R.string.panel_task_summary_mall,
        listOf(config.visitFriends, config.shopping, config.creditFight).count { it },
    )
    is AwardConfig -> stringResource(
        R.string.panel_task_summary_award,
        listOf(
            config.award,
            config.mail,
            config.freeGacha,
            config.orundum,
            config.mining,
            config.specialAccess,
        ).count { it },
    )
    is RoguelikeConfig -> stringResource(
        R.string.panel_task_summary_roguelike,
        config.theme,
    )
    is ReclamationConfig -> stringResource(
        R.string.panel_task_summary_reclamation,
        config.theme,
    )
    is UserDataUpdateConfig -> stringResource(
        R.string.panel_task_summary_user_data_update,
        listOf(config.updateOperBox, config.updateDepot).count { it },
    )
    is DepotMaintainConfig -> stringResource(
        R.string.panel_task_summary_depot_maintain,
        config.plans.size,
    )
}
