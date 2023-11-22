package com.example.tender

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.gson.Gson

class BusinessDetailsFragment : DialogFragment() {

    private var business: Business? = null

    override fun onStart() {
        super.onStart()
        val metrics = resources.displayMetrics
        val width = (metrics.widthPixels * 0.9).toInt() // 90% of screen width
        val height = (metrics.heightPixels * 0.9).toInt() // 60% of screen height
        dialog?.window?.setLayout(width, height)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.getString("BUSINESS_DETAILS")?.let {
            business = Gson().fromJson(it, Business::class.java)
        }
    }
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_business_details, container, false)
        val tvBusinessName = view.findViewById<TextView>(R.id.tvBusinessNameDetails)
        val viewPager = view.findViewById<ViewPager2>(R.id.viewPagerImages)

        business?.let {
            tvBusinessName.text = it.name

            if (it.photos.isNotEmpty()) {
                logImageUrls(it.photos) // Log the URLs
                val adapter = ImageAdapter(it.photos)
                viewPager.adapter = adapter
            }
        }


        return view
    }

    private fun logImageUrls(urls: List<String>) {
        urls.forEach { url ->
            Log.d("BusinessDetailsFragment", "Photo URL: $url")
        }
    }

    companion object {
        fun newInstance(business: Business): BusinessDetailsFragment {
            val fragment = BusinessDetailsFragment()
            val args = Bundle()
            args.putString("BUSINESS_DETAILS", Gson().toJson(business))
            fragment.arguments = args
            return fragment
        }
    }
}
