package com.aliothmoon.maameow.presentation.view.background

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.R
import com.aliothmoon.maameow.data.model.TaskTypeInfo
import com.aliothmoon.maameow.presentation.components.AdaptiveTaskPromptDialog
import com.aliothmoon.maameow.presentation.components.ITextField
import com.aliothmoon.maameow.presentation.view.panel.TaskConfigPanel
import com.aliothmoon.maameow.presentation.view.panel.TaskListPanel
import com.aliothmoon.maameow.presentation.view.panel.taskTypeLabel
import com.aliothmoon.maameow.presentation.view.panel.taskTypeDescription
import com.aliothmoon.maameow.presentation.view.panel.taskTypeIcon
import com.aliothmoon.maameow.presentation.viewmodel.BackgroundTaskViewModel
import com.aliothmoon.maameow.theme.MaaDesignTokens

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun TaskProfileEditorView(
    navController: NavController,
    viewModel: BackgroundTaskViewModel,
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val nodes by viewModel.chainState.chain.collectAsStateWithLifecycle()
    val profiles by viewModel.chainState.profiles.collectAsStateWithLifecycle()
    val activeProfileId by viewModel.chainState.activeProfileId.collectAsStateWithLifecycle()
    val activeProfile = profiles.find { it.id == activeProfileId }
    val selectedNode = nodes.find { it.id == state.selectedNodeId }

    var showDetail by rememberSaveable { mutableStateOf(false) }
    var showTaskPicker by rememberSaveable { mutableStateOf(false) }
    var menuExpanded by rememberSaveable { mutableStateOf(false) }
    var showRenameProfile by rememberSaveable { mutableStateOf(false) }
    var showDeleteProfile by rememberSaveable { mutableStateOf(false) }
    var showRenameTask by rememberSaveable { mutableStateOf(false) }
    var showDeleteTask by rememberSaveable { mutableStateOf(false) }
    var renameValue by rememberSaveable { mutableStateOf("") }
    var predictiveBackProgress by remember { mutableFloatStateOf(0f) }

    val detailVisible = showDetail && selectedNode != null

    fun closeDetail() {
        viewModel.onDismissTaskEditorDetail()
        showDetail = false
        menuExpanded = false
    }

    fun closeEditor() {
        viewModel.onCloseTaskEditor()
        navController.navigateUp()
    }

    val pageTitle = if (detailVisible) {
        selectedNode?.name.orEmpty()
    } else {
        stringResource(
            R.string.panel_profile_editor_named_title,
            activeProfile?.name.orEmpty(),
        )
    }

    PredictiveBackHandler(enabled = detailVisible) { progressEvents ->
        try {
            progressEvents.collect { event ->
                predictiveBackProgress = event.progress
            }
            closeDetail()
        } finally {
            predictiveBackProgress = 0f
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.onCloseTaskEditor() }
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            LargeFlexibleTopAppBar(
                title = { Text(pageTitle) },
                navigationIcon = {
                    Row {
                        IconButton(
                            onClick = {
                                if (detailVisible) closeDetail() else closeEditor()
                            },
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = null,
                            )
                        }
                        Spacer(Modifier.size(16.dp))
                    }
                },
                actions = {
                    if (!detailVisible) {
                        IconButton(
                            onClick = {
                                renameValue = activeProfile?.name.orEmpty()
                                showRenameProfile = true
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.common_rename),
                            )
                        }
                    }
                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = null,
                            )
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                        ) {
                            if (detailVisible && selectedNode != null) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.common_rename)) },
                                    leadingIcon = {
                                        Icon(Icons.Default.Edit, contentDescription = null)
                                    },
                                    onClick = {
                                        renameValue = selectedNode.name
                                        showRenameTask = true
                                        menuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.panel_config_duplicate_task))
                                    },
                                    leadingIcon = {
                                        Icon(Icons.Default.ContentCopy, contentDescription = null)
                                    },
                                    onClick = {
                                        viewModel.onDuplicateNode(selectedNode.id)
                                        menuExpanded = false
                                    },
                                )
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.panel_config_delete_task))
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    onClick = {
                                        showDeleteTask = true
                                        menuExpanded = false
                                    },
                                )
                            } else {
                                DropdownMenuItem(
                                    text = {
                                        Text(stringResource(R.string.panel_profile_delete_action))
                                    },
                                    leadingIcon = {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.error,
                                        )
                                    },
                                    enabled = profiles.size > 1,
                                    onClick = {
                                        showDeleteProfile = true
                                        menuExpanded = false
                                    },
                                )
                            }
                        }
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
        AnimatedContent(
            targetState = detailVisible,
            transitionSpec = {
                val transform = if (targetState) {
                    (slideInHorizontally(tween(220)) { width -> width / 5 } + fadeIn(tween(140)))
                        .togetherWith(
                            slideOutHorizontally(tween(180)) { width -> -width / 12 } +
                                    fadeOut(tween(120)),
                        )
                } else {
                    (slideInHorizontally(tween(220)) { width -> -width / 12 } + fadeIn(tween(140)))
                        .togetherWith(
                            slideOutHorizontally(tween(180)) { width -> width / 5 } +
                                    fadeOut(tween(120)),
                        )
                }
                transform.using(null)
            },
            label = "taskProfileDetail",
        ) { showingDetail ->
        if (showingDetail) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .navigationBarsPadding()
                    .padding(
                        horizontal = MaaDesignTokens.Spacing.listHorizontal,
                        vertical = MaaDesignTokens.Spacing.sm,
                    )
                    .graphicsLayer {
                        translationX = size.width * predictiveBackProgress
                        alpha = 1f - predictiveBackProgress * 0.15f
                    },
            ) {
                TaskConfigPanel(
                        selectedNode = selectedNode,
                        isEditMode = false,
                        isAddingTask = false,
                        isProfileMode = false,
                        profiles = profiles,
                        activeProfileId = activeProfileId,
                        onConfigChange = { config ->
                            selectedNode?.id?.let { viewModel.onNodeConfigChange(it, config) }
                        },
                        onAddNode = viewModel::onAddNode,
                        onRemoveNode = { nodeId ->
                            viewModel.onRemoveNode(nodeId)
                            closeDetail()
                        },
                        onDuplicateNode = viewModel::onDuplicateNode,
                        onRenameNode = viewModel::onRenameNode,
                        onSwitchProfile = viewModel::onSwitchProfile,
                        onRenameProfile = viewModel::onRenameProfile,
                        onDuplicateProfile = viewModel::onDuplicateProfile,
                        onDeleteProfile = viewModel::onDeleteProfile,
                        onCreateProfile = viewModel::onCreateProfile,
                        onReorderProfile = viewModel::onReorderProfile,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .navigationBarsPadding()
                    .padding(horizontal = MaaDesignTokens.Spacing.listHorizontal),
            ) {
                Spacer(modifier = Modifier.height(MaaDesignTokens.Spacing.sm))

                Text(
                    text = stringResource(R.string.panel_profile_task_list),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = stringResource(R.string.panel_task_list_drag_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )

                Spacer(modifier = Modifier.height(MaaDesignTokens.Spacing.md))

                TaskListPanel(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    nodes = nodes,
                    selectedNodeId = state.selectedNodeId,
                    isEditMode = false,
                    isAddingTask = false,
                    isProfileMode = false,
                    onNodeEnabledChange = viewModel::onNodeEnabledChange,
                    onNodeSelected = { nodeId ->
                        viewModel.onNodeSelected(nodeId)
                        showDetail = true
                    },
                    onNodeMove = viewModel::onNodeMove,
                    onToggleEditMode = viewModel::onToggleEditMode,
                    onToggleAddingTask = { showTaskPicker = true },
                    onToggleProfileMode = viewModel::onToggleProfileMode,
                    showManagementActions = false,
                    useIntrinsicWidth = false,
                    expressiveStyle = true,
                )

                Spacer(modifier = Modifier.height(MaaDesignTokens.Spacing.sm))
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTaskPicker = true },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.size(12.dp))
                        Text(
                            text = stringResource(R.string.panel_task_list_add),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MaaDesignTokens.Spacing.sm))
            }
        }
        }
    }

    if (showTaskPicker) {
        TaskTypePickerBottomSheet(
            onDismiss = { showTaskPicker = false },
            onSelected = { typeInfo ->
                viewModel.onAddNode(typeInfo)
                showTaskPicker = false
            },
        )
    }

    AdaptiveTaskPromptDialog(
        visible = showRenameProfile,
        title = stringResource(R.string.panel_profile_rename_title),
        onDismissRequest = { showRenameProfile = false },
        onConfirm = {
            val name = renameValue.trim()
            if (name.isNotEmpty()) {
                activeProfile?.id?.let { viewModel.onRenameProfile(it, name) }
            }
            showRenameProfile = false
        },
        content = {
            ITextField(
                value = renameValue,
                onValueChange = { if (it.length <= 20) renameValue = it },
                label = stringResource(R.string.panel_profile_name_label),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            )
        },
    )

    AdaptiveTaskPromptDialog(
        visible = showDeleteProfile,
        title = stringResource(R.string.panel_profile_delete_title),
        message = stringResource(
            R.string.panel_profile_delete_message,
            activeProfile?.name.orEmpty(),
        ),
        icon = Icons.Default.Delete,
        confirmColor = MaterialTheme.colorScheme.error,
        confirmText = stringResource(R.string.common_delete),
        onDismissRequest = { showDeleteProfile = false },
        onConfirm = {
            activeProfile?.id?.let { viewModel.onDeleteProfile(it) }
            showDeleteProfile = false
            closeEditor()
        },
    )

    AdaptiveTaskPromptDialog(
        visible = showRenameTask,
        title = stringResource(R.string.panel_task_rename_title),
        onDismissRequest = { showRenameTask = false },
        onConfirm = {
            val name = renameValue.trim()
            if (name.isNotEmpty()) {
                selectedNode?.id?.let { viewModel.onRenameNode(it, name) }
            }
            showRenameTask = false
        },
        content = {
            ITextField(
                value = renameValue,
                onValueChange = { if (it.length <= 20) renameValue = it },
                label = stringResource(R.string.panel_config_task_name_label),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
            )
        },
    )

    AdaptiveTaskPromptDialog(
        visible = showDeleteTask,
        title = stringResource(R.string.panel_task_delete_title),
        message = stringResource(
            R.string.panel_task_delete_message,
            selectedNode?.name.orEmpty(),
        ),
        icon = Icons.Default.Delete,
        confirmColor = MaterialTheme.colorScheme.error,
        confirmText = stringResource(R.string.common_delete),
        onDismissRequest = { showDeleteTask = false },
        onConfirm = {
            selectedNode?.id?.let { viewModel.onRemoveNode(it) }
            showDeleteTask = false
            closeDetail()
        },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TaskTypePickerBottomSheet(
    onDismiss: () -> Unit,
    onSelected: (TaskTypeInfo) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
        ) {
            Text(
                text = stringResource(R.string.panel_config_select_type),
                style = MaterialTheme.typography.titleLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp),
            )
            LazyColumn(
                modifier = Modifier.heightIn(max = 560.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                items(TaskTypeInfo.entries) { typeInfo ->
                    val itemIndex = TaskTypeInfo.entries.indexOf(typeInfo)
                    val itemShape = when {
                        TaskTypeInfo.entries.size == 1 -> androidx.compose.foundation.shape.RoundedCornerShape(16.dp)
                        itemIndex == 0 -> androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 16.dp,
                            topEnd = 16.dp,
                            bottomStart = 5.dp,
                            bottomEnd = 5.dp,
                        )
                        itemIndex == TaskTypeInfo.entries.lastIndex -> androidx.compose.foundation.shape.RoundedCornerShape(
                            topStart = 5.dp,
                            topEnd = 5.dp,
                            bottomStart = 16.dp,
                            bottomEnd = 16.dp,
                        )
                        else -> androidx.compose.foundation.shape.RoundedCornerShape(5.dp)
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelected(typeInfo) },
                        shape = itemShape,
                        color = MaterialTheme.colorScheme.surfaceBright,
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = taskTypeIcon(typeInfo),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp),
                            )
                            Spacer(modifier = Modifier.size(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = taskTypeLabel(typeInfo),
                                    style = MaterialTheme.typography.titleMedium,
                                )
                                Text(
                                    text = taskTypeDescription(typeInfo),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
        }
    }
}
