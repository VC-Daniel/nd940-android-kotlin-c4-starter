package com.udacity.project4.locationreminders.reminderslist

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.TEST_ERROR_MESSAGE
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.LocalDB
import com.udacity.project4.locationreminders.data.local.RemindersLocalRepository
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.core.IsNot
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
//UI Testing
@MediumTest
class ReminderListFragmentTest :
    AutoCloseKoinTest() { // Extended Koin Test - embed autoclose @after method to close Koin after every test

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private val dataBindingIdlingResource = DataBindingIdlingResource()

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

        // Stop the original app koin
        stopKoin()
        val myModule = module {
            viewModel {
                RemindersListViewModel(
                    repository
                )
            }
            single {
                saveReminderViewModel
            }
            single { RemindersLocalRepository(get()) }
            single { LocalDB.createRemindersDao(appContext) }
        }

        // Declare a new koin module
        startKoin {
            modules(listOf(myModule))
        }

        // Clear the data to start fresh
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
    fun reminder_DisplayInUi() = runBlockingTest {
        // GIVEN - A reminder is saved in the repository
        repository.saveReminder(testReminderOne)

        // WHEN - Launch the reminder list screen
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        // THEN - Reminder details are displayed on the screen about the reminder retrieved from the
        // repository such as the title, description and location
        onView(withText(testReminderOne.title)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(withText(testReminderOne.description)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
        onView(withText(testReminderOne.location)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))

        // Also the no data message is not visible
        onView(withId(R.id.noDataTextView)).check(
            ViewAssertions.matches(
                IsNot.not(
                    ViewMatchers.isDisplayed()
                )
            )
        )
    }

    @Test
    fun reminder_NoData() = runBlockingTest {
        // GIVEN - No reminders are saved in the repository

        // WHEN - Launch the reminder list screen
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        // THEN - the no data message is visible
        onView(withId(R.id.noDataTextView)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun reminder_ErrorDisplayed() = runBlockingTest {
        // GIVEN - The reminder data source will return an error when reminder data
        // is attempted to be retrieved
        (repository as FakeAndroidDataSource).setReturnError(true)

        // WHEN - Launch the reminder list screen which will attempt to retrieve the reminders
        // from the repository
        val scenario =
            launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(scenario)

        // THEN - the error message is displayed in a snackbar
        onView(withText(TEST_ERROR_MESSAGE)).check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun clickFab_navigateToAddNewReminder() = runBlockingTest {
        // Given - On the reminder list screen
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = Mockito.mock(NavController::class.java)
        scenario.onFragment { Navigation.setViewNavController(it.view!!, navController) }
        dataBindingIdlingResource.monitorFragment(scenario)

        // When - Click on the add a reminder button
        onView(withId(R.id.addReminderFAB)).perform(ViewActions.click())

        // THEN - Verify that we navigate to the add a reminder screen
        Mockito.verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }
}