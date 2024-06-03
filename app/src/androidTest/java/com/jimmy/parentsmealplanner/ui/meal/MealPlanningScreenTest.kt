package com.jimmy.parentsmealplanner.ui.meal

import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.hasStateDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToIndex
import com.jimmy.parentsmealplanner.ui.shared.DishDetails
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
import com.jimmy.parentsmealplanner.ui.shared.MealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.UserDetails
import com.jimmy.parentsmealplanner.ui.theme.ParentsMealPlannerTheme
import kotlinx.datetime.LocalDate
import org.junit.Rule
import org.junit.Test

class MealPlanningScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `MealPlanningScreen_Week bar displays correctly`() {
        val day = LocalDate(2019, 12, 30)
        val user = UserDetails(id = 1, name = "User 1")
        composeTestRule.setContent {
            ParentsMealPlannerTheme {
                MealPlanningScreen(
                    mealUiState =
                        MealUiState(
                            mealInstanceDetails =
                                listOf(
                                    MealInstanceDetails(
                                        mealInstanceId = 1,
                                        mealDetails =
                                            MealDetails(
                                                mealId = 1,
                                                name = "Breakfast",
                                                dishes =
                                                    listOf(
                                                        DishDetails(name = "Dish 1"),
                                                        DishDetails(name = "Dish 2"),
                                                        DishDetails(name = "Dish 3"),
                                                    ),
                                            ),
                                        userId = 1,
                                    ),
                                ),
                            selectedDay = day,
                        ),
                    userUiState =
                        UserUiState(
                            selectedUserDetails = user,
                            allUsersDetails = listOf(user),
                            targetUserDetails = user,
                        ),
                )
            }
        }

