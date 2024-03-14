package com.jimmy.parentsmealplanner.ui.meal

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.jimmy.parentsmealplanner.R
import com.jimmy.parentsmealplanner.ui.nav.NavigationDestination
import com.jimmy.parentsmealplanner.ui.shared.DishDetails
import com.jimmy.parentsmealplanner.ui.shared.MealDetails
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import com.jimmy.parentsmealplanner.ui.shared.Rating
import com.jimmy.parentsmealplanner.ui.shared.TopBar
import kotlinx.datetime.LocalDate

object MealPlanningDest : NavigationDestination {
    override val route = "meal_planning"
    override val titleRes = R.string.app_name
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealPlanner(
    modifier: Modifier = Modifier,
    navigateToMealDetail: (Int?, LocalDate, Occasion) -> Unit = { _, _, _ -> },
    viewModel: MealPlanningViewModel = hiltViewModel(),
) {
    val mealUiState by viewModel.mealUiState.collectAsState()
    val dateUiState by viewModel.dateUiState.collectAsState()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()


    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
                title = stringResource(id = R.string.app_name),
                canNavigateBack = false,
                scrollBehavior = scrollBehavior,
            )
        },
    ) {
        Column(modifier = Modifier
            .padding(paddingValues = it)
        ) {
            WeekBar(dateUiState = dateUiState)
            MealPlanningBody(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                onMealClick = navigateToMealDetail,
                mealUiState = mealUiState,
                dateUiState = dateUiState,
            )
        }
    }
}

