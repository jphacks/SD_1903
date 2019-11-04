package com.example.durian.SaveActivityComponents

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.recyclerview.widget.RecyclerView
import com.example.durian.R
import com.example.durian.resizeBitmap
import com.example.durian.trimCenterBitmap

class SavedImagesAdapter(val context: Context, val uriList: List<Uri>, val viewWidth: Int): RecyclerView.Adapter<SavedImagesAdapter.SavedImagesViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedImagesViewHolder {
        val view = inflater.inflate(R.layout.saved_image_cell, parent, false)

        val viewHolder = SavedImagesViewHolder(view)
        return viewHolder
    }

    override fun onBindViewHolder(holder: SavedImagesViewHolder, position: Int) {
        val metrics = context.getResources().getDisplayMetrics()

        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uriList.get(position))
        var dispBitmap = trimCenterBitmap(bitmap)
        dispBitmap = resizeBitmap(dispBitmap, scale = (viewWidth- (32.0f * metrics.density).toInt()) / 2)   // サイズ調整
        holder.imageView.setImageBitmap(dispBitmap)
        // TODO: 通信処理＆プログレスバー
        Thread {

        }.start()
    }


    override fun getItemCount(): Int = uriList.count()


    class SavedImagesViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imageView = view.findViewById<ImageView>(R.id.savedImageCellImageView)
        val progressBar = view.findViewById<ProgressBar>(R.id.savedImageCellProgressBar)

    }
}