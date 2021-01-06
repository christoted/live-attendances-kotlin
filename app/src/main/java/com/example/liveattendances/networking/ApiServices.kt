package com.example.liveattendances.networking

object ApiServices {
    fun getLiveAttendanceServices() : LiveAttendanceApiServices {
        return RetrofitClient
            .getClient()
            .create(LiveAttendanceApiServices::class.java)
    }
}