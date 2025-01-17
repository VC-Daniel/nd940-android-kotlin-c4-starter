package com.udacity.project4.locationreminders.savereminder.selectreminderlocation

import android.app.Application
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.assertion.ViewAssertions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PointOfInterest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeAndroidDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import com.udacity.project4.util.DataBindingIdlingResource
import com.udacity.project4.util.monitorFragment
import com.udacity.project4.utils.EspressoIdlingResource
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import java.util.*

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class SelectLocationFragmentTest :
    AutoCloseKoinTest() {// Extended Koin Test - embed autoclose @after method to close Koin after every test

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: ReminderDataSource
    private lateinit var appContext: Application
    private lateinit var saveReminderViewModel: SaveReminderViewModel
    private val dataBindingIdlingResource = DataBindingIdlingResource()

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
                saveReminderViewModel
            }
        }

        // Declare a new koin module
        startKoin {
            modules(listOf(myModule))
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
    fun selectLocation_saveVisible() = runBlockingTest {
        // GIVEN - Launch the select location screen.
        val fragment = launchFragmentInContainer<SelectLocationFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragment)

        // WHEN - The select location screen is displayed

        // THEN - The save button should be displayed
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
            .check(ViewAssertions.matches(ViewMatchers.isDisplayed()))
    }

    @Test
    fun selectLocation_saveNotEnabled() = runBlockingTest {
        // GIVEN - The select location screen is displayed
        val fragment = launchFragmentInContainer<SelectLocationFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragment)

        // WHEN - No Poi has been selected Poi

        // THEN - The save button is not enabled
        Espresso.onView(ViewMatchers.withId(R.id.saveButton)).check(
            ViewAssertions.matches(
                IsNot.not(
                    ViewMatchers.isEnabled()
                )
            )
        )
    }

    @Test
    fun selectLocation_saveEnabledPoi() = runBlockingTest {
        val reminderPoi = PointOfInterest(
            LatLng(
                37.4222359720916,
                -122.08406823759553
            ), "", "Googleplex"
        )


        val fragment = launchFragmentInContainer<SelectLocationFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragment)

        // GIVEN - The select location screen is displayed with no currently selected Poi
        Espresso.onView(ViewMatchers.withId(R.id.saveButton)).check(
            ViewAssertions.matches(
                IsNot.not(
                    ViewMatchers.isEnabled()
                )
            )
        )

        // WHEN - A reminder location is selected that corresponds to a poi
        saveReminderViewModel.selectReminderLocation(reminderPoi)

        // THEN - The save button is enabled
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
            .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
    }

    @Test
    fun selectLocation_saveEnabledCustomLocation() = runBlockingTest {
        val customLatLng = LatLng(37.348541, -122.102254)
        val formattedName = String.format(
            Locale.getDefault(),
            appContext.getString(R.string.lat_long_snippet),
            customLatLng.latitude,
            customLatLng.longitude
        )

        val reminderPoi = PointOfInterest(
            customLatLng, appContext.getString(R.string.dropped_pin), formattedName
        )


        val fragment = launchFragmentInContainer<SelectLocationFragment>(Bundle(), R.style.AppTheme)
        dataBindingIdlingResource.monitorFragment(fragment)

        // GIVEN - The select location screen is displayed with no currently selected Poi
        Espresso.onView(ViewMatchers.withId(R.id.saveButton)).check(
            ViewAssertions.matches(
                IsNot.not(
                    ViewMatchers.isEnabled()
                )
            )
        )

        // WHEN - A reminder is selected that uses a custom location rather then a defined poi
        saveReminderViewModel.selectReminderLocation(reminderPoi)

        // THEN - The save button is enabled
        Espresso.onView(ViewMatchers.withId(R.id.saveButton))
            .check(ViewAssertions.matches(ViewMatchers.isEnabled()))
    }
}