package com.jimmy.parentsmealplanner.ui.meal

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jimmy.parentsmealplanner.data.MealRepository
import com.jimmy.parentsmealplanner.model.Meal
import com.jimmy.parentsmealplanner.model.MealWithDishes
import com.jimmy.parentsmealplanner.ui.shared.DishDetails
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.toDish
import com.jimmy.parentsmealplanner.ui.shared.toMeal
import com.jimmy.parentsmealplanner.ui.shared.toMealDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
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

    private val mealId: Int = checkNotNull(savedStateHandle[MealDetailDest.MEAL_ID_ARG])
    private val date: LocalDate =
        LocalDate.fromEpochDays(checkNotNull(savedStateHandle[MealDetailDest.DATE_ARG]))
    private val occasion: Occasion = checkNotNull(savedStateHandle[MealDetailDest.OCCASION_ARG])

    private var searchChar by mutableStateOf("")

    var mealDetailUiState by mutableStateOf(MealDetailUiState())
        private set
    var searchTerm by mutableStateOf("")
        private set


    // To avoid too many database queries, only retrieve new search results when the search
    // character changes rather than the entire search string
    @OptIn(ExperimentalCoroutinesApi::class)
    private val searchResults: StateFlow<List<Meal>> =
        snapshotFlow { searchChar }
            .filterNotNull()
            .flatMapLatest { searchChar ->
                mealRepository.searchForMeals(searchChar)
            }
            .stateIn(
                scope = viewModelScope,
                initialValue = emptyList(),
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS)
            )

    // Filter the search results every time the search term changes
    val filteredSearchResults: StateFlow<List<Meal>> =
        snapshotFlow { searchTerm }
            .combine(searchResults) { searchTerm, searchResults ->
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

    fun updateUiState(mealDetails: MealDetails) {
        mealDetailUiState =
            MealDetailUiState(meal = mealDetails, isEntryValid = validateInput(mealDetails))
    }

    init {
        viewModelScope.launch {
            mealDetailUiState = when (mealId == 0) {
                true -> { MealDetailUiState(meal = MealDetails(date = date, occasion = occasion)) }
                false -> {
                    MealDetailUiState(
                        meal =
                        mealRepository.getMealWithDishes(mealId)?.toMealDetails()
                            ?:MealDetails(date = date, occasion = occasion),
                        isEntryValid = true,
                    )
                }
            }
        }
    }

    fun onSearchTermChange(newTerm: String?) {
        when (newTerm.isNullOrBlank()) {
            false -> {
                searchTerm = newTerm
                if (searchTerm.first().toString() != searchChar) {
                    searchChar = searchTerm.first().toString()
                }
            }
            true -> {
                searchTerm = ""
                searchChar = ""
            }
        }
    }

    suspend fun saveMeal() {
        if (validateInput(mealDetailUiState.meal)) {
            if (mealDetailUiState.dishesToDelete.isNotEmpty()) {
                mealRepository.deleteDishesFromMeal(
                    mealId = mealId, dishes = mealDetailUiState.dishesToDelete.map { it.toDish() }
                )
            }
            mealRepository.upsertMealWithDishes(mealDetailUiState.toMealWithDishes())
        }
    }

    private fun validateInput(uiInput: MealDetails): Boolean {
        return with(uiInput) {
            name.isNotBlank()
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class MealDetailUiState(
    val meal: MealDetails = MealDetails(
        date = Clock.System.now().toLocalDateTime(
        TimeZone.currentSystemDefault()
    ).date),
    val dishesToDelete: List<DishDetails> = listOf(),
    val isEntryValid: Boolean = false,
)
fun MealDetailUiState.toMealWithDishes(): MealWithDishes = MealWithDishes(
    meal = meal.toMeal(),
    dishes = meal.dishes?.map { it.toDish() } ?: listOf(),
)