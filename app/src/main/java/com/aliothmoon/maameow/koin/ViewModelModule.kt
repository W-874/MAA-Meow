package com.aliothmoon.maameow.koin

import com.aliothmoon.maameow.presentation.viewmodel.AchievementViewModel
import com.aliothmoon.maameow.presentation.viewmodel.AppEventsViewModel
import com.aliothmoon.maameow.presentation.viewmodel.BackgroundTaskViewModel
import com.aliothmoon.maameow.presentation.viewmodel.CopilotViewModel
import com.aliothmoon.maameow.presentation.viewmodel.ErrorLogViewModel
import com.aliothmoon.maameow.presentation.viewmodel.ExpandedControlPanelViewModel
import com.aliothmoon.maameow.presentation.viewmodel.HomeViewModel
import com.aliothmoon.maameow.presentation.viewmodel.LogHistoryViewModel
import com.aliothmoon.maameow.presentation.viewmodel.NotificationSettingsViewModel
import com.aliothmoon.maameow.presentation.viewmodel.SettingsViewModel
import com.aliothmoon.maameow.presentation.viewmodel.TaskOverrideEditorViewModel
import com.aliothmoon.maameow.presentation.viewmodel.ToolboxViewModel
import com.aliothmoon.maameow.presentation.viewmodel.UpdateViewModel
import com.aliothmoon.maameow.schedule.ui.ScheduleEditViewModel
import com.aliothmoon.maameow.schedule.ui.ScheduleListViewModel
import com.aliothmoon.maameow.schedule.ui.ScheduleTriggerLogViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module


val viewModelModule = module {
    viewModel {
        HomeViewModel(
            application = get(),
            appSettingsManager = get(),
            overlayController = lazy { get() },
            permissionManager = get(),
            resourceLoadStateStore = get(),
            executionStateStore = get(),
            resourceInitService = get(),
            overlayStateStore = get(),
        )
    }
    viewModelOf(::AchievementViewModel)
    viewModelOf(::AppEventsViewModel)
    viewModelOf(::SettingsViewModel)
    viewModel {
        UpdateViewModel(
            appContext = get(),
            updateServiceProvider = lazy { get() },
            appSettingsManager = get(),
            maaResourceLoader = lazy { get() },
            pathConfig = get(),
            initializationCoordinator = get(),
        )
    }
    viewModelOf(::LogHistoryViewModel)
    viewModelOf(::ErrorLogViewModel)
    viewModelOf(::BackgroundTaskViewModel)
    viewModelOf(::ScheduleListViewModel)
    viewModelOf(::ScheduleEditViewModel)
    viewModelOf(::ScheduleTriggerLogViewModel)
    viewModelOf(::NotificationSettingsViewModel)
    viewModelOf(::TaskOverrideEditorViewModel)
}


val floatingWindowModule = module {
    singleOf(::ExpandedControlPanelViewModel)
    singleOf(::CopilotViewModel)
    singleOf(::ToolboxViewModel)
}
