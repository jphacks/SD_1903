package com.example.durian.SaveActivityComponents

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.net.Uri
import android.os.Environment
import android.os.Handler
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.util.Size
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.durian.*
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*

class SavedImagesAdapter(val context: Context, val uriList: List<Uri>, val viewWidth: Int): RecyclerView.Adapter<SavedImagesAdapter.SavedImagesViewHolder>() {

    private val inflater = LayoutInflater.from(context)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SavedImagesViewHolder {
        val view = inflater.inflate(R.layout.saved_image_cell, parent, false)
        val viewHolder = SavedImagesViewHolder(view)
        viewHolder.savedCheckImageView.isVisible = false

        return viewHolder
    }

    override fun onBindViewHolder(holder: SavedImagesViewHolder, position: Int) {
        val metrics = context.getResources().getDisplayMetrics()

        val bitmap = MediaStore.Images.Media.getBitmap(context.contentResolver, uriList.get(position))
        holder.imageView.setImageBitmap(createThumbnail(bitmap, (viewWidth- (32.0f * metrics.density).toInt()) / 2))

        // TODO: 通信処理＆プログレスバー
        val handler = Handler()
        // BitmapをBytesBufferに変換
        val byteBuffer = ByteArrayOutputStream()
        val postBitmap = resizeBitmap(bitmap)
        postBitmap.compress(Bitmap.CompressFormat.JPEG, 100, byteBuffer)
        val url = URL("https://us-central1-crasproject.cloudfunctions.net/auto_mosaic")
        // JSONObject作成
        val postJson = JSONObject()
        postJson.put("img", Base64.encodeToString(byteBuffer.toByteArray(), Base64.DEFAULT))
        Thread {
            val resultJSONObj = cloudFunRequest(url, postJson.toString())
            if (resultJSONObj != null) {
                if (resultJSONObj.has("img")) {
                    // モザイク画像取得取得
                    val imgStr = resultJSONObj.getString("img")
                    val imgByte = Base64.decode(imgStr, Base64.DEFAULT)
                    val imgByteStream = ByteArrayInputStream(imgByte)
                    val getImageBitmap = BitmapFactory.decodeStream(imgByteStream)
                    handler.post {
                        holder.imageView.setImageBitmap(createThumbnail(getImageBitmap, (viewWidth- (32.0f * metrics.density).toInt()) / 2))
                        holder.progressBar.isVisible = false
                        // チェックアニメーション
                        holder.savedCheckImageView.isVisible = true
                        holder.savedCheckImageView.setImageResource(R.drawable.ok)
                        val icon = holder.savedCheckImageView.drawable
                        if (icon is AnimatedVectorDrawable){
                            icon.start()
                        }
                    }
                    // TODO：画像保存処理
                    val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.JAPAN).format(
                        Date()
                    )
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
                    val imgOutStream = FileOutputStream(file)
                    getImageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, imgOutStream)
                    // ギャラリーに登録
                    val contentValues = ContentValues().apply {
                        put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                        put("_data", file.absolutePath)
                    }
                    context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                } else {
                    handler.post {
                        Toast.makeText(context, "画像取得に失敗しました", Toast.LENGTH_SHORT).show()
                        holder.progressBar.isVisible = false
                    }
                }
            }
        }.start()
    }


    override fun getItemCount(): Int = uriList.count()


    class SavedImagesViewHolder(view: View): RecyclerView.ViewHolder(view) {
        val imageView = view.findViewById<ImageView>(R.id.savedImageCellImageView)
        val progressBar = view.findViewById<ProgressBar>(R.id.savedImageCellProgressBar)
        val savedCheckImageView = view.findViewById<ImageView>(R.id.savedCheckImageView)
    }
}