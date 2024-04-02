package com.jimmy.parentsmealplanner.data

import com.jimmy.parentsmealplanner.model.Dish
import com.jimmy.parentsmealplanner.model.DishDao
import com.jimmy.parentsmealplanner.model.DishInMeal
import com.jimmy.parentsmealplanner.model.DishInMealDao
import com.jimmy.parentsmealplanner.model.Instance
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealDao
import com.jimmy.parentsmealplanner.model.MealInstance
import com.jimmy.parentsmealplanner.model.MealInstanceDao
import com.jimmy.parentsmealplanner.model.MealWithDishes
import com.jimmy.parentsmealplanner.model.MealWithDishesAndAllInstances
import com.jimmy.parentsmealplanner.model.User
import com.jimmy.parentsmealplanner.model.UserDao
import com.jimmy.parentsmealplanner.model.toDish
import com.jimmy.parentsmealplanner.model.toDishInMeal
import com.jimmy.parentsmealplanner.model.toDishesInMeal
import com.jimmy.parentsmealplanner.model.toMealInstance
import com.jimmy.parentsmealplanner.model.toMealWithDishes
import com.jimmy.parentsmealplanner.ui.shared.UserDetails
import com.jimmy.parentsmealplanner.ui.shared.toUser
import com.jimmy.parentsmealplanner.ui.shared.toUserDetails
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.datetime.LocalDate
import javax.inject.Inject

class LocalMealRepository @Inject constructor(
    private val mealDao: MealDao,
    private val dishDao: DishDao,
    private val dishInMealDao: DishInMealDao,
    private val mealInstanceDao: MealInstanceDao,
    private val userDao: UserDao,
) : MealRepository {
    override fun getMealsInDateRangeStream(dateStart: LocalDate, dateEnd: LocalDate):
        Flow<List<Meal>> = mealDao.getMealsInDateRangeStream(dateStart, dateEnd)

    override fun getMealStream(id: Long): Flow<Meal?> = mealDao.getMealStream(id)

    override fun getInstanceInDateRangeStream(
        dateStart: LocalDate,
        dateEnd: LocalDate
    ): Flow<List<Instance>> =
        mealDao.getMealsWithDishesAndInstancesInDateRangeStream(dateStart, dateEnd)

    override fun getMealsWithDishesAndInstancesInDateRangeStream(
        dateStart: LocalDate,
        dateEnd: LocalDate
    ): Flow<List<MealWithDishesAndAllInstances>> =
        mealDao.getMealsWithDishesAndAllInstancesInDateRangeStream(dateStart, dateEnd)

    override suspend fun getMealWithDishes(id: Long): MealWithDishes? =
        mealDao.getMealWithDishes(id)

    override suspend fun getMealWithDishesAndInstance(instanceId: Long):
        Instance? = mealDao.getMealWithDishesAndInstance(instanceId)

    override fun getMealWithDishesStream(id: Long): Flow<MealWithDishes> =
        mealDao.getMealWithDishesStream(id)

    override suspend fun insertMeal(meal: Meal): Long = mealDao.insert(meal)

    override suspend fun insertMealWithDishes(
        mealWithDishes: MealWithDishes
    ): MealWithDishes {
        val mealId = mealDao.insert(mealWithDishes.meal)
        val dishIds = dishDao.insertAll(mealWithDishes.dishes)

        // Set ids of meal and dishes
        val mealWithDishesWithIds = mealWithDishes.copy(
            meal = mealWithDishes.meal.copy(mealId = mealId),
            dishes = dishIds.mapIndexed {
                index, newId -> mealWithDishes.dishes[index].copy(dishId = newId)
            }
        )
        dishInMealDao.insertAll(mealWithDishesWithIds.toDishesInMeal())
        return mealWithDishesWithIds
    }

    override suspend fun updateMealWithDishes(
        mealWithDishes: MealWithDishes
    ): MealWithDishes {
        mealDao.update(mealWithDishes.meal)
        val dishIds = dishDao.insertAll(mealWithDishes.dishes)


        // Set ids of new dishes
        val updatedDishes = dishIds.mapIndexed { index, newId ->
            if (newId != -1L) {
                mealWithDishes.dishes[index].copy(dishId = newId)
            } else {
                mealWithDishes.dishes[index]
            }
        }

        dishInMealDao.insertAll(updatedDishes.toDishesInMeal(mealId = mealWithDishes.meal.mealId))
        return mealWithDishes.copy(
            dishes = updatedDishes
        )
    }

    /**
     * Upsert an instance and return the updated instance.
     * @param instance the instance to upsert
     * @return the updated instance
     */
    override suspend fun upsertInstance(
        instance: Instance): Instance {
        val updatedMealWithDishes =
            when (mealDao.getMeal(instance.mealWithDishes.meal.mealId) == null) {
                true -> insertMealWithDishes(instance.toMealWithDishes())
                false -> updateMealWithDishes(instance.toMealWithDishes())
            }

        instance.mealWithDishes = updatedMealWithDishes
        val newInstanceId = upsertMealInstance(instance.toMealInstance())
        instance.instanceDetails.mealInstanceId = newInstanceId
        instance.mealWithDishes = updatedMealWithDishes

        return instance
    }

    override suspend fun insertMealInstance(mealInstance: MealInstance): Long =
        mealInstanceDao.insert(mealInstance)

    override suspend fun updateMealInstance(mealInstance: MealInstance) =
        mealInstanceDao.update(mealInstance)

    override suspend fun deleteMealInstance(instanceId: Long) =
        mealInstanceDao.deleteById(instanceId)

    override suspend fun upsertMealInstance(mealInstance: MealInstance): Long {
        var id = mealInstance.mealInstanceId
        if (mealInstanceDao.getMealInstance(mealInstance.mealInstanceId) == null) {
            id = insertMealInstance(mealInstance)
        } else {
            updateMealInstance(mealInstance)
        }
        return id
    }

    override suspend fun deleteDishesFromMeal(mealId: Long, dishes: List<Dish>) {
        val dishesInMeal: List<DishInMeal> =
            dishes.map {
                    it.toDishInMeal(mealId)
            }
        dishInMealDao.deleteAll(dishesInMeal)
        deleteDishesInNoMeal(dishes)
    }

    override suspend fun deleteDishesInNoMeal(dishes: List<Dish>)
    {
        val dishesFound = dishInMealDao.getDishesInMeals(dishes.map { it.dishId }).toSet()
        val dishesToDelete: Set<Dish> = dishes.subtract(dishesFound.map
        { it.toDish() }.toSet())

        dishDao.deleteAll(dishesToDelete.toList())
    }

    override fun searchForMeal(searchTerm: String): Flow<List<Meal>> {
        return when (searchTerm.isBlank()) {
            true -> flowOf(emptyList())
            false -> mealDao.searchForMealStream(searchTerm)
        }
    }

    override fun searchForDish(searchTerm: String): Flow<List<Dish>> {
        return when (searchTerm.isBlank()) {
            true -> flowOf(emptyList())
            false -> dishDao.searchForDish(searchTerm)
        }
    }

    override suspend fun deleteMeal(meal: Meal) = mealDao.delete(meal)

    override suspend fun updateMeal(meal: Meal) = mealDao.update(meal)

    override suspend fun getUserDetails(id: Long): UserDetails? = userDao.getUser(id)?.toUserDetails()

    override suspend fun updateUserName(id: Long, name: String) = userDao.update(User(id, name))

    override suspend fun insertUser(user: UserDetails): Long = userDao.insert(user.toUser())
}