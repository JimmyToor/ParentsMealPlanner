package com.jimmy.parentsmealplanner

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.jimmy.parentsmealplanner.data.Datasource
import com.jimmy.parentsmealplanner.model.DishDao
import com.jimmy.parentsmealplanner.model.DishInMealDao
import com.jimmy.parentsmealplanner.model.MealDao
import com.jimmy.parentsmealplanner.model.MealPlannerDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.io.IOException

class DishDaoTest {
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
    fun `DishDao_Insert dish and retrieve`() =
        runBlocking<Unit> {
            val dish = Datasource.loadDishes()[0]
            dishDao.insert(dish)
            val retrievedDish = dishDao.getDish(dish.dishId)
            assert(retrievedDish == dish) { "Expected ${dish}, but got ${retrievedDish}" }
        }

    @Test
    @Throws(Exception::class)
    fun `DishDao_Insert multiple dishes and retrieve`() =
        runBlocking<Unit> {
            val dishes = Datasource.loadDishes()
            dishDao.insertAll(dishes)
            val retrievedDishes = dishDao.getAllDishesStream().first()
            assert(retrievedDishes == dishes) { "Expected ${dishes}, but got ${retrievedDishes}" }
        }

    @Test
    @Throws(Exception::class)
    fun `DishDao_Update dish`() =
        runBlocking<Unit> {
            val dish = Datasource.loadDishes()[0]
            dishDao.insert(dish)
            val updatedDish = dish.copy(name = "Updated Dish")
            dishDao.update(updatedDish)
            val retrievedDish = dishDao.getDish(dish.dishId)
            assert(retrievedDish == updatedDish) { "Expected ${updatedDish}, but got ${retrievedDish}" }
        }

    @Test
    @Throws(Exception::class)
    fun `DishDao_Delete dish`() =
        runBlocking<Unit> {
            val dish = Datasource.loadDishes()[0]
            dishDao.insert(dish)
            dishDao.delete(dish)
            val retrievedDish = dishDao.getDish(dish.dishId)
            assert(retrievedDish == null) { "Expected null, but got $retrievedDish" }
        }

    @Test
    @Throws(Exception::class)
    fun `DishDao_Get dish by ID`() =
        runBlocking<Unit> {
            val dish = Datasource.loadDishes()[0]
            dishDao.insert(dish)
            val retrievedDish = dishDao.getDish(dish.dishId)
            assert(retrievedDish == dish) { "Expected ${dish}, but got $retrievedDish" }
        }

    @Test
    @Throws(Exception::class)
    fun `DishDao_Get dish stream by ID`() =
        runBlocking<Unit> {
            val dish = Datasource.loadDishes()[0]
            dishDao.insert(dish)
            val retrievedDishFlow = dishDao.getDishStream(dish.dishId)
            val retrievedDish = retrievedDishFlow.firstOrNull()
            assert(retrievedDish == dish) { "Expected ${dish}, but got $retrievedDish" }
        }

    @Test
    @Throws(Exception::class)
    fun `DishDao_Get dish with invalid ID`() =
        runBlocking<Unit> {
            val retrievedDish = dishDao.getDish(-1)
            assert(retrievedDish == null) { "Expected null, but got $retrievedDish" }
        }

    @Test
    @Throws(Exception::class)
    fun `DishDao_Get dish stream with invalid ID`() =
        runBlocking<Unit> {
            val retrievedDishFlow = dishDao.getDishStream(-1)
            val retrievedDish = retrievedDishFlow.firstOrNull()
            assert(retrievedDish == null) { "Expected null, but got $retrievedDish" }
        }
}
