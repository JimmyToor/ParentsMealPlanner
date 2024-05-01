package com.jimmy.parentsmealplanner.ui.meal

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.jimmy.parentsmealplanner.R
import com.jimmy.parentsmealplanner.ui.nav.NavigationDestination
import com.jimmy.parentsmealplanner.ui.shared.DishDetails
import com.jimmy.parentsmealplanner.ui.shared.MainViewModel
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
import com.jimmy.parentsmealplanner.ui.shared.MealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.Rating
import com.jimmy.parentsmealplanner.ui.shared.RatingEmoji
import com.jimmy.parentsmealplanner.ui.shared.TopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


object MealDetailDest : NavigationDestination {
    override val route = "meal_detail"
    override val titleRes = R.string.meal_detail
    const val MEAL_ID_ARG = "meal_id"
    const val DATE_ARG = "date"
    const val OCCASION_ARG = "occasion"
    const val USER_ID_ARG = "user_id"
    const val INSTANCE_ID_ARG = "instance_id"
    val routeWithArgs = "$route/{$DATE_ARG}/{$OCCASION_ARG}/{$MEAL_ID_ARG}/{$USER_ID_ARG}/" +
        "{$INSTANCE_ID_ARG}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealDetail(
    modifier: Modifier = Modifier,
    navigateBack: () -> Boolean = { true },
    onNavigateUp: () -> Unit = {},
    canNavigateBack: Boolean = true,
    viewModel: MealDetailViewModel = hiltViewModel(),
    mainViewModel: MainViewModel = hiltViewModel<MainViewModel>(),
) {
    val mealDetailUiState = viewModel.mealDetailUiState
    val mealSearchResults by viewModel.filteredMealSearchResults.collectAsStateWithLifecycle()
    val dishSearchResults by viewModel.filteredDishSearchResults.collectAsStateWithLifecycle()
    val showRenameMealDialog = rememberSaveable { mutableStateOf(false) }
    val showRenameDishDialog = rememberSaveable { mutableStateOf(false) }
    val showUnsavedChangesDialog = rememberSaveable { mutableStateOf(false) }
    val targetDishIndex = rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current

    // Show the dialog when the state variable is true
    if (showUnsavedChangesDialog.value) {
        AlertDialog(
            onDismissRequest = { showUnsavedChangesDialog.value = false },
            title = { Text("Discard Changes?") },
            text = { Text("Any unsaved changes will be lost.") },
            confirmButton = {
                TextButton(onClick = {
                    showUnsavedChangesDialog.value = false
                    if (!canNavigateBack || !navigateBack()) onNavigateUp()
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUnsavedChangesDialog.value = false }) {
                    Text("No")
                }
            }
        )
    }

    when {
        showRenameMealDialog.value -> {
            RenameMealDialog(
                onDismissRequest = { showRenameMealDialog.value = false },
                onConfirmation = {
                    viewModel.viewModelScope.launch {
                        val result = viewModel.updateMealName(
                            newName = it,
                        )
                        checkResult(
                            result,
                            context,
                            showRenameMealDialog,
                            "A meal with that name already exists."
                        )
                    }
                },
                name = mealDetailUiState.mealInstanceDetails.mealDetails.name,
            )
        }
    }

    when {
        showRenameDishDialog.value -> {
            RenameDishDialog(
                onDismissRequest = { showRenameDishDialog.value = false },
                onConfirmation = {
                    viewModel.viewModelScope.launch {
                        val result = viewModel.updateDishName(
                            index = targetDishIndex.intValue,
                            newName = it,
                        )
                        checkResult(
                            result,
                            context,
                            showRenameDishDialog,
                            "A dish with that name already exists."
                        )
                    }
                },
                name = mealDetailUiState.mealInstanceDetails.mealDetails
                    .dishes[targetDishIndex.intValue].name,
            )
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = stringResource(
                    id = R.string.meal_detail_header,
                    mealDetailUiState.mealInstanceDetails.occasion,
                    mealDetailUiState.mealInstanceDetails.date
                ),
                canNavigateBack = canNavigateBack,
                navigateUp = {
                    showUnsavedChangesDialog.value = true
                },
                onThemeToggle = { mainViewModel.changeTheme(it) },
            )
        },
        floatingActionButton = {
            SaveButton(
                enabled = mealDetailUiState.isEntryValid,
                onSaveClick = {
                    viewModel.viewModelScope.launch {
                        withContext(Dispatchers.IO) {
                            viewModel.saveMeal()
                        }
                    }
                    if (!canNavigateBack || !navigateBack()) onNavigateUp()
                })
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues = paddingValues)) {
            MealDetailBody(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                mealDetailUiState = viewModel.mealDetailUiState,
                onDishAdded = viewModel::addDish,
                onDishesChanged = { viewModel.updateUiState(mealDetails = it) },
                onMealInstanceDetailsChange = { viewModel.updateUiState(mealInstanceDetails = it) },
                onDeleteDishClick = viewModel::deleteDish,
                onRestoreDishClick = viewModel::unMarkDishForDeletion,
                onMealNameChanged = viewModel::changeMeal,
                onDishValueChanged = viewModel::changeDish,
                mealSearchResults = mealSearchResults,
                dishSearchResults = dishSearchResults,
                onFindExistingMeal = viewModel::findExistingMeal,
                onFindExistingDish = viewModel::findExistingDish,
                onMealSearchTermChanged = viewModel::onMealSearchTermChange,
                onDishSearchTermChanged = viewModel::onDishSearchTermChange,
                onMealEditClick = { showRenameMealDialog.value = true },
                onDishEditClick = {
                    targetDishIndex.intValue = it
                    showRenameDishDialog.value = true
                },
                onUpdateImage = viewModel::updateImage,
                isDuplicateCheck = viewModel::isDuplicate,
            )
        }
    }
}

