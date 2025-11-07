package com.example.cmpuzz_events;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.action.ViewActions.click;

import android.content.Context;

import androidx.navigation.NavHostController;
import androidx.navigation.Navigation;
import androidx.navigation.testing.TestNavHostController;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.example.cmpuzz_events.auth.SignupActivity;
import com.example.cmpuzz_events.ui.browse.BrowseEventsFragment;
import com.example.cmpuzz_events.ui.home.HomeFragment;
import androidx.fragment.app.testing.FragmentScenario;


/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */

@RunWith(AndroidJUnit4.class)
public class ExampleInstrumentedTest {
    private FragmentScenario<HomeFragment> scenario;
    private TestNavHostController navController;

    @Before
    public void setUp()
    {
        scenario = FragmentScenario.launchInContainer(HomeFragment.class);
        navController = new TestNavHostController(
                ApplicationProvider.getApplicationContext());
    }

    @Test
    public void testSignIn()
    {
        onView(withId(R.id.etEmail)).perform(typeText("opscrown@gmail.com"), closeSoftKeyboard());
        onView(withId(R.id.etPassword)).perform(typeText("opscrown"), closeSoftKeyboard());

        onView(withId(R.id.btnSignup)).perform(click());

        scenario.onFragment(fragment -> {
            navController.setGraph(R.navigation.mobile_navigation_organizer);
            Navigation.setViewNavController(fragment.requireView(), navController);
            navController.navigate(R.id.navigation_home);
        });
    }

}

