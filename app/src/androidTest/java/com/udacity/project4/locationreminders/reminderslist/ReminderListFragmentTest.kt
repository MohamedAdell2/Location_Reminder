package com.udacity.project4.locationreminders.reminderslist

import android.content.Context
import android.os.Bundle
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.fragment.app.testing.launchFragmentInContainer
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.udacity.project4.R
import com.udacity.project4.locationreminders.data.FakeDataSource
import com.udacity.project4.locationreminders.data.ReminderDataSource
import com.udacity.project4.locationreminders.data.dto.ReminderDTO
import com.udacity.project4.locationreminders.savereminder.SaveReminderViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.hamcrest.CoreMatchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(AndroidJUnit4::class)
@ExperimentalCoroutinesApi
//UI Testing
@MediumTest
class ReminderListFragmentTest {
    @get:Rule
    var instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var repository: ReminderDataSource
    private lateinit var context: Context

    private val test1 = ReminderDTO(
        "test1", "description", "egypt", 52.5, 12.5
    )

    @Before
    fun setupKoin() {
        stopKoin()
        repository = FakeDataSource()
        context = ApplicationProvider.getApplicationContext()

        val mModule = module {
            //Declare a ViewModel - be later inject into Fragment with dedicated injector using by viewModel()
            viewModel {
                RemindersListViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            //Declare singleton definitions to be later injected using by inject()
            single {
                //This view model is declared singleton to be used across multiple fragments
                SaveReminderViewModel(
                    get(),
                    get() as ReminderDataSource
                )
            }
            single { repository as ReminderDataSource }
        }
        startKoin {
            androidContext(context)
            modules(listOf(mModule))
        }
    }

    @Test
    fun navigation() {
        //Given launching fragment
        val scenario = launchFragmentInContainer<ReminderListFragment>(Bundle(), R.style.AppTheme)
        val navController = mock(NavController::class.java)
        scenario.onFragment{
            Navigation.setViewNavController(it.view!!, navController)
        }
        //when clicking on add reminder button
        onView(withId(R.id.addReminderFAB)).perform(click())
        //then navigate to save reminder fragment
        verify(navController).navigate(ReminderListFragmentDirections.toSaveReminder())
    }

    @Test
    fun dataDisplayed() = runTest {
        //insert some data to fake repo
        repository.saveReminder(test1)
        //When launching fragment
        launchFragmentInContainer<ReminderListFragment>(Bundle() , R.style.AppTheme)
        //then the data should be on the screen
        onView(withId(R.id.reminderssRecyclerView))
            .check(matches(hasDescendant(withText("test1"))))
    }

    @Test
    fun errorMessage() {
        //when launch the fragment without any data
        launchFragmentInContainer<ReminderListFragment>(Bundle() , R.style.AppTheme)
        //then no data should appear
        onView(withId(R.id.noDataTextView)).check(matches(isDisplayed()))
    }
}