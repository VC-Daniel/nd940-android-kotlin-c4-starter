package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.hamcrest.CoreMatchers.*
import org.hamcrest.MatcherAssert.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.*

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    private val testReminderOne = ReminderDTO(
        "Reminder Title",
        "Reminder Description",
        "Googleplex",
        37.4222359720916,
        -122.08406823759553
    )

    @Before
    fun initializeDatabase() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            RemindersDatabase::class.java
        ).build()
    }

    @After
    fun closeDatabase() = database.close()

    @Test
    fun insertReminderAndGetById() = runBlockingTest {
        // Given - a reminder is saved in the database
        database.reminderDao().saveReminder(testReminderOne)

        // When - Retrieve the reminder by it's id from the database
        val retrievedReminder = database.reminderDao().getReminderById(testReminderOne.id)

        // Then - The loaded reminder should match the inserted reminder
        assertThat<ReminderDTO>(retrievedReminder as ReminderDTO, notNullValue())
        assertThat(retrievedReminder.id, `is`(testReminderOne.id))
        assertThat(retrievedReminder.description, `is`(testReminderOne.description))
        assertThat(retrievedReminder.latitude, `is`(testReminderOne.latitude))
        assertThat(retrievedReminder.longitude, `is`(testReminderOne.longitude))
        assertThat(retrievedReminder.title, `is`(testReminderOne.title))
    }

    @Test
    fun getMultipleReminders() = runBlockingTest {
        // Given - Multiple reminders are saved in the database
        val reminderTwo = ReminderDTO(
            "Reminder Two",
            "Reminder two description",
            "Boat Launch",
            37.458530097023115, -122.10187916306691
        )

        database.reminderDao().saveReminder(testReminderOne)
        database.reminderDao().saveReminder(reminderTwo)

        // When - retrieve all the reminders
        val reminders = database.reminderDao().getReminders()

        // Then - Two reminders should be returned
        assertThat<List<ReminderDTO>>(reminders, notNullValue())
        assertThat(reminders.size, `is`(2))
    }

    @Test
    fun reminderIdNotFound() = runBlockingTest {
        // Given - At least one reminder is saved in the database
        database.reminderDao().saveReminder(testReminderOne)

        // When - Retrieve a reminder from an id that is not in the database
        val nonRealId = UUID.randomUUID().toString()
        val retrievedReminder = database.reminderDao().getReminderById(nonRealId)

        // Then - The retrieved reminder should be null
        assertThat(retrievedReminder, `is`(nullValue()))
    }

    @Test
    fun noDataInDatabase() = runBlockingTest {
        // Given - All data is deleted from the database
        database.reminderDao().saveReminder(testReminderOne)
        database.reminderDao().deleteAllReminders()

        // When - Retrieve all reminders from the database
        val reminders = database.reminderDao().getReminders()

        // Then - Zero reminders should be returned
        assertThat<List<ReminderDTO>>(reminders, notNullValue())
        assertThat(reminders.size, `is`(0))
    }
}