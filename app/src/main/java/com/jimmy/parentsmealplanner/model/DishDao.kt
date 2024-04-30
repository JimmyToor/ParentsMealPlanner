package com.jimmy.parentsmealplanner.model

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface DishDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(dish: Dish): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dishes: List<Dish>): List<Long>

    @Update
    suspend fun update(dish: Dish)

    @Upsert
    suspend fun upsert(dish: Dish): Long

    @Upsert
    suspend fun upsertAll(dishes: List<Dish>): List<Long>

    @Delete
    suspend fun delete(dish: Dish)

    @Delete
    suspend fun deleteAll(dishes: List<Dish>)

    @Query("SELECT * from dishes WHERE dishId = :id")
    suspend fun getDish(id: Long): Dish?

    @Query("SELECT * from dishes WHERE name = :name")
    suspend fun getDishByName(name: String): Dish?

    @Query("SELECT * from dishes WHERE dishId = :id")
    fun getDishStream(id: Long): Flow<Dish>

    @Query("SELECT * from dishes")
    fun getAllDishesStream(): Flow<List<Dish>>

    @Transaction
    @Query("SELECT * FROM dishes WHERE dishId = :id")
    fun getDishWithMealsStream(id: Long): Flow<DishWithMeals>

    @Query("SELECT * FROM dishes WHERE name LIKE '%' || :searchTerm || '%'")
    fun searchForDish(searchTerm: String): Flow<List<Dish>>
}