package com.example.durian

import android.graphics.Bitmap
import android.graphics.BitmapFactory


// リサイズメソッド
fun resizeBitmap(img: Bitmap, scale: Int = 800): Bitmap {
    val widthF = img.width.toFloat()
    val heightF = img.height.toFloat()
    when (widthF > heightF) {
        true -> return Bitmap.createScaledBitmap(img, scale, ((heightF / widthF) * scale.toFloat()).toInt(), true)
        false -> return Bitmap.createScaledBitmap(img, ((widthF / heightF) * scale.toFloat()).toInt(), scale, true)
    }
}


// 画像の中央をトリム
fun trimCenterBitmap(img: Bitmap): Bitmap {
    if (img.width > img.height) {
        val tmpImg = Bitmap.createBitmap(img, (img.width-img.height)/2, 0, img.height, img.height)
        return tmpImg
    } else {
        val tmpImg = Bitmap.createBitmap(img, 0, (img.height-img.width)/2, img.width, img.width)
        return tmpImg
    }
}


// 画像から正方形のサムネイル作成
fun createThumbnail(img: Bitmap, size: Int = img.width): Bitmap {
    val trimedImg = trimCenterBitmap(img)
    val resizedImg = resizeBitmap(trimedImg, scale = size)   // プレビュー用サイズ調整
    return resizedImg
}