package com.example.durian

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment

import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import java.io.*
import java.text.SimpleDateFormat
import java.util.*

class ScanActivity : AppCompatActivity() {

    private lateinit var detectionView: ImageView
    private lateinit var addMosaicButton: Button

    private val CAMERA_INTENT = 1

    private var path = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        detectionView = findViewById(R.id.detectionView)
        addMosaicButton = findViewById(R.id.addMosaicButton)

        val toStr = intent.getStringExtra("to")
        if (toStr == "CAMERA") {
            Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager)?.let {
                takePicture()
            } ?: turnBack("CAMERA")
        } else if (toStr == "PICTURES") {

        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == CAMERA_INTENT && resultCode == Activity.RESULT_OK) {
            val contentValues = ContentValues().apply {
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put("_data", path)
            }
            contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            try {
                val inputStream = FileInputStream(File(path))
                var bitmap = BitmapFactory.decodeStream(inputStream)
                // TODO 画像サイズ変えるかも
                detectionView.setImageBitmap(bitmap)

                val byteBuffer = ByteArrayOutputStream()
                bitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteBuffer)
                // TODO 画像解析リクエスト

            } catch (e: IOException) {
                e.printStackTrace()
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


    private fun turnBack(reason: String) {
        when (reason) {
            "CAMERA" -> {
                Toast.makeText(this, "カメラがありません", Toast.LENGTH_SHORT)
            }
        }
    }

}
