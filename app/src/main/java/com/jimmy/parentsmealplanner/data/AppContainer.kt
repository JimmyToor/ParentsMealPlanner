import android.content.Context
import com.jimmy.parentsmealplanner.data.LocalMealRepository
import com.jimmy.parentsmealplanner.data.MealRepository
import com.jimmy.parentsmealplanner.model.MealPlannerDatabase

/**
 * App container for Dependency injection.
 */
interface AppContainer {
    val mealRepository: MealRepository
}

/**
 * [AppContainer] implementation that provides instance of [LocalMealRepository]
 */
class AppDataContainer(private val context: Context) : AppContainer {

    override val mealRepository: MealRepository by lazy {
        LocalMealRepository(MealPlannerDatabase.getDatabase(context).mealDao())
    }
}
