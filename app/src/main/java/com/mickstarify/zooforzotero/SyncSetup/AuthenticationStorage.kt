package com.mickstarify.zooforzotero.SyncSetup

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit

class AuthenticationStorage(context: Context) {
    private var sharedPreferences: SharedPreferences =
        context.getSharedPreferences("credentials", Context.MODE_PRIVATE)

    fun hasCredentials(): Boolean {
        return (sharedPreferences.contains("userkey") && sharedPreferences.getString(
            "userkey",
            ""
        ) != "")
    }

    fun getUsername(): String {
        return sharedPreferences.getString("username", "error")!!
    }

    fun setCredentials(username: String, userID: String, userkey: String) {
        sharedPreferences.edit {
            putString("username", username)
            putString("userID", userID)
            putString("userkey", userkey)
        }
    }

    fun getUserKey(): String {
        return sharedPreferences.getString("userkey", "error")!!
    }

    fun getUserID(): String {
        return sharedPreferences.getString("userID", "error")!!
    }

    fun destroyCredentials() {
        val editor = sharedPreferences.edit()
        editor.remove("username")
        editor.remove("userID")
        editor.remove("userkey")
        editor.apply()
    }
}