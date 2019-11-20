package com.mickstarify.zooforzotero.ZoteroAPI

import android.content.Context
import android.util.Log
import com.thegrizzlylabs.sardineandroid.Sardine
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipFile

class Webdav(address: String, val username: String, val password: String) {
    var sardine: Sardine = OkHttpSardine()
    var address: String
    fun testConnection(): Boolean {
        return sardine.exists(address)
    }

    fun getAttachment(itemKey: String, context: Context): File? {
        val zipFile = File(context.cacheDir, "/${itemKey.toUpperCase()}.zip")
        if (zipFile.exists()) {
            zipFile.delete()
        }
        val status = downloadFile(address + "/${itemKey.toUpperCase()}.zip", zipFile)
        if (status == false) {
            return null
        }
        ZipFile(zipFile.absolutePath)
        val zipFile2 = net.lingala.zip4j.ZipFile(zipFile)
        val attachmentFilename =
            zipFile2.fileHeaders.firstOrNull()?.fileName ?: throw Exception("Error empty zipfile.")
        net.lingala.zip4j.ZipFile(zipFile).extractAll(context.externalCacheDir!!.absolutePath)
        zipFile.delete() // don't need this anymore.
        return File(context.externalCacheDir, attachmentFilename)

    }

    fun downloadFile(webpath: String, outputFile: File): Boolean {
        var inputStream: InputStream? = null
        try {
            inputStream = sardine.get(webpath)
        } catch (e: IllegalArgumentException) {
            Log.e("zotero", "${e}")
            throw(e)
        } catch (e: Exception) {
            Log.e("zotero", "${e}")
            throw(IOException("File not found."))
        }
        val outputStream = outputFile.outputStream()
        val buffer = ByteArray(32768)
        var read = inputStream.read(buffer)
        var failed = false
        while (read > 0) {
            try {
                outputStream.write(buffer, 0, read)
                read = inputStream.read(buffer)
            } catch (e: Exception) {
                Log.e("zotero", "exception downloading webdav attachment ${e.message}")
                failed = true
                break
            }
        }
        outputStream.close()
        if (read > 0 || failed) {
            return false
        }
        return true
    }

    init {
        if (username != "" && password != "") {
            sardine.setCredentials(username, password)
        }

        this.address = if (address.endsWith("/zotero")) {
            address
        } else {
            address + "/zotero"
        }
    }

}