@Composable
@Preview
fun FormatCheckBox(
    modifier: Modifier = Modifier,
    value: Boolean = false,
    onCheck: (Boolean) -> Unit = {},
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = "Love/Like/Learn"
        )
        Checkbox(
            modifier = Modifier.padding(0.dp),
            checked = value,
            onCheckedChange = onCheck
        )
    }
}

private fun checkResult(
    result: Boolean,
    context: Context,
    showRenameDishDialog: MutableState<Boolean>,
    errorMessage: String,
) {
    if (!result) {
        Toast.makeText(
            context,
            errorMessage,
            Toast.LENGTH_SHORT
        ).show()
    } else {
        showRenameDishDialog.value = false
    }
}

@Composable
@Preview
fun RenameMealDialog(
    onDismissRequest: () -> Unit = { },
    onConfirmation: (String) -> Unit = { },
    name: String = "Meal Name",
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        RenameForm(
            onDismissRequest = onDismissRequest,
            onConfirmation = onConfirmation,
            titleText = R.string.rename_meal,
            initialName = name,
        )
    }
}

@Composable
@Preview
fun RenameDishDialog(
    onDismissRequest: () -> Unit = { },
    onConfirmation: (String) -> Unit = { },
    name: String = "Dish Name",
) {
    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        RenameForm(
            onDismissRequest = onDismissRequest,
            onConfirmation = onConfirmation,
            titleText = R.string.rename_dish,
            initialName = name
        )
    }
}

@Composable
fun RenameForm(
    onDismissRequest: () -> Unit,
    onConfirmation: (String) -> Unit,
    titleText: Int = R.string.rename,
    initialName: String = "Default Name"
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
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
                text = stringResource(id = titleText),
            )
            TextField(
                value = name,
                onValueChange = {
                    name = it
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
                    onClick = { onConfirmation(name) },
                    modifier = Modifier
                        .padding(8.dp)
                ) {
                    Text("Confirm")
                }
            }
        }
    }
}

