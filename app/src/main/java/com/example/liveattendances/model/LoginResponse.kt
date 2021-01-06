package com.example.liveattendances.model

data class LoginResponse(
    val user: User,
    val message: String,
    val meta: Meta
)