package com.example.ecouganda;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.isSelected;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.not;

import android.content.Context;
import android.content.pm.ActivityInfo;

import androidx.fragment.app.Fragment;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;

import java.util.concurrent.atomic.AtomicReference;

/** Helpers shared by the device tests. Works on phones and tablets. */
final class EcoTestUtils {

    /** Drawer items, in drawer order. */
    static final int[] NAV_ITEMS = {
            R.id.nav_dashboard, R.id.nav_projects, R.id.nav_schools, R.id.nav_reports, R.id.nav_settings};

    /** Expected toolbar title for each drawer item (same order as NAV_ITEMS). */
    static final int[] NAV_TITLES = {
            R.string.dashboard_title, R.string.nav_projects, R.string.nav_schools,
            R.string.nav_reports, R.string.nav_settings};

    private EcoTestUtils() { }

    static Context context() {
        return ApplicationProvider.getApplicationContext();
    }

    /** Same resource the app uses: true when smallest width >= 600dp. */
    static boolean isTablet() {
        return context().getResources().getBoolean(R.bool.is_tablet);
    }

    static int smallestWidthDp() {
        return context().getResources().getConfiguration().smallestScreenWidthDp;
    }

    /** Opens the drawer on phones; on tablets the sidebar is already visible. */
    static void openDrawerIfPhone() {
        if (isTablet()) return;
        onView(withId(R.id.menu_toggle)).perform(click());
        onView(withId(R.id.mobile_drawer)).check(matches(isDisplayed()));
    }

    /** Taps a drawer item the way a user would on this device. */
    static void selectDrawerItem(int navId) {
        openDrawerIfPhone();
        if (isTablet()) {
            onView(withId(navId)).perform(click());
        } else {
            // the phone drawer scrolls; needed on small screens in landscape
            onView(withId(navId)).perform(scrollTo(), click());
        }
    }

    /** Exactly one drawer item is highlighted. */
    static void assertOnlySelected(int navId) {
        for (int id : NAV_ITEMS) {
            onView(withId(id)).check(matches(id == navId ? isSelected() : not(isSelected())));
        }
    }

    /** The Fragment currently hosted in page_container (null for layout-based pages). */
    static Fragment currentPageFragment(ActivityScenario<MainActivity> scenario) {
        AtomicReference<Fragment> ref = new AtomicReference<>();
        scenario.onActivity(a -> {
            a.getSupportFragmentManager().executePendingTransactions();
            ref.set(a.getSupportFragmentManager().findFragmentById(R.id.page_container));
        });
        return ref.get();
    }

    /** Real orientation change: the activity is destroyed and recreated by the system. */
    static void rotate(ActivityScenario<MainActivity> scenario, boolean landscape) {
        scenario.onActivity(a -> a.setRequestedOrientation(landscape
                ? ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_PORTRAIT));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }

    static void resetOrientation(ActivityScenario<MainActivity> scenario) {
        scenario.onActivity(a -> a.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED));
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
    }
}
