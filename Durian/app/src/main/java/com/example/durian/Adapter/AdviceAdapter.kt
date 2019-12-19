package com.example.durian.Adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import com.example.durian.R
import org.json.JSONArray


/*
TODOリスト用のアダプタークラス
 */
open class AdviceAdapter(val context: Context, val adviceJson: JSONArray): BaseAdapter() {

    /*
    プロパティ郡
     */
    protected val inflater = LayoutInflater.from(context)

    private data class ViewHolder(val cellView: View) {
        val tagView = cellView.findViewById<TextView>(R.id.tagTextView)
        val adviceTextView = cellView.findViewById<TextView>(R.id.adviceTextView)
    }

    private fun createCellView(parent: ViewGroup?): View {
        val cell_view:View = inflater.inflate(R.layout.advice_cell, parent, false)
        cell_view.tag = ViewHolder(cell_view)
        return cell_view
    }

    //--- 各Cellに対応するViewを返すメソッド ---//
    override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
        // セルViewを作成、または再利用
        val cell_view = convertView ?: createCellView(parent)
        // セルに必要なデータを取得
        val adviceData = getItem(position)
        // セルView内のUIにデータをセット
        val viewHolder = (cell_view.tag as ViewHolder)
        viewHolder.tagView.text = adviceData.getString(0)
        viewHolder.adviceTextView.text = adviceData.getString(1)

        // ビューを返却
        return cell_view
    }


    //--- 各Cellに使うデータを返すメソッド ---//
    override fun getItem(position: Int): JSONArray {
        // positionに相当するレコードのデータを取得
        return adviceJson[position] as JSONArray
    }


    //--- 各セルのIDを返すメソッド ---//
    override fun getItemId(position: Int): Long {
        return position.toLong()
    }


    //--- ListViewのセルの数を返すメソッド ---//
    override fun getCount(): Int {
        return adviceJson.length()
    }

}