package com.jimmy.parentsmealplanner

import com.jimmy.parentsmealplanner.data.Datasource.Companion.loadMealsWithDishesAndInstances
import com.jimmy.parentsmealplanner.data.MealRepository
import com.jimmy.parentsmealplanner.model.MealWithDishesAndAllInstances
import com.jimmy.parentsmealplanner.model.User
import com.jimmy.parentsmealplanner.rules.MainDispatcherRule
import com.jimmy.parentsmealplanner.ui.meal.MealPlanningViewModel
import com.jimmy.parentsmealplanner.ui.meal.MealUiState
import com.jimmy.parentsmealplanner.ui.meal.UserUiState
import com.jimmy.parentsmealplanner.ui.meal.getMealInstancesForDateAndUser
import com.jimmy.parentsmealplanner.ui.shared.MealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.UserDetails
import com.jimmy.parentsmealplanner.ui.shared.toMealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.toUser
import com.jimmy.parentsmealplanner.ui.shared.toUserDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.spyk
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertFalse
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

            viewModel.userUiState.first()

            assertEquals(userUiState, viewModel.userUiState.value)
        }
    }

    @Test
    fun `MealPlanningViewModel_Initialize Meal UI State`() {
        runTest {
            val testMeal: MealWithDishesAndAllInstances = loadMealsWithDishesAndInstances().first()
            val mealUiState = MealUiState(mealInstanceDetails = testMeal.toMealInstanceDetails())
            val testUser = User(1, "User 1")

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

            viewModel.mealUiState.first()

            assertEquals(mealUiState, viewModel.mealUiState.value)
        }
    }

    @Test
    fun `MealPlanningViewModel__Get meal instances for date and user`() {
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

        val result = mealUiState.getMealInstancesForDateAndUser(date, userId)

        assertEquals(1, result.size)
        assertEquals(date, result[0].date)
        assertEquals(userId, result[0].userId)
    }

    @Test
    fun `MealPlanningViewModel_Change selected user`() {
        runTest {
            val testUser = User(userId = 1, name = "User 1")
            val testUser2 = User(userId = 2, name = "User 2")

            coEvery {
                mealRepository.getMealsWithDishesAndInstancesInDateRangeStream(
                    any(),
                    any(),
                )
            } returns
                    flowOf(
                        loadMealsWithDishesAndInstances(),
                    )
            coEvery { mealRepository.getUser(1) } returns testUser
            coEvery { mealRepository.getUser(2) } returns testUser
            coEvery { mealRepository.getAllUsersStream() } returns flowOf(listOf(testUser, testUser2))
            coEvery { mealRepository.getAllUsers() } returns listOf(testUser, testUser2)
            coEvery {
                mealRepository.getMealsWithDishesAndInstancesInDateRangeStream(
                    any(),
                    any(),
                )
            } returns flowOf(listOf<MealWithDishesAndAllInstances>())

            viewModel.userUiState.first()

            val userUiState =
                UserUiState(
                    selectedUserDetails = testUser.toUserDetails(),
                    allUsersDetails = listOf(testUser.toUserDetails(), testUser2.toUserDetails()),
                )
            assertEquals(userUiState, viewModel.userUiState.value)

            viewModel.updateSelectedUser(2)

            val expectedUserUiState = UserUiState(
                selectedUserDetails = testUser2.toUserDetails(),
                allUsersDetails = listOf(testUser.toUserDetails(), testUser2.toUserDetails()),
            )

            assertEquals(expectedUserUiState, viewModel.userUiState.value)
        }
    }

    @Test
    fun `MealPlanningViewModel_Prevent saving user with duplicate name`() {
        runTest {
            val existingUser = UserDetails(id = 1, name = "John")
            val newUser = UserDetails(id = 0, name = "John")

            coEvery {
                mealRepository.getMealsWithDishesAndInstancesInDateRangeStream(
                    any(),
                    any(),
                )
            } returns
                    flowOf(
                        listOf(),
                    )
            coEvery { mealRepository.getUser(any()) } returns existingUser.toUser()
            coEvery { mealRepository.getAllUsersStream() } returns flowOf(listOf(existingUser.toUser()))
            coEvery { mealRepository.getAllUsers() } returns listOf(existingUser.toUser())
            viewModel.userUiState.first()

            viewModel.updateTargetUser(newUser)
            val result = viewModel.saveTargetUser()

            assertFalse(result)
        }
    }

    @Test
    fun `MealPlanningViewModel_Delete instance tries to delete the instance from the repository`() {
        runTest {
            val mealInstanceToDelete = MealInstanceDetails(mealInstanceId = 1)

            viewModel.deleteInstance(mealInstanceToDelete.mealInstanceId)

            coVerify { mealRepository.deleteMealInstance(mealInstanceToDelete.mealInstanceId) }
        }
    }

    @Test
    fun `MealPlanningViewModel_Delete user tries to delete the user from the repository`() {
        runTest {
            val userToDelete = User(userId = 1)

            viewModel.deleteUser(userToDelete.toUserDetails())
            coVerify { mealRepository.deleteUser(userToDelete)}
        }
    }
}
