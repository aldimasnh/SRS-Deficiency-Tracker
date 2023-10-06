@file:Suppress("PrivatePropertyName")

package com.srs.deficiencytracker.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
import com.downloader.Error
import com.downloader.OnDownloadListener
import com.downloader.PRDownloader
import com.downloader.PRDownloaderConfig
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import com.srs.deficiencytracker.BuildConfig
import com.srs.deficiencytracker.MainActivity
import com.srs.deficiencytracker.R
import com.srs.deficiencytracker.database.PemupukanSQL
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_afdeling
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_app_ver
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_archive
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_blok
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_datetime
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_estate
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_id
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_idPk
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_jenisPupukID
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_komen
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_kondisi
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_photo
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_status
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_tabPkKuning
import com.srs.deficiencytracker.database.PupukList
import com.srs.deficiencytracker.database.ViewPkKuning
import com.srs.deficiencytracker.utilities.AlertDialogUtility
import com.srs.deficiencytracker.utilities.Database
import com.srs.deficiencytracker.utilities.FileMan
import com.srs.deficiencytracker.utilities.PrefManager
import com.srs.deficiencytracker.utilities.UpdateMan
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_list_upload.clLayoutUpload
import kotlinx.android.synthetic.main.activity_list_upload.header_list_upload
import kotlinx.android.synthetic.main.activity_list_upload.listViewUpload
import kotlinx.android.synthetic.main.activity_list_upload.llHeaderListUpload
import kotlinx.android.synthetic.main.activity_list_upload.loadingUpload
import kotlinx.android.synthetic.main.activity_list_upload.sd_list_upload
import kotlinx.android.synthetic.main.activity_list_upload.switchListUpload
import kotlinx.android.synthetic.main.dialog_layout_success.view.*
import kotlinx.android.synthetic.main.header.*
import kotlinx.android.synthetic.main.header.view.*
import kotlinx.android.synthetic.main.layout_list_header.view.cvDelHeader
import kotlinx.android.synthetic.main.loading_file_layout.view.logoFileLoader
import kotlinx.android.synthetic.main.loading_file_layout.view.lottieFileLoader
import kotlinx.android.synthetic.main.loading_file_layout.view.progressBarFileLoader
import kotlinx.android.synthetic.main.loading_file_layout.view.textViewFileLoader
import kotlinx.android.synthetic.main.loading_file_layout.view.tvHintFileLoader
import kotlinx.coroutines.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.Runnable
import java.lang.String.*
import java.nio.charset.Charset
import java.util.*

class PkKuningListActivity : AppCompatActivity() {

    //upload
    private val urlCekFoto = "https://srs-ssms.com/deficiency_tracker/checkFotoTracker.php"
    private val urlInsert = "https://srs-ssms.com/deficiency_tracker/postDataTracker2.php"
    var serverURL: String = "https://srs-ssms.com/deficiency_tracker/recordFotoTracker.php"
    private val client = OkHttpClient()

    private var urlCategory = "Jqayb4aORkQvs2oEa.KdQ"
    private val url = "https://palmsentry.srs-ssms.com/files/"

    private val uploadScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    var messageCheckFoto = ""
    var successResponse = 0
    var messageInsert = ""
    var successResponseInsert = 0

