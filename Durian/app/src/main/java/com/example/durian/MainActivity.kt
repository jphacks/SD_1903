package com.example.durian

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class MainActivity : AppCompatActivity() {

    private lateinit var cameraButton: Button
    private lateinit var pictureButton: Button

    private val REQUEST_PERMISSION_CAMERA = 1
    private val REQUEST_PERMISSION_WRITEFILE = 2
    private val REQUEST_PERMISSION_READFILE = 3

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraButton = findViewById(R.id.camraButton)
        pictureButton = findViewById(R.id.pictureButton)

        // カメラ, ファイルのアクセス許可
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // パーミションを取得していない場合
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_CAMERA)
        }
    }

    override fun onResume() {
        super.onResume()

        cameraButton.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra("to", "CAMERA")
            startActivity(intent)
        }

        pictureButton.setOnClickListener {
            val intent = Intent(this, ScanActivity::class.java)
            intent.putExtra("to", "PICTURES")
            startActivity(intent)
        }


        // 通知からのIntent
        val inteEx = intent.extras
        if (inteEx != null) {
            if ("key" in inteEx.keySet()) {

            }
        }
    }

    // パーミッション取得結果
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            REQUEST_PERMISSION_CAMERA -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("[LOG - OK]", "getting CAMERA")
                } else {
                    Log.d("[LOG - OK]", "failed getting CAMERA")
                }
            }
            REQUEST_PERMISSION_WRITEFILE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("[LOG - OK]", "getting WRITE_EXTERNAL_STORAGE")
                } else {
                    Log.d("[LOG - FAILED]", "failed getting WRITE_EXTERNAL_STORAGE")
                }
            }
            REQUEST_PERMISSION_READFILE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d("[LOG - OK]", "getting READ_EXTERNAL_STORAGE")
                } else {
                    Log.d("[LOG - FAILED]", "failed getting READ_EXTERNAL_STORAGE")
                }
            }
        }
    }
}

