package com.aliothmoon.maameow.benchmark;

import androidx.benchmark.macro.junit4.BaselineProfileRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;
import kotlin.Unit;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public final class BaselineProfileGenerator {
    private static final String PACKAGE_NAME = "com.walker874.maameow";

    @Rule
    public final BaselineProfileRule rule = new BaselineProfileRule();

    @Test
    public void generate() {
        rule.collect(PACKAGE_NAME, scope -> {
            scope.pressHome();
            scope.startActivityAndWait();
            UiDevice device = scope.getDevice();
            ensureHome(device);
            scroll(device);
            open(device, "nav_background_task", "background_page");
            UiObject2 editProfile = device.wait(
                    Until.findObject(By.res("edit_task_profile")),
                    15_000L);
            if (editProfile != null) {
                editProfile.click();
                requireObject(device, "task_profile_editor");
                device.pressBack();
            }
            open(device, "nav_settings", "settings_list");
            scroll(device);
            return Unit.INSTANCE;
        });
    }

    private static void open(UiDevice device, String navigationTag, String pageTag) {
        requireObject(device, navigationTag).click();
        requireObject(device, pageTag);
        device.waitForIdle();
    }

    private static UiObject2 requireObject(UiDevice device, String id) {
        UiObject2 object = device.wait(Until.findObject(By.res(id)), 15_000L);
        if (object == null) throw new AssertionError("Missing UI object: " + id);
        return object;
    }

    private static void ensureHome(UiDevice device) {
        if (device.findObject(By.res("home_list")) != null) return;
        requireObject(device, "nav_home").click();
        requireObject(device, "home_list");
    }

    private static void scroll(UiDevice device) {
        int centerX = device.getDisplayWidth() / 2;
        int bottom = device.getDisplayHeight() * 3 / 4;
        int top = device.getDisplayHeight() / 4;
        device.swipe(centerX, bottom, centerX, top, 12);
        device.swipe(centerX, top, centerX, bottom, 12);
    }
}
