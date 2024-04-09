package com.jimmy.parentsmealplanner.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface DishInMealDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dishInMeal: DishInMeal)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dishesInMeals: List<DishInMeal>)

    @Upsert
    suspend fun upsert(dishInMeal: DishInMeal)

    @Upsert
    suspend fun upsertAll(dishesInMeals: List<DishInMeal>)

    @Delete
    suspend fun delete(dishInMeal: DishInMeal)

    @Delete(entity = DishInMeal::class)
    suspend fun deleteAll(dishesInMeal: List<DishInMeal>)
    @Query("SELECT * FROM dishes_in_meals WHERE dishId = :dishId AND mealId = :mealId")
    suspend fun getDishInMeal(dishId: Long, mealId: Long): DishInMeal?

    @Query("SELECT * from dishes_in_meals WHERE dishId IN (:dishIds)")
    suspend fun getDishesInMeals(dishIds: List<Long>): List<DishInMeal>

    @Query("SELECT * FROM dishes_in_meals")
    suspend fun getAllDishesInMeals(): List<DishInMeal>
}