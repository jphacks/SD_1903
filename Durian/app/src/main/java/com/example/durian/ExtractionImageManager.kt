package com.example.durian

import android.content.Context
import android.media.Image
import android.text.Layout
import android.view.View
import android.widget.Button
import android.widget.ImageView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.constraintlayout.widget.ConstraintSet
import org.json.JSONObject


/*
* ExtractionImageManager
* 対象画像における選択可能箇所から任意の抽出部分を取得する管理クラス
* */

class ExtractionImageManager(val context: Context, val parentLayout: ConstraintLayout, val imageView: ImageView) {

    enum class Axis {
        X, Y
    }

    // 選択ボタンリスト
    private val selectionButton = mutableListOf<ButtonExtend>()
    // スキャン対象の画像Pixel
    var pixel_width: Int? = null
        get() {
            return field
        }
        set(value) {
            field = value
        }
    var pixel_height: Int? = null
        get() {
            return field
        }
        set(value) {
            field = value
        }
    // Aspect radio of Image ( width / height )
    val ImageAspectRatio: Float
        get() {
            return (pixel_width?.toFloat() ?: 1.0f) / (pixel_height?.toFloat() ?: 1.0f)
        }
    // Aspect radio of ImageView ( width / height )
    val ViewAspectRatio: Float
        get() {
            return imageView.width.toFloat() / imageView.height.toFloat()
        }


    // 選択ボタン追加
    fun addSelectionButton(button: ButtonExtend) {
        selectionButton.add(button)
    }

    // 選択されているボタンの取得メソッド
    fun pushingButtonList(): List<ButtonExtend> {
        val tmpList = mutableListOf<ButtonExtend>()
        for (button in this.selectionButton) {
            if (!button.isPushing) {
                tmpList.add(button)
            }
        }

        return tmpList.toList()
    }


    // 表示メソッド
    fun showSelection() {
        for (button in this.selectionButton) {
            val positionData: JSONObject = button.tag as JSONObject
            val top_x = positionData.getInt("top_x")
            val top_y = positionData.getInt("top_y")
            val end_x = positionData.getInt("end_x")
            val end_y = positionData.getInt("end_y")

            val button_width = end_x - top_x
            val button_height = end_y - top_y
            button.layoutParams = ConstraintLayout.LayoutParams(convertActualAccept(button_width, Axis.X), convertActualAccept(button_height, Axis.Y))

            // レイアウト設定
            parentLayout.addView(button)
            val constrainSet = ConstraintSet()
            constrainSet.clone(parentLayout)

            constrainSet.connect(button.id, ConstraintSet.LEFT, imageView.id, ConstraintSet.LEFT, convertActualOrigin(top_x, Axis.X))
            constrainSet.connect(button.id, ConstraintSet.TOP, imageView.id, ConstraintSet.TOP, convertActualOrigin(top_y, Axis.Y))

            constrainSet.applyTo(parentLayout)
        }
    }


    // 画像におけるピクセルサイズから、ディスプレイ上でのピクセルサイズ
    private fun convertActualAccept(value: Int, axis: Axis): Int {
        when (axis) {
            Axis.X -> {
                val disp_image_width = actualImgSize(Axis.X).toFloat()
                // 選択範囲のサイズ
                val relative_width = (value.toFloat() * (disp_image_width / (pixel_width?.toFloat() ?: disp_image_width))).toInt()
                return relative_width
            }

            Axis.Y -> {
                val disp_image_height = actualImgSize(Axis.Y).toFloat()
                // 選択範囲のサイズ
                val relative_height = (value.toFloat() * (disp_image_height / (pixel_height?.toFloat() ?: disp_image_height))).toInt()
                return relative_height
            }
        }
    }

    // 画像におけるピクセル座標から、ディスプレイ上でのピクセル座標
    private fun convertActualOrigin(value: Int, axis: Axis): Int {
        val size = convertActualAccept(value, axis)

        when (axis) {
            Axis.X -> {
                // ImageViewにおける画像の基準点
                val origin_img_left = if (ImageAspectRatio >= ViewAspectRatio) 0 else (imageView.width -  actualImgSize(Axis.X)) / 2
                return origin_img_left + size
            }

            Axis.Y -> {
                // ImageViewにおける画像の基準点
                val origin_img_top = if (ViewAspectRatio >= ImageAspectRatio) 0 else (imageView.height -  actualImgSize(Axis.Y)) / 2
                return origin_img_top + size
            }
        }
    }

    private fun actualImgSize(axis: Axis): Int {
        when (axis) {
            Axis.X -> {
                // ディスプレイ上の画像サイズ
                val disp_image_width = if (ImageAspectRatio >= ViewAspectRatio) {
                    imageView.width.toFloat()
                } else {
                    imageView.height.toFloat() * ((pixel_width?.toFloat() ?: imageView.width.toFloat()) / (pixel_height?.toFloat() ?: imageView.height.toFloat()))
                }
                return disp_image_width.toInt()
            }

            Axis.Y -> {
                // ディスプレイ上の画像サイズ
                val disp_image_height = if (ViewAspectRatio >= ImageAspectRatio) {
                    imageView.height.toFloat()
                } else {
                    imageView.width.toFloat() * ((pixel_height?.toFloat() ?: imageView.height.toFloat()) / (pixel_width?.toFloat() ?: imageView.width.toFloat()))
                }
                return disp_image_height.toInt()
            }
        }
    }
}