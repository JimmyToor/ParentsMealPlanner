package com.jimmy.parentsmealplanner

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jimmy.parentsmealplanner.data.Datasource.Companion.loadDishInMeals
import com.jimmy.parentsmealplanner.data.Datasource.Companion.loadDishes
import com.jimmy.parentsmealplanner.data.Datasource.Companion.loadMeals
import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.DishDao
import com.jimmy.parentsmealplanner.model.DishInMeal
import com.jimmy.parentsmealplanner.model.DishInMealDao
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealDao
import com.jimmy.parentsmealplanner.model.MealPlannerDatabase
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

class DisInMealDaoTest {
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
    fun `DishInMealDao_Insert dish in meal and retrieve`() =
        runBlocking<Unit> {
            val dishInMeal = loadDishInMeals()[0]
            val meal = Meal(id = dishInMeal.mealId)
            val dish = Dish(id = dishInMeal.dishId)
            mealDao.insert(meal)
            dishDao.insert(dish)
            dishInMealDao.insert(dishInMeal)
            val retrievedDishInMeal = dishInMealDao.getAllDishesInMeals().first()
            assert(retrievedDishInMeal == dishInMeal)
        }

    @Test
    @Throws(Exception::class)
    fun `DishInMealDao_Insert multiple dishes in meals and retrieve`() =
        runBlocking<Unit> {
            val meals = loadMeals()
            val dishes = loadDishes()
            val dishesInMeals = loadDishInMeals()
            mealDao.insertAll(meals)
            dishDao.insertAll(dishes)
            dishInMealDao.insertAll(dishesInMeals)
            val retrievedDishesInMeals = dishInMealDao.getAllDishesInMeals()
            assert(retrievedDishesInMeals.containsAll(dishesInMeals))
        }

    @Test
    @Throws(Exception::class)
    fun `DishInMealDao_Delete dish in meal`() =
        runBlocking<Unit> {
            val dishInMeal = loadDishInMeals()[0]
            val meal = Meal(id = dishInMeal.mealId)
            val dish = Dish(id = dishInMeal.dishId)
            mealDao.insert(meal)
            dishDao.insert(dish)
            dishInMealDao.insert(dishInMeal)
            dishInMealDao.delete(dishInMeal)
            val retrievedDishInMeal = dishInMealDao.getAllDishesInMeals().firstOrNull()
            assert(retrievedDishInMeal == null)
        }

    @Test
    @Throws(Exception::class)
    fun `DishInMealDao_Delete all dishes in meals`() =
        runBlocking<Unit> {
            val dishesInMeals = loadDishInMeals()
            mealDao.insertAll(loadMeals())
            dishDao.insertAll(loadDishes())
            dishInMealDao.insertAll(dishesInMeals)
            dishInMealDao.deleteAll(dishesInMeals)
            val retrievedDishesInMeals = dishInMealDao.getAllDishesInMeals().firstOrNull()
            assert(retrievedDishesInMeals == null)
        }

    @Test(expected = SQLiteConstraintException::class)
    @Throws(Exception::class)
    fun `DishInMealDao_Insert dish in meal with non-existent meal foreign key`() =
        runBlocking<Unit> {
            val dishInMeal = DishInMeal(100, loadDishInMeals()[0].dishId)
            val meal = Meal(id = 1)
            val dish = Dish(id = dishInMeal.dishId)
            mealDao.insert(meal)
            dishDao.insert(dish)

            dishInMealDao.insert(dishInMeal)
        }

    @Test(expected = SQLiteConstraintException::class)
    @Throws(Exception::class)
    fun `DishInMealDao_Insert dish in meal with non-existent dish foreign key`() =
        runBlocking<Unit> {
            val dishInMeal = DishInMeal(loadDishInMeals()[0].mealId, 100)
            val meal = Meal(id = dishInMeal.mealId)
            val dish = Dish(id = 1)
            mealDao.insert(meal)
            dishDao.insert(dish)
            dishInMealDao.insert(dishInMeal)
        }
}
