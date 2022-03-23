package com.udacity.project4.locationreminders.savereminder

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.ToastMatcher
import com.udacity.project4.util.monitorFragment
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
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
import org.mockito.Mockito

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class SaveReminderFragmentTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private val dataBindingIdlingResource = DataBindingIdlingResource()

    private val reminderPoi = PointOfInterest(
        LatLng(
            37.4222359720916,
            -122.08406823759553
        ), "", "Googleplex"
    )

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
        repository = FakeAndroidDataSource()
        appContext = ApplicationProvider.getApplicationContext()
        saveReminderViewModel =
            SaveReminderViewModel(
                appContext,
                repository
            )
        stopKoin()//stop the original app koin
        val myModule = module {
            viewModel {
                saveReminderViewModel
            }
        }
        //declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }
        //clear the data to start fresh
        runBlocking {
            repository.deleteAllReminders()
        }
    }

    @Before
    fun registerIdlingResource() {
        IdlingRegistry.getInstance().register(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().register(dataBindingIdlingResource)
    }

    @After
    fun unregisterIdlingResource() {
        IdlingRegistry.getInstance().unregister(EspressoIdlingResource.countingIdlingResource)
        IdlingRegistry.getInstance().unregister(dataBindingIdlingResource)
    }

    @Test
    fun reminder_ReminderNoLocation() = runBlocking {
        // GIVEN - Specify a title for a reminder but not a location
        val fragment = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragment)
        Espresso.onView(ViewMatchers.withId(R.id.reminderTitle))
            .perform(ViewActions.replaceText(testReminderOne.title))

        // WHEN - Attempt to save a reminder without having specified a location
        Espresso.onView(ViewMatchers.withId(R.id.saveReminder)).perform(ViewActions.click())

        // THEN - The reminder is not saved and an error message is shown in a snackbar
        Espresso.onView(ViewMatchers.withText(R.string.err_select_location))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        val reminders = (repository.getReminders() as Result.Success).data
        MatcherAssert.assertThat(
            reminders.size,
            CoreMatchers.`is`(0)
        )
    }

    @Test
    fun reminder_ReminderNoTitle() = runBlocking {
        // GIVEN - Specify a location and description for a reminder but not a title
        val fragment = launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragment)
        Espresso.onView(ViewMatchers.withId(R.id.reminderDescription))
            .perform(ViewActions.replaceText(testReminderOne.description))
        saveReminderViewModel.selectReminderLocation(reminderPoi)

        // WHEN - Attempt to save a reminder without having specified a title
        Espresso.onView(ViewMatchers.withId(R.id.saveReminder)).perform(ViewActions.click())

        // THEN - The reminder is not saved and an error message is shown in a snackbar
        Espresso.onView(ViewMatchers.withText(R.string.err_enter_title))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        val reminders = (repository.getReminders() as Result.Success).data
        MatcherAssert.assertThat(
            reminders.size,
            CoreMatchers.`is`(0)
        )
    }


    @Test
    fun reminder_SaveReminder() = runBlocking {
        // GIVEN - Specify all the required details of a reminder as well as the description
        val navController = Mockito.mock(NavController::class.java)
        val scenario =
            launchFragmentInContainer<SaveReminderFragment>(Bundle(), R.style.AppTheme)
        scenario.onFragment { Navigation.setViewNavController(it.view!!, navController) }
        dataBindingIdlingResource.monitorFragment(scenario)

        Espresso.onView(ViewMatchers.withId(R.id.reminderTitle))
            .perform(ViewActions.replaceText(testReminderOne.title))
        Espresso.onView(ViewMatchers.withId(R.id.reminderDescription))
            .perform(ViewActions.replaceText(testReminderOne.description))
        saveReminderViewModel.selectReminderLocation(reminderPoi)

        // WHEN - Save the reminder
        Espresso.onView(ViewMatchers.withId(R.id.saveReminder)).perform(ViewActions.click())

        // THEN - A toast message confirms the reminder was saved and it is saved to the repository
        Espresso.onView(ViewMatchers.withText(R.string.reminder_saved))
            .inRoot(ToastMatcher()).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        val reminders = (repository.getReminders() as Result.Success).data
        MatcherAssert.assertThat(
            reminders.size,
            CoreMatchers.`is`(1)
        )
    }
}