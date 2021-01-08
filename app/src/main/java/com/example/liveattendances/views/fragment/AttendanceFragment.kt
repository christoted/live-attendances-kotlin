package com.example.liveattendances.views.fragment

import android.app.Activity.RESULT_OK
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.provider.MediaStore
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.example.liveattendances.BuildConfig
import com.example.liveattendances.MyDate
import com.example.liveattendances.R
import com.example.liveattendances.databinding.BottomSheetAttendanceBinding
import com.example.liveattendances.databinding.FragmentAttendanceBinding
import com.example.liveattendances.dialog.MyDialog
import com.example.liveattendances.hawkstorage.HawkStorage
import com.example.liveattendances.model.AttendanceResponse
import com.example.liveattendances.model.HistoryResponse
import com.example.liveattendances.networking.ApiServices
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.material.bottomsheet.BottomSheetBehavior
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.jetbrains.anko.toast
import retrofit2.Call
import retrofit2.Response
import java.io.File
import java.io.IOException
import java.lang.StringBuilder
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashMap


class AttendanceFragment : Fragment(), OnMapReadyCallback {

    companion object {
        private const val REQUEST_CODE_MAP_PERMISSIONS = 1000
        private const val REQUEST_CODE_LOCATION = 2000
        private const val REQUEST_CODE_CAMERA_PERMISSION = 3000
        private const val REQUEST_CODE_CAMERA_CAPTURE = 1500
        private val TAG = AttendanceFragment::class.java.simpleName
    }

