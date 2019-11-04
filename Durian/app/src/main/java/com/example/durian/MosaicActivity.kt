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
import android.widget.*
import androidx.core.view.isVisible
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
    private lateinit var tabButton1: Button
    private lateinit var tabButton2: Button
    private lateinit var progressBar: ProgressBar

    private val REQUEST_SAVE_IMAGE = 1

    private var planeImageBitmap: Bitmap? = null
    private var mosaicImageBitmap: Bitmap? = null
    private var stampImageBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mosaic)

        mosaicView = findViewById(R.id.mosaicView)
        saveButton = findViewById(R.id.saveButton)
        backButton = findViewById(R.id.backButton)
        tabButton1 = findViewById(R.id.tabButton_1)
        tabButton2 = findViewById(R.id.tabButton_2)
        progressBar = findViewById(R.id.mosaicProgressBar)

        progressBar.isVisible = false

        // TabButtonの初期設定
        tabButton1.isEnabled = false
        tabButton2.isEnabled = false

        // SharedPreferencesから遷移前のデータ取得
        val pref = getSharedPreferences("tmpShared", Context.MODE_PRIVATE)
        val imgStr = pref.getString("tmp_img", "")
        if (imgStr != "") {
            val imgBytes =  Base64.decode(imgStr, Base64.DEFAULT)
            val bitmap = BitmapFactory.decodeByteArray(imgBytes, 0, imgBytes.size)
            mosaicView.setImageBitmap(bitmap)
            pref.edit().remove("tmp_img")
            planeImageBitmap = bitmap      // 元の画像データ -> グローバル

            val mosaicPoints = intent.getStringExtra("mosaic_points")
            mosaicProcess(imgBytes, mosaicPoints)
        } else {
            Log.d("[Log]", "shared pref has not tmp_img key.")
        }
    }

    override fun onResume() {
        super.onResume()

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
            finish()
        }

        tabButton1.setOnClickListener {
            // TODO: ImageView変更。
            if (mosaicImageBitmap != null) {
                mosaicView.setImageBitmap(mosaicImageBitmap)
            } else {
                Log.d("[LOG] - ERROR", "mosaicImageBitmap is not found.")
            }
            // tabButtonのisEnableの変更
            tabButton2.isEnabled = true
            tabButton1.isEnabled = false
            // 背景変更
            setBGTabButton1(TOGGLE.ON)
            setBGTabButton2(TOGGLE.OFF)
        }
        tabButton2.setOnClickListener {
            // TODO: ImageView変更。
            if (stampImageBitmap != null) {
                mosaicView.setImageBitmap(stampImageBitmap)
            } else {
                Log.d("[LOG] - ERROR", "stampImageBitmap is not found.")
            }
            // tabButtonのisEnableの変更
            tabButton1.isEnabled = true
            tabButton2.isEnabled = false
            // 背景変更
            setBGTabButton1(TOGGLE.OFF)
            setBGTabButton2(TOGGLE.ON)
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
        progressBar.isVisible = true

        val handler = Handler()
        Thread {
            val url = URL("https://us-central1-crasproject.cloudfunctions.net/mosaic_process")
            val postJson = JSONObject()
            postJson.put("img", Base64.encodeToString(imgBytes, Base64.DEFAULT))
            postJson.put("mosaic_points", JSONArray(mosaicPoints))

            val resultJson: JSONObject? = cloudFunRequest(url, postJson.toString())
            if (resultJson != null) {
                if (resultJson.has("img")) {
                    // モザイク画像取得取得
                    val imgStr = resultJson.getString("img")
                    val imgByte = Base64.decode(imgStr, Base64.DEFAULT)
                    val imgByteStream = ByteArrayInputStream(imgByte)
                    // スタンプ画像取得
                    val stampedImgStr = resultJson.getString("stamp_img")
                    val stampedImgByte = Base64.decode(stampedImgStr, Base64.DEFAULT)
                    val stampedImgByteStream = ByteArrayInputStream(stampedImgByte)
                    handler.post {
                        val getImageBitmap = BitmapFactory.decodeStream(imgByteStream)
                        mosaicImageBitmap = getImageBitmap   // モザイク画像データ -> グローバル
                        val getStampedImageBitmap = BitmapFactory.decodeStream(stampedImgByteStream)
                        stampImageBitmap = getStampedImageBitmap    // スタンプ画像データ -> グローバル

                        mosaicView.setImageBitmap(getImageBitmap)
                        progressBar.isVisible = false

                        // TabButtonの初期設定
                        tabButton1.isEnabled = false
                        setBGTabButton1(TOGGLE.ON)
                        tabButton2.isEnabled = true
                        setBGTabButton1(TOGGLE.OFF)
                    }
                } else {
                    Log.d("[Log] onActivityResult", "img key is not find.")
                }
            }
        }.start()
    }


    // タブボタン１の背景変更メソッド
    private fun setBGTabButton1(toggle: TOGGLE) {
        // ここにTabButton1の背景変更処理
        when (toggle) {
            TOGGLE.ON -> {
                // TODO: ONのとき
                Log.d("[LOG] - DEBUG", "TabButton1 is ON")
            }
            TOGGLE.OFF -> {
                // TODO: OFFのとき
                Log.d("[LOG] - DEBUG", "TabButton1 is OFF")
            }
        }

    }

    // タブボタン２の背景変更メソッド
    private fun setBGTabButton2(toggle: TOGGLE) {
        // ここにTabButton1の背景変更処理
        when (toggle) {
            TOGGLE.ON -> {
                // TODO: ONのとき (to 松崎)
                Log.d("[LOG] - DEBUG", "TabButton2 is ON")
            }
            TOGGLE.OFF -> {
                // TODO: OFFのとき (to 松崎)
                Log.d("[LOG] - DEBUG", "TabButton2 is OFF")
            }
        }
    }


    enum class TOGGLE {
        ON, OFF
    }
}
