package com.example.durian

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.durian.Adapter.InformationAdapter

class InformationActivity : AppCompatActivity() {

    private lateinit var informationsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_information)

        informationsRecyclerView = findViewById(R.id.infoRecyclerView)
        informationsRecyclerView.adapter = InformationAdapter(this)
        informationsRecyclerView.layoutManager = GridLayoutManager(this, 1)

    }
}
