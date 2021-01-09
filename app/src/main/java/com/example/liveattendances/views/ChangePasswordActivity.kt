package com.example.liveattendances.views

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.liveattendances.R
import com.example.liveattendances.databinding.ActivityChangePasswordBinding
import com.example.liveattendances.dialog.MyDialog
import com.example.liveattendances.hawkstorage.HawkStorage
import com.example.liveattendances.model.ChangePasswordResponse
import com.example.liveattendances.model.LoginResponse
import com.example.liveattendances.networking.ApiServices
import com.example.liveattendances.networking.RetrofitClient
import com.google.gson.Gson
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Converter
import retrofit2.Response
import java.io.IOException

class ChangePasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChangePasswordBinding

    companion object {
        private val TAG = ChangePasswordActivity::class.java.simpleName
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityChangePasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        init()
        onClick()
    }

    private fun onClick() {
        binding.tbChangePassword.setNavigationOnClickListener {
            finish()
        }

        binding.btnChangePassword.setOnClickListener {
            val oldPass = binding.etOldPassword.text.toString()
            val newPass = binding.etNewPassword.text.toString()
            val confPass = binding.etConfirmNewPassword.text.toString()

            if (checkValidation(oldPass, newPass, confPass)) {
                changePassToServer(oldPass, newPass, confPass)
            }
        }
    }

    private fun changePassToServer(oldPass: String, newPass: String, confPass: String) {
        val token = HawkStorage.instance(this).getToken()
        val changePasswordRequest = ChangePasswordRequest(oldPass, newPass, confPass)
        MyDialog.showProgressBar(this)
        val changePasswordRequestString = Gson().toJson(changePasswordRequest)
        ApiServices.getLiveAttendanceServices()
            .changePassword("Bearer $token", changePasswordRequestString)
            .enqueue(object : Callback<ChangePasswordResponse> {
                override fun onFailure(call: Call<ChangePasswordResponse>, t: Throwable) {
                    MyDialog.hideDialog()
                }

                override fun onResponse(
                    call: Call<ChangePasswordResponse>,
                    response: Response<ChangePasswordResponse>
                ) {
                    MyDialog.hideDialog()
                    if (response.isSuccessful) {
                        MyDialog.dynamicDialog(
                            this@ChangePasswordActivity,
                            "Success",
                            "Your password has been updated"
                        )

                        Handler(Looper.getMainLooper()).postDelayed({
                            MyDialog.hideDialog()
                            finish()
                        },2000)
                    } else{
                        val errorConverter: Converter<ResponseBody, ChangePasswordResponse> =
                            RetrofitClient
                                .getClient()
                                .responseBodyConverter(
                                    LoginResponse::class.java,
                                    arrayOfNulls<Annotation>(0)
                                )
                        var errorResponse: ChangePasswordResponse?
                        try {
                            response.errorBody()?.let {
                                errorResponse = errorConverter.convert(it)
                                MyDialog.dynamicDialog(this@ChangePasswordActivity, getString(R.string.failed), errorResponse?.message.toString())
                            }
                        }catch (e: IOException){
                            Log.e(TAG, "Error: ${e.message}")
                        }
                    }
                }


            })
    }

    private fun checkValidation(oldPass: String, newPass: String, confirmNewPass: String): Boolean {
        when {
            oldPass.isEmpty() -> {
                binding.etOldPassword.error = getString(R.string.please_field_your_password)
                binding.etOldPassword.requestFocus()
            }
            newPass.isEmpty() -> {
                binding.etNewPassword.error = getString(R.string.please_field_your_password)
                binding.etNewPassword.requestFocus()
            }
            confirmNewPass.isEmpty() -> {
                binding.etConfirmNewPassword.error = getString(R.string.please_field_your_password)
                binding.etConfirmNewPassword.requestFocus()
            }
            newPass != confirmNewPass -> {
                binding.etNewPassword.error = "Not Match"
                binding.etNewPassword.requestFocus()
                binding.etConfirmNewPassword.error = "Confirm password Not Matched"
                binding.etConfirmNewPassword.requestFocus()
            }
            else -> {
                binding.etNewPassword.error = null
                binding.etConfirmNewPassword.error = null
                return true
            }
        }
        return false
    }

    private fun init() {
        setSupportActionBar(binding.tbChangePassword)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
    }
}