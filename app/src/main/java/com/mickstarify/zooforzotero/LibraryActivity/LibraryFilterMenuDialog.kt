package com.mickstarify.zooforzotero.LibraryActivity

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import com.google.firebase.analytics.FirebaseAnalytics
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.R
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.sdk27.coroutines.onClick

class LibraryFilterMenuDialog(val context: Context, val onFilterChange: (() -> (Unit))) {
    lateinit var preferences: PreferenceManager

    var selected_sorting_method = "UNSET"
    var is_showing_pdf: Boolean = false
    var is_showing_notes: Boolean = false

    fun setSortingMethod(index: Int) {
        try {
            selected_sorting_method =
                context.resources.getStringArray(R.array.sort_options_values)[index]
        } catch (e: ArrayIndexOutOfBoundsException) {
            Log.e("zotero", "Error array out of index for LibraryFilterDialog")
        }
    }

    private fun saveSettings(onlyNotes: Boolean, onlyPDFs: Boolean) {
        preferences.setIsShowingOnlyPdfs(onlyPDFs)
        preferences.setIsShowingOnlyNotes(onlyNotes)

        preferences.setSortMethod(selected_sorting_method)

        val params = Bundle().apply {
            putBoolean("show_pdfs", onlyPDFs)
            putBoolean("only_notes", onlyNotes)
            putString("sort_method", selected_sorting_method)
        }
        FirebaseAnalytics.getInstance(context).logEvent("set_filter", params)

        onFilterChange()
    }

    private fun getSortString(method: String): String {
        val i = context.resources.getStringArray(R.array.sort_options_values).indexOf(method)
        if (i == -1) {
            val params = Bundle()
            params.putString("method", method)
            FirebaseAnalytics.getInstance(context).logEvent("error_sort_method_not_found", params)
            return "Error"
        }
        return context.resources.getTextArray(R.array.sort_options_entries)[i].toString()
    }

    fun show() {
        val dialogBuilder = AlertDialog.Builder(context).create()
        val inflater = context.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_filter_menu, null)

        val sortingMethodButton = dialogView.findViewById<Button>(R.id.button_sort_by)
        sortingMethodButton.setText(this.getSortString(selected_sorting_method))
        val checkbox_show_only_pdf = dialogView.findViewById<CheckBox>(R.id.checkBox_show_only_pdf)
        val checkbox_show_only_notes =
            dialogView.findViewById<CheckBox>(R.id.checkBox_show_only_notes)


        checkbox_show_only_notes.isChecked = this.is_showing_notes
        checkbox_show_only_pdf.isChecked = this.is_showing_pdf

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Sorting Method")
        builder.setItems(context.resources.getTextArray(R.array.sort_options_entries),
            DialogInterface.OnClickListener
            { dialogInterface, i ->
                this.setSortingMethod(i)
                sortingMethodButton.setText(this.getSortString(this.selected_sorting_method))

            })

        sortingMethodButton.onClick { builder.show() }

        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)
        val submitButton = dialogView.findViewById<Button>(R.id.btn_submit)

        cancelButton.onClick {
            dialogBuilder.dismiss()
        }

        submitButton.onClick {
            saveSettings(checkbox_show_only_notes.isChecked, checkbox_show_only_pdf.isChecked)
            dialogBuilder.dismiss()
        }


        dialogBuilder.setView(dialogView)

        //not letting user dismiss dialog because otherwise the keyboard stays and it's a pain to
        //dismiss it. (need to find currentFocusedView, etc)
        dialogBuilder.setCanceledOnTouchOutside(false)

        dialogBuilder.show()
    }


    init {
        preferences = PreferenceManager(context)
        selected_sorting_method =
            preferences.sortMethodToString(preferences.getSortMethod()) //terrible code, i know.
        is_showing_pdf = preferences.getIsShowingOnlyPdfs()
        is_showing_notes = preferences.getIsShowingOnlyNotes()

    }

}