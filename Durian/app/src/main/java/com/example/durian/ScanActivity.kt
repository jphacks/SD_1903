package com.example.durian

import android.app.Activity
import android.content.ContentValues
import android.content.Context
import android.content.Intent
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
import androidx.core.content.FileProvider
import androidx.core.view.isVisible
import org.json.JSONObject
import java.io.*
import java.net.URL
import java.text.SimpleDateFormat
import java.util.*
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.AnimatedVectorDrawable
import android.view.View
import android.widget.*
import androidx.core.view.setPadding
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.Legend
import com.github.mikephil.charting.components.LegendEntry
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.LargeValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.github.mikephil.charting.renderer.CandleStickChartRenderer
import kotlinx.android.synthetic.main.activity_scan.*


class ScanActivity : AppCompatActivity() {

    private lateinit var detectionView: ImageView
    private lateinit var addMosaicButton: Button
    private lateinit var progressBar: ProgressBar
    private lateinit var adviceListView: ListView

    private val CAMERA_INTENT = 1
    private val SELECTION_INTENT = 2
    private val ADD_MOSAIC_INTENT = 3

    private var path = ""

    // スタイルとフォントの設定
    private var mTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)

        detectionView = findViewById(R.id.detectionView)
        addMosaicButton = findViewById(R.id.addMosaicButton)
        addMosaicButton.isEnabled = false
        progressBar = this.findViewById(R.id.scanProgressBar)
        progressBar.isVisible = false
        adviceListView = findViewById(R.id.adviceListView)

        val toStr = intent.getStringExtra("to") ?: ""
        when (toStr) {
            "CAMERA" -> {
                // カメラへのリクエスト
                Intent(MediaStore.ACTION_IMAGE_CAPTURE).resolveActivity(packageManager)?.let {
                    takePicture()
                } ?: turnBack("CAMERA")
            }
            "PICTURES" -> {
                // ギャラリーへのリクエスト
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT)
                intent.addCategory(Intent.CATEGORY_OPENABLE)
                intent.type = "image/*"
                startActivityForResult(intent, SELECTION_INTENT)
            }
            "NOTIFICATION" -> {
                // 通知からのリクエスト
                // prefからBase64形式の画像を取得
                val pref = getSharedPreferences("tmpShared", Context.MODE_PRIVATE)
                // prefのkeyはintentから取得
                val prefKey = intent.getStringExtra("pref_key") ?: ""
                val imgStr = pref.getString(prefKey, "")
                if (imgStr != null) {
                    val imgBytes = Base64.decode(imgStr, Base64.DEFAULT)
                    val imgByteStream = ByteArrayInputStream(imgBytes)
                    val img = BitmapFactory.decodeStream(imgByteStream)
                    detectionView.setImageBitmap(img)

                    visionAnnotation(imgBytes)

                    pref.edit().remove(prefKey)
                }
            }
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

        if (requestCode == CAMERA_INTENT) {
            if (resultCode == Activity.RESULT_OK) {
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
            } else {
                finish()
            }
        }

        if (requestCode == SELECTION_INTENT) {
            if (resultCode == Activity.RESULT_OK) {
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
            } else {
                finish()
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
        progressBar.isVisible = true

        // anime start()
        val handler = Handler()

        val url = URL("https://us-central1-crasproject.cloudfunctions.net/privacy_scan")
        val postJson = JSONObject()
        postJson.put("img", Base64.encodeToString(imgBytes, Base64.DEFAULT))

        Thread {
            val resultJSONObj = cloudFunRequest(url, postJson.toString())

            // flag用のmap 右側にJSONの値
            val checkIdItem = listOf<ImageView>(faceCheck,pupilCheck,handCheck,charCheck,landmarkCheck)
            val checkMarkJSON = mutableListOf<Boolean>()

            if (resultJSONObj != null) {
                if (resultJSONObj.has("img")) {
                    val imgStr = resultJSONObj.getString("img")
                    val imgByte = Base64.decode(imgStr, Base64.DEFAULT)
                    val imgByteStream = ByteArrayInputStream(imgByte)
                    detectionView.setImageBitmap(BitmapFactory.decodeStream(imgByteStream))

                    val pref = getSharedPreferences("tmpShared", Context.MODE_PRIVATE)
                    pref.edit().putString("tmp_img", Base64.encodeToString(imgBytes, Base64.DEFAULT)).apply()
                }

//                if (resultJSONObj.has("statistics")) {
//                    val statisticsObj = resultJSONObj.getJSONObject("statistics")
//                    chart.data = BarData(
//                        statisticsObj.getInt("face").toFloat(),
//                        statisticsObj.getInt("pupil").toFloat(),
//                        statisticsObj.getInt("finger").toFloat(),
//                        statisticsObj.getInt("text").toFloat(),
//                        statisticsObj.getInt("landmark").toFloat())
//                }
                chart.data = barData(500f,450f,400f,350f,300f,250f,200f,150f,100f,50f , 0f)
                setupbarchart("Aa","Ba","Ca","Da","Ea","Fa","Ga","Ha","Ia","Ja","")

                if (resultJSONObj.has("checks")) {
                    val checksObj = resultJSONObj.getJSONObject("checks")
                    checkMarkJSON.add(!checksObj.getBoolean("face")) //face
                    checkMarkJSON.add(!checksObj.getBoolean("pupil"))  //pupil
                    checkMarkJSON.add(!checksObj.getBoolean("finger")) //hand
                    checkMarkJSON.add(!checksObj.getBoolean("text")) //char
                    checkMarkJSON.add(!checksObj.getBoolean("landmark"))//landmark
                }


                handler.post {
                    if (resultJSONObj.has("advice")) {
                        adviceListView.adapter = AdviceAdapter(this, resultJSONObj.getJSONArray("advice"))
                    }

                    enableMosaicButton(resultJSONObj.getJSONArray("mosaic_points").toString())
                    progressBar.isVisible = false

                    for (i in 0 until checkMarkJSON.size){
                        checkIdItem[i].setImageResource(if (checkMarkJSON[i]) {
                            R.drawable.ok
                        }else {
                            R.drawable.ng
                        })
                        var icon = checkIdItem[i].drawable
                        if (icon is AnimatedVectorDrawable){
                            icon.start()
                        }
                    }


                }
                Log.d("[LOG] - DEBUG", resultJSONObj.getJSONArray("advice").toString())
                Log.d("[LOG] - DEBUG", resultJSONObj.getJSONObject("statistics").toString())
            } else {
                handler.post {
                    Toast.makeText(this, "画像取得に失敗しました", Toast.LENGTH_SHORT).show()
                    progressBar.isVisible = false
                }
            }
        }.start()
    }

    private fun barData(item11: Float,
                        item10: Float,
                        item9: Float,
                        item8: Float,
                        item7: Float,
                        item6: Float,
                        item5: Float,
                        item4: Float,
                        item3: Float,
                        item2: Float,
                        item1: Float): BarData {
        val values = mutableListOf<BarEntry>()
        values.add(BarEntry(0f,item1))
        values.add(BarEntry(1f,item2))
        values.add(BarEntry(2f,item3))
        values.add(BarEntry(3f,item4))
        values.add(BarEntry(4f,item5))
        values.add(BarEntry(5f,item6))
        values.add(BarEntry(6f,item7))
        values.add(BarEntry(7f,item8))
        values.add(BarEntry(8f,item9))
        values.add(BarEntry(9f,item10))
        values.add(BarEntry(10f, item11))



        val yVals = BarDataSet(values,"").apply {
            setColors(Color.GREEN)
            axisDependency = YAxis.AxisDependency.RIGHT
            setDrawIcons(false)
            setDrawValues(true)
            setValues(values)
            valueTextSize = 12f
        }

        val data = BarData(yVals)
        data.barWidth = 0.7f
        return data
    }

    private fun setupbarchart(label_11:String,
                              label_10:String,
                              label_9:String,
                              label_8:String,
                              label_7:String,
                              label_6:String,
                              label_5:String,
                              label_4:String,
                              label_3:String,
                              label_2:String,
                              label_1:String){
        val xAxisValue = mutableListOf<String>()
        xAxisValue.add(label_1)
        xAxisValue.add(label_2)
        xAxisValue.add(label_3)
        xAxisValue.add(label_4)
        xAxisValue.add(label_5)
        xAxisValue.add(label_6)
        xAxisValue.add(label_7)
        xAxisValue.add(label_8)
        xAxisValue.add(label_9)
        xAxisValue.add(label_10)
        xAxisValue.add(label_11)

        chart.apply {
            description.isEnabled = false
            description.textSize = 0f
//            LargeValueFormatter()
            xAxis.axisMinimum = 0f
            xAxis.axisMaximum = 11f

            // ここのmaxYRangeの値を適度に変更すること！
            setVisibleYRange(0f, 700f ,YAxis.AxisDependency.LEFT)

            data.isHighlightEnabled = false
            invalidate()
            isScaleXEnabled = false
            setPinchZoom(false)
            setDrawGridBackground(false)
            setDrawValueAboveBar(true)


            legend.apply{
                isEnabled = false
            }

            xAxis.apply {
                granularity = 1f
                isGranularityEnabled = true
                setDrawGridLines(false)
                setDrawLabels(true)
                textSize = 12f

                position = XAxis.XAxisPosition.BOTTOM_INSIDE
                valueFormatter = IndexAxisValueFormatter(xAxisValue)

                setLabelCount(11)
                mAxisMinimum = 0f
                mAxisMaximum = 11f
//                mAxisRange = 12f
                setCenterAxisLabels(false)
                setAvoidFirstLastClipping(true)
                spaceMin  = 4f
                spaceMax = 4f
            }

            axisRight.isEnabled = false
            setScaleEnabled(false)
            axisRight.setDrawZeroLine(true)

            axisLeft.apply {
//                LargeValueFormatter()
                setDrawGridLines(true)
//                spaceTop = 1f
                axisMinimum = 0f
            }
        }
    }
}
