package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    var remindersList = mutableListOf<ReminderDTO>()
    private var returnError = false

    fun setReturnError(value: Boolean){
        returnError=value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return if (returnError){
            Result.Error("Error cant get data")
        }else{
            Result.Success(remindersList)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = remindersList.find { it.id ==id }
        return if (returnError || reminder == null){
            Result.Error("Error cant get data")
        }else{
            Result.Success(reminder)
        }
    }

    override suspend fun deleteAllReminders() {
        remindersList.clear()
    }


}