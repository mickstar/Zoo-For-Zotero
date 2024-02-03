package com.mickstarify.zooforzotero

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import javax.inject.Inject


class PreferenceManager @Inject constructor(context: Context) {
    val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)

    fun setSortMethod(method: String) {
        val sortMethod = stringToSortMethod(method)
        this.setSortMethod(sortMethod)
    }

    fun setSortMethod(method: SortMethod) {
        val methodString = sortMethodToString(method)
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
        val methodString = sharedPreferences.getString("sort_method", "TITLE") ?: "TITLE"

        return stringToSortMethod(methodString)
    }

    fun isSortedAscendingly(): Boolean {
        /* We will define ascending as going from smallest to largest. As such, A-Z will be defined
        * as ascending and  Z-A will be descending.
        * Similarly, Ascending will refer to older dates to newer ones.
        * Ascending will be represented as an up arrow.*/
        val direction = sharedPreferences.getString("SORT_DIRECTION", "DEFAULT")
        return when (direction) {
            SORT_METHOD_ASCENDING -> true
            SORT_METHOD_DESCENDING -> false
            else -> true // ascending will be default.
        }
    }

    fun setSortDirection(direction: String) {
        if (direction != SORT_METHOD_DESCENDING && direction != SORT_METHOD_ASCENDING) {
            Log.e(
                "zotero",
                "got request to change sort method to $direction which is not understood"
            )
            return
        }
        val editor = sharedPreferences.edit()
        editor.putString("SORT_DIRECTION", direction)
        editor.apply()
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

    fun setWebDAVAuthentication(
        address: String,
        username: String,
        password: String,
    ) {
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
        editor.putBoolean("webdav_allowInsecureSSL", false)
        editor.putBoolean("webdav_verify_ssl", false)
        editor.putBoolean("webdav_add_zotero_to_url", false)
        editor.putString("webdav_auth_mode", "automatic")

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

    fun isWebDAVConfigured(): Boolean {
        return getWebDAVAddress() != ""
    }

    fun isWebDAVEnabled(): Boolean {
        return sharedPreferences.getBoolean("use_webdav", false)
    }

    fun setWebDAVEnabled(state: Boolean) {
        sharedPreferences.edit().apply {
            putBoolean("use_webdav", state)
            commit()
        }
    }

    fun getWebDAVAddZoteroToUrl(): Boolean {
        return sharedPreferences.getBoolean("webdav_add_zotero_to_url", true)
    }

    fun setWebDAVAddZoteroToUrl(flag: Boolean) {
        sharedPreferences.edit {
            this.putBoolean("webdav_add_zotero_to_url", flag)
            apply()
        }
    }

    fun getWebDAVAuthMode(): WebdavAuthMode {
        return when (sharedPreferences.getString("webdav_auth_mode", "automatic")) {
            "automatic" -> WebdavAuthMode.AUTOMATIC
            "basic" -> WebdavAuthMode.BASIC
            "digest" -> WebdavAuthMode.DIGEST
            else -> WebdavAuthMode.AUTOMATIC
        }
    }

    fun setWebDAVAuthMode(authMode: WebdavAuthMode) {
        sharedPreferences.edit {
            this.putString(
                "webdav_auth_mode", when (authMode) {
                    WebdavAuthMode.AUTOMATIC -> "automatic"
                    WebdavAuthMode.DIGEST -> "digest"
                    WebdavAuthMode.BASIC -> "basic"
                }
            )
            apply()
        }
    }

    fun getVerifySSLForWebDAV(): Boolean {
        var default = true

        // this is for legacy reasons.
        // I originally used "allowInsecureSSL" for the preference name but I changed it to
        // verifySSL. new users won't ever execute this path.
        if (!sharedPreferences.contains("webdav_verify_ssl")) {
            default = !sharedPreferences.getBoolean("webdav_allowInsecureSSL", false)
        }

        return sharedPreferences.getBoolean("webdav_verify_ssl", default)
    }

    fun setVerifySSLForWebDAV(value: Boolean) {
        sharedPreferences.edit {
            this.putBoolean("webdav_verify_ssl", value)
            this.apply()
        }
    }

    fun isWebDAVEnabledForGroups(): Boolean {
        return sharedPreferences.getBoolean("use_webdav_shared_libraries", false)
    }

    fun setCustomAttachmentStorage(data: String) {
        val editor = sharedPreferences.edit()
        editor.putString("custom_attachment_storage_location", data)
        editor.apply()
    }

    fun getCustomAttachmentStorageLocation(): String {
        return sharedPreferences.getString("custom_attachment_storage_location", "") ?: ""
    }

    fun getStorageMode(): String {
        return sharedPreferences.getString("attachment_sync_location", "") ?: ""
    }

    fun useExternalCache() {
        /*Sets the attachment storage location as being external cache.
        * For use in case of errors related to the custom location. */
        val editor = sharedPreferences.edit()
        editor.putString("attachment_sync_location", "EXTERNAL_CACHE")
        editor.putString("custom_attachment_storage_location", "")
        editor.apply()
    }

    fun isAttachmentUploadingEnabled(): Boolean {
        return sharedPreferences.getBoolean("attachments_uploading_enabled", true)
    }

    fun firstRunForVersion28(): Boolean {
        /*check to see if this is the first time the user is opening on version 2.1c (25)*/
        val firstRun = sharedPreferences.getBoolean("firstrun_version28", true)
        if (firstRun) {
            val editor = sharedPreferences.edit()
            editor.putBoolean("firstrun_version28", false)
            editor.apply()
        }
        return firstRun
    }

    fun firstRunForVersion42(): Boolean {
        /*check to see if this is the first time the user is opening on version 2.9 (42)*/
        val firstRun = sharedPreferences.getBoolean("firstrun_version42", true)
        if (firstRun) {
            val editor = sharedPreferences.edit()
            editor.putBoolean("firstrun_version42", false)
            editor.apply()
        }
        return firstRun
    }

    fun shouldLiveSearch(): Boolean {
        /* This preference defines whether library search should update as the user types.
        * This may be problematic for devices with a slow refresh rate or slow IO. */
        return sharedPreferences.getBoolean("should_live_search", true)
    }

    fun hasShownCustomStorageWarning(): Boolean {
        return sharedPreferences.getBoolean("has_shown_custom_storage_warning", false)
    }

    fun setShownCustomStorageWarning(value: Boolean) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("has_shown_custom_storage_warning", true)
        editor.apply()
    }

    fun shouldCheckMd5SumBeforeOpening(): Boolean {
        return sharedPreferences.getBoolean("check_md5sum_before_attachment_open", false)
    }

    fun getWebDAVConnectTimeout(): Long {
        return sharedPreferences.getLong("webdav_connect_timeout", 10000L)

    }

    fun setWebDAVConnectTimeout(time: Long) {
        if (time < 0L) {
            Log.e("Zotero", "Error invalid time $time.")
            return
        }

        sharedPreferences.edit {
            this.putLong("webdav_connect_timeout", time)
            this.apply()
        }
    }

    fun getWebDAVReadTimeout(): Long {
        return sharedPreferences.getLong("webdav_read_timeout", 30000L)
    }

    fun setWebDAVReadTimeout(time: Long) {
        if (time < 0L) {
            Log.e("Zotero", "Error invalid time $time.")
            return
        }

        sharedPreferences.edit {
            this.putLong("webdav_read_timeout", time)
            this.apply()
        }
    }

    fun getWebDAVWriteTimeout(): Long {
        return sharedPreferences.getLong("webdav_write_timeout", 30000L)
    }

    fun setWebDAVWriteTimeout(time: Long) {
        if (time < 0L) {
            Log.e("Zotero", "Error invalid time $time.")
            return
        }

        sharedPreferences.edit {
            this.putLong("webdav_write_timeout", time)
            this.apply()
        }
    }

    fun getHttpWriteTimeout(): Long {
        val s = sharedPreferences.getString("http_write_timeout", "60000")
        try {
            return s?.toLong() ?: 60000L
        } catch (e: NumberFormatException) {
            Log.e("zotero", "error parsing http write timeout. $s")
            return 60000L
        }
    }

    companion object {
        val SORT_METHOD_ASCENDING = "ASCENDING"
        val SORT_METHOD_DESCENDING = "DESCENDING"
    }

}

enum class WebdavAuthMode {
    BASIC,
    DIGEST,
    AUTOMATIC
}

enum class SortMethod {
    TITLE,
    DATE,
    AUTHOR,
    DATE_ADDED
}