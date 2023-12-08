package com.example.tender


import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SearchLocationFragment : DialogFragment() {
    companion object {
        val SEARCH_TERM_KEY = stringPreferencesKey("searchTerm")
        val LOCATION_KEY = stringPreferencesKey("location")
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_search_location, container, false)
        loadSavedPreferences(view)

        val etSearch = view.findViewById<EditText>(R.id.etSearch)
        val etLocation = view.findViewById<EditText>(R.id.etLocation)

        // Set OnEditorActionListener for etLocation
        val editorActionListener = TextView.OnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                sendBackResult(etSearch.text.toString(), etLocation.text.toString())
                true
            } else {
                false
            }
        }

        etSearch.setOnEditorActionListener(editorActionListener)
        etLocation.setOnEditorActionListener(editorActionListener)

        val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)
        btnConfirm.setOnClickListener {
            val search = etSearch.text.toString()
            val location = etLocation.text.toString()
            sendBackResult(search, location)
        }
        return view
    }

    private fun loadSavedPreferences(view: View) {
        CoroutineScope(Dispatchers.IO).launch {
            val preferences = DataStoreManager.getInstance(requireContext()).data.first()
            val savedSearchTerm = preferences[SEARCH_TERM_KEY] ?: ""
            val savedLocation = preferences[LOCATION_KEY] ?: ""

            withContext(Dispatchers.Main) {
                view.findViewById<EditText>(R.id.etSearch)?.setText(savedSearchTerm)
                view.findViewById<EditText>(R.id.etLocation)?.setText(savedLocation)
            }
        }
    }

    private fun sendBackResult(searchTerm: String, location: String) {
        CoroutineScope(Dispatchers.IO).launch {
            DataStoreManager.getInstance(requireContext()).edit { preferences ->
                preferences[SEARCH_TERM_KEY] = searchTerm
                preferences[LOCATION_KEY] = location
            }
            withContext(Dispatchers.Main) {
                (activity as? MainActivity)?.updateSearchCriteriaDataStore(searchTerm, location)
            }
        }
        dismiss()
    }
}
