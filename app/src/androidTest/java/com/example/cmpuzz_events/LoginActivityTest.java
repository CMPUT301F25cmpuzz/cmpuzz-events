package com.example.cmpuzz_events;

import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.LoginActivity;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.typeText;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

@RunWith(AndroidJUnit4.class)
@LargeTest
public class LoginActivityTest {

    private static final String TEST_EMAIL = "opscrown@gmail.com";
    private static final String TEST_PASSWORD = "opscrown";

    // This rule launches LoginActivity before each test
    @Rule
    public ActivityScenarioRule<LoginActivity> activityRule =
            new ActivityScenarioRule<>(LoginActivity.class);

    /**
     * Tests that a user with valid credentials can successfully log in
     * and is navigated to the MainActivity.
     */
    @Test
    public void testSuccessfulLogin() {
        // Type valid email into the R.id.etEmail field
        onView(withId(R.id.etEmail)) //
                .perform(typeText(TEST_EMAIL), closeSoftKeyboard());

        // Type valid password into the R.id.etPassword field
        onView(withId(R.id.etPassword)) //
                .perform(typeText(TEST_PASSWORD), closeSoftKeyboard());

        // Click the R.id.btnLogin button
        onView(withId(R.id.btnLogin)).perform(click()); //

        // ** THIS IS THE "FLAKY" PART **
        // We MUST pause the test to wait for the network call to Firebase.
        // 5 seconds is usually enough, but this may fail on a slow connection.
        // If it fails, try increasing this number (e.g., 8000).
        try {
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        // After logging in, check if MainActivity is displayed by looking for
        // its root layout ID (R.id.container from activity_main.xml)
        onView(withId(R.id.container)).check(matches(isDisplayed())); //
    }

    /**
     * Tests that clicking the "Register Here" TextView successfully
     * navigates to the SignupActivity.
     */
    @Test
    public void testNavigateToSignup() {
        // Find the "Sign Up" TextView (R.id.tvSignup) and click it
        onView(withId(R.id.tvSignup)).perform(click());

        // Check if the Signup Activity is now displayed by looking for
        // its root layout ID (R.id.tvTitle from activity_signup.xml)
        onView(withId(R.id.tvTitle)).check(matches(isDisplayed()));
    }

    /**
     * Tests that attempting a login with invalid credentials fails and
     * remains on the LoginActivity. This test requires a network connection
     * to fail the Firebase authentication.
     */
    @Test
    public void testLogin_InvalidCredentials() {
        // Type invalid email into the R.id.etEmail field
        onView(withId(R.id.etEmail))
                .perform(typeText("not-a-real-user@fake.com"), closeSoftKeyboard());

        // Type an invalid password into the R.id.etPassword field
        onView(withId(R.id.etPassword))
                .perform(typeText("fakepassword123"), closeSoftKeyboard());

        // Click the R.id.btnLogin button
        onView(withId(R.id.btnLogin)).perform(click());

        // The app will attempt to log in via Firebase and fail.
        // We expect to stay on the LoginActivity.
        // We can verify this by checking that the login button is still displayed.
        onView(withId(R.id.btnLogin)).check(matches(isDisplayed()));
    }
}