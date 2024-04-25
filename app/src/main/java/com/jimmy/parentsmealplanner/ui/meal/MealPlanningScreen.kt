package com.jimmy.parentsmealplanner.ui.meal

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.jimmy.parentsmealplanner.R
import com.jimmy.parentsmealplanner.ui.nav.NavigationDestination
import com.jimmy.parentsmealplanner.ui.shared.DishDetails
import com.jimmy.parentsmealplanner.ui.shared.DragAnchors
import com.jimmy.parentsmealplanner.ui.shared.IndeterminateCircularIndicator
import com.jimmy.parentsmealplanner.ui.shared.MainViewModel
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
import com.jimmy.parentsmealplanner.ui.shared.MealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.Rating
import com.jimmy.parentsmealplanner.ui.shared.TopBar
import com.jimmy.parentsmealplanner.ui.shared.UserDetails
import com.jimmy.parentsmealplanner.ui.theme.ParentsMealPlannerTheme
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.math.roundToInt

/**
 * Navigation destination for meal planning.
 */
object MealPlanningDest : NavigationDestination {
    override val route = "meal_planning"
    override val titleRes = R.string.app_name
}

/**
 * Main composable function for the meal planner screen.
 *
 * @param modifier Modifier for styling.
 * @param navigateToMealDetail Function to navigate to meal detail screen.
 * @param viewModel ViewModel for the meal planner screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanner(
    modifier: Modifier = Modifier,
    navigateToMealDetail: (
        mealId: Long,
        date: LocalDate,
        occasion: Occasion,
        userId: Long,
        instanceId: Long,
    ) -> Unit = { _, _, _, _, _ -> },
    viewModel: MealPlanningViewModel = hiltViewModel<MealPlanningViewModel>(),
    mainViewModel: MainViewModel = hiltViewModel<MainViewModel>(),
) {
    val mealUiState by viewModel.mealUiState.collectAsStateWithLifecycle()
    val dateUiState by viewModel.dateUiState.collectAsStateWithLifecycle()
    val userUiState by viewModel.userUiState.collectAsStateWithLifecycle()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val showUserDetailsDialog = rememberSaveable { mutableStateOf(false) }
    val isLoading by viewModel.loading.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    LaunchedEffect(key1 = true) {
        viewModel.initializeData()
    }

    when {
        showUserDetailsDialog.value -> {
            UserDialog(
                userDetails = userUiState.targetUserDetails,
                onDismissRequest = { showUserDetailsDialog.value = false },
                onNameChange = viewModel::updateTargetUser,
                onConfirmation = {
                    showUserDetailsDialog.value = false
                    viewModel.saveTargetUser()
                },
                titleText =
                    when (userUiState.targetUserDetails.id) {
                        0L -> stringResource(R.string.new_user)
                        else -> stringResource(R.string.edit_user)
                    },
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = stringResource(id = R.string.app_name),
                canNavigateBack = false,
                scrollBehavior = scrollBehavior,
                onThemeToggle = { mainViewModel.changeTheme(it) },
            )
        },
    ) {
        Column(
            modifier = Modifier.padding(paddingValues = it),
        ) {
            WeekBar(
                dateUiState = dateUiState,
                onSwipe = viewModel::incrementSelectedDay,
                onDayClick = { index: Int, day: LocalDate -> scope.launch {
                    listState.animateScrollToItem(index)
                    viewModel.updateSelectedDay(day)
                } },
            )
            UserBar(
                userUiState = userUiState,
                onUserChange = viewModel::updateSelectedUser,
                onUserEditClick = { userDetails ->
                    viewModel.editUser(userDetails)
                    showUserDetailsDialog.value = true
                },
                onUserAddClick = {
                    viewModel.addUser()
                    showUserDetailsDialog.value = true
                },
                onUserDeleteClick = viewModel::deleteUser,
            )
            MealPlanningBody(
                modifier = Modifier,
                mealUiState = mealUiState,
                userUiState = userUiState,
                dateUiState = dateUiState,
                onMealClick = navigateToMealDetail,
                onMealDeleteClick = viewModel::deleteInstance,
                listState = listState,
            )
        }
    }

    when {
        isLoading -> {
            IndeterminateCircularIndicator()
        }
    }
}

/**
 * The WeekBar displays the days of the selected week in a horizontal list.
 * Each day is represented by a Column that contains two Texts. The first Text displays the day of the week and the second Text displays the day of the month.
 * The Column is clickable and calls the onDayClick function when clicked.
 *
 * @param modifier The Modifier to be applied to the WeekBar.
 * @param dateUiState The state of the date interface. This is used to get the days of the selected week.
 * @param onSwipe A function that is called when a day is clicked.
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
@Preview(apiLevel = 33)
fun WeekBar(
    modifier: Modifier = Modifier,
    dateUiState: DateUiState = DateUiState(),
    onSwipe: (Int) -> Unit = { _ -> },
    onDayClick: (Int, LocalDate) -> Unit = {_, _, -> },
) {
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val currentDay = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val weekSwipeState = remember {
        AnchoredDraggableState(
            initialValue = DragAnchors.Middle,
            positionalThreshold = { distance: Float -> distance * 0.5f },
            velocityThreshold = { with(density) { 100.dp.toPx() } },
            animationSpec = tween(),
        )
    }

    LaunchedEffect(weekSwipeState.currentValue) {
        snapshotFlow { weekSwipeState }.collect { state ->
            if (state.currentValue == DragAnchors.Start) {
                onSwipe(7)
                state.animateTo(DragAnchors.Middle)
            }
            else if (state.currentValue == DragAnchors.End) {
                onSwipe(-7)
                state.animateTo(DragAnchors.Middle)
            }
        }
    }

    val alpha: Float by animateFloatAsState(
        targetValue = if (weekSwipeState.targetValue != DragAnchors.Middle)
            weekSwipeState.progress else 0f,
        animationSpec = tween(
            easing = LinearEasing
        ), label = "Week Bar Arrow Alpha Animation"
    )
    val scaleY: Float by animateFloatAsState(
        targetValue = weekSwipeState.progress,
        animationSpec = tween(
            easing = LinearEasing
        ), label = "Week Bar Arrow Size Animation"
    )


    Box(
        modifier = modifier
            .fillMaxWidth(),
        contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) {
            if (weekSwipeState.targetValue <= DragAnchors.Middle && !weekSwipeState.offset.isNaN()
                && weekSwipeState.offset < 0) {
                Icon(
                    modifier = Modifier
                        .graphicsLayer(alpha = alpha, scaleY = scaleY)
                        .align(Alignment.CenterEnd),
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Arrow",
                )
            }
            if (weekSwipeState.targetValue >= DragAnchors.Middle && !weekSwipeState.offset.isNaN()
                && weekSwipeState.offset > 0) {
                Icon(
                    modifier = Modifier
                        .graphicsLayer(alpha = alpha, scaleY = scaleY)
                        .align(Alignment.CenterStart),
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Arrow",
                )
            }
        }

        LazyRow(
            modifier = modifier
                .fillMaxWidth()
                .offset {
                    IntOffset(x = weekSwipeState.requireOffset().roundToInt(), y = 0)
                }
                .onSizeChanged { size ->
                    val dragEndPoint = size.width - screenWidthPx / 1.03f
                    weekSwipeState.updateAnchors(
                        DraggableAnchors {
                            DragAnchors.entries
                                .forEach { anchor ->
                                    anchor at dragEndPoint * anchor.fraction
                                }
                        }
                    )
                },
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            itemsIndexed(dateUiState.daysOfSelectedWeek) { index, day ->
                Column(
                    modifier = Modifier
                        .clickable { onDayClick(index, day) }
                        .let { modifier ->
                            if (day == currentDay) {
                                modifier.background(
                                    shape = MaterialTheme.shapes.small,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            } else {
                                modifier
                            }
                        }
                        .anchoredDraggable(
                            state = weekSwipeState,
                            orientation = Orientation.Horizontal,
                        ),
                ) {
                    Text(
                        text = day.dayOfWeek.toString().substring(0..2),
                        modifier = Modifier.padding(horizontal = 12.dp),
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = day.dayOfMonth.toString(),
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        style = MaterialTheme.typography.titleLarge,
                    )
                }
            }
        }
    }
}

/**
 * The UserBar consists of a dropdown menu for selecting a user and an icon for adding a new user.
 *
 * @param userUiState The state of the user interface. This is used to get the list of all users and the currently selected user.
 * @param userValues A list of UserDetails objects. Each UserDetails object represents a user.
 * @param onUserChange A function that is called when a user is selected from the dropdown menu.
 * @param onUserEditClick A function that is called when the edit icon of a user is clicked in the dropdown menu.
 * @param onUserAddClick A function that is called when the add user icon is clicked.
 * @param onUserDeleteClick A function that is called when the delete icon of a user is clicked in the dropdown menu.
 */
