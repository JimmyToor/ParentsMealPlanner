package com.jimmy.parentsmealplanner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.jimmy.parentsmealplanner.data.Datasource
import com.jimmy.parentsmealplanner.model.DishDao
import com.jimmy.parentsmealplanner.model.DishInMealDao
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealDao
import com.jimmy.parentsmealplanner.model.MealInstance
import com.jimmy.parentsmealplanner.model.MealInstanceDao
import com.jimmy.parentsmealplanner.model.MealPlannerDatabase
import com.jimmy.parentsmealplanner.model.User
import com.jimmy.parentsmealplanner.model.UserDao
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.Rating
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for MealDao, DishDao, and DishInMealDao.
 */
@RunWith(AndroidJUnit4::class)
class MealDaoTest {
    private lateinit var mealDao: MealDao
    private lateinit var dishDao: DishDao
    private lateinit var dishInMealDao: DishInMealDao
    private lateinit var mealInstanceDao: MealInstanceDao
    private lateinit var userDao: UserDao
    private lateinit var mealPlannerDatabase: MealPlannerDatabase

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        mealPlannerDatabase =
            Room.inMemoryDatabaseBuilder(context, MealPlannerDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        mealDao = mealPlannerDatabase.mealDao()
        dishDao = mealPlannerDatabase.dishDao()
        dishInMealDao = mealPlannerDatabase.dishInMealDao()
        mealInstanceDao = mealPlannerDatabase.mealInstanceDao()
        userDao = mealPlannerDatabase.userDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        mealPlannerDatabase.close()
    }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Insert meal and retrieve`() =
        runBlocking {
            val meal = Datasource.loadMeals()[0]
            mealDao.insert(meal)
            val retrievedMeals = mealDao.getAllMeals()
            assert(retrievedMeals[0] == meal) { "Expected ${meal}, but got ${retrievedMeals[0]}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDao_Insert multiple meals and retrieve`() =
        runBlocking {
            val meals = Datasource.loadMeals()
            mealDao.insertAll(meals)
            val retrievedMeals = mealDao.getAllMeals()
            assert(retrievedMeals.containsAll(meals)) { "Expected ${meals}, but got ${retrievedMeals}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve all meals`() =
        runBlocking {
            val meals = Datasource.loadMeals()
            mealDao.insertAll(meals)
            val retrievedMeals = mealDao.getAllMeals()
            assert(retrievedMeals.containsAll(meals)) { "Expected ${meals}, but got ${retrievedMeals}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Update non-existent meal`() =
        runBlocking {
            val nonExistentMeal =
                Meal(
                    100,
                    Rating.LOVEIT,
                    "non-existent",
                )
            mealDao.update(nonExistentMeal)
            val retrievedMeal = mealDao.getMeal(nonExistentMeal.mealId)
            assert(retrievedMeal == null) { "Expected null, but got ${retrievedMeal}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Get meal with dishes by id`() =
        runBlocking {
            val mealsWithDishes = Datasource.loadMealsWithDishes()
            val dishesInMeals = Datasource.loadDishInMeals()
            mealDao.insert(mealsWithDishes[0].meal)
            dishDao.insertAll(mealsWithDishes[0].dishes)
            dishInMealDao.insertAll(dishesInMeals.filter { it.mealId == mealsWithDishes[0].meal.mealId })
            val retrievedMealWithDishes = mealDao.getMealWithDishes(mealsWithDishes[0].meal.mealId)
            assert(retrievedMealWithDishes != null) { "Expected not null, but got null" }
            assert(retrievedMealWithDishes == mealsWithDishes[0]) { "Expected ${mealsWithDishes[0]}, but got $retrievedMealWithDishes" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Get meals in date range`() =
        runBlocking {
            val mealToInsert1 = Meal(mealId = 1, name = "mealTest1")
            val mealToInsert2 = Meal(mealId = 2, name = "mealTest2")
            val mealInstanceToInsert1 = MealInstance(
                mealId = 1,
                userId = 1,
                occasion = Occasion.BREAKFAST,
                date = LocalDate(2021, 1, 1),
            )
            val mealInstanceToInsert2 = MealInstance(
                mealId = 2,
                userId = 1,
                occasion = Occasion.BREAKFAST,
                date = LocalDate(1990, 1, 2),
            )
            val mealInstanceToInsert3 = MealInstance(
                mealId = 2,
                userId = 2,
                occasion = Occasion.BREAKFAST,
                date = LocalDate(2021, 1, 2),
            )
            val userToInsert1 = User(1, "userTest")
            val userToInsert2 = User(2, "userTest2")
            userDao.insert(userToInsert1)
            userDao.insert(userToInsert2)
            mealDao.insert(mealToInsert1)
            mealDao.insert(mealToInsert2)
            mealInstanceDao.insert(mealInstanceToInsert1)
            mealInstanceDao.insert(mealInstanceToInsert2)
            mealInstanceDao.insert(mealInstanceToInsert3)
            val startDate = LocalDate(2021, 1, 1)
            val endDate = LocalDate(2021, 1, 2)
            val retrievedMeals = mealDao.getMealsInDateRangeStream(startDate, endDate).first()
            assert(retrievedMeals.size == 2) { "Expected two meals, but got ${retrievedMeals.size}" }
            assert(retrievedMeals[0] == mealToInsert1) { "Expected ${mealToInsert1}, but got ${retrievedMeals[0]}" }
        }

    // Search for a meal using a search query that does not match any meal names
    @Test
    @Throws(Exception::class)
    fun `MealDAO_Search for non-existent meal`() =
        runBlocking {
            val searchQuery = "non-existent"
            val searchResults = mealDao.searchForMealStream(searchQuery).first()
            assert(searchResults.isEmpty()) { "Expected empty list, but got $searchResults" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Insert multiple meals and retrieve with getAllMealsStream`() =
        runBlocking {
            val meals = Datasource.loadMeals()
            meals.forEach { meal ->
                mealDao.insert(meal)
            }
            val retrievedMeals = mealDao.getAllMealsStream().first()
            assert(retrievedMeals.containsAll(meals)) { "Expected ${meals}, but got ${retrievedMeals}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Insert meal and retrieve by id with getMealStream`() =
        runBlocking {
            val meals = Datasource.loadMeals()
            meals.forEach { meal ->
                mealDao.insert(meal)
            }
            val retrievedMeal = mealDao.getMealStream(id = 1).first()
            assert(retrievedMeal.mealId == 1L) { "Expected mealId 1, but got ${retrievedMeal.mealId}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Update meal`() =
        runBlocking {
            val meal = Datasource.loadMeals()[0]
            mealDao.insert(meal)
            val updatedMeal = meal.copy(name = "Updated Meal")
            mealDao.update(updatedMeal)
            val retrievedMeal = mealDao.getMeal(updatedMeal.mealId)
            assert(retrievedMeal == updatedMeal) { "Expected ${updatedMeal}, but got ${retrievedMeal}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Delete meal`() =
        runBlocking {
            val meal = Datasource.loadMeals()[0]
            mealDao.insert(meal)
            val mealToDelete = meal
            mealDao.delete(mealToDelete)
            val retrievedMeal = mealDao.getMeal(mealToDelete.mealId)
            assert(retrievedMeal == null) { "Expected null, but got ${retrievedMeal}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve meal by id with getMeal`() =
        runBlocking {
            val meal = Datasource.loadMeals()[0]
            mealDao.insert(meal)
            val retrievedMeal = mealDao.getMeal(meal.mealId)
            assert(retrievedMeal == meal) { "Expected ${meal}, but got ${retrievedMeal}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve meal by id with getMealStream`() =
        runBlocking {
            val meal = Datasource.loadMeals()[0]
            mealDao.insert(meal)
            val retrievedMealFlow = mealDao.getMealStream(meal.mealId)
            val retrievedMeal = retrievedMealFlow.first()
            assert(retrievedMeal == meal) { "Expected ${meal}, but got ${retrievedMeal}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve all meals with getAllMeals`() =
        runBlocking {
            val meals = Datasource.loadMeals()
            meals.forEach { mealDao.insert(it) }
            val retrievedMeals = mealDao.getAllMeals()
            assert(retrievedMeals == meals) { "Expected ${meals}, but got ${retrievedMeals}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Delete non-existent meal with getMeal`() =
        runBlocking {
            val nonExistentMeal = Meal(mealId = 100L, name = "Non-existent Meal")
            mealDao.delete(nonExistentMeal)
            val retrievedMeal = mealDao.getMeal(nonExistentMeal.mealId)
            assert(retrievedMeal == null) { "Expected null, but got ${retrievedMeal}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve non-existent meal by id with getMeal`() =
        runBlocking {
            val nonExistentMealId = 100L
            val retrievedMeal = mealDao.getMeal(nonExistentMealId)
            assert(retrievedMeal == null) { "Expected null, but got ${retrievedMeal}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve non-existent meal stream by id`() =
        runBlocking {
            val nonExistentMealId = 100L
            val retrievedMealFlow = mealDao.getMealStream(nonExistentMealId)
            assert(retrievedMealFlow.firstOrNull() == null) { "Expected null, but got ${retrievedMealFlow.firstOrNull()}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve meal with dishes that do not exist`() =
        runBlocking {
            val nonExistentMealId = 100L
            val retrievedMealWithDishes = mealDao.getMealWithDishes(nonExistentMealId)
            assert(retrievedMealWithDishes == null) { "Expected null, but got $retrievedMealWithDishes" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Search for existing meal by partial name`() =
        runBlocking {
            val partialName = "meal"
            val mealToInsert = Meal(mealId = 1, name = "mealTest")
            mealDao.insert(mealToInsert)
            val searchResults = mealDao.searchForMealStream(partialName).first()
            assert(searchResults.isNotEmpty()) { "Expected not empty, but got empty" }
            assert(searchResults.first() == mealToInsert) { "Expected ${mealToInsert}, but got ${searchResults.first()}" }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Search for non-existing meal by partial name`() =
        runBlocking {
            val partialName = "noName"
            val mealToInsert = Meal(mealId = 1, name = "mealTest")
            mealDao.insert(mealToInsert)
            val searchResults = mealDao.searchForMealStream(partialName).first()
            assert(searchResults.isEmpty()) { "Expected empty, but got ${searchResults}" }
        }
}
