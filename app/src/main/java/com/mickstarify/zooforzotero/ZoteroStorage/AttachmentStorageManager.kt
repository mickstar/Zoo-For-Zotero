package com.mickstarify.zooforzotero.ZoteroStorage

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.FileProvider
import androidx.documentfile.provider.DocumentFile
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Item
import okhttp3.internal.toHexString
import okio.buffer
import okio.sink
import okio.source
import java.io.*
import java.security.MessageDigest
import java.util.*


const val STORAGE_ACCESS_REQUEST = 1  // The request code

/* This classes handles the data storage for all attachments. */

//todo proper folder structure.
class AttachmentStorageManager(val context: Context) {
    val preferenceManager = PreferenceManager(context)

    enum class StorageMode {
        CUSTOM,
        EXTERNAL_CACHE,
        NONE_SET
    }

    val storageMode: StorageMode
        get() = when (preferenceManager.getStorageMode()) {
            "CUSTOM" -> StorageMode.CUSTOM
            "EXTERNAL_CACHE" -> StorageMode.EXTERNAL_CACHE
            else -> StorageMode.EXTERNAL_CACHE
        }


    init {
    }

    fun validateAccess() {
        if (storageMode == StorageMode.CUSTOM) {
            if (testStorage() == false) {
                throw (IOException("Cannot Read Attachment Directory"))
            }
        }
    }

    fun getFilenameForItem(item: Item): String {
        return item.data.get("filename") ?: "unknown.pdf"
    }

    fun validateMd5ForItem(item: Item): Boolean {
        if (item.getItemType() != Item.ATTACHMENT_TYPE || item.data["md5"] == null) {
            throw(Exception("error invalid item ${item.ItemKey}: ${item.getItemType()} cannot calculate md5."))
        }
        val md5Key = calculateMd5(item)
        return (md5Key == item.data["md5"])
    }

    fun checkIfAttachmentExists(item: Item, checkMd5: Boolean = true): Boolean {
        val location = preferenceManager.getCustomAttachmentStorageLocation()
        val rootDocFile = DocumentFile.fromTreeUri(context, Uri.parse(location))
        val filename = getFilenameForItem(item)
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            val outputDir = context.externalCacheDir
            val file = File(outputDir, filename)
            if (checkMd5) {
                return file.exists() && calculateMd5(file.inputStream()) == item.data["md5"]
            } else {
                return file.exists()
            }

        } else if (storageMode == StorageMode.CUSTOM) {
            val file = rootDocFile!!.findFile(filename)
            if (file == null) {
                return false
            }
            val exists = rootDocFile?.findFile(filename)?.exists() == true
            if (checkMd5) {
                return exists && (calculateMd5(context.contentResolver.openInputStream(file.uri)!!) == item.data["md5"])
            } else {
                return exists
            }
        }

