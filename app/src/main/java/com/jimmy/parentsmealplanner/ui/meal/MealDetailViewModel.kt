package com.jimmy.parentsmealplanner.ui.meal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jimmy.parentsmealplanner.data.MealRepository
import com.jimmy.parentsmealplanner.model.Instance
import com.jimmy.parentsmealplanner.ui.shared.DishDetails
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
import com.jimmy.parentsmealplanner.ui.shared.MealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.toDish
import com.jimmy.parentsmealplanner.ui.shared.toDishDetails
import com.jimmy.parentsmealplanner.ui.shared.toInstanceDetails
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

    var mealDetailUiState by mutableStateOf(MealDetailUiState())
        private set
    var mealSearchTerm by mutableStateOf("")
        private set
    var dishSearchTerm by mutableStateOf("")
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
                mealRepository.searchForMeal(searchChar)
            }
            .map { searchResults -> searchResults.map { it.toMealDetails() } } // convert for
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
        snapshotFlow { mealSearchChar }
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
                if (mealSearchTerm.first().toString() != mealSearchChar) {
                    mealSearchChar = mealSearchTerm.first().toString()
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
        if (existingMeal != null && existingMeal != mealDetailUiState.mealInstanceDetails.mealDetails) {
            updateUiState(mealDetails = existingMeal, dishesToDelete = emptyList())
        }
    }

    fun updateMealName(newName: String) {
        onMealSearchTermChange(newTerm = newName)
        updateUiState(
            mealDetails = mealDetailUiState.mealInstanceDetails.mealDetails.copy(name = newName)
        )
    }

    fun updateDishName(index: Int, newName: String) {
        val updatedDishes = mealDetailUiState.mealInstanceDetails.mealDetails.dishes.toMutableList()
        updatedDishes[index] = updatedDishes[index].copy(name = newName)
        onDishSearchTermChange(newTerm = newName)
        updateUiState(mealDetailUiState.mealInstanceDetails.mealDetails.copy(dishes = updatedDishes))
    }

    fun findExistingDish(index: Int, dishName: String) {
        val existingDish = filteredDishSearchResults.value.find { it.name == dishName }
        if (existingDish != null) {
            val updatedDishes = mealDetailUiState.mealInstanceDetails.mealDetails.dishes.toMutableList()
            updatedDishes[index] = updatedDishes[index].copy(dishId = existingDish.dishId)
            updateUiState(
                mealDetailUiState.mealInstanceDetails.mealDetails.copy(dishes = updatedDishes)
            )
        }
    }

    fun markDishForDeletion(index: Int) {
        updateUiState(
            mealDetails = null,
            dishesToDelete = mealDetailUiState.dishesToDelete
                + mealDetailUiState.mealInstanceDetails.mealDetails.dishes[index]
            )
    }

    fun unMarkDishForDeletion(index: Int) {
        updateUiState(
            mealDetails = null,
            dishesToDelete = mealDetailUiState.dishesToDelete
                - mealDetailUiState.mealInstanceDetails.mealDetails.dishes[index]
        )
    }

    /**
     * Updates the UI state with new meal instance details and dishes to delete.
     * If mealDetails or dishesToDelete are not provided or null, it retains the existing values.
     * Also validates the input.
     *
     * @param mealInstanceDetails The new meal instance details to update the UI with
     * @param dishesToDelete The list of dish details to delete
     */
    fun updateUiState(
        mealInstanceDetails: MealInstanceDetails? = null,
        dishesToDelete: List<DishDetails>? = null
    ) {
        val newMealInstance = mealInstanceDetails ?: mealDetailUiState.mealInstanceDetails
        mealDetailUiState =
            MealDetailUiState(
                mealInstanceDetails = newMealInstance,
                dishesToDelete = dishesToDelete ?: mealDetailUiState.dishesToDelete,
                isEntryValid = validateInput(newMealInstance),
            )
    }

    /**
     * Updates the UI state with new meal details and dishes to delete.
     * If mealDetails or dishesToDelete are not provided or null, it retains the existing values.
     * Also validates the input.
     *
     * @param mealDetails The new meal details to update the UI with
     * @param dishesToDelete The list of dish details to delete
     */
    fun updateUiState(mealDetails: MealDetails? = null, dishesToDelete: List<DishDetails>? = null) {
        val newMealDetails = mealDetails ?: mealDetailUiState.mealInstanceDetails.mealDetails
        val newMealInstance = mealDetailUiState.mealInstanceDetails.copy(mealDetails = newMealDetails)
        mealDetailUiState =
            MealDetailUiState(
                mealInstanceDetails = newMealInstance,
                dishesToDelete = dishesToDelete ?: mealDetailUiState.dishesToDelete,
                isEntryValid = validateInput(newMealInstance),
            )
    }

    suspend fun saveMeal() {
        if (validateInput(mealDetailUiState.mealInstanceDetails)) {
            if (mealDetailUiState.dishesToDelete.isNotEmpty()) {
                mealRepository.deleteDishesFromMeal(
                    mealId = mealId, dishes = mealDetailUiState.dishesToDelete.map { it.toDish() }
                )
            }
            val updatedInstance = mealRepository.upsertInstance(mealDetailUiState.toInstance())
            updateUiState(mealInstanceDetails = updatedInstance.toMealInstanceDetails())
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
    val dishesToDelete: List<DishDetails> = listOf(),
    val isEntryValid: Boolean = false,
)

fun MealDetailUiState.toInstance(removeEmptyDishes: Boolean = true): Instance =
    Instance(
        mealWithDishes =
            mealInstanceDetails.mealDetails.toMealWithDishes(removeEmptyDishes = removeEmptyDishes),
        instanceDetails = mealInstanceDetails.toInstanceDetails()
    )