@Composable
fun MealDetailBody(
    modifier: Modifier = Modifier,
    mealDetailUiState: MealDetailUiState = MealDetailUiState(),
    onDishesChanged: (MealDetails) -> Unit = {},
    onMealInstanceDetailsChange: (MealInstanceDetails) -> Unit = {},
    onMealNameChanged: (String) -> Unit = {},
    onDishValueChanged: (Int, String) -> Unit = { _: Int, _: String -> },
    mealSearchResults: List<MealDetails> = listOf(),
    dishSearchResults: List<DishDetails> = listOf(),
    onDeleteDishClick: (Int) -> Boolean = { _: Int -> true },
    onRestoreDishClick: (DishDetails) -> Unit = {},
    onFindExistingMeal: (String) -> Unit = {},
    onFindExistingDish: (Int, String) -> Unit = { _: Int, _: String -> },
    onDishAdded: (DishDetails) -> Unit = {},
    onMealSearchTermChanged: (String) -> Unit = {},
    onDishSearchTermChanged: (String) -> Unit = {},
    onMealEditClick: () -> Unit = {},
    onDishEditClick: (Int) -> Unit = {},
    onUpdateImage: (String) -> Unit = {},
    isDuplicateCheck: (Int) -> Boolean = { false },
) {
    val mealInstanceDetails = mealDetailUiState.mealInstanceDetails
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(
            dimensionResource(id = R.dimen.padding_small)
        )
    ) {
        MealField(
            modifier = Modifier
                .fillMaxWidth(),
            mealDetails = mealInstanceDetails.mealDetails,
            onNameChange = onMealNameChanged,
            onMealClick = onFindExistingMeal,
            searchResults = mealSearchResults,
            onSearchTermChanged = onMealSearchTermChanged,
            onEditClick = onMealEditClick,
        )
        DishesFields(
            modifier = Modifier,
            onNameChange = onDishValueChanged,
            mealDetails = mealInstanceDetails.mealDetails,
            onDishAdded = onDishAdded,
            onDishClick = onFindExistingDish,
            searchResults = dishSearchResults,
            onDeleteDishClick = onDeleteDishClick,
            onRestoreDishClick = onRestoreDishClick,
            onDishSearchTermChanged = onDishSearchTermChanged,
            onDishEditClick = onDishEditClick,
            isDuplicate = isDuplicateCheck,
        )
        OccasionDropdown(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium)),
            onOccasionChange = {
                onMealInstanceDetailsChange(mealInstanceDetails.copy(occasion = it))
            },
            selectedValue = mealInstanceDetails.occasion,
        )
        RatingSelector(
            modifier = Modifier
                .fillMaxWidth(),
            onRatingChange = {
                onDishesChanged(mealInstanceDetails.mealDetails.copy(rating = it))
            },
            rating = mealInstanceDetails.mealDetails.rating,
        )
        ImageField(
            modifier = Modifier,
            imageSrc = mealInstanceDetails.mealDetails.imgSrc,
            onUpdateImage = onUpdateImage,
        )
    }
}

@Composable
@Preview
fun ImageField(
    modifier: Modifier = Modifier,
    imageSrc: String? = null,
    onUpdateImage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val pickImageContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()) { uri: Uri? ->
            if (uri != null) {
                onUpdateImage(uri.toString())
            }
    }

    if (!imageSrc.isNullOrBlank()) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(id = R.dimen.padding_small)),
        ){
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                onClick = { onUpdateImage("") },
            ) {
                Text(text = "Remove Image")
            }
        }
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(imageSrc)
                .crossfade(true)
                .build(),
            contentScale = ContentScale.Crop,
            contentDescription = null,
            modifier = modifier
                .clip(
                    RoundedCornerShape(
                        corner = CornerSize(dimensionResource(id = R.dimen.padding_small))
                    )
                )
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium))
                .clickable { pickImageContract.launch("image/*") },
        )
    }
    else {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium))
                .height(200.dp)
                .clickable { pickImageContract.launch("image/*") }
                .drawBehind {
                    drawRoundRect(
                        color = Color.Gray,
                        style = Stroke(
                            width = 2f,
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                        ),
                        cornerRadius = CornerRadius(8.dp.toPx()),
                    )
                },
        )
        {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = dimensionResource(id = R.dimen.padding_small)),
            ) {
                Text(text = stringResource(R.string.add_image))
                Icon(
                    imageVector = Icons.Filled.AddCircle,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally),
                    contentDescription = null,
                )
            }
        }
    }
}

