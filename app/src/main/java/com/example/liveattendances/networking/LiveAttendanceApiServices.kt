package com.example.liveattendances.networking

import com.example.liveattendances.model.ForgotPasswordResponse
import com.example.liveattendances.model.LoginResponse
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface LiveAttendanceApiServices {
    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/login")
    fun loginRequest(@Body body: String) : Call<LoginResponse>


    @Headers("Accept: application/json", "Content-Type: application/json")
    @POST("auth/password/forgot")
    fun forgotRequest(@Body body: String) : Call<ForgotPasswordResponse>

}