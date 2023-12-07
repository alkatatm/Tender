package com.example.tender

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class BusinessCardAdapter(
    private var businesses: List<Business>,
    private val onImageClicked: (Business) -> Unit
) : RecyclerView.Adapter<BusinessCardAdapter.ViewHolder>() {


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvBusinessName: TextView = view.findViewById(R.id.tvBusinessName)
        val tvBusinessImage: ImageView = view.findViewById(R.id.imageView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.layout_card, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val business = businesses[position]
        holder.tvBusinessName.text = business.name
        Glide.with(holder.itemView.context).load(business.image_url).into(holder.tvBusinessImage)

        holder.tvBusinessImage.setOnClickListener {
            onImageClicked(business)  // Invoke the callback function on image click
        }
    }

    fun getBusinessAt(position: Int): Business? {
        return if (position >= 0 && position < businesses.size) {
            businesses[position]
        } else null
    }

    fun updateData(newBusinesses: List<Business>) {
        businesses = newBusinesses
        notifyDataSetChanged()
    }
    override fun getItemCount(): Int = businesses.size
}