@Composable
@Preview
fun SaveButton(
    onSaveClick: () -> Unit = {},
    enabled: Boolean = true,
) {
    Button(
        onClick = onSaveClick,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        )
    ) {
        Text(stringResource(R.string.save))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
@Preview
fun MealField(
    modifier: Modifier = Modifier,
    mealDetails: MealDetails = MealDetails(name = "mealName"),
    onNameChange: (String) -> Unit = {},
    searchResults: List<MealDetails> = emptyList(),
    onMealClick: (String) -> Unit = {},
    onSearchTermChanged: (String) -> Unit = {},
    onEditClick: () -> Unit = {},
) {
    val keyboardVisibleState = rememberUpdatedState(WindowInsets.isImeVisible)
    var active by remember { mutableStateOf(false) }
    val paddingId = R.dimen.padding_medium
    val minHeight = 56.dp + (dimensionResource(id = paddingId)*2)
    val defaultHeight = 56.dp
    val targetHeight = when {
        searchResults.isEmpty() || !active -> minHeight
        else -> (minHeight.value + (searchResults.size * defaultHeight.value))
            .coerceAtMost(448F).dp
    }
    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = 300), label = "Animated Meal Field Height"
    )

    // Reset the active state when the keyboard is closed
    LaunchedEffect(key1 = keyboardVisibleState.value) {
        if (!keyboardVisibleState.value) active = false
    }

    FieldHeader(modifier = modifier, string = stringResource(id = R.string.meal_header))

    Box(modifier = Modifier
        .height(animatedHeight)
        .padding(vertical = dimensionResource(id = paddingId)),
    ) {
        DockedSearchBar(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = dimensionResource(id = paddingId))
                .onFocusChanged { focusState ->
                    if (!focusState.isFocused) {
                        onMealClick(mealDetails.name)
                        active = false
                    }
                },
            query = mealDetails.name,
            onQueryChange = onNameChange,
            onSearch = {
                onMealClick(mealDetails.name)
                active = false
            },
            placeholder = {
                when (mealDetails.name.isBlank()) {
                    true -> {
                        Text(text = stringResource(R.string.meal_name_req))
                    }

                    else -> {
                        Text(text = mealDetails.name)
                    }
                }
            },
            trailingIcon = {
                if (mealDetails.mealId != 0L) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_meal),
                        modifier = Modifier
                            .clickable { onEditClick() }
                            .padding(dimensionResource(id = R.dimen.padding_small))
                    )
                }
            },
            content = {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(32.dp),
                    contentPadding = PaddingValues(16.dp),
                    modifier = Modifier
                        .fillMaxSize(),
                ) {
                    items(
                        count = searchResults.size,
                        key = { index -> searchResults[index].mealId }
                    ) { index ->
                        val meal = searchResults[index]
                        MealListItem(
                            onMealClick = {
                                onMealClick(it)
                                active = false
                            },
                            mealDetails = meal
                        )
                    }
                }
            },
            colors = SearchBarDefaults.colors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                dividerColor = MaterialTheme.colorScheme.secondaryContainer,
                inputFieldColors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
            ),
            active = active,
            onActiveChange = {
                if (active) onMealClick(mealDetails.name) else onSearchTermChanged(mealDetails.name)
                active = it
            },
        )
    }
}

@Composable
fun FieldHeader(
    modifier: Modifier = Modifier,
    string: String = "Meal",
    style: TextStyle = MaterialTheme.typography.titleLarge
) {
    Text(
        modifier = modifier,
        textAlign = TextAlign.Center,
        text = string,
        style = style,
    )
}

@Composable
fun MealListItem(
    mealDetails: MealDetails,
    onMealClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onMealClick(mealDetails.name) },
    ) {
        Text(text = mealDetails.name)
        Text(text = mealDetails.rating.toString())
    }
}

