package com.example.liveattendances.views.fragment

import android.app.AlertDialog
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.provider.Settings.ACTION_LOCALE_SETTINGS
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.example.liveattendances.BuildConfig
import com.example.liveattendances.MainActivity
import com.example.liveattendances.databinding.FragmentAttendanceBinding
import com.example.liveattendances.databinding.FragmentProfileBinding
import com.example.liveattendances.dialog.MyDialog
import com.example.liveattendances.hawkstorage.HawkStorage
import com.example.liveattendances.model.LogoutResponse
import com.example.liveattendances.networking.ApiServices
import com.example.liveattendances.views.ChangePasswordActivity
import com.example.liveattendances.views.login.LoginActivity
import org.jetbrains.anko.startActivity
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ProfileFragment : Fragment() {

    private var binding: FragmentProfileBinding? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        onClick()
        updateView()
    }

    private fun updateView() {
        val user = HawkStorage.instance(context).getUser()
        val imageUrl = BuildConfig.BASE_IMAGE_URL + user.photo
        Glide.with(context!!).load(imageUrl).into(binding!!.ivProfile)
        binding?.tvNameProfile?.text = user.name
        binding?.tvEmailProfile?.text = user.email
    }


    private fun onClick() {
        binding?.btnChangePassword?.setOnClickListener {
            context?.startActivity<ChangePasswordActivity>()
        }

        binding?.btnChangeLanguage?.setOnClickListener {
            startActivity(Intent(ACTION_LOCALE_SETTINGS))
        }

        binding?.btnLogout?.setOnClickListener {
            AlertDialog.Builder(context)
                .setTitle("Logout")
                .setMessage("Are Your sure?")
                .setPositiveButton("Yes") { dialog, _ ->
                    logoutRequest(dialog)
                }
                .setNegativeButton("No") { dialog, _ ->
                    dialog.dismiss()
                }
                .show()
        }
    }

    private fun logoutRequest(dialog: DialogInterface?) {
        val token = HawkStorage.instance(context).getToken()
        MyDialog.showProgressBar(context)
        ApiServices.getLiveAttendanceServices().logoutRequest(token)
            .enqueue(object : Callback<LogoutResponse> {
                override fun onFailure(call: Call<LogoutResponse>, t: Throwable) {
                    dialog?.dismiss()
                    MyDialog.hideDialog()
                    MyDialog.dynamicDialog(context, "Alert", "GAGAL ON Failure")
                }

                override fun onResponse(
                    call: Call<LogoutResponse>,
                    response: Response<LogoutResponse>
                ) {
                    dialog?.dismiss()
                    MyDialog.hideDialog()
                    if (response.isSuccessful) {
                        HawkStorage.instance(context).deleteAll()
                        (activity as MainActivity).finishAffinity()
                        context?.startActivity<LoginActivity>()
                    } else {
                        MyDialog.dynamicDialog(context, "Alert", "Reponse tapi gagal")
                    }
                }

            })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }
}