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
interface DishDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(dish: Dish): Long

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(dishes: List<Dish>): List<Long>

    @Update
    suspend fun update(dish: Dish)

    @Delete
    suspend fun delete(dish: Dish)

    @Delete
    suspend fun deleteAll(dishes: List<Dish>)

    @Query("SELECT * from dishes WHERE dishId = :id")
    suspend fun getDish(id: Long): Dish?

    @Query("SELECT * from dishes WHERE dishId = :id")
    fun getDishStream(id: Long): Flow<Dish>

    @Query("SELECT * from dishes")
    fun getAllDishesStream(): Flow<List<Dish>>

    @Transaction
    @Query("SELECT * FROM dishes WHERE dishId = :id")
    fun getDishWithMeals(id: Long): Flow<DishWithMeals>

    @Query("SELECT * FROM dishes WHERE name LIKE :searchQuery")
    fun searchForDish(searchQuery: String): Flow<List<Dish>>
}