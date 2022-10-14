package com.udacity.project4.locationreminders.reminderslist

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.rule.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.pauseDispatcher
import kotlinx.coroutines.test.resumeDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.stopKoin
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
@Config(sdk = [29])
class RemindersListViewModelTest {
    @get:Rule
    var instantTaskExecutorRule= InstantTaskExecutorRule()
    @get:Rule
    var mainCoroutineRule = MainCoroutineRule()

    private lateinit var datasource : FakeDataSource
    private lateinit var viewModel: RemindersListViewModel

    private val testData1 = ReminderDTO(
        "test1" ,"description", "test1", 1.05 ,16.5)

    @Before
    fun setupViewModelAndInsertFakeData()= runTest(UnconfinedTestDispatcher()){
        stopKoin()
        datasource = FakeDataSource()
        viewModel = RemindersListViewModel(ApplicationProvider
            .getApplicationContext(),datasource)
        //save data to fake repo
        datasource.saveReminder(testData1)
    }

    @Test
    fun loadReminders()= runTest(UnconfinedTestDispatcher()){
        //pause coroutine and check the data is loading(livedata)
        mainCoroutineRule.pauseDispatcher()
        viewModel.loadReminders()
        assertEquals(true , viewModel.showLoading.value)
        //Resume coroutine and check the data has been loaded (livedata)
        mainCoroutineRule.resumeDispatcher()
        assertEquals(false , viewModel.showLoading.value)
    }

    @Test
    fun errorDataHandel() = runTest(UnconfinedTestDispatcher()){
        // check when there is an error the snack bar give error message
        datasource.setReturnError(FakeDataSource.ErrorType.ExceptionError)
        viewModel.loadReminders()
        assertEquals("Exception error" , viewModel.showSnackBar.value)
    }

}