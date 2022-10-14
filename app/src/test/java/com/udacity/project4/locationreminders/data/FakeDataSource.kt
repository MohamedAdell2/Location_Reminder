package com.udacity.project4.locationreminders.data

import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.Result
import com.udacity.project4.locationreminders.data.dto.succeeded

//Use FakeDataSource that acts as a test double to the LocalDataSource
class FakeDataSource : ReminderDataSource {

    private var remindersList = mutableListOf<ReminderDTO>()
    private var returnError = ErrorType.NoError

    fun setReturnError(value: ErrorType){
        returnError=value
    }

    override suspend fun getReminders(): Result<List<ReminderDTO>> {
        return when (returnError){
            ErrorType.ExceptionError -> Result.Error("Exception error")
            else -> Result.Success(remindersList)
        }
    }

    override suspend fun saveReminder(reminder: ReminderDTO) {
        remindersList.add(reminder)
    }

    override suspend fun getReminder(id: String): Result<ReminderDTO> {
        val reminder = remindersList
            .find { it.id ==id } ?: return Result.Error("Error cant get data")
        return when (returnError){
                ErrorType.NoError -> Result.Success(reminder)
                ErrorType.ExceptionError -> Result.Error("Exception error")
                ErrorType.NotFound -> Result.Error("Reminder not found!")
            }
    }

    override suspend fun deleteAllReminders() {
        remindersList.clear()
    }

    enum class ErrorType{
        ExceptionError , NotFound , NoError
    }

}