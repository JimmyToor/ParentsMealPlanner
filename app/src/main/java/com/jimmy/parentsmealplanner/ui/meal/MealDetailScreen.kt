package com.jimmy.parentsmealplanner.ui.meal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.jimmy.parentsmealplanner.R
import com.jimmy.parentsmealplanner.ui.nav.NavigationDestination
import com.jimmy.parentsmealplanner.ui.shared.DishDetails
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
import com.jimmy.parentsmealplanner.ui.shared.MealInstanceDetails
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.Rating
import com.jimmy.parentsmealplanner.ui.shared.RatingEmoji
import com.jimmy.parentsmealplanner.ui.shared.TopBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalDate

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
    navigateBack: () -> Unit = {},
    onNavigateUp: () -> Unit = {},
    canNavigateBack: Boolean = true,
    viewModel: MealDetailViewModel = hiltViewModel(),
) {
    val mealDetailUiState = viewModel.mealDetailUiState
    val mealSearchResults by viewModel.filteredMealSearchResults.collectAsStateWithLifecycle()
    val dishSearchResults by viewModel.filteredDishSearchResults.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = stringResource(id = R.string.app_name),
                canNavigateBack = canNavigateBack,
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
                    navigateBack()
                }
            })
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues = paddingValues)) {
            Header(date = mealDetailUiState.mealInstanceDetails.date)
            MealDetailBody(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .fillMaxWidth(),
                mealDetailUiState = viewModel.mealDetailUiState,
                onMealDetailsChange = { viewModel.updateUiState(mealDetails = it) },
                onMealInstanceDetailsChange = { viewModel.updateUiState(mealInstanceDetails = it) },
                onDeleteDishClick = viewModel::markDishForDeletion,
                onRestoreDishClick = viewModel::unMarkDishForDeletion,
                onMealNameChanged = viewModel::updateMealName,
                onDishNameChanged = viewModel::updateDishName,
                mealSearchResults = mealSearchResults,
                dishSearchResults = dishSearchResults,
                onFindExistingMeal = viewModel::findExistingMeal,
                onFindExistingDish = viewModel::findExistingDish,
            )
        }
    }
}

@Composable
@Preview(apiLevel = 33)
fun Header(
    modifier: Modifier = Modifier,
    date: LocalDate = LocalDate(2022, 1, 1),
) {
    Column(modifier = modifier) {
        Text(text = date.toString())
    }
}

@Composable
fun MealDetailBody(
    modifier: Modifier = Modifier,
    mealDetailUiState: MealDetailUiState = MealDetailUiState(),
    onMealDetailsChange: (MealDetails) -> Unit = {},
    onMealInstanceDetailsChange: (MealInstanceDetails) -> Unit = {},
    onMealNameChanged: (String) -> Unit = {},
    onDishNameChanged: (Int, String) -> Unit = { _: Int, _: String -> },
    mealSearchResults: List<MealDetails> = listOf(),
    dishSearchResults: List<DishDetails> = listOf(),
    onDeleteDishClick: (Int) -> Unit = {},
    onRestoreDishClick: (Int) -> Unit = {},
    onFindExistingMeal: (String) -> Unit = {},
    onFindExistingDish: (Int, String) -> Unit = { _: Int, _: String -> },
    ) {
    val mealInstanceDetails = mealDetailUiState.mealInstanceDetails
    Column(modifier = modifier) {
        Column(
            modifier = Modifier,
            verticalArrangement = Arrangement.spacedBy(
                dimensionResource(id = R.dimen.padding_small)
            )
        ) {
            MealField(
                modifier = Modifier,
                mealDetails = mealInstanceDetails.mealDetails,
                onNameChange = onMealNameChanged,
                onFindExistingMeal = onFindExistingMeal,
                searchResults = mealSearchResults,
            )
            DishesFields(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.padding_medium)),
                onNameChange = onDishNameChanged,
                mealDetails = mealInstanceDetails.mealDetails,
                onDishAdded = onMealDetailsChange,
                onFindExistingDish = onFindExistingDish,
                searchResults = dishSearchResults,
                onDeleteDishClick = onDeleteDishClick,
                onRestoreDishClick = onRestoreDishClick
            )
            OccasionDropdown(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(dimensionResource(id = R.dimen.padding_medium)),
                onOccasionChange = { onMealInstanceDetailsChange(mealInstanceDetails.copy(occasion = it))},
                selectedValue = mealInstanceDetails.occasion,
            )
            RatingSelector(
                modifier = Modifier,
                onRatingChange = { onMealDetailsChange(mealInstanceDetails.mealDetails.copy(rating = it))},
                rating = mealInstanceDetails.mealDetails.rating,
                emojis = listOf(
                    RatingEmoji(emoji = "\uD83E\uDD14", description = "Learning to Love It",
                        Rating.LEARNING),
                    RatingEmoji(emoji = "\uD83D\uDE0A", description = "Like It", Rating.LIKEIT),
                    RatingEmoji(emoji = "\uD83E\uDD70", description = "Love It", Rating.LOVEIT),)
            )
        }
    }
}

