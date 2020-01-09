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
import com.mickstarify.zooforzotero.ZooForZoteroApplication
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import okhttp3.internal.toHexString
import okio.buffer
import okio.sink
import okio.source
import java.io.*
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton


const val STORAGE_ACCESS_REQUEST = 1  // The request code

/* This classes handles the data storage for all attachments. */


@Singleton
class AttachmentStorageManager @Inject constructor(
    val context: Context,
    val preferenceManager: PreferenceManager
) {

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
//        ((context as Activity).application as ZooForZoteroApplication).component.inject(this)
        Log.e("zotero", "MADE AN INSTANCE OF ATTACHMENT MANAGER")
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

    fun validateMd5ForItem(
        item: Item,
        md5Key: String
    ): Boolean {
        if (item.itemType != Item.ATTACHMENT_TYPE) {
            throw(Exception("error invalid item ${item.itemKey}: ${item.itemType} cannot calculate md5."))
        }
        if (md5Key == "") {
            Log.d("zotero", "error cannot check MD5, no MD5 Available")
            return true
        }
        val calculatedMd5Key = calculateMd5(item)
        return (calculatedMd5Key == md5Key)
    }

    fun checkIfAttachmentExists(item: Item, checkMd5: Boolean = true): Boolean {
        val filename = getFilenameForItem(item)
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            val outputDir = File(context.externalCacheDir, item.itemKey.toUpperCase(Locale.ROOT))
            if (!outputDir.exists() || outputDir.isDirectory == false) {
                return false
            }
            val file = File(outputDir, filename)
            if (checkMd5) {
                return file.exists() && calculateMd5(file.inputStream()) == item.data["md5"]
            } else {
                return file.exists()
            }

        } else if (storageMode == StorageMode.CUSTOM) {
            val location = preferenceManager.getCustomAttachmentStorageLocation()
            val rootDocFile = DocumentFile.fromTreeUri(context, Uri.parse(location))
            var directory = rootDocFile?.findFile(item.itemKey.toUpperCase(Locale.ROOT))
            if (directory == null || directory.isDirectory == false) {
                directory = rootDocFile?.createDirectory(item.itemKey.toUpperCase(Locale.ROOT))
            }
            val file = directory?.findFile(filename)
            if (file == null) {
                return false
            }
            val exists = file.exists()
            if (file.exists() && checkMd5) {
                return calculateMd5(context.contentResolver.openInputStream(file.uri)!!) == item.data["md5"]
            } else {
                return exists
            }
        }

        throw Exception("not implemented")
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
        val checksumByteArray = complete.digest()
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
            val file = getAttachmentFile(item)
            return file.outputStream()
        } else if (storageMode == StorageMode.CUSTOM) {
            val documentTree = DocumentFile.fromTreeUri(context, getCustomStorageTreeURI())
            var directory = documentTree?.findFile(item.itemKey.toUpperCase(Locale.ROOT))
            if (directory == null || directory.isDirectory == false) {
                directory = documentTree?.createDirectory(item.itemKey.toUpperCase(Locale.ROOT))
            }
            var itemFile = directory!!.findFile(filename)
            if (itemFile == null || !itemFile.exists()) {
                itemFile = directory.createFile(mimeType, filename)
            }
            return context.contentResolver.openOutputStream(itemFile!!.uri)!!
        }
        throw Exception("not implemented")
    }

    fun getItemInputStream(item: Item): InputStream {
        val filename = getFilenameForItem(item)
        val mimeType = item.data["contentType"] ?: "application/pdf"
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            val file = getAttachmentFile(item)
            return file.inputStream()
        } else if (storageMode == StorageMode.CUSTOM) {
            return context.contentResolver.openInputStream(getAttachmentUri(item))!!
        }
        throw Exception("not implemented")
    }

    fun getAttachmentUri(item: Item): Uri {
        val filename = getFilenameForItem(item)
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            val file = getAttachmentFile(item)
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
            val directory = documentTree?.findFile(item.itemKey.toUpperCase(Locale.ROOT))
            val file = directory?.findFile(filename) ?: throw FileNotFoundException()
            if (file.exists()) {
                return file.uri
            }
            throw FileNotFoundException()
        }
        throw Exception("not implemented")
    }

    fun askUserForPath(activity: Activity) {
        /* Attempts to get access to a directory to store zotero storage. */
        val intent = Intent()
        intent.setAction(Intent.ACTION_OPEN_DOCUMENT_TREE)
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        activity.startActivityForResult(
            intent,
            STORAGE_ACCESS_REQUEST
        )
    }

    fun openAttachment(attachment: Item): Intent {
        var intent = Intent(Intent.ACTION_VIEW)
        var attachmentUri = getAttachmentUri(attachment)
        Log.d("zotero", "opening PDF with Uri $attachmentUri")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Log.d("zotero", "${attachmentUri.query}")
            intent = Intent(Intent.ACTION_VIEW)
            intent.data = attachmentUri
            intent.flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        } else {
            intent.setDataAndType(attachmentUri, "application/pdf")
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            intent = Intent.createChooser(intent, "Open File")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        }
        return intent
    }


    fun testStorage(): Boolean {
        if (storageMode == StorageMode.EXTERNAL_CACHE) {
            return true //nothing to test.
        }

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

    fun createTempFile(filename: String): File {
        val zipFile = File(context.cacheDir, filename)
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

        val uri = Uri.parse(location)

        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or
                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )
        context.contentResolver.takePersistableUriPermission(
            uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION
        )

        preferenceManager.setCustomAttachmentStorage(location)
        testStorage()
    }

    fun getCustomStorageTreeURI(): Uri {
        return Uri.parse(preferenceManager.getCustomAttachmentStorageLocation())
    }

    fun getMtime(attachment: Item): Long {
        when (storageMode) {
            StorageMode.EXTERNAL_CACHE -> {
                val file = getAttachmentFile(attachment)
                return file.lastModified()
            }
            StorageMode.CUSTOM -> {
                val docFile = DocumentFile.fromSingleUri(context, getAttachmentUri(attachment))
                return docFile?.lastModified() ?: throw FileNotFoundException()
            }
            else -> throw Exception("Invalid storage mode")
        }

    }

    private fun getAttachmentFile(attachment: Item): File {
        val filename = getFilenameForItem(attachment)
        val directory = File(context.externalCacheDir, attachment.itemKey.toUpperCase(Locale.ROOT))
        if (!directory.exists()) {
            directory.mkdirs()
        }
        return File(directory, filename)
    }

    fun getFileSize(attachment: Item): Long {
        if (storageMode == StorageMode.EXTERNAL_CACHE){
            return getAttachmentFile(attachment).length()

        } else if (storageMode == StorageMode.CUSTOM) {
            return getFileSize(getAttachmentUri(attachment))
        }
        throw Exception("Invalid storage mode")
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

    fun deleteAttachment(attachment: Item) {
        when (storageMode) {
            StorageMode.EXTERNAL_CACHE -> {
                getAttachmentFile(attachment).delete()
            }
            StorageMode.CUSTOM -> {
                val docFile = DocumentFile.fromSingleUri(context, getAttachmentUri(attachment))
                docFile?.delete()
            }
            else -> throw Exception("not implemented")
        }
    }

}