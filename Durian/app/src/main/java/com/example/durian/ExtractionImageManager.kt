package com.example.durian

import android.content.Context
import android.widget.Button
import android.widget.ImageView
import org.json.JSONObject


/*
* ExtractionImageManager
* 対象画像における選択可能箇所から任意の抽出部分を取得する管理クラス
* */

class ExtractionImageManager(val context: Context, val imageView: ImageView) {

    // 選択ボタンリスト
    private val selectionButton = mutableListOf<Button>()


    // 選択ボタン追加
    fun addSelectionButton(button: Button) {
        selectionButton.add(button)
    }


    fun showSelection() {
        for (button in this.selectionButton) {
            val positionData: JSONObject = button.tag as JSONObject
            val top_x = positionData.getInt("top_x")
            val top_y = positionData.getInt("top_y")
            val end_x = positionData.getInt("end_x")
            val end_y = positionData.getInt("end_y")

            // dp単位
            val scale = context.resources.displayMetrics.density
        }
    }


}