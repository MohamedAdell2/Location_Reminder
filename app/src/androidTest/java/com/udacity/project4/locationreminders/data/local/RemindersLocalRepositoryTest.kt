package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.data.dto.succeeded
import com.udacity.project4.locationreminders.data.dto.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Medium Test to test the repository
@MediumTest
class RemindersLocalRepositoryTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase
    private lateinit var repository: RemindersLocalRepository

    @Before
    fun setupRepo(){
        //initiate database and repo
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext() , RemindersDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        repository = RemindersLocalRepository(database.reminderDao() , Dispatchers.Main)
    }

    @After
    fun clearDB() = database.close()

    @Test
    fun saveReminder_RetrieveIt () = runTest(UnconfinedTestDispatcher()){
        //Given data to insert in the database
        val test1 = ReminderDTO(
            "test1" , "description" , "egypt" , 52.5, 12.5
        )
        repository.saveReminder(test1)
        //When retentive data successfully
        val result = repository.getReminder(test1.id)
        assertEquals(true , result.succeeded)
        //then check data is correct
        result as Result.Success
        assertEquals(test1.title , result.data.title)
        assertEquals(test1.description , result.data.description)
        assertEquals(test1.location , result.data.location)
        assertEquals(test1.latitude , result.data.latitude)
        assertEquals(test1.longitude , result.data.longitude)
    }

    @Test
    fun retrieveListReminder() = runTest(UnconfinedTestDispatcher()){
        //given insert several data
        val test1 = ReminderDTO(
            "test1" , "description" , "egypt" , 52.5, 12.5
        )
        val test2 = ReminderDTO(
            "test2" , "description" , "egypt" , 52.5, 12.5
        )
        repository.saveReminder(test1)
        repository.saveReminder(test2)
        //When retrieving data
        val result = repository.getReminders()
        assertEquals(true , result.succeeded)
        //then check the list has size
        result as Result.Success
        assertEquals(2 ,result.data.size)
    }
}