package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.ERROR_REMINDER_NOT_FOUND
import com.udacity.project4.utils.wrapEspressoIdlingResource

const val TEST_ERROR_MESSAGE = "Test error"

/** Use FakeDataSource that acts as a test double to the LocalDataSource */
class FakeAndroidDataSource : ReminderDataSource {

    private var remindersData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()
    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        wrapEspressoIdlingResource {
            shouldReturnError = value
        }
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        wrapEspressoIdlingResource {
            return if (shouldReturnError) {
                Result.Error(TEST_ERROR_MESSAGE)
            } else {
                val reminders: List<ReminderDTO> = remindersData.values.toList()
                Result.Success(reminders)
            }
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        wrapEspressoIdlingResource {
            remindersData.put(reminder.id, reminder)
        }
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        wrapEspressoIdlingResource {
            val reminder = remindersData.get(id)
            return if (reminder != null || shouldReturnError) {
                Result.Success(reminder!!)
            } else {
                Result.Error(ERROR_REMINDER_NOT_FOUND)
            }
        }
    }

    override suspend fun deleteAllReminders() {
        wrapEspressoIdlingResource {
            remindersData.clear()
        }
    }

    fun addReminders(vararg reminders: ReminderDTO) {
        wrapEspressoIdlingResource {
            for (reminder in reminders) {
                remindersData[reminder.id] = reminder
            }
        }
    }
}