@Composable
fun DishesFields(
    modifier: Modifier,
    mealDetails: MealDetails = MealDetails(),
    onNameChange: (Int, String) -> Unit,
    onDishAdded: (DishDetails) -> Unit = {},
    searchResults: List<DishDetails>,
    onDeleteDishClick: (Int) -> Boolean = { _ -> false },
    onRestoreDishClick: (DishDetails) -> Unit = {},
    onDishClick: (Int, String) -> Unit,
    onDishSearchTermChanged: (String) -> Unit,
    onDishEditClick: (Int) -> Unit,
    isDuplicate: (Int) -> Boolean = { _ -> true },
) {
    val altFormat = remember { mutableStateOf(false) }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        FieldHeader(
            modifier = Modifier,
            string = stringResource(id = R.string.dishes_header),
        )
        Row(
            modifier = Modifier.align(Alignment.CenterEnd),
            horizontalArrangement = Arrangement.End
        ) {
            FormatCheckBox(
                modifier = Modifier
                    .padding(
                        vertical = 0.dp,
                        horizontal = dimensionResource(id = R.dimen.padding_medium)
                    ),
                value = altFormat.value,
                onCheck = { altFormat.value = it }
            )
        }
    }

    if (altFormat.value) {
        FieldHeader(
            modifier = Modifier.fillMaxWidth(),
            string = stringResource(id = R.string.dishes_subheader_learning),
            style = MaterialTheme.typography.titleMedium
        )
        mealDetails.dishes.forEachIndexed { index, dish ->
            if (dish.rating == Rating.LEARNING) {
                DishField(
                    modifier = Modifier,
                    onNameChange = { onNameChange(index, it) },
                    onDishClick = { onDishClick(index, it) },
                    dishDetails = dish,
                    searchResults = searchResults.filter { it.rating == Rating.LEARNING },
                    valid = !isDuplicate(index),
                    onDeleteDishClick = { onDeleteDishClick(index) },
                    onRestoreDishClick = onRestoreDishClick,
                    onDishEditClick = { onDishEditClick(index) },
                    onDishSearchTermChanged = onDishSearchTermChanged,
                )
            }
        }
        AddDishButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium)),
            onClick = {
                onDishAdded(DishDetails(dishId = 0, name = "", rating = Rating.LEARNING))
            },
        )

        FieldHeader(
            modifier = Modifier.fillMaxWidth(),
            string = stringResource(id = R.string.dishes_subheader_like),
            style = MaterialTheme.typography.titleMedium
        )
        mealDetails.dishes.forEachIndexed { index, dish ->
            if (dish.rating == Rating.LIKEIT) {
                DishField(
                    modifier = Modifier,
                    onNameChange = { onNameChange(index, it) },
                    onDishClick = { onDishClick(index, it) },
                    dishDetails = dish,
                    searchResults = searchResults.filter { it.rating == Rating.LIKEIT },
                    valid = !isDuplicate(index),
                    onDeleteDishClick = { onDeleteDishClick(index) },
                    onRestoreDishClick = onRestoreDishClick,
                    onDishEditClick = { onDishEditClick(index) },
                    onDishSearchTermChanged = onDishSearchTermChanged,
                )
            }
        }
        AddDishButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium)),
            onClick = {
                onDishAdded(DishDetails(dishId = 0, name = "", rating = Rating.LIKEIT))
            },
        )

        FieldHeader(
            modifier = Modifier.fillMaxWidth(),
            string = stringResource(id = R.string.dishes_subheader_love),
            style = MaterialTheme.typography.titleMedium
        )
        mealDetails.dishes.forEachIndexed { index, dish ->
            if (dish.rating == Rating.LOVEIT) {
                DishField(
                    modifier = Modifier,
                    onNameChange = { onNameChange(index, it) },
                    onDishClick = { onDishClick(index, it) },
                    dishDetails = dish,
                    searchResults = searchResults.filter { it.rating == Rating.LOVEIT },
                    valid = !isDuplicate(index),
                    onDeleteDishClick = { onDeleteDishClick(index) },
                    onRestoreDishClick = onRestoreDishClick,
                    onDishEditClick = { onDishEditClick(index) },
                    onDishSearchTermChanged = onDishSearchTermChanged,
                )
            }
        }
        AddDishButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium)),
            onClick = {
                onDishAdded(DishDetails(dishId = 0, name = "", rating = Rating.LOVEIT))
            },
        )
    }
    else
    {
        mealDetails.dishes.forEachIndexed { index, dish ->
            DishField(
                modifier = Modifier,
                onNameChange = { onNameChange(index, it) },
                onDishClick = { onDishClick(index, it) },
                dishDetails = dish,
                searchResults = searchResults,
                valid = !isDuplicate(index),
                onDeleteDishClick = { onDeleteDishClick(index) },
                onRestoreDishClick = onRestoreDishClick,
                onDishEditClick = { onDishEditClick(index) },
                onDishSearchTermChanged = onDishSearchTermChanged,
            )
        }
        AddDishButton(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium)),
            onClick = {
                onDishAdded(DishDetails(dishId = 0, name = "", rating = Rating.LIKEIT))
            },
        )
    }
}

