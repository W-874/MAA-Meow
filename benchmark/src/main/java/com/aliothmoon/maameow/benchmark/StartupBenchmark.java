package com.aliothmoon.maameow.benchmark;

import android.os.SystemClock;
import androidx.annotation.OptIn;
import androidx.benchmark.macro.ArtMetric;
import androidx.benchmark.macro.CompilationMode;
import androidx.benchmark.macro.ExperimentalMetricApi;
import androidx.benchmark.macro.FrameTimingMetric;
import androidx.benchmark.macro.MemoryUsageMetric;
import androidx.benchmark.macro.Metric;
import androidx.benchmark.macro.StartupMode;
import androidx.benchmark.macro.StartupTimingMetric;
import androidx.benchmark.macro.TraceSectionMetric;
import androidx.benchmark.macro.junit4.MacrobenchmarkRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import java.util.Arrays;
import java.util.List;
import java.io.IOException;
import kotlin.Unit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
@OptIn(markerClass = ExperimentalMetricApi.class)
public final class StartupBenchmark {
    private static final String PACKAGE_NAME = "com.walker874.maameow";
    private static final long UI_TIMEOUT_MS = 15_000L;

    @Rule
    public final MacrobenchmarkRule benchmarkRule = new MacrobenchmarkRule();

    @Test
    public void coldStartup() {
        measureStartup(StartupMode.COLD);
    }

    @Test
    public void warmStartup() {
        measureStartup(StartupMode.WARM);
    }

    @Test
    public void hotStartup() {
        measureStartup(StartupMode.HOT);
    }

    @Test
    public void homeScroll() {
        measurePageInteraction("nav_home", "home_list", device -> scroll(device));
    }

    @Test
    public void settingsPage() {
        measurePageInteraction("nav_settings", "settings_list", device -> scroll(device));
    }

    @Test
    public void backgroundTaskPage() {
        measurePageInteraction("nav_background_task", "background_page", device -> scroll(device));
    }

    @Test
    public void firstResourceInitialization() {
        benchmarkRule.measureRepeated(
                PACKAGE_NAME,
                interactionMetrics(),
                CompilationMode.DEFAULT,
                StartupMode.COLD,
                3,
                scope -> {
                    clearAppData(scope.getDevice());
                    scope.pressHome();
                    return Unit.INSTANCE;
                },
                scope -> {
                    scope.startActivityAndWait();
                    UiDevice device = scope.getDevice();
                    dismissBlockingPermissionDialog(device);
                    requireObject(device, "home_list");
                    if (!device.wait(Until.hasObject(By.res("resource_init_ready")), 120_000L)) {
                        throw new AssertionError("Resource initialization did not finish");
                    }
                    dismissIfPresent(device, "确认", "Confirm");
                    return Unit.INSTANCE;
                });
    }

    private void measureStartup(StartupMode mode) {
        benchmarkRule.measureRepeated(
                PACKAGE_NAME,
                startupMetrics(),
                CompilationMode.DEFAULT,
                mode,
                5,
                scope -> {
                    scope.pressHome();
                    return Unit.INSTANCE;
                },
                scope -> {
                    scope.startActivityAndWait();
                    requireObject(scope.getDevice(), "home_list");
                    return Unit.INSTANCE;
                });
    }

    private void measurePageInteraction(
            String navigationTag,
            String pageTag,
            DeviceAction action) {
        benchmarkRule.measureRepeated(
                PACKAGE_NAME,
                interactionMetrics(),
                CompilationMode.DEFAULT,
                null,
                5,
                scope -> {
                    scope.startActivityAndWait();
                    ensureHome(scope.getDevice());
                    return Unit.INSTANCE;
                },
                scope -> {
                    UiDevice device = scope.getDevice();
                    requireObject(device, navigationTag).click();
                    requireObject(device, pageTag);
                    action.run(device);
                    device.waitForIdle();
                    return Unit.INSTANCE;
                });
    }

    private static List<Metric> startupMetrics() {
        return Arrays.asList(
                new StartupTimingMetric(),
                new FrameTimingMetric(),
                memoryMetric(),
                new ArtMetric(),
                new TraceSectionMetric("MaaNyan.firstFrame"),
                new TraceSectionMetric("MaaNyan.firstInteractive"));
    }

    private static List<Metric> interactionMetrics() {
        return Arrays.asList(
                new FrameTimingMetric(),
                memoryMetric(),
                new ArtMetric(),
                new TraceSectionMetric("ResourceInitService.extract"));
    }

    private static MemoryUsageMetric memoryMetric() {
        return new MemoryUsageMetric(
                MemoryUsageMetric.Mode.Max,
                Arrays.asList(
                        MemoryUsageMetric.SubMetric.HeapSize,
                        MemoryUsageMetric.SubMetric.RssAnon,
                        MemoryUsageMetric.SubMetric.RssFile,
                        MemoryUsageMetric.SubMetric.RssShmem,
                        MemoryUsageMetric.SubMetric.Gpu));
    }

    private static UiObject2 requireObject(UiDevice device, String id) {
        UiObject2 object = device.wait(Until.findObject(By.res(id)), UI_TIMEOUT_MS);
        if (object == null) throw new AssertionError("Missing UI object: " + id);
        return object;
    }

    private static void ensureHome(UiDevice device) {
        if (device.findObject(By.res("home_list")) != null) return;
        requireObject(device, "nav_home").click();
        requireObject(device, "home_list");
    }

    private static void clearAppData(UiDevice device) {
        try {
            device.executeShellCommand("pm clear " + PACKAGE_NAME);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to clear app data", error);
        }
    }

    private static void dismissBlockingPermissionDialog(UiDevice device) {
        long deadline = SystemClock.uptimeMillis() + UI_TIMEOUT_MS;
        while (device.findObject(By.res("home_list")) == null
                && SystemClock.uptimeMillis() < deadline) {
            dismissIfPresent(
                    device,
                    "允许",
                    "始终允许",
                    "仅在使用该应用时允许",
                    "Allow",
                    "Allow all the time",
                    "While using the app");
            device.wait(Until.findObject(By.res("home_list")), 500L);
        }
    }

    private static void dismissIfPresent(UiDevice device, String... labels) {
        for (String label : labels) {
            UiObject2 object = device.findObject(By.text(label));
            if (object != null && object.isEnabled()) {
                object.click();
                device.waitForIdle();
                return;
            }
        }
    }

    private static void scroll(UiDevice device) {
        int centerX = device.getDisplayWidth() / 2;
        int bottom = device.getDisplayHeight() * 3 / 4;
        int top = device.getDisplayHeight() / 4;
        device.swipe(centerX, bottom, centerX, top, 12);
        device.swipe(centerX, top, centerX, bottom, 12);
    }

    private interface DeviceAction {
        void run(UiDevice device);
    }
}
