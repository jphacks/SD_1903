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

    private val REQUEST_PERMISSION_WRITEFILE = 1
    private val REQUEST_PERMISSION_READFILE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cameraButton = findViewById(R.id.camraButton)
        pictureButton = findViewById(R.id.pictureButton)

        // ファイルのアクセス許可
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE), REQUEST_PERMISSION_WRITEFILE)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_PERMISSION_READFILE)
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

        if (requestCode == REQUEST_PERMISSION_WRITEFILE) {
            Log.d("[LOG - OK]", "getting WRITE_EXTERNAL_STORAGE")
        } else {
            Log.d("[LOG - FAILED]", "failed getting WRITE_EXTERNAL_STORAGE")
        }

        if (requestCode == REQUEST_PERMISSION_READFILE) {
            Log.d("[LOG - OK]", "getting READ_EXTERNAL_STORAGE")
        } else {
            Log.d("[LOG - FAILED]", "failed getting READ_EXTERNAL_STORAGE")
        }
    }
}

