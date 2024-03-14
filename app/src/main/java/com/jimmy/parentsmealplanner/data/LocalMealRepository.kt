package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.DishDao
import com.jimmy.parentsmealplanner.model.DishInMeal
import com.jimmy.parentsmealplanner.model.DishInMealDao
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealDao
import com.jimmy.parentsmealplanner.model.MealWithDishes
import com.jimmy.parentsmealplanner.ui.shared.toDishInMeal
import com.jimmy.parentsmealplanner.ui.shared.toDishesInMeal
import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class LocalMealRepository @Inject constructor(
    private val mealDao: MealDao,
    private val dishDao: DishDao,
    private val dishInMealDao: DishInMealDao
) : MealRepository {
    override fun getMealsInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<Meal>> = mealDao.getMealsInDateRange(dateStart, dateEnd)

    override fun getMealStream(id: Int): Flow<Meal?> = mealDao.getMealStream(id)

    override fun getMealsWithDishesInDateRangeStream(
        dateStart: LocalDate,
        dateEnd: LocalDate
    ): Flow<List<MealWithDishes>> = mealDao.getMealsWithDishesInDateRange(dateStart, dateEnd)

    override suspend fun getMealWithDishes(id: Int): MealWithDishes? = mealDao.getMealWithDishes(id)

    override fun getMealWithDishesStream(id: Int): Flow<MealWithDishes> =
        mealDao.getMealWithDishesStream(id)

    override suspend fun insertMeal(meal: Meal) = mealDao.insert(meal)

    override suspend fun insertMealWithDishes(mealWithDishes: MealWithDishes) {
        mealDao.insert(mealWithDishes.meal)
        dishDao.insertAll(mealWithDishes.dishes)
        dishInMealDao.insertAll(mealWithDishes.toDishesInMeal())
    }

    override suspend fun upsertMealWithDishes(mealWithDishes: MealWithDishes) {
        when (mealDao.getMeal(mealWithDishes.meal.id)) {
            null -> insertMealWithDishes(mealWithDishes)
            else -> {
                mealDao.update(mealWithDishes.meal)
                dishDao.insertAll(mealWithDishes.dishes)
                dishInMealDao.insertAll(mealWithDishes.toDishesInMeal())
            }
        }
    }

    override suspend fun deleteDishesFromMeal(mealId: Int, dishes: List<Dish>) {
        val dishesInMeal: List<DishInMeal> =
            dishes.map {
                    it.toDishInMeal(mealId)
            }
        dishInMealDao.deleteAll(dishesInMeal)
    }

    override fun searchForMeals(searchTerm: String): Flow<List<Meal>> {
        return mealDao.searchForMeals(searchTerm)
    }

    override suspend fun deleteMeal(meal: Meal) = mealDao.delete(meal)

    override suspend fun updateMeal(meal: Meal) = mealDao.update(meal)
}