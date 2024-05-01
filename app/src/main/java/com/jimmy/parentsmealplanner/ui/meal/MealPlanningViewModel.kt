package com.jimmy.parentsmealplanner.ui.meal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.jimmy.parentsmealplanner.data.MealRepository
import com.jimmy.parentsmealplanner.ui.shared.MealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.UserDetails
import com.jimmy.parentsmealplanner.ui.shared.toMealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.toUser
import com.jimmy.parentsmealplanner.ui.shared.toUserDetails
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
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

private const val FIRST_USER_ID = 1L

/**
 * [ViewModel] for the meal planning screen.
 */
@HiltViewModel
class MealPlanningViewModel @Inject constructor(
    private val mealRepository: MealRepository,
) : ViewModel() {
    private var _mealUiState = MutableStateFlow(MealUiState())
    val mealUiState: StateFlow<MealUiState> = _mealUiState

    private var _dateUiState = MutableStateFlow(DateUiState())
    val dateUiState: StateFlow<DateUiState> = _dateUiState

    private var _userUiState = MutableStateFlow(UserUiState())
    val userUiState: StateFlow<UserUiState> = _userUiState

    val loading = MutableStateFlow(true)

    /**
     * Initializes the [MealPlanningViewModel] and collects the [dateUiState] to get the meals for the
     * selected day. The [mealUiState] is updated when the MealsWithDishes flow updates or when
     * [dateUiState] changes, i.e. when the selected day changes.
     * Also initializes the [userUiState] and inserts an initial [User] to the database if it doesn't exist.
     */
    fun initializeData() {
        viewModelScope.launch {
            loading.value = true
            initializeUser()
            collectUsers()
            collectMeals()
            loading.value = false
        }
    }

    private suspend fun initializeUser() {
        when {
            mealRepository.getUser(FIRST_USER_ID) == null -> {
                mealRepository.insertUser(
                    UserDetails(id = FIRST_USER_ID, name = "User #$FIRST_USER_ID").toUser(),
                )
            }
        }
    }

    private suspend fun collectUsers() =
        viewModelScope.launch {
            mealRepository.getAllUsersStream().collectLatest { users ->
                val allUsersDetails = users.map { it.toUserDetails() }

                _userUiState.value =
                    userUiState.value.copy(
                        allUsersDetails = allUsersDetails,
                        selectedUserDetails =
                            if (_userUiState.value.selectedUserDetails.id == 0L) {
                                allUsersDetails.first { it.id == 1L }
                            } else {
                                allUsersDetails.first {
                                    it.id == _userUiState.value.selectedUserDetails.id
                                }
                            },
                    )
            }
        }

    private suspend fun collectMeals() =
        viewModelScope.launch {
            _dateUiState.collectLatest { dateUiState ->
                getMealsWithDishesStreamForSurroundingWeek(dateUiState.selectedDay).collectLatest {
                        mealUiState -> _mealUiState.value = mealUiState
                }
            }
    }

    fun updateSelectedDay(selectedDay: LocalDate) {
        _dateUiState.value = DateUiState(selectedDay = selectedDay)
    }

    /**
     * Increments the selected day in the dateUiState by the given number of days.
     *
     * This function increments the selected day in the [_dateUiState] by adding a specified number of days to it.
     * The incremented selected day is then wrapped in a new [DateUiState] object and assigned back to [_dateUiState].
     *
     * @param days The number of days to add to the current selected day. Can be negative to subtract days.
     */
    fun incrementSelectedDay(days: Int) {
        _dateUiState.value =
            DateUiState(
                selectedDay = _dateUiState.value.selectedDay.plus(DatePeriod(days = days))
            )
    }

    fun updateUserUiState(
        selectedUserDetails: UserDetails = userUiState.value.selectedUserDetails,
        allUsersDetails: List<UserDetails> = userUiState.value.allUsersDetails,
        targetUserDetails: UserDetails = userUiState.value.targetUserDetails,
    ) {
        _userUiState.value =
            userUiState.value.copy(
                selectedUserDetails = selectedUserDetails,
                allUsersDetails = allUsersDetails,
                targetUserDetails = targetUserDetails,
            )
    }

    fun updateSelectedUser(userId: Long) {
        updateUserUiState(
            selectedUserDetails = userUiState.value.allUsersDetails.first { it.id == userId },
        )
    }

    fun updateTargetUser(targetUserDetails: UserDetails) {
        updateUserUiState(
            targetUserDetails = targetUserDetails,
        )
    }

    fun addUser() {
        updateUserUiState(
            targetUserDetails = UserDetails(),
        )
    }

    fun editUser(userDetails: UserDetails) {
        updateUserUiState(
            targetUserDetails = userDetails,
        )
    }

    fun saveTargetUser() {
        val newUserDetails = userUiState.value.targetUserDetails
        var id = newUserDetails.id
        viewModelScope.launch {
            when (newUserDetails.id) {
                0L -> {
                    id = mealRepository.insertUser(newUserDetails.toUser())
                }
                else -> mealRepository.updateUser(newUserDetails.toUser())
            }

            updateTargetUser(userUiState.value.targetUserDetails.copy(id = id))
            when (_userUiState.value.selectedUserDetails.id) {
                id -> updateSelectedUser(id)
            }
        }
    }

    fun deleteUser(userDetails: UserDetails) {
        viewModelScope.launch {
            mealRepository.deleteUser(userDetails.toUser())
        }
    }

    fun deleteInstance(instanceId: Long) {
        viewModelScope.launch {
            mealRepository.deleteMealInstance(instanceId)
        }
    }

    private fun getMealsWithDishesStreamForSurroundingWeek(
        selectedDay: LocalDate = Clock.System.now().toLocalDateTime(
            TimeZone.currentSystemDefault()
        ).date
    ): StateFlow<MealUiState> {
        return mealRepository.getMealsWithDishesAndInstancesInDateRangeStream(
            dateStart = getFirstDayOfSurroundingWeek(selectedDay),
            dateEnd = getLastDayOfSurroundingWeek(selectedDay),
        ).filterNotNull()
            .map { mealsWithDishesAndAllInstances -> MealUiState(
                mealInstanceDetails = mealsWithDishesAndAllInstances.flatMap {
                    mealWithDishesAndInstances ->
                        mealWithDishesAndInstances.toMealInstanceDetails()
                }
            )}
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
    val mealInstanceDetails: List<MealInstanceDetails> = listOf(),
)

data class DateUiState(
    val selectedDay: LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
) {
    val daysOfSelectedWeek: List<LocalDate> = getDaysFromSurroundingWeek(selectedDay)
}

data class UserUiState(
    val selectedUserDetails: UserDetails = UserDetails(),
    val allUsersDetails: List<UserDetails> = listOf(),
    val targetUserDetails: UserDetails = UserDetails(),
)

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
    val daysPastMonday = selectedDay.dayOfWeek.ordinal
    return selectedDay.minus(DatePeriod(days = daysPastMonday))
}

fun getLastDayOfSurroundingWeek(selectedDay: LocalDate): LocalDate {
    val daysUntilSunday = 7 - selectedDay.dayOfWeek.ordinal
    return selectedDay.plus(DatePeriod(days = daysUntilSunday))
}

fun MealUiState.getMealInstancesForDateAndUser(date: LocalDate, userId: Long):
    List<MealInstanceDetails> =
        mealInstanceDetails.filter { mealInstanceDetail ->
            mealInstanceDetail.date == date && mealInstanceDetail.userId == userId
        }