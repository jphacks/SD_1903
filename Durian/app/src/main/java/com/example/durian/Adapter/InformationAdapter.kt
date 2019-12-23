package com.example.durian.Adapter

import android.content.Context
import android.net.Uri
import android.view.LayoutInflater
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.browser.customtabs.CustomTabsIntent
import androidx.recyclerview.widget.RecyclerView
import com.example.durian.R

class InformationAdapter(val context: Context): RecyclerView.Adapter<InformationAdapter.InfoCellViewHolder>() {

    private val adapterDatas = listOf<String>("プライバシーポリシー")
    private val inflater = LayoutInflater.from(context)

    class InfoCellViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        val cellTextView: TextView = view.findViewById(R.id.infoCellText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): InfoCellViewHolder {
        val view: View = inflater.inflate(R.layout.information_cell, parent, false)
        val viewHolder = InformationAdapter.InfoCellViewHolder(view)
        view.setOnClickListener {
            val position = viewHolder.adapterPosition
            when (position) {
                0 -> {
                    val intent = CustomTabsIntent.Builder().build()
                    intent.launchUrl(context, Uri.parse("https://storage.googleapis.com/cras_storage/StaticPage/PrivacyPolicy.html"))
                }
            }
        }
        return viewHolder
    }

    override fun getItemCount(): Int {
        return adapterDatas.count()
    }

    override fun onBindViewHolder(holder: InfoCellViewHolder, position: Int) {
        holder.cellTextView.setText(adapterDatas[position])
    }
}