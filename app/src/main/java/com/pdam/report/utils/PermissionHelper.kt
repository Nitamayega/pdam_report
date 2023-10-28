package com.pdam.report.utils

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat.requestPermissions
import androidx.core.content.ContextCompat

object PermissionHelper {
    const val REQUEST_CAMERA_PERMISSION = 10
    const val REQUEST_LOCATION_PERMISSION = 20

    fun requestCameraPermission(activity: Activity) {
        if (!hasCameraPermission(activity)) {
            requestPermissions(
                activity,
                arrayOf(Manifest.permission.CAMERA),
                REQUEST_CAMERA_PERMISSION
            )
        }
    }

    fun requestLocationPermission(activity: Activity) {
        if (!hasLocationPermission(activity)) {
            requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                REQUEST_LOCATION_PERMISSION
            )
        }
    }

    fun hasCameraPermission(activity: Activity): Boolean {
        return ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun hasLocationPermission(activity: Activity): Boolean {
        return (ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
            activity,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED)
    }

    fun checkAndRequestPermissions(activity: Activity) {
        val cameraPermission = hasCameraPermission(activity)
        val locationPermission = hasLocationPermission(activity)

        if (!cameraPermission || !locationPermission) {
            val permissionsToRequest = mutableListOf<String>()
            if (!cameraPermission) {
                permissionsToRequest.add(Manifest.permission.CAMERA)
            }
            if (!locationPermission) {
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION)
            }

            val requestCode = if (!cameraPermission && !locationPermission) {
                30 // Atau nilai lain jika keduanya belum diizinkan
            } else if (!cameraPermission) {
                10 // Izin kamera belum diizinkan
            } else {
                20 // Izin lokasi belum diizinkan
            }

            requestPermissions(activity, permissionsToRequest.toTypedArray(), requestCode)
        } else {
            // Izin sudah diizinkan, lanjutkan dengan tindakan yang diinginkan
        }
    }

    fun handlePermissionResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        when (requestCode) {
            REQUEST_CAMERA_PERMISSION -> {
                // Handle camera permission result
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // Izin kamera diberikan
                    // Lakukan tindakan yang sesuai
                } else {
                    // Izin kamera ditolak
                    // Lakukan tindakan yang sesuai, seperti menampilkan pesan kepada pengguna
                }
            }
            REQUEST_LOCATION_PERMISSION -> {
                // Handle location permission result
                if (grantResults.size == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    // Izin lokasi diberikan
                    // Lakukan tindakan yang sesuai
                } else {
                    // Izin lokasi ditolak
                    // Lakukan tindakan yang sesuai, seperti menampilkan pesan kepada pengguna
                }
            }
        }
    }

}