@Composable
fun SaveButton(
    onSaveClick: () -> Unit,
    enabled: Boolean = false,
) {
    Button(
        onClick = onSaveClick,
        enabled = enabled
    ) {
        Text(stringResource(R.string.save))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealField(
    modifier: Modifier = Modifier,
    mealDetails: MealDetails,
    onNameChange: (String) -> Unit = {},
    searchResults: List<MealDetails>,
    onFindExistingMeal: (String) -> Unit,
) {
    var active by remember { mutableStateOf(false) }

    DockedSearchBar(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.padding_medium)),
        query = mealDetails.name,
        onQueryChange = onNameChange,
        onSearch = {
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
        trailingIcon = {},
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = searchResults.size,
                    key = { index -> searchResults[index].mealId }
                ) { index ->
                    val meal = searchResults[index]
                    MealListItem(mealDetails = meal)
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
            active = !active
            if (active) onFindExistingMeal(mealDetails.name)
        },
    )
}

@Composable
fun MealListItem(
    mealDetails: MealDetails,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
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
    onDishAdded: (MealDetails) -> Unit = {},
    searchResults: List<DishDetails>,
    onDeleteDishClick: (Int) -> Unit = {},
    onRestoreDishClick: (Int) -> Unit = {},
    onFindExistingDish: (Int, String) -> Unit,
) {
    mealDetails.dishes.forEachIndexed { index, dish ->
        DishField(
            modifier = Modifier,
            onNameChange = { onNameChange(index, it) },
            onFindExistingDish = { onFindExistingDish(index, it) },
            dishDetails = dish,
            searchResults = searchResults,
            valid = !(
                mealDetails.dishes
                    .minus(dish)
                    .filterNot{ it.name.isBlank() }
                    .any { it.name == dish.name }
            )
        )
    }
    AddDishButton(
        modifier = modifier,
        onClick = {
            onDishAdded(mealDetails.copy(dishes = mealDetails.dishes.plus(DishDetails(name = ""))))
        },
    )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishField(
    modifier: Modifier = Modifier,
    onNameChange: (String) -> Unit,
    onFindExistingDish: (String) -> Unit,
    dishDetails: DishDetails,
    valid: Boolean = true,
    searchResults: List<DishDetails>,
) {
    var active by remember { mutableStateOf(false) }

    DockedSearchBar(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.padding_medium)),
        query = dishDetails.name,
        onQueryChange = onNameChange,
        onSearch = {
            active = false
        },
        placeholder = {
            when (dishDetails.name.isBlank()) {
                true -> {
                    Text(text = stringResource(R.string.dish_name_req))
                }
                else -> {
                    Text(text = dishDetails.name)
                }
            }
        },
        trailingIcon = {},
        content = {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(32.dp),
                contentPadding = PaddingValues(16.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    count = searchResults.size,
                    key = { index -> searchResults[index].dishId }
                ) { index ->
                    val dish = searchResults[index]
                    DishListItem(dishDetails = dish)
                }
            }
        },
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
            active = !active
            if (active) onFindExistingDish(dishDetails.name)
        },
    )
}

@Composable
fun DishListItem(
    dishDetails: DishDetails,
    modifier: Modifier = Modifier
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = modifier.fillMaxWidth()
    ) {
        Text(text = dishDetails.name)
        Text(text = dishDetails.rating.toString())
    }
}

@Composable
fun RatingSelector(
    modifier: Modifier,
    onRatingChange: (Rating) -> Unit,
    rating: Rating,
    emojis: List<RatingEmoji> = listOf(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.padding_medium)),
        horizontalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small))
    ) {
        emojis.map { value ->
            RatingSelectorItem(
                onClick = { onRatingChange(value.rating) },
                description = value.description,
                emoji = value.emoji,
                selected = value.rating == rating,
            )
        }
    }
}

@Composable
fun RatingSelectorItem(
    onClick: () -> Unit,
    description: String,
    emoji: String,
    selected: Boolean = false,
) {
    Row {
        Column (modifier = Modifier
            .padding(dimensionResource(id = R.dimen.padding_medium))
            .align(Alignment.CenterVertically)
            .clickable(onClick = onClick),
            verticalArrangement = Arrangement.SpaceEvenly,
        ) {
            Text(text = emoji, modifier = Modifier.align(Alignment.CenterHorizontally),
                fontSize =
                when (selected) {
                    true -> 18.sp
                    false -> 10.sp
                }
            )
            Text(text = description)
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
@Preview (showBackground = true)
fun PreviewBody() {
    MealDetailBody()
}