@Composable
@Preview
fun UserBar(
    userUiState: UserUiState = UserUiState(),
    userValues: List<UserDetails> =
        listOf(
            UserDetails(1, "user1"),
            UserDetails(2, "user2"),
        ),
    onUserChange: (userId: Long) -> Unit = {},
    onUserEditClick: (userDetails: UserDetails) -> Unit = {},
    onUserAddClick: () -> Unit = {},
    onUserDeleteClick: (userDetails: UserDetails) -> Unit = {},
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        UserDropdown(
            modifier =
            Modifier
                .fillMaxWidth(0.9f)
                .padding(0.dp),
            values = userUiState.allUsersDetails.sortedBy { it.name },
            onUserChange = onUserChange,
            onUserDeleteClick = onUserDeleteClick,
            onUserAddClick = onUserAddClick,
            onUserEditClick = onUserEditClick,
            selectedValue = userUiState.selectedUserDetails.name,
        )
        Icon(
            modifier =
            Modifier
                .size(width = 200.dp, height = 40.dp)
                .align(Alignment.CenterVertically)
                .clickable { onUserAddClick() },
            imageVector = Icons.Default.Add,
            contentDescription = "Add User Button",
        )
    }
}

/**
 * The UserDropdown consists of a TextField and an ExposedDropdownMenu.
 * The TextField displays the currently selected user. The ExposedDropdownMenu displays a list of all users.
 * Each user in the list is represented by a DropdownMenuItem. Each DropdownMenuItem has a leading icon for deleting the user and a trailing icon for editing the user.
 *
 * @param modifier The Modifier to be applied to the UserDropdown.
 * @param values A list of UserDetails objects. Each UserDetails object represents a user.
 * @param onUserChange A function that is called when a user is selected from the dropdown menu.
 * @param onUserEditClick A function that is called when the edit icon of a user is clicked in the dropdown menu.
 * @param onUserAddClick A function that is called when the add user icon is clicked.
 * @param onUserDeleteClick A function that is called when the delete icon of a user is clicked in the dropdown menu.
 * @param selectedValue The name of the currently selected user. This is displayed in the TextField.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
fun UserDropdown(
    modifier: Modifier = Modifier,
    values: List<UserDetails> =
        listOf(
            UserDetails(1, "user1"),
            UserDetails(2, "user2"),
        ),
    onUserChange: (userId: Long) -> Unit = {},
    onUserEditClick: (userDetails: UserDetails) -> Unit = {},
    onUserAddClick: () -> Unit = {},
    onUserDeleteClick: (userDetails: UserDetails) -> Unit = {},
    selectedValue: String = "user1",
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        },
        modifier = modifier.fillMaxWidth(),
    ) {
        TextField(
            value = selectedValue,
            onValueChange = { },
            readOnly = true,
            placeholder = {
                Text(text = "User")
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier =
            Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            modifier = Modifier.fillMaxWidth(),
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            values.forEach { userDetails ->
                DropdownMenuItem(
                    text = { Text(userDetails.name) },
                    onClick = {
                        onUserChange(userDetails.id)
                        expanded = false
                    },
                    trailingIcon = {
                        Icon(
                            modifier = Modifier.clickable { onUserEditClick(userDetails) },
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit User ${userDetails.name} Button",
                        )
                    },
                    leadingIcon = {
                        if (values.size > 1) {
                            Icon(
                                modifier = Modifier.clickable { onUserDeleteClick(userDetails) },
                                imageVector = Icons.Default.Close,
                                contentDescription = "Delete User ${userDetails.name} Button",
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * The UserDialog is a Dialog that contains a UserForm. The UserDialog is used to add a new user or edit an existing user.
 *
 * @param onDismissRequest A function that is called when the dialog is dismissed.
 * @param onConfirmation A function that is called when the user confirms the action in the dialog.
 * @param userDetails A UserDetails object that represents the user. This is used to pre-fill the UserForm with the user's details.
 * @param onNameChange A function that is called when the name of the user is changed in the UserForm.
 * @param titleText The title of the dialog. This is displayed at the top of the dialog.
 */