    var uploading = false
    var stoppedByConditions = false
    var shouldStop = false
    val handler = Handler()
    val delay = 5000
    private val timeOut = 600000
    private val runnableCode = object : Runnable {
        override fun run() {
            if (shouldStop) {
                return
            }

            if (uploading) {
                checkFotoArray()
            }

            if (successResponse == 2 && PemupukanSQL(this@PkKuningListActivity).setRecordPkKuning()
                    .toInt() <= 0
            ) {
                stoppedByConditions = true
                shouldStop = true
                uploading = false
                handler.removeCallbacks(this)

                if (messageCheckFoto.isNotEmpty()) {
                    if (successResponse == 0 || successResponse == 1) {
                        Toasty.warning(
                            this@PkKuningListActivity,
                            messageCheckFoto,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        Toasty.success(
                            this@PkKuningListActivity,
                            messageCheckFoto,
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    }

                    if (messageInsert.isNotEmpty()) {
                        handler.postDelayed({
                            if (successResponseInsert == 1) {
                                Toasty.success(
                                    this@PkKuningListActivity,
                                    messageInsert,
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            } else {
                                Toasty.warning(
                                    this@PkKuningListActivity,
                                    messageInsert,
                                    Toast.LENGTH_SHORT
                                )
                                    .show()
                            }
                        }, 1000)

                        checkUpdate()
                        UpdateMan().checkUpdateYellow(null, this@PkKuningListActivity, loadingUpload)
                    } else {
                        clLayoutUpload.visibility = View.GONE
                    }

                }
            }

            handler.postDelayed(this, delay.toLong())
        }
    }
    private val stopRunnable = Runnable {
        if (!stoppedByConditions) {
            shouldStop = true
            uploading = false
            handler.removeCallbacks(runnableCode)

            if (messageCheckFoto.isNotEmpty()) {
                if (successResponse == 0 || successResponse == 1) {
                    Toasty.warning(this, messageCheckFoto, Toast.LENGTH_SHORT).show()
                } else {
                    Toasty.success(this, messageCheckFoto, Toast.LENGTH_SHORT).show()
                }

                if (messageInsert.isNotEmpty()) {
                    handler.postDelayed({
                        if (successResponseInsert == 1) {
                            Toasty.success(this, messageInsert, Toast.LENGTH_SHORT).show()
                        } else {
                            Toasty.warning(this, messageInsert, Toast.LENGTH_SHORT).show()
                        }
                    }, 1000)

                    checkUpdate()
                    UpdateMan().checkUpdateYellow(null, this, loadingUpload)
                } else {
                    clLayoutUpload.visibility = View.GONE
                }
            }
        }
    }

    private val arrayMissPhoto = ArrayList<String>()
    val arrayCheckFoto = ArrayList<String>()
    val idArray = ArrayList<Int>()
    val idPkArray = ArrayList<Int>()
    val estArray = ArrayList<String>()
    val afdArray = ArrayList<String>()
    val blokArray = ArrayList<String>()
    val statusArray = ArrayList<String>()
    val kondisiArray = ArrayList<String>()
    val datetimeArray = ArrayList<String>()
    val jenisPupukArray = ArrayList<String>()
    val photoArray = ArrayList<String>()
    val komenArray = ArrayList<String>()
    val appVerArray = ArrayList<String>()

    var getArchive = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateMan().hideStatusNavigationBar(window)
        setContentView(R.layout.activity_list_upload)
        urlCategory = PrefManager(this).dataReg!!
        tv_ver_header.text = "App ver: ${BuildConfig.VERSION_NAME}"
        tv_tanggal.text = "Up: ${PrefManager(this).lastUpdate}"
        utils()

        getArchive = try {
            intent.getStringExtra("archive")!!
        } catch (e: Exception) {
            "0"
        }

        loadingUpload.tvHintFileLoader.text = "Mohon ditunggu, sedang memproses"
        val archiveNotEmpty = getArchive.isNotEmpty()
        if (archiveNotEmpty || PemupukanSQL(this).setRecordPkKuning().toInt() > 0) {
            switchListUpload.isChecked = archiveNotEmpty && getArchive.toInt() == 1
            llHeaderListUpload.cvDelHeader.visibility =
                if (archiveNotEmpty && getArchive.toInt() == 1) View.GONE else View.VISIBLE
            sd_list_upload.visibility =
                if (archiveNotEmpty && getArchive.toInt() == 1) View.GONE else View.VISIBLE
            makeList(if (archiveNotEmpty) getArchive.toInt() else 0)
            sdInit()
        }

        switchListUpload.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                llHeaderListUpload.cvDelHeader.visibility = View.GONE
                sd_list_upload.visibility = View.GONE
                makeList(1)
            } else {
                llHeaderListUpload.cvDelHeader.visibility = View.VISIBLE
                sd_list_upload.visibility = View.VISIBLE
                makeList(0)
            }
        }

        Glide.with(this)//GLIDE LOGO FOR LOADING LAYOUT
            .load(R.drawable.logo_png_white)
            .into(loadingUpload.logoFileLoader)
        Glide.with(this)//GLIDE LOGO FOR LOADING LAYOUT
            .load(R.drawable.ssms_green)
            .into(loadingUpload.logoFileLoader)
        loadingUpload.lottieFileLoader.setAnimation("loading_circle.json")//ANIMATION WITH LOTTIE FOR LOADING LAYOUT
        @Suppress("DEPRECATION")
        loadingUpload.lottieFileLoader.loop(true)
        loadingUpload.lottieFileLoader.playAnimation()
    }

