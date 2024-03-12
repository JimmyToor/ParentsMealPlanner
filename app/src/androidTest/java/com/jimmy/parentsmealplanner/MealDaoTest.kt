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
import com.jimmy.parentsmealplanner.model.MealPlannerDatabase
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
            assert(retrievedMeals[0] == meal)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDao_Insert multiple meals and retrieve`() =
        runBlocking {
            val meals = Datasource.loadMeals()
            mealDao.insertAll(meals)
            val retrievedMeals = mealDao.getAllMeals()
            assert(retrievedMeals.containsAll(meals))
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve all meals`() =
        runBlocking {
            val meals = Datasource.loadMeals()
            mealDao.insertAll(meals)
            val retrievedMeals = mealDao.getAllMeals()
            assert(retrievedMeals.containsAll(meals))
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Update non-existent meal`() =
        runBlocking {
            val nonExistentMeal =
                Meal(
                    100,
                    Occasion.BREAKFAST,
                    Rating.LOVEIT,
                    "non-existent",
                    LocalDate(2021, 1, 1),
                )
            mealDao.update(nonExistentMeal)
            val retrievedMeal = mealDao.getMeal(nonExistentMeal.id)
            assert(retrievedMeal == null)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Get meal with dishes by id`() =
        runBlocking {
            val mealsWithDishes = Datasource.loadMealsWithDishes()
            val dishesInMeals = Datasource.loadDishInMeals()
            mealDao.insert(mealsWithDishes[0].meal)
            dishDao.insertAll(mealsWithDishes[0].dishes)
            dishInMealDao.insertAll(dishesInMeals.filter { it.mealId == mealsWithDishes[0].meal.id })
            val retrievedMealWithDishes = mealDao.getMealWithDishes(mealsWithDishes[0].meal.id)
            assert(retrievedMealWithDishes != null)
            if (retrievedMealWithDishes != null) {
                assert(retrievedMealWithDishes == mealsWithDishes[0])
            }
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Get meals in date range`() =
        runBlocking {
            val mealToInsert1 = Meal(id = 1, name = "mealTest1", date = LocalDate(2021, 1, 1))
            val mealToInsert2 = Meal(id = 2, name = "mealTest2", date = LocalDate(1990, 1, 1))
            mealDao.insert(mealToInsert1)
            mealDao.insert(mealToInsert2)
            val startDate = LocalDate(2021, 1, 1)
            val endDate = LocalDate(2021, 1, 2)
            val retrievedMeals = mealDao.getMealsInDateRange(startDate, endDate).first()
            assert(retrievedMeals.size == 1)
            assert(retrievedMeals[0] == mealToInsert1)
        }

    // Search for a meal using a search query that does not match any meal names
    @Test
    @Throws(Exception::class)
    fun `MealDAO_Search for non-existent meal`() =
        runBlocking {
            val searchQuery = "non-existent"
            val searchResults = mealDao.searchForMeals(searchQuery).first()
            assert(searchResults.isEmpty())
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
            assert(retrievedMeals.containsAll(meals))
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
            assert(retrievedMeal.id == 1)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Update meal`() =
        runBlocking {
            val meal = Datasource.loadMeals()[0]
            mealDao.insert(meal)
            val updatedMeal = meal.copy(name = "Updated Meal")
            mealDao.update(updatedMeal)
            val retrievedMeal = mealDao.getMeal(updatedMeal.id)
            assert(retrievedMeal == updatedMeal)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Delete meal`() =
        runBlocking {
            val meal = Datasource.loadMeals()[0]
            mealDao.insert(meal)
            val mealToDelete = meal
            mealDao.delete(mealToDelete)
            val retrievedMeal = mealDao.getMeal(mealToDelete.id)
            assert(retrievedMeal == null)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve meal by id with getMeal`() =
        runBlocking {
            val meal = Datasource.loadMeals()[0]
            mealDao.insert(meal)
            val retrievedMeal = mealDao.getMeal(meal.id)
            assert(retrievedMeal == meal)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve meal by id with getMealStream`() =
        runBlocking {
            val meal = Datasource.loadMeals()[0]
            mealDao.insert(meal)
            val retrievedMealFlow = mealDao.getMealStream(meal.id)
            val retrievedMeal = retrievedMealFlow.first()
            assert(retrievedMeal == meal)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve all meals with getAllMeals`() =
        runBlocking {
            val meals = Datasource.loadMeals()
            meals.forEach { mealDao.insert(it) }
            val retrievedMeals = mealDao.getAllMeals()
            assert(retrievedMeals == meals)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Delete non-existent meal with getMeal`() =
        runBlocking {
            val nonExistentMeal = Meal(id = 100, name = "Non-existent Meal")
            mealDao.delete(nonExistentMeal)
            val retrievedMeal = mealDao.getMeal(nonExistentMeal.id)
            assert(retrievedMeal == null)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve non-existent meal by id with getMeal`() =
        runBlocking {
            val nonExistentMealId = 100
            val retrievedMeal = mealDao.getMeal(nonExistentMealId)
            assert(retrievedMeal == null)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve non-existent meal stream by id`() =
        runBlocking {
            val nonExistentMealId = 100
            val retrievedMealFlow = mealDao.getMealStream(nonExistentMealId)
            assert(retrievedMealFlow.firstOrNull() == null)
        }

    // Retrieve a meal with dishes that does not exist using getMealWithDishes() method.
    @Test
    @Throws(Exception::class)
    fun `MealDAO_Retrieve meal with dishes that does not exist`() =
        runBlocking {
            val nonExistentMealId = 100
            val retrievedMealWithDishes = mealDao.getMealWithDishes(nonExistentMealId)
            assert(retrievedMealWithDishes == null)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Search for existing meal by partial name`() =
        runBlocking {
            val partialName = "meal"
            val mealToInsert = Meal(id = 1, name = "mealTest")
            mealDao.insert(mealToInsert)
            val searchResults = mealDao.searchForMeals(partialName).first()
            assert(searchResults.isNotEmpty())
            assert(searchResults.first() == mealToInsert)
        }

    @Test
    @Throws(Exception::class)
    fun `MealDAO_Search for non-existing meal by partial name`() =
        runBlocking {
            val partialName = "noName"
            val mealToInsert = Meal(id = 1, name = "mealTest")
            mealDao.insert(mealToInsert)
            val searchResults = mealDao.searchForMeals(partialName).first()
            assert(searchResults.isEmpty())
        }
}
