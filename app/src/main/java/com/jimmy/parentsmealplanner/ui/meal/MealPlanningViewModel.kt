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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
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

const val DEFAULT_USER_ID = 1L

/**
 * [ViewModel] for the meal planning screen.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class MealPlanningViewModel @Inject constructor(
    private val mealRepository: MealRepository,
) : ViewModel() {
    private val selectedUser: MutableStateFlow<Long?> = MutableStateFlow(null)
    private val selectedDay: MutableStateFlow<LocalDate?> =
        MutableStateFlow(Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date)
    private val targetUserDetails: MutableStateFlow<UserDetails> = MutableStateFlow(UserDetails())
    val loading = MutableStateFlow(true)

    val userUiState: StateFlow<UserUiState> = selectedUser.flatMapLatest (::userUiStateOfUserId)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = UserUiState(),
        )

    val mealUiState: StateFlow<MealUiState> = selectedDay.flatMapLatest (::mealUiStateOfDate)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(TIMEOUT_MILLIS),
            initialValue = MealUiState(),
        )


    /**
     * Creates a [Flow] of [MealUiState] containing all the [MealInstanceDetails] for the selected date.
     * The [Flow] is then shared in the [viewModelScope] and will remain active as long as there are active subscribers.
     *
     * @param date The selected date for which the [MealUiState] is to be created. If null, an empty [MealUiState] is returned.
     * @return A [Flow] of [MealUiState] based on the provided date.
     */
    private fun mealUiStateOfDate(date: LocalDate?): Flow<MealUiState> {
        if (date == null) return flowOf(MealUiState())
        return mealRepository.getMealsWithDishesAndInstancesInDateRangeStream(
            dateStart = getFirstDayOfSurroundingWeek(date),
            dateEnd = getLastDayOfSurroundingWeek(date),
        ).filterNotNull()
            .map { mealsWithDishesAndAllInstances -> MealUiState(
                mealInstanceDetails = mealsWithDishesAndAllInstances.flatMap {
                        mealWithDishesAndInstances ->
                    mealWithDishesAndInstances.toMealInstanceDetails()
                },
                selectedDay = date
            )}
    }

    /**
     * Creates a [Flow] of [UserUiState] based on the provided user ID as the selected user.
     * The resulting [UserUiState] contains the details of the selected user, all users, and the target user.
     * The [Flow] is then shared in the [viewModelScope] and will remain active as long as there are active subscribers.
     *
     * The [Flow] is updated whenever targetUserDetails or the list of all users in the [mealRepository] changes.
     *
     * If no users are found in the meal repository, a new user is inserted with the ID [DEFAULT_USER_ID] and a name "User #FIRST_USER_ID".
     *
     * @param userId The ID of the user for which the [UserUiState] is to be created. If null, a new user is inserted into the [mealRepository] and its ID is assigned to [selectedUser].
     * @return A [Flow] of [UserUiState] based on the provided user ID.
     */
    private suspend fun userUiStateOfUserId(userId: Long?): Flow<UserUiState> {
        if (userId == null) {
            if (mealRepository.getAllUsers().isEmpty()) {
                mealRepository.insertUser(
                    UserDetails(id = DEFAULT_USER_ID, name = "User #$DEFAULT_USER_ID").toUser()
                )
            }
            selectedUser.value = mealRepository.getAllUsers().first().userId
        }

        return combine(
            targetUserDetails,
            mealRepository.getAllUsersStream(),
        ) { targetUserDetails, allUsers ->
            UserUiState(
                selectedUserDetails = allUsers.firstOrNull { it.userId == userId }?.toUserDetails()
                    ?:allUsers.first().toUserDetails(),
                allUsersDetails = allUsers.map { it.toUserDetails() },
                targetUserDetails = targetUserDetails,
            )
        }
    }


    /**
     * Initializes the data for the ViewModel.
     * Sets the loading state to true until users are loaded.
     */
    fun initializeData() {
        loading.value = true

        viewModelScope.launch {
            userUiState.firstOrNull { it.allUsersDetails.isNotEmpty() }
            loading.value = false
        }
    }

    fun updateSelectedDay(newSelectedDay: LocalDate) {
        selectedDay.value = newSelectedDay
    }

    /**
     * Increments the selected day in the dateUiState by the given number of days.
     *
     * @param days The number of days to add to the current selected day. Can be negative to subtract days.
     */
    fun incrementSelectedDay(days: Int) {
        selectedDay.value?.let { updateSelectedDay(it.plus(DatePeriod(days = days))) }
    }

    /**
     * Updates the selected user in the userUiState.
     */
    fun updateSelectedUser(userId: Long) {
        selectedUser.value = userId
    }

    /**
     * Updates the target user in the userUiState.
     */
    fun updateTargetUser(newTargetUserDetails: UserDetails = UserDetails()) {
        targetUserDetails.value = newTargetUserDetails
    }

    /**
     * Saves/updates the [userUiState]'s target user in the meal repository.
     *
     * @return True if the user was saved successfully.
     *
     * False if a user with the same name already exists.
     */
    suspend fun saveTargetUser(): Boolean {
        val newUserDetails = userUiState.value.targetUserDetails

        if (userUiState.value.allUsersDetails.any { it.name == newUserDetails.name }) {
            return false // Username already exists
        }

        var id = newUserDetails.id

        when (newUserDetails.id) {
            0L -> {
                id = mealRepository.insertUser(newUserDetails.toUser())
            }
            else -> mealRepository.updateUser(newUserDetails.toUser())
        }

        updateTargetUser(userUiState.value.targetUserDetails.copy(id = id))
        when (userUiState.value.selectedUserDetails.id) {
            id -> updateSelectedUser(id)
        }

        return true
    }

    /**
     * Deletes the provided user from the meal repository.
     *
     * @param userDetails The user to delete.
     */
    fun deleteUser(userDetails: UserDetails) {
        viewModelScope.launch {
            mealRepository.deleteUser(userDetails.toUser())
        }
    }


    /**
     * Deletes the [MealInstance] with the provided id from the meal repository.
     *
     * @param instanceId The ID of the instance to delete.
     */
    fun deleteInstance(instanceId: Long) {
        viewModelScope.launch {
            mealRepository.deleteMealInstance(instanceId)
        }
    }

    companion object {
        private const val TIMEOUT_MILLIS = 5_000L
    }
}

data class MealUiState(
    val mealInstanceDetails: List<MealInstanceDetails> = listOf(),
    val selectedDay: LocalDate =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
) {
    val daysOfSelectedWeek: List<LocalDate> = getDaysFromSurroundingWeek(selectedDay)
}

data class UserUiState(
    val selectedUserDetails: UserDetails = UserDetails(), // The current user in use
    val allUsersDetails: List<UserDetails> = listOf(),
    val targetUserDetails: UserDetails = UserDetails(), // The user that is being edited
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