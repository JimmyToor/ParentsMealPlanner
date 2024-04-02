package com.jimmy.parentsmealplanner.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.RewriteQueriesToDropUnusedColumns
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface MealDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(meal: Meal): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(meals: List<Meal>): List<Long>

    @Update
    suspend fun update(meal: Meal)

    @Delete
    suspend fun delete(meal: Meal)

    @Query("SELECT * FROM ViewMeals WHERE mealId = :id")
    suspend fun getMeal(id: Long): Meal?

    @Query("SELECT * FROM ViewMeals WHERE mealId = :id")
    fun getMealStream(id: Long): Flow<Meal>

    @Query("SELECT * FROM ViewMeals")
    suspend fun getAllMeals(): List<Meal>

    @Query("SELECT * FROM ViewMeals")
    fun getAllMealsStream(): Flow<List<Meal>>

    @Transaction
    @Query("SELECT * FROM ViewMeals WHERE mealId = :id")
    suspend fun getMealWithDishes(id: Long): MealWithDishes?

    @Transaction
    @Query("SELECT * FROM ViewMealsAndInstances WHERE mealInstanceId = :instanceId")
    fun getMealWithDishesAndInstance(instanceId: Long): Instance?

    @Transaction
    @Query("SELECT * FROM ViewMeals WHERE mealId = :id")
    fun getMealWithDishesStream(id: Long): Flow<MealWithDishes>

    @Transaction
    @Query("SELECT * FROM ViewMeals")
    fun getAllMealsWithDishesStream(): Flow<List<MealWithDishes>>

    @Transaction
    @Query("SELECT * FROM ViewMealsAndInstances " +
        "WHERE date >= :dateStart AND date <= :dateEnd ORDER BY date DESC"
    )
    fun getMealsWithDishesAndInstancesInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<Instance>>

    @Transaction
    @RewriteQueriesToDropUnusedColumns
    @Query("SELECT * FROM ViewMealsAndInstances " +
        "WHERE date >= :dateStart AND date <= :dateEnd ORDER BY date DESC"
    )
    fun getMealsWithDishesAndAllInstancesInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate)
        : Flow<List<MealWithDishesAndAllInstances>>

    @Query("SELECT * FROM ViewMeals WHERE name LIKE '%' || :searchTerm || '%'")
    fun searchForMealStream(searchTerm: String?): Flow<List<Meal>>

    @Transaction
    @Query("SELECT * FROM ViewMeals " +
        "WHERE mealId IN (SELECT DISTINCT(mealId) " +
        "FROM meal_instances " +
        "WHERE date >= :dateStart AND date <= :dateEnd ORDER BY date DESC)"
    )
    fun getMealsInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<Meal>>
}