package com.jimmy.parentsmealplanner.ui.shared

import com.jimmy.parentsmealplanner.model.User

data class UserDetails(
    val id: Long = 0,
    val name: String = "",
)

fun User.toUserDetails(): UserDetails = UserDetails(
    id = userId,
    name = name,
)

fun UserDetails.toUser(): User = User(
    userId = id,
    name = name,
)