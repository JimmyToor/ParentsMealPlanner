package com.jimmy.parentsmealplanner.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(meal: Meal)

    @Update
    suspend fun update(meal: Meal)

    @Delete
    suspend fun delete(meal: Meal)

    @Query("SELECT * FROM meals WHERE id = :id")
    suspend fun getMeal(id: Int): Meal

    @Query("SELECT * FROM meals WHERE id = :id")
    fun getMealStream(id: Int): Flow<Meal>

    @Query("SELECT * FROM meals")
    suspend fun getAllMeals(): List<Meal>

    @Query("SELECT * FROM meals")
    fun getAllMealsStream(): Flow<List<Meal>>

    @Transaction
    @Query("SELECT * FROM meals WHERE id = :id")
    fun getMealWithDishes(id: Int): Flow<MealWithDishes>

    @Transaction
    @Query("SELECT * FROM meals")
    fun getAllMealsWithDishes(): Flow<List<MealWithDishes>>

    @Query("SELECT * FROM meals WHERE date > :dateStart AND date < :dateEnd ORDER BY date DESC")
    fun getMealsInDateRange(dateStart: LocalDate, dateEnd: LocalDate): Flow<List<Meal>>

    @Transaction
    @Query("SELECT * FROM meals WHERE date > :dateStart AND date < :dateEnd ORDER BY date DESC")
    fun getMealsWithDishesInDateRange(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<MealWithDishes>>

    @Query("SELECT * FROM meals WHERE name LIKE :searchQuery")
    fun searchForMeal(searchQuery: String): Flow<List<Meal>>
}