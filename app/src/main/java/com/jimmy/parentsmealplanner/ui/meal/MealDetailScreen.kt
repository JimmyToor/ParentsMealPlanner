package com.jimmy.parentsmealplanner.ui.meal

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewModelScope
import com.jimmy.parentsmealplanner.R
import com.jimmy.parentsmealplanner.ui.nav.NavigationDestination
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
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
    val routeWithArgs = "$route/{$DATE_ARG}/{$OCCASION_ARG}/{$MEAL_ID_ARG}"
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

    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = stringResource(id = R.string.app_name),
                canNavigateBack = canNavigateBack,
                )
        },
    ) {
        Column(modifier = Modifier.padding(paddingValues = it)) {
            Header(date = mealDetailUiState.meal.date)
            MealDetailBody(
                modifier = Modifier
                .verticalScroll(rememberScrollState())
                .fillMaxWidth(),
                mealDetailUiState = viewModel.mealDetailUiState,
                onValueChange = viewModel::updateUiState,
                onNameChanged = viewModel::onSearchTermChange,
                onSaveClick = {
                    viewModel.viewModelScope.launch {
                        withContext(Dispatchers.IO) {
                            viewModel.saveMeal()
                        }
                        navigateBack()
                    }
                },
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
    onValueChange: (MealDetails) -> Unit = {},
    onSaveClick: () -> Unit = {},
    onNameChanged: (String) -> Unit = {},
    ) {
    Column(modifier = modifier) {
        MealInputForm(
            mealDetails = mealDetailUiState.meal,
            onValueChange = onValueChange,
            onNameChanged = onNameChanged,
        )
        SaveButton(
            onSaveClick = onSaveClick,
            enabled = mealDetailUiState.isEntryValid,
        )
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

@Composable
fun MealInputForm(
    mealDetails: MealDetails = MealDetails(),
    modifier: Modifier = Modifier,
    onValueChange: (MealDetails) -> Unit = {},
    onNameChanged: (String) -> Unit = {},
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(dimensionResource(id = R.dimen.padding_small))
    ) {
        OutlinedTextField(
            value = mealDetails.name,
            onValueChange = {
                onNameChanged(it)
                onValueChange(mealDetails.copy(name = it))
            },
            label = { Text(stringResource(R.string.meal_name_req)) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                unfocusedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                disabledContainerColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium)),
            singleLine = true
        )
        OccasionDropdown(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium)),
            onValueChange = onValueChange,
            selectedValue = mealDetails.occasion,
            mealDetails = mealDetails,
        )
        RatingSelector(
            modifier = Modifier,
            onRatingChange = { onValueChange(mealDetails.copy(rating = it)) },
            rating = mealDetails.rating,
            emojis = listOf(
                RatingEmoji(emoji = "\uD83E\uDD70", description = "Learning to Love It",
                    Rating.LEARNING),
                RatingEmoji(emoji = "\uD83E\uDD14", description = "Like It", Rating.LIKEIT),
                RatingEmoji(emoji = "\uD83D\uDE0A", description = "Love It", Rating.LOVEIT),)
        )
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
        Column (modifier = Modifier.padding(dimensionResource(id = R.dimen.padding_medium))
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
    mealDetails: MealDetails = MealDetails(),
    onValueChange: (MealDetails) -> Unit = {},
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
                        onValueChange(mealDetails.copy(occasion = occasion))
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