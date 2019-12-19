package com.example.durian.Adapter

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.durian.R
import com.example.durian.SaveActivityComponents.SavedImagesAdapter
import kotlinx.android.synthetic.main.detect_labels_cell.view.*
import org.json.JSONArray
import org.json.JSONObject

class DetectLabelsAdapter(val context: Context, val detectLabelList: JSONArray): RecyclerView.Adapter<DetectLabelsAdapter.DetectLabelsViewHolder>() {
    private val inflater = LayoutInflater.from(context)


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetectLabelsViewHolder {
        val view = inflater.inflate(R.layout.detect_labels_cell, parent, false)
        val viewHolder = DetectLabelsAdapter.DetectLabelsViewHolder(view)

        return viewHolder
    }

    override fun getItemCount(): Int {
        return detectLabelList.length()
    }

    override fun onBindViewHolder(holder: DetectLabelsViewHolder, position: Int) {
        if (detectLabelList.getJSONObject(position).has("label")) {
            holder.labelTextView.text = detectLabelList.getJSONObject(position).getString("label")
        }
        if (detectLabelList.getJSONObject(position).getBoolean("check")) {
            holder.labelTextView.setTextColor(Color.argb(1.0f, 214.0f/255.0f, 48/255.0f, 49/255.0f)) //rgba(214, 48, 49,1.0)
//            holder.view.setBackgroundColor(Color.argb(1.0f, 250.0f/255.0f, 177/255.0f, 160/255.0f))    //#rgba(250, 177, 160,1.0)
            holder.labelTextView.setBackgroundColor(Color.argb(1.0f, 250.0f/255.0f, 177/255.0f, 160/255.0f))    //#rgba(250, 177, 160,1.0)
        } else {
            holder.labelTextView.setTextColor(Color.argb(1.0f, 0.0f/255.0f, 184/255.0f, 148/255.0f)) //rgba(0, 184, 148,1.0)
//            holder.view.setBackgroundColor(Color.argb(1.0f, 184.0f/255.0f, 233/255.0f, 148/255.0f))    //rgba(184, 233, 148,1.0)
            holder.labelTextView.setBackgroundColor(Color.argb(1.0f, 184.0f/255.0f, 233/255.0f, 148/255.0f))    //rgba(184, 233, 148,1.0)
        }
    }

    class DetectLabelsViewHolder(val view: View): RecyclerView.ViewHolder(view) {
        val labelTextView: TextView = view.findViewById(R.id.detectLabelsCellTextView)
    }

}