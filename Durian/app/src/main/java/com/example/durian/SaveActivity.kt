package com.example.durian

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.durian.SaveActivityComponents.SavedImagesAdapter
import java.net.URI

class SaveActivity : AppCompatActivity() {

    private lateinit var savedImagesRecyclerView: RecyclerView

    private val SELECTION_INTENT = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_save)

        savedImagesRecyclerView = findViewById(R.id.savedImagesRecyclerView)

        val toActivity = intent.getStringExtra("to") ?: ""
        when (toActivity) {
            "PICTURES" -> {
                // ギャラリーへのリクエスト
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                intent.type = "image/*"
                startActivityForResult(intent, SELECTION_INTENT)
            }
        }
    }


    // アクティビティのリザルト処理
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        when(requestCode) {
            SELECTION_INTENT -> {
                if (resultCode == Activity.RESULT_OK && data != null) {
                    // 選択された画像のUriを取得
                    val clipData = data.clipData
                    if (clipData != null) {
                        val count = clipData.itemCount

                        val uriList = mutableListOf<Uri>()
                        for (i in 0 until count) {
                            uriList.add(clipData.getItemAt(i).uri)
                        }

                        // 取得したUriリストをもとにアダプタセット
                        savedImagesRecyclerView.adapter = SavedImagesAdapter(this, uriList.toList(), savedImagesRecyclerView.width)
                        savedImagesRecyclerView.layoutManager = GridLayoutManager(this, 2)
                    } else {
                        Toast.makeText(this, "データが見つかりませんでした", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    finish()
                }
            }
        }
    }
}
