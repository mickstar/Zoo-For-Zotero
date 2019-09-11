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


    fun setSortMethod(method: SortMethod) {
        val methodString = when (method) {
            SortMethod.TITLE -> "TITLE"
            SortMethod.DATE -> "DATE"
            SortMethod.AUTHOR -> "AUTHOR"
            SortMethod.DATE_ADDED -> "DATEADDED"
        }

        val editor = sharedPreferences.edit()
        editor.putString("sort_method", methodString)
        editor.apply()
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
}