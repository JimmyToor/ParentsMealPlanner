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
    suspend fun insert(dish: Dish)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(dishes: List<Dish>)

    @Update
    suspend fun update(dish: Dish)

    @Delete
    suspend fun delete(dish: Dish)

    @Query("SELECT * from dishes WHERE id = :id")
    suspend fun getDish(id: Int): Dish?

    @Query("SELECT * from dishes WHERE id = :id")
    fun getDishStream(id: Int): Flow<Dish>

    @Query("SELECT * from dishes")
    fun getAllDishesStream(): Flow<List<Dish>>

    @Transaction
    @Query("SELECT * FROM dishes WHERE id = :id")
    fun getDishWithMeals(id: Int): Flow<DishWithMeals>

    @Query("SELECT * FROM dishes WHERE name LIKE :searchQuery")
    fun searchForDish(searchQuery: String): Flow<List<Dish>>
}