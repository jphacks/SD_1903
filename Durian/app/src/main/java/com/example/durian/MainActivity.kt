package com.example.durian

import android.Manifest
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
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

        // 通知＆定期ジョブを設定
        createChannel(this)

        val fetchJob = JobInfo.Builder(1, ComponentName(this, ScanJobService::class.java))
//            .setMinimumLatency(5000)
//            .setOverrideDeadline(10000)
            .setPeriodic(0)
            .setPersisted(true)
            .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            .build()

        getSystemService(JobScheduler::class.java).schedule(fetchJob)
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
            if ("pref_key" in inteEx.keySet()) {
                Log.d("[LOG] DEBUG", "get pref_key from notification")
                val nextIntent = Intent(this, ScanActivity::class.java)
                nextIntent.putExtra("pref_key", intent.getStringExtra("pref_key"))
                nextIntent.putExtra("to", "NOTIFICATION")
                startActivity(nextIntent)
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

