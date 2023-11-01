package com.pdam.report.utils

import android.content.Context
import android.location.Address
import android.location.Geocoder
import android.util.Log
import com.google.android.gms.maps.model.LatLng
import java.io.IOException
import java.util.Locale

@Suppress("DEPRECATION")
class GeocoderHelper(private val context: Context) {

    // Fungsi untuk mendapatkan alamat dari LatLng
    fun getAddressFromLatLng(latLng: LatLng): String? {
        val geocoder = Geocoder(context, Locale.getDefault())
        try {
            val addresses: MutableList<Address>? = geocoder.getFromLocation(
                latLng.latitude,
                latLng.longitude,
                1
            )

            if (addresses != null) {
                if (addresses.isNotEmpty()) {
                    val address = addresses[0]
                    val addressParts = mutableListOf<String>()

                    // Menambahkan bagian-bagian alamat yang tersedia
                    address.thoroughfare?.let { addressParts.add(it) }
                    address.subAdminArea?.let { addressParts.add(it) }
                    address.adminArea?.let { addressParts.add(it) }
                    address.countryName?.let { addressParts.add(it) }

                    // Menggabungkan bagian-bagian alamat menjadi satu string
                    return addressParts.joinToString(", ")
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error getting address from LatLng: ${e.localizedMessage}")
        }
        return null
    }

    companion object {
        private const val TAG = "GeocoderHelper"
    }
}
