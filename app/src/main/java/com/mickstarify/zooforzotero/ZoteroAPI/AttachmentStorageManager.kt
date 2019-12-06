package com.mickstarify.zooforzotero.ZoteroAPI

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.documentfile.provider.DocumentFile
import com.google.common.hash.Hashing
import com.google.common.io.Files
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import dagger.Module
import java.io.File
import java.io.IOException
import java.io.OutputStream
import javax.inject.Singleton

const val STORAGE_ACCESS_REQUEST = 1  // The request code

@Module
@Singleton
class AttachmentStorageManager(val context: Context) {
    val preferenceManager = PreferenceManager(context)

    enum class StorageMode {
        CUSTOM,
        EXTERNAL_CACHE,
        NONE_SET
    }

    var storageMode: StorageMode = StorageMode.NONE_SET
    var rootDocFile: DocumentFile? = null // will get inited in testStorage()

    init {
        val storageModeString = preferenceManager.getStorageMode()
        storageMode = when (storageModeString) {
            "CUSTOM" -> StorageMode.CUSTOM
            "EXTERNAL_CACHE" -> StorageMode.EXTERNAL_CACHE
            else -> StorageMode.EXTERNAL_CACHE
        }
        if (storageMode == StorageMode.CUSTOM) {
            if (testStorage() == false) {
                throw (IOException("Cannot Read Attachment Directory"))
            }
        }
    }

    fun getFilenameForItem(item: Item): String {
        return item.data.get("filename") ?: "unknown.pdf"
    }

    fun checkIfFileExists(filename: String): Boolean {
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            val outputDir = context.externalCacheDir
            val file = File(outputDir, filename)
            return file.exists()
        } else if (storageMode == StorageMode.CUSTOM) {
            return rootDocFile?.findFile(filename)?.exists() == true
        }

        throw Exception()
    }

    fun checkFileForMd5(filename: String, md5String: String): Boolean {
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            val md5Key =
                Files.hash(File(context.externalCacheDir, filename), Hashing.md5()).toString()
            return md5Key == md5String
        } else {
            throw(Exception("not implemented"))
        }

    }

    fun getFileOutputStream(filename: String): OutputStream {
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            val file = File(context.externalCacheDir, filename)
            return file.outputStream()
        } else {
            throw(Exception("not implemented"))
        }
    }

    fun askUserForPath(activity: Activity) {
        /* Attempts to get access to a directory to store zotero storage. */
        val intent = Intent()
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity.startActivityForResult(intent, STORAGE_ACCESS_REQUEST)
    }


    fun testStorage(): Boolean {
        val location = preferenceManager.getCustomAttachmentStorageLocation()
        if (location == "") {
            return false
        }
        rootDocFile = DocumentFile.fromTreeUri(context, Uri.parse(location))
        Log.d(
            "zotero",
            "testing dir canWrite=${rootDocFile?.canWrite()} canRead=${rootDocFile?.canRead()}"
        )
        return rootDocFile?.canWrite() == true
    }

    fun setStorage(location: String?) {
        if (location == null) {
            Log.e("zotero", "error got null for location in setStorage()")
            return
        }
        preferenceManager.setCustomAttachmentStorage(location)
        testStorage()
    }
}