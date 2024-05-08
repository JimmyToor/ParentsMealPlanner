package com.jimmy.parentsmealplanner.ui.meal

import android.content.Context
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.Companion.isPhotoPickerAvailable
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
import com.jimmy.parentsmealplanner.util.checkResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID


/**
 * Object that represents the MealDetail destination in the navigation system.
 * It contains the route for the destination and the arguments required for the route.
 */
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

/**
 * Main composable function for the meal planner screen.
 * It displays the details of a meal and dishes and allows the user to edit them.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param navigateBack Function to navigate back in the navigation stack.
 * @param onNavigateUp Function to navigate up in the navigation hierarchy.
 * @param canNavigateBack Boolean indicating if navigation back is possible.
 * @param viewModel ViewModel that provides the data for the screen.
 * @param mainViewModel MainViewModel that provides for the activity.
 */
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
    val mealDetailUiState by viewModel.mealDetailUiState.collectAsStateWithLifecycle()
    val mealSearchResults by viewModel.filteredMealSearchResults.collectAsStateWithLifecycle()
    val dishSearchResults by viewModel.filteredDishSearchResults.collectAsStateWithLifecycle()
    var showRenameMealDialog by rememberSaveable { mutableStateOf(false) }
    var showEditDishDialog by rememberSaveable { mutableStateOf(false) }
    var showUnsavedChangesDialog by rememberSaveable { mutableStateOf(false) }

    // Index of the dish to be edited
    var targetDishIndex by rememberSaveable { mutableIntStateOf(0) }
    val context = LocalContext.current

    when { // Dialog to discard any unsaved changes made to the meal or dish.
        showUnsavedChangesDialog -> {
            UnsavedChangesDialog(
                onDismissRequest = { showUnsavedChangesDialog = false },
                onConfirmation = {
                    showUnsavedChangesDialog = false
                    if (!canNavigateBack || !navigateBack()) onNavigateUp()
                },
            )
        }

        // Dialog to rename the meal
        showRenameMealDialog -> {
            RenameMealDialog(
                onDismissRequest = { showRenameMealDialog = false },
                onConfirmation = { newName ->
                    viewModel.viewModelScope.launch {
                        val result = viewModel.updateMealName(
                            newName = newName,
                        )
                        checkResult(
                            result = result,
                            context = context,
                            onSuccess = { showRenameMealDialog = false },
                            errorMessage = "A meal with that name already exists."
                        )
                    }
                },
                initialName = mealDetailUiState.mealInstanceDetails.mealDetails.name,
            )
        }

        // Dialog to change the dish name or rating
        showEditDishDialog -> {
            EditDishDialog(
                onDismissRequest = { showEditDishDialog = false },
                onConfirmation = { newName, newRating ->
                    viewModel.viewModelScope.launch {
                        val result = viewModel.updateDish(
                            index = targetDishIndex,
                            newName = newName,
                            newRating = newRating,
                        )
                        checkResult(
                            result = result,
                            context = context,
                            onSuccess = { showEditDishDialog = false },
                            errorMessage = "A dish with that name already exists."
                        )
                    }
                },
                initialName = mealDetailUiState.mealInstanceDetails.mealDetails
                    .dishes[targetDishIndex].name,
                initialRating = mealDetailUiState.mealInstanceDetails.mealDetails
                    .dishes[targetDishIndex].rating,
                saved = mealDetailUiState.mealInstanceDetails.mealDetails
                        .dishes[targetDishIndex].dishId != 0L,
            )
        }
    }

    // Scaffold that contains the main UI of the screen
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
                    showUnsavedChangesDialog = true
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
                mealDetailUiState = mealDetailUiState,
                onDishAdded = viewModel::addDish,
                onMealRatingChanged = viewModel::changeMealRating,
                onMealInstanceDetailsChange = { viewModel.updateUiState(mealInstanceDetails = it) },
                onDeleteDishClick = viewModel::deleteDish,
                onRestoreDishClick = viewModel::unMarkDishForDeletion,
                onMealNameChanged = viewModel::changeMeal,
                onDishNameChanged = viewModel::changeDish,
                mealSearchResults = mealSearchResults,
                dishSearchResults = dishSearchResults,
                onFindExistingMeal = viewModel::findExistingMeal,
                onFindExistingDish = viewModel::findExistingDish,
                onMealSearchTermChanged = viewModel::onMealSearchTermChange,
                onDishSearchTermChanged = viewModel::onDishSearchTermChange,
                onMealEditClick = { showRenameMealDialog = true },
                onDishEditClick = {
                    targetDishIndex = it
                    showEditDishDialog = true
                },
                onUpdateImage = viewModel::updateImage,
                isDuplicateCheck = viewModel::isDuplicate,
                isDishSaved = viewModel::isDishSaved,
            )
        }
    }
}

