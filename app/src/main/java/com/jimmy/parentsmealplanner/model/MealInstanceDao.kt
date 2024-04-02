package com.jimmy.parentsmealplanner.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

@Dao
interface MealInstanceDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(meal: MealInstance): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(meals: List<MealInstance>): List<Long>

    @Update
    suspend fun update(meal: MealInstance)

    @Delete
    suspend fun delete(meal: MealInstance)

    @Query("DELETE FROM meal_instances WHERE mealInstanceId = :id")
    suspend fun deleteById(id: Long)

    @Query("SELECT * FROM meal_instances WHERE mealInstanceId = :id")
    suspend fun getMealInstance(id: Long): MealInstance?

    @Query("SELECT * FROM meal_instances " +
        "WHERE date >= :dateStart AND date <= :dateEnd ORDER BY date DESC")
    fun getMealInstanceInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<MealInstance?>
}