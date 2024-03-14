package com.jimmy.parentsmealplanner.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jimmy.parentsmealplanner.data.MealRepository
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
import com.jimmy.parentsmealplanner.ui.shared.toMealDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.minus
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import javax.inject.Inject

/**
 * [ViewModel] for the meal planning screen.
 */
@HiltViewModel
class MealPlanningViewModel @Inject constructor(
    private val mealRepository: MealRepository,
): ViewModel() {

    private val _mealUiState = MutableStateFlow(MealUiState())
    val mealUiState: StateFlow<MealUiState> = _mealUiState

    private val _dateUiState = MutableStateFlow(DateUiState())
    var dateUiState: StateFlow<DateUiState> = _dateUiState

    /**
     * Initializes the [MealPlanningViewModel] and collects the [dateUiState] to get the meals for the
     * selected day. The [mealUiState] is updated when the MealsWithDishes flow updates or when
     * [dateUiState] changes, i.e. when the selected day changes.
     */
    init {
        viewModelScope.launch {
            _dateUiState.collect { dateUiState ->
                getMealsWithDishesStreamForSurroundingWeek(dateUiState.selectedDay).collect {
                    mealUiState -> _mealUiState.value = mealUiState
                }
            }
        }
    }

    fun updateSelectedDay(selectedDay: LocalDate) {
        _dateUiState.value = DateUiState(selectedDay = selectedDay)
    }

    private fun getMealsWithDishesStreamForSurroundingWeek(
        selectedDay: LocalDate = Clock.System.now().toLocalDateTime(
            TimeZone.currentSystemDefault()
        ).date
    ): StateFlow<MealUiState> {
        return mealRepository.getMealsWithDishesInDateRangeStream(
            dateStart = getFirstDayOfSurroundingWeek(selectedDay),
            dateEnd = getLastDayOfSurroundingWeek(selectedDay),
        ).filterNotNull()
            .map { MealUiState(it.map { mealWithDishes -> mealWithDishes.toMealDetails() })}
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
                initialValue = MealUiState(),
            )
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class MealUiState(
    val meals: List<MealDetails> = listOf(),
)

data class DateUiState(
    val selectedDay: LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
) {
    val daysOfSelectedWeek: List<LocalDate> = getDaysFromSurroundingWeek(selectedDay)
}

/**
 * Returns a list of [LocalDate]s for the days of the week surrounding the [selectedDay].
 */
fun getDaysFromSurroundingWeek(selectedDay: LocalDate): List<LocalDate> {
    val daysOfSurroundingWeek = mutableListOf<LocalDate>()
    val startOfWeek = getFirstDayOfSurroundingWeek(selectedDay)

    for (daysRemaining in 0..6) {
        daysOfSurroundingWeek.add(startOfWeek.plus(DatePeriod(days = daysRemaining)))
    }

    return daysOfSurroundingWeek
}

fun getFirstDayOfSurroundingWeek(selectedDay: LocalDate): LocalDate {
    val daysPastMonday = selectedDay.dayOfWeek.ordinal-1
    return selectedDay.minus(DatePeriod(days = daysPastMonday))
}

fun getLastDayOfSurroundingWeek(selectedDay: LocalDate): LocalDate {
    val daysUntilSunday = 7-selectedDay.dayOfWeek.ordinal
    return selectedDay.plus(DatePeriod(days = daysUntilSunday))
}

fun MealUiState.getMealsForDate(date: LocalDate): List<MealDetails> =
    meals.filter { details ->
        details.date == date
    }