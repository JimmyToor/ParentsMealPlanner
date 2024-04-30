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
import com.jimmy.parentsmealplanner.ui.shared.toDish
import com.jimmy.parentsmealplanner.ui.shared.toDishDetails
import com.jimmy.parentsmealplanner.ui.shared.toDishInMeal
import com.jimmy.parentsmealplanner.ui.shared.toInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.toMeal
import com.jimmy.parentsmealplanner.ui.shared.toMealDetails
import com.jimmy.parentsmealplanner.ui.shared.toMealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.toMealWithDishes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import javax.inject.Inject

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

    private var mealSearchChar by mutableStateOf("")
    private var dishSearchChar by mutableStateOf("")
    private var mealSearchTerm by mutableStateOf("")
    private var dishSearchTerm by mutableStateOf("")

    private val savedDishes = mutableListOf<DishDetails>()
    private var dishesToDelete: Set<DishDetails> = setOf()
    private val duplicateDishIndices = mutableSetOf<Int>()

    var mealDetailUiState by mutableStateOf(MealDetailUiState())
        private set

    init {
        viewModelScope.launch {
            mealDetailUiState = when (instanceId == 0.toLong()) {
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
                when {
                    searchTerm.isNotEmpty() -> searchResults.filter {
                            meal -> meal.name.contains(searchTerm, ignoreCase = true)
                    }
                    else -> searchResults
                }
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
                when {
                    searchTerm.isNotEmpty() -> {
                        searchResults.filter {
                                dish -> dish.name.contains(searchTerm, ignoreCase = true)
                        }
                    }
                    else -> searchResults
                }
            }
            .stateIn(
                scope = viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS)
            )

    fun onMealSearchTermChange(newTerm: String?) {
        when (newTerm.isNullOrBlank()) {
            true -> {
                mealSearchTerm = ""
                mealSearchChar = ""
            }
            false -> {
                mealSearchTerm = newTerm
                val firstChar = mealSearchTerm.first().toString()
                if (firstChar != mealSearchChar) {
                    mealSearchChar = firstChar
                }
            }
        }
    }

    fun onDishSearchTermChange(newTerm: String?) {
        when (newTerm.isNullOrBlank()) {
            true -> {
                dishSearchTerm = ""
                dishSearchChar = ""
            }
            false -> {
                dishSearchTerm = newTerm
                if (dishSearchTerm.first().toString() != dishSearchChar) {
                    dishSearchChar = dishSearchTerm.first().toString()
                }
            }
        }
    }

    /**
     * Use an existing meal by name. Useful if the user types in the name of an existing meal
     * instead of selecting it from the search results
     *
     * @param mealName The name of the meal to use.
     */
    fun findExistingMeal(mealName: String) {
        val existingMeal = filteredMealSearchResults.value.find { it.name == mealName }
        val differentMeal = (existingMeal != mealDetailUiState.mealInstanceDetails.mealDetails)

        if (existingMeal != null) {
            if (differentMeal) {
                updateUiState(
                    mealDetails = existingMeal,
                )
                dishesToDelete = emptySet()
                savedDishes += existingMeal.dishes
            }
        }
        else {
            updateUiState(
                mealDetails = MealDetails(
                    name = mealName,
                    dishes = if (mealDetailUiState.mealInstanceDetails.mealDetails.mealId == 0L) {
                        mealDetailUiState.mealInstanceDetails.mealDetails.dishes
                    }
                    else emptyList()
                ),
            )
            dishesToDelete = emptySet()
            savedDishes.clear()
        }
    }

    fun findExistingDish(index: Int, dishName: String) {
        val existingDish = filteredDishSearchResults.value.find { it.name == dishName }
        val differentDish =
            (existingDish != mealDetailUiState.mealInstanceDetails.mealDetails.dishes[index])
        val updatedDishes = mealDetailUiState.mealInstanceDetails.mealDetails.dishes.toMutableList()

        if (existingDish != null) {
            if (differentDish) {
                markDishForDeletion(updatedDishes[index])
                updatedDishes[index] = existingDish
                updateUiState(
                    mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(
                        dishes = updatedDishes
                    ),
                )
            }
        }
        else {
            updateUiState(
                mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(
                    dishes = updatedDishes,
                ),
            )
        }
    }

    fun changeMeal(newName: String) {
        onMealSearchTermChange(newTerm = newName)
        updateUiState(
            mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(name = newName)
        )
    }

    fun changeDish(index: Int, newName: String) {
        val updatedDishes = mealDetailUiState.mealInstanceDetails.mealDetails.dishes.toMutableList()
        val targetDish = updatedDishes[index]

        markDishForDeletion(targetDish)

        updatedDishes[index] = targetDish.copy(
            dishId = 0,
            name = newName,
        )

        onDishSearchTermChange(newTerm = newName)
        updateUiState(
            mealDetailUiState.mealInstanceDetails.mealDetails.copy(dishes = updatedDishes)
        )
    }

    fun addDish(dishDetails: DishDetails) {
        updateUiState(
            mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(
                dishes = mealDetailUiState.mealInstanceDetails.mealDetails.dishes + dishDetails
            )
        )
    }

    suspend fun updateDishName(index: Int, newName: String): Boolean {
        val updatedDishes = mealDetailUiState.mealInstanceDetails.mealDetails.dishes.toMutableList()
        val savedDishIndex = savedDishes.indexOf(updatedDishes[index])
        val oldDish = updatedDishes[index]

        updatedDishes[index] = updatedDishes[index].copy(
            name = newName
        )

        val updateSuccess = mealRepository.updateDish(updatedDishes[index].toDish())

        if (updateSuccess) {
            savedDishes[savedDishIndex] = updatedDishes[index]
            updatedDishes.replaceAll { if (it.name == oldDish.name) it.copy(dishId = 0) else it }

            updateUiState(
                mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(
                    dishes = updatedDishes
                )
            )
        }

        return updateSuccess
    }

    suspend fun updateMealName(newName: String): Boolean {
        if (!mealRepository.updateMeal(
                mealDetailUiState.mealInstanceDetails.mealDetails.copy(name = newName).toMeal())
            )
            return false


        updateUiState(
            mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(name = newName)
        )
        return true
    }

    fun updateImage(imageSrc: String) {
        updateUiState(
            mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(imgSrc = imageSrc)
        )
    }

    /**
     * Deletes a dish from the meal.
     *
     * This function checks if the dish is saved as part of the meal in the database.
     * If it is, the dish is marked for deletion and the function returns false.
     * If the dish is not saved in the database, it is removed from the list immediately and the function returns true.
     *
     * @param index The index of the dish to be deleted.
     * @return Boolean value indicating whether the dish was immediately removed (true) or marked for deletion (false).
     */
    fun deleteDish(index: Int): Boolean {
        val targetDish = mealDetailUiState.mealInstanceDetails.mealDetails.dishes[index]

        // If the dish is saved as part of the meal in the db, it needs to be marked for deletion
        if (markDishForDeletion(targetDish)) {
            Log.d("DEBUG", "Dish marked for deletion: $targetDish" )
            return false
        }
        else { // otherwise it can just be removed from the list immediately
            Log.d("DEBUG", "Dish deleted immediately: $targetDish" )
            val updatedDishes =
                mealDetailUiState.mealInstanceDetails.mealDetails.dishes.minus(
                    targetDish
                )

            updateUiState(
                mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(
                    dishes = updatedDishes,
                ),
            )
            return true
        }
    }

    /**
     * Marks a dish for deletion if it is contained in the saved dishes
     * and exactly once in the list of all dishes.
     *
     * @param targetDish The dish to be marked for deletion.
     * @return Boolean value indicating whether the dish was marked for deletion (true) or not (false).
     */
    private fun markDishForDeletion(targetDish: DishDetails): Boolean {
        val dishes = mealDetailUiState.mealInstanceDetails.mealDetails.dishes
        if (savedDishes.contains(targetDish) && dishes.count{it == targetDish} == 1) {
            dishesToDelete += targetDish
            return true
        }
        return false
    }

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
        val newMealInstance = mealInstanceDetails ?: mealDetailUiState.mealInstanceDetails
        updateIndices(newMealInstance.mealDetails.dishes)

        mealDetailUiState =
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
        val newMealInstance = when (mealDetails != null) {
             true -> {
                mealDetailUiState.mealInstanceDetails.copy(
                    mealDetails = mealDetails
                )
            }
            false -> {
                mealDetailUiState.mealInstanceDetails
            }
        }
        updateUiState(mealInstanceDetails = newMealInstance)
    }

    private fun updateIndices(dishes: List<DishDetails>) {
        duplicateDishIndices.clear()
        val dishIndices: MutableMap<String,MutableSet<Int>> = mutableMapOf()
        dishes.forEachIndexed { index, dish ->
            dishIndices.getOrPut(dish.name) { mutableSetOf() }.add(index)
        }

        dishIndices.forEach { (_, indices) ->
            if (indices.size > 1) {
                duplicateDishIndices.addAll(indices)
            }
        }
    }

    fun isDuplicate(index: Int): Boolean {
        return duplicateDishIndices.contains(index)
    }

    suspend fun saveMeal() {
        if (dishesToDelete.isNotEmpty()) {
            mealRepository.deleteDishesFromMeal(
                dishesToDelete.map {
                    it.toDishInMeal(mealDetailUiState.mealInstanceDetails.mealDetails.mealId)
                }
            )
        }

        val updatedDishes =
            mealDetailUiState.mealInstanceDetails.mealDetails.dishes - dishesToDelete
        val updatedMealDetails =
            mealDetailUiState.mealInstanceDetails.mealDetails.copy(dishes = updatedDishes)
        val updatedInstance =
            mealDetailUiState.mealInstanceDetails.copy(mealDetails = updatedMealDetails)

        val savedInstance = mealRepository.upsertMealWithDishesInstance(
            updatedInstance.toInstance()
        ).toMealInstanceDetails()
        updateUiState(mealInstanceDetails = savedInstance)
    }


    // Check that no dish names are duplicated (blank duplicates are ignored)
    // Indices of duplicates are stored in duplicateDishIndices
    private fun validateInput(uiInput: MealInstanceDetails): Boolean {
        return with(uiInput) {
            mealDetails.name.isNotBlank() && duplicateDishIndices.isEmpty()
        }
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