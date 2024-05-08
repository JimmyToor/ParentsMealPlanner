package com.jimmy.parentsmealplanner

import com.jimmy.parentsmealplanner.data.Datasource.Companion.loadMealsWithDishesAndInstances
import com.jimmy.parentsmealplanner.data.MealRepository
import com.jimmy.parentsmealplanner.model.MealWithDishesAndAllInstances
import com.jimmy.parentsmealplanner.model.User
import com.jimmy.parentsmealplanner.rules.MainDispatcherRule
import com.jimmy.parentsmealplanner.ui.meal.DEFAULT_USER_ID
import com.jimmy.parentsmealplanner.ui.meal.MealPlanningViewModel
import com.jimmy.parentsmealplanner.ui.meal.MealUiState
import com.jimmy.parentsmealplanner.ui.meal.UserUiState
import com.jimmy.parentsmealplanner.ui.meal.getMealInstancesForDateAndUser
import com.jimmy.parentsmealplanner.ui.shared.MealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.UserDetails
import com.jimmy.parentsmealplanner.ui.shared.toMealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.toUserDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.spyk
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class MealPlanningViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private val mealRepository = spyk<MealRepository>(recordPrivateCalls = true)
    private lateinit var viewModel: MealPlanningViewModel

    private fun createViewModel() = spyk<MealPlanningViewModel>(MealPlanningViewModel(mealRepository))

    @Before
    fun setUp() {
        viewModel = createViewModel()
    }

    @Test
    fun `MealPlanningViewModel_Initialize User UI State`() {
        runTest { // Set up expected values
            val testUser = User(userId = 1, name = "User 1")
            val userUiState =
                UserUiState(
                    selectedUserDetails = testUser.toUserDetails(),
                    allUsersDetails = listOf(testUser.toUserDetails()),
                )

            // Set up mock behavior
            coEvery {
                mealRepository.getMealsWithDishesAndInstancesInDateRangeStream(
                    any(),
                    any(),
                )
            } returns
                flowOf(
                    loadMealsWithDishesAndInstances(),
                )
            coEvery { mealRepository.getUser(any()) } returns testUser
            coEvery { mealRepository.getAllUsersStream() } returns flowOf(listOf(testUser))
            coEvery { mealRepository.getAllUsers() } returns listOf(testUser)
            coEvery {
                mealRepository.getMealsWithDishesAndInstancesInDateRangeStream(
                    any(),
                    any(),
                )
            } returns flowOf(listOf<MealWithDishesAndAllInstances>())

            // Call the function to be tested
            viewModel.userUiState.first()

            // Verify the results
            assertEquals(userUiState, viewModel.userUiState.value)
        }
    }

    @Test
    fun `MealPlanningViewModel_Initialize Meal UI State`() {
        runTest {
            // Set up expected values
            val testMeal: MealWithDishesAndAllInstances = loadMealsWithDishesAndInstances().first()
            val mealUiState = MealUiState(mealInstanceDetails = testMeal.toMealInstanceDetails())
            val testUser = User(1, "User 1")

            // Set up mock behavior
            coEvery {
                mealRepository.getMealsWithDishesAndInstancesInDateRangeStream(
                    any(),
                    any(),
                )
            } returns
                flowOf(
                    listOf(testMeal),
                )
            coEvery { mealRepository.getUser(any()) } returns testUser
            coEvery { mealRepository.getAllUsersStream() } returns flowOf(listOf(testUser))
            coEvery { mealRepository.getAllUsers() } returns listOf(testUser)

            // Call the function to be tested
            viewModel.mealUiState.first()

            // Verify the results
            assertEquals(mealUiState, viewModel.mealUiState.value)
        }
    }

    @Test
    fun `MealPlanningViewModel_userUiStateOfUserId_Initialize UserUiState with no users should create UserUiState with default User`() {
        runTest {
            // Set up expected values
            val testUser = User(DEFAULT_USER_ID, "User #$DEFAULT_USER_ID")
            val testUserDetails = testUser.toUserDetails()
            val userUiState =
                UserUiState(
                    selectedUserDetails = testUserDetails,
                    allUsersDetails = listOf(testUserDetails),
                    targetUserDetails = UserDetails(),
                )

            // Set up mock behavior
            coEvery {
                mealRepository.getMealsWithDishesAndInstancesInDateRangeStream(
                    any(),
                    any(),
                )
            } returns
                flowOf(
                    listOf(),
                )

            coEvery { mealRepository.insertUser(any()) } returns 1L
            coEvery { mealRepository.getUser(any()) } returns null
            coEvery { mealRepository.getAllUsersStream() } returns flowOf(listOf(testUser))
            coEvery { mealRepository.getAllUsers() } returns emptyList() andThen listOf(testUser)

            // Call the function to be tested
            viewModel.userUiState.first()

            // Verify the results
            coVerify(exactly = 1) { mealRepository.insertUser(any()) }
            assertEquals(userUiState, viewModel.userUiState.value)
        }
    }

    @Test
    fun `MealPlanningViewModel__Get meal instances for date and user`() {
        // Arrange
        val date =
            Clock.System.now().toLocalDateTime(
                TimeZone.currentSystemDefault(),
            ).date
        val userId = 1L
        val mealInstanceDetails =
            listOf(
                MealInstanceDetails(date = date, userId = userId),
                MealInstanceDetails(date = date, userId = userId + 1L),
                MealInstanceDetails(date = date.plus(DatePeriod(days = 1)), userId = userId),
            )
        val mealUiState = MealUiState(mealInstanceDetails = mealInstanceDetails)

        // Act
        val result = mealUiState.getMealInstancesForDateAndUser(date, userId)

        // Assert
        assertEquals(1, result.size)
        assertEquals(date, result[0].date)
        assertEquals(userId, result[0].userId)
    }
}
