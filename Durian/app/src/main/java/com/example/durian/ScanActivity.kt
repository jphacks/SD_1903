package com.example.durian

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Base64
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class ScanActivity : AppCompatActivity() {

    private lateinit var detectionView: ImageView
    private lateinit var addMosaicButton: Button

    private val CAMERA_INTENT = 1
    private val SELECTION_INTENT = 2
    private val ADD_MOSAIC_INTENT = 3

    private var path = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        detectionView = findViewById(R.id.detectionView)
        addMosaicButton = findViewById(R.id.addMosaicButton)
        addMosaicButton.isEnabled = false

        val toStr = intent.getStringExtra("to")
        if (toStr == "CAMERA") {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager)?.let {
                takePicture()
            } ?: turnBack("CAMERA")
        } else if (toStr == "PICTURES") {
            val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
            intent.addCategory(Intent.CATEGORY_OPENABLE)
            intent.type = "image/*"
            startActivityForResult(intent, SELECTION_INTENT)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == ADD_MOSAIC_INTENT) {
            if (resultCode == Activity.RESULT_OK) {
                finish()
            }
            else if (resultCode == Activity.RESULT_CANCELED) {
                finish()
            }
        }

        if (requestCode == CAMERA_INTENT && resultCode == Activity.RESULT_OK) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put("_data", path)
            }
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            try {
                val inputStream = FileInputStream(File(path))
                var bitmap = BitmapFactory.decodeStream(inputStream)
                bitmap = resizeBitmap(bitmap)
                detectionView.setImageBitmap(bitmap)

                val byteBuffer = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteBuffer)
                visionAnnotation(byteBuffer.toByteArray())

            } catch (e: IOException) {
                e.printStackTrace()
            }
        }

        if (requestCode == SELECTION_INTENT && resultCode == Activity.RESULT_OK) {
            if (data != null) {
                val uri = data.data
                try {
                    var bitmap = MediaStore.Images.Media.getBitmap(contentResolver, uri)
                    bitmap = resizeBitmap(bitmap)
                    detectionView.setImageBitmap(bitmap)

                    val byteBuffer = ByteArrayOutputStream()
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteBuffer)
                    visionAnnotation(byteBuffer.toByteArray())

                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
        }
    }

    private fun takePicture() {
        val intent = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            addCategory(Intent.CATEGORY_DEFAULT)
            putExtra(MediaStore.EXTRA_OUTPUT, createSaveFileUri())
        }

        startActivityForResult(intent, CAMERA_INTENT)
    }

    private fun createSaveFileUri(): Uri {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN).format(Date())
        val imageFileName = "Durian" + timeStamp

        val storageDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM + "/Durian")
        if (!storageDir.exists()) {
            storageDir.mkdir()
        }
        val file = File.createTempFile(
            imageFileName, /* prefix */
            ".jpg", /* suffix */
            storageDir      /* directory */
        )
        path = file.absolutePath

        return FileProvider.getUriForFile(this, packageName+".fileprovider", file)
    }


    private fun enableMosaicButton(putStr: String) {
        addMosaicButton.isEnabled = true
        addMosaicButton.setOnClickListener {
            val intent = Intent(this, MosaicActivity::class.java)
            intent.putExtra("mosaic_points", putStr)
            startActivityForResult(intent, ADD_MOSAIC_INTENT)
        }
    }

    // アクティビティ終了メソッド
    private fun turnBack(reason: String) {
        when (reason) {
            "CAMERA" -> {
                Toast.makeText(this, "カメラがありません", Toast.LENGTH_SHORT)
            }
        }
    }

    private fun visionAnnotation(imgBytes: ByteArray) {
        val handler = Handler()

        val url = URL("https://us-central1-crasproject.cloudfunctions.net/privacy_scan")
        val postJson = JSONObject()
        postJson.put("img", Base64.encodeToString(imgBytes, Base64.DEFAULT))

        Thread {
            val resultJSONObj = cloudFunRequest(url, postJson.toString())

            if (resultJSONObj != null) {
                if (resultJSONObj.has("img")) {
                    val imgStr = resultJSONObj.getString("img")
                    val imgByte = Base64.decode(imgStr, Base64.DEFAULT)
                    val imgByteStream = ByteArrayInputStream(imgByte)
                    detectionView.setImageBitmap(BitmapFactory.decodeStream(imgByteStream))

                    val pref = getSharedPreferences("tmpShared", Context.MODE_PRIVATE)
                    pref.edit().putString("tmp_img", Base64.encodeToString(imgBytes, Base64.DEFAULT)).apply()
                }
                // TODO key is "statistics" etc...

                handler.post {
                    enableMosaicButton(resultJSONObj.getJSONArray("mosaic_points").toString())
                }

                Log.d("[LOG] - DEBUG", resultJSONObj.toString())
            }
        }.start()
    }
}
