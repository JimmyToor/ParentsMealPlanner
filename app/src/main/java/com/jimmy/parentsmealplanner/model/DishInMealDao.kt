package com.jimmy.parentsmealplanner.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DishInMealDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(dishInMeal: DishInMeal)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(dishesInMeals: List<DishInMeal>)

    @Delete
    suspend fun delete(dishInMeal: DishInMeal)

    @Delete
    suspend fun deleteAll(dishesInMeals:List<DishInMeal>)

    @Query("SELECT * FROM dishes_in_meals WHERE dishId = :dishId AND mealId = :mealId")
    suspend fun getDishInMeal(dishId: Int, mealId: Int): DishInMeal?

    @Query("SELECT * from dishes_in_meals WHERE dishId IN (:dishIds)")
    suspend fun getDishesInMeals(dishIds: List<Long>): List<DishInMeal>

    @Query("SELECT * FROM dishes_in_meals")
    suspend fun getAllDishesInMeals(): List<DishInMeal>
}