    private val mapPermission = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )
    private val cameraPermission = arrayOf(
        android.Manifest.permission.CAMERA,
        android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    )


    //Map Config
    private var locationManager: LocationManager? = null
    private var locationRequest: LocationRequest? = null
    private var locationSettingsRequest: LocationSettingsRequest? = null
    private var settingsClient: SettingsClient? = null
    private var currentLocation: Location? = null
    private var locationCallback: LocationCallback? = null
    private var mapAttedance: SupportMapFragment? = null
    private var map: GoogleMap? = null
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null

    //UI
    private var binding: FragmentAttendanceBinding? = null
    private var bindingBottomSheet: BottomSheetAttendanceBinding? = null
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<ConstraintLayout>

    //Photo
    private var currentPhotoPath: String? = null

    private var isCheckIn = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        bindingBottomSheet = binding?.layoutBottomSheet
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMaps()
        init()
        onClick()
        checkIfAlreadyPresent()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CODE_CAMERA_CAPTURE) {
            if (resultCode == RESULT_OK) {
                if (currentPhotoPath!!.isNotEmpty()) {
                    val uri = Uri.parse(currentPhotoPath)
                    bindingBottomSheet?.ivCapturePhoto?.setImageURI(uri)
                    bindingBottomSheet?.ivCapturePhoto?.adjustViewBounds = true

                }
            } else {
                if (currentPhotoPath!!.isNotEmpty()) {
                    val file = File(currentPhotoPath)
                    file.delete()
                    currentPhotoPath = ""
                    Toast.makeText(context, "Failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun onClick() {
        binding?.fabGetCurrentLocation?.setOnClickListener {
            goToCurrentLocation()
        }

        bindingBottomSheet?.ivCapturePhoto?.setOnClickListener {
            if (checkPermissionCamera()) {
                openCamera()
            } else {
                setRequestPermissionCamera()
            }
        }

        bindingBottomSheet?.btnCheckIn?.setOnClickListener {
            val token = HawkStorage.instance(context).getToken()
            if (checkValidation()) {
                if (isCheckIn) {
                    AlertDialog.Builder(context)
                        .setTitle("Are your sure?")
                        .setPositiveButton("Yes") { _, _ ->
                            sendDataAttendance(token, "out")
                        }.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                        .show()
                } else {
                    AlertDialog.Builder(context)
                        .setTitle("Are your sure?")
                        .setPositiveButton("Yes") { _, _ ->
                            sendDataAttendance(token, "in")
                        }.setNegativeButton("No") { dialog, _ -> dialog.dismiss() }
                        .show()
                }
            }
        }
    }

    private fun sendDataAttendance(token: String, type: String) {
        val params = HashMap<String, RequestBody>()

        if (currentLocation != null && currentPhotoPath!!.isNotEmpty()) {
            val latitude = currentLocation?.latitude.toString()
            val longitude = currentLocation?.longitude.toString()
            val address = bindingBottomSheet?.tvCurrentLocation.toString()

            val file = File(currentPhotoPath)
            val uri = FileProvider.getUriForFile(
                context!!,
                BuildConfig.APPLICATION_ID + ".fileprovider", file
            )
            val typeFile = context?.contentResolver?.getType(uri)

            val mediaTypeText = MultipartBody.FORM
            val mediaTypeFile = typeFile?.toMediaType()

            val requestLatitude = latitude.toRequestBody(mediaTypeText)
            val requestLongitude = longitude.toRequestBody(mediaTypeText)
            val requestAddress = address.toRequestBody(mediaTypeText)
            val requestType = type.toRequestBody(mediaTypeText)

            params["lat"] = requestLatitude
            params["long"] = requestLongitude
            params["address"] = requestAddress
            params["type"] = requestType

            val requestPhotoFile = file.asRequestBody(mediaTypeFile)
            val multipartBody = MultipartBody.Part.createFormData("photo",file.name, requestPhotoFile)
            ApiServices.getLiveAttendanceServices().attend("Bearer $token", params, multipartBody)
                .enqueue(object : retrofit2.Callback<AttendanceResponse> {
                    override fun onFailure(call: Call<AttendanceResponse>, t: Throwable) {
                        MyDialog.hideDialog()
                    }

                    override fun onResponse(
                        call: Call<AttendanceResponse>,
                        response: Response<AttendanceResponse>
                    ) {
                        if ( response.isSuccessful) {
                            val attendanceResponse = response.body()
                            currentPhotoPath = ""
                            bindingBottomSheet?.ivCapturePhoto?.setImageDrawable(ContextCompat.getDrawable(context!!, R.drawable.ic_baseline_add_24 ))

                            bindingBottomSheet?.ivCapturePhoto?.adjustViewBounds = false

                            if ( type == "in") {
                                MyDialog.dynamicDialog(context, "Success Check in", attendanceResponse?.message.toString())
                            } else {
                                MyDialog.dynamicDialog(context, "Success Check out", attendanceResponse?.message.toString())
                            }

                            checkIfAlreadyPresent()
                        } else {
                            MyDialog.dynamicDialog(context, "Alert", "Something wrong")
                        }
                    }

                })
        }

    }

    private fun checkIfAlreadyPresent() {
        val token = HawkStorage.instance(context).getToken()
        val currentDate = MyDate.getCurrentDateForServer()

        ApiServices.getLiveAttendanceServices().getHistoryAttendance(token, currentDate, currentDate)
            .enqueue(object : retrofit2.Callback<HistoryResponse> {
                override fun onFailure(call: Call<HistoryResponse>, t: Throwable) {
                    TODO("Not yet implemented")
                }

                override fun onResponse(
                    call: Call<HistoryResponse>,
                    response: Response<HistoryResponse>
                ) {
                   if (response.isSuccessful) {
                       val histories = response.body()?.histories
                       if ( histories != null && histories.isNotEmpty()) {
                           if ( histories[0]?.status == 1) {
                               isCheckIn = false
                               checkIsCheckIn()
                               bindingBottomSheet?.btnCheckIn?.isEnabled = false
                               bindingBottomSheet?.btnCheckIn?.text = "You are already present"
                           } else {
                               isCheckIn = true
                               checkIsCheckIn()
                           }
                       }
                   }
                }

            })
    }

    private fun checkIsCheckIn() {
        if (isCheckIn) {
            bindingBottomSheet?.btnCheckIn?.text = "Check out"
        } else {
            bindingBottomSheet?.btnCheckIn?.text = "Check in"
        }
    }

    private fun checkValidation(): Boolean {
        if (currentPhotoPath!!.isEmpty()) {
            MyDialog.dynamicDialog(context, "Alert", "Please take your photo")
            return false
        }
        return true
    }

    private fun init() {

        //Setup location
        locationManager = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        settingsClient = LocationServices.getSettingsClient(context!!)
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(context!!)
        locationRequest = LocationRequest()
            .setInterval(10000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        //Setup bottomsheet
        bottomSheetBehavior = BottomSheetBehavior.from(bindingBottomSheet!!.bottomSheetAttendance)
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
        bindingBottomSheet = null
    }

    override fun onDestroy() {
        super.onDestroy()
        if (currentLocation != null && locationCallback != null) {
            fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        }
    }

    private fun setupMaps() {
        mapAttedance =
            childFragmentManager.findFragmentById(R.id.map_attendance) as SupportMapFragment
        mapAttedance?.getMapAsync(this)
    }


    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        if (checkPermission()) {
            val sydney = LatLng(-7.789302889219771, 110.36463041428438)
//            map!!.addMarker(
//                MarkerOptions()
//                    .position(sydney)
//                    .title("Marker in Sydney")
//            )
            map!!.moveCamera(CameraUpdateFactory.newLatLng(sydney))
            map!!.animateCamera(CameraUpdateFactory.zoomTo(20f))

            goToCurrentLocation()
        } else {
            setRequestPermission()
        }

    }


    private fun checkPermissionCamera(): Boolean {
        var isHasPermission = false
        context?.let {
            for (permission in cameraPermission) {
                isHasPermission = ActivityCompat.checkSelfPermission(
                    it,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }
        return isHasPermission
    }

    private fun setRequestPermissionCamera() {
        requestPermissions(cameraPermission, REQUEST_CODE_CAMERA_PERMISSION)
    }

    private fun checkPermission(): Boolean {
        var isHasPermission = false

        context?.let {
            for (permission in mapPermission) {
                isHasPermission = ActivityCompat.checkSelfPermission(
                    it,
                    permission
                ) == PackageManager.PERMISSION_GRANTED
            }
        }

        return isHasPermission
    }

    private fun setRequestPermission() {
        requestPermissions(mapPermission, REQUEST_CODE_MAP_PERMISSIONS)
    }

    private fun goToCurrentLocation() {

        bindingBottomSheet?.tvCurrentLocation?.text = getString(R.string.search_your_location)
        if (checkPermission()) {
            if (isLocationEnabled()) {
                map?.isMyLocationEnabled = true
                map?.uiSettings?.isMyLocationButtonEnabled = false

                locationCallback = object : LocationCallback() {
                    override fun onLocationResult(locationResult: LocationResult?) {
                        super.onLocationResult(locationResult)

                        currentLocation = locationResult?.lastLocation

                        if (currentLocation != null) {
                            var latitude = currentLocation?.latitude
                            var longitude = currentLocation?.longitude

                            if (latitude != null && longitude != null) {
                                val latLng = LatLng(latitude, longitude)
                                map!!.moveCamera(CameraUpdateFactory.newLatLng(latLng))
                                map!!.animateCamera(CameraUpdateFactory.zoomTo(20f))

                                val address = getAddress(latitude, longitude)
                                if (address != null && address.isNotEmpty()) {
                                    bindingBottomSheet?.tvCurrentLocation?.text = address
                                }

                            }
                        }
                    }

                }

                fusedLocationProviderClient?.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.myLooper()
                )
            } else {
                goToTurnOnGPS()
            }
        } else {
            setRequestPermission()
        }
    }

    private fun getAddress(latitude: Double, longitude: Double): String? {
        val result: String
        context?.let {
            val geocode = Geocoder(it, Locale.getDefault())
            val address = geocode.getFromLocation(latitude, longitude, 1)

            if (address.size > 0) {
                result = address[0].getAddressLine(0)
                return result
            }
        }

        return null
    }

    private fun goToTurnOnGPS() {
        settingsClient?.checkLocationSettings(locationSettingsRequest)
            ?.addOnSuccessListener {
                goToCurrentLocation()
            }?.addOnFailureListener {
                when ((it as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        try {
                            val resolvableApiException = it as ResolvableApiException
                            resolvableApiException.startResolutionForResult(
                                activity,
                                REQUEST_CODE_LOCATION
                            )
                        } catch (e: IntentSender.SendIntentException) {
                            e.printStackTrace()
                            Log.d(TAG, "Error ${e.message}")
                        }
                    }
                }
            }
    }

    private fun isLocationEnabled(): Boolean {
        if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER)!! || locationManager?.isProviderEnabled(
                LocationManager.NETWORK_PROVIDER
            )!!
        ) {
            return true
        }

        return false
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            REQUEST_CODE_MAP_PERMISSIONS -> {
                var isHasPermission = false
                var permissionNotGranted = StringBuilder()

                for (i in permissions.indices) {
                    isHasPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED

                    if (!isHasPermission) {
                        permissionNotGranted.append("${permissions[i]}")
                    }
                }

                if (isHasPermission) {
                    setupMaps()
                } else {
                    val message = permissionNotGranted.toString() + "Not Granted"
                    MyDialog.dynamicDialog(context, "Required Permission", message)
                }
            }

            REQUEST_CODE_CAMERA_PERMISSION -> {
                var isHasPermission = false
                var permissionGranted = StringBuilder()

                for (i in permissions.indices) {
                    isHasPermission = grantResults[i] == PackageManager.PERMISSION_GRANTED

                    if (!isHasPermission) {
                        permissionGranted.append("${permissions[i]}")
                    }
                }

                if (isHasPermission) {
                    openCamera()
                } else {
                    val message = permissionGranted.toString() + "Not Granted"
                    MyDialog.dynamicDialog(context, "Required Permission", message)
                }
            }
        }
    }

    private fun openCamera() {
        context?.let { context ->
            val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            //Save path to Server
            if (cameraIntent.resolveActivity(context.packageManager) != null) {
                val photoFile = try {
                    createImageFile()
                } catch (e: IOException) {
                    null
                }

                photoFile?.also {
                    val photoUri = FileProvider.getUriForFile(
                        context,
                        BuildConfig.APPLICATION_ID + ".fileprovider",
                        it
                    )
                    cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    startActivityForResult(cameraIntent, REQUEST_CODE_CAMERA_CAPTURE)
                }
            }
        }
    }

    @Throws(IOException::class)
    private fun createImageFile(): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val storageDir = context?.getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        return File.createTempFile(
            "JPEG_${timestamp}",
            ".jpg",
            storageDir
        ).apply {
            currentPhotoPath = absolutePath
        }
    }
}