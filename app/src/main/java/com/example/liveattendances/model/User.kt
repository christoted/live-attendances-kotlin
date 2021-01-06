package com.example.liveattendances.model

data class User(
    val created_at: String,
    val email: String,
    val email_verified_at: Any,
    val id: Int,
    val is_admin: Int,
    val name: String,
    val photo: Any,
    val updated_at: String
)