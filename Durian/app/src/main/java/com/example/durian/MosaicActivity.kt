package com.example.durian

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.icu.text.SimpleDateFormat
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL
import java.util.*

class MosaicActivity : AppCompatActivity() {

    private lateinit var mosaicView: ImageView
    private lateinit var saveButton: Button
    private lateinit var backButton: Button

    private val REQUEST_SAVE_IMAGE = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mosaic)

        mosaicView = findViewById(R.id.mosaicView)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)

        val pref = getSharedPreferences("tmpShared", Context.MODE_PRIVATE)
        val imgStr = pref.getString("tmp_img", "")
        if (imgStr != "") {
            val imgBytes =  Base64.decode(imgStr, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
            mosaicView.setImageBitmap(bitmap)
            pref.edit().remove("tmp_img")

            val mosaicPoints = intent.getStringExtra("mosaic_points")
            mosaicProcess(imgBytes, mosaicPoints)
        } else {
            Log.d("[Log]", "shared pref has not tmp_img key.")
        }

        this.title = "モザイク"


        saveButton.setOnClickListener {
            if (saveButton.text == "TOPへ") {
                finish()
            } else {
                val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN).format(Date())
                val imageFileName = "Durian_" + timeStamp + ".jpg"

                val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
                    type = "image/jpeg"
                    putExtra(Intent.EXTRA_TITLE, imageFileName)
                }

                startActivityForResult(intent, REQUEST_SAVE_IMAGE)
            }
        }

        backButton.setOnClickListener {

        }
    }

    override fun onResume() {
        super.onResume()

        backButton.setOnClickListener {
            finish()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_SAVE_IMAGE && resultCode == Activity.RESULT_OK && data?.data != null) {
            val uri = data.data

            if (uri != null) {
                try {
                    val os = contentResolver.openOutputStream(uri)
                    (mosaicView.drawable as BitmapDrawable).bitmap.compress(Bitmap.CompressFormat.JPEG, 100, os)

                    val pref = getSharedPreferences("notifi_data", Context.MODE_PRIVATE)

                val cursor = contentResolver.query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, null, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC" + " LIMIT 1")
                if (cursor != null) {
                    if (cursor.moveToFirst()) {
                        pref.edit().putString("last_img_date", cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED)))
                        Log.d("[Log] last_img_data update", "to %s".format(cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATE_ADDED))))
                    }
                    cursor.close()
                }

                Toast.makeText(this, "画像を保存しました", Toast.LENGTH_SHORT).show()
                    saveButton.text = "TOPへ"
                    backButton.isEnabled = false
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "画像を保存できませんでした", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun mosaicProcess(imgBytes: ByteArray, mosaicPoints: String) {
        val handler = Handler()
        Thread {
            val url = URL("https://us-central1-crasproject.cloudfunctions.net/mosaic_process")
            val postJson = JSONObject()
            postJson.put("img", Base64.encodeToString(imgBytes, Base64.DEFAULT))
            postJson.put("mosaic_points", JSONArray(mosaicPoints))

            val resultJson: JSONObject? = cloudFunRequest(url, postJson.toString())
            if (resultJson != null) {
                if (resultJson.has("img")) {
                    val imgStr = resultJson.getString("img")
                    val imgByte = Base64.decode(imgStr, Base64.DEFAULT)
                    val imgByteStream = ByteArrayInputStream(imgByte)
                    handler.post {
                        mosaicView.setImageBitmap(BitmapFactory.decodeStream(imgByteStream))
                    }
                } else {
                    Log.d("[Log] onActivityResult", "img key is not find.")
                }
            }
        }.start()
    }
}
