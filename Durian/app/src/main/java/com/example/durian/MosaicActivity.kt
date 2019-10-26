package com.example.durian

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.ImageView

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


    }

    override fun onResume() {
        super.onResume()

        backButton.setOnClickListener {
            finish()
        }
    }
}