// Creates a top bar that displays the current week and day
@Composable
@Preview(apiLevel = 33)
fun WeekBar(
    modifier: Modifier = Modifier,
    dateUiState: DateUiState = DateUiState(),
) {
    LazyRow(modifier = modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
        items(dateUiState.daysOfSelectedWeek) { day ->
            Column {
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

// Creates the main body for the screen
@Composable
@Preview(apiLevel = 33)
fun MealPlanningBody(
    modifier: Modifier = Modifier,
    mealUiState: MealUiState = MealUiState(),
    dateUiState: DateUiState = DateUiState(),
    onMealClick: (Int?, LocalDate, Occasion) -> Unit = { _: Int?, _: LocalDate, _: Occasion -> },
) {
    Column (
        modifier = modifier.fillMaxWidth()
    ) {
        dateUiState.daysOfSelectedWeek.forEach { day ->
            DayOfTheWeek(
                date = day,
                meals = mealUiState.getMealsForDate(date = day),
                onMealClick = onMealClick
            )
        }
    }
}

// Creates a column for the meals for a day of the week
@Composable
@Preview(apiLevel = 33)
fun DayOfTheWeek(
    modifier: Modifier = Modifier,
    date: LocalDate = LocalDate(1, 1, 1),
    meals: List<MealDetails> = listOf(
        MealDetails(name = "Breakfast meal"),
        MealDetails(name = "Breakfast meal 2"),
        MealDetails(occasion = Occasion.DINNER, name = "Dinner meal")
    ),
    onMealClick: (Int?, LocalDate, Occasion) -> Unit = { _: Int?, _: LocalDate, _: Occasion -> },
) {
    Column (modifier = modifier) {
        Text(text = String.format(stringResource(R.string.planner_date_entry),
            date.dayOfWeek.toString(), date.month.toString(), date.dayOfMonth.toString()))
        DailyMeals(mealList = meals, onMealClick = onMealClick, date = date)
    }
}

/**
 * Creates a column for the meals for a day of the week
 */
@Composable
@Preview(apiLevel = 33)
fun DailyMeals(
    modifier: Modifier = Modifier,
    mealList: List<MealDetails> = listOf(
        MealDetails(name = "Breakfast meal"),
        MealDetails(name = "Breakfast meal 2"),
        MealDetails(occasion = Occasion.DINNER, name = "Dinner meal")
    ),
    onMealClick: (Int?, LocalDate, Occasion) -> Unit = { _: Int?, _: LocalDate, _: Occasion -> },
    date: LocalDate = LocalDate(1, 1, 1),
){
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Occasion.entries.forEach { occasion ->
            val mealsForOccasion = mealList.filter { it.occasion == occasion }
            OccasionMeals(
                occasion = occasion,
                mealsForOccasion = mealsForOccasion,
                onMealClick = onMealClick,
                date = date
            )
        }
    }
}

/**
 * Creates MealHolders for the different meal occasions
 */
@Composable
@Preview(apiLevel = 33)
fun OccasionMeals(
    modifier: Modifier = Modifier,
    occasion: Occasion = Occasion.BREAKFAST,
    @DrawableRes icon: Int = R.drawable.food_icon,
    mealsForOccasion: List<MealDetails> = listOf(
        MealDetails(
            name = "Breakfast meal", dishes = listOf(
                DishDetails(name = "Dish 1"),
                DishDetails(name = "Dish 2"),
                DishDetails(name = "Dish 3"),
            )
        ),
        MealDetails(name = "Breakfast meal 2"),
        MealDetails(name = "Breakfast meal 3")
    ),
    onMealClick: (id: Int?, date: LocalDate, occasion: Occasion) -> Unit =
        { _: Int?, _: LocalDate, _: Occasion -> },
    date: LocalDate = LocalDate(1, 1, 1),
) {
    OutlinedCard (modifier = modifier){
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start,
        ) {
            Row(modifier = Modifier, verticalAlignment = Alignment.CenterVertically) {
                Image(painter = painterResource(id = icon), contentDescription = null)
                Column {
                    Text(text = occasion.name)
                    mealsForOccasion.forEach { meal ->
                        MealHolder(
                            meal = meal,
                            modifier = modifier.padding(1.dp),
                            onMealClick = onMealClick,
                        )
                    }
                    AddMealButton(onClick = { onMealClick(0, date, occasion) })
                }
            }
        }
    }
}

/**
 * Creates a section to display a list meals that can be clicked to dropdown a list of dishes
 */
@Composable
@Preview(apiLevel = 33)
fun MealHolder(
    modifier: Modifier = Modifier,
    meal: MealDetails = MealDetails(
        id = 0,
        name = stringResource(id = R.string.meal_contents_placeholder),
        date = LocalDate(1, 1, 1),
        occasion = Occasion.BREAKFAST, rating = Rating.LIKEIT,
    ),
    onMealClick: (Int?, LocalDate, Occasion) -> Unit = { _: Int?, _: LocalDate, _: Occasion -> },
) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val drawable: Painter = (when (expanded) {
        true -> painterResource(id = R.drawable.ic_chevron_up)
        false -> painterResource(id = R.drawable.ic_chevron_down)
    })
    Row(modifier = Modifier
        .background(Color.LightGray)
    ) {
        MealText(modifier = Modifier.clickable { onMealClick(meal.id, meal.date, meal.occasion) },
            mealName = meal.name
        )
        Image(painter = drawable,
            contentDescription = "Button: Expand/Collapse meal list",
            modifier = Modifier.
            padding(start = 4.dp, top = 2.dp).
            fillMaxSize(0.05f).
            clickable { expanded = !expanded }
        )
    }
    when {
        expanded -> {
            /*LazyColumn(modifier = Modifier) {
                items(items = meal.dishes ?: listOf()) { dish ->
                    DishText(dishName = dish.name)
                }
            }*/
            (meal.dishes ?: listOf()).forEach { dish ->
                DishText(dishName = dish.name)}
        }
    }
}

@Composable
@Preview(apiLevel = 33)
fun MealText(
    modifier: Modifier = Modifier,
    mealName: String = stringResource(R.string.meal_contents_placeholder),
) {
    Text(text = mealName)
}

@Composable
@Preview(apiLevel = 33)
fun DishText(
    modifier: Modifier = Modifier,
    dishName: String = stringResource(R.string.dish_name_placeholder),
) {
    Text(text = dishName)
}

// Creates a button that allows the user to add a meal
@Composable
@Preview(apiLevel = 33)
fun AddMealButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit = { }
) {
    Button (onClick =  onClick, modifier = modifier, shape = MaterialTheme.shapes.small) {
        Text(text = stringResource(id = R.string.add_meal_button_text))
    }
}