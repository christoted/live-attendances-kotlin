package com.example.liveattendances.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import com.example.liveattendances.MainActivity
import com.example.liveattendances.R
import com.example.liveattendances.hawkstorage.HawkStorage
import com.example.liveattendances.views.login.LoginActivity
import org.jetbrains.anko.startActivity

class SplashActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        AfterDelayGoToLogin()
    }

    private fun AfterDelayGoToLogin() {
        // Handler synchonizasi dengan UI
        Handler(Looper.getMainLooper()).postDelayed({
            checkIsLogin()

            finishAffinity()
        }, 1200)
    }

    private fun checkIsLogin() {
        val isLogin = HawkStorage.instance(this).isLogin()
        if ( isLogin) {
            startActivity<MainActivity>()
        } else {
            startActivity<LoginActivity>()
        }
    }
}