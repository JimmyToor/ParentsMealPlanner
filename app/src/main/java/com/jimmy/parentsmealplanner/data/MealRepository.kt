package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealWithDishes
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate

interface MealRepository {
    fun getMealsInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate): Flow<List<Meal>>

    fun getMealStream(id: Int): Flow<Meal?>

    fun getMealsWithDishesInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<MealWithDishes>>

    fun getMealWithDishesStream(id: Int): Flow<MealWithDishes?>

    suspend fun insertItem(meal: Meal)

    suspend fun deleteItem(meal: Meal)

    suspend fun updateItem(meal: Meal)
}