/**
 * Displays a dialog to the user when there are unsaved changes.
 *
 * @param onDismissRequest The function that is invoked when the dialog is dismissed.
 * @param onConfirmation The function that is invoked when the user confirms to discard the changes.
 */
@Composable
private fun UnsavedChangesDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text("Discard Changes?") },
        text = { Text("Any unsaved changes will be lost.") },
        confirmButton = {
            TextButton(onClick = onConfirmation) {
                Text("Yes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text("No")
            }
        }
    )
}

/**
 * Displays a checkbox with a label.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param value The initial state of the checkbox. Default is false (unchecked).
 * @param onCheck The function that is invoked when the checkbox state changes.
 * @param text The text to be displayed above the checkbox.
 */
@Composable
@Preview
fun LabelledCheckBox(
    modifier: Modifier = Modifier,
    value: Boolean = false,
    onCheck: (Boolean) -> Unit = {},
    text: String = "Label",
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = 4.dp),
            text = text,
        )
        Checkbox(
            modifier = Modifier.padding(0.dp),
            checked = value,
            onCheckedChange = onCheck
        )
    }
}

/**
 * Displays a dialog for renaming a meal.
 *
 * @param onDismissRequest The function that is invoked when the dialog is dismissed.
 * @param onConfirmation The function that is invoked when the user confirms the action.
 * @param initialName A string that represents the current name of the meal.
 */
@Composable
@Preview
fun RenameMealDialog(
    onDismissRequest: () -> Unit = { },
    onConfirmation: (String) -> Unit = { },
    initialName: String = "Meal Name",
) {
    var name by rememberSaveable { mutableStateOf(initialName) }

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Card(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            RenameForm(
                onValueChange = {
                    name = it
                },
                titleText = stringResource(R.string.rename_meal) + " " + initialName,
                name = name,
            )
            DialogButtons(
                onDismissRequest = onDismissRequest,
                onConfirmation = { onConfirmation(name) }
            )
        }
    }
}

/**
 * Displays a dialog for editing a dish's name and rating.

 * @param onDismissRequest The function that is invoked when the dialog is dismissed.
 * @param onConfirmation The function that is invoked when the user confirms the action.
 * @param initialName The initial name of the dish.
 * @param initialRating The initial rating of the dish.
 * @param saved A boolean indicating whether the dish is saved. If true, the rename form is displayed.
 */
@Composable
@Preview
fun EditDishDialog(
    onDismissRequest: () -> Unit = { },
    onConfirmation: (newName: String, newRating: Rating) -> Unit = { _, _ -> },
    initialName: String = "Dish Name",
    initialRating: Rating = Rating.LIKEIT,
    saved: Boolean = true,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var rating by rememberSaveable { mutableStateOf(initialRating) }

    Dialog(
        onDismissRequest = onDismissRequest,
    ) {
        Card(
            modifier =
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            if (saved){
                RenameForm(
                    onValueChange = {
                        name = it
                    },
                    titleText = stringResource(id = R.string.edit_dish) + " " + initialName,
                    name = name
                )
            }
            RatingSelector(
                modifier = Modifier.fillMaxWidth(),
                onRatingChange = { rating = it },
                rating = rating
            )
            DialogButtons(
                onDismissRequest = onDismissRequest,
                onConfirmation = { onConfirmation(name, rating) }
            )
        }
    }
}

