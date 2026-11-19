package com.example.ecouganda;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.ecouganda.EcoTestUtils.assertOnlySelected;
import static com.example.ecouganda.EcoTestUtils.context;
import static com.example.ecouganda.EcoTestUtils.currentPageFragment;
import static com.example.ecouganda.EcoTestUtils.isTablet;
import static com.example.ecouganda.EcoTestUtils.openDrawerIfPhone;
import static com.example.ecouganda.EcoTestUtils.resetOrientation;
import static com.example.ecouganda.EcoTestUtils.rotate;
import static com.example.ecouganda.EcoTestUtils.selectDrawerItem;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.ecouganda.notifications.ReportNotifications;
import com.example.ecouganda.ui.ReportsFragment;

import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Requirement: after screen rotation the selected drawer item, current fragment
 * and its arguments are preserved, on phones and tablets.
 *
 * rotate() is a real orientation change (activity destroyed and recreated);
 * scenario.recreate() is the same configuration-change path without a layout switch.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class ConfigurationChangeTest {

    private ActivityScenario<MainActivity> scenario;

    @After
    public void tearDown() {
        if (scenario != null) {
            resetOrientation(scenario);
            scenario.close();
        }
    }

    @Test
    public void selectedItemAndTitle_surviveRotation() {
        scenario = ActivityScenario.launch(MainActivity.class);
        selectDrawerItem(R.id.nav_schools);

        rotate(scenario, true);
        onView(withId(R.id.screen_title)).check(matches(withText(R.string.nav_schools)));
        assertOnlySelected(R.id.nav_schools);

        rotate(scenario, false);
        onView(withId(R.id.screen_title)).check(matches(withText(R.string.nav_schools)));
        assertOnlySelected(R.id.nav_schools);
    }

    @Test
    public void reportsFragmentAndReportIdArgument_surviveRotation() {
        scenario = ActivityScenario.launch(ReportNotifications.buildIntent(context(), "RPT-2041"));
        assertReportFragment("RPT-2041");

        rotate(scenario, true);
        assertReportFragment("RPT-2041");

        rotate(scenario, false);
        assertReportFragment("RPT-2041");
    }

    @Test
    public void reportsList_survivesRecreate_withoutGainingAReportId() {
        scenario = ActivityScenario.launch(MainActivity.class);
        selectDrawerItem(R.id.nav_reports);

        scenario.recreate();

        Fragment f = currentPageFragment(scenario);
        assertTrue(f instanceof ReportsFragment);
        assertEquals(null, ((ReportsFragment) f).getReportId());
        onView(withId(R.id.screen_title)).check(matches(withText(R.string.nav_reports)));
        assertOnlySelected(R.id.nav_reports);
    }

    @Test
    public void phone_openDrawer_staysOpenAfterRotation() {
        assumeFalse(isTablet());
        scenario = ActivityScenario.launch(MainActivity.class);
        selectDrawerItem(R.id.nav_projects);
        openDrawerIfPhone();

        rotate(scenario, true);
        onView(withId(R.id.mobile_drawer)).check(matches(isDisplayed()));
        assertOnlySelected(R.id.nav_projects);
    }

    @Test
    public void tablet_permanentDrawer_inBothOrientations() {
        assumeTrue(isTablet());
        scenario = ActivityScenario.launch(MainActivity.class);
        selectDrawerItem(R.id.nav_projects);

        rotate(scenario, true);
        onView(withId(R.id.permanent_drawer)).check(matches(isDisplayed()));
        assertOnlySelected(R.id.nav_projects);

        rotate(scenario, false);
        onView(withId(R.id.permanent_drawer)).check(matches(isDisplayed()));
        assertOnlySelected(R.id.nav_projects);
    }

    private void assertReportFragment(String expectedId) {
        Fragment f = currentPageFragment(scenario);
        assertTrue("expected ReportsFragment, got " + f, f instanceof ReportsFragment);
        assertEquals(expectedId, f.requireArguments().getString(ReportsFragment.ARG_REPORT_ID));
        onView(withId(R.id.screen_title)).check(matches(withText(R.string.report_detail_title)));
        onView(withId(R.id.report_detail_id)).check(matches(withText(containsString(expectedId))));
        assertOnlySelected(R.id.nav_reports);
    }
}
