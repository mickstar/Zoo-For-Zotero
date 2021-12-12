package com.mickstarify.zooforzotero.ZoteroAPI

import android.content.Context
import android.util.Log
import com.burgstaller.okhttp.AuthenticationCacheInterceptor
import com.burgstaller.okhttp.CachingAuthenticatorDecorator
import com.burgstaller.okhttp.DispatchingAuthenticator
import com.burgstaller.okhttp.basic.BasicAuthenticator
import com.burgstaller.okhttp.digest.CachingAuthenticator
import com.burgstaller.okhttp.digest.Credentials
import com.burgstaller.okhttp.digest.DigestAuthenticator
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.WebdavAuthMode
import com.mickstarify.zooforzotero.ZoteroStorage.AttachmentStorageManager
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import com.thegrizzlylabs.sardineandroid.impl.OkHttpSardine
import io.reactivex.Completable
import io.reactivex.Observable
import net.lingala.zip4j.ZipFile
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okio.buffer
import okio.sink
import okio.source
import java.io.File
import java.io.InputStream
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import java.util.Locale
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class Webdav(
    preferenceManager: PreferenceManager
) {
    private var sardine: OkHttpSardine
    var address: String


    fun testConnection(): Boolean {
        return sardine.exists(address)
    }

    fun downloadPropToString(webpath: String): String {
        val reader = sardine.get(webpath).source()
        val bufferedReader = reader.buffer()
        val s = bufferedReader.readUtf8()
        bufferedReader.close()
        return s
    }

    fun downloadFileRx(
        attachment: Item,
        context: Context,
        attachmentStorageManager: AttachmentStorageManager
    ): Observable<DownloadProgress> {
        val webpathProp = address + "/${attachment.itemKey.toUpperCase(Locale.ROOT)}.prop"
        val webpathZip = address + "/${attachment.itemKey.toUpperCase(Locale.ROOT)}.zip"

        val observable: Observable<DownloadProgress> = Observable.create { emitter ->
            try {
                val propString = downloadPropToString(webpathProp)
                val prop = WebdavProp(propString)

                var inputStream: InputStream? = null
                try {
                    inputStream = sardine.get(webpathZip)
                } catch (e: IllegalArgumentException) {
                    Log.e("zotero", "${e}")
                    throw(e)
                } catch (e: Exception) {
                    Log.e("zotero", "${e}")
                    throw(e)
                }

                val zipFile =
                    attachmentStorageManager.createTempFile(
                        "${attachment.itemKey.toUpperCase(Locale.ROOT)}.pdf"
                    )
                val downloadOutputStream = zipFile.outputStream()

                val buffer = ByteArray(32768)
                var read = inputStream.read(buffer)
                var total: Long = 0
                while (read > 0) {
                    try {
                        total += read
                        emitter.onNext(DownloadProgress(total, -1, prop.mtime, prop.hash))
                        downloadOutputStream.write(buffer, 0, read)
                        read = inputStream.read(buffer)
                    } catch (e: Exception) {
                        Log.e("zotero", "exception downloading webdav attachment ${e.message}")
                        throw RuntimeException("Error downloading webdav attachment ${e.message}")
                    }
                }
                downloadOutputStream.close()
                inputStream.close()
                if (read > 0) {
                    throw RuntimeException(
                        "Error did not finish downloading ${
                            attachment.itemKey.toUpperCase(
                                Locale.ROOT
                            )
                        }.zip"
                    )
                }
                val zipFile2 = ZipFile(zipFile)
                val attachmentFilename =
                    zipFile2.fileHeaders.firstOrNull()?.fileName
                        ?: throw Exception("Error empty zipfile.")
                ZipFile(zipFile).extractAll(context.cacheDir.absolutePath)
                zipFile.delete() // don't need this anymore.
                attachmentStorageManager.writeAttachmentFromFile(
                    File(
                        context.cacheDir,
                        attachmentFilename
                    ), attachment
                )
                File(context.cacheDir, attachmentFilename).delete()
                emitter.onComplete()
            } catch (e: Exception) {
                Log.e("zotero", "big exception hit ${e} || ${emitter.isDisposed}")
                // this your brain on rxjava. this is to avoid a undeliverable crash on dispose().
                if (!emitter.isDisposed) {
                    throw e
                }
            }
        }
        return observable
    }

    fun uploadAttachment(
        attachment: Item,
        attachmentStorageManager: AttachmentStorageManager
    ): Completable {
        /*Uploading will take 3 steps,
        * 1. Compress attachment into a ZIP file (using internal cache dir)
        * 2. Create  F3FXJF_NEW.prop file.
        * 3. Upload to webdav server F3FXJF_NEW.zip and F3FXJF_NEW.prop
        * 4. send a delete request and  rename request to server so we have
        *  F3FXJF.zip + F3FXJF.prop resulting*/

        return Completable.fromAction {
            // STEP 1 CREATE ZIP
            val fileInputStream =
                attachmentStorageManager.getItemInputStream(attachment).source()
            val filename = attachmentStorageManager.getFilenameForItem(attachment)
            val tempFile = attachmentStorageManager.createTempFile(filename)
            val sinkBuffer = tempFile.outputStream().sink().buffer()
            sinkBuffer.writeAll(fileInputStream)
            sinkBuffer.close()
            fileInputStream.close()

            val zipFile =
                attachmentStorageManager.createTempFile("${attachment.itemKey.toUpperCase(Locale.ROOT)}_NEW.zip")

            ZipFile(zipFile).addFile(tempFile)
            tempFile.delete()
            // STEP 2 -- CREATE PROP
            val propFile =
                attachmentStorageManager.createTempFile("${attachment.itemKey.toUpperCase(Locale.ROOT)}_NEW.prop")
            val propContent = WebdavProp(
                attachmentStorageManager.getMtime(attachment),
                attachmentStorageManager.calculateMd5(attachment)
            ).serialize()
            val outputStream = propFile.outputStream()
            outputStream.write(propContent.toByteArray(Charsets.UTF_8))
            outputStream.close()

            // STEP 3 -- UPLOAD ZIP AND PROP
            val newZipPath = address + "/${attachment.itemKey.toUpperCase(Locale.ROOT)}_NEW.zip"
            val newPropPath = address + "/${attachment.itemKey.toUpperCase(Locale.ROOT)}_NEW.prop"

            // mostly useless step, delete any old _NEW.zip files that shouldn't exist
            // but might incase of a failed update earlier.
            if (sardine.exists(newZipPath)) {
                sardine.delete(newZipPath)
            }
            if (sardine.exists(newPropPath)) {
                sardine.delete(newPropPath)
            }

            // upload files
            sardine.put(newPropPath, propFile, "text/plain")
            sardine.put(newZipPath, zipFile, "application/zip")

            zipFile.delete()
            propFile.delete()

            // STEP 4 -- DELETE AND RENAME TO REPLACE OLD CONTENT.
            val zipPath = address + "/${attachment.itemKey.toUpperCase(Locale.ROOT)}.zip"
            val propPath = address + "/${attachment.itemKey.toUpperCase(Locale.ROOT)}.prop"

            sardine.delete(propPath)
            sardine.delete(zipPath)

            sardine.move(newPropPath, propPath)
            sardine.move(newZipPath, zipPath)

            // DONE.
        }

    }

    val authCach: MutableMap<String, CachingAuthenticator> = HashMap()

    init {
        val username = preferenceManager.getWebDAVUsername()
        val password = preferenceManager.getWebDAVPassword()
        val verifySSL = preferenceManager.getVerifySSLForWebDAV()


        val clientBuilder = OkHttpClient.Builder()
            .protocols(listOf(Protocol.HTTP_1_1, Protocol.HTTP_2))
            .callTimeout(0, TimeUnit.SECONDS) // no call timeout.
            .connectTimeout(preferenceManager.getWebDAVConnectTimeout(), TimeUnit.MILLISECONDS)
            .readTimeout(preferenceManager.getWebDAVReadTimeout(), TimeUnit.MILLISECONDS)
            .writeTimeout(preferenceManager.getWebDAVWriteTimeout(), TimeUnit.MILLISECONDS);


        if (username != "" && password != "") {
            val credentials = Credentials(username, password)
            val digestAuthenticator = DigestAuthenticator(credentials)
            val basicAuthenticator = BasicAuthenticator(credentials)

            val authenticator = when (preferenceManager.getWebDAVAuthMode()) {
                WebdavAuthMode.BASIC -> basicAuthenticator
                WebdavAuthMode.DIGEST -> digestAuthenticator
                WebdavAuthMode.AUTOMATIC -> {
                    DispatchingAuthenticator.Builder()
                        .with("digest", digestAuthenticator)
                        .with("basic", basicAuthenticator)
                        .build()
                }
            }

            clientBuilder
                .authenticator(CachingAuthenticatorDecorator(authenticator, authCach))
                .addInterceptor(AuthenticationCacheInterceptor(authCach))
        }

        if (!verifySSL) {
            val trustAllCerts = arrayOf<TrustManager>(
                object : X509TrustManager {
                    @Throws(CertificateException::class)
                    override fun checkClientTrusted(
                        chain: Array<X509Certificate>,
                        authType: String
                    ) {
                    }

                    @Throws(CertificateException::class)
                    override fun checkServerTrusted(
                        chain: Array<X509Certificate>,
                        authType: String
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<X509Certificate> {
                        return arrayOf()
                    }
                }
            )
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            val sslSocketFactory = sslContext.socketFactory
            clientBuilder.sslSocketFactory(sslSocketFactory, trustAllCerts[0] as X509TrustManager)
            clientBuilder.hostnameVerifier(HostnameVerifier { hostname, session -> true })
        }

        sardine = OkHttpSardine(
            clientBuilder.build()
        )

        this.address = preferenceManager.getWebDAVAddress()
    }
}


