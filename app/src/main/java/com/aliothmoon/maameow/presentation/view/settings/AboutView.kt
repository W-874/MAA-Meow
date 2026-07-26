package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.NavController
import com.aliothmoon.maameow.BuildConfig
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.domain.service.AchievementReporter
import com.aliothmoon.maameow.presentation.components.LogExportController
import com.aliothmoon.maameow.presentation.components.SectionHeader
import com.aliothmoon.maameow.presentation.components.SegmentedSettingsGroup
import com.aliothmoon.maameow.presentation.components.SettingRow
import com.aliothmoon.maameow.utils.Misc
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AboutView(
    navController: NavController,
    achievementReporter: AchievementReporter = koinInject(),
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    var showExportSheet by remember { mutableStateOf(false) }
    val appIcon = remember(context) {
        context.packageManager.getApplicationIcon(context.packageName)
            .toBitmap(width = 144, height = 144)
            .asImageBitmap()
    }

    LogExportController(
        sheetVisible = showExportSheet,
        onSheetDismiss = { showExportSheet = false },
    )

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(stringResource(R.string.settings_section_about)) },
                navigationIcon = {
                    Row {
                        IconButton(onClick = { navController.navigateUp() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                            )
                        }
                        Spacer(Modifier.size(16.dp))
                    }
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = paddingValues.calculateTopPadding(),
                bottom = paddingValues.calculateBottomPadding() + 16.dp,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(top = 8.dp, bottom = 12.dp),
                ) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                        ),
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Image(
                                bitmap = appIcon,
                                contentDescription = null,
                                modifier = Modifier.size(56.dp),
                            )
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleLarge,
                            )
                            Text(
                                text = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }
            }

            item {
                AboutGroup(title = stringResource(R.string.settings_section_about)) {
                    item {
                        AboutNavigationItem(
                            icon = Icons.Rounded.Code,
                            title = stringResource(R.string.about_source_code_title),
                            description = stringResource(R.string.about_source_code_desc),
                        ) {
                            Misc.openUriSafely(context, "https://github.com/W-874/MAA-Meow")
                        }
                    }
                    item {
                        AboutNavigationItem(
                            icon = Icons.Rounded.Forum,
                            title = stringResource(R.string.settings_about_qq_group_title),
                            description = stringResource(R.string.settings_about_qq_group_desc),
                        ) {
                            achievementReporter.reportFeedbackGroupOpened()
                            Misc.openUriSafely(context, "https://qm.qq.com/q/j4CFbeDQXu")
                        }
                    }
                }
            }

            item {
                AboutGroup(title = stringResource(R.string.about_debug_section)) {
                    item {
                        AboutNavigationItem(
                            icon = Icons.Rounded.BugReport,
                            title = stringResource(R.string.settings_log_error_title),
                            description = stringResource(R.string.settings_log_error_desc),
                        ) {
                            navController.navigate(Routes.ERROR_LOG)
                        }
                    }
                    item {
                        SettingRow(
                            icon = Icons.Rounded.FolderZip,
                            title = stringResource(R.string.settings_log_export_title),
                            description = stringResource(R.string.settings_log_export_desc),
                            onClick = { showExportSheet = true },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AboutGroup(
    title: String,
    content: com.aliothmoon.maameow.presentation.components.SegmentedSettingsScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        SectionHeader(title)
        SegmentedSettingsGroup(
            modifier = Modifier.padding(horizontal = 16.dp),
            content = content,
        )
    }
}

@Composable
private fun AboutNavigationItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    SettingRow(
        icon = icon,
        title = title,
        description = description,
        trailing = {
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                contentDescription = null,
            )
        },
        onClick = onClick,
    )
}
