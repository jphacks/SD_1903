package com.example.durian

import android.graphics.Bitmap
import android.graphics.BitmapFactory

fun resizeBitmap(img: Bitmap, scale: Int = 800): Bitmap {
    val widthF = img.width.toFloat()
    val heightF = img.height.toFloat()
    when (widthF > heightF) {
        true -> return Bitmap.createScaledBitmap(img, scale, ((heightF / widthF) * scale.toFloat()).toInt(), true)
        false -> return Bitmap.createScaledBitmap(img, ((widthF / heightF) * scale.toFloat()).toInt(), scale, true)
    }
}


fun trimCenterBitmap(img: Bitmap): Bitmap {
    if (img.width > img.height) {
        val tmpImg = Bitmap.createBitmap(img, img.width/4, 0, img.height, img.height)
        return tmpImg
    } else {
        val tmpImg = Bitmap.createBitmap(img, 0, img.height/4, img.width, img.width)
        return tmpImg
    }
}