@Composable
@Preview
fun UserDialog(
    onDismissRequest: () -> Unit = { },
    onConfirmation: (UserDetails) -> Unit = { },
    userDetails: UserDetails = UserDetails(0, ""),
    onNameChange: (UserDetails) -> Unit = {},
    titleText: String = "New User",
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        UserForm(
            onDismissRequest = onDismissRequest,
            onConfirmation = onConfirmation,
            onNameChange = onNameChange,
            titleText = titleText,
            userDetails = userDetails,
        )
    }
}

/**
 * The UserForm is used to add a new user or edit an existing user.
 * The UserForm consists of a TextField for entering the user's name and two buttons for confirming or dismissing the action.
 *
 * @param onDismissRequest A function that is called when the dismiss button is clicked.
 * @param onConfirmation A function that is called when the confirm button is clicked.
 * @param onNameChange A function that is called when the name of the user is changed in the TextField.
 * @param titleText The title of the dialog. This is displayed at the top of the dialog.
 * @param userDetails A UserDetails object that represents the target user. This is used to pre-fill the TextField with the user's name.
 */
@Composable
fun UserForm(
    onDismissRequest: () -> Unit,
    onConfirmation: (UserDetails) -> Unit,
    onNameChange: (UserDetails) -> Unit = {},
    titleText: String = "New User",
    userDetails: UserDetails,
) {
    Card(
        modifier =
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
        ) {
            Text(
                modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                text = titleText,
            )
            TextField(
                value = userDetails.name,
                onValueChange = {
                    onNameChange(userDetails.copy(name = it))
                },
                placeholder = {
                    when {
                        userDetails.name.isEmpty() -> {
                            Text(text = "Enter user name")
                        }
                    }
                },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                TextButton(
                    onClick = onDismissRequest,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Cancel")
                }
                TextButton(
                    onClick = { onConfirmation(userDetails) },
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

/**
 * The main body of the Meal Planning screen.
 * It displays a list of days for the selected week. For each day, it displays the meals planned for the selected user.
 *
 * @param modifier The Modifier to be applied to the MealPlanningBody.
 * @param mealUiState The state of the meal interface. This is used to get the meal instances for the selected user and date.
 * @param dateUiState The state of the date interface. This is used to get the days of the selected week.
 * @param onMealClick A function that is called when a meal is clicked. The ID of the meal, the date, the occasion, the ID of the user, and the ID of the meal instance are passed as parameters to this function.
 * @param userUiState The state of the user interface. This is used to get the ID of the selected user.
 * @param onMealDeleteClick A function that is called when the delete icon of a meal is clicked.
 */
@Composable

fun MealPlanningBody(
    modifier: Modifier = Modifier,
    mealUiState: MealUiState = MealUiState(),
    dateUiState: DateUiState = DateUiState(),
    onMealClick: (mealId: Long, date: LocalDate, occasion: Occasion, userId: Long, instanceId: Long)
        -> Unit = { _, _, _, _, _ -> },
    userUiState: UserUiState = UserUiState(),
    onMealDeleteClick: (Long) -> Unit = { _ -> },
    listState: LazyListState = LazyListState(),
) {
    LaunchedEffect(key1 = true) {
        listState.animateScrollToItem(dateUiState.selectedDay.dayOfWeek.ordinal)
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
    ) {
        items(dateUiState.daysOfSelectedWeek) {day ->
            DayOfTheWeek(
                date = day,
                mealInstances =
                mealUiState.getMealInstancesForDateAndUser(
                    date = day,
                    userId = userUiState.selectedUserDetails.id,
                ),
                onMealClick = onMealClick,
                userId = userUiState.selectedUserDetails.id,
                onDeleteClick = onMealDeleteClick,
            )
        }
    }
}

@Composable
@Preview(apiLevel = 33,
    name = "BodyPreviewDark"
)
fun MealPlanningBodyPreviewDark() {
    ParentsMealPlannerTheme(darkTheme = true) {
        MealPlanningBody()
    }
}

@Composable
@Preview(apiLevel = 33,
    name = "BodyPreviewLight"
)
fun MealPlanningBodyPreviewLight() {
    ParentsMealPlannerTheme(darkTheme = false) {
        MealPlanningBody()
    }
}


/**
 * The DayOfTheWeek displays the meals planned for a specific day.
 * It consists of a Text that displays the date and a DailyMeals that displays the meals.
 *
 * @param modifier The Modifier to be applied to the DayOfTheWeek.
 * @param date The date of the day. This is displayed in the Text.
 * @param mealInstances A list of MealInstanceDetails objects. Each MealInstanceDetails object represents a meal planned for the day.
 * @param onMealClick A function that is called when a meal is clicked. The ID of the meal, the date, the occasion, the ID of the user, and the ID of the meal instance are passed as parameters to this function.
 * @param userId The ID of the user. This is used to filter the meals.
 * @param onDeleteClick A function that is called when the delete icon of a meal is clicked.
 */
@Composable
@Preview(apiLevel = 33)
fun DayOfTheWeek(
    modifier: Modifier = Modifier,
    date: LocalDate = LocalDate(1, 1, 1),
    mealInstances: List<MealInstanceDetails> =
        listOf(
            MealInstanceDetails(mealDetails = MealDetails(name = "Breakfast meal")),
            MealInstanceDetails(mealDetails = MealDetails(name = "Breakfast meal2")),
            MealInstanceDetails(
                occasion = Occasion.DINNER,
                mealDetails = MealDetails(name = "Dinner meal"),
            ),
        ),
    onMealClick: (mealId: Long, date: LocalDate, occasion: Occasion, userId: Long, instanceId: Long)
    -> Unit = { _, _, _, _, _ -> },
    userId: Long = 1,
    onDeleteClick: (Long) -> Unit = { _ -> },
) {
    Column(modifier = modifier) {
        Text(
            text =
                String.format(
                    stringResource(R.string.planner_date_entry),
                    date.dayOfWeek.toString(),
                    date.month.toString(),
                    date.dayOfMonth.toString(),
                ),
        )
        DailyMeals(
            mealInstances = mealInstances,
            onMealClick = onMealClick,
            date = date,
            occasion = Occasion.BREAKFAST,
            userId = userId,
            onDeleteClick = onDeleteClick,
        )
    }
}

/**
 * DailyMeals displays the meals planned for each occasion of a specific day.
 * It consists of a Column that contains OccasionMeals for each occasion. The OccasionMeals displays the meals planned for a specific occasion.
 *
 * @param modifier The Modifier to be applied to the DailyMeals.
 * @param mealInstances A list of MealInstanceDetails objects. Each MealInstanceDetails object represents a meal planned for the day.
 * @param onMealClick A function that is called when a meal is clicked. The ID of the meal, the date, the occasion, the ID of the user, and the ID of the meal instance are passed as parameters to this function.
 * @param date The date of the day. This is used to filter the meals.
 * @param occasion The occasion of the meal. This is used to filter the meals.
 * @param userId The ID of the user. This is used to filter the meals.
 * @param onDeleteClick A function that is called when the delete icon of a meal is clicked.
 */
@Composable
@Preview(apiLevel = 33)
fun DailyMeals(
    modifier: Modifier = Modifier,
    mealInstances: List<MealInstanceDetails> =
        listOf(
            MealInstanceDetails(mealDetails = MealDetails(name = "Breakfast meal")),
            MealInstanceDetails(mealDetails = MealDetails(name = "Breakfast meal2")),
            MealInstanceDetails(
                occasion = Occasion.DINNER,
                mealDetails = MealDetails(name = "Dinner meal"),
            ),
        ),
    onMealClick: (
        mealId: Long,
        date: LocalDate,
        occasion: Occasion,
        userId: Long,
        instanceId: Long,
    ) -> Unit = { _, _, _, _, _ -> },
    date: LocalDate = LocalDate(1, 1, 1),
    occasion: Occasion = Occasion.BREAKFAST,
    userId: Long = 1,
    onDeleteClick: (Long) -> Unit = { _ -> },
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Occasion.entries.forEach { occasion ->
            val mealsForOccasion = mealInstances.filter { it.occasion == occasion }
            OccasionMeals(
                occasion = occasion,
                mealsForOccasion = mealsForOccasion,
                onEditClick = onMealClick,
                date = date,
                userId = userId,
                onDeleteClick = onDeleteClick,
            )
        }
    }
}

@Composable
@Preview
fun OccasionMealsPreview() {
    OccasionMeals(
        mealsForOccasion =
            listOf(
                MealInstanceDetails(
                    mealInstanceId = 1,
                    mealDetails =
                        MealDetails(
                            mealId = 1,
                            name = "Breakfast meal",
                            dishes =
                                listOf(
                                    DishDetails(name = "Dish 1"),
                                    DishDetails(name = "Dish 2"),
                                    DishDetails(name = "Dish 3"),
                                ),
                        ),
                ),
                MealInstanceDetails(
                    mealInstanceId = 2,
                    mealDetails =
                        MealDetails(
                            mealId = 2,
                            name = "Breakfast meal 2",
                            dishes =
                                listOf(
                                    DishDetails(name = "Dish 1"),
                                    DishDetails(name = "Dish 2"),
                                    DishDetails(name = "Dish 3"),
                                ),
                        ),
                ),
                MealInstanceDetails(
                    mealInstanceId = 3,
                    mealDetails =
                        MealDetails(
                            mealId = 3,
                            name = "Breakfast meal 3",
                            dishes =
                                listOf(
                                    DishDetails(name = "Dish 1"),
                                    DishDetails(name = "Dish 2"),
                                    DishDetails(name = "Dish 3"),
                                ),
                        ),
                ),
            ),
    )
}

/**
 * This composable function represents the meals for a specific occasion.
 *
 * @param modifier The Modifier to be applied to the OccasionMeals.
 * @param icon The Drawable resource ID for the icon to be displayed.
 * @param mealsForOccasion A list of MealInstanceDetails objects representing the meals for the occasion.
 * @param onEditClick A function that is called when a meal is clicked for editing. It takes the mealId, date, occasion, userId, and instanceId as parameters.
 * @param date The date of the meals. This is used to filter the meals.
 * @param occasion The occasion of the meals. This is used to filter the meals.
 * @param userId The ID of the user. This is used to filter the meals.
 * @param onDeleteClick A function that is called when the delete icon of a meal is clicked.
 */
@Composable
fun OccasionMeals(
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int = R.drawable.food_icon,
    mealsForOccasion: List<MealInstanceDetails>,
    onEditClick: (
        mealId: Long,
        date: LocalDate,
        occasion: Occasion,
        userId: Long,
        instanceId: Long,
    ) -> Unit = { _, _, _, _, _ -> },
    date: LocalDate = LocalDate(1, 1, 1),
    occasion: Occasion = Occasion.BREAKFAST,
    userId: Long = 1,
    onDeleteClick: (Long) -> Unit = { _ -> },
) {
    OutlinedCard(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = icon), contentDescription = null)
                Column {
                    Text(text = occasion.name)
                    mealsForOccasion.forEach { mealInstanceDetails ->
                        MealHolder(
                            mealInstance = mealInstanceDetails,
                            modifier = modifier.padding(1.dp),
                            onEditClick = onEditClick,
                            onDeleteClick = onDeleteClick,
                        )
                    }
                    AddMealButton(
                        onClick = {
                            onEditClick(0, date, occasion, userId, 0)
                        },
                    )
                }
            }
        }
    }
}

