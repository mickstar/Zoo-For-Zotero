package com.mickstarify.zooforzotero.LibraryActivity

import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Collection
import com.mickstarify.zooforzotero.ZoteroStorage.Database.Item
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.temporal.ChronoField

class ZoteroItemListAdapter(val context: Context, var list: List<ListEntry>) : BaseAdapter() {

    fun updateItems(newList: List<ListEntry>) {
        list = newList
        this.notifyDataSetChanged()
    }

    override fun getView(i: Int, _convertView: View?, parent: ViewGroup?): View {
        val entry = getItem(i)
        // Check if an existing view is being reused, otherwise inflate the view
        val convertView: View = if (_convertView == null) {
            LayoutInflater.from(context).inflate(R.layout.zotero_list_item_adapter, parent, false)
        } else {
            _convertView
        }

        if (entry.isItem()) {
            initItem(entry.getItem(), convertView)
        } else {
            initCollection(entry.getCollection(), convertView)
        }

        return convertView
    }

    fun initItem(item: Item, view: View) {
        val lbl_title: TextView = view.findViewById(R.id.TextView_library_list_title) as TextView
        val author = view.findViewById<TextView>(R.id.TextView_library_list_author)
        val img: ImageView = view.findViewById(R.id.imageView_library_list_image) as ImageView
        var icon: Int = R.drawable.treeitem_2x
        when (item.itemType) {
            "note" -> {
                icon = R.drawable.note_24dp
            }
            "book" -> {
                icon = R.drawable.book_24dp
            }
            "bookSection" -> {
                icon = R.drawable.book_section_24dp
            }
            "journalArticle" -> {
                icon = R.drawable.journal_article_24dp
            }
            "magazineArticle" -> {
                icon = R.drawable.magazine_article_24dp
            }
            "newspaperArticle" -> {
                icon = R.drawable.newspaper_article_24dp
            }
            "thesis" -> {
                icon = R.drawable.thesis_24dp
            }
            "letter" -> {
                icon = R.drawable.letter_24dp
            }
            "manuscript" -> {
                icon = R.drawable.manuscript_24dp
            }
            "interview" -> {
                icon = R.drawable.interview_24dp
            }
            "film" -> {
                icon = R.drawable.film_24dp
            }
            "artwork" -> {
                icon = R.drawable.artwork_24dp
            }
            "webpage" -> {
                icon = R.drawable.web_page_24dp
            }
            "attachment" -> {
                icon = R.drawable.treeitem_attachment_web_link_2x
            }
            "report" -> {
                icon = R.drawable.report_24dp
            }
            "bill" -> {
                icon = R.drawable.bill_24dp
            }
            "case" -> {
                icon = R.drawable.case_24dp
            }
            "hearing" -> {
                icon = R.drawable.hearing_24dp
            }
            "patent" -> {
                icon = R.drawable.patent_24dp
            }
            "statute" -> {
                icon = R.drawable.statute_24dp
            }
            "email" -> {
                icon = R.drawable.email_24dp
            }
            "map" -> {
                icon = R.drawable.map_24dp
            }
            "blogPost" -> {
                icon = R.drawable.blog_post_24dp
            }
            "instantMessage" -> {
                icon = R.drawable.instant_message_24dp
            }
            "forumPost" -> {
                icon = R.drawable.forum_post_24dp
            }
            "audioRecording" -> {
                icon = R.drawable.audio_recording_24dp
            }
            "presentation" -> {
                icon = R.drawable.presentation_24dp
            }
            "videoRecording" -> {
                icon = R.drawable.video_recording_24dp
            }
            "tvBroadcast" -> {
                icon = R.drawable.tv_broadcast_24dp
            }
            "radioBroadcast" -> {
                icon = R.drawable.radio_broadcast_24dp
            }
            "podcast" -> {
                icon = R.drawable.podcast_24dp
            }
            "computerProgram" -> {
                icon = R.drawable.computer_program_24dp
            }
            "conferencePaper" -> {
                icon = R.drawable.conference_paper_24dp
            }
            "document" -> {
                icon = R.drawable.document_24dp
            }
            "encyclopediaArticle" -> {
                icon = R.drawable.encyclopedia_article_24dp
            }
            "dictionaryEntry" -> {
                icon = R.drawable.dictionary_entry_24dp
            }
        }
        img.setImageResource(icon)
//        if (item.itemType == "note") {
//            val note = item.data["note"]
//            if (note == null) {
//                lbl_title.text = "Unknown note"
//            } else {
//                // we want to subjugate longer strings with ellipses
//                val noteSubstring = note.take(20)
//                lbl_title.text = if (noteSubstring == note) {
//                    note
//                } else {
//                    "${noteSubstring}..."
//                }
//            }
//            lbl_title.text = item.data["note"] ?: "Note"
//        }
        lbl_title.text = item.getTitle()
        val date_value = item.getItemData("date") ?: ""
        val date = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            tryParse(date_value)
        } else {
            date_value
        }
        val journal_abbrev = item.data["journalAbbreviation"] ?: ""
        val journal_title = item.data["publicationTitle"] ?: ""
        val journal = if (journal_abbrev.isBlank()) journal_title else journal_abbrev
        author.text = "%s %s %s".format(item.getAuthor(), date, journal)
    }

    fun initCollection(collection: Collection, view: View) {
        val lbl_title: TextView = view.findViewById(R.id.TextView_library_list_title) as TextView
        val author = view.findViewById<TextView>(R.id.TextView_library_list_author)
        val img: ImageView = view.findViewById(R.id.imageView_library_list_image) as ImageView
        img.setImageResource(R.drawable.treesource_collection_2x)
        lbl_title.text = collection.name
        author.text = "Subcollection"
    }

    override fun getItem(position: Int): ListEntry {
        return list[position]
    }

    override fun getItemId(position: Int): Long {
//        return getItem(position).
        return 0
    }

    override fun getCount(): Int {
        return list.size
    }

}

@RequiresApi(Build.VERSION_CODES.O)
fun tryParse(dateString: String): String {
    //Here we try to extract the year from the dateString using several possible formats. This
    //could potentially be avoided if Zotero dates are parsed in an standarized way.
    val formatStrings: Array<String> = arrayOf(
        "yyyy", "yyyy/M", "yyyy-M", "yyyy-M-d", "yyyy/M/d", "yyyy/MM", "yyyy-M-d", "yyyy, M dd",
        "M/y", "M/d/y", "M-d-y", "M/d/yyy", "M d, yyyy", "MMM d, yyyy", "M-d",
        "M yyyy", "M/yyyy", "MMMM d, yyyy", "MMMM, yyyy",
        "MMMM yyyy", "yyyy, MMMM", "yyyy, MM",
        "dd/MMM/yyyy", "dd/MMMM/yyyy", "dd MMM yyyy", "dd MMMM yyyy")
    for (formatString in formatStrings) {
        try {
            val formatter = DateTimeFormatter.ofPattern(formatString)
            val date = formatter.parse(dateString)
            var year = ""
            if (date.isSupported(ChronoField.YEAR)) {
                year = date.get(ChronoField.YEAR).toString()
            }else{
                year = dateString
            }
            return year
        } catch (e: DateTimeParseException) {
            continue
        }
    }
    return dateString
}