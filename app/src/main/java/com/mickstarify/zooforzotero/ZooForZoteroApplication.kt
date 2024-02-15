package com.mickstarify.zooforzotero

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import org.acra.ACRA
import org.acra.ReportField
import org.acra.config.dialog
import org.acra.config.mailSender
import org.acra.data.StringFormat
import org.acra.ktx.initAcra

@HiltAndroidApp
class ZooForZoteroApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        initAcra {
            //core configuration:
            reportContent = listOf(
                ReportField.APP_VERSION_CODE,
                ReportField.ANDROID_VERSION,
                ReportField.PHONE_MODEL,
                ReportField.CUSTOM_DATA,
                ReportField.STACK_TRACE,
                ReportField.LOGCAT
            )

            reportFormat = StringFormat.JSON
            excludeMatchingSharedPreferencesKeys = listOf("*")
            //each plugin you chose above can be configured in a block like this:

            dialog {
                //required
                text = "Zoo for Zotero crashed. Would you like to report it?"
                //optional, enables the dialog title
                title = "Crash Report"
                //defaults to android.R.string.ok
                positiveButtonText = "Send"
                //defaults to android.R.string.cancel
                negativeButtonText = "Cancel"
                resTheme = R.style.Theme_ZooForZotero
            }

            mailSender {
                //required
                mailTo = "mickstarify@gmail.com"
                //defaults to true
                reportAsFile = true
                //defaults to ACRA-report.stacktrace
                reportFileName = "Crash.txt"
                //defaults to "<applicationId> Crash Report"
                subject = "Zoo for Zotero crash report"
            }
        }
    }

}
