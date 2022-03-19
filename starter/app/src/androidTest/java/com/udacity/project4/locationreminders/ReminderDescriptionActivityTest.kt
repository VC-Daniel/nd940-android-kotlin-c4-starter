package com.udacity.project4.locationreminders

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class ReminderDescriptionActivityTest {

    private val testReminderDataItem = ReminderDataItem(
        "Reminder Title",
        "Reminder Description",
        "Googleplex",
        37.4222359720916,
        -122.08406823759553
    )

    /**
     * Verify that the reminder details are displayed as expected
     */
    @Test
    fun reminderDescription_viewDetails() = runBlockingTest {
        // GIVEN - Pass in a reminder data item to be displayed
        val intent: Intent =
            ReminderDescriptionActivity.newIntent(
                ApplicationProvider.getApplicationContext(),
                testReminderDataItem
            )

        // WHEN - ReminderDescription activity launched to display the passed in reminder
        // This is based off the code at https://developer.android.com/reference/androidx/test/core/app/ActivityScenario
        ActivityScenario.launch<ReminderDescriptionActivity>(intent)

        // THEN - The reminder information should be displayed
        Espresso.onView(withId(R.id.reminderTitleText))
            .check(ViewAssertions.matches(ViewMatchers.withText(testReminderDataItem.title)))
        Espresso.onView(withId(R.id.reminderDescText))
            .check(ViewAssertions.matches(ViewMatchers.withText(testReminderDataItem.description)))
        Espresso.onView(withId(R.id.reminderLocationText))
            .check(ViewAssertions.matches(ViewMatchers.withText(testReminderDataItem.location)))
    }
}