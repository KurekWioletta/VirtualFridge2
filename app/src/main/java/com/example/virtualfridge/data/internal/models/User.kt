package com.example.virtualfridge.data.internal.models

data class User(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val familyName: String?,
    val accountConfirmed: Boolean
)