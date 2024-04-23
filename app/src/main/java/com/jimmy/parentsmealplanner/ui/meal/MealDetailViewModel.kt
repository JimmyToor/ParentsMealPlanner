package com.jimmy.parentsmealplanner.ui.meal

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

    private val savedDishes = mutableSetOf<DishDetails>()
    private var dishesToDelete: Set<DishDetails> = setOf()

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
        val differentDish = (existingDish != mealDetailUiState.mealInstanceDetails.mealDetails.dishes[index])
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

        if (mealDetailUiState.isEntryValid) {
            markDishForDeletion(targetDish)
        }

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

    fun updateDishName(index: Int, newName: String) {
        val updatedDishes = mealDetailUiState.mealInstanceDetails.mealDetails.dishes.toMutableList()
        updatedDishes[index] = updatedDishes[index].copy(
            name = newName
        )
        viewModelScope.launch {
            mealRepository.updateDish(updatedDishes[index].toDish())
        }
        updateUiState(
            mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(
                dishes = updatedDishes
            )
        )
    }

    fun updateMealName(newName: String) {
        updateUiState(
            mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(name = newName)
        )
        viewModelScope.launch {
            mealRepository.updateMeal(mealDetailUiState.mealInstanceDetails.mealDetails.toMeal())
        }
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
            return false
        }
        else { // otherwise it can just be removed from the list immediately
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
     * Marks a dish for deletion if it is contained in the saved dishes.
     *
     * @param targetDish The dish to be marked for deletion.
     * @return Boolean value indicating whether the dish was marked for deletion (true) or not (false).
     */
    private fun markDishForDeletion(targetDish: DishDetails): Boolean {
        if (savedDishes.contains(targetDish)) {
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

        mealDetailUiState =
            MealDetailUiState(
                mealInstanceDetails = newMealInstance,
                isEntryValid = validateInput(newMealInstance),
            )
    }

    suspend fun saveMeal() {
        if (validateInput(mealDetailUiState.mealInstanceDetails)) {
            viewModelScope.launch {
                if (dishesToDelete.isNotEmpty()) {
                    mealRepository.deleteDishesFromMeal(
                        dishesToDelete.map { it.toDishInMeal(
                            mealId = mealDetailUiState.mealInstanceDetails.mealDetails.mealId
                        ) }
                    )
                }
                updateUiState(
                    mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.apply {
                        dishes.subtract(dishesToDelete)
                    }
                )
                val updatedInstance =
                    mealRepository.upsertMealWithDishesInstance(mealDetailUiState.toInstance())
                updateUiState(mealInstanceDetails = updatedInstance.toMealInstanceDetails())
            }
        }
    }

    private fun validateInput(uiInput: MealInstanceDetails): Boolean {
        return with(uiInput) { // Check that no non-blank dish names are duplicated
            val duplicateNames = mealDetails.dishes.filter { it.name.isNotBlank() }
                .groupingBy { it.name }
                .eachCount()
                .filter { it.value > 1 }
                .keys
            mealDetails.name.isNotBlank() && duplicateNames.isEmpty()
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

fun MealDetailUiState.toInstance(removeEmptyDishes: Boolean = true): MealWithDishesInstance =
    MealWithDishesInstance(
        mealWithDishes =
            mealInstanceDetails.mealDetails.toMealWithDishes(removeEmptyDishes = removeEmptyDishes),
        instanceDetails = mealInstanceDetails.toInstanceDetails()
    )