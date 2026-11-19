package com.example.ecouganda;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.Espresso.pressBack;
import static androidx.test.espresso.action.ViewActions.actionWithAssertions;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static com.example.ecouganda.EcoTestUtils.NAV_ITEMS;
import static com.example.ecouganda.EcoTestUtils.NAV_TITLES;
import static com.example.ecouganda.EcoTestUtils.assertOnlySelected;
import static com.example.ecouganda.EcoTestUtils.isTablet;
import static com.example.ecouganda.EcoTestUtils.openDrawerIfPhone;
import static com.example.ecouganda.EcoTestUtils.selectDrawerItem;
import static org.hamcrest.Matchers.not;
import static org.junit.Assume.assumeFalse;

import android.view.InputDevice;
import android.view.MotionEvent;

import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.GeneralLocation;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Requirement: navigation drawer opening/closing, item selection, highlighting
 * and toolbar title updates, on every device in the matrix.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class NavigationDrawerTest {

    @Rule
    public ActivityScenarioRule<MainActivity> rule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void startsOnDashboardWithDashboardHighlighted() {
        onView(withId(R.id.screen_title)).check(matches(withText(R.string.dashboard_title)));
        assertOnlySelected(R.id.nav_dashboard);
    }

    @Test
    public void phone_menuButtonOpensDrawer_scrimClosesIt() {
        assumeFalse("Phones only: tablets have a permanent sidebar", isTablet());

        onView(withId(R.id.mobile_drawer)).check(matches(not(isDisplayed())));
        onView(withId(R.id.menu_toggle)).perform(click());
        onView(withId(R.id.mobile_drawer)).check(matches(isDisplayed()));
        onView(withId(R.id.drawer_scrim)).check(matches(isDisplayed()));

        // tap the dimmed area to the right of the drawer
        onView(withId(R.id.drawer_scrim)).perform(actionWithAssertions(new GeneralClickAction(
                Tap.SINGLE, GeneralLocation.CENTER_RIGHT, Press.FINGER,
                InputDevice.SOURCE_UNKNOWN, MotionEvent.BUTTON_PRIMARY)));
        onView(withId(R.id.mobile_drawer)).check(matches(not(isDisplayed())));
    }

    @Test
    public void phone_menuButtonTogglesDrawerClosed() {
        assumeFalse(isTablet());
        onView(withId(R.id.menu_toggle)).perform(click());
        onView(withId(R.id.mobile_drawer)).check(matches(isDisplayed()));
        onView(withId(R.id.menu_toggle)).perform(click());
        onView(withId(R.id.mobile_drawer)).check(matches(not(isDisplayed())));
    }

    @Test
    public void phone_backClosesDrawerBeforeLeavingScreen() {
        assumeFalse(isTablet());
        openDrawerIfPhone();
        pressBack();
        onView(withId(R.id.mobile_drawer)).check(matches(not(isDisplayed())));
        onView(withId(R.id.screen_title)).check(matches(withText(R.string.dashboard_title)));
    }

    @Test
    public void everyItem_updatesTitle_highlightsItself_andClosesPhoneDrawer() {
        // go through the items in reverse so every step is a real change of page
        for (int i = NAV_ITEMS.length - 1; i >= 0; i--) {
            selectDrawerItem(NAV_ITEMS[i]);

            onView(withId(R.id.screen_title)).check(matches(withText(NAV_TITLES[i])));
            assertOnlySelected(NAV_ITEMS[i]);
            if (!isTablet()) {
                onView(withId(R.id.mobile_drawer)).check(matches(not(isDisplayed())));
            }
        }
    }

    @Test
    public void reselectingCurrentItem_keepsTitleAndHighlight() {
        selectDrawerItem(R.id.nav_projects);
        selectDrawerItem(R.id.nav_projects);
        onView(withId(R.id.screen_title)).check(matches(withText(R.string.nav_projects)));
        assertOnlySelected(R.id.nav_projects);
    }

    @Test
    public void openingAReport_keepsReportsHighlighted() {
        selectDrawerItem(R.id.nav_reports);
        onView(withId(R.id.report_dumping)).perform(click());
        onView(withId(R.id.screen_title)).check(matches(withText(R.string.report_detail_title)));
        assertOnlySelected(R.id.nav_reports);
    }
}
