package com.jimmy.parentsmealplanner.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.jimmy.parentsmealplanner.ui.shared.Occasion
import kotlinx.datetime.LocalDate

@Entity(
    tableName = "meal_instances",
    foreignKeys = [
        ForeignKey(
            entity = Meal::class,
            parentColumns = ["mealId"],
            childColumns = ["mealId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [
        androidx.room.Index(value = ["mealId"]),
        androidx.room.Index(value = ["userId"]),
    ]
)
data class MealInstance(
    @PrimaryKey(autoGenerate = true)
    var mealInstanceId: Long = 0,
    var mealId: Long = 0,
    var occasion: Occasion = Occasion.BREAKFAST,
    var date: LocalDate = LocalDate(1, 1, 1),
    var userId: Long = 0,
)

data class InstanceDetails(
    var mealInstanceId: Long,
    var occasion: Occasion,
    var userId: Long,
    var date: LocalDate,
)

/**
 * Converts a [MealInstance] to an [InstanceDetails]
 */
fun MealInstance.toInstanceDetails(): InstanceDetails =
    InstanceDetails(
        mealInstanceId = mealInstanceId,
        date = date,
        occasion = occasion,
        userId = userId,
    )