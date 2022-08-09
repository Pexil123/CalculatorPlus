package com.aikyn.calculator

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide


class ImageAdapter(listImage: ArrayList<Image>, context: Context):
    RecyclerView.Adapter<ImageAdapter.ImageViewHolder>() {

    val list = listImage
    val cont = context

    class ImageViewHolder (view: View): RecyclerView.ViewHolder(view) {

        var imageHolder = view.findViewById<ImageView>(R.id.row_image)

        fun bind(image: Image, context: Context){

            Glide.with(context)
                .load(image.image_bitmap)
                .into(imageHolder)

            itemView.setOnClickListener {
                val i = Intent(context, ImageFullActivity::class.java)

                i.putExtra("name", image.image_name)
                context.startActivity(i)
            }

        }

    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val inflater = LayoutInflater.from(cont)
        return ImageViewHolder(inflater.inflate(R.layout.row_custom_recycler_item, parent, false))
    }

    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        holder.bind(list.get(position), cont)
    }

    override fun getItemCount(): Int {
        return list.size
    }
}