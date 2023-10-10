package com.pdam.report.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map


class UserPreference private constructor(private val dataStore: DataStore<Preferences>){
    fun getUser(): Flow<UserDataLogin> {
        return dataStore.data.map {
            UserDataLogin(
                it[USERNAME_KEY] ?: "",
                it[TEAM_KEY] ?: 0,
                it[IS_LOGIN_KEY] ?: false
            )
        }
    }

    suspend fun loginUser(username: String, team: Int) {
        dataStore.edit { preferences ->
            preferences[USERNAME_KEY] = username
            preferences[TEAM_KEY] = team
            preferences[IS_LOGIN_KEY] = true
        }
    }

    suspend fun logoutUser() {
        dataStore.edit { preferences ->
            preferences[IS_LOGIN_KEY] = false
            preferences[USERNAME_KEY] = ""
            preferences[TEAM_KEY] = 0
            preferences.clear()
        }
    }

    companion object {
        private val USERNAME_KEY = stringPreferencesKey("username")
        private val TEAM_KEY = intPreferencesKey("team")
        private val IS_LOGIN_KEY = booleanPreferencesKey("is_login")

        @Volatile
        private var INSTANCE: UserPreference? = null

        fun getInstance(dataStore: DataStore<Preferences>): UserPreference {
            return INSTANCE ?: synchronized(this) {
                val instance = UserPreference(dataStore)
                INSTANCE = instance
                instance
            }
        }
    }
}