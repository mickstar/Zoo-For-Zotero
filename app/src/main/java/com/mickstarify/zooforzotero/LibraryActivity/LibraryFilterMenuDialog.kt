package com.mickstarify.zooforzotero.LibraryActivity

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.util.Log
import android.view.View
import android.widget.Button
import com.mickstarify.zooforzotero.PreferenceManager
import com.mickstarify.zooforzotero.R
import org.jetbrains.anko.layoutInflater
import org.jetbrains.anko.sdk27.coroutines.onClick

class LibraryFilterMenuDialog(val context: Context) {
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

    fun show() {
        if (context == null) {
            Log.e("Zotero", "Error LibraryFilterMenuDialog got null context.")
            return
        }

        val dialogBuilder = AlertDialog.Builder(context).create()
        val inflater = context.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.dialog_filter_menu, null)

        val cancelButton = dialogView.findViewById<Button>(R.id.btn_cancel)
        val submitButton = dialogView.findViewById<Button>(R.id.btn_submit)

        val builder = AlertDialog.Builder(context)
        builder.setTitle("Sorting Method")
        builder.setItems(context.resources.getTextArray(R.array.sort_options_entries),
            DialogInterface.OnClickListener
            { dialogInterface, i ->
                Log.d("Zotero", "pressed ${i} ")
                this.setSortingMethod(i)

            })

        builder.show()

        cancelButton.onClick {
            dialogBuilder.dismiss()
        }

        submitButton.onClick {
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
        val sortMethod = preferences.getSortMethod()

    }

}