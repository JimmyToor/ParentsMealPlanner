package com.jimmy.parentsmealplanner.ui.meal

import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jimmy.parentsmealplanner.data.MealRepository
import com.jimmy.parentsmealplanner.model.MealWithDishesInstance
import com.jimmy.parentsmealplanner.ui.shared.DishDetails
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
import com.jimmy.parentsmealplanner.ui.shared.MealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.Rating
import com.jimmy.parentsmealplanner.ui.shared.toDishDetails
import com.jimmy.parentsmealplanner.ui.shared.toDishInMeal
import com.jimmy.parentsmealplanner.ui.shared.toInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.toMealDetails
import com.jimmy.parentsmealplanner.ui.shared.toMealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.toMealWithDishes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val mealRepository: MealRepository,
) : ViewModel(){

    private val mealId: Long = checkNotNull(savedStateHandle[MealDetailDest.MEAL_ID_ARG])
    private val date: LocalDate =
        LocalDate.fromEpochDays(checkNotNull(savedStateHandle[MealDetailDest.DATE_ARG]))
    private val occasion: Occasion = checkNotNull(savedStateHandle[MealDetailDest.OCCASION_ARG])
    private val userId: Long = checkNotNull(savedStateHandle[MealDetailDest.USER_ID_ARG])
    private val instanceId: Long = checkNotNull(savedStateHandle[MealDetailDest.INSTANCE_ID_ARG])

    // Search chars are used to query the database
    // Search terms are used to filter the results
    private var mealSearchChar by mutableStateOf("")
    private var dishSearchChar by mutableStateOf("")
    private var mealSearchTerm by mutableStateOf("")
    private var dishSearchTerm by mutableStateOf("")

    // Represents the current state of dishes saved in the database for this meal
    private val savedDishes = mutableListOf<DishDetails>()
    // Represents the dishes that will be deleted when the meal is saved
    private var dishesToDelete: Set<DishDetails> = setOf()
    // Holds the indices of dishes that are duplicates of another dish
    private val duplicateDishIndices = mutableSetOf<Int>()

    private val _mealDetailUiState = MutableStateFlow(MealDetailUiState())
    val mealDetailUiState: StateFlow<MealDetailUiState> = _mealDetailUiState

    init {
        viewModelScope.launch {
            _mealDetailUiState.value = when (instanceId == 0.toLong()) {
                true -> {
                    MealDetailUiState(mealInstanceDetails =
                        MealInstanceDetails(
                            mealDetails = MealDetails(),
                            date = date,
                            occasion = occasion,
                            userId = userId,
                        )
                    )
                }
                false -> {
                    val mealInstance = withContext(Dispatchers.IO) {
                        mealRepository.getMealWithDishesAndInstance(instanceId)
                            ?.toMealInstanceDetails()
                    }
                    mealInstance?.let {
                        onMealSearchTermChange(it.mealDetails.name)
                        savedDishes += it.mealDetails.dishes
                    }

                    MealDetailUiState(
                        mealInstanceDetails = mealInstance ?: MealInstanceDetails(
                            mealDetails = MealDetails(),
                            date = date,
                            occasion = occasion,
                            userId = userId,
                        ),
                        isEntryValid = true,
                    )
                }
            }
        }
    }

    // To avoid too many database queries, only retrieve new search results when the search
    // character changes rather than the entire search string
    @OptIn(ExperimentalCoroutinesApi::class)
    private val mealSearchResults: StateFlow<List<MealDetails>> =
        snapshotFlow { mealSearchChar }
            .filterNotNull()
            .flatMapLatest { searchChar ->
                mealRepository.searchForMealWithDishes(searchChar)
            }
            .map { searchResults -> searchResults.map { it.toMealDetails() } }
            .stateIn(
                scope = viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS)
            )

    // Filter the search results every time the search term changes
    val filteredMealSearchResults: StateFlow<List<MealDetails>> =
        snapshotFlow { mealSearchTerm }
            .combine(mealSearchResults) { searchTerm, searchResults ->
                if (searchTerm.isNotEmpty()) {
                    searchResults.filter { dish ->
                        dish.name.contains(searchTerm, ignoreCase = true)
                    }
                }
                else searchResults
            }
            .stateIn(
                scope = viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS)
            )

    // To avoid too many database queries, only retrieve new search results when the search
    // character changes rather than the entire search string
    @OptIn(ExperimentalCoroutinesApi::class)
    private val dishSearchResults: StateFlow<List<DishDetails>> =
        snapshotFlow { dishSearchChar }
            .filterNotNull()
            .flatMapLatest { searchChar ->
                mealRepository.searchForDish(searchChar)
            }
            .map { searchResults -> searchResults.map { it.toDishDetails() } }
            .stateIn(
                scope = viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS)
            )

    // Filter the search results every time the search term changes
    val filteredDishSearchResults: StateFlow<List<DishDetails>> =
        snapshotFlow { dishSearchTerm }
            .combine(dishSearchResults) { searchTerm, searchResults ->
                if (searchTerm.isNotEmpty()) {
                    searchResults.filter { dish ->
                        dish.name.contains(searchTerm, ignoreCase = true)
                    }
                }
                else searchResults
            }
            .stateIn(
                scope = viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS)
            )

    /**
     * This function is used to handle changes in the meal search term.
     * It updates the [mealSearchTerm] and [mealSearchChar] based on the input [newTerm].
     *
     * @param newTerm The new search term for meals.
     */
    fun onMealSearchTermChange(newTerm: String?) {
        mealSearchTerm = newTerm ?: ""

        val firstChar = mealSearchTerm.firstOrNull()?.toString() ?: ""
        if (firstChar != mealSearchChar) {
            mealSearchChar = firstChar
        }
    }

    /**
     * This function is used to handle changes in the dish search term.
     * It updates the [dishSearchTerm] and [dishSearchChar] based on the input [newTerm].
     *
     * @param newTerm The new search term for meals.
     */
    fun onDishSearchTermChange(newTerm: String?) {
        dishSearchTerm = newTerm ?: ""
        val firstChar = dishSearchTerm.firstOrNull()?.toString() ?: ""
        if (firstChar != dishSearchChar) {
            dishSearchChar = firstChar
        }
    }

    /**
     * Updates the UI state with the details (including dishes) of the existing meal if it exists.
     *
     * @param mealName The name of the meal to find.
     */
    fun findExistingMeal(mealName: String) {
        val existingMeal = filteredMealSearchResults.value.find { it.name == mealName }
        val differentMeal =
            (existingMeal != _mealDetailUiState.value.mealInstanceDetails.mealDetails)

        if (existingMeal == null) {
            updateUiState(
                mealDetails = MealDetails(
                    name = mealName,
                    dishes = mealDetailUiState.value.mealInstanceDetails.mealDetails.dishes,
                ),
            )

            dishesToDelete = emptySet()
            savedDishes.clear()
        } else if (differentMeal) {
            updateUiState(
                mealDetails = existingMeal,
            )

            dishesToDelete = emptySet()
            savedDishes.clear()
            savedDishes += existingMeal.dishes
        }

    }

    /**
     * Updates the UI state with the details of the existing dish if it exists.
     *
     * @param index The index of the dish in the meal details to be replaced.
     * @param dishName The name of the dish to find.
     */
    fun findExistingDish(index: Int, dishName: String) {
        val existingDish = filteredDishSearchResults.value.find { it.name == dishName }
        val updatedDishes =
            mealDetailUiState.value.mealInstanceDetails.mealDetails.dishes.toMutableList()

        if (existingDish != null && existingDish != updatedDishes[index]) {
            markDishForDeletion(updatedDishes[index])
            updatedDishes[index] = existingDish
            updateUiState(
                mealDetails = mealDetailUiState.value.mealInstanceDetails.mealDetails.copy(
                    dishes = updatedDishes
                )
            )
        }
    }

    /**
     * Updates the UI state and search term with the new meal name.
     *
     * @param newName The new name for the meal.
     */
    fun changeMeal(newName: String) {
        Log.d("MealDetailViewModel", "changeMeal: $newName")
        onMealSearchTermChange(newTerm = newName)
        updateUiState(
            mealDetails =
                mealDetailUiState.value.mealInstanceDetails.mealDetails.copy(name = newName)
        )
    }

    /**
     * Updates the UI state with the new meal rating.
     *
     * @param newRating The new rating for the meal
     */
    fun changeMealRating(newRating: Rating) {
        updateUiState(
            mealDetails =
                mealDetailUiState.value.mealInstanceDetails.mealDetails.copy(rating = newRating)
        )
    }


    /**
     * This function is used to change the dish at index from one dish to another.
     *
     * @param index The index of the dish in the meal details to be changed.
     * @param newName The new name for the dish.
     */
    fun changeDish(index: Int, newName: String) {
        val updatedDishes =
            mealDetailUiState.value.mealInstanceDetails.mealDetails.dishes.toMutableList()
        val targetDish = updatedDishes[index]

        markDishForDeletion(targetDish)

        updatedDishes[index] = targetDish.copy(
            dishId = 0,
            name = newName,
        )

        onDishSearchTermChange(newTerm = newName)
        updateUiState(
            mealDetailUiState.value.mealInstanceDetails.mealDetails.copy(dishes = updatedDishes)
        )
    }

    /**
     * Updates the UI state with the new dish details.
     *
     * @param dishDetails The new dish details to update the UI with
     */
    fun addDish(dishDetails: DishDetails) {
        val updatedDishes =
            mealDetailUiState.value.mealInstanceDetails.mealDetails.dishes + dishDetails

        updateUiState(
            mealDetails = mealDetailUiState.value.mealInstanceDetails.mealDetails.copy(
                dishes = updatedDishes
            )
        )

    }

    /**
     * Checks if a dish is already saved in the list of dishes for the meal.
     *
     * @param dishDetails The details of the dish to check.
     * @return Boolean value indicating whether the dish is saved (true) or not (false).
     */
    fun isDishSaved(dishDetails: DishDetails): Boolean {
        return savedDishes.any { it.dishId == dishDetails.dishId }
    }


    /**
     * Updates the dish at the given index with
     * the new name and rating while maintaining id.
     * Updates the UI state if the update was successful.
     *
     * @param index The index of the dish to be updated.
     * @param newName The new name for the dish.
     * @param newRating The new rating for the dish.
     * @return Boolean value indicating whether the update was successful.
     */
    suspend fun updateDish(index: Int, newName: String, newRating: Rating): Boolean {
        val updatedDishes =
            mealDetailUiState.value.mealInstanceDetails.mealDetails.dishes.toMutableList()
        val oldDish = updatedDishes[index]

        updatedDishes[index] = oldDish.copy(
            name = newName,
            rating = newRating
        )

        mealRepository.getDishByName(newName)?.let {
            if (it.dishId != oldDish.dishId) {
                return false // Name is already taken by another dish
            }
        }

        // In case there are duplicates in the UI state, update them to match the database
        updatedDishes.replaceAll {
            if (it.name == oldDish.name) it.copy(
                name = newName,
                rating = newRating,
            ) else it
        }

        updateUiState(
            mealDetails = mealDetailUiState.value.mealInstanceDetails.mealDetails.copy(
                dishes = updatedDishes
            )
        )
        return true
    }

    /**
     * Updates the meal with the new name.
     * Updates the UI state if the update was successful.
     *
     * @param newName The new name for the meal.
     * @return Boolean value indicating whether the update was successful.
     */
    suspend fun updateMealName(newName: String): Boolean {
        val updatedMeal = mealDetailUiState.value.mealInstanceDetails.mealDetails.copy(
            name = newName,
        )

        val mealRecord = mealRepository.getMealByName(newName)
        if (mealRecord != null && mealRecord.mealId != updatedMeal.mealId)
            return false // Name is already taken by another meal
        
        updateUiState(mealDetails = updatedMeal)
        return true
    }

    /**
     * Updates the UI state with the new image source for the meal.
     *
     * @param imageSrc The new image source for the meal.
     */
    fun updateImage(imageSrc: String) {
        updateUiState(
            mealDetails =
                mealDetailUiState.value.mealInstanceDetails.mealDetails.copy(imgSrc = imageSrc)
        )
    }

    /**
     * Deletes a dish from the meal.
     *
     * Dishes saved in the database will be marked for deletion and the function returns false.
     * Otherwise, the dish is removed from the list immediately and the function returns true.
     * Updates the UI state if the deletion was immediate.
     *
     * @param index The index of the dish to be deleted.
     * @return Boolean value indicating whether the dish was immediately
     * removed (true) or marked for deletion (false).
     */
    fun deleteDish(index: Int): Boolean {
        val targetDish = mealDetailUiState.value.mealInstanceDetails.mealDetails.dishes[index]

        // If the dish is saved as part of the meal in the db, it needs to be marked for deletion
        if (markDishForDeletion(targetDish)) {
            return false
        }
        else { // otherwise it can just be removed from the list immediately
            val updatedDishes =
                mealDetailUiState.value.mealInstanceDetails.mealDetails.dishes.minus(
                    targetDish
                )

            updateUiState(
                mealDetails = mealDetailUiState.value.mealInstanceDetails.mealDetails.copy(
                    dishes = updatedDishes,
                ),
            )
            return true
        }
    }

    /**
     * Marks a dish for deletion if it is saved as part of the meal in the database
     * and appears exactly once in the UI state's list of dishes.
     *
     * @param targetDish The dish to be marked for deletion.
     * @return Boolean value indicating whether the dish was
     * marked for deletion (true) or not (false).
     */
    private fun markDishForDeletion(targetDish: DishDetails): Boolean {
        val dishes = mealDetailUiState.value.mealInstanceDetails.mealDetails.dishes
        if (isDishSaved(targetDish) && dishes.count { it == targetDish } == 1) {
            dishesToDelete += targetDish
            return true
        }
        return false
    }

    /**
     * Removes the dish from the list of dishes to be deleted.
     *
     * @param dishDetails The dish to be unmarked for deletion.
     */
    fun unMarkDishForDeletion(dishDetails: DishDetails) {
        dishesToDelete -= dishDetails
    }

    /**
     * Updates the UI state with new meal instance details.
     * If any parameter is not provided or null, it retains the existing value.
     * Also validates the input.
     *
     * @param mealInstanceDetails The new meal instance details to update the UI with
     */
    fun updateUiState(
        mealInstanceDetails: MealInstanceDetails? = null,
    ) {
        val newMealInstance = mealInstanceDetails ?: mealDetailUiState.value.mealInstanceDetails
        updateIndices(newMealInstance.mealDetails.dishes)

        _mealDetailUiState.value =
            MealDetailUiState(
                mealInstanceDetails = newMealInstance,
                isEntryValid = validateInput(newMealInstance),
            )
    }

    /**
     * Updates the UI state with new meal details.
     * If any parameter is not provided or null, it retains the existing value.
     * Also validates the input.
     *
     * @param mealDetails The new meal details to update the UI with
     */
    fun updateUiState(
        mealDetails: MealDetails? = null,
    ) {
        val newMealInstance = if (mealDetails != null) {
            mealDetailUiState.value.mealInstanceDetails.copy(
                mealDetails = mealDetails
            )
        } else {
            mealDetailUiState.value.mealInstanceDetails
        }
        updateUiState(mealInstanceDetails = newMealInstance)
    }

    /**
     * Updates the indices of duplicate dishes in the meal based on the passed list of dishes.
     *
     * @param dishes The list of dishes in the meal.
     */
    private fun updateIndices(dishes: List<DishDetails>) {
        duplicateDishIndices.clear()

        dishes.forEachIndexed { index, dish ->
            val dishCount = dishes.count {
                it.name.lowercase(Locale.getDefault()) == dish.name.lowercase(Locale.getDefault())
            }
            if (dishCount > 1) {
                duplicateDishIndices.add(index)
            }
        }
    }

    /**
     * Checks if a dish at a given index is a duplicate.
     *
     * @param index The index of the dish to check.
     * @return Boolean value indicating whether the dish at the given index is a duplicate.
     */
    fun isDuplicate(index: Int): Boolean {
        return duplicateDishIndices.contains(index)
    }

    /**
     * Saves all meal and dish details to the database.
     * Updates the UI state with the saved details.
     */
    suspend fun saveMeal() {
        if (dishesToDelete.isNotEmpty()) {
            mealRepository.deleteDishesFromMeal(
                dishesToDelete.map {
                    it.toDishInMeal(mealDetailUiState.value.mealInstanceDetails.mealDetails.mealId)
                }
            )
        }

        val updatedDishes =
            mealDetailUiState.value.mealInstanceDetails.mealDetails.dishes - dishesToDelete
        val updatedMealDetails =
            mealDetailUiState.value.mealInstanceDetails.mealDetails.copy(dishes = updatedDishes)
        val updatedInstance =
            mealDetailUiState.value.mealInstanceDetails.copy(mealDetails = updatedMealDetails)

        val savedInstance = mealRepository.upsertMealWithDishesInstance(
            updatedInstance.toInstance()
        ).toMealInstanceDetails()

        updateUiState(mealInstanceDetails = savedInstance)
    }


    /**
     * Validates the input for a meal instance by checking for the following:
     * - If the name of the meal is not blank.
     * - If there are no duplicate dishes in the meal.
     *
     * @param uiInput The meal instance details to validate.
     * @return Boolean value indicating whether the input is valid.
     */
    private fun validateInput(uiInput: MealInstanceDetails): Boolean {
        return uiInput.mealDetails.name.isNotBlank() && duplicateDishIndices.isEmpty()

    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class MealDetailUiState(
    val mealInstanceDetails: MealInstanceDetails = MealInstanceDetails(
        mealDetails = MealDetails(),
        date = Clock.System.now().toLocalDateTime(
        TimeZone.currentSystemDefault()
    ).date),
    var isEntryValid: Boolean = false,
)

fun MealInstanceDetails.toInstance(removeEmptyDishes: Boolean = true): MealWithDishesInstance =
    MealWithDishesInstance(
        mealWithDishes =
            mealDetails.toMealWithDishes(removeEmptyDishes = removeEmptyDishes),
        instanceDetails = toInstanceDetails()
    )