package com.mickstarify.zooforzotero.LibraryActivity.ItemViewFragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import androidx.fragment.app.Fragment
import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Creator

private const val ARG_CREATOR = "creator"

class ItemAuthorsEntry : Fragment() {
    private var creator: Creator? = null
    private var listener: OnFragmentInteractionListener? = null

    private val authorTypes = arrayOf("Author", "Contributor","Editor", "Series Editor", "Translator")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            creator = it.getParcelable<Creator>(ARG_CREATOR)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_item_authors_entry, container, false)
        val authorTypesAdapter = ArrayAdapter<String>(this.context!!, R.layout.simple_spinner_item, authorTypes)
        authorTypesAdapter.setDropDownViewResource(R.layout.simple_spinner_item)
        val creatorType = view.findViewById<Spinner>(R.id.spinner_creatorType)
        creatorType.setAdapter(authorTypesAdapter)

        val lastName = view.findViewById<EditText>(R.id.editText_lastname)
        val firstName = view.findViewById<EditText>(R.id.editText_firstname)

        lastName.setText(creator?.lastName?:"")
        firstName.setText(creator?.firstName?:"")

        return view
    }

    fun onButtonPressed(uri: Uri) {
        listener?.onFragmentInteraction(uri)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
//        if (context is OnFragmentInteractionListener) {
//            listener = context
//        } else {
//            throw RuntimeException(context.toString() + " must implement OnFragmentInteractionListener")
//        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }


    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        @JvmStatic
        fun newInstance(creator : Creator) =
            ItemAuthorsEntry().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CREATOR, creator)
                }
            }
    }
}
