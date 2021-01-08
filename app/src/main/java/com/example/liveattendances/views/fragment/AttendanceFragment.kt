package com.example.liveattendances.views.fragment

import android.content.pm.PackageManager
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.app.ActivityCompat
import com.example.liveattendances.R
import com.example.liveattendances.databinding.FragmentAttendanceBinding
import com.example.liveattendances.dialog.MyDialog
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import java.lang.StringBuilder


class AttendanceFragment : Fragment(), OnMapReadyCallback  {


    companion object {
        private const val REQUEST_CODE_MAP_PERMISSIONS = 1000
    }

    private val mapPermission = arrayOf(
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )


    private var mapAttedance : SupportMapFragment ?= null
    private var map: GoogleMap ?= null

    private var binding: FragmentAttendanceBinding? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = FragmentAttendanceBinding.inflate(inflater, container, false)
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupMaps()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    private fun setupMaps() {
        mapAttedance = childFragmentManager.findFragmentById(R.id.map_attendance) as SupportMapFragment
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

    private fun setRequestPermission() {
        requestPermissions(mapPermission, REQUEST_CODE_MAP_PERMISSIONS)
    }

    private fun goToCurrentLocation() {
        TODO("Not yet implemented")
    }

    private fun checkPermission(): Boolean {
        var isHasPermission = false

        context?.let {
            for ( permission in mapPermission) {
                isHasPermission = ActivityCompat.checkSelfPermission(it, permission) == PackageManager.PERMISSION_GRANTED
            }
        }

        return isHasPermission
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when(requestCode) {
            REQUEST_CODE_MAP_PERMISSIONS -> {
                var isHasPermission = false
                var permissionNotGranted = StringBuilder()

                for ( i in permissions.indices) {
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
        }
    }
}