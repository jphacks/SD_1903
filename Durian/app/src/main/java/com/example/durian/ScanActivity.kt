package com.example.durian

import android.graphics.Color
import android.graphics.Typeface
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Space
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
import com.github.mikephil.charting.renderer.CandleStickChartRenderer
import kotlinx.android.synthetic.main.activity_scan.*

class ScanActivity : AppCompatActivity() {

    // スタイルとフォントの設定
    private var mTypeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_scan)


        setupBarchart()
        chart.data = BarData()
    }

    private fun BarData(): BarData {
        val values = mutableListOf<BarEntry>()
        val faceval = 100f
        val pupilval = 100f
        val handval = 100f
        val charval = 100f
        val landval = 100f
        values.add(BarEntry(1f,faceval))
        values.add(BarEntry(2f,pupilval))
        values.add(BarEntry(3f,handval))
        values.add(BarEntry(4f,charval))
        values.add(BarEntry(5f,landval))

        val yVals = BarDataSet(values, "ユーザー全体の統計").apply {
            axisDependency = YAxis.AxisDependency.LEFT
            color = Color.GREEN
            setDrawIcons(false)
            setDrawValues(false)
        }

        val data = BarData(yVals)
        return data
    }

    private fun setupBarchart(){

        var xAxisValue = ArrayList<String>()
        xAxisValue.add("face")
        xAxisValue.add("pupil")
        xAxisValue.add("hand")
        xAxisValue.add("char")
        xAxisValue.add("landmark")

        chart.apply {
            description.isEnabled = false
            isScaleXEnabled = false
            setPinchZoom(false)
            setDrawGridBackground(false)


            legend.apply{
                formSize = 10f
                typeface = mTypeface
                textColor = Color.BLACK
                verticalAlignment = Legend.LegendVerticalAlignment.BOTTOM
                horizontalAlignment = Legend.LegendHorizontalAlignment.LEFT
                orientation = Legend.LegendOrientation.HORIZONTAL
                setDrawInside(false)
            }

            xAxis.apply {
                granularity = 1f
                isGranularityEnabled = true
                setCenterAxisLabels(true)
                setDrawGridLines(false)
                setDrawLabels(false)
                textSize = 9f

                position = XAxis.XAxisPosition.BOTTOM
                valueFormatter = IndexAxisValueFormatter(xAxisValue)

//                setLabelCount(5)
                mAxisMaximum = 12f
                setCenterAxisLabels(true)
                setAvoidFirstLastClipping(true)
                spaceMin = 4f
                spaceMax = 4f

                setVisibleXRangeMaximum(12f)
                setVisibleXRangeMinimum(12f)
                isDragEnabled = false
            }

            axisRight.isEnabled = false
            setScaleEnabled(true)
            axisLeft.apply {
                valueFormatter = LargeValueFormatter()
                setDrawGridLines(false)
            }



        }
    }
}
