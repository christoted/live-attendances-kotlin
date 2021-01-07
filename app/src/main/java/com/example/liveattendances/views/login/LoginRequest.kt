package com.example.liveattendances.views.login

data class LoginRequest(
    val device_name: String,
    val email: String,
    val password: String
)