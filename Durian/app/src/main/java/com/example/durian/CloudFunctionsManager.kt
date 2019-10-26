package com.example.durian

import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.IOException
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL
import java.nio.charset.StandardCharsets

fun cloudFunRequest(url: URL, postStr: String): JSONObject? {
    try {
        // HTTPリクエストの準備
        val connection = url.openConnection() as HttpURLConnection
        connection.setRequestProperty("Content-Type", "application/json")
        connection.apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 60000
            instanceFollowRedirects = true
            doOutput = true
        }

        try {
            val outputStream = OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8)
            outputStream.write(postStr)
            outputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        connection.connect()

        if (connection.responseCode in 200 .. 299) {
            val reader: BufferedReader = BufferedInputStream(connection.inputStream).bufferedReader()
            val jsonText: String = reader.readLine()
            Log.d("[LOG] Response read", jsonText)
            val resJson = JSONObject(jsonText)
            reader.close()

            return resJson
        } else {
            Log.d("[LOG]- ERROR", "response failed. code = %d. message = %s".format(connection.responseCode, connection.responseMessage))
        }

    } catch (e: Exception) {
        e.printStackTrace()
    }

    return null
}

//fun visionAnnotation(imgBytes: ByteArray): JSONObject? {
//    val url: URL = URL("https://us-central1-crasproject.cloudfunctions.net/privacy_scan")
//
//    try {
//        // HTTPリクエストの準備
//        val connection = url.openConnection() as HttpURLConnection
//        connection.setRequestProperty("Content-Type", "application/json")
//        connection.apply {
//            requestMethod = "POST"
//            connectTimeout = 10000
//            readTimeout = 60000
//            instanceFollowRedirects = true
//            doOutput = true
//        }
//
//        // POSTするJSONを作成。OutputStreamに添付
//        val postJson = JSONObject()
//        postJson.put("img", Base64.encodeToString(imgBytes, Base64.DEFAULT))
//        try {
//            val outputStream = OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8)
//            outputStream.write(postJson.toString())
//            outputStream.flush()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//
//        connection.connect()
//
//        if (connection.responseCode in 200 .. 299) {
//            val reader: BufferedReader = BufferedInputStream(connection.inputStream).bufferedReader()
//            val jsonText: String = reader.readLine()
//            Log.d("[LOG] Response read", jsonText)
//            val resJson = JSONObject(jsonText)
//            reader.close()
//
//            return resJson
//        } else {
//            Log.d("[LOG]", "response failed. code = %d. message = %s".format(connection.responseCode, connection.responseMessage))
//        }
//
//    } catch (e: Exception) {
//        e.printStackTrace()
//    }
//
//    return null
//}



//fun mosaicRequest(imgBytes: ByteArray, mosaicPoints: String): JSONObject? {
//    val url: URL = URL("https://us-central1-crasproject.cloudfunctions.net/mosaic_process")
//
//    try {
//        // HTTPリクエストの準備
//        val connection = url.openConnection() as HttpURLConnection
//        connection.setRequestProperty("Content-Type", "application/json")
//        connection.apply {
//            requestMethod = "POST"
//            connectTimeout = 10000
//            readTimeout = 60000
//            instanceFollowRedirects = true
//            doOutput = true
//        }
//
//        // POSTするJSONを作成。OutputStreamに添付
//        val postJson = JSONObject()
//        postJson.put("img", Base64.encodeToString(imgBytes, Base64.DEFAULT))
//        postJson.put("mosaic_points", JSONArray(mosaicPoints))
//
//        try {
//            val outputStream = OutputStreamWriter(connection.outputStream, StandardCharsets.UTF_8)
//            outputStream.write(postJson.toString())
//            outputStream.flush()
//        } catch (e: IOException) {
//            e.printStackTrace()
//        }
//
//        connection.connect()
//
//        if (connection.responseCode in 200 .. 299) {
//            val reader: BufferedReader = BufferedInputStream(connection.inputStream).bufferedReader()
//            val jsonText: String = reader.readLine()
//            Log.d("[LOG] Response read", jsonText)
//            val resJson = JSONObject(jsonText)
//            reader.close()
//
//            return resJson
//        } else {
//            Log.d("[LOG]", "response code is faild. code = %d. message = %s".format(connection.responseCode, connection.responseMessage))
//        }
//
//    } catch (e: Exception) {
//
//    }
//
//    return null
//}