/**
 * Displays a row of two buttons, "Cancel" and "Confirm".
 * The buttons are centered and take up the full width of the parent.

 * @param onDismissRequest The function that is invoked when the "Cancel" button is clicked.
 * @param onConfirmation The function that is invoked when the "Confirm" button is clicked.
 */
@Composable
private fun DialogButtons(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
) {
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
            onClick = onConfirmation,
            modifier = Modifier
                .padding(8.dp)
        ) {
            Text("Confirm")
        }
    }
}

/**
 * Displays a form for renaming an item.
 *
 * @param titleText The resource ID of the title string to be displayed above the TextField.
 * @param name The initial value of the TextField.
 * @param onValueChange The function that is invoked when the value of the TextField changes.
 */
@Composable
fun RenameForm(
    titleText: String = stringResource(id = R.string.rename),
    name: String = "Default Name",
    onValueChange: (String) -> Unit = {},
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
            value = name,
            onValueChange = onValueChange,
        )
    }
}

/**
 * Displays the main body of the MealDetail screen.
 * It includes fields for meal name, dishes, occasion, rating, and image.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param mealDetailUiState The UI state of the MealDetail screen.
 * @param onMealRatingChanged The function that is invoked when the meal rating changes.
 * @param onMealInstanceDetailsChange The function that is invoked when the meal instance details change.
 * @param onMealNameChanged The function that is invoked when the meal name changes.
 * @param onDishNameChanged The function that is invoked when the dish name changes.
 * @param mealSearchResults A list of meal search results.
 * @param dishSearchResults A list of dish search results.
 * @param onDeleteDishClick The function that is invoked when a dish is deleted.
 * @param onRestoreDishClick The function that is invoked when a dish is restored.
 * @param onFindExistingMeal The function that is invoked when an existing meal is found.
 * @param onFindExistingDish The function that is invoked when an existing dish is found.
 * @param onDishAdded The function that is invoked when a dish is added.
 * @param onMealSearchTermChanged The function that is invoked when the meal search term changes.
 * @param onDishSearchTermChanged The function that is invoked when the dish search term changes.
 * @param onMealEditClick The function that is invoked when the meal edit button is clicked.
 * @param onDishEditClick The function that is invoked when the dish edit button is clicked.
 * @param onUpdateImage The function that is invoked when the image is updated.
 * @param isDuplicateCheck The function that checks if a dish is a duplicate.
 * @param isDishSaved The function that checks if a dish is saved.
 */
@Composable
fun MealDetailBody(
    modifier: Modifier = Modifier,
    mealDetailUiState: MealDetailUiState = MealDetailUiState(),
    onMealRatingChanged: (Rating) -> Unit = { _: Rating -> },
    onMealInstanceDetailsChange: (MealInstanceDetails) -> Unit = {},
    onMealNameChanged: (String) -> Unit = {},
    onDishNameChanged: (Int, String) -> Unit = { _: Int, _: String -> },
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
    isDishSaved: (DishDetails) -> Boolean = { false },
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
            onNameChange = onDishNameChanged,
            dishes = mealInstanceDetails.mealDetails.dishes,
            onDishAdded = onDishAdded,
            onDishClick = onFindExistingDish,
            searchResults = dishSearchResults,
            onDeleteDishClick = onDeleteDishClick,
            onRestoreDishClick = onRestoreDishClick,
            onDishSearchTermChanged = onDishSearchTermChanged,
            onDishEditClick = onDishEditClick,
            isDuplicate = isDuplicateCheck,
            isDishSaved = isDishSaved,
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
            onRatingChange = onMealRatingChanged,
            rating = mealInstanceDetails.mealDetails.rating,
        )
        ImageField(
            modifier = Modifier,
            image = mealInstanceDetails.mealDetails.imgSrc ?: "",
            onUpdateImage = onUpdateImage,
        )
    }
}

/**
 * Saves an image to the internal storage of the device.
 *
 * The image is saved with a unique filename generated using a UUID, and the ".jpg" extension.
 *
 * @param context The context in which the function is called.
 * @param uri The Uri of the image to be saved.
 * @return The filename of the saved image.
 */
