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
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;

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

    private ActivityScenario<MainActivity> scenario;

    @Before
    public void grantNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().getUiAutomation().grantRuntimePermission(
                    context().getPackageName(), Manifest.permission.POST_NOTIFICATIONS);
        }
    }

    @After
    public void cleanUp() {
        NotificationManagerCompat.from(context()).cancelAll();
        if (scenario != null) scenario.close();
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
        context().startActivity(ReportNotifications.buildIntent(context(), REPORT_ID));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();

        assertReportShown(REPORT_ID);
    }

    @Test
    public void tappingRealNotification_opensReport() {
        Context ctx = context();
        assumeTrue("notifications must be allowed on this device", ReportNotifications.canPost(ctx));

        scenario = ActivityScenario.launch(MainActivity.class);
        assertTrue(ReportNotifications.show(ctx, REPORT_ID, REPORT_TITLE, "Wakiso District - needs review"));

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        device.openNotification();
        UiObject2 notification = device.wait(Until.findObject(By.text(REPORT_TITLE)), TIMEOUT_MS);
        assertNotNull("notification not visible in the shade", notification);
        notification.click();

        // wait until the app is back in front and showing the report
        assertTrue(device.wait(Until.hasObject(By.pkg(ctx.getPackageName()).depth(0)), TIMEOUT_MS));
        assertTrue(device.wait(Until.hasObject(By.textContains(REPORT_ID)), TIMEOUT_MS));

        assertReportShown(REPORT_ID);
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
