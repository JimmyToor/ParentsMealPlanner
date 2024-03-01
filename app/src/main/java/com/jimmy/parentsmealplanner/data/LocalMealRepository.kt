package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealDao
import com.jimmy.parentsmealplanner.model.MealWithDishes
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class LocalMealRepository @Inject constructor(private val mealDao: MealDao) : MealRepository {
    override fun getMealsInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<Meal>> = mealDao.getMealsInDateRange(dateStart, dateEnd)

    override fun getMealStream(id: Int): Flow<Meal?> = mealDao.getMealStream(id)

    override fun getMealsWithDishesInDateRangeStream(
        dateStart: LocalDate,
        dateEnd: LocalDate
    ): Flow<List<MealWithDishes>> = mealDao.getMealsWithDishesInDateRange(dateStart, dateEnd)

    override fun getMealWithDishesStream(id: Int): Flow<MealWithDishes?> =
        mealDao.getMealWithDishes(id)

    override suspend fun insertItem(meal: Meal) = mealDao.insert(meal)

    override suspend fun deleteItem(meal: Meal) = mealDao.delete(meal)

    override suspend fun updateItem(meal: Meal) = mealDao.update(meal)
}