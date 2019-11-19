package com.mickstarify.zooforzotero

import android.content.Context
import org.jetbrains.anko.defaultSharedPreferences

enum class SortMethod {
    TITLE,
    DATE,
    AUTHOR,
    DATE_ADDED
}

class PreferenceManager(context: Context) {
    val sharedPreferences = context.defaultSharedPreferences


    fun setSortMethod(method: String) {
        val sortMethod = stringToSortMethod(method)
        this.setSortMethod(sortMethod)
    }

    fun setSortMethod(method: SortMethod) {
        val methodString = when (method) {
            SortMethod.TITLE -> "TITLE"
            SortMethod.DATE -> "DATE"
            SortMethod.AUTHOR -> "AUTHOR"
            SortMethod.DATE_ADDED -> "DATE_ADDED"
        }
        val editor = sharedPreferences.edit()
        editor.putString("sort_method", methodString)
        editor.apply()
    }

    fun sortMethodToString(method: SortMethod): String {
        return when (method) {
            SortMethod.TITLE -> "TITLE"
            SortMethod.DATE -> "DATE"
            SortMethod.AUTHOR -> "AUTHOR"
            SortMethod.DATE_ADDED -> "DATE_ADDED"
        }
    }

    fun stringToSortMethod(methodString: String): SortMethod {
        return when (methodString) {
            "TITLE" -> SortMethod.TITLE
            "DATE" -> SortMethod.DATE
            "AUTHOR" -> SortMethod.AUTHOR
            "DATE_ADDED" -> SortMethod.DATE_ADDED
            else -> SortMethod.TITLE
        }
    }

    fun getSortMethod(): SortMethod {
        val methodString = sharedPreferences.getString("sort_method", "TITLE")

        return when (methodString) {
            "TITLE" -> SortMethod.TITLE
            "DATE" -> SortMethod.DATE
            "AUTHOR" -> SortMethod.AUTHOR
            "DATE_ADDED" -> SortMethod.DATE_ADDED
            else -> SortMethod.TITLE
        }
    }

    fun getIsShowingOnlyPdfs(): Boolean {
        return sharedPreferences.getBoolean("is_showing_only_with_pdfs", false)
    }

    fun getIsShowingOnlyNotes(): Boolean {
        return sharedPreferences.getBoolean("is_showing_only_with_notes", false)
    }

    fun setIsShowingOnlyPdfs(value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_showing_only_with_pdfs", value)
        editor.apply()
    }

    fun setIsShowingOnlyNotes(value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("is_showing_only_with_notes", value)
        editor.apply()
    }

    fun setWebDAVAuthentication(address: String, username: String, password: String) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("use_webdav", true)
        editor.putString("webdav_address", address)
        editor.putString("webdav_username", username)
        editor.putString("webdav_password", password)

        editor.apply()
    }

    fun destroyWebDAVAuthentication() {
        val editor = sharedPreferences.edit()
        editor.putBoolean("use_webdav", false)
        editor.putString("webdav_address", "")
        editor.putString("webdav_username", "")
        editor.putString("webdav_password", "")

        editor.apply()
    }

    fun getWebDAVUsername(): String {
        return sharedPreferences.getString("webdav_username", "") ?: ""
    }

    fun getWebDAVPassword(): String {
        return sharedPreferences.getString("webdav_password", "") ?: ""
    }

    fun getWebDAVAddress(): String {
        return sharedPreferences.getString("webdav_address", "") ?: ""
    }

    fun isWebDAVEnabled(): Boolean {
        return sharedPreferences.getBoolean("use_webdav", false)
    }

}