package com.mickstarify.zooforzotero.adapters

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.mickstarify.zooforzotero.LibraryActivity.ListEntry
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item

class LibraryListRecyclerViewAdapter(
    var items: List<ListEntry>,
    val listener: LibraryListInteractionListener
) : RecyclerView.Adapter<LibraryListRecyclerViewAdapter.ListEntryViewHolder>() {
    class ListEntryViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageView = view.findViewById<ImageView>(R.id.imageView_library_list_image)
        val textView_title = view.findViewById<TextView>(R.id.TextView_library_list_title)
        val textView_author = view.findViewById<TextView>(R.id.TextView_library_list_author)
        val pdfImage = view.findViewById<ImageButton>(R.id.imageButton_library_list_attachment)
        val layout = view.findViewById<ConstraintLayout>(R.id.constraintLayout_library_list_item)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ListEntryViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.library_screen_list_item, parent, false)

        return ListEntryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ListEntryViewHolder,
        position: Int
    ) {

        val entry = items[position]
        if (entry.isItem()) {
            val item = entry.getItem()
            holder.textView_title.text = item.getTitle()
            holder.textView_author.visibility = View.VISIBLE
            holder.textView_author.text = item.getAuthor()
            val pdfAttachment = item.getPdfAttachment()
            if (pdfAttachment != null) {
                holder.pdfImage.visibility = View.VISIBLE
                holder.pdfImage.setOnClickListener {
                    Log.d("zotero", "Open Attachment ${item.getTitle()}")
                    listener.onItemAttachmentOpen(pdfAttachment)
                }
            } else {
                holder.pdfImage.visibility = View.GONE
            }
            holder.layout.setOnClickListener {
                Log.d("zotero", "Open Item")
                listener.onItemOpen(item)
            }

            val iconResource = when (item.itemType) {
                "note" -> {
                    R.drawable.note_24dp
                }
                "book" -> {
                    R.drawable.book_24dp
                }
                "bookSection" -> {
                    R.drawable.book_section_24dp
                }
                "journalArticle" -> {
                    R.drawable.journal_article_24dp
                }
                "magazineArticle" -> {
                    R.drawable.magazine_article_24dp
                }
                "newspaperArticle" -> {
                    R.drawable.newspaper_article_24dp
                }
                "thesis" -> {
                    R.drawable.thesis_24dp
                }
                "letter" -> {
                    R.drawable.letter_24dp
                }
                "manuscript" -> {
                    R.drawable.manuscript_24dp
                }
                "interview" -> {
                    R.drawable.interview_24dp
                }
                "film" -> {
                    R.drawable.film_24dp
                }
                "artwork" -> {
                    R.drawable.artwork_24dp
                }
                "webpage" -> {
                    R.drawable.web_page_24dp
                }
                "attachment" -> {
                    R.drawable.treeitem_attachment_web_link_2x
                }
                "report" -> {
                    R.drawable.report_24dp
                }
                "bill" -> {
                    R.drawable.bill_24dp
                }
                "case" -> {
                    R.drawable.case_24dp
                }
                "hearing" -> {
                    R.drawable.hearing_24dp
                }
                "patent" -> {
                    R.drawable.patent_24dp
                }
                "statute" -> {
                    R.drawable.statute_24dp
                }
                "email" -> {
                    R.drawable.email_24dp
                }
                "map" -> {
                    R.drawable.map_24dp
                }
                "blogPost" -> {
                    R.drawable.blog_post_24dp
                }
                "instantMessage" -> {
                    R.drawable.instant_message_24dp
                }
                "forumPost" -> {
                    R.drawable.forum_post_24dp
                }
                "audioRecording" -> {
                    R.drawable.audio_recording_24dp
                }
                "presentation" -> {
                    R.drawable.presentation_24dp
                }
                "videoRecording" -> {
                    R.drawable.video_recording_24dp
                }
                "tvBroadcast" -> {
                    R.drawable.tv_broadcast_24dp
                }
                "radioBroadcast" -> {
                    R.drawable.radio_broadcast_24dp
                }
                "podcast" -> {
                    R.drawable.podcast_24dp
                }
                "computerProgram" -> {
                    R.drawable.computer_program_24dp
                }
                "conferencePaper" -> {
                    R.drawable.conference_paper_24dp
                }
                "document" -> {
                    R.drawable.document_24dp
                }
                "encyclopediaArticle" -> {
                    R.drawable.encyclopedia_article_24dp
                }
                "dictionaryEntry" -> {
                    R.drawable.dictionary_entry_24dp
                }
                else -> {
                    R.drawable.treeitem_2x
                }
            }
            holder.imageView.setImageResource(iconResource)
        } else {
            val collection = entry.getCollection()
            holder.textView_title.text = collection.name
            holder.textView_author.visibility = View.GONE
            holder.pdfImage.visibility = View.GONE
            holder.imageView.setImageResource(R.drawable.treesource_collection_2x)
            holder.layout.setOnClickListener {
                Log.d("zotero", "Open Collection")
                listener.onCollectionOpen(collection)
            }
        }
    }

    override fun getItemCount(): Int {
        return items.size
    }
}

interface LibraryListInteractionListener {
    fun onItemOpen(item: Item)
    fun onCollectionOpen(collection: Collection)
    fun onItemAttachmentOpen(item: Item)
}