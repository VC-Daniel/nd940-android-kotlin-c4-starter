package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.MainCoroutineRule
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.local.ERROR_REMINDER_NOT_FOUND
import com.udacity.project4.locationreminders.getOrAwaitValue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.hamcrest.CoreMatchers
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@MediumTest
class RemindersListViewModelTest {
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var remindersRepository: FakeDataSource

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    // Subject under test
    private lateinit var remindersListViewModel: RemindersListViewModel

    @Before
    fun setupViewModel() {
        remindersRepository = FakeDataSource()
        remindersListViewModel = RemindersListViewModel(remindersRepository)
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
            /** Test that when reminders are being loaded from the repository
             * the loading indicator is set to be visible */
    fun loadReminders_Loading() {
        mainCoroutineRule.pauseDispatcher()

        remindersListViewModel.loadReminders()

        MatcherAssert.assertThat(
            remindersListViewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(true)
        )
        mainCoroutineRule.resumeDispatcher()
        MatcherAssert.assertThat(
            remindersListViewModel.showLoading.getOrAwaitValue(),
            CoreMatchers.`is`(false)
        )
    }

    @Test
            /** Test that when no reminders are in the repository the no data indicator is
             * set to be shown */
    fun loadReminders_NoData() {
        remindersListViewModel.loadReminders()

        MatcherAssert.assertThat(
            remindersListViewModel.showNoData.getOrAwaitValue(),
            CoreMatchers.`is`(true)
        )
    }

    @Test
            /** Test that an error message is set to be displayed in a snackbar when the
             * repository returns an error */
    fun loadReminders_ShowError() {
        remindersRepository.setReturnError(true)
        remindersListViewModel.loadReminders()

        MatcherAssert.assertThat(
            remindersListViewModel.showSnackBar.getOrAwaitValue(),
            CoreMatchers.`is`(ERROR_REMINDER_NOT_FOUND)
        )
    }

    @Test
            /** Test loading multiple reminders from the repository */
    fun loadReminders_LoadMultipleReminders() {
        val testReminderOne = ReminderDTO(
            "Reminder Title",
            "Reminder Description",
            "Googleplex",
            37.4222359720916,
            -122.08406823759553
        )

        val reminderTwo = ReminderDTO(
            "Reminder Two",
            "Reminder two description",
            "Boat Launch",
            37.458530097023115, -122.10187916306691
        )

        remindersRepository.addReminders(testReminderOne, reminderTwo)

        remindersListViewModel.loadReminders()

        MatcherAssert.assertThat(
            remindersListViewModel.remindersList.getOrAwaitValue().size,
            CoreMatchers.`is`(2)
        )
    }
}