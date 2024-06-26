package com.jimmy.parentsmealplanner.ui.meal

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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
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
import com.jimmy.parentsmealplanner.ui.theme.AppTheme
import com.jimmy.parentsmealplanner.ui.theme.LocalDarkTheme
import com.jimmy.parentsmealplanner.ui.theme.ParentsMealPlannerTheme
import com.jimmy.parentsmealplanner.util.checkResult
import kotlinx.coroutines.CoroutineScope
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
 * @param mainViewModel Main ViewModel for the activity
 */
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
    val userUiState by viewModel.userUiState.collectAsStateWithLifecycle()

    val isLoading by viewModel.loading.collectAsState()

    LaunchedEffect(key1 = true) {
        viewModel.initializeData()
    }

    MealPlanningScreen(
        modifier = modifier,
        userUiState = userUiState,
        mealUiState = mealUiState,
        saveTargetUser = viewModel::saveTargetUser,
        updateTargetUser = { viewModel.updateTargetUser(it) },
        viewModelScope = viewModel.viewModelScope,
        onMealClick = navigateToMealDetail,
        deleteUser = { viewModel.deleteUser(it) },
        incrementSelectedDay = { viewModel.incrementSelectedDay(it) },
        updateSelectedDay = { viewModel.updateSelectedDay(it) },
        updateSelectedUser = { viewModel.updateSelectedUser(it) },
        deleteInstance = { viewModel.deleteInstance(it) },
        changeTheme = { mainViewModel.changeTheme(it) },
    )

    when {
        isLoading -> {
            IndeterminateCircularIndicator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanningScreen(
    modifier: Modifier = Modifier,
    userUiState: UserUiState,
    mealUiState: MealUiState,
    saveTargetUser: (suspend () -> Boolean) = { true },
    updateTargetUser: (userDetails: UserDetails) -> Unit = { _ -> },
    viewModelScope: CoroutineScope = rememberCoroutineScope(),
    onMealClick: (mealId: Long, date: LocalDate, occasion: Occasion, userId: Long, instanceId: Long) -> Unit = { _, _, _, _, _ -> },
    deleteUser: (UserDetails) -> Unit = { _ -> },
    incrementSelectedDay: (Int) -> Unit = { },
    updateSelectedDay: (newSelectedDay: LocalDate) -> Unit = { _ -> },
    updateSelectedUser: (newSelectedUser: Long) -> Unit = { _ -> },
    deleteInstance: (Long) -> Unit = { _ -> },
    changeTheme: (AppTheme) -> Unit = { _ -> },
) {
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val showUserDetailsDialog = rememberSaveable { mutableStateOf(false) }
    val showDeleteUserDialog = rememberSaveable { mutableStateOf(false) }
    val targetOriginalName = remember { mutableStateOf("") }
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = mealUiState.selectedDay.dayOfWeek.ordinal)
    val context = LocalContext.current

    when {
        showUserDetailsDialog.value -> {
            UserDialog(
                userDetails = userUiState.targetUserDetails,
                onDismissRequest = { showUserDetailsDialog.value = false },
                onNameChange = updateTargetUser,
                onConfirmation = {
                    viewModelScope.launch {
                        val result = saveTargetUser()
                        checkResult(
                            result = result,
                            context = context,
                            onSuccess = { showUserDetailsDialog.value = false },
                            errorMessage = "A user with that name already exists.",
                        )
                    }
                },
                titleText =
                    when (userUiState.targetUserDetails.id) {
                        0L -> stringResource(R.string.new_user)
                        else ->
                            "${stringResource(R.string.edit_user)} " +
                                targetOriginalName.value
                    },
            )
        }

        showDeleteUserDialog.value -> {
            DeleteUserDialog(
                onDismissRequest = { showDeleteUserDialog.value = false },
                onConfirmation = {
                    deleteUser(userUiState.targetUserDetails)
                    showDeleteUserDialog.value = false
                },
                userName = userUiState.targetUserDetails.name,
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
                onThemeToggle = { changeTheme(it) },
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.padding(paddingValues = paddingValues),
        ) {
            WeekBar(
                daysOfSelectedWeek = mealUiState.daysOfSelectedWeek,
                onSwipe = { daysToIncrement -> incrementSelectedDay(daysToIncrement) },
                onDayClick = { index: Int, day: LocalDate ->
                    scope.launch {
                        listState.animateScrollToItem(index)
                        updateSelectedDay(day)
                    }
                },
            )
            UserBar(
                userUiState = userUiState,
                onUserChange = { newSelectedUserId -> updateSelectedUser(newSelectedUserId) },
                onUserEditClick = { userDetails ->
                    updateTargetUser(userDetails)
                    targetOriginalName.value = userDetails.name
                    showUserDetailsDialog.value = true
                },
                onUserAddClick = {
                    updateTargetUser(UserDetails())
                    showUserDetailsDialog.value = true
                },
                onUserDeleteClick = { userDetails ->
                    updateTargetUser(userDetails)
                    showDeleteUserDialog.value = true
                },
            )
            MealPlanningBody(
                modifier = Modifier,
                mealUiState = mealUiState,
                userUiState = userUiState,
                onMealClick = onMealClick,
                onMealDeleteClick = { instanceId -> deleteInstance(instanceId) },
                daysOfWeekListState = listState,
            )
        }
    }
}

/**
 * Displays the days of the selected week in a horizontal list.
 *
 * @param modifier The Modifier to be applied to the WeekBar.
 * @param daysOfSelectedWeek The state of the date interface.
 * @param onSwipe The function that is called when the week bar is swiped left or right.
 * @param onDayClick The function that is called when a day is clicked.
 *
 */
@Composable
@OptIn(ExperimentalFoundationApi::class)
@Preview(apiLevel = 33)
fun WeekBar(
    modifier: Modifier = Modifier,
    daysOfSelectedWeek: List<LocalDate> =
        listOf(
            LocalDate(2019, 12, 30),
            LocalDate(2019, 12, 31),
            LocalDate(2020, 1, 1),
            LocalDate(2020, 1, 2),
            LocalDate(2020, 1, 3),
            LocalDate(2020, 1, 4),
            LocalDate(2020, 1, 5),
        ),
    onSwipe: (Int) -> Unit = { _ -> },
    onDayClick: (Int, LocalDate) -> Unit = { _, _ -> },
) {
    val density = LocalDensity.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp.dp
    val screenWidthPx = with(density) { screenWidthDp.toPx() }
    val currentDay = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date

    val weekSwipeState =
        remember {
            AnchoredDraggableState(
                initialValue = DragAnchors.Middle,
                positionalThreshold = { distance: Float -> distance * 0.5f },
                velocityThreshold = { with(density) { 100.dp.toPx() } },
                animationSpec = tween(),
            )
        }

    // Change the week bar to the previous or next week when swiped
    LaunchedEffect(weekSwipeState.currentValue) {
        snapshotFlow { weekSwipeState }.collect { state ->
            if (state.currentValue == DragAnchors.Start) {
                onSwipe(7)
                state.animateTo(DragAnchors.Middle)
            } else if (state.currentValue == DragAnchors.End) {
                onSwipe(-7)
                state.animateTo(DragAnchors.Middle)
            }
        }
    }

    val alpha: Float by animateFloatAsState(
        targetValue =
            if (weekSwipeState.targetValue != DragAnchors.Middle) {
                weekSwipeState.progress
            } else {
                0f
            },
        animationSpec =
            tween(
                easing = LinearEasing,
            ),
        label = "Week Bar Arrow Alpha Animation",
    )
    val scaleY: Float by animateFloatAsState(
        targetValue = weekSwipeState.progress,
        animationSpec =
            tween(
                easing = LinearEasing,
            ),
        label = "Week Bar Arrow Size Animation",
    )

    Box(
        modifier =
            modifier
                .fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth(),
            contentAlignment = Alignment.Center,
        ) { // Week Bar Arrow Animation
            if (weekSwipeState.targetValue <= DragAnchors.Middle && !weekSwipeState.offset.isNaN() &&
                weekSwipeState.offset < 0
            ) {
                Icon(
                    modifier =
                        Modifier
                            .graphicsLayer(alpha = alpha, scaleY = scaleY)
                            .align(Alignment.CenterEnd),
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Arrow",
                )
            }
            if (weekSwipeState.targetValue >= DragAnchors.Middle && !weekSwipeState.offset.isNaN() &&
                weekSwipeState.offset > 0
            ) {
                Icon(
                    modifier =
                        Modifier
                            .graphicsLayer(alpha = alpha, scaleY = scaleY)
                            .align(Alignment.CenterStart),
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Arrow",
                )
            }
        }

        // Row of days
        LazyRow(
            modifier =
                modifier
                    .fillMaxWidth()
                    .offset {
                        IntOffset(
                            x =
                                weekSwipeState
                                    .requireOffset()
                                    .roundToInt(),
                            y = 0,
                        )
                    }
                    .onSizeChanged { size ->
                        val dragEndPoint = size.width - screenWidthPx / 1.03f
                        weekSwipeState.updateAnchors(
                            DraggableAnchors {
                                DragAnchors.entries
                                    .forEach { anchor ->
                                        anchor at dragEndPoint * anchor.fraction
                                    }
                            },
                        )
                    },
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            itemsIndexed(daysOfSelectedWeek) { index, day ->
                Column(
                    modifier =
                        Modifier
                            .clickable { onDayClick(index, day) }
                            .let { modifier ->
                                if (day == currentDay) {
                                    modifier.background(
                                        shape = MaterialTheme.shapes.small,
                                        color = MaterialTheme.colorScheme.primary,
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
 * @param userUiState The state of the user interface.
 * @param onUserChange The function that is called when a user is selected from the dropdown menu.
 * @param onUserEditClick The function that is called when the edit icon of a user is clicked in the dropdown menu.
 * @param onUserAddClick The function that is called when the add user icon is clicked.
 * @param onUserDeleteClick The function that is called when the delete icon of a user is clicked in the dropdown menu.
 */
@Composable
@Preview
fun UserBar(
    userUiState: UserUiState = UserUiState(),
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
 * Displays a dropdown menu for selecting a user.
 *
 * @param modifier The Modifier to be applied to the UserDropdown.
 * @param values A list of UserDetails objects. Each UserDetails object represents a user.
 * @param onUserChange The function that is called when a user is selected from the dropdown menu.
 * @param onUserEditClick The function that is called when the edit icon of a user is clicked in the dropdown menu.
 * @param onUserAddClick The function that is called when the add user icon is clicked.
 * @param onUserDeleteClick The function that is called when the delete icon of a user is clicked in the dropdown menu.
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

    // Dropdown menu of users
    ExposedDropdownMenuBox(
        modifier =
            modifier.fillMaxWidth().semantics {
                stateDescription =
                    when (expanded) {
                        true -> "User Dropdown Expanded" else -> "User Dropdown Collapsed"
                    }
            },
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        },
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
                            contentDescription = "Button: Edit User ${userDetails.name}",
                        )
                    },
                    leadingIcon = {
                        if (values.size > 1) {
                            Icon(
                                modifier = Modifier.clickable { onUserDeleteClick(userDetails) },
                                imageVector = Icons.Default.Close,
                                contentDescription = "Button: Delete User ${userDetails.name}",
                            )
                        }
                    },
                )
            }
        }
    }
}

/**
 * Displays a Dialog that contains a UserForm used to add a new user or edit an existing user.
 *
 * @param onDismissRequest The function that is called when the dialog is dismissed.
 * @param onConfirmation The function that is called when the user confirms the action in the dialog.
 * @param userDetails A UserDetails object that represents the user.
 * @param onNameChange The function that is called when the name of the user is changed in the UserForm.
 * @param titleText The title of the dialog.
 */
@Composable
@Preview
fun UserDialog(
    onDismissRequest: () -> Unit = { },
    onConfirmation: () -> Unit = { },
    userDetails: UserDetails = UserDetails(0, ""),
    onNameChange: (userDetails: UserDetails) -> Unit = {},
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

// Displays a dialog that asks the user to confirm if they want to delete the user.
@Composable
@Preview
fun DeleteUserDialog(
    onDismissRequest: () -> Unit = { },
    onConfirmation: () -> Unit = { },
    userName: String = "",
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        DeleteUserConfirmation(
            onDismissRequest = onDismissRequest,
            onConfirmation = onConfirmation,
            userName = userName,
        )
    }
}

@Composable
fun DeleteUserConfirmation(
    onDismissRequest: () -> Unit = { },
    onConfirmation: () -> Unit = { },
    userName: String = "Default User",
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
            Text( // Title
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                text = "Delete $userName's meal plan?",
            )
            Row( // Buttons
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
                    onClick = onConfirmation,
                    modifier = Modifier.padding(8.dp),
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

/**
 * The UserForm is used to add a new user or edit an existing user.
 *
 * @param onDismissRequest The function that is called when the dismiss button is clicked.
 * @param onNameChange The function that is called when the name of the user is changed in the TextField.
 * @param titleText The title of the dialog.
 * @param userDetails A UserDetails object that represents the target user. This is used to pre-fill the TextField with the user's name.
 */
@Composable
fun UserForm(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
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
            Text( // Title
                modifier =
                    Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 12.dp),
                style = MaterialTheme.typography.titleLarge,
                text = titleText,
            )
            TextField( // User name
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
            Row( // Buttons
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
                    onClick = onConfirmation,
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
 * @param mealUiState The state of the meal interface.
 * @param onMealClick The function that is called when a meal is clicked.
 * @param userUiState The state of the user interface. This is used to get the ID of the selected user.
 * @param onMealDeleteClick The function that is called when the delete icon of a meal is clicked.
 * @param daysOfWeekListState The state of the list of days. This is used to scroll to the selected day.
 */
@Composable
fun MealPlanningBody(
    modifier: Modifier = Modifier,
    mealUiState: MealUiState = MealUiState(),
    onMealClick: (mealId: Long, date: LocalDate, occasion: Occasion, userId: Long, instanceId: Long)
    -> Unit = { _, _, _, _, _ -> },
    userUiState: UserUiState = UserUiState(),
    onMealDeleteClick: (Long) -> Unit = { _ -> },
    daysOfWeekListState: LazyListState = rememberLazyListState(),
) {
    // Display the meals for each day of the selected week
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = daysOfWeekListState,
    ) {
        items(mealUiState.daysOfSelectedWeek) { day ->
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
@Preview(
    apiLevel = 33,
    name = "BodyPreviewDark",
)
fun MealPlanningBodyPreviewDark() {
    ParentsMealPlannerTheme(darkTheme = true) {
        MealPlanningBody()
    }
}

@Composable
@Preview(
    apiLevel = 33,
    name = "BodyPreviewLight",
)
fun MealPlanningBodyPreviewLight() {
    ParentsMealPlannerTheme(darkTheme = false) {
        MealPlanningBody()
    }
}

/**
 * Displays the meals planned for a specific day.
 *
 * @param modifier The Modifier to be applied to the DayOfTheWeek.
 * @param date The date of the day. This is displayed in the Text.
 * @param mealInstances A list of MealInstanceDetails objects. Each MealInstanceDetails object represents a meal planned for the day.
 * @param onMealClick The function that is called when a meal is clicked.
 * @param userId The ID of the user. This is used to filter the meals.
 * @param onDeleteClick The function that is called when the delete icon of a meal is clicked.
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
 * Displays the meals planned for each occasion of a specific day.
 *
 * @param modifier The Modifier to be applied to the DailyMeals.
 * @param mealInstances A list of MealInstanceDetails objects. Each MealInstanceDetails object represents a meal planned for the day.
 * @param onMealClick The function that is called when a meal is clicked.
 * @param date The date of the day. This is used to filter the meals.
 * @param occasion The occasion of the meal. This is used to filter the meals.
 * @param userId The ID of the user. This is used to filter the meals.
 * @param onDeleteClick The function that is called when the delete icon of a meal is clicked.
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
 * Displays the meals for a specific occasion.
 *
 * @param modifier The Modifier to be applied to the OccasionMeals.
 * @param mealsForOccasion A list of MealInstanceDetails objects representing the meals for the occasion.
 * @param onEditClick The function that is called when a meal is clicked for editing.
 * @param date The date of the meals. This is used to filter the meals.
 * @param occasion The occasion of the meals. This is used to filter the meals.
 * @param userId The ID of the user. This is used to filter the meals.
 * @param onDeleteClick The function that is called when the delete icon of a meal is clicked.
 */
@Composable
fun OccasionMeals(
    modifier: Modifier = Modifier,
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
                Image(
                    painter = painterResource(id = occasion.icon),
                    contentDescription = null,
                    colorFilter =
                        ColorFilter.tint(
                            color = if (LocalDarkTheme.current) Color.White else Color.Black,
                        ),
                    modifier = Modifier.size(48.dp),
                )
                Column {
                    Text(text = occasion.name)
                    mealsForOccasion.forEach { mealInstanceDetails ->
                        MealHolder(
                            modifier = modifier.padding(1.dp),
                            mealInstance = mealInstanceDetails,
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
 * Displays a single meal instance with all related details and actions.
 *
 * @param modifier The Modifier to be applied to the MealHolder.
 * @param mealInstance The details of the meal instance to be displayed.
 * @param onEditClick The function that is called when the meal is clicked for editing.
 * @param onDeleteClick The function that is called when the delete icon of a meal is clicked.
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
        modifier =
            modifier
                .background(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.background,
                )
                .semantics {
                    stateDescription =
                        when (expanded) {
                            true -> "Expanded" else -> "Collapsed"
                        }
                },
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
        Text(text = mealInstance.mealDetails.rating.ratingEmoji.emojiString)
        if (mealInstance.mealDetails.dishes.isNotEmpty()) {
            Image(
                modifier =
                    Modifier
                        .padding(start = 2.dp, top = 2.dp)
                        .size(30.dp)
                        .clickable { expanded = !expanded }
                        .align(Alignment.CenterVertically),
                imageVector = imageVector,
                colorFilter =
                    ColorFilter.tint(
                        color = if (LocalDarkTheme.current) Color.White else Color.Black,
                    ),
                contentDescription = "Button: Expand/Collapse meal list",
            )
        }
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
                Row {
                    DishText(dishName = dish.name)
                    Text(text = dish.rating.ratingEmoji.emojiString)
                }
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
    Text(modifier = modifier, text = mealName, style = MaterialTheme.typography.titleMedium)
}

@Composable
@Preview(apiLevel = 33)
fun DishText(
    modifier: Modifier = Modifier,
    dishName: String = stringResource(R.string.dish_name_placeholder),
) {
    Text(
        modifier = modifier.padding(top = 4.dp, bottom = 4.dp, start = 32.dp),
        text = "- $dishName",
        style = MaterialTheme.typography.labelLarge,
    )
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
