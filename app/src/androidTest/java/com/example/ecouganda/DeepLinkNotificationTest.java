package com.example.ecouganda;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.ecouganda.EcoTestUtils.assertOnlySelected;
import static com.example.ecouganda.EcoTestUtils.context;
import static com.example.ecouganda.EcoTestUtils.currentPageFragment;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.SystemClock;

import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.By;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject2;
import androidx.test.uiautomator.Until;

import com.example.ecouganda.navigation.DeepLinks;
import com.example.ecouganda.notifications.ReportNotifications;
import com.example.ecouganda.ui.ReportsFragment;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Requirement: tapping a push notification navigates to ReportsFragment with
 * the correct reportId argument.
 *
 * The first tests launch the exact Intent the notification carries (fast and
 * stable on every device). The last test is end to end: it posts the real
 * notification, opens the shade with UI Automator and taps it.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class DeepLinkNotificationTest {

    private static final String REPORT_ID = "RPT-2041";          // not the default report
    private static final String REPORT_TITLE = "Illegal dumping near wetland";
    private static final long TIMEOUT_MS = 10_000;
    private static final String SYSTEM_UI = "com.android.systemui";

    private ActivityScenario<MainActivity> scenario;

    /**
     * True when a second intent was delivered to the running MainActivity (onNewIntent).
     * MainActivity calls setIntent() there, after which ActivityScenario no longer matches the
     * activity to the intent it launched: it misses the DESTROYED event and close() times out
     * after 45 s even though the activity really finished (seen on Test Lab API 30 and 34).
     */
    private boolean deliveredNewIntent;

    @Before
    public void grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    context().getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @After
    public void cleanUp() {
        // A shade left open by a failed test keeps window focus away from the app, which made
        // every later test on the device fail with RootViewWithoutFocusException (Test Lab API 26).
        closeNotificationShade();
        NotificationManagerCompat.from(context()).cancelAll();
        if (scenario == null) return;
        if (deliveredNewIntent) {
            scenario.onActivity(Activity::finish);
        } else {
            scenario.close();
        }
    }

    private static void closeNotificationShade() {
        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        for (int i = 0; i < 2 && device.hasObject(By.res(SYSTEM_UI, "notification_stack_scroller")); i++) {
            device.pressBack();   // first press may only collapse quick settings
            device.waitForIdle();
        }
    }

    @Test
    public void notificationIntent_opensReportsFragmentWithReportId() {
        Intent intent = ReportNotifications.buildIntent(context(), REPORT_ID);
        scenario = ActivityScenario.launch(intent);

        assertReportShown(REPORT_ID);
    }

    @Test
    public void deepLinkUri_opensReportsFragmentWithReportId() {
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("ecouganda://reports/rpt-2048"))
                .setClass(context(), MainActivity.class);
        scenario = ActivityScenario.launch(intent);

        assertReportShown("RPT-2048"); // id is normalised to upper case
    }

    @Test
    public void fcmTrayExtra_opensReportsFragmentWithReportId() {
        // what Android delivers when the system tray shows an FCM "notification" payload
        Intent intent = new Intent(context(), MainActivity.class)
                .putExtra(DeepLinks.EXTRA_REPORT_ID, REPORT_ID);
        scenario = ActivityScenario.launch(intent);

        assertReportShown(REPORT_ID);
    }

    @Test
    public void unknownReportId_fallsBackToReportsList() {
        scenario = ActivityScenario.launch(ReportNotifications.buildIntent(context(), "RPT-9999"));

        Fragment f = currentPageFragment(scenario);
        assertTrue("expected ReportsFragment, got " + f, f instanceof ReportsFragment);
        assertNull(((ReportsFragment) f).getReportId());
        onView(withId(R.id.screen_title)).check(matches(withText(R.string.nav_reports)));
    }

    @Test
    public void notificationWhileAppOpen_navigatesWithoutNewTask() {
        scenario = ActivityScenario.launch(MainActivity.class);
        onView(withId(R.id.screen_title)).check(matches(withText(R.string.dashboard_title)));

        // singleTop: delivered to onNewIntent of the running activity
        deliveredNewIntent = true;
        context().startActivity(ReportNotifications.buildIntent(context(), REPORT_ID));
        // onNewIntent arrives asynchronously from the system; on Test Lab API 34 the first check
        // ran before it and saw the dashboard (flaky). Wait for the report instead of checking once.
        waitForReport(REPORT_ID);

        assertReportShown(REPORT_ID);
    }

    @Test
    public void tappingRealNotification_opensReport() {
        Context ctx = context();
        assumeTrue("notifications must be allowed on this device", ReportNotifications.canPost(ctx));
        // On the Test Lab Android 8.0 image UI Automator does not find the posted notification in
        // the shade (two runs, even after a second swipe), although it is posted. The five tests
        // above launch the same intent the tap produces and pass on API 26, so only this
        // system-UI-dependent check is skipped there. It runs on API 27+ (passes on 30 and 34).
        assumeTrue("notification shade not reachable by UI Automator on API 26",
                Build.VERSION.SDK_INT > Build.VERSION_CODES.O);

        scenario = ActivityScenario.launch(MainActivity.class);
        assertTrue(ReportNotifications.show(ctx, REPORT_ID, REPORT_TITLE, "Wakiso District - needs review"));

        assertTrue("notification was not posted", waitUntilPosted(ctx));

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.openNotification();
        UiObject2 notification = device.wait(Until.findObject(By.textContains(REPORT_TITLE)), TIMEOUT_MS);
        if (notification == null) {
            // Older shades (Android 8) can open only part-way on the first swipe
            device.openNotification();
            notification = device.wait(Until.findObject(By.textContains(REPORT_TITLE)), TIMEOUT_MS);
        }
        assertNotNull("notification not visible in the shade", notification);
        deliveredNewIntent = true; // the tap reaches the running activity via onNewIntent
        notification.click();

        // wait until the app is back in front and showing the report
        assertTrue(device.wait(Until.hasObject(By.pkg(ctx.getPackageName()).depth(0)), TIMEOUT_MS));
        assertTrue(device.wait(Until.hasObject(By.textContains(REPORT_ID)), TIMEOUT_MS));
        waitForReport(REPORT_ID);

        assertReportShown(REPORT_ID);
    }

    /** The notification manager posts asynchronously; wait until ours is active before opening the shade. */
    private static boolean waitUntilPosted(Context ctx) {
        NotificationManager nm = ctx.getSystemService(NotificationManager.class);
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline) {
            if (nm.getActiveNotifications().length > 0) return true;
            SystemClock.sleep(100);
        }
        return false;
    }

    /** Waits until the running activity shows ReportsFragment for {@code reportId} (or times out). */
    private void waitForReport(String reportId) {
        long deadline = SystemClock.uptimeMillis() + TIMEOUT_MS;
        while (SystemClock.uptimeMillis() < deadline) {
            Fragment f = currentPageFragment(scenario);
            if (f instanceof ReportsFragment && reportId.equals(((ReportsFragment) f).getReportId())) return;
            SystemClock.sleep(100);
        }
    }

    private void assertReportShown(String expectedId) {
        Fragment f = currentPageFragment(scenario);
        assertTrue("expected ReportsFragment, got " + f, f instanceof ReportsFragment);
        assertEquals("reportId argument", expectedId,
                f.requireArguments().getString(ReportsFragment.ARG_REPORT_ID));

        onView(withId(R.id.screen_title)).check(matches(withText(R.string.report_detail_title)));
        onView(withId(R.id.report_detail_id)).check(matches(withText(containsString(expectedId))));
        assertOnlySelected(R.id.nav_reports);
    }
}