fun saveImageToInternalStorage(context: Context, uri: Uri): String {
    val fileName = UUID.randomUUID().toString() + ".jpg"
    val inputStream = context.contentResolver.openInputStream(uri)
    val outputStream = context.openFileOutput(fileName, Context.MODE_PRIVATE)
    inputStream?.use { input ->
        outputStream.use { output ->
            input.copyTo(output)
        }
    }

    return fileName
}

/**
 * Displays an image field.
 *
 * If an image is provided, it displays the image with a "Remove Image" button.
 * If no image is provided, it displays a box with a "Add Image" button.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param image The filename of the image to be displayed. Default is an empty string.
 * @param onUpdateImage The function that is invoked when the image is updated. It takes the filename of the new image as a parameter.
 */
@Composable
@Preview
fun ImageField(
    modifier: Modifier = Modifier,
    image: String = "",
    onUpdateImage: (String) -> Unit = {},
) {
    val context = LocalContext.current
    val pickImageContract = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
            if (uri != null) {
                onUpdateImage(saveImageToInternalStorage(context = context, uri = uri))
            }
    }

    if (image.isNotBlank()) { // Display the image
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(top = dimensionResource(id = R.dimen.padding_small)),
        ){
            Button(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    deleteImage(context, image)
                    onUpdateImage("")
                },
            ) {
                Text(text = "Remove Image")
            }
        }
        AsyncImage(
            modifier = modifier
                .clip(
                    RoundedCornerShape(
                        corner = CornerSize(dimensionResource(id = R.dimen.padding_small))
                    )
                )
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium))
                .clickable {
                    if (isPhotoPickerAvailable(context)) {
                        pickImageContract.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    } else {
                        Toast
                            .makeText(
                                context,
                                "Not supported on this device",
                                Toast.LENGTH_LONG
                            )
                            .show()
                    }
                },
            model = ImageRequest.Builder(context)
                .data(context.getFileStreamPath(image))
                .crossfade(true)
                .build(),
            contentScale = ContentScale.Crop,
            contentDescription = "Meal image",
        )
    }
    else { // Display any empty box
        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(dimensionResource(id = R.dimen.padding_medium))
                .height(200.dp)
                .clickable {
                    if (isPhotoPickerAvailable(context)) {
                        pickImageContract.launch(
                            PickVisualMediaRequest(
                                mediaType = ActivityResultContracts.PickVisualMedia.ImageOnly
                            )
                        )
                    } else {
                        Toast
                            .makeText(
                                context,
                                "Not supported on this device",
                                Toast.LENGTH_LONG
                            )
                            .show()
                    }
                }
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
        {// Add image button
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

/**
 * Deletes an image from the internal storage of the device.
 * If the file does not exist, no action is performed.
 *
 * @param context The context in which the function is called. This is used to access the file output stream.
 * @param image The filename of the image to be deleted.
 */
private fun deleteImage(context: Context, image: String) {
    val file = context.getFileStreamPath(image)
    if (file.exists()) {
        file.delete()
    }
}

/**
 * Displays a Save button.
 *
 * @param onSaveClick The function that is invoked when the button is clicked.
 * @param enabled A boolean that determines whether the button is enabled or disabled.
 */
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

/**
 * Displays a search bar that allows the user to search for a meal by name.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param mealDetails The details of the meal to be displayed in the MealField.
 * @param onNameChange The function that is invoked when the name of the meal changes.
 * @param searchResults A list of meal search results.
 * @param onMealClick The function that is invoked when a meal is clicked.
 * @param onSearchTermChanged The function that is invoked when the search term changes.
 * @param onEditClick The function that is invoked when the edit icon is clicked.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
@Preview
fun MealField(
    modifier: Modifier = Modifier,
    mealDetails: MealDetails = MealDetails(name = "mealName"),
    onNameChange: (newName: String) -> Unit = {},
    searchResults: List<MealDetails> = emptyList(),
    onMealClick: (mealName: String) -> Unit = {},
    onSearchTermChanged: (newSearchTerm: String) -> Unit = {},
    onEditClick: () -> Unit = {},
) {
    val keyboardVisibleState by rememberUpdatedState(WindowInsets.isImeVisible)
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

    LaunchedEffect(key1 = keyboardVisibleState) {
        if (!keyboardVisibleState) active = false
    }

    Column(modifier = modifier.fillMaxWidth()) {
        FieldHeader( // Title
            modifier = modifier,
            string = stringResource(id = R.string.meal_header),
            style = MaterialTheme.typography.titleLarge,
        )

        Box(
            modifier = Modifier
                .height(animatedHeight)
                .padding(vertical = dimensionResource(id = paddingId)),
        ) {
            DockedSearchBar( // Meal search bar
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
                    active = false
                },
                placeholder = {
                    Text(
                        text = mealDetails.name.ifBlank { stringResource(R.string.meal_name_req) }
                    )
                },
                trailingIcon = { // Edit Button
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
                content = { // Meal search results
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
                    if
                        (active) onMealClick(mealDetails.name)
                    else
                        onSearchTermChanged(mealDetails.name)
                    active = it
                },
            )
        }
    }
}

@Composable
fun FieldHeader(
    modifier: Modifier = Modifier,
    string: String = "Meal",
    style: TextStyle = MaterialTheme.typography.titleLarge
) {
    Text(
        modifier = modifier.fillMaxWidth(),
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

/**
 * Displays all dish-relevant content.
 *
 * The section includes a checkbox for toggling between the default and alternative format.
 * In the default format, all dishes are displayed together.
 * In the alternative format, dishes are grouped by their rating.
 *
 * Each group of dishes is preceded by a header that displays the description of the rating.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param dishes The dishes to be displayed.
 * @param onNameChange The function that is invoked when the name of a dish changes. It takes the index of the dish and the new name as parameters.
 * @param onDishAdded The function that is invoked when a dish is added. It takes the details of the new dish as a parameter.
 * @param searchResults A list of dish search results.
 * @param onDeleteDishClick The function that is invoked when a dish is deleted. It takes the index of the dish as a parameter and returns a Boolean indicating whether the deletion was successful.
 * @param onRestoreDishClick The function that is invoked when a dish is restored. It takes the details of the dish as a parameter.
 * @param onDishClick The function that is invoked when a dish is clicked. It takes the index of the dish and the name of the dish as parameters.
 * @param onDishSearchTermChanged The function that is invoked when the dish search term changes. It takes the new search term as a parameter.
 * @param onDishEditClick The function that is invoked when the dish edit button is clicked. It takes the index of the dish as a parameter.
 * @param isDuplicate The function that checks if a dish is a duplicate. It takes the index of the dish as a parameter and returns a Boolean indicating whether the dish is a duplicate.
 * @param isDishSaved The function that checks if a dish is saved. It takes the details of the dish as a parameter and returns a Boolean indicating whether the dish is saved.
 */
@Composable
@Preview
fun DishesFields(
    modifier: Modifier = Modifier,
    dishes: List<DishDetails> = listOf(
        DishDetails(name = "dish1"),
        DishDetails(name = "dish2"),
    ),
    onNameChange: (index: Int, newName: String) -> Unit = { _: Int, _: String -> },
    onDishAdded: (dishToAdd: DishDetails) -> Unit = {},
    searchResults: List<DishDetails> = listOf(),
    onDeleteDishClick: (index: Int) -> Boolean = { _ -> false },
    onRestoreDishClick: (dishToRestore: DishDetails) -> Unit = {},
    onDishClick: (index: Int, String) -> Unit = { _: Int, _: String -> },
    onDishSearchTermChanged: (newSearchTerm: String) -> Unit = {},
    onDishEditClick: (index: Int) -> Unit = {},
    isDuplicate: (index: Int) -> Boolean = { _ -> false },
    isDishSaved: (dishToCheck:DishDetails) -> Boolean = { false },
) {
    var altFormat by remember { mutableStateOf(false) }

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
            LabelledCheckBox(
                modifier = Modifier
                    .padding(
                        vertical = 0.dp,
                        horizontal = dimensionResource(id = R.dimen.padding_medium)
                    ),
                value = altFormat,
                onCheck = { altFormat = it },
                text = "Love/Like/Learn",
            )
        }
    }

    Column {
        if (altFormat) {
            Rating.entries.forEach { rating ->
                FieldHeader(
                    modifier = Modifier.fillMaxWidth(),
                    string = rating.ratingEmoji.description,
                    style = MaterialTheme.typography.titleMedium,
                )
                DishFieldSection(
                    modifier = Modifier,
                    dishes = dishes,
                    onNameChange = onNameChange,
                    onDishClick = onDishClick,
                    searchResults = searchResults.filter { it.rating == rating },
                    isDuplicate = isDuplicate,
                    onDeleteDishClick = onDeleteDishClick,
                    onRestoreDishClick = onRestoreDishClick,
                    onDishEditClick = onDishEditClick,
                    onDishSearchTermChanged = onDishSearchTermChanged,
                    onDishAdded = onDishAdded,
                    isDishSaved = isDishSaved,
                    defaultRating = rating,
                    filterByDefaultRating = true,
                )
            }
        } else {
            DishFieldSection(
                modifier = Modifier,
                dishes = dishes,
                onNameChange = onNameChange,
                onDishClick = onDishClick,
                searchResults = searchResults,
                isDuplicate = isDuplicate,
                onDeleteDishClick = onDeleteDishClick,
                onRestoreDishClick = onRestoreDishClick,
                onDishEditClick = onDishEditClick,
                onDishSearchTermChanged = onDishSearchTermChanged,
                onDishAdded = onDishAdded,
                isDishSaved = isDishSaved,
                defaultRating = Rating.LIKEIT,
            )
        }
    }
}

