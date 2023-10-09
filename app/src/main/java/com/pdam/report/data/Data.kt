package com.pdam.report.data

data class UserData (
    val username: String = "",
    val team: Int = 0,
    val listCustomer: HashMap<String, DataCustomer> = hashMapOf(),
    val listPresence: HashMap<String, DataPresence> = hashMapOf()
)

data class DataCustomer(
    val createdAt: Long = 0,
    val name: String = "",
    val address: String = "",
)

data class DataPresence(
    val currentDate: String = "",
    val location: String = "",
    val photoUrl: String = "",
)
