package com.jimmy.parentsmealplanner.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(meal: Meal)

    @Update
    suspend fun update(meal: Meal)

    @Delete
    suspend fun delete(meal: Meal)

    @Query("SELECT * FROM meals WHERE id = :id")
    fun getMeal(id: Int): Flow<Meal>

    @Query("SELECT * FROM meals")
    fun getAllMeals(id: Int): Flow<Meal>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :id")
    fun getMealWithDishes(id: Int): Flow<MealWithDishes>

    @Transaction
    @Query("SELECT * FROM meals")
    fun getAllMealsWithDishes(): Flow<List<MealWithDishes>>

    @Query("SELECT * FROM meals WHERE date > :dateStart AND date < :dateEnd ORDER BY date DESC")
    fun getMealsInDateRange(dateStart: Long, dateEnd: Long): Flow<List<Meal>>

    @Transaction
    @Query("SELECT * FROM meals WHERE date > :dateStart AND date < :dateEnd ORDER BY date DESC")
    fun getMealsWithDishesInDateRange(dateStart: Long, dateEnd: Long): Flow<List<MealWithDishes>>

    @Query("SELECT * FROM meals WHERE name LIKE :searchQuery")
    fun searchForMeal(searchQuery: String): Flow<List<Meal>>
}