    private fun utils() {
        header_list_upload.icLocation.visibility = View.GONE
        header_list_upload.tvKeluar.visibility = View.GONE
        header_list_upload.tvUser.text =
            "${PrefManager(this).name}" //setting nama dari shared preferences
    }

    private fun makeList(archive: Int? = 0) {
        val databaseHandler = PemupukanSQL(this)
        val arrayList: ArrayList<ViewPkKuning> = databaseHandler.viewListPkKuning(archive)

        val id = ArrayList<Int>()
        val idPk = ArrayList<Int>()
        val est = ArrayList<String>()
        val afd = ArrayList<String>()
        val blok = ArrayList<String>()
        val status = ArrayList<String>()
        val kondisi = ArrayList<String>()
        val datetime = ArrayList<String>()

        for (e in arrayList) {
            if (!id.contains(e.db_id)) {
                id.add(e.db_id)
                idPk.add(e.db_idPk)
                est.add(e.db_estate)
                afd.add(e.db_afdeling)
                blok.add(e.db_blok)
                status.add(e.db_status)
                kondisi.add(e.db_kondisi)
                datetime.add(e.db_datetime)
            }
        }

        val myListAdapter = PkKuningListAdapter(
            this, id, idPk, est, afd, blok, status, kondisi, datetime, archive
        )
        listViewUpload.adapter = myListAdapter
    }

