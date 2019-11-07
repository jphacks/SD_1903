package com.example.durian

import android.Manifest
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL

class ScanJobService(): JobService() {
    override fun onStopJob(p0: JobParameters?): Boolean {
        // 停止された場合、リトライポリシーに従って再スケジュールして欲しい場合はtrue
        return true
    }

    override fun onStartJob(p0: JobParameters?): Boolean {
        // 権限無ければ終了
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.d("[Log]", "No Permission")
            return false
        }

        // SharedPreferencesとCursor取得
        val pref = getSharedPreferences("notifi_data", Context.MODE_PRIVATE)
        val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC")
        var async_flag = false
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                // 最終日を取得
                var last_img_date = pref.getString("last_img_date", "") ?: ""
                for (i in 0 .. cursor.count) {
                    // 最後のデータがない場合は記録を取ってBreak
                    if (last_img_date == "") {
                        Log.d("[Log]", "last_img_date is not find")
                        last_img_date = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
                        pref.edit().putString("last_img_date", last_img_date).apply()
                        break
                    }

                    val presentImgDate = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))
                    if (i == 0) {
                        // ラストパスを更新
                        pref.edit().putString("last_img_date", cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))).apply()
                    }

                    if (presentImgDate.toInt() <= last_img_date.toInt()) {
                        // 最後のデータより追加日が前の場合
                        Log.d("[Log]", "last_img_path is not update")
                        break
                    } else {
                        Log.d("[Log]", "img scan -> added date of file = %s".format(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))))
                        Log.d("[Log]", "img scan -> path of file  = %s".format(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))))

                        // 画像を取得
                        val presentPath = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA))
                        val file = File(presentPath)
                        val uri = Uri.fromFile(file)
                        var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                        if (bitmap == null) {
                            return async_flag
                        }
                        bitmap = resizeBitmap(bitmap)

                        val byteBuffer = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteBuffer)
                        val handler = Handler()
                        val detectImgName = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME))
                        Thread {
                            val url = URL("https://us-central1-crasproject.cloudfunctions.net/privacy_scan")
                            val jsonObj = JSONObject()
                            jsonObj.put("img", Base64.encodeToString(byteBuffer.toByteArray(), Base64.DEFAULT))
                            val resultJson: JSONObject? = cloudFunRequest(url, jsonObj.toString())
                            if (resultJson != null) {
                                if (resultJson.has("img")) {
                                    val imgStr = resultJson.getString("img")
                                    val imgByte = Base64.decode(imgStr, Base64.DEFAULT)
                                    val imgByteStream = ByteArrayInputStream(imgByte)

                                    if (resultJson.has("checks")) {
                                        val checksObj = resultJson.getJSONObject("checks")
                                        if (checksObj.getBoolean("face") ||
                                            checksObj.getBoolean("pupil") ||
                                            checksObj.getBoolean("finger") ||
                                            checksObj.getBoolean("text") ||
                                            checksObj.getBoolean("landmark")) {
                                            handler.post {
                                                notifyWarning(this, detectImgName, bitmap)
                                            }
                                        }
                                    }
                                } else {
                                    Log.d("[Log] onActivityResult", "img key is not find.")
                                }
                            }
                        }.start()
                        async_flag = true
                    }

                    if (!cursor.moveToNext()) {
                        break
                    }
                }
            }

            cursor.close()
        } else {
            Log.d("[LOG] - ERROR", "cursor don't find")
        }
        // 処理が完了しているならばfalse
        // 別スレッドで処理されているならばtrue
        return async_flag
    }
}