@Composable
fun AddDishButton(
    modifier: Modifier,
    onClick: () -> Unit = {},
) {
    Button(
        modifier = modifier,
        onClick = onClick,
    ) {
        Text(stringResource(R.string.add_dish))
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
@Preview
fun DishField(
    modifier: Modifier = Modifier,
    onNameChange: (String) -> Unit = { _ -> },
    onDishEditClick: () -> Unit = {},
    onDishClick: (String) -> Unit = { _ -> },
    dishDetails: DishDetails = DishDetails(1L, "Dish", Rating.LIKEIT),
    valid: Boolean = true,
    searchResults: List<DishDetails> = listOf(DishDetails(2L, "Dish", Rating.LIKEIT)),
    onDeleteDishClick: () -> Boolean = { false },
    onRestoreDishClick: (DishDetails) -> Unit = {},
    onDishSearchTermChanged: (String) -> Unit = { _ -> },
) {
    val keyboardVisibleState = rememberUpdatedState(WindowInsets.isImeVisible)
    var active by remember { mutableStateOf(false) }
    var deleted by remember { mutableStateOf(false) }
    val horizontalPaddingId = R.dimen.padding_medium
    val verticalPaddingId = R.dimen.padding_small
    val minHeight = 56.dp + (dimensionResource(id = verticalPaddingId)*2)
    val defaultHeight = 56.dp
    val targetHeight = when {
        searchResults.isEmpty() || !active -> minHeight
        else -> (minHeight.value + (searchResults.size * defaultHeight.value))
            .coerceAtMost(448F).dp
    }

    val animatedHeight by animateDpAsState(
        targetValue = targetHeight,
        animationSpec = tween(durationMillis = 300), label = "Animated Dish Field Height"
    )

    // Reset the active state when the keyboard is closed
    LaunchedEffect(key1 = keyboardVisibleState.value) {
        if (!keyboardVisibleState.value) active = false
    }

    ProvideTextStyle(
        value = if (deleted)
            TextStyle(textDecoration = TextDecoration.LineThrough)
        else LocalTextStyle.current
    ) {
        Box(modifier = Modifier
            .height(animatedHeight)
            .padding(vertical = dimensionResource(id = verticalPaddingId)),
        ) {
            DockedSearchBar(
                modifier = modifier
                    .fillMaxSize()
                    .padding(horizontal = dimensionResource(id = horizontalPaddingId))
                    .onFocusChanged { focusState ->
                        if (!focusState.isFocused) {
                            onDishClick(dishDetails.name)
                            active = false
                        }
                    },
                query = dishDetails.name,
                onQueryChange = onNameChange,
                onSearch = {
                    onDishClick(dishDetails.name)
                    active = false
                },
                placeholder = {
                    when (dishDetails.name.isBlank()) {
                        true -> {
                            Text(text = stringResource(R.string.dish_name_req))
                        }

                        else -> {
                            if (deleted) {
                                Text(
                                    text = dishDetails.name,
                                    textDecoration = TextDecoration.LineThrough
                                )
                            } else Text(text = dishDetails.name)
                        }
                    }
                },
                trailingIcon = {
                    if (dishDetails.dishId != 0L)
                        Icon(
                            modifier = Modifier.clickable { onDishEditClick() },
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit name of dish $dishDetails.name Button",
                        )
                },
                leadingIcon = {
                    Icon(
                        modifier = Modifier.clickable {
                            deleted = when (deleted) {
                                true -> {
                                    onRestoreDishClick(dishDetails)
                                    false
                                }
                                else -> {
                                    !onDeleteDishClick()
                                }
                            }
                        },
                        imageVector = when (deleted) {
                            true -> ImageVector.vectorResource(id = R.drawable.baseline_undo_24)
                            else -> Icons.Filled.Delete
                        },
                        contentDescription = "Delete dish " + dishDetails.name + " Button",
                    )
                },
                content = {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(32.dp),
                        contentPadding = PaddingValues(16.dp),
                        modifier = Modifier
                            .fillMaxSize(),
                    ) {
                        items(
                            count = searchResults.size,
                            key = { index -> searchResults[index].dishId }
                        ) { index ->
                            val dish = searchResults[index]
                            DishListItem(
                                onDishClick = {
                                    onDishClick(dish.name)
                                    active = false
                                },
                                dishDetails = dish,
                            )
                        }
                    }
                },
                enabled = !deleted,
                colors = when (valid) {
                    true -> SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        dividerColor = MaterialTheme.colorScheme.secondaryContainer,
                        inputFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                        ),
                    )

                    else -> SearchBarDefaults.colors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        dividerColor = MaterialTheme.colorScheme.errorContainer,
                        inputFieldColors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.errorContainer,
                            disabledContainerColor = MaterialTheme.colorScheme.errorContainer,
                        ),
                    )
                },
                active = active,
                onActiveChange = {
                    if
                        (active) onDishClick(dishDetails.name)
                    else
                        onDishSearchTermChanged(dishDetails.name)
                    active = it
                },
            )
        }
    }
}

