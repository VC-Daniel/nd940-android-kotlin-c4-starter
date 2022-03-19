package com.udacity.project4.authentication

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.test.AutoCloseKoinTest

@RunWith(AndroidJUnit4::class)
@MediumTest
class AuthenticationActivityTest :
    AutoCloseKoinTest() {
    @Test
    fun authenticationActivity_LoginUi() = runBlocking {
        // Start Authentication screen.
        val activityScenario = ActivityScenario.launch(AuthenticationActivity::class.java)

        // Confirm the user interface has the expected views
        Espresso.onView(ViewMatchers.withId(R.id.login_button))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withId(R.id.app_summary_text))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        activityScenario.close()
    }
}