package com.udacity.project4.locationreminders.savereminder

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.reminderslist.ReminderDataItem
import com.udacity.project4.locationreminders.rule.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@ExperimentalCoroutinesApi
@RunWith(AndroidJUnit4::class)
@Config(sdk = [29])
class SaveReminderViewModelTest {

    private lateinit var fakeDataSource :ReminderDataSource
    private lateinit var viewModel: SaveReminderViewModel

    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    @Before
    fun setupViewModel(){
        stopKoin()
        fakeDataSource = FakeDataSource()
        viewModel = SaveReminderViewModel(ApplicationProvider.getApplicationContext()
            , fakeDataSource)
    }

    private fun createInvalidData():ReminderDataItem{
        return ReminderDataItem(
            "", "description", "test",55.0,125.25
        )
    }

    private fun createValidData():ReminderDataItem{
        return ReminderDataItem(
            "test1", "description", "test",55.0,125.25
        )
    }

    @Test
    fun checkValidation(){
        // check when the data is invalid should retuen false
        val resultFalse = viewModel.validateEnteredData(createInvalidData())
        assertEquals(false , resultFalse)
        // check when the data is valid should return true
        val resultTrue = viewModel.validateEnteredData(createValidData())
        assertEquals(true , resultTrue)
    }

    @Test
    fun saveData() = runTest(UnconfinedTestDispatcher()){
        //Insert a given data to the data base and pause the coroutine
        val data = createValidData()
        mainCoroutineRule.pauseDispatcher()
        viewModel.saveReminder(data)
        //Check if the live data is changed
        assertEquals(true, viewModel.showLoading.value)
        //resume coroutine and check that the live data return to false
        mainCoroutineRule.resumeDispatcher()
        assertEquals(false , viewModel.showLoading.value)
    }

}