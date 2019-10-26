package com.example.durian

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.util.Base64
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.URL

class MosaicActivity : AppCompatActivity() {

    private lateinit var mosaicView: ImageView
    private lateinit var saveButton: Button
    private lateinit var backButton: Button

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
