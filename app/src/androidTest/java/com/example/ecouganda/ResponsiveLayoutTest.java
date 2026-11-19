package com.example.ecouganda;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static com.example.ecouganda.EcoTestUtils.isTablet;
import static com.example.ecouganda.EcoTestUtils.selectDrawerItem;
import static com.example.ecouganda.EcoTestUtils.smallestWidthDp;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeFalse;
import static org.junit.Assume.assumeTrue;

import android.util.Log;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.MediumTest;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Requirement: the drawer is hidden on phones and permanently open on tablets (&gt;= 600dp).
 * Each device in the Test Lab matrix runs the branch that applies to it; the
 * first test proves the app picked the right layout for that screen.
 */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class ResponsiveLayoutTest {

    private static final String TAG = "ResponsiveLayoutTest";

    @Rule
    public ActivityScenarioRule<MainActivity> rule = new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void layoutModeMatchesSmallestScreenWidth() {
        int sw = smallestWidthDp();
        Log.i(TAG, "smallestScreenWidthDp=" + sw + " tablet=" + isTablet());
        assertEquals("is_tablet must be true exactly when smallest width >= 600dp", sw >= 600, isTablet());
    }

    @Test
    public void phone_drawerHiddenUntilMenuTapped() {
        assumeFalse("phone-only check", isTablet());
        onView(withId(R.id.menu_toggle)).check(matches(isDisplayed()));
        onView(withId(R.id.mobile_drawer)).check(matches(not(isDisplayed())));
        onView(withId(R.id.permanent_drawer)).check(doesNotExist());
    }

    @Test
    public void tablet_drawerPermanentlyOpen_noMenuButton() {
        assumeTrue("tablet-only check", isTablet());
        onView(withId(R.id.permanent_drawer)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_dashboard)).check(matches(isDisplayed()));
        onView(withId(R.id.nav_settings)).check(matches(isDisplayed()));
        onView(withId(R.id.menu_toggle)).check(doesNotExist());
        onView(withId(R.id.mobile_drawer)).check(doesNotExist());
    }

    @Test
    public void tablet_drawerStaysOpenAfterSelectingItems() {
        assumeTrue(isTablet());
        selectDrawerItem(R.id.nav_schools);
        onView(withId(R.id.permanent_drawer)).check(matches(isDisplayed()));
        selectDrawerItem(R.id.nav_reports);
        onView(withId(R.id.permanent_drawer)).check(matches(isDisplayed()));
    }
}
