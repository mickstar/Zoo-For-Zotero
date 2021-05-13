package com.mickstarify.zooforzotero.LibraryActivity.ItemView


import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.mickstarify.zooforzotero.LibraryActivity.ViewModels.LibraryListViewModel
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item

private const val ARG_ATTACHMENT = "attachment"


class ItemAttachmentEntry(
    val itemKey: String
) : Fragment() {

    private var attachment: Item? = null
    var fileOpenListener: OnAttachmentFragmentInteractionListener? = null
    lateinit var libraryViewModel: LibraryListViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_item_attachment_entry, container, false)
        return view
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)

        libraryViewModel =
            ViewModelProvider(requireActivity()).get(LibraryListViewModel::class.java)

        val layout =
            requireView().findViewById<ConstraintLayout>(R.id.constraintLayout_attachments_entry)
        val filename = requireView().findViewById<TextView>(R.id.textView_filename)
        val icon = requireView().findViewById<ImageView>(R.id.imageView_attachment_icon)

        libraryViewModel.getOnItemClicked().observe(viewLifecycleOwner) { item ->
            attachment = item.attachments.filter { it.itemKey == itemKey }.firstOrNull()
            if (attachment == null) {
                Log.e("zotero", "Error attachments is null, please reload view.")
                return@observe
            }

            val linkMode = attachment?.getItemData("linkMode")

            filename.text = attachment?.data?.get("filename") ?: "unknown"
            if (linkMode == "linked_file") { // this variant uses title as a filename.
                filename.text = "[Linked] ${attachment?.getItemData("title")}"

            } else if (linkMode == "linked_url") {
                filename.text = "[Linked Url] ${attachment?.getItemData("title")}"
                layout.setOnClickListener {
                    val url = attachment?.getItemData("url")
                    AlertDialog.Builder(context)
                        .setMessage("Would you like to open this URL: $url")
                        .setPositiveButton("Yes") { dialog, which ->
                            val intent = Intent(Intent.ACTION_VIEW)
                            intent.setData(Uri.parse(url))
                            startActivity(intent)
                        }
                        .setNegativeButton("No", { _, _ -> })
                        .show()
                }
            }

            val filetype = attachment?.data!!["contentType"] ?: ""

            if (attachment?.isDownloadable() == true) {
                if (filetype == "application/pdf") {
                    icon.setImageResource(R.drawable.treeitem_attachment_pdf_2x)
                } else if (filetype == "image/vnd.djvu") {
                    icon.setImageResource(R.drawable.djvu_icon)
                } else if (attachment?.getFileExtension() == "epub") {
                    icon.setImageResource(R.drawable.epub_icon)
                } else {
                    // todo get default attachment icon.
                }
                layout.setOnClickListener {
                    if (linkMode == "linked_file") {
                        fileOpenListener?.openLinkedAttachmentListener(
                            attachment ?: throw Exception("No Attachment given.")
                        )
                    } else {
                        fileOpenListener?.openAttachmentFileListener(
                            attachment ?: throw Exception("No Attachment given.")
                        )
                    }
                }

                layout.setOnLongClickListener {
                    AlertDialog.Builder(context)
                        .setTitle("Attachment")
                        .setItems(
                            arrayOf("Open", "Force Re-upload", "Delete local copy of attachment"),
                            object : DialogInterface.OnClickListener {
                                override fun onClick(dialog: DialogInterface?, item: Int) {
                                    when (item) {
                                        0 -> fileOpenListener?.openAttachmentFileListener(
                                            attachment ?: throw Exception("No Attachment given.")
                                        )
                                        1 -> fileOpenListener?.forceUploadAttachmentListener(
                                            attachment ?: throw Exception("No Attachment given.")
                                        )
                                        2 -> {
                                            fileOpenListener?.deleteLocalAttachment(
                                                attachment
                                                    ?: throw Exception("No Attachment given.")
                                            )
                                        }
                                    }
                                }

                            }).show()
                    true
                }
            }

        }

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
        fun forceUploadAttachmentListener(item: Item)
        fun openLinkedAttachmentListener(item: Item)
        fun deleteLocalAttachment(item: Item)
    }

    companion object {
        @JvmStatic
        fun newInstance(itemKey: String) = ItemAttachmentEntry(itemKey)
    }
}
