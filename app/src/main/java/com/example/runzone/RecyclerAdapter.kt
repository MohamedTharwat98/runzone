package com.example.runzone

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView

class RecyclerAdapter(
    private var titles: List<String>,
    private var details: List<String>,
    private var images: List<Int>
) : RecyclerView.Adapter<RecyclerAdapter.ViewHolder>() {
    inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        val itemTitle: TextView = itemView.findViewById(R.id.home_list_item_title)
        val itemDetails: TextView = itemView.findViewById(R.id.home_list_item_details)
        val itemPicture: ImageView = itemView.findViewById(R.id.home_list_item_image)

        init {
            itemView.setOnClickListener { v: View ->
                val position: Int = adapterPosition
                Toast.makeText(
                    itemView.context,
                    "You clicked on item # ${position + 1}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.home_list_items, parent, false)
        return ViewHolder(v)
    }

    override fun getItemCount(): Int {
        return titles.size
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.itemTitle.text = titles[position]
        holder.itemDetails.text = details[position]
        holder.itemPicture.setImageResource(images[position])
    }

}