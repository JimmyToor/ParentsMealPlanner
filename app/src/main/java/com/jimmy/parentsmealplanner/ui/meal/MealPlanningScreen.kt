package com.jimmy.parentsmealplanner.ui.meal

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.jimmy.parentsmealplanner.R
import com.jimmy.parentsmealplanner.ui.nav.TopBar
import kotlinx.datetime.LocalDate


@Composable
@Preview
fun MealPlanner(
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopBar(
            )
        }
    ) {
        Column (modifier = Modifier.padding(paddingValues = it)){
            WeekBar()
            MealPlanningBody()
        }
    }

}

// Creates a top bar that displays the current week and day
@Composable
@Preview
fun WeekBar(
    modifier: Modifier = Modifier,
    dates: List<LocalDate> = listOf<LocalDate>(LocalDate(2024,1,1),
        LocalDate(2024,1,2),
        LocalDate(2024,1,3),
        LocalDate(2024,1,4),
        LocalDate(2024,1,5),
        LocalDate(2024,1,6),
        LocalDate(2024,1,7)),
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        for (date in dates) {
            Column() {
                Text(
                    text = date.dayOfWeek.toString().substring(0..2),
                    modifier = Modifier.padding(horizontal = 12.dp),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = date.dayOfMonth.toString(),
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    style = MaterialTheme.typography.titleLarge,
                    )
            }

        }
    }
}

// Creates the main body for the screen
@Composable
@Preview
fun MealPlanningBody(
    modifier: Modifier = Modifier,
) {
    Column (modifier = Modifier.fillMaxWidth()){
        DayOfTheWeek()
        DayOfTheWeek()
        DayOfTheWeek()
        DayOfTheWeek()
        DayOfTheWeek()
        DayOfTheWeek()
        DayOfTheWeek()
    }
}


// Creates a column for the meals for a day of the week
@Composable
@Preview
fun DayOfTheWeek(
    modifier: Modifier = Modifier,
    day: String = stringResource(R.string.placeholder_day)
) {
    Column (modifier = modifier) {
        Text(text = "Monday")
        DailyMeals()
    }
}

// Creates MealHolders for the different meals of the day e.g. breakfast, lunch
@Composable
@Preview
fun DailyMeals(
    modifier: Modifier = Modifier,
) {
    Column ( modifier = modifier,
        ){  // lazycolumn? have a snack option after/before each of these?
        MealHolder(occasion = R.string.breakfast, modifier = modifier.padding(1.dp))
        MealHolder(occasion = R.string.lunch, modifier = modifier.padding(1.dp))
        MealHolder(occasion = R.string.dinner, modifier = modifier.padding(1.dp))
    }
}

// Creates a section to display a specific meal
@Composable
@Preview
fun MealHolder(
    modifier: Modifier = Modifier,
    @StringRes occasion: Int = R.string.meal_holder_title_default,
    @DrawableRes icon: Int = R.drawable.food_icon,
) {
    Column (
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(modifier = modifier
            .background(Color.DarkGray)
            .fillMaxWidth(.9f)) {
            Row(modifier = Modifier) {
                Image(painter = painterResource(id = icon), contentDescription = null)
                Column {
                    Text(text = stringResource(id = occasion))
                    Text(text = stringResource(R.string.meal_contents_placeholder))
                }
            }
        }
    }
}