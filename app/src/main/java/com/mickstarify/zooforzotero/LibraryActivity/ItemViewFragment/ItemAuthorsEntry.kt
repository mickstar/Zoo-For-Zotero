package com.mickstarify.zooforzotero.LibraryActivity.ItemViewFragment

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Spinner
import androidx.core.view.get

import com.mickstarify.zooforzotero.R
import com.mickstarify.zooforzotero.ZoteroAPI.Model.Creator
import org.jetbrains.anko.Android
import java.util.ArrayList

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_CREATOR = "creator"

/**
 * A simple [Fragment] subclass.
 * Activities that contain this fragment must implement the
 * [ItemAuthorsEntry.OnFragmentInteractionListener] interface
 * to handle interaction events.
 * Use the [ItemAuthorsEntry.newInstance] factory method to
 * create an instance of this fragment.
 *
 */
class ItemAuthorsEntry : Fragment() {
    // TODO: Rename and change types of parameters
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

    // TODO: Rename method, update argument and hook method into UI event
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

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     *
     *
     * See the Android Training lesson [Communicating with Other Fragments]
     * (http://developer.android.com/training/basics/fragments/communicating.html)
     * for more information.
     */
    interface OnFragmentInteractionListener {
        // TODO: Update argument type and name
        fun onFragmentInteraction(uri: Uri)
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment ItemAuthorsEntry.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(creator : Creator) =
            ItemAuthorsEntry().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_CREATOR, creator)
                }
            }
    }
}