        // Verify that there are no meals, the weekbar has 7 days, and the day selected matches the 'today' value
        composeTestRule.onNodeWithText("29").assertDoesNotExist()
        composeTestRule.onNodeWithText("30").assertIsDisplayed()
        composeTestRule.onNodeWithText("31").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("3").assertIsDisplayed()
        composeTestRule.onNodeWithText("4").assertIsDisplayed()
        composeTestRule.onNodeWithText("5").assertIsDisplayed()
        composeTestRule.onNodeWithText("6").assertDoesNotExist()
    }

    @Test
    fun `MealPlanningScreen_Days of the week load correctly`() {
        val day = LocalDate(2020, 1, 1)
        composeTestRule.setContent {
            MealPlanningBody(
                modifier = Modifier.testTag("meal_planning_body"),
                mealUiState =
                    MealUiState(
                        selectedDay = day,
                    ),
                userUiState =
                    UserUiState(
                        selectedUserDetails = UserDetails(id = 1, name = "User 1"),
                        allUsersDetails = listOf(UserDetails(id = 1, name = "User 1")),
                        targetUserDetails = UserDetails(id = 1, name = "User 1"),
                    ),
                onMealClick = { _, _, _, _, _ -> },
                onMealDeleteClick = { _ -> },
                daysOfWeekListState = rememberLazyListState(0),
            )
        }

        composeTestRule.onNodeWithText("MONDAY, DECEMBER 30").assertExists("Expected MONDAY, DECEMBER 30 to exist")
        composeTestRule.onNodeWithText("TUESDAY, DECEMBER 31").assertExists("Expected TUESDAY, DECEMBER 31 to exist")
        composeTestRule.onNodeWithText("WEDNESDAY, JANUARY 1").assertExists("Expected WEDNESDAY, JANUARY 1 to exist")

        composeTestRule.onNodeWithTag("meal_planning_body").performScrollToIndex(3)
        composeTestRule.onNodeWithText("THURSDAY, JANUARY 2").assertExists("Expected THURSDAY, JANUARY 2 to exist")
        composeTestRule.onNodeWithText("FRIDAY, JANUARY 3").assertExists("Expected FRIDAY, JANUARY 3 to exist")
        composeTestRule.onNodeWithText("SATURDAY, JANUARY 4").assertExists("Expected SATURDAY, JANUARY 4 to exist")

        composeTestRule.onNodeWithTag("meal_planning_body").performScrollToIndex(6)
        composeTestRule.onNodeWithText("SUNDAY, JANUARY 5").assertExists("Expected SUNDAY, JANUARY 5 to exist")
    }

    @Test
    fun `MealPlanningScreen_Expand and collapse correctly`() {
        val day = LocalDate(2019, 12, 30)
        val testDish = DishDetails(dishId = 1L, name = "Dish 1")
        composeTestRule.setContent {
            MealPlanningBody(
                mealUiState =
                    MealUiState(
                        mealInstanceDetails =
                            listOf(
                                MealInstanceDetails(
                                    mealInstanceId = 1,
                                    mealDetails =
                                        MealDetails(
                                            mealId = 1,
                                            name = "testMeal",
                                            dishes =
                                                listOf(
                                                    testDish,
                                                ),
                                        ),
                                    occasion = Occasion.BREAKFAST,
                                    userId = 1,
                                    date = day,
                                ),
                            ),
                        selectedDay = day,
                    ),
                userUiState =
                    UserUiState(
                        selectedUserDetails = UserDetails(id = 1, name = "User 1"),
                        allUsersDetails = listOf(UserDetails(id = 1, name = "User 1")),
                        targetUserDetails = UserDetails(id = 1, name = "User 1"),
                    ),
                onMealClick = { _, _, _, _, _ -> },
                onMealDeleteClick = { _ -> },
            )
        }

        // Click the expand button
        composeTestRule.onNodeWithText(testDish.name).assertIsNotDisplayed()
        composeTestRule.onNode(hasStateDescription("Collapsed")).assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Button: Expand/Collapse meal list").assertIsDisplayed()
        composeTestRule.onNodeWithContentDescription("Button: Expand/Collapse meal list").performClick()
        composeTestRule.onNode(hasStateDescription("Expanded")).assertIsDisplayed()

        // Click the expand button again to collapse
        composeTestRule.onNodeWithContentDescription("Button: Expand/Collapse meal list").performClick()
        composeTestRule.onNodeWithText(testDish.name).assertIsNotDisplayed()
    }

    @Test
    fun `MealPlanningScreen_User dropdown menu opens and closes correctly`() {
        val day = LocalDate(2019, 12, 30)
        val user = UserDetails(id = 1, name = "User 1")
        val user2 = UserDetails(id = 2, name = "User 2")
        composeTestRule.setContent {
            ParentsMealPlannerTheme {
                MealPlanningScreen(
                    mealUiState =
                        MealUiState(
                            mealInstanceDetails =
                                listOf(
                                    MealInstanceDetails(
                                        mealInstanceId = 1,
                                        mealDetails =
                                            MealDetails(
                                                mealId = 1,
                                                name = "Breakfast",
                                                dishes =
                                                    listOf(
                                                        DishDetails(name = "Dish 1"),
                                                        DishDetails(name = "Dish 2"),
                                                        DishDetails(name = "Dish 3"),
                                                    ),
                                            ),
                                        userId = 1,
                                    ),
                                ),
                            selectedDay = day,
                        ),
                    userUiState =
                        UserUiState(
                            selectedUserDetails = user,
                            allUsersDetails = listOf(user, user2),
                            targetUserDetails = user,
                        ),
                )
            }
        }
        // User dropdown menu should be collapsed by default and display the active user's name
        composeTestRule.onNodeWithText(user.name).assertIsDisplayed()
        composeTestRule.onNode(hasStateDescription("User Dropdown Collapsed")).assertIsDisplayed()

        // Click the user dropdown menu to expand it, displaying all users
        composeTestRule.onNode(hasStateDescription("User Dropdown Collapsed")).performClick()
        composeTestRule.onNode(hasStateDescription("User Dropdown Expanded")).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(user.name).assertCountEquals(2)
        composeTestRule.onNodeWithText(user2.name).assertIsDisplayed()

        // Click the user dropdown menu again to collapse it, displaying only the active user's name
        composeTestRule.onNode(hasStateDescription("User Dropdown Expanded")).performClick()
        composeTestRule.onNode(hasStateDescription("User Dropdown Collapsed")).assertIsDisplayed()
        composeTestRule.onNodeWithText(user.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(user2.name).assertIsNotDisplayed()
    }

    @Test
    fun `MealPlanningScreen_User switches successfully`() {
        val user = UserDetails(id = 1, name = "User 1")
        val user2 = UserDetails(id = 2, name = "User 2")
        val userState =
            mutableStateOf(
                UserUiState(
                    selectedUserDetails = user,
                    allUsersDetails = listOf(user, user2),
                    targetUserDetails = user,
                ),
            )
        composeTestRule.setContent {
            ParentsMealPlannerTheme {
                UserBar(
                    userUiState = userState.value,
                    onUserChange = { userState.value = userState.value.copy(selectedUserDetails = user2) },
                )
            }
        }
        // User dropdown menu should be collapsed by default and display the active user's name
        composeTestRule.onNodeWithText(user.name).assertIsDisplayed()
        composeTestRule.onNodeWithText(user2.name).assertIsNotDisplayed()

        composeTestRule.onNode(hasStateDescription("User Dropdown Collapsed")).assertIsDisplayed()

        // Click the user dropdown menu to expand it, displaying all users
        composeTestRule.onNode(hasStateDescription("User Dropdown Collapsed")).performClick()
        composeTestRule.onNode(hasStateDescription("User Dropdown Expanded")).assertIsDisplayed()
        composeTestRule.onAllNodesWithText(user.name).assertCountEquals(2)
        composeTestRule.onNodeWithText(user2.name).assertIsDisplayed()

        // Click user2 to collapse it and switch to user2, displaying only the active user's name
        composeTestRule.onNodeWithText(user2.name).performClick()
        composeTestRule.onNode(hasStateDescription("User Dropdown Collapsed")).assertIsDisplayed()
        composeTestRule.onNodeWithText(user.name).assertIsNotDisplayed()
        composeTestRule.onNodeWithText(user2.name).assertIsDisplayed()
    }

    @Test
    fun `MealPlanningScreen_Delete user modal opens and closes correctly`() {
        val day = LocalDate(2019, 12, 30)
        val user = UserDetails(id = 1, name = "User 1")
        composeTestRule.setContent {
            ParentsMealPlannerTheme {
                MealPlanningScreen(
                    mealUiState =
                        MealUiState(
                            mealInstanceDetails =
                                listOf(
                                    MealInstanceDetails(
                                        mealInstanceId = 1,
                                        mealDetails =
                                            MealDetails(
                                                mealId = 1,
                                                name = "Breakfast",
                                                dishes = listOf(),
                                            ),
                                        userId = 1,
                                    ),
                                ),
                            selectedDay = day,
                        ),
                    userUiState =
                        UserUiState(
                            selectedUserDetails = user,
                            allUsersDetails = listOf(user),
                            targetUserDetails = user,
                        ),
                )
            }
        }

        // Verify that the rename user modal displays correctly and can be closed
        composeTestRule.onNode(hasStateDescription("User Dropdown Collapsed")).performClick()
        composeTestRule.onNodeWithContentDescription("Button: Edit User User 1").performClick()
        composeTestRule.onNodeWithText("Cancel").assertIsDisplayed()
        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.onNodeWithText("Cancel").assertIsNotDisplayed()
    }
}
