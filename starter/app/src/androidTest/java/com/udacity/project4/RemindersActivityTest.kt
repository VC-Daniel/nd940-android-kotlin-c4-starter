package com.udacity.project4

import android.app.Application
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.locationreminders.RemindersActivity
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.reminderslist.RemindersListViewModel
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.ToastMatcher
import com.udacity.project4.util.monitorActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.koin.test.AutoCloseKoinTest
import org.koin.test.get

@RunWith(AndroidJUnit4::class)
@LargeTest
//END TO END test to black box test the app
class RemindersActivityTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application

    private val testReminderOne = ReminderDTO(
        "Reminder Title",
        "Reminder Description",
        "Googleplex",
        37.4222359720916,
        -122.08406823759553
    )

    /**
     * As we use Koin as a Service Locator Library to develop our code, we'll also use Koin to test our code.
     * at this step we will initialize Koin related code to be able to use it in out testing.
     */
    @Before
    fun init() {
        //stop the original app koin
        stopKoin()

        appContext = getApplicationContext()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    get() as ReminderDataSource
                )
            }
            single {
                SaveReminderViewModel(
                    appContext,
                    get() as ReminderDataSource
                )
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }

        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        //Get our real repository
        repository = get()

        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun reminderActivity_backButton() = runBlocking {
        // GIVEN - Launch the reminder activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // WHEN - Press the back button
        Espresso.pressBack()

        // THEN - Return the the login screen
        Espresso.onView(withId(R.id.login_button))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        activityScenario.close()
    }

    @Test
    fun reminderActivity_LogoutButton() = runBlocking {
        // GIVEN - Launch the reminder activity
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)

        // WHEN - The user logs out
        Espresso.onView(withId(R.id.logout)).perform(ViewActions.click())

        // THEN - The app takes the user to the login screen
        Espresso.onView(withId(R.id.login_button))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        activityScenario.close()
    }

    @Test
    fun reminder_SaveReminderDataValidAfterMapBack() = runBlocking {
        // GIVEN - Launch the reminder activity and begin adding a reminder by navigating to
        // the save reminder screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        Espresso.onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // Enter valid data for the reminder including a title and description
        Espresso.onView(withId(R.id.reminderTitle))
            .perform(ViewActions.replaceText(testReminderOne.title))
        Espresso.onView(withId(R.id.reminderDescription))
            .perform(ViewActions.replaceText(testReminderOne.description))

        // WHEN - Navigate to the screen to select a location and navigate back to the reminder
        // using the back button
        Espresso.onView(withId(R.id.selectLocation)).perform(ViewActions.click())
        Espresso.pressBack()

        // THEN - The title and description that were previously entered are still displayed
        Espresso.onView(ViewMatchers.withText(testReminderOne.title))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(testReminderOne.description))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        activityScenario.close()
    }

    @Test
    fun reminder_SaveReminderDataValidAfterMapUp() = runBlocking {
        // GIVEN - Launch the reminder activity and begin adding a reminder by navigating to
        // the save reminder screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        Espresso.onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // Enter valid data for the reminder including a title and description
        Espresso.onView(withId(R.id.reminderTitle))
            .perform(ViewActions.replaceText(testReminderOne.title))
        Espresso.onView(withId(R.id.reminderDescription))
            .perform(ViewActions.replaceText(testReminderOne.description))

        // WHEN - Navigate to the screen to select a location and navigate back to the reminder
        // using the up button
        Espresso.onView(withId(R.id.selectLocation)).perform(ViewActions.click())

        // Navigating using the up button is based on the logic found at
        // https://stackoverflow.com/a/35462828
        Espresso.onView(ViewMatchers.withContentDescription(R.string.abc_action_bar_up_description))
            .perform(ViewActions.click())

        // THEN - The title and description that were previously entered are still displayed
        Espresso.onView(ViewMatchers.withText(testReminderOne.title))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(testReminderOne.description))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        activityScenario.close()
    }

    @Test
    fun reminder_SaveReminder() = runBlocking {
        // A reminder poi to enter as a simulated poi selected from the Google Map
        val reminderPoi = PointOfInterest(
            LatLng(
                37.4222359720916,
                -122.08406823759553
            ), "", "Googleplex"
        )

        appContext = getApplicationContext()

        // Create a view model to inject so that we can simulate selecting a poi rather then
        // interacting with the Google Maps fragment
        val saveReminderViewModel = SaveReminderViewModel(
            appContext,
            get() as ReminderDataSource
        )

        // stop the koin created during test initialization so we can inject the saveReminderViewModel
        stopKoin()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    get() as ReminderDataSource
                )
            }
            single {
                saveReminderViewModel
            }
            single { RemindersLocalRepository(get()) as ReminderDataSource }
            single { LocalDB.createRemindersDao(appContext) }
        }

        // declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        // Get our real repository
        repository = get()

        // clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }

        // GIVEN - Launch the reminder activity and begin adding a reminder by navigating to
        // the save reminder screen
        val activityScenario = ActivityScenario.launch(RemindersActivity::class.java)
        dataBindingIdlingResource.monitorActivity(activityScenario)
        Espresso.onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        Espresso.onView(withId(R.id.reminderTitle))
            .perform(ViewActions.replaceText(testReminderOne.title))
        Espresso.onView(withId(R.id.reminderDescription))
            .perform(ViewActions.replaceText(testReminderOne.description))

        // Navigate to the select location screen and simulate the user selecting a poi
        Espresso.onView(withId(R.id.selectLocation)).perform(ViewActions.click())
        saveReminderViewModel.selectReminderLocation(reminderPoi)
        Espresso.onView(withId(R.id.saveButton)).perform(ViewActions.click())

        // WHEN - Save a reminder that has all the required data supplied
        Espresso.onView(withId(R.id.saveReminder)).perform(ViewActions.click())

        // THEN - The reminder is displayed in the list of reminders and a toast message is
        // displayed confirming the reminder was saved
        Espresso.onView(ViewMatchers.withText(R.string.reminder_saved))
            .inRoot(ToastMatcher()).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(testReminderOne.title))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(testReminderOne.description))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        Espresso.onView(ViewMatchers.withText(reminderPoi.name))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        activityScenario.close()
    }
}