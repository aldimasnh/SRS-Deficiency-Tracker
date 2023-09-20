@file:Suppress("PrivatePropertyName")

package com.srs.deficiencytracker.activity

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.exifinterface.media.ExifInterface
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.bumptech.glide.Glide
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
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_dosisPupuk
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_estate
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_id
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_idPk
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_jenisPupukID
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_komen
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_kondisi
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_metode
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_photo
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_status
import com.srs.deficiencytracker.database.PemupukanSQL.Companion.db_tabPkKuning
import com.srs.deficiencytracker.database.ViewPkKuning
import com.srs.deficiencytracker.utilities.AlertDialogUtility
import com.srs.deficiencytracker.utilities.PrefManager
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_list_upload.sd_list_upload
import kotlinx.android.synthetic.main.activity_list_upload.header_list_upload
import kotlinx.android.synthetic.main.activity_list_upload.listViewUpload
import kotlinx.android.synthetic.main.activity_list_upload.logo_ssms_upload_pk
import kotlinx.android.synthetic.main.activity_list_upload.lt_list_upload
import kotlinx.android.synthetic.main.activity_list_upload.pb_holder_list_upload
import kotlinx.android.synthetic.main.activity_list_upload.switchListUpload
import kotlinx.android.synthetic.main.activity_list_upload.view.logo_ssms_upload_pk
import kotlinx.android.synthetic.main.dialog_layout_success.view.*
import kotlinx.android.synthetic.main.header.*
import kotlinx.android.synthetic.main.header.view.*
import kotlinx.coroutines.*
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONException
import org.json.JSONObject
import java.io.*
import java.lang.Runnable
import java.util.*

class PkKuningListActivity : AppCompatActivity() {

