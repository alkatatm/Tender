package com.example.tender

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class LikedDislikedFragment : DialogFragment() {

    // Assuming BusinessAdapter is a RecyclerView.Adapter that takes a list of businesses
    private lateinit var likedAdapter: SimpleBusinessAdapter
    private lateinit var dislikedAdapter: SimpleBusinessAdapter
    private lateinit var dbHelper: DBHelper

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_liked_disliked, container, false)

        dbHelper = DBHelper(requireContext())

        val rvLiked = view.findViewById<RecyclerView>(R.id.rvLikedBusinesses)
        val rvDisliked = view.findViewById<RecyclerView>(R.id.rvDislikedBusinesses)

        // Initialize adapters with data from the database
        likedAdapter = SimpleBusinessAdapter(dbHelper.getBusinesses(1)) { business ->
            showBusinessDetails(business)
        }
        dislikedAdapter = SimpleBusinessAdapter(dbHelper.getBusinesses(0)) { business ->
            showBusinessDetails(business)
        }

        // Set up RecyclerViews
        rvLiked.apply {
            adapter = likedAdapter
            layoutManager = LinearLayoutManager(context)
        }
        rvDisliked.apply {
            adapter = dislikedAdapter
            layoutManager = LinearLayoutManager(context)
        }

        Log.d("LikedDislikedFragment", "Liked businesses count: ${likedAdapter.itemCount}")
        Log.d("LikedDislikedFragment", "Disliked businesses count: ${dislikedAdapter.itemCount}")
        return view
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog
        if (dialog != null) {
            val width = resources.displayMetrics.widthPixels * 0.9 // 90% of screen width
            val height = ViewGroup.LayoutParams.WRAP_CONTENT // Adjust the height as needed
            dialog.window?.setLayout(width.toInt(), height)
        }
    }

    private fun showBusinessDetails(business: Business) {
        // Code to show BusinessDetailsFragment with the clicked business
        BusinessDetailsFragment.newInstance(business).show(parentFragmentManager, "BusinessDetails")
    }

}

