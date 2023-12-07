package com.example.tender

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class SimpleBusinessAdapter(private val businesses: List<Business>, private val onItemClicked: (Business) -> Unit)
    : RecyclerView.Adapter<SimpleBusinessAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBusinessNameSimple: TextView = view.findViewById(R.id.tvBusinessNameSimple)
        val imgYelpStars: ImageView = view.findViewById(R.id.imgYelpStarsSimple)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_business_simple, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val business = businesses[position]
        val nameParts = business.name.split(" ").take(2)  // Split the name and take the first two words
        val displayName = nameParts.joinToString(" ")  // Join them back into a string

        businesses.forEach { Log.d("Debug", "Business name: ${it.name}") }
        holder.tvBusinessNameSimple.text = displayName
        holder.imgYelpStars.setImageResource(getStarResource(business.rating))

        Log.d("SimpleBusinessAdapter", "Binding: ${business.name}, Rating: ${business.rating}")

        holder.itemView.setOnClickListener { onItemClicked(business) }
    }


    override fun getItemCount() = businesses.size

    private fun getStarResource(rating: Double): Int {
        return when (rating) {
            1.0 -> R.drawable.stars_regular_1
            1.5 -> R.drawable.stars_regular_1_half
            2.0 -> R.drawable.stars_regular_2
            2.5 -> R.drawable.stars_regular_2_half
            3.0 -> R.drawable.stars_regular_3
            3.5 -> R.drawable.stars_regular_3_half
            4.0 -> R.drawable.stars_regular_4
            4.5 -> R.drawable.stars_regular_4_half
            5.0 -> R.drawable.stars_regular_5
            else -> R.drawable.stars_regular_0 // Default or no rating
        }
    }
}
