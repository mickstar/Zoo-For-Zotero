package com.mickstarify.zooforzotero.LibraryActivity.ItemView


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import org.jetbrains.anko.support.v4.toast

private const val ARG_ATTACHMENT = "attachment"


class ItemAttachmentEntry : Fragment() {
    private var attachment: Item? = null
    var fileOpenListener: OnAttachmentFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            attachment = it.getParcelable(ARG_ATTACHMENT)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val linkMode = attachment?.getItemData("linkMode")

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_item_attachment_entry, container, false)
        val filename = view.findViewById<TextView>(R.id.textView_filename)
        filename.text = attachment?.data?.get("filename") ?: "unknown"
        if (linkMode == "linked_file") { // this variant uses title as a filename.
            filename.text = "[Linked] ${attachment?.getItemData("title")}"
        }

        val filetype = attachment?.data!!["contentType"] ?: ""
        val layout = view.findViewById<ConstraintLayout>(R.id.constraintLayout_attachments_entry)
        val icon = view.findViewById<ImageView>(R.id.imageView_attachment_icon)
        if (attachment?.isDownloadable() == true) {
            if (filetype == "application/pdf") {
                icon.setImageResource(R.drawable.treeitem_attachment_pdf_2x)
            } else if (filetype == "image/vnd.djvu") {
                icon.setImageResource(R.drawable.djvu_icon)
            } else if (attachment?.getFileExtension() == "epub"){
                icon.setImageResource(R.drawable.epub_icon)
            }
            layout.setOnClickListener {
                if (linkMode == "linked_file") {
                    toast("This attachment is linked and I cannot download it.")
                } else {
                    fileOpenListener?.openAttachmentFileListener(
                        attachment ?: throw Exception("No Attachment given.")
                    )
                }
            }
        }
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnAttachmentFragmentInteractionListener) {
            fileOpenListener = context
        } else {
            throw RuntimeException(context.toString() + " must implement OnAttachmentFragmentInteractionListener")
        }
    }

    interface OnAttachmentFragmentInteractionListener {
        fun openAttachmentFileListener(item: Item)
    }

    companion object {
        @JvmStatic
        fun newInstance(attachment: Item) =
            ItemAttachmentEntry().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_ATTACHMENT, attachment)
                }
            }
    }
}