    //upload
    private val urlCekFoto = "https://srs-ssms.com/deficiency_tracker/checkFotoTracker.php"
    private val urlInsert = "https://srs-ssms.com/deficiency_tracker/postDataTracker.php"
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
    val timeOut = 600000
    val runnableCode = object : Runnable {
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
                pb_holder_list_upload.visibility = View.GONE

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
                }
            }

            handler.postDelayed(this, delay.toLong())
        }
    }
    val stopRunnable = Runnable {
        if (!stoppedByConditions) {
            shouldStop = true
            uploading = false
            handler.removeCallbacks(runnableCode)
            pb_holder_list_upload.visibility = View.GONE

            if (messageCheckFoto.isNotEmpty()) {
                if (successResponse == 0 || successResponse == 1) {
                    Toasty.warning(this, messageCheckFoto, Toast.LENGTH_SHORT).show()
                } else {
                    Toasty.success(this, messageCheckFoto, Toast.LENGTH_SHORT).show()
                }
            }

            if (messageInsert.isNotEmpty()) {
                handler.postDelayed({
                    if (successResponseInsert == 1) {
                        Toasty.success(this, messageInsert, Toast.LENGTH_SHORT).show()
                    } else {
                        Toasty.warning(this, messageInsert, Toast.LENGTH_SHORT).show()
                    }
                }, 1000)
            }
        }
    }

    val arrayMissPhoto = ArrayList<String>()
    val arrayCheckFoto = ArrayList<String>()
    val idArray = ArrayList<Int>()
    val idPkArray = ArrayList<Int>()
    val estArray = ArrayList<String>()
    val afdArray = ArrayList<String>()
    val blokArray = ArrayList<String>()
    val statusArray = ArrayList<String>()
    val kondisiArray = ArrayList<String>()
    val datetimeArray = ArrayList<String>()
    val jenisPupukArray = ArrayList<Int>()
    val dosisPupukArray = ArrayList<String>()
    val metodeArray = ArrayList<String>()
    val photoArray = ArrayList<String>()
    val komenArray = ArrayList<String>()
    val appVerArray = ArrayList<String>()

    var getArchive = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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

        val archiveNotEmpty = getArchive.isNotEmpty()
        if (archiveNotEmpty || PemupukanSQL(this).setRecordPkKuning().toInt() > 0) {
            switchListUpload.isChecked = archiveNotEmpty && getArchive.toInt() == 1
            sd_list_upload.visibility =
                if (archiveNotEmpty && getArchive.toInt() == 1) View.GONE else View.VISIBLE
            makeList(if (archiveNotEmpty) getArchive.toInt() else 0)
            sdInit()
        }

        switchListUpload.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sd_list_upload.visibility = View.GONE
                makeList(1)
            } else {
                sd_list_upload.visibility = View.VISIBLE
                makeList(0)
            }
        }

        Glide.with(this)//GLIDE LOGO FOR LOADING LAYOUT
            .load(R.drawable.logo_png_white)
            .into(logo_ssms_upload_pk)
        Glide.with(this)//GLIDE LOGO FOR LOADING LAYOUT
            .load(R.drawable.ssms_green)
            .into(pb_holder_list_upload.logo_ssms_upload_pk)
        lt_list_upload.setAnimation("loading_circle.json")//ANIMATION WITH LOTTIE FOR LOADING LAYOUT
        @Suppress("DEPRECATION")
        lt_list_upload.loop(true)
        lt_list_upload.playAnimation()
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

        var idX = 0
        for (e in arrayList) {
            if (!idPk.contains(e.db_idPk)) {
                id.add(idX)
                idPk.add(e.db_idPk)
                est.add(e.db_estate)
                afd.add(e.db_afdeling)
                blok.add(e.db_blok)
                status.add(e.db_status)
                kondisi.add(e.db_kondisi)
                datetime.add(e.db_datetime)
                idX++
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
                    if (PemupukanSQL(this).setRecordPkKuning().toInt() > 0) {
                        val department = try {
                            PrefManager(this).departemen!!
                        } catch (e: Exception) {
                            "null"
                        }
                        if (department.contains("QC")) {
                            post()
                        } else {
                            AlertDialogUtility.alertDialog(
                                this,
                                "${PrefManager(this).name!!} bukan termasuk anggota QC!",
                                "warning.json"
                            )
                        }
                    } else {
                        Toasty.warning(this, "Tidak ada data dalam list!!", Toast.LENGTH_SHORT)
                            .show()
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
        dosisPupukArray.clear()
        metodeArray.clear()
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
                    jenisPupukArray.add(getData(db_jenisPupukID, c).toInt())
                    dosisPupukArray.add(getData(db_dosisPupuk, c))
                    metodeArray.add(getData(db_metode, c))
                    photoArray.add(getData(db_photo, c))
                    val arrayFoto =
                        getData(db_photo, c).split("$")
                    for (a in arrayFoto.indices) {
                        if (arrayFoto[a].isNotEmpty()) {
                            arrayCheckFoto.add(arrayFoto[a])
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
                        {
                        },
                        {
                        },
                        true
                    )
                },
                {
                    uploading = true
                    pb_holder_list_upload.visibility = View.VISIBLE
                    arrayCheckFoto.removeAll(arrayMissPhoto)

                    handler.postDelayed(runnableCode, delay.toLong())
                    handler.postDelayed(stopRunnable, timeOut.toLong())
                }
            )
        } else {
            uploading = true
            pb_holder_list_upload.visibility = View.VISIBLE
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
        jenisPupukUp: Int,
        dosisPupukUp: String,
        metodeUp: String,
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
                params["petugas"] = PrefManager(this@PkKuningListActivity).name!!
                params[db_idPk] = idPkUp.toString()
                params[db_estate] = estUp
                params[db_afdeling] = afdUp
                params[db_blok] = blokUp
                params[db_status] = statusUp
                params[db_kondisi] = kondisiUp
                params[db_datetime] = dateUp
                params[db_jenisPupukID] = jenisPupukUp.toString()
                params[db_dosisPupuk] = dosisPupukUp
                params[db_metode] = metodeUp
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

    private fun rotateBitmapOrientation(photoFilePath: String?): Bitmap? {
        // Create and configure BitmapFactory
        val bounds = BitmapFactory.Options()
        bounds.inJustDecodeBounds = true
        BitmapFactory.decodeFile(photoFilePath, bounds)
        val opts = BitmapFactory.Options()
        val bm = BitmapFactory.decodeFile(photoFilePath, opts)
        // Read EXIF Data
        var exif: ExifInterface? = null
        try {
            exif = photoFilePath?.let { ExifInterface(it) }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        val orientString: String? = exif?.getAttribute(ExifInterface.TAG_ORIENTATION)
        val orientation =
            orientString?.toInt() ?: ExifInterface.ORIENTATION_NORMAL
        var rotationAngle = 0
        when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> {
                rotationAngle = 90
            }

            ExifInterface.ORIENTATION_ROTATE_180 -> {
                rotationAngle = 180
            }

            ExifInterface.ORIENTATION_ROTATE_270 -> {
                rotationAngle = 270
            }
        }
        // Rotate Bitmap
        val matrix = Matrix()
        matrix.setRotate(
            rotationAngle.toFloat(),
            bm.width.toFloat() / 2,
            bm.height.toFloat() / 2
        )
        // Return result
        return Bitmap.createBitmap(bm, 0, 0, bounds.outWidth, bounds.outHeight, matrix, true)
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
                                dosisPupukUp = dosisPupukArray[i],
                                metodeUp = metodeArray[i],
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

    private fun uploadFileNew(sourceFile: File): Boolean {
        val fileName: String = sourceFile.name
        val maxSize = 1000f
        val takenImage = rotateBitmapOrientation(sourceFile.absolutePath)
        val pembagiSize: Float = if (takenImage!!.width > takenImage!!.height) {
            takenImage!!.width.toFloat() / maxSize
        } else {
            takenImage!!.height.toFloat() / maxSize
        }
        val newHeight = takenImage.height / pembagiSize
        val newWidth = takenImage.width / pembagiSize
        val bitmap = Bitmap.createScaledBitmap(
            takenImage!!,
            newWidth.toInt(),
            newHeight.toInt(),
            false
        )
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, outputStream) // Compress the bitmap
        val byteArray =
            outputStream.toByteArray() // Get the compressed bitmap as a byte array
        val requestBody: RequestBody =
            MultipartBody
                .Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    fileName,
                    byteArray.toRequestBody(MultipartBody.FORM, 0, byteArray.size)
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