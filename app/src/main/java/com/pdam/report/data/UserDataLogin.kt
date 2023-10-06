package com.pdam.report.data

data class UserDataLogin(
    val id: Long = 0,
    val username: String = "",
    val team: Int = 0,
    val isLogin: Boolean = false,
)