@Composable
fun DishListItem(
    dishDetails: DishDetails,
    modifier: Modifier = Modifier,
    onDishClick: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier
            .fillMaxWidth()
            .clickable { onDishClick() }
    ) {
        Text(text = dishDetails.name)
        Text(text = dishDetails.rating.ratingEmoji.emojiString)
    }
}

@Composable
fun RatingSelector(
    modifier: Modifier,
    onRatingChange: (Rating) -> Unit,
    rating: Rating,
) {
    Row(
        modifier = modifier
            .padding(dimensionResource(id = R.dimen.padding_medium)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small))
    ) {
        Rating.entries.map { value ->
            RatingSelectorItem(
                onClick = { onRatingChange(value) },
                ratingEmoji = value.ratingEmoji,
                selected = value == rating,
            )
        }
    }
}

@Composable
fun RatingSelectorItem(
    onClick: () -> Unit,
    ratingEmoji: RatingEmoji,
    selected: Boolean = false,
) {
    Row {
        Column(
            modifier = Modifier
                .padding(dimensionResource(id = R.dimen.padding_medium))
                .align(Alignment.CenterVertically)
                .clickable(onClick = onClick),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                text = ratingEmoji.emojiString,
                fontSize = when (selected) {
                    true -> 18.sp
                    false -> 10.sp
                },
            )
            Text(
                modifier = Modifier,
                text = ratingEmoji.description,
                maxLines = 2,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OccasionDropdown(
    modifier: Modifier,
    onOccasionChange: (Occasion) -> Unit = {},
    selectedValue: Occasion,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
        },
        modifier = modifier
            .fillMaxWidth()
    ) {
        OutlinedTextField(
            value = selectedValue.name,
            onValueChange = { },
            readOnly = true,
            label = { Text(stringResource(R.string.occasion)) },
            placeholder = {
                Text(text = "Select the occasion")
            },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.textFieldColors(),
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            Occasion.entries.forEach { occasion ->
                DropdownMenuItem(
                    text = { Text(occasion.name) },
                    onClick = {
                        onOccasionChange(occasion)
                        expanded = false
                    },
                )
            }
        }
    }
}
@Composable
@Preview(showBackground = true)
fun PreviewBody() {
    MealDetailBody()
}