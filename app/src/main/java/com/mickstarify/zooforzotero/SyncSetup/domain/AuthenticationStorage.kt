package com.mickstarify.zooforzotero.SyncSetup.domain

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

interface AuthenticationStorage {
    fun hasCredentials(): Boolean
    fun getUsername(): String
    fun getUserKey(): String
    fun getUserID(): String
    fun setCredentials(username: String, userID: String, userkey: String)
    fun destroyCredentials()
}

class AuthenticationStorageImpl(context: Context) : AuthenticationStorage {
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    override fun hasCredentials(): Boolean {
        return (sharedPreferences.contains("userkey") && sharedPreferences.getString(
            "userkey",
            ""
        ) != "")
    }

    override fun getUsername(): String {
        return sharedPreferences.getString("username", "error")!!
    }

    override fun setCredentials(username: String, userID: String, userkey: String) {
        sharedPreferences.edit {
            putString("username", username)
            putString("userID", userID)
            putString("userkey", userkey)
        }
    }

    override fun getUserKey(): String {
        return sharedPreferences.getString("userkey", "error")!!
    }

    override fun getUserID(): String {
        return sharedPreferences.getString("userID", "error")!!
    }

    override fun destroyCredentials() {
        sharedPreferences.edit {
            remove("username")
            remove("userID")
            remove("userkey")
        }
    }
}