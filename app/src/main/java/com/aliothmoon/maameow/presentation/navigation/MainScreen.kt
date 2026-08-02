package com.aliothmoon.maameow.presentation.navigation

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.aliothmoon.maameow.constant.Routes
import com.aliothmoon.maameow.presentation.enableBenchmarkTestTags
import com.aliothmoon.maameow.presentation.view.background.BackgroundTaskView
import com.aliothmoon.maameow.presentation.view.home.HomeView
import com.aliothmoon.maameow.presentation.view.settings.SettingsView
import com.aliothmoon.maameow.schedule.service.ScheduledLaunchInbox
import com.aliothmoon.maameow.schedule.ui.ScheduleListView
import com.aliothmoon.maameow.theme.MaaAnimations
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.koinInject
import kotlin.math.abs

internal val LocalMainBottomBarPadding = compositionLocalOf<Dp> { 0.dp }

@Composable
fun MainScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    onViewAnnouncement: () -> Unit = {},
    fullscreen: Boolean = false,
) {
    val scheduledLaunchInbox: ScheduledLaunchInbox = koinInject()
    val pagerState = rememberPagerState(pageCount = { BottomNavTab.all.size })
    val tabStateHolder = rememberSaveableStateHolder()
    val scope = rememberCoroutineScope()
    var predictiveBackActive by remember { mutableStateOf(false) }
    var backgroundPageActivated by remember { mutableStateOf(false) }
    val backgroundPageIndex = BottomNavTab.all.indexOf(BottomNavTab.BACKGROUND)
    val backgroundPageVisible = pagerState.layoutInfo.visiblePagesInfo.any {
        it.index == backgroundPageIndex
    }
    val shouldComposeBackground = backgroundPageActivated ||
        backgroundPageVisible ||
        pagerState.currentPage == backgroundPageIndex ||
        pagerState.targetPage == backgroundPageIndex
    val latestOnViewAnnouncement by rememberUpdatedState(onViewAnnouncement)
    val homeContent = remember(navController) {
        movableContentOf {
            HomeView(
                navController = navController,
                onViewAnnouncement = latestOnViewAnnouncement,
            )
        }
    }

    // targetPage：点击/滑动一旦确定目标即生效，停稳后等于 currentPage。
    // animateScrollToPage 内部走 MutatorMutex，连续调用时后者自动接管，无需手动取消。
    fun goToPage(index: Int) {
        if (index !in BottomNavTab.all.indices || index == pagerState.targetPage) return
        scope.launch {
            val distance = abs(index - pagerState.currentPage).coerceAtLeast(1)
            pagerState.animateScrollToPage(
                page = index,
                animationSpec = tween(
                    durationMillis = 100 * distance + 100,
                    easing = MaaAnimations.springEasing,
                ),
            )
        }
    }

    // 非首页 Tab 按返回键先回到首页；全屏由 BackgroundTaskView 自行处理。
    PredictiveBackHandler(
        enabled = !fullscreen && (predictiveBackActive || pagerState.targetPage != 0),
    ) { events ->
        val startPage = pagerState.currentPage
        val startPageOffset = pagerState.currentPageOffsetFraction
        val pageSize = pagerState.layoutInfo.pageSize.toFloat()
        val startPosition = startPage + startPageOffset
        var appliedOffset = 0f
        try {
            events.collect { event ->
                predictiveBackActive = true
                val targetOffset = -startPosition * pageSize * event.progress
                val delta = targetOffset - appliedOffset
                pagerState.scroll {
                    appliedOffset += scrollBy(delta)
                }
            }
            pagerState.scrollToPage(0)
        } catch (exception: CancellationException) {
            withContext(NonCancellable) {
                pagerState.scrollToPage(startPage, startPageOffset)
            }
            throw exception
        } finally {
            predictiveBackActive = false
        }
    }

    LaunchedEffect(shouldComposeBackground) {
        if (shouldComposeBackground) backgroundPageActivated = true
    }

    // 定时任务触发时：若正处于子页面，先弹回主 Tab 浮出主界面，再滑到后台任务页
    // （恢复旧导航 navigate(BACKGROUND){popUpTo(HOME)} 的“自动浮出后台页”语义）。
    val pendingScheduledExecution by scheduledLaunchInbox.pending.collectAsStateWithLifecycle()
    LaunchedEffect(pendingScheduledExecution?.requestId) {
        if (pendingScheduledExecution != null) {
            navController.popBackStack(Routes.HOME, false)
            goToPage(BottomNavTab.all.indexOf(BottomNavTab.BACKGROUND))
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .enableBenchmarkTestTags(),
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        bottomBar = {
            if (!fullscreen) {
                AppBottomNavigation(
                    currentRoute = BottomNavTab.all[pagerState.targetPage].route,
                    onTabSelected = { tab -> goToPage(BottomNavTab.all.indexOf(tab)) },
                )
            }
        },
    ) { paddingValues ->
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { BottomNavTab.all[it].route },
            userScrollEnabled = !fullscreen,
            beyondViewportPageCount = 1,
        ) { page ->
            val tab = BottomNavTab.all[page]
            tabStateHolder.SaveableStateProvider(tab.route) {
                CompositionLocalProvider(
                    LocalMainBottomBarPadding provides
                        paddingValues.calculateBottomPadding(),
                ) {
                    when (BottomNavTab.all[page]) {
                        BottomNavTab.HOME -> homeContent()

                        BottomNavTab.BACKGROUND -> {
                            if (shouldComposeBackground) {
                                BackgroundTaskView(navController = navController)
                            }
                        }

                        BottomNavTab.SCHEDULE -> ScheduleListView(navController = navController)
                        BottomNavTab.SETTINGS -> SettingsView(navController = navController)
                    }
                }
            }
        }
    }
}
