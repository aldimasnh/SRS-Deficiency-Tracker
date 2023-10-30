package com.srs.deficiencytracker.utilities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.Window
import android.view.WindowManager
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.constraintlayout.widget.ConstraintLayout
import com.android.volley.DefaultRetryPolicy
import com.android.volley.NetworkResponse
import com.android.volley.Response
import com.android.volley.VolleyError
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.google.android.material.button.MaterialButton
import com.srs.deficiencytracker.R
import com.srs.deficiencytracker.database.PemupukanSQL
import com.srs.deficiencytracker.database.PupukList
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.dialog_layout_success.view.btn_action
import kotlinx.android.synthetic.main.dialog_layout_success.view.btn_dismiss
import kotlinx.android.synthetic.main.dialog_layout_success.view.lottie_anim
import kotlinx.android.synthetic.main.dialog_layout_success.view.space
import kotlinx.android.synthetic.main.dialog_layout_success.view.tv_alert
import kotlinx.android.synthetic.main.loading_file_layout.view.logoFileLoader
import kotlinx.android.synthetic.main.loading_file_layout.view.lottieFileLoader
import kotlinx.android.synthetic.main.loading_file_layout.view.progressBarFileLoader
import kotlinx.android.synthetic.main.loading_file_layout.view.textViewFileLoader
import org.apache.commons.codec.binary.Hex
import org.apache.commons.codec.digest.DigestUtils
import org.json.JSONException
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Locale

private val url = "https://palmsentry.srs-ssms.com/files/"
private val urlMaps = "https://mobilepro.srs-ssms.com/config/plotMaps"

@Suppress("DEPRECATION")
class UpdateMan {

    @SuppressLint("SimpleDateFormat")
    fun setLastUpdateText(tv_tanggal: TextView, fileDir: String, context: Context) {
        val fileMain = File(fileDir)
        val sdf = SimpleDateFormat("dd-MMM-yyyy HH:mm")
        val lastModDate = sdf.format(fileMain.lastModified())
        tv_tanggal.text = "Up: $lastModDate"
        PrefManager(context).lastUpdate = lastModDate.toString()
    }

    @SuppressLint("SetTextI18n")
    @Suppress("RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS")
    fun loadFile(
        intentTo: Intent,
        urlCat: String,
        strEst: String,
        context: Context,
        loaderView: View,
        activity: String? = null
    ) {
        val prefManager = PrefManager(context)

        val urlCatMaps = "maps$urlCat"
        val urlCatPk = "pk$urlCat"

        val filePath = context.getExternalFilesDir(null)?.absolutePath + "/MAIN/"
        val f = File(filePath + urlCat)
        if ((!f.exists())) {
            loaderView.visibility = View.VISIBLE

            if (activity == "download") {
                updateListPupuk(context)
            }

            Glide.with(context)
                .load(R.drawable.logo_png_white)
                .into(loaderView.logoFileLoader)

            loaderView.lottieFileLoader.setAnimation("loading_circle.json")
            loaderView.lottieFileLoader.loop(true)
            loaderView.lottieFileLoader.playAnimation()

            val config = PRDownloaderConfig.newBuilder()
                .setReadTimeout(30000)
                .setConnectTimeout(30000)
                .build()
            PRDownloader.initialize(context.applicationContext, config)
            val zipFile = "$urlCat.zip"
            val url = "$url$urlCat".subSequence(0, ("$url$urlCat".length - 3))
                .toString() + "zip"
            @Suppress("UNUSED_VARIABLE") val downloadId =
                PRDownloader.download(url, filePath, zipFile)
                    .build()
                    .setOnStartOrResumeListener { }
                    .setOnPauseListener { }
                    .setOnCancelListener { }
                    .setOnProgressListener { progress ->
                        val progressPercent: Long =
                            progress.currentBytes * 100 / progress.totalBytes
                        Log.d("cek", "1")
                        loaderView.progressBarFileLoader.progress = progressPercent.toInt()
                        Log.d("cek", "2")
                        loaderView.textViewFileLoader.text =
                            "${getBytesToMBString(progress.currentBytes)} / ${
                                getBytesToMBString(progress.totalBytes)
                            }"
                        Log.d("cek", "3")
                        loaderView.progressBarFileLoader.isIndeterminate = false
                        Log.d("cek", "4")
                    }
                    .start(object : OnDownloadListener {
                        @RequiresApi(Build.VERSION_CODES.O)
                        override fun onDownloadComplete() {
                            try {
                                FileMan().unzip(filePath + zipFile, filePath)
                                Log.d("cek", "5")
                            } finally {
                                File(filePath + zipFile).delete()
                                Log.d("cek", "6")

                                checkMd5Maps(
                                    urlCat,
                                    intentTo,
                                    context,
                                    filePath,
                                    loaderView,
                                    prefManager.idReg.toString(),
                                    strEst
                                )
                                /*val fMaps = File(filePath + urlCatMaps)
                                val fPk = File(filePath + urlCatPk)
                                if (!fMaps.exists()) {

                                    if (!fPk.exists()) {
                                        checkMd5Pk(intentTo, urlCat, context, filePath, loaderView, strEst)
                                    } else {
                                        intentTo.putExtra("ViewType", "Online")
                                        context.startActivity(intentTo)
                                    }
                                } else {
                                    if (!fPk.exists()) {
                                        checkMd5Pk(intentTo, urlCat, context, filePath, loaderView, strEst)
                                    } else {
                                        intentTo.putExtra("ViewType", "Online")
                                        context.startActivity(intentTo)
                                    }
                                }*/
                            }
                        }

                        override fun onError(error: Error?) {
                            loaderView.visibility = View.GONE
                        }
                    })
        } else if (f.exists()) {
            intentTo.putExtra("ViewType", "Online")
            context.startActivity(intentTo)
        }
    }

    fun checkMd5Maps(
        urlCat: String,
        intentTo: Intent?,
        context: Context,
        filePath: String,
        loaderView: View,
        idReg: String,
        strEst: String,
        hexMapsApp: String? = "",
        hexPkApp: String? = ""
    ) {
        val strReq: StringRequest =
            @SuppressLint("SetTextI18n")
            object : StringRequest(
                Method.POST,
                "$urlMaps/checkMapsBlok1.php",
                Response.Listener { response ->
                    try {
                        val jObj = JSONObject(response)
                        val success = try {
                            jObj.getInt("success")
                        } catch (e: Exception) {
                            ""
                        }

                        if (success == 0) {
                            Log.d("logMaps", "${jObj.getString(Database.TAG_MESSAGE)}")
                        } else if (success == 1) {
                            if (hexMapsApp!!.isNotEmpty()) {
                                Toasty.success(context, jObj.getString(Database.TAG_MESSAGE)).show()
                            }

                            Log.d("logMaps", jObj.getString(Database.TAG_MESSAGE))
                        } else {
                            val prefManager = PrefManager(context)
                            prefManager.hexMaps = jObj.getString("hex")

                            val file = jObj.getString("file")
                            base64ToZip(file, filePath + "blok_maps.zip")

                            Log.d("logMaps", jObj.getString(Database.TAG_MESSAGE))

                            val totalBytes = response.length
                            if (totalBytes == totalBytes) {
                                try {
                                    FileMan().unzip(filePath + "blok_maps.zip", filePath)
                                } finally {
                                    File(filePath + "blok_maps.zip").delete()

                                    if (hexMapsApp!!.isNotEmpty()) {
                                        Toasty.success(
                                            context,
                                            jObj.getString(Database.TAG_MESSAGE)
                                        ).show()
                                    }
                                }
                            }
                        }

                        Handler().postDelayed({
                            checkMd5Pk(
                                intentTo,
                                urlCat,
                                context,
                                filePath,
                                loaderView,
                                strEst,
                                hexPkApp
                            )
                        }, 1000)
                    } catch (e: JSONException) {
                        Log.d("logMaps", "Data error, hubungi pengembang: $e")
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    Log.d("logMaps", "Terjadi kesalahan koneksi: $error")
                }) {

                override fun getParams(): Map<String, String> {
                    val params: MutableMap<String, String> =
                        java.util.HashMap()
                    params["reg"] = idReg
                    params["hexApp"] = hexMapsApp!!
                    return params
                }

                override fun deliverError(error: VolleyError) {
                    super.deliverError(error)
                    Log.d("logMaps", "Terjadi kesalahan koneksi deliver: $error")
                }

                override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                    val bytesRead = loaderView.progressBarFileLoader.progress + response.data.size
                    val totalBytes = response.data.size // You need to keep track of this value
                    val progress = (bytesRead.toFloat() / totalBytes * 100).toInt()

                    loaderView.progressBarFileLoader.progress = progress
                    loaderView.textViewFileLoader.text =
                        "${getBytesToMBString(bytesRead.toLong())} / ${getBytesToMBString(totalBytes.toLong())}"

                    return super.parseNetworkResponse(response)
                }
            }

        strReq.retryPolicy = DefaultRetryPolicy(
            180000,  // Socket timeout in milliseconds (60 seconds)
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(context).add(strReq)
    }

    fun checkMd5Pk(
        intentTo: Intent? = null,
        urlCat: String,
        context: Context,
        filePath: String,
        loaderView: View,
        selectedEst: String,
        hexMapsApp: String? = ""
    ) {
        val strReq: StringRequest =
            @SuppressLint("SetTextI18n")
            object : StringRequest(
                Method.POST,
                "$urlMaps/checkMapsPk1.php",
                Response.Listener { response ->
                    try {
                        val jObj = JSONObject(response)
                        val success = try {
                            jObj.getInt("success")
                        } catch (e: Exception) {
                            ""
                        }

                        if (success == 0) {
                            loaderView.visibility = View.GONE
                            Log.d("logMaps", "${jObj.getString(Database.TAG_MESSAGE)}")
                        } else if (success == 1) {
                            if (hexMapsApp!!.isNotEmpty()) {
                                Toasty.success(context, jObj.getString(Database.TAG_MESSAGE)).show()

                                if (intentTo != null) {
                                    intentTo.putExtra("ViewType", "Online")
                                    context.startActivity(intentTo)
                                }
                            }

                            loaderView.visibility = View.GONE
                            Log.d("logMaps", jObj.getString(Database.TAG_MESSAGE))
                        } else {
                            val prefManager = PrefManager(context)
                            prefManager.hexPk = jObj.getString("hex")

                            val file = jObj.getString("file")
                            base64ToZip(file, filePath + "yellow_trees.zip")

                            Log.d("logMaps", jObj.getString(Database.TAG_MESSAGE))

                            val totalBytes = response.length
                            if (totalBytes == totalBytes) {
                                try {
                                    FileMan().unzip(filePath + "yellow_trees.zip", filePath)
                                } finally {
                                    File(filePath + "yellow_trees.zip").delete()

                                    if (hexMapsApp!!.isNotEmpty()) {
                                        Toasty.success(
                                            context,
                                            jObj.getString(Database.TAG_MESSAGE)
                                        ).show()
                                    }

                                    loaderView.visibility = View.GONE

                                    if (intentTo != null) {
                                        intentTo.putExtra("ViewType", "Online")
                                        context.startActivity(intentTo)
                                    }
                                }
                            }
                        }
                    } catch (e: JSONException) {
                        loaderView.visibility = View.GONE
                        Log.d("logMaps", "Data error, hubungi pengembang: $e")
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    loaderView.visibility = View.GONE
                    Log.d("logMaps", "Terjadi kesalahan koneksi: $error")
                }) {

                override fun getParams(): Map<String, String> {
                    val params: MutableMap<String, String> =
                        java.util.HashMap()
                    params["hexApp"] = hexMapsApp!!
                    params["dataReg"] = urlCat
                    params["est"] = selectedEst
                    return params
                }

                override fun deliverError(error: VolleyError) {
                    super.deliverError(error)
                    loaderView.visibility = View.GONE
                    Log.d("logMaps", "Terjadi kesalahan koneksi deliver: $error")
                }

                override fun parseNetworkResponse(response: NetworkResponse): Response<String> {
                    val bytesRead = loaderView.progressBarFileLoader.progress + response.data.size
                    val totalBytes = response.data.size // You need to keep track of this value
                    val progress = (bytesRead.toFloat() / totalBytes * 100).toInt()

                    loaderView.progressBarFileLoader.progress = progress
                    loaderView.textViewFileLoader.text =
                        "${getBytesToMBString(bytesRead.toLong())} / ${getBytesToMBString(totalBytes.toLong())}"

                    return super.parseNetworkResponse(response)
                }
            }

        strReq.retryPolicy = DefaultRetryPolicy(
            180000,  // Socket timeout in milliseconds (60 seconds)
            DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
            DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        )

        Volley.newRequestQueue(context).add(strReq)
    }

    fun base64ToZip(base64Data: String, outputFilePath: String) {
        try {
            val decodedBytes = Base64.getDecoder().decode(base64Data)
            val outputStream = FileOutputStream(outputFilePath)
            outputStream.write(decodedBytes)
            outputStream.close()
            Log.d("logMaps", "Base64 data successfully converted to a zip file at $outputFilePath")
        } catch (e: Exception) {
            Log.e("logMaps", "Error converting base64 data to zip file: ${e.message}")
        }
    }

    fun updateListPupuk(context: Context) {
        val prefManager = PrefManager(context) //init shared preference
        val strReq: StringRequest =
            object : StringRequest(
                Method.POST,
                "https://srs-ssms.com/getListPupukParams.php",
                Response.Listener { response ->
                    try {
                        val jObj = JSONObject(response)
                        val success = jObj.getInt("status")

                        // Check for error node in json
                        val databaseHandler =
                            PemupukanSQL(context)
                        if (success == 1) {
                            val version = jObj.getInt("version")
                            databaseHandler.deleteDb()
                            val dataListPupukArray = jObj.getJSONObject("listPupuk")
                            val beforeSplitId = dataListPupukArray.getJSONArray("id")
                            val beforeSplitNama = dataListPupukArray.getJSONArray("nama")
                            Log.d("parsing", beforeSplitNama.toString())

                            var idArray = ArrayList<Int>()
                            for (i in 0 until beforeSplitId.length()) {
                                idArray.add(beforeSplitId.getInt(i))
                            }

                            var namaArray = ArrayList<String>()
                            for (i in 0 until beforeSplitNama.length()) {
                                namaArray.add(beforeSplitNama.getString(i))
                            }

                            var statusQuery = 1L
                            for (i in 0 until idArray.size) {
                                val status = databaseHandler.addPupuk(
                                    PupukList(
                                        db_id = idArray[i],
                                        db_pupuk = namaArray[i]
                                    )
                                )
                                if (status == 0L) {
                                    statusQuery = 0L
                                }
                            }

                            if (statusQuery > -1) {
                                Log.d("logPupuk", "${jObj.getString(Database.TAG_MESSAGE)}")
                                prefManager.version = version
                            } else {
                                Log.d("logPupuk", "Terjadi kesalahan, hubungi pengembang")
                                databaseHandler.deleteDb()
                            }
                        } else {
                            Log.d("logPupuk", "${jObj.getString(Database.TAG_MESSAGE)}")
                        }
                    } catch (e: JSONException) {
                        Log.d("logPupuk", "Data error, hubungi pengembang: $e")
                        e.printStackTrace()
                    }
                },
                Response.ErrorListener { error ->
                    Log.d("logPupuk", "Terjadi kesalahan koneksi")
                }) {
                override fun getParams(): Map<String, String> {
                    // Posting parameters to login url
                    val params: MutableMap<String, String> = HashMap()
                    params["version"] = prefManager.version.toString()
                    return params
                }
            }
        Volley.newRequestQueue(context).add(strReq)
    }

    private fun getBytesToMBString(bytes: Long): String? {
        return java.lang.String.format(Locale.ENGLISH, "%.2fMb", bytes / (1024.00 * 1024.00))
    }

    fun convertFileToByteArray(file: File): ByteArray {
        val fileInputStream = FileInputStream(file)
        val byteArray = ByteArray(file.length().toInt())

        try {
            fileInputStream.read(byteArray)
        } finally {
            fileInputStream.close()
        }

        return byteArray
    }

    fun md5Checksum(data: String? = "", byteArray: ByteArray? = null): String {
        var digest = ""
        val hexString = StringBuilder()
        if (data!!.isNotEmpty()) {
            try { // Define the data file path and create an InputStream object.
                val file = File(data)
                val `is`: InputStream = FileInputStream(file)
                // Calculates the MD5 digest of the given InputStream object.
                // It will generate a 32 characters hex string.
                digest = String(Hex.encodeHex(DigestUtils.md5(`is`)))
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            val md5Digest = MessageDigest.getInstance("MD5")
            val md5Bytes = md5Digest.digest(byteArray!!)
            for (byte in md5Bytes) {
                hexString.append(String.format("%02x", byte))
            }
        }
        return if (data.isNotEmpty()) digest else hexString.toString()
    }

    fun updater(
        jsonFile: String,
        context: Context,
        bt_update: MaterialButton,
        tv_tanggal: TextView,
        progressBarHolder: ConstraintLayout
    ) {
        if (bt_update.text == "UPDATE") {
            /*            UpdateMan().checkUpdate(context, jsonFile, progressBarHolder)*/
        } else if (bt_update.text == "UNDUH") {
            tv_tanggal.setTextColor(Color.RED)
            bt_update.text = context.getString(R.string.terbaru)
        }
    }

    fun checkUpdateYellow(intentTo: Intent? = null, context: Context, loaderView: View) {
        val prefManager = PrefManager(context)
        val urlCat = prefManager.dataReg!!
        val urlCatMaps = "maps$urlCat"
        val urlCatPk = "pk$urlCat"

        val connected: Boolean
        val connectivityManager: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connected =
            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state === NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state === NetworkInfo.State.CONNECTED

        if (connected) {
            loaderView.visibility = View.VISIBLE

            Glide.with(context)//GLIDE LOGO FOR LOADING LAYOUT
                .load(R.drawable.logo_png_white)
                .into(loaderView.logoFileLoader)

            loaderView.lottieFileLoader.setAnimation("loading_circle.json")//ANIMATION WITH LOTTIE FOR LOADING LAYOUT
            loaderView.lottieFileLoader.loop(true)
            loaderView.lottieFileLoader.playAnimation()
            loaderView.textViewFileLoader.text = ""

            val filePath = context.getExternalFilesDir(null)?.absolutePath + "/MAIN/"
            val fMaps = File(filePath + urlCatMaps)
            var md5ChecksumMaps = ""
            if (fMaps.exists()) {
                val byteArrayMaps = convertFileToByteArray(fMaps)
                md5ChecksumMaps = md5Checksum("", byteArrayMaps)
            }

            val fPk = File(filePath + urlCatPk)
            var md5ChecksumPk = ""
            if (fPk.exists()) {
                val byteArrayPk = convertFileToByteArray(fPk)
                md5ChecksumPk = md5Checksum("", byteArrayPk)
            }

            checkMd5Maps(
                urlCat,
                intentTo,
                context,
                filePath,
                loaderView,
                prefManager.idReg.toString(),
                prefManager.estYellow.toString(),
                md5ChecksumMaps,
                md5ChecksumPk
            )
        } else {
            AlertDialogUtility.alertDialog(
                context,
                "Jaringan anda tidak stabil, mohon hubungkan ke jaringan yang stabil!",
                "network_error.json"
            )
        }
    }

    fun checkUpdate(context: Context, loadingView: View, urlCat: String) {
        val filePath = context.getExternalFilesDir(null)?.absolutePath + "/CACHE/"
        val connected: Boolean
        val connectivityManager: ConnectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        connected =
            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state === NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state === NetworkInfo.State.CONNECTED
        val f = File(filePath + urlCat)
        if ((connected && !f.exists())) {
            loadingView.visibility = View.VISIBLE

            Glide.with(context)
                .load(R.drawable.logo_png_white)
                .into(loadingView.logoFileLoader)

            loadingView.lottieFileLoader.setAnimation("loading_circle.json")
            loadingView.lottieFileLoader.loop(true)
            loadingView.lottieFileLoader.playAnimation()
            loadingView.textViewFileLoader.text = ""

            updateListPupuk(context)

            val config = PRDownloaderConfig.newBuilder()
                .setReadTimeout(30000)
                .setConnectTimeout(30000)
                .build()
            PRDownloader.initialize(context, config)
            val dLId = PRDownloader.download(
                "${url}header$urlCat",
                filePath,
                "header$urlCat"
            )
                .build()
                .setOnStartOrResumeListener { }
                .setOnPauseListener { }
                .setOnCancelListener { }
                .setOnProgressListener { }
                .start(object : OnDownloadListener {
                    override fun onDownloadComplete() {
                        var cacheCheck: String? = ""
                        loadingView.visibility = View.GONE
                        val mainCheck =
                            UpdateMan().md5Checksum(context.getExternalFilesDir(null)?.absolutePath + "/MAIN/$urlCat")
                        try {
                            val charset: Charset = Charsets.UTF_8
                            val `is`: InputStream =
                                FileInputStream(context.getExternalFilesDir(null)?.absolutePath + "/CACHE/header$urlCat")
                            val size = `is`.available()
                            val buffer = ByteArray(size)
                            `is`.read(buffer)
                            `is`.close()
                            val json = String(buffer, charset)
                            val objCache = JSONObject(json)
                            val userArrayCache = objCache.getJSONArray("md5Hex")
                            cacheCheck = try {
                                (userArrayCache.getJSONObject(0).getJSONArray("hex").getString(0))
                            } catch (e: Exception) {
                                ""
                            }
                        } catch (e: Exception) {
                            Log.d("cek", e.toString())
                        }
                        if (cacheCheck == mainCheck) {
                            Log.d("cek", "SAMA")
                            val fDeleteDBC =
                                File(context.getExternalFilesDir(null)?.absolutePath + "/CACHE/" + "header$urlCat")
                            if (fDeleteDBC.exists()) {
                                fDeleteDBC.delete()
                            }
                            Toasty.success(context, "Data blok is up to date!").show()
                        } else if (cacheCheck != mainCheck) {
                            Log.d("cek", "BEDA")
                            val fDeleteDBC =
                                File(context.getExternalFilesDir(null)?.absolutePath + "/CACHE/" + "header$urlCat")
                            if (fDeleteDBC.exists()) {
                                fDeleteDBC.delete()
                            }
                            val layoutBuilder = LayoutInflater.from(context)
                                .inflate(R.layout.dialog_layout_success, null)
                            val builder: AlertDialog.Builder =
                                AlertDialog.Builder(context).setView(layoutBuilder)
                            val alertDialog: AlertDialog = builder.show()
                            alertDialog.window?.setBackgroundDrawableResource(R.drawable.background_white)
                            layoutBuilder.tv_alert.text = "TERDAPAT DATA UPDATE TERBARU"
                            layoutBuilder.lottie_anim.setAnimation("download.json")
                            layoutBuilder.lottie_anim.loop(true)
                            layoutBuilder.lottie_anim.playAnimation()
                            layoutBuilder.btn_action.visibility = View.VISIBLE
                            layoutBuilder.space.visibility = View.VISIBLE
                            layoutBuilder.btn_dismiss.text = "LAIN KALI"
                            layoutBuilder.btn_dismiss.setOnClickListener {
                                alertDialog.dismiss()
                            }
                            layoutBuilder.btn_action.setOnClickListener {
                                alertDialog.dismiss()
                                val fDelete =
                                    File(context.getExternalFilesDir(null)?.absolutePath + "/MAIN/" + urlCat)
                                if (fDelete.exists()) {
                                    fDelete.delete()
                                }
                                val filePath =
                                    context.getExternalFilesDir(null)?.absolutePath + "/MAIN/"
                                val f = File(filePath + urlCat)
                                if ((!f.exists())) {
                                    loadingView.visibility = View.VISIBLE
                                    Glide.with(context)
                                        .load(R.drawable.logo_png_white)
                                        .into(loadingView.logoFileLoader)

                                    loadingView.lottieFileLoader.setAnimation("loading_circle.json")
                                    loadingView.lottieFileLoader.loop(true)
                                    loadingView.lottieFileLoader.playAnimation()
                                    val config = PRDownloaderConfig.newBuilder()
                                        .setReadTimeout(30000)
                                        .setConnectTimeout(30000)
                                        .build()
                                    PRDownloader.initialize(context.applicationContext, config)
                                    val zipFile = "$urlCat.zip"
                                    val url = "$url$urlCat".subSequence(
                                        0,
                                        ("$url$urlCat".length - 3)
                                    )
                                        .toString() + "zip"
                                    Log.d("testzip", "zipfile: $zipFile || url: $url")
                                    val downloadId =
                                        PRDownloader.download(url, filePath, zipFile)
                                            .build()
                                            .setOnStartOrResumeListener { }
                                            .setOnPauseListener { }
                                            .setOnCancelListener { }
                                            .setOnProgressListener { progress ->
                                                val progressPercent: Long =
                                                    progress.currentBytes * 100 / progress.totalBytes

                                                loadingView.progressBarFileLoader.progress =
                                                    progressPercent.toInt()

                                                loadingView.textViewFileLoader.text =
                                                    "${getBytesToMBString(progress.currentBytes)} / ${
                                                        getBytesToMBString(progress.totalBytes)
                                                    }"

                                                loadingView.progressBarFileLoader.isIndeterminate =
                                                    false

                                            }
                                            .start(object : OnDownloadListener {
                                                @RequiresApi(Build.VERSION_CODES.O)
                                                override fun onDownloadComplete() {
                                                    try {
                                                        FileMan().unzip(
                                                            filePath + zipFile,
                                                            filePath
                                                        )

                                                    } finally {
                                                        File(filePath + zipFile).delete()

                                                        loadingView.visibility = View.GONE
                                                    }
                                                }

                                                override fun onError(error: Error?) {
                                                    Log.d("testzip", error.toString())
                                                }
                                            })
                                } else if (f.exists()) {
                                    //update
                                    //gagal, jalan intent.putExtra("ViewType", "Online")
                                }
                            }
                        }
                    }

                    override fun onError(error: Error) {
                        loadingView.visibility = View.GONE
                        AlertDialogUtility.alertDialog(
                            context,
                            "Terjadi kesalahan!!, ${error.serverErrorMessage} || ${error.isServerError} || ${error.responseCode} || ${error.connectionException}",
                            "warning.json"
                        )
                        Log.d(
                            "error download",
                            "${error.serverErrorMessage} || ${error.isServerError} || ${error.responseCode} || ${error.connectionException}"
                        )
                    }
                })
        } else {
            AlertDialogUtility.alertDialog(
                context,
                "Jaringan anda tidak stabil, mohon hubungkan ke jaringan yang stabil!",
                "network_error.json"
            )
        }
    }

    fun transparentStatusNavBar(window: Window) {
        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

}