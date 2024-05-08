package com.jimmy.parentsmealplanner.util

import android.content.Context
import android.widget.Toast

/**
 * This function checks the result of an operation and performs an action based on the result.
 * If the result is false, it shows a Toast message with an error message.
 * If the result is true, it performs a success action.
 *
 * @param result The result of the operation to check where true = success, and false = error.
 * @param context The context in which to show the Toast message.
 * @param onSuccess The function that is invoked when the result is true.
 * @param errorMessage The message to show in the Toast when the result is false.
 */
fun checkResult(
    result: Boolean,
    context: Context,
    onSuccess: () -> Unit,
    errorMessage: String,
) {
    if (!result) {
        Toast.makeText(
            context,
            errorMessage,
            Toast.LENGTH_SHORT
        ).show()
    } else {
        onSuccess()
    }
}
