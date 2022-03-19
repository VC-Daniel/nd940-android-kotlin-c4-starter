package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.local.ERROR_REMINDER_NOT_FOUND

const val TEST_ERROR_MESSAGE = "Test error"

/** Use FakeDataSource that acts as a test double to the LocalDataSource */
class FakeAndroidDataSource : ReminderDataSource {

    private var remindersData: LinkedHashMap<String, ReminderDTO> = LinkedHashMap()
    private var shouldReturnError = false

    fun setReturnError(value: Boolean) {
        shouldReturnError = value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (shouldReturnError) {
            Result.Error(TEST_ERROR_MESSAGE)
        } else {
            val reminders: List<ReminderDTO> = remindersData.values.toList()
            Result.Success(reminders)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersData.put(reminder.id, reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = remindersData.get(id)
        return if (reminder != null || shouldReturnError) {
            Result.Success(reminder!!)
        } else {
            Result.Error(ERROR_REMINDER_NOT_FOUND)
        }
    }

    override suspend fun deleteAllReminders() {
        remindersData.clear()
    }

    fun addReminders(vararg reminders: ReminderDTO) {
        for (reminder in reminders) {
            remindersData[reminder.id] = reminder
        }
    }
}