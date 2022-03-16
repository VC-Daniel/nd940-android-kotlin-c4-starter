package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import org.hamcrest.CoreMatchers
import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.notNullValue
import org.hamcrest.MatcherAssert
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var reminderLocalRepository: RemindersLocalRepository

    private val testReminderOne = ReminderDTO(
        "Reminder A",
        "Reminder A Description",
        "Googleplex",
        37.4222359720916,
        -122.08406823759553
    )

    @Before
    fun initializeDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).allowMainThreadQueries().build()

        reminderLocalRepository =
            RemindersLocalRepository(
                database.reminderDao(),
                Dispatchers.Main
            )
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun insertReminderAndGetById() = runBlocking {
        // Given - a reminder is saved in the repository
        reminderLocalRepository.saveReminder(testReminderOne)

        // When - Retrieve the reminder by it's id from the repository
        val reminderResult = reminderLocalRepository.getReminder(testReminderOne.id)

        // Then - The loaded reminder should match the inserted reminder
        MatcherAssert.assertThat(reminderResult as Result.Success, notNullValue())
        MatcherAssert.assertThat(reminderResult.data.id, `is`(testReminderOne.id))
        MatcherAssert.assertThat(
            reminderResult.data.description,
            `is`(testReminderOne.description)
        )
        MatcherAssert.assertThat(reminderResult.data.latitude, `is`(testReminderOne.latitude))
        MatcherAssert.assertThat(
            reminderResult.data.longitude,
            `is`(testReminderOne.longitude)
        )
        MatcherAssert.assertThat(reminderResult.data.title, `is`(testReminderOne.title))
    }

    @Test
    fun getMultipleReminders() = runBlocking {
        // Given - Multiple reminders are saved in the repository
        val reminderTwo = ReminderDTO(
            "Reminder Two",
            "Reminder two description",
            "Boat Launch",
            37.458530097023115, -122.10187916306691
        )

        reminderLocalRepository.saveReminder(testReminderOne)
        reminderLocalRepository.saveReminder(reminderTwo)

        // When - retrieve all the reminders
        val reminderResult = reminderLocalRepository.getReminders()

        // Then - Two reminders should be returned
        MatcherAssert.assertThat(reminderResult as Result.Success, notNullValue())
        MatcherAssert.assertThat<List<ReminderDTO>>(
            reminderResult.data,
            CoreMatchers.notNullValue()
        )
        MatcherAssert.assertThat(reminderResult.data.size, CoreMatchers.`is`(2))
    }

    @Test
    fun reminderIdNotFound() = runBlocking {
        // Given - At least one reminder is saved to the repository
        reminderLocalRepository.saveReminder(testReminderOne)

        // When - Retrieve a reminder from an id that is not in the repository
        val nonRealId = UUID.randomUUID().toString()
        val reminderResult = reminderLocalRepository.getReminder(nonRealId)

        // Then - The retrieved result should be an error that contains a message
        // with the error information
        MatcherAssert.assertThat(reminderResult as Result.Error, notNullValue())
        MatcherAssert.assertThat(reminderResult.message, `is`(ERROR_REMINDER_NOT_FOUND))
    }

    @Test
    fun noDataInRepository() = runBlocking {
        // Given - All data is deleted from the repository
        reminderLocalRepository.saveReminder(testReminderOne)
        reminderLocalRepository.deleteAllReminders()

        // When - Retrieve all reminders from the repository
        val reminderResult = reminderLocalRepository.getReminders()

        // Then - Zero reminders should be returned
        MatcherAssert.assertThat(reminderResult as Result.Success, notNullValue())
        MatcherAssert.assertThat<List<ReminderDTO>>(reminderResult.data, notNullValue())
        MatcherAssert.assertThat(reminderResult.data.size, `is`(0))
    }


}