/**
 * Displays a DishField for every dish in the list.
 *
 * If the filterByDefaultRating flag is set to true, only dishes with the default rating are shown.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param dishes List of DishDetails objects representing the dishes to be displayed.
 * @param onNameChange The function that is invoked when the name of a dish changes.
 * @param onDishClick The function that is invoked when a dish is clicked.
 * @param searchResults A list of DishDetails objects representing the search results.
 * @param isDuplicate The function that checks if a dish is a duplicate.
 * @param onDeleteDishClick The function that is invoked when a dish is deleted.
 * @param onRestoreDishClick The function that is invoked when a dish is restored.
 * @param onDishEditClick The function that is invoked when the dish edit button is clicked.
 * @param onDishSearchTermChanged The function that is invoked when the dish search term changes.
 * @param onDishAdded The function that is invoked when a dish is added.
 * @param isDishSaved The function that checks if a dish is saved.
 * @param defaultRating The default rating to be used when filtering dishes by rating.
 * @param filterByDefaultRating A Boolean that determines whether dishes should be filtered by the default rating.
 */
@Composable
private fun DishFieldSection(
    modifier: Modifier = Modifier,
    dishes: List<DishDetails>,
    onNameChange: (Int, String) -> Unit,
    onDishClick: (Int, String) -> Unit,
    searchResults: List<DishDetails>,
    isDuplicate: (Int) -> Boolean,
    onDeleteDishClick: (Int) -> Boolean,
    onRestoreDishClick: (DishDetails) -> Unit,
    onDishEditClick: (Int) -> Unit,
    onDishSearchTermChanged: (String) -> Unit,
    onDishAdded: (DishDetails) -> Unit,
    isDishSaved: (DishDetails) -> Boolean = { false },
    defaultRating: Rating = Rating.LIKEIT,
    filterByDefaultRating: Boolean = false,
) {
    dishes.forEachIndexed { index, dish ->
        if (filterByDefaultRating && dish.rating != defaultRating) return@forEachIndexed
        DishField(
            modifier = modifier,
            onNameChange = { onNameChange(index, it) },
            onDishClick = { onDishClick(index, it) },
            dishDetails = dish,
            searchResults = if (filterByDefaultRating) searchResults.filter {
                it.rating == dish.rating
            }
            else searchResults ,
            valid = !isDuplicate(index),
            onDeleteDishClick = { onDeleteDishClick(index) },
            onRestoreDishClick = onRestoreDishClick,
            onDishEditClick = { onDishEditClick(index) },
            onDishSearchTermChanged = onDishSearchTermChanged,
            isDishSaved = isDishSaved,
        )
    }
    AddDishButton(
        modifier = Modifier
            .fillMaxWidth()
            .padding(dimensionResource(id = R.dimen.padding_medium)),
        onClick = {
            onDishAdded(DishDetails(dishId = 0, name = "", rating = defaultRating))
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

/**
 * Displays a UI component for displaying and managing a dish and searching for other dishes.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param onNameChange The function that is invoked when the name of the dish changes.
 * @param onDishEditClick The function that is invoked when the dish edit icon is clicked.
 * @param onDishClick The function that is invoked when a dish is clicked.
 * @param dishDetails The details of the dish to be displayed in the DishField.
 * @param valid A boolean indicating whether the dish is valid.
 * @param searchResults A list of dish search results.
 * @param onDeleteDishClick The function that is invoked when the dish delete icon is clicked.
 * @param onRestoreDishClick The function that is invoked when a deleted dish is restored.
 * @param onDishSearchTermChanged The function that is invoked when the dish search term changes.
 * @param isDishSaved The function that checks if a dish is saved.
 */
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
    isDishSaved: (DishDetails) -> Boolean = { false },
) {
    val keyboardVisibleState by rememberUpdatedState(WindowInsets.isImeVisible)
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
    LaunchedEffect(key1 = keyboardVisibleState) {
        if (!keyboardVisibleState) active = false
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
                trailingIcon = { // Edit Button
                    if (dishDetails.name.isNotBlank() && !deleted) {
                        Icon(
                            modifier = Modifier.clickable { onDishEditClick() },
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit name of dish $dishDetails.name Button",
                        )
                    }
                },
                leadingIcon = { // Delete/Restore button
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
                content = { // Dish search results
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
                    true -> {
                        if (!isDishSaved(dishDetails)) {
                            SearchBarDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                dividerColor = MaterialTheme.colorScheme.secondaryContainer,
                                inputFieldColors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor =
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    unfocusedContainerColor =
                                        MaterialTheme.colorScheme.secondaryContainer,
                                    disabledContainerColor =
                                        MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            )
                        } else {
                            SearchBarDefaults.colors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                dividerColor = MaterialTheme.colorScheme.tertiaryContainer,
                                inputFieldColors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor =
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                    unfocusedContainerColor =
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                    disabledContainerColor =
                                        MaterialTheme.colorScheme.tertiaryContainer,
                                ),
                            )
                        }
                    }
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
                    if (active)
                        onDishClick(dishDetails.name)
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
    onDishClick: () -> Unit,
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

/**
 * Displays a UI component for viewing the current rating and selecting a new rating.
 *
 * @param modifier Modifier to be applied to the layout.
 * @param onRatingChange A lambda function that is invoked when a rating is selected. It takes the selected rating as a parameter.
 * @param rating The current rating.
 */
@Composable
fun RatingSelector(
    modifier: Modifier,
    onRatingChange: (Rating) -> Unit,
    rating: Rating,
) {
    Row(
        modifier = modifier
            .padding(dimensionResource(id = R.dimen.padding_medium)),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
    ) {
        Rating.entries.map { value ->
            RatingSelectorItem(
                modifier = Modifier.weight(1f),
                onClick = { onRatingChange(value) },
                ratingEmoji = value.ratingEmoji,
                selected = value == rating,
            )
        }
    }
}

@Composable
fun RatingSelectorItem(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    ratingEmoji: RatingEmoji,
    selected: Boolean = false,
) {
    Row (
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top
    ){
        Column(
            modifier = Modifier
                .align(Alignment.CenterVertically)
                .clickable(onClick = onClick),
        ) {
            Text(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally),
                text = ratingEmoji.emojiString,
                fontSize = when (selected) {
                    true -> 32.sp
                    false -> 18.sp
                },
            )
            Text(
                modifier = Modifier,
                text = ratingEmoji.description,
                textAlign = TextAlign.Center,
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