/**
 * This composable function represents a holder for a meal.
 *
 * @param modifier The Modifier to be applied to the MealHolder.
 * @param mealInstance The details of the meal instance to be displayed.
 * @param onEditClick A function that is called when the meal is clicked for editing. It takes the mealId, date, occasion, userId, and instanceId as parameters.
 * @param onDeleteClick A function that is called when the delete icon of a meal is clicked.
 */
@Composable
@Preview(apiLevel = 33)
fun MealHolder(
    modifier: Modifier = Modifier,
    mealInstance: MealInstanceDetails =
        MealInstanceDetails(
            mealInstanceId = 0,
            mealDetails =
                MealDetails(
                    name = stringResource(id = R.string.meal_contents_placeholder),
                    rating = Rating.LIKEIT,
                ),
            date = LocalDate(1, 1, 1),
            occasion = Occasion.BREAKFAST,
        ),
    onEditClick: (
        mealId: Long,
        date: LocalDate,
        occasion: Occasion,
        userId: Long,
        instanceId: Long,
    ) -> Unit = { _, _, _, _, _ -> },
    onDeleteClick: (instanceId: Long) -> Unit = { _ -> },
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val imageVector: ImageVector = (
        when (expanded) {
            true -> ImageVector.vectorResource(id = R.drawable.baseline_arrow_drop_up_24)
            false -> Icons.Default.ArrowDropDown
        }
    )
    Row(
        modifier = Modifier.background(Color.LightGray),
    ) {
        Icon(
            modifier =
            Modifier
                .clickable {
                    onDeleteClick(
                        mealInstance.mealInstanceId,
                    )
                }
                .size(18.dp)
                .align(Alignment.CenterVertically),
            imageVector = Icons.Default.Close,
            contentDescription = "Edit meal",
        )
        Column(modifier = Modifier.align(Alignment.CenterVertically)) {
            MealText(
                modifier =
                Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable {
                        onEditClick(
                            mealInstance.mealDetails.mealId,
                            mealInstance.date,
                            mealInstance.occasion,
                            mealInstance.userId,
                            mealInstance.mealInstanceId,
                        )
                    },
                mealName = mealInstance.mealDetails.name,
            )
        }
        Image(
            modifier =
            Modifier
                .padding(start = 4.dp, top = 2.dp)
                .size(18.dp)
                .clickable { expanded = !expanded }
                .align(Alignment.CenterVertically),
            imageVector = imageVector,
            contentDescription = "Button: Expand/Collapse meal list",
        )
        Column(
            modifier =
            Modifier
                .padding(4.dp)
                .fillMaxWidth()
                .align(Alignment.Top),
        ) {
            Icon(
                modifier =
                Modifier
                    .clickable {
                        onEditClick(
                            mealInstance.mealDetails.mealId,
                            mealInstance.date,
                            mealInstance.occasion,
                            mealInstance.userId,
                            mealInstance.mealInstanceId,
                        )
                    }
                    .align(Alignment.End),
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit meal",
            )
        }
    }
    when {
        expanded -> {
            mealInstance.mealDetails.dishes.forEach { dish ->
                DishText(dishName = dish.name)
            }
        }
    }
}

@Composable
@Preview(apiLevel = 33)
fun MealText(
    modifier: Modifier = Modifier,
    mealName: String = stringResource(R.string.meal_contents_placeholder),
) {
    Text(modifier = modifier, text = mealName)
}

@Composable
@Preview(apiLevel = 33)
fun DishText(
    modifier: Modifier = Modifier,
    dishName: String = stringResource(R.string.dish_name_placeholder),
) {
    Text(text = dishName)
}

@Composable
@Preview(apiLevel = 33)
fun AddMealButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { },
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
    ) {
        Text(text = stringResource(id = R.string.add_meal_button_text))
    }
}