        throw Exception()
    }

    fun calculateMd5(attachment: Item): String {
        val attachmentInputStream = this.getItemInputStream(attachment)
        return calculateMd5(attachmentInputStream)
    }

    fun calculateMd5(inputStream: InputStream): String {
        val buffer = ByteArray(1024)
        val complete: MessageDigest = MessageDigest.getInstance("MD5")
        var numRead: Int

        do {
            numRead = inputStream.read(buffer)
            if (numRead > 0) {
                complete.update(buffer, 0, numRead)
            }
        } while (numRead != -1)

        inputStream.close()
        var result = ""
        var checksumByteArray = complete.digest()
        for (i in 0 until checksumByteArray.size) {
            result += ((checksumByteArray.get(i).toInt() and 0xff) + 0x100).toHexString()
                .substring(1)
        }
        return result
    }

    fun getItemOutputStream(item: Item): OutputStream {
        val filename = getFilenameForItem(item)
        val mimeType = item.data["contentType"] ?: "application/pdf"
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            val file = File(context.externalCacheDir, filename)
            return file.outputStream()
        } else if (storageMode == StorageMode.CUSTOM) {
            val documentTree = DocumentFile.fromTreeUri(context, getCustomStorageTreeURI())
            var itemFile = documentTree!!.findFile(filename)
            if (itemFile == null || !itemFile.exists()) {
                itemFile = documentTree!!.createFile(mimeType, filename)
            }
            return context.contentResolver.openOutputStream(itemFile!!.uri)!!
        }
        throw Exception("not implemented")
    }

    fun getItemInputStream(item: Item): InputStream {
        val filename = getFilenameForItem(item)
        val mimeType = item.data["contentType"] ?: "application/pdf"
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            val file = File(context.externalCacheDir, filename)
            return file.inputStream()
        } else if (storageMode == StorageMode.CUSTOM) {
            val documentTree = DocumentFile.fromTreeUri(context, getCustomStorageTreeURI())
            val itemFile = documentTree?.findFile(filename)
                ?: throw(FileNotFoundException("cannot open inputstream."))
            return context.contentResolver.openInputStream(itemFile.uri)!!
        }
        throw Exception("not implemented")
    }

    fun getAttachmentUri(item: Item): Uri {
        val filename = getFilenameForItem(item)
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            val file = File(context.externalCacheDir, filename)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                return FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
            } else {
                return Uri.fromFile(file)
            }

        } else if (storageMode == StorageMode.CUSTOM) {
            val documentTree = DocumentFile.fromTreeUri(context, getCustomStorageTreeURI())
            val file = documentTree?.findFile(filename) ?: throw IOException("File not found! null")
            if (file.exists()) {
                return file.uri
            }
            throw IOException("File not found!")
        }
        throw Exception("not implemented")
    }

    fun askUserForPath(activity: Activity) {
        /* Attempts to get access to a directory to store zotero storage. */
        val intent = Intent()
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
        activity.startActivityForResult(
            intent,
            STORAGE_ACCESS_REQUEST
        )
    }

    fun openAttachment(attachmentUri: Uri) {
        var intent = Intent(Intent.ACTION_VIEW)
        Log.d("zotero", "opening PDF with Uri $attachmentUri")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d("zotero", "${attachmentUri.query}")
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = attachmentUri
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
        } else {
            intent.setDataAndType(attachmentUri, "application/pdf")
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            intent = Intent.createChooser(intent, "Open File")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        }
        context.startActivity(intent)
    }


    fun testStorage(): Boolean {
        val location = preferenceManager.getCustomAttachmentStorageLocation()
        if (location == "") {
            return false
        }
        val rootDocFile = DocumentFile.fromTreeUri(context, Uri.parse(location))
        Log.d(
            "zotero",
            "testing dir canWrite=${rootDocFile?.canWrite()} canRead=${rootDocFile?.canRead()}"
        )
        return rootDocFile?.canWrite() == true
    }

    fun createTempFile(filename: String, fileExt: String): File {
        val zipFile = File(context.cacheDir, "${filename.toUpperCase(Locale.ROOT)}.${fileExt}")
        if (zipFile.exists()) {
            zipFile.delete()
        }
        return zipFile
    }

    fun setStorage(location: String?) {
        if (location == null) {
            Log.e("zotero", "error got null for location in setStorage()")
            return
        }

        preferenceManager.setCustomAttachmentStorage(location)
        testStorage()
    }

    fun getCustomStorageTreeURI(): Uri {
        return Uri.parse(preferenceManager.getCustomAttachmentStorageLocation())
    }

    fun getMtime(attachmentUri: Uri): Long {
        val docFile = DocumentFile.fromSingleUri(context, attachmentUri)
        return docFile?.lastModified() ?: throw FileNotFoundException()
    }

    fun getFileSize(attachmentUri: Uri): Long {
        val docFile = DocumentFile.fromSingleUri(context, attachmentUri)
        return docFile?.length() ?: throw FileNotFoundException()
    }

    fun readBytes(attachment: Item): ByteArray {
        return getItemInputStream(attachment).readBytes()
    }

    fun writeAttachmentFromFile(file: File, attachment: Item): Uri {
        val outputStream = getItemOutputStream(attachment)
        val inputSource = file.inputStream().source()

        val outputSinkBuffer = outputStream.sink().buffer()
        outputSinkBuffer.writeAll(inputSource)
        outputSinkBuffer.close()
        inputSource.close()
        outputStream.close()
        return getAttachmentUri(attachment)
    }

    // todo Fix duplicate files (1)
    // todo Fix External Cache for file uploading.
}