package com.team.mogolog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.app.Instrumentation;
import android.os.SystemClock;
import android.view.InputDevice;
import android.view.MotionEvent;
import android.view.View;
import android.widget.CheckBox;
import android.widget.ListView;

import androidx.test.core.app.ActivityScenario;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MainNavigationTest {

    @Test
    public void homeReviewAndExampleDataStayInsideHome() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertVisibleTab(scenario, R.id.nav_home, R.id.home_title);
            scenario.onActivity(activity -> {
                activity.findViewById(R.id.home_demo_action).performClick();
                activity.findViewById(R.id.home_review_action).performClick();
                Fragment home = activity.getSupportFragmentManager().getPrimaryNavigationFragment();
                assertNotNull(home);
                FragmentManager homeChildren = home.getChildFragmentManager();
                HomeReviewDialog review = (HomeReviewDialog) homeChildren.findFragmentByTag(HomeReviewDialog.TAG);
                assertNotNull(review);
                assertNull(activity.getSupportFragmentManager().findFragmentByTag(HomeReviewDialog.TAG));

                CheckBox filter = review.requireDialog().findViewById(R.id.home_review_pending_filter);
                assertTrue(filter.isChecked());
                filter.setChecked(false);
                ListView list = review.requireDialog().findViewById(R.id.home_review_list);
                assertTrue(list.getCount() > 0);
                list.performItemClick(list.getChildAt(0), 0, list.getAdapter().getItemId(0));
                QuestionDetailFragment question = (QuestionDetailFragment)
                        homeChildren.findFragmentByTag(QuestionDetailFragment.TAG);
                assertNotNull(question);
                assertTrue(question.requireView().findViewById(R.id.question_title).isShown());
                question.dismissNow();
                review.dismissNow();
            });

            tapTab(scenario, R.id.nav_records);
            assertVisibleTab(scenario, R.id.nav_records, R.id.exam_list_title);
            tapTab(scenario, R.id.nav_community);
            assertVisibleTab(scenario, R.id.nav_community, R.id.community_title);
            scenario.onActivity(activity -> {
                assertNull(activity.findViewById(R.id.home_review_list));
                assertNull(activity.findViewById(R.id.question_detail_screen));
            });
        }
    }

    @Test
    public void tabsShowOnlyTheirOwnScreenAndAllowReselection() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            assertVisibleTab(scenario, R.id.nav_home, R.id.home_title);
            tapTab(scenario, R.id.nav_records);
            assertVisibleTab(scenario, R.id.nav_records, R.id.exam_list_title);
            tapTab(scenario, R.id.nav_community);
            assertVisibleTab(scenario, R.id.nav_community, R.id.community_title);
            tapTab(scenario, R.id.nav_community);
            assertVisibleTab(scenario, R.id.nav_community, R.id.community_title);
            tapTab(scenario, R.id.nav_home);
            assertVisibleTab(scenario, R.id.nav_home, R.id.home_title);
        }
    }

    @Test
    public void activityRecreationRestoresSelectedTabAndVisibleScreen() {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            tapTab(scenario, R.id.nav_records);
            scenario.recreate();

            assertVisibleTab(scenario, R.id.nav_records, R.id.exam_list_title);
            tapTab(scenario, R.id.nav_community);
            scenario.recreate();
            assertVisibleTab(scenario, R.id.nav_community, R.id.community_title);
        }
    }

    private void assertVisibleTab(ActivityScenario<MainActivity> scenario, int tabId, int titleId) {
        waitForWindow(scenario);
        InstrumentationRegistry.getInstrumentation().waitForIdleSync();
        scenario.onActivity(activity -> {
            BottomNavigationView navigation = activity.findViewById(R.id.bottom_navigation);
            assertEquals(tabId, navigation.getSelectedItemId());
            for (int id : new int[]{R.id.home_title, R.id.exam_list_title, R.id.community_title}) {
                View title = activity.findViewById(id);
                if (id == titleId) {
                    assertTrue("Selected screen title must be visible", title != null && title.isShown());
                } else if (title != null) {
                    assertFalse("Other screen titles must be hidden", title.isShown());
                }
            }
        });
    }

    private void tapTab(ActivityScenario<MainActivity> scenario, int tabId) {
        waitForWindow(scenario);
        Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        instrumentation.waitForIdleSync();
        float[] position = new float[2];
        scenario.onActivity(activity -> {
            View tab = activity.findViewById(tabId);
            assertTrue(tab.isShown());
            int[] location = new int[2];
            tab.getLocationOnScreen(location);
            position[0] = location[0] + tab.getWidth() / 2f;
            position[1] = location[1] + tab.getHeight() / 2f;
        });

        // Platform input injection works on the installed API 36 emulator without changing
        // the project's older Espresso dependency or its Gradle configuration.
        long downTime = SystemClock.uptimeMillis();
        MotionEvent down = MotionEvent.obtain(downTime, downTime, MotionEvent.ACTION_DOWN,
                position[0], position[1], 0);
        MotionEvent up = MotionEvent.obtain(downTime, downTime + 50, MotionEvent.ACTION_UP,
                position[0], position[1], 0);
        down.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        up.setSource(InputDevice.SOURCE_TOUCHSCREEN);
        try {
            assertTrue(instrumentation.getUiAutomation().injectInputEvent(down, true));
            assertTrue(instrumentation.getUiAutomation().injectInputEvent(up, true));
        } finally {
            down.recycle();
            up.recycle();
        }
        instrumentation.waitForIdleSync();
    }

    private void waitForWindow(ActivityScenario<MainActivity> scenario) {
        // ActivityScenario can reach RESUMED before the first frame and window focus.
        long deadline = SystemClock.uptimeMillis() + 10_000;
        boolean[] ready = new boolean[1];
        do {
            scenario.onActivity(activity -> {
                View navigation = activity.findViewById(R.id.bottom_navigation);
                ready[0] = activity.hasWindowFocus() && navigation.isLaidOut();
            });
            if (ready[0]) {
                return;
            }
            SystemClock.sleep(50);
        } while (SystemClock.uptimeMillis() < deadline);
        assertTrue("Activity window must be focused and laid out", ready[0]);
    }
}
