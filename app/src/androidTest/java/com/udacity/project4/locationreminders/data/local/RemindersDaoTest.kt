package com.udacity.project4.locationreminders.data.local

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SmallTest;
import com.udacity.project4.locationreminders.data.dto.ReminderDTO

import org.junit.Before;
import org.junit.Rule;
import org.junit.runner.RunWith;

import kotlinx.coroutines.ExperimentalCoroutinesApi;
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Test
import org.koin.core.context.stopKoin
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
//Unit test the DAO
@SmallTest
class RemindersDaoTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var database: RemindersDatabase

    @Before
    fun createDB(){
        stopKoin()
        database = Room.inMemoryDatabaseBuilder(ApplicationProvider
            .getApplicationContext() , RemindersDatabase::class.java)
            .build()
    }

    @After
    fun cleanDB()=database.close()

    @Test
    fun updateReminder() = runTest{
        //Given insert a reminder into database
        val test1 = ReminderDTO(
            "test1" , "description" , "egypt" , 52.5, 12.5)
        //when update it with same id
        database.reminderDao().saveReminder(test1)
        val updatedReminder = ReminderDTO(
            "updated" , "description2" , "suez"
            , 2.5, 20.5 , test1.id)
        database.reminderDao().saveReminder(updatedReminder)
        //when retrieving it should return updated data
        val result = database.reminderDao().getReminderById(test1.id)
        assertNotNull(result)
        assertEquals(updatedReminder.title  , result.title)
        assertEquals(updatedReminder.description  , result.description)
        assertEquals(updatedReminder.longitude  , result.longitude)
        assertEquals(updatedReminder.location  , result.location)
        assertEquals(updatedReminder.latitude  , result.latitude)
    }

    @Test
    fun deleteData() = runTest{
        //Given insert multiple reminder in db
        val test1 = ReminderDTO(
            "test1" , "description" , "egypt" , 52.5, 12.5)
        val test2 = ReminderDTO(
            "test2" , "description2" , "egypt2" , 2.5, 512.5)
        database.reminderDao().saveReminder(test1)
        database.reminderDao().saveReminder(test2)
        //make sure they are saved
        assertEquals(2 , database.reminderDao().getReminders().size)
        //When deleting all data
        database.reminderDao().deleteAllReminders()
        //then it should return empty list
        val reminderList = database.reminderDao().getReminders()
        assertTrue(reminderList.isEmpty())
    }

}