    private fun sdInit() {
        createSD(
            "Upload",
            R.id.upload_data,
            R.drawable.ic_cloud_upload_black_24dp,
            R.color.ssmsPantone
        )

        sd_list_upload.setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.upload_data -> {
                    val connected: Boolean
                    val connectivityManager: ConnectivityManager =
                        this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
                    connected =
                        connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state === NetworkInfo.State.CONNECTED ||
                                connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state === NetworkInfo.State.CONNECTED

                    if (connected) {
                        if (PemupukanSQL(this).setRecordPkKuning().toInt() > 0) {
                            post()
                        } else {
                            Toasty.warning(this, "Tidak ada data dalam list!!", Toast.LENGTH_SHORT)
                                .show()
                        }
                    } else {
                        AlertDialogUtility.alertDialog(
                            this,
                            "Jaringan anda tidak stabil, mohon hubungkan ke jaringan yang stabil!",
                            "network_error.json"
                        )
                    }
                    sd_list_upload.close()
                    return@OnActionSelectedListener true
                }
            }
            false
        })
    }

    @Suppress("DEPRECATION")
    fun createSD(string: String, id: Int, drawable: Int, color: Int) {
        sd_list_upload.addActionItem(
            SpeedDialActionItem.Builder(id, drawable)
                .setLabel(string)
                .setFabBackgroundColor(resources.getColor(color))
                .setFabImageTintColor(resources.getColor(R.color.white))
                .create()
        )
    }

    @Suppress(
        "RECEIVER_NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS",
        "NULLABILITY_MISMATCH_BASED_ON_JAVA_ANNOTATIONS"
    )
    @SuppressLint("SimpleDateFormat", "SetTextI18n")
    private fun post() {
        arrayMissPhoto.clear()
        arrayCheckFoto.clear()
        idArray.clear()
        idPkArray.clear()
        estArray.clear()
        afdArray.clear()
        blokArray.clear()
        appVerArray.clear()
        statusArray.clear()
        kondisiArray.clear()
        datetimeArray.clear()
        jenisPupukArray.clear()
        photoArray.clear()
        komenArray.clear()
        val selectQuery =
            "SELECT * FROM $db_tabPkKuning WHERE $db_archive = '0'"
        val db = PemupukanSQL(this).readableDatabase
        val c: Cursor?
        try {
            c = db.rawQuery(selectQuery, null)
            if (c.moveToFirst()) {
                do {
                    idArray.add(getData(db_id, c).toInt())
                    idPkArray.add(getData(db_idPk, c).toInt())
                    estArray.add(getData(db_estate, c))
                    afdArray.add(getData(db_afdeling, c))
                    blokArray.add(getData(db_blok, c))
                    statusArray.add(getData(db_status, c))
                    kondisiArray.add(getData(db_kondisi, c))
                    datetimeArray.add(getData(db_datetime, c))
                    jenisPupukArray.add(getData(db_jenisPupukID, c))
                    photoArray.add(getData(db_photo, c))
                    val arrayFoto =
                        getData(db_photo, c).split("$")
                    for (a in arrayFoto.indices) {
                        if (arrayFoto[a].isNotEmpty()) {
                            if (!arrayCheckFoto.contains(arrayFoto[a])) {
                                arrayCheckFoto.add(arrayFoto[a])
                            }
                        }
                    }
                    komenArray.add(getData(db_komen, c))
                    appVerArray.add(getData(db_app_ver, c))
                } while (c!!.moveToNext())
            }
        } catch (e: SQLiteException) {
            Log.e("ET", "Error: $e")
        }

        for (p in arrayCheckFoto.indices) {
            if (arrayCheckFoto[p].isNotEmpty() || arrayCheckFoto[p] != "") {
                if (!isFileExists(arrayCheckFoto[p])) {
                    arrayMissPhoto.add(arrayCheckFoto[p])
                }
            }
        }

        shouldStop = false
        stoppedByConditions = false
        successResponse = 0
        successResponseInsert = 0
        messageCheckFoto = ""
        messageInsert = ""
        if (arrayMissPhoto.isNotEmpty()) {
            AlertDialogUtility.withCheckBox(
                this,
                "Batal",
                "Tetap Upload",
                "List foto tidak tersedia:",
                arrayMissPhoto.toTypedArray().contentToString().replace("[", "").replace("]", "")
                    .replace(",", "\n")
                    .replace("\"", "").replace(" ", ""),
                "Ada foto yang hilang. Apakah yakin ingin melanjutkan?",
                "Saya mengerti dan ingin melanjutkan",
                {
                    AlertDialogUtility.withCheckBox(
                        this,
                        "OK",
                        "",
                        "List foto tidak tersedia:",
                        arrayMissPhoto.toTypedArray().contentToString().replace("[", "")
                            .replace("]", "").replace(",", "\n")
                            .replace("\"", "").replace(" ", ""),
                        "Harap mengambil foto ulang dan rename sesuai dengan pemberitahuan berikut.",
                        "",
                        null,
                        null,
                        true
                    )
                },
                {
                    uploading = true
                    clLayoutUpload.visibility = View.VISIBLE
                    arrayCheckFoto.removeAll(arrayMissPhoto.toSet())

                    handler.postDelayed(runnableCode, delay.toLong())
                    handler.postDelayed(stopRunnable, timeOut.toLong())
                }
            )
        } else {
            uploading = true
            clLayoutUpload.visibility = View.VISIBLE
            handler.postDelayed(runnableCode, delay.toLong())
            handler.postDelayed(stopRunnable, timeOut.toLong())
        }
        Log.d("fileupload", "success response: $successResponse")
    }

    override fun onBackPressed() {
        if (!uploading) {
            val intent = Intent(this@PkKuningListActivity, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    @SuppressLint("Range")
    private fun getData(str: String, cursor: Cursor): String {
        return try {
            cursor.getString(cursor.getColumnIndex(str))
        } catch (e: Exception) {
            ""
        }
    }

    private fun upload(
        idUp: Int,
        idPkUp: Int,
        estUp: String,
        afdUp: String,
        blokUp: String,
        statusUp: String,
        kondisiUp: String,
        dateUp: String,
        jenisPupukUp: String,
        photoUp: String,
        komenUp: String,
        appVerUp: String
    ) {
        val postRequest: StringRequest = object : StringRequest(
            Method.POST, urlInsert,
            Response.Listener { response ->
                try {
                    val jObj = JSONObject(response)
                    messageInsert = try {
                        jObj.getString("message")
                    } catch (e: Exception) {
                        e.toString()
                    }
                    successResponseInsert = jObj.getInt("success")
                    Log.d(
                        "fileupload",
                        "upload data -- m: $messageInsert, s: $successResponseInsert"
                    )
                    if (successResponseInsert == 1) {
                        var updatedRows = 100
                        try {
                            val db = PemupukanSQL(this).readableDatabase
                            val values = ContentValues()
                            values.put(db_archive, 1)

                            updatedRows =
                                db.update(
                                    db_tabPkKuning,
                                    values,
                                    "id" + "=?",
                                    arrayOf(idUp.toString())
                                )

                            db.close()
                        } catch (e: Exception) {
                            messageInsert = "Tidak dapat update database, code: $updatedRows"
                            Log.e("fileupload", "Tidak dapat update database, code: $updatedRows")
                        }
                        makeList(0)
                    }
                } catch (e: JSONException) {
                    messageInsert = "Failed to parse server response: ${e.message}"
                    Log.e("fileupload", "Failed to parse server response: ${e.message}")
                }
            },
            Response.ErrorListener {
                messageInsert = "Terjadi kesalahan koneksi: $it"
                Log.e("fileupload", "Terjadi kesalahan koneksi: $it")
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> =
                    HashMap()
                params["jabatan"] = PrefManager(this@PkKuningListActivity).jabatan!!
                params["petugas"] = PrefManager(this@PkKuningListActivity).name!!
                params[db_idPk] = idPkUp.toString()
                params[db_estate] = estUp
                params[db_afdeling] = afdUp
                params[db_blok] = blokUp
                params[db_status] = statusUp
                params[db_kondisi] = kondisiUp
                params[db_datetime] = dateUp
                params[db_jenisPupukID] = jenisPupukUp
                params[db_photo] = photoUp
                params[db_komen] = komenUp
                params[db_app_ver] = appVerUp
                return params
            }
        }
        val queue = Volley.newRequestQueue(this)
        queue.cache.clear()
        queue.add(postRequest)
    }

    private fun checkFotoArray() {
        if (shouldStop) {
            return
        }

        val postRequest: StringRequest = object : StringRequest(
            Method.POST, urlCekFoto,
            Response.Listener { response ->
                try {
                    Log.d("fileupload", "response full: $response")
                    val jObj = JSONObject(response)
                    messageCheckFoto = try {
                        jObj.getString("message")
                    } catch (e: Exception) {
                        "error $urlCekFoto, error code:$e"
                    }
                    successResponse = jObj.getInt("success")
                    if (successResponse == 1) {
                        val photoArrStr = jObj.getString("listfoto")
                        val photoArr =
                            photoArrStr.replace("[", "").replace("]", "").split(",")

                        for (p in photoArr.indices) {
                            if (photoArr[p].isNotEmpty() || photoArr[p] != "") {
                                val myDir =
                                    File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString())
                                val imageData =
                                    File(myDir, photoArr[p].replace("\"", "").replace(" ", ""))
                                uploadScope.launch {
                                    try {
                                        uploadFileNew(imageData)
                                    } catch (e: Exception) {
                                        Log.e("UploadFile", e.toString())
                                    }
                                }
                            }
                        }
                    } else if (successResponse == 2) {
                        for (i in idArray.indices) {
                            upload(
                                idUp = idArray[i],
                                idPkUp = idPkArray[i],
                                estUp = estArray[i],
                                afdUp = afdArray[i],
                                blokUp = blokArray[i],
                                statusUp = statusArray[i],
                                kondisiUp = kondisiArray[i],
                                dateUp = datetimeArray[i],
                                jenisPupukUp = jenisPupukArray[i],
                                photoUp = photoArray[i],
                                komenUp = komenArray[i],
                                appVerUp = appVerArray[i]
                            )
                        }
                    }
                } catch (e: JSONException) {
                    messageCheckFoto = "Failed to parse server response: ${e.message}"
                    Log.e("fileupload", "Failed to parse server response: ${e.message}")
                }
            },
            Response.ErrorListener {
                //TODO
                messageCheckFoto = "Terjadi kesalahan:$it"
                Log.e("fileupload", "Terjadi kesalahan:$it")
            }
        ) {
            override fun getParams(): Map<String, String> {
                val params: MutableMap<String, String> = HashMap()

                val arrayCheckFotoStr =
                    arrayCheckFoto.toTypedArray().contentToString().replace("[", "")
                        .replace("]", "").replace(",", ";").replace(" ", "")
                params["foto"] = arrayCheckFotoStr
                Log.d("fileupload", "file yg dicheck: $arrayCheckFotoStr")

                return params
            }
        }
        val queue = Volley.newRequestQueue(this)
        queue.cache.clear()
        queue.add(postRequest)
    }

    private fun checkUpdate() {
        val filePath = this.getExternalFilesDir(null)?.absolutePath + "/CACHE/"
        val connected: Boolean
        val connectivityManager: ConnectivityManager =
            this.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        connected =
            connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE)!!.state === NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI)!!.state === NetworkInfo.State.CONNECTED
        val f = File(filePath + urlCategory)
        if ((connected && !f.exists())) {
            updateListPupuk()

            val config = PRDownloaderConfig.newBuilder()
                .setReadTimeout(30000)
                .setConnectTimeout(30000)
                .build()
            PRDownloader.initialize(this as Activity, config)
            val dLId = PRDownloader.download(
                "${url}header$urlCategory",
                filePath,
                "header$urlCategory"
            )
                .build()
                .setOnStartOrResumeListener { }
                .setOnPauseListener { }
                .setOnCancelListener { }
                .setOnProgressListener { }
                .start(object : OnDownloadListener {
                    @SuppressLint("SetTextI18n")
                    override fun onDownloadComplete() {
                        var cacheCheck: String? = ""
                        val mainCheck =
                            UpdateMan().md5Checksum(getExternalFilesDir(null)?.absolutePath + "/MAIN/$urlCategory")
                        try {
                            val charset: Charset = Charsets.UTF_8
                            val `is`: InputStream =
                                FileInputStream(getExternalFilesDir(null)?.absolutePath + "/CACHE/header$urlCategory")
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
                                File(getExternalFilesDir(null)?.absolutePath + "/CACHE/" + "header$urlCategory")
                            if (fDeleteDBC.exists()) {
                                fDeleteDBC.delete()
                            }
                        } else {
                            Log.d("cek", "BEDA")
                            val fDeleteDBC =
                                File(getExternalFilesDir(null)?.absolutePath + "/CACHE/" + "header$urlCategory")
                            if (fDeleteDBC.exists()) {
                                fDeleteDBC.delete()
                            }
                            val layoutBuilder = LayoutInflater.from(this@PkKuningListActivity)
                                .inflate(R.layout.dialog_layout_success, null)
                            val builder: AlertDialog.Builder =
                                AlertDialog.Builder(this@PkKuningListActivity).setView(layoutBuilder)
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
                                clLayoutUpload.visibility = View.VISIBLE
                                alertDialog.dismiss()
                                val fDelete =
                                    File(getExternalFilesDir(null)?.absolutePath + "/MAIN/" + urlCategory)
                                if (fDelete.exists()) {
                                    fDelete.delete()
                                }
                                val fileP =
                                    getExternalFilesDir(null)?.absolutePath + "/MAIN/"
                                val f = File(fileP + urlCategory)
                                if ((!f.exists())) {
                                    clLayoutUpload.visibility = View.VISIBLE
                                    val config = PRDownloaderConfig.newBuilder()
                                        .setReadTimeout(30000)
                                        .setConnectTimeout(30000)
                                        .build()
                                    PRDownloader.initialize(applicationContext, config)
                                    val zipFile = "$urlCategory.zip"
                                    val url = "$url$urlCategory".subSequence(
                                        0,
                                        ("$url$urlCategory".length - 3)
                                    )
                                        .toString() + "zip"
                                    Log.d("testzip", "zipfile: $zipFile || url: $url")
                                    val downloadId =
                                        PRDownloader.download(url, fileP, zipFile)
                                            .build()
                                            .setOnStartOrResumeListener { }
                                            .setOnPauseListener { }
                                            .setOnCancelListener { }
                                            .setOnProgressListener { progress ->
                                                val progressPercent: Long =
                                                    progress.currentBytes * 100 / progress.totalBytes

                                                loadingUpload.progressBarFileLoader.visibility = View.VISIBLE
                                                loadingUpload.progressBarFileLoader.progress =
                                                    progressPercent.toInt()

                                                loadingUpload.textViewFileLoader.text =
                                                    "${getBytesToMBString(progress.currentBytes)} / ${
                                                        getBytesToMBString(progress.totalBytes)
                                                    }"

                                                loadingUpload.progressBarFileLoader.isIndeterminate = false

                                            }
                                            .start(object : OnDownloadListener {
                                                @RequiresApi(Build.VERSION_CODES.O)
                                                override fun onDownloadComplete() {
                                                    try {
                                                        FileMan().unzip(
                                                            fileP + zipFile,
                                                            fileP
                                                        )

                                                    } finally {
                                                        File(fileP + zipFile).delete()
                                                        clLayoutUpload.visibility = View.GONE
                                                    }
                                                }

                                                override fun onError(error: Error?) {
                                                    Log.d("testzip", error.toString())
                                                    clLayoutUpload.visibility = View.GONE
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
                        AlertDialogUtility.alertDialog(
                            this@PkKuningListActivity,
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
                this@PkKuningListActivity,
                "Jaringan anda tidak stabil, mohon hubungkan ke jaringan yang stabil!",
                "network_error.json"
            )
        }
    }

    fun updateListPupuk() {
        val prefManager = PrefManager(this) //init shared preference
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
                            PemupukanSQL(this)
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
        Volley.newRequestQueue(this).add(strReq)
    }

    private fun getBytesToMBString(bytes: Long): String? {
        return format(Locale.ENGLISH, "%.2fMb", bytes / (1024.00 * 1024.00))
    }

    private fun uploadFileNew(sourceFile: File): Boolean {
        val fileName: String = sourceFile.name
        val requestBody: RequestBody =
            MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    sourceFile.asRequestBody()
                )
                .build()
        val request: Request = Request
            .Builder()
            .url(serverURL)
            .post(requestBody)
            .build()
        return try {
            val response: okhttp3.Response = client
                .newCall(request)
                .execute()
            response.isSuccessful
        } catch (ex: Exception) {
            ex.printStackTrace()
            false
        }
    }

    private fun isFileExists(nameFile: String): Boolean {
        val myDir = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString())
        val imageData = File(myDir, nameFile)
        return imageData.exists()
    }
}