package com.example.durian

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Icon
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import java.util.*

private val CHANNEL_ID = "ScanImage"
private val NOTIFICATION_ID = 1
private val REQUEST_CODE = 1
private val GROUP_KEY = "SCAN_IMAGES"

fun createChannel(context: Context) {
    // 通知チャンネル作成
    val channel = NotificationChannel(CHANNEL_ID, "スキャン通知", NotificationManager.IMPORTANCE_DEFAULT).apply {
        enableLights(false)
        enableVibration(true)
        setShowBadge(true)
    }

    val manager = context.getSystemService(NotificationManager::class.java)
    manager.createNotificationChannel(channel)
}

fun notifyWarning(context: Context, fileName:String, bitmap: Bitmap) {
    val uuid = UUID.randomUUID().hashCode()
    // 通知をタップしたときに起動する画面
    val intent = Intent(context, MainActivity::class.java)
    intent.putExtra("key", "hogehoge")
    val pendingIntent = PendingIntent.getActivity(context, REQUEST_CODE, intent, PendingIntent.FLAG_ONE_SHOT)

    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setContentTitle("画像に危険な情報が確認されました")
        .setContentText("ファイル名：%s".format(fileName))
        .setContentIntent(pendingIntent)    // 通知タップ時に起動するインテント
//        .setGroup(GROUP_KEY)
        .setStyle(NotificationCompat.BigPictureStyle().bigPicture(bitmap))
        .setLargeIcon(bitmap)
        .setGroupSummary(false)
        .setSmallIcon(R.mipmap.durian_launcher_foreground)
        .setAutoCancel(true)
        .build()

    NotificationManagerCompat.from(context).notify(uuid, notification)
}


fun notifySafety(context: Context, message:String) {

}