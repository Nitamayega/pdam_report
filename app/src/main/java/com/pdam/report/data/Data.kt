package com.pdam.report.data

data class UserData (
    val username: String = "",
    val team: Int = 0,
    val listCustomer: HashMap<String, DataCustomer> = hashMapOf()
)

data class DataCustomer(
    val currentDate: String = "",
    val petugas: String = "",
    val jenisPekerjaan: String = "",
    val PW: Int = 0,
    val nomorRegistrasi: String = "",
    val nomorKL: String = "",
    val name: String = "",
    val address: String = "",
    val merkMeter: String = "",
    val diameterMeter: String = "",
    val standMeter: String = "",
    val nomorMeter: String = "",
    val nomorSegel: String = "",
    val xKoordinat: String = "",
    val yKoordinat: String = "",
    val zKooridnat: String = "",
    val keterangan: String = "",
    val dokumentasi1: String = "",
    val dokumentasi2: String = "",
    val dokumentasi3: String = "",
    val dokumentasi4: String = "",
    val status: Boolean = false,
)

data class PresenceData(
    val currentDate: String = "",
    val username: String = "",
    val location: String = "",
    val photoUrl: String = "",
)
