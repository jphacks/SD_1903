package com.example.durian

import android.graphics.Bitmap

fun resizeBitmap(img: Bitmap, scale: Int = 1000): Bitmap {
    val widthF = img.width.toFloat()
    val heightF = img.height.toFloat()
    when (widthF > heightF) {
        true -> return Bitmap.createScaledBitmap(img, scale, ((heightF / widthF) * scale.toFloat()).toInt(), true)
        false -> return Bitmap.createScaledBitmap(img, ((widthF / heightF) * scale.toFloat()).toInt(), scale, true)
    }
}