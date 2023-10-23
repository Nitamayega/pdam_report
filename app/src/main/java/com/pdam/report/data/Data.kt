package com.pdam.report.data

data class UserData (
    val username: String = "",
    val team: Int = 0,
    val dailyTeam: Int = 0,
    var lastPresence: String = "",
)

data class PresenceData(
    val currentDate: Long = 0,
    val username: String = "",
    val location: String = "",
    val photoUrl: String = "",
)

data class CustomerData(
    val firebaseKey: String = "",
    val currentDate: Long = 0,
    val updateVerifDate: Long = 0,
    val updateInstallDate: Long = 0,
    val petugas: String = "",
    val dailyTeam: Int = 0,
    val jenisPekerjaan: String = "",
    val pw: Int = 0,
    val nomorRegistrasi: String = "",
    val nomorKL: String = "",
    val name: String = "",
    val address: String = "",
    val merkMeter: String = "",
    val diameterMeter: String = "",
    val standMeter: String = "",
    val nomorMeter: String = "",
    val nomorSegel: String = "",
    val xkoordinat: String = "",
    val ykoordinat: String = "",
    val zkoordinat: String = "",
    val keterangan1: String = "",
    val keterangan2: String = "",
    val dokumentasi1: String = "",
    val dokumentasi2: String = "",
    val dokumentasi3: String = "",
    val dokumentasi4: String = "",
    val dokumentasi5: String = "",
    val data: Int = 0,
)
