package com.srs.deficiencytracker.activity

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ImageFormat
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.hardware.camera2.CaptureRequest
import android.media.ImageReader
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.util.Log
import android.util.Rational
import android.util.Size
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.widget.addTextChangedListener
import androidx.exifinterface.media.ExifInterface
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.daimajia.androidanimations.library.Techniques
import com.daimajia.androidanimations.library.YoYo
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jaredrummler.materialspinner.MaterialSpinner
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import com.srs.deficiencytracker.BuildConfig
import com.srs.deficiencytracker.MainActivity
import com.srs.deficiencytracker.R
import com.srs.deficiencytracker.database.PemupukanSQL
import com.srs.deficiencytracker.utilities.AlertDialogUtility
import com.srs.deficiencytracker.utilities.FileMan
import com.srs.deficiencytracker.utilities.PrefManager
import com.srs.deficiencytracker.utilities.PrefManagerEstate
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_camera.view.camApi
import kotlinx.android.synthetic.main.activity_camera.view.captureCam
import kotlinx.android.synthetic.main.activity_camera.view.torchButton
import kotlinx.android.synthetic.main.activity_form_est.sp_est_form
import kotlinx.android.synthetic.main.activity_foto_temuan.view.cvTemuan2
import kotlinx.android.synthetic.main.activity_foto_temuan.view.et_komentar_temuan
import kotlinx.android.synthetic.main.activity_foto_temuan.view.iv_temuan1
import kotlinx.android.synthetic.main.activity_handling_form.et_dosis_hand
import kotlinx.android.synthetic.main.activity_handling_form.header_hand
import kotlinx.android.synthetic.main.activity_handling_form.lyCameraPupuk
import kotlinx.android.synthetic.main.activity_handling_form.sdHandSaveUpload
import kotlinx.android.synthetic.main.activity_handling_form.sp_jenis_hand
import kotlinx.android.synthetic.main.activity_handling_form.sp_metode_hand
import kotlinx.android.synthetic.main.activity_handling_form.svMainPupuk
import kotlinx.android.synthetic.main.activity_handling_form.temuanPupuk
import kotlinx.android.synthetic.main.activity_handling_form.zoomPupuk
import kotlinx.android.synthetic.main.header_form.tv_update_header_gudang
import kotlinx.android.synthetic.main.header_form.tv_ver_app_header_gudang
import kotlinx.android.synthetic.main.header_form.view.icLocationHeader
import kotlinx.android.synthetic.main.header_form.view.ic_gudang_wh
import kotlinx.android.synthetic.main.header_form.view.tv_gudang_main
import kotlinx.android.synthetic.main.zoom_foto_layout.view.closeZoom
import kotlinx.android.synthetic.main.zoom_foto_layout.view.deletePhoto
import kotlinx.android.synthetic.main.zoom_foto_layout.view.fotoZoom
import kotlinx.android.synthetic.main.zoom_foto_layout.view.retakePhoto
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

open class HandlingFormActivity : AppCompatActivity() {

    private var getId = ""
    private var getEst = ""
    private var getAfd = ""
    private var getBlok = ""
    private var getCons = ""
    private var getGPS = ""
    private var posPhoto = false

    private var pupukArray = ArrayList<String>()
    private var pupukIdArray = ArrayList<Int>()

    private var jenisPupukId = 0
    private var metode = ""
    private var dosisPupuk = ""
    private var komen = ""
    private var komenResult = ""

    private var rootDCIM = ""
    private var rootApp = ""

    private var fotoArray = ArrayList<String>()
    private var komArray = ArrayList<String>()

    /* [camera] */
    private val imageCaptureCode1 = 111
    private var photoTaken1 = false
    private var fname = ""
    private var fname1 = ""
    lateinit var dateFormat: String
    private var file1: File? = null

    /* [camera2api] */
    val aspectRatio = Rational(16, 9)
    var selectedSize: Size? = null
    lateinit var capReq: CaptureRequest.Builder
    var handler: Handler? = null
    var handlerThread: HandlerThread? = null
    lateinit var cameraManager: CameraManager
    var cameraCaptureSession: CameraCaptureSession? = null
    var cameraDevice: CameraDevice? = null
    var imageReader: ImageReader? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private lateinit var textureViewCam: TextureView
    private var isCameraOpen = false
    private var isFlashlightOn = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_handling_form)

        getId = getDataIntent("id")
        getEst = getDataIntent("est")
        getAfd = getDataIntent("afd")
        getBlok = getDataIntent("blok")
        getCons = getDataIntent("kondisi")
        getGPS = getDataIntent("gps")

        rootDCIM = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "Deficiency Tracker"
        ).toString()
        rootApp = getExternalFilesDir(Environment.DIRECTORY_PICTURES).toString()

        getListPupuk()
        header_hand.icLocationHeader.visibility = View.GONE
        header_hand.tv_gudang_main.text = "PENANGANAN"
        header_hand.ic_gudang_wh.setImageResource(R.drawable.baseline_content_paste_24)
        setSpeedDial()

        var urlCategory = PrefManager(this).dataReg!!
        JSONObject(
            FileMan().onlineInputStream(
                urlCategory,
                this,
                tv_update_header_gudang,
                tv_ver_app_header_gudang
            )
        )

        hide(sp_jenis_hand)
        hide(sp_metode_hand)

        sp_jenis_hand.setItems(pupukArray)
        sp_jenis_hand.text = "Pilih Jenis Pupuk"
        sp_jenis_hand.setOnItemSelectedListener { view, position, id, item ->
            jenisPupukId = pupukIdArray[position]
        }

        val metodeArray = arrayListOf("Tanam", "Sebar")
        sp_metode_hand.setItems(metodeArray)
        sp_metode_hand.text = "Pilih Metode"

        sp_metode_hand.setOnItemSelectedListener { view, position, id, item ->
            metode = item.toString()
        }

        et_dosis_hand.addTextChangedListener {
            dosisPupuk = try {
                et_dosis_hand.text.toString()
            } catch (e: Exception) {
                "0"
            }
        }

        temuanPupuk.iv_temuan1.setOnClickListener {
            if (fotoArray.size >= 1) {
                Toasty.warning(this, "Hanya boleh mengunggah maksimal 1 foto!").show()
            } else {
                if (photoTaken1) {
                    YoYo.with(Techniques.RotateInUpRight)
                        .onStart {
                            Glide.with(this).load(Uri.fromFile(file1)).diskCacheStrategy(
                                DiskCacheStrategy.NONE
                            ).skipMemoryCache(true).into(zoomPupuk.fotoZoom)
                            Glide.with(this).load(Uri.fromFile(file1)).diskCacheStrategy(
                                DiskCacheStrategy.NONE
                            ).skipMemoryCache(true).centerCrop().into(temuanPupuk.iv_temuan1)
                            sdHandSaveUpload.visibility = View.GONE
                            zoomPupuk.visibility = View.VISIBLE
                            window.statusBarColor = ContextCompat.getColor(this, R.color.black)
                        }
                        .duration(500)
                        .repeat(0)
                        .playOn(findViewById(R.id.zoomPupuk))
                } else {
                    if (temuanPupuk.et_komentar_temuan.text.toString().isNotEmpty()) {
                        initializeCameraCapture(imageCaptureCode1)
                    } else {
                        Toasty.warning(this, "Tambahkan komentar terlebih dahulu!").show()
                    }
                }
                zoomPupuk.retakePhoto.setOnClickListener {
                    initializeCameraCapture(imageCaptureCode1)
                }
            }
        }

        zoomPupuk.deletePhoto.visibility = View.INVISIBLE
        zoomPupuk.closeZoom.setOnClickListener {
            closeZoom()
        }
    }

    private fun closeZoom() {
        YoYo.with(Techniques.RotateOutUpRight)
            .onEnd {
                zoomPupuk.visibility = View.GONE
                temuanPupuk.visibility = View.VISIBLE
                sdHandSaveUpload.visibility = View.VISIBLE
                window.statusBarColor = ContextCompat.getColor(this, R.color.white)
            }
            .duration(500)
            .repeat(0)
            .playOn(findViewById(R.id.temuanPupuk))
    }

    private fun createSD(string: String, id: Int, drawable: Int, color: Int) {
        val sdSaveUpload: SpeedDialView = findViewById(R.id.sdHandSaveUpload)
        sdSaveUpload.addActionItem(
            SpeedDialActionItem.Builder(id, drawable)
                .setLabel(string)
                .setFabSize(FloatingActionButton.SIZE_NORMAL)
                .setFabBackgroundColor(resources.getColor(color))
                .setFabImageTintColor(resources.getColor(R.color.white))
                .create()
        )
    }

    private fun setSpeedDial() {
        val sdSaveUpload: SpeedDialView = findViewById(R.id.sdHandSaveUpload)

        updateSpeedDialItems(sdSaveUpload)

        sdSaveUpload.setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.openCamera -> {
                    posPhoto = !posPhoto

                    svMainPupuk.visibility = if (!posPhoto) View.VISIBLE else View.GONE
                    temuanPupuk.visibility = if (!posPhoto) View.GONE else View.VISIBLE

                    temuanPupuk.cvTemuan2.visibility = View.GONE

                    updateSpeedDialItems(sdSaveUpload)

                    sdSaveUpload.close()
                    return@OnActionSelectedListener true
                }

                R.id.saveHandling -> {
                    try {
                        if (posPhoto) {
                            if (fname1.isNotEmpty() && temuanPupuk.et_komentar_temuan.text.toString()
                                    .isNotEmpty()
                            ) {
                                takeFindingPhoto()

                                posPhoto = !posPhoto

                                svMainPupuk.visibility = if (!posPhoto) View.VISIBLE else View.GONE
                                temuanPupuk.visibility = if (!posPhoto) View.GONE else View.VISIBLE

                                updateSpeedDialItems(sdSaveUpload)

                                Toasty.success(this, "Sukses menyimpan foto!").show()
                            } else {
                                if (fotoArray.size < 1) {
                                    Toasty.warning(this, "Tambahkan foto temuan dan komentar!")
                                        .show()
                                } else {
                                    Toasty.warning(this, "Hanya boleh mengunggah maksimal 1 foto!").show()
                                }
                            }
                        } else {
                            if (sp_jenis_hand.text == "Pilih Jenis Pupuk" || et_dosis_hand.text.toString()
                                    .isEmpty() || sp_metode_hand.text == "Pilih Metode" || fotoArray.isEmpty()
                            ) {
                                AlertDialogUtility.alertDialog(
                                    this,
                                    "Ada field atau foto yang belum diisi!",
                                    "warning.json"
                                )
                            } else {
                                AlertDialogUtility.withTwoActions(
                                    this@HandlingFormActivity,
                                    "BATAL",
                                    "SIMPAN",
                                    "Yakin menyimpan data?",
                                    "warning.json"
                                ) {
                                    if (fotoArray.isNotEmpty()) {
                                        fname = fotoArray.toTypedArray().contentToString()
                                        komen = komArray.toTypedArray().contentToString()
                                    }

                                    var revKomen =
                                        komen.replace("[", "").replace("]", "").split(",")
                                            .toTypedArray()
                                    for (i in revKomen.indices) {
                                        val prefix = if (i == 0) "" else "$"
                                        komenResult += "${prefix}${
                                            revKomen[i].trim().replace("|", ",")
                                        }"
                                    }

                                    val databaseHandler = PemupukanSQL(this)
                                    val status = databaseHandler.addPokokKuning(
                                        idPk = getId.toInt(),
                                        estate = getEst,
                                        afdeling = getAfd,
                                        blok = getBlok,
                                        kondisi = getCons,
                                        datetime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                                            Calendar.getInstance().time
                                        ),
                                        jenisPupukId = jenisPupukId,
                                        dosisPupuk = dosisPupuk,
                                        metodePupuk = metode,
                                        foto = fname.replace("[", "").replace("]", "")
                                            .replace(",", "$").replace(" ", ""),
                                        komen = komenResult,
                                        app_ver = "${BuildConfig.VERSION_NAME};${android.os.Build.VERSION.RELEASE};${android.os.Build.MODEL};$getGPS"
                                    )
                                    if (status > -1) {
                                        val pkPath =
                                            this.getExternalFilesDir(null)?.absolutePath + "/MAIN/pk" + PrefManager(
                                                this
                                            ).dataReg!!
                                        val fileMaps = File(pkPath)
                                        if (fileMaps.exists()) {
                                            try {
                                                val readMaps = fileMaps.readText()
                                                val objMaps = JSONObject(readMaps)

                                                val estObjMaps = objMaps.getJSONObject(getEst)
                                                if (getAfd.isEmpty()) {
                                                    for (indexEst in estObjMaps.keys()) {
                                                        var itemEst =
                                                            estObjMaps.getJSONObject(indexEst)
                                                        for (indexAfd in itemEst.keys()) {
                                                            var itemAfd =
                                                                itemEst.getJSONObject(indexAfd)
                                                            for (index in itemAfd.keys()) {
                                                                val item =
                                                                    itemAfd.getJSONObject(index)
                                                                if (getId == index) {
                                                                    item.put("status", "Sudah")
                                                                    fileMaps.writeText(objMaps.toString())
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    val afdObjMaps =
                                                        estObjMaps.getJSONObject(getAfd)
                                                    if (getBlok.isEmpty()) {
                                                        for (indexAfd in afdObjMaps.keys()) {
                                                            var itemAfd =
                                                                afdObjMaps.getJSONObject(indexAfd)
                                                            for (index in itemAfd.keys()) {
                                                                val item =
                                                                    itemAfd.getJSONObject(index)
                                                                if (getId == index) {
                                                                    item.put("status", "Sudah")
                                                                    fileMaps.writeText(objMaps.toString())
                                                                }
                                                            }
                                                        }
                                                    } else {
                                                        val blokObjMaps =
                                                            afdObjMaps.getJSONObject(getBlok)
                                                        for (index in blokObjMaps.keys()) {
                                                            val item =
                                                                blokObjMaps.getJSONObject(index)
                                                            if (getId == index) {
                                                                item.put("status", "Sudah")
                                                                fileMaps.writeText(objMaps.toString())
                                                            }
                                                        }
                                                    }
                                                }

                                                AlertDialogUtility.withSingleAction(
                                                    this,
                                                    "OK",
                                                    "Pemupukan Pokok Kuning telah tersimpan!",
                                                    "success.json"
                                                ) {
                                                    val intent =
                                                        Intent(this, MainActivity::class.java)
                                                    startActivity(intent)
                                                    finishAffinity()
                                                }
                                            } catch (e: Exception) {
                                                AlertDialogUtility.alertDialog(
                                                    this,
                                                    "Terjadi kesalahan, hubungi pengembang. Error: $e",
                                                    "warning.json"
                                                )
                                                e.printStackTrace()
                                            }
                                        } else {
                                            AlertDialogUtility.alertDialog(
                                                this,
                                                "File JSON tidak ditemukan!",
                                                "warning.json"
                                            )
                                        }
                                    } else {
                                        AlertDialogUtility.alertDialog(
                                            this,
                                            "Terjadi kesalahan, hubungi pengembang",
                                            "warning.json"
                                        )
                                    }
                                }
                            }
                        }
                    } catch (e: IOException) {
                        AlertDialogUtility.alertDialog(
                            this,
                            "Terjadi kesalahan, hubungi pengembang. Error: $e",
                            "warning.json"
                        )
                    }

                    sdSaveUpload.close()
                    return@OnActionSelectedListener true
                }
            }
            false
        })
    }

    private fun updateSpeedDialItems(sdSaveUpload: SpeedDialView) {
        sdSaveUpload.clearActionItems() // Remove all existing action items

        createSD(
            if (!posPhoto) "TAMBAH FOTO" else "KEMBALI",
            R.id.openCamera,
            if (!posPhoto) R.drawable.ic_baseline_add_a_photo_24 else R.drawable.baseline_arrow_back_24,
            if (!posPhoto) R.color.chart_blue4 else R.color.colorRed_A400
        )

        createSD(
            "SIMPAN" + if (!posPhoto) " DATA" else " FOTO",
            R.id.saveHandling,
            R.drawable.ic_save_black_24dp,
            R.color.green_basiccolor
        )
    }

    private fun takeFindingPhoto() {
        komArray.add(temuanPupuk.et_komentar_temuan.text.toString().replace(",", "|"))
        fotoArray.add(fname1)

        temuanPupuk.et_komentar_temuan.setText("")
        fname1 = ""
        photoTaken1 = false
        file1 = null

        YoYo.with(Techniques.RotateOutUpRight)
            .onEnd {
                zoomPupuk.fotoZoom.setImageDrawable(null)
                temuanPupuk.iv_temuan1.setImageResource(R.drawable.ic_baseline_add_a_photo_24)
                zoomPupuk.visibility = View.GONE
                window.statusBarColor = ContextCompat.getColor(this, R.color.white)
            }
            .duration(500)
            .repeat(0)
            .playOn(findViewById(R.id.zoomPupuk))

        svMainPupuk.visibility = View.VISIBLE
        temuanPupuk.visibility = View.GONE
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

    private fun addToGallery(photoFile: File) {
        val galleryIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        val f = photoFile!!
        val picUri = Uri.fromFile(f)
        galleryIntent.data = picUri
        this.sendBroadcast(galleryIntent)
    }

    private fun addWatermark(bitmap: Bitmap, watermarkText: String): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

        val canvas = Canvas(resultBitmap)
        canvas.drawBitmap(bitmap, 0f, 0f, null)

        val textPaint = Paint()
        textPaint.color = Color.YELLOW
        textPaint.textSize = height / 25f
        textPaint.textAlign = Paint.Align.RIGHT
        textPaint.typeface = ResourcesCompat.getFont(this, R.font.helvetica)

        val backgroundPaint = Paint()
        backgroundPaint.color = Color.parseColor("#3D000000") // Black color with 25% transparency

        val watermarkLines = watermarkText.split("\n")
        val textHeight = textPaint.fontMetrics.bottom - textPaint.fontMetrics.top

        val x = width - width / 40f
        val y = height - (textHeight * watermarkLines.size) - height / 40f

        var maxWidth = 0f
        for (line in watermarkLines) {
            val lineWidth = textPaint.measureText(line)
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth
            }
        }

        val backgroundWidth = maxWidth
        for (i in watermarkLines.indices) {
            val line = watermarkLines[i]
            val lineY = y + (textHeight * (i + 1))
            canvas.drawRect(x - backgroundWidth, lineY - textHeight, x, lineY, backgroundPaint)
            canvas.drawText(line, x, lineY, textPaint)
        }

        return resultBitmap
    }

    private fun initializeCameraCapture(resultCode: Int) {
        zoomPupuk.visibility = View.GONE
        hideKeyboard(this)
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        mainHandler.postDelayed({
            textureViewCam = TextureView(this)
            val layoutParams = RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.MATCH_PARENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT
            )
            layoutParams.addRule(RelativeLayout.ABOVE, lyCameraPupuk.captureCam.id)
            layoutParams.setMargins(0, 0, 0, 50)
            textureViewCam.layoutParams = layoutParams

            temuanPupuk.visibility = View.GONE
            sdHandSaveUpload.visibility = View.GONE
            lyCameraPupuk.visibility = View.VISIBLE
            lyCameraPupuk.camApi.addView(textureViewCam)
            lyCameraPupuk.captureCam.setBackgroundResource(R.drawable.circle)
            lyCameraPupuk.captureCam.backgroundTintList = ColorStateList.valueOf(Color.WHITE)
            lyCameraPupuk.torchButton.setBackgroundResource(R.drawable.ic_lightning_off)
            lyCameraPupuk.torchButton.backgroundTintList = ColorStateList.valueOf(Color.WHITE)

            cameraManager = getSystemService(Context.CAMERA_SERVICE) as CameraManager
            handlerThread = HandlerThread("videoThread")
            handlerThread!!.start()
            handler = Handler((handlerThread)!!.looper)

            textureViewCam.surfaceTextureListener =
                object : TextureView.SurfaceTextureListener {
                    override fun onSurfaceTextureAvailable(p0: SurfaceTexture, p1: Int, p2: Int) {
                        openCameraSessions(resultCode)
                    }

                    override fun onSurfaceTextureSizeChanged(p0: SurfaceTexture, p1: Int, p2: Int) {

                    }

                    override fun onSurfaceTextureDestroyed(p0: SurfaceTexture): Boolean {
                        return true
                    }

                    override fun onSurfaceTextureUpdated(p0: SurfaceTexture) {

                    }
                }

            lyCameraPupuk.captureCam.apply {
                setOnClickListener {
                    capReq =
                        cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE)
                    capReq.addTarget(imageReader!!.surface)
                    cameraCaptureSession!!.capture(capReq.build(), null, null)
                }
            }
        }, 500)
    }

    @SuppressLint("MissingPermission")
    private fun openCameraSessions(resultCode: Int) {
        cameraManager.openCamera(
            cameraManager.cameraIdList[0],
            object : CameraDevice.StateCallback() {
                override fun onOpened(p0: CameraDevice) {
                    var fileName = ""
                    lateinit var file: File

                    cameraDevice = p0
                    isCameraOpen = true
                    capReq = cameraDevice!!.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)

                    val characteristics = cameraManager.getCameraCharacteristics(cameraDevice!!.id)
                    val streamConfigurationMap =
                        characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                    val outputSizes = streamConfigurationMap?.getOutputSizes(ImageFormat.JPEG)

                    for (size in outputSizes!!) {
                        val rational = Rational(size.width, size.height)
                        if (rational.equals(aspectRatio)) {
                            selectedSize = size
                            break
                        }
                    }

                    dateFormat =
                        SimpleDateFormat("yyyyMdd_HHmmss").format(Calendar.getInstance().time) //nentuin tanggal dan jam
                    var pictureFile = "PK_${dateFormat}_${getEst}_${getAfd}_${getBlok}"

                    selectedSize?.let { size ->
                        val surfaceTexture = textureViewCam.surfaceTexture
                        surfaceTexture!!.setDefaultBufferSize(size.width, size.height)
                        val surface = Surface(surfaceTexture)
                        capReq.addTarget(surface)

                        lyCameraPupuk.torchButton.apply {
                            setOnClickListener {
                                isFlashlightOn = !isFlashlightOn
                                if (isFlashlightOn) {
                                    lyCameraPupuk.torchButton.setBackgroundResource(R.drawable.ic_lightning_on)
                                    lyCameraPupuk.torchButton.backgroundTintList =
                                        ColorStateList.valueOf(
                                            Color.YELLOW
                                        )
                                    capReq.set(
                                        CaptureRequest.FLASH_MODE,
                                        CaptureRequest.FLASH_MODE_TORCH
                                    )
                                } else {
                                    lyCameraPupuk.torchButton.setBackgroundResource(R.drawable.ic_lightning_off)
                                    lyCameraPupuk.torchButton.backgroundTintList =
                                        ColorStateList.valueOf(
                                            Color.WHITE
                                        )
                                    capReq.set(
                                        CaptureRequest.FLASH_MODE,
                                        CaptureRequest.FLASH_MODE_OFF
                                    )
                                }
                                cameraCaptureSession!!.setRepeatingRequest(
                                    capReq.build(),
                                    null,
                                    null
                                )
                            }
                        }

                        imageReader =
                            ImageReader.newInstance(size.width, size.height, ImageFormat.JPEG, 1)
                        imageReader!!.setOnImageAvailableListener({ p0 ->
                            var image = p0?.acquireLatestImage()
                            var fileDCIM: File? = null
                            if (image != null) {
                                var buffer = image.planes[0].buffer
                                var bytes = ByteArray(buffer.remaining())
                                buffer.get(bytes)

                                val dirApp = File(rootApp)
                                dirApp.mkdirs()
                                val dirDCIM = File(rootDCIM)
                                dirDCIM.mkdirs()

                                when (resultCode) {
                                    imageCaptureCode1 -> {
                                        fname1 = "$pictureFile.jpg"
                                        fileName = fname1
                                        file1 = File(dirApp, fileName)
                                        file = file1!!
                                    }
                                }

                                fileDCIM = File(dirDCIM, fileName)
                                if (fileDCIM.exists()) fileDCIM.delete()
                                addToGallery(fileDCIM)

                                if (file.exists()) file.delete()
                                addToGallery(file)

                                var opStream = FileOutputStream(file)
                                opStream.write(bytes)
                                opStream.close()
                                image.close()
                            } else {
                                Toasty.error(
                                    this@HandlingFormActivity,
                                    "Unable to capture image. Please try again.",
                                    Toast.LENGTH_LONG
                                ).show()
                                closeCamera()
                            }

                            val takenImage = rotateBitmapOrientation(file.path)
                            val bitmap = takenImage!!
                            val dateWM = SimpleDateFormat(
                                "dd MMMM yyyy HH:mm:ss",
                                Locale("id", "ID")
                            ).format(Calendar.getInstance().time) //nentuin tanggal dan jam

                            var komentarEmp = temuanPupuk.et_komentar_temuan.text.toString()
                            val watermarkedBitmap = addWatermark(
                                bitmap,
                                "POKOK KUNING/$getEst/$getAfd/$getBlok\n${
                                    komentarEmp.replace("\n", " ")
                                }\n$dateWM"
                            )

                            try {
                                val out = FileOutputStream(file)
                                watermarkedBitmap.compress(Bitmap.CompressFormat.JPEG, 50, out)
                                out.flush()
                                out.close()
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }

                            try {
                                val outDCIM = FileOutputStream(fileDCIM)
                                watermarkedBitmap.compress(
                                    Bitmap.CompressFormat.JPEG,
                                    50,
                                    outDCIM
                                )
                                outDCIM.flush()
                                outDCIM.close()
                            } catch (e: java.lang.Exception) {
                                e.printStackTrace()
                            }

                            mainHandler.post {
                                closeCamera()

                                when (resultCode) {
                                    imageCaptureCode1 -> {
                                        photoTaken1 = true
                                        Glide.with(this@HandlingFormActivity)
                                            .load(Uri.fromFile(file1))
                                            .diskCacheStrategy(
                                                DiskCacheStrategy.NONE
                                            ).skipMemoryCache(true).centerCrop()
                                            .into(temuanPupuk.iv_temuan1)
                                        Glide.with(this@HandlingFormActivity)
                                            .load(Uri.fromFile(file1))
                                            .diskCacheStrategy(
                                                DiskCacheStrategy.NONE
                                            ).skipMemoryCache(true).into(zoomPupuk.fotoZoom)
                                    }
                                }
                            }
                        }, handler)

                        cameraDevice!!.createCaptureSession(
                            listOf(surface, imageReader!!.surface),
                            object : CameraCaptureSession.StateCallback() {
                                override fun onConfigured(p0: CameraCaptureSession) {
                                    cameraCaptureSession = p0
                                    cameraCaptureSession!!.setRepeatingRequest(
                                        capReq.build(),
                                        null,
                                        null
                                    )
                                }

                                override fun onConfigureFailed(p0: CameraCaptureSession) {

                                }
                            },
                            handler
                        )
                    }
                }

                override fun onDisconnected(p0: CameraDevice) {

                }

                override fun onError(p0: CameraDevice, p1: Int) {

                }
            },
            handler
        )
    }

    private fun closeCamera() {
        if (isCameraOpen) {
            lyCameraPupuk.camApi.removeView(textureViewCam)

            cameraCaptureSession!!.close()
            cameraCaptureSession!!.device.close()
            cameraCaptureSession = null

            cameraDevice!!.close()
            cameraDevice = null

            imageReader!!.close()
            imageReader = null

            handlerThread!!.quitSafely()
            handlerThread = null
            handler = null

            isCameraOpen = false

            lyCameraPupuk.visibility = View.GONE
            temuanPupuk.visibility = View.VISIBLE
            sdHandSaveUpload.visibility = View.VISIBLE

            window.clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
    }

    @SuppressLint("Range")
    private fun getListPupuk() {
        val selectQuery =
            "SELECT  * FROM ${PemupukanSQL.db_tabPupuk} ORDER BY ${PemupukanSQL.db_namaPupuk} ASC"
        val db = PemupukanSQL(this).readableDatabase
        var i: Cursor?
        try {
            i = db.rawQuery(selectQuery, null)
            if (i.moveToFirst()) {
                do {
                    pupukArray.add(
                        try {
                            i.getString(i.getColumnIndex(PemupukanSQL.db_namaPupuk))
                        } catch (e: Exception) {
                            ""
                        }
                    )
                    pupukIdArray.add(
                        try {
                            i.getInt(i.getColumnIndex(PemupukanSQL.db_id))
                        } catch (e: Exception) {
                            0
                        }
                    )
                } while (i!!.moveToNext())
            }
        } catch (e: SQLiteException) {
            Log.e("ET", "Error: $e")
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun hide(spinner: MaterialSpinner) {
        spinner.setOnTouchListener { _, _ ->
            hideKeyboard(this)
            false // Return false to allow the normal spinner click event to proceed
        }
    }

    private fun hideKeyboard(activity: Activity) {
        val inputMethodManager =
            activity.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        val view = activity.currentFocus ?: return
        inputMethodManager.hideSoftInputFromWindow(view.windowToken, 0)
    }

    private fun getDataIntent(data: String? = null): String {
        return try {
            intent.getStringExtra(data)!!
        } catch (e: Exception) {
            ""
        }
    }

    override fun onBackPressed() {
        if (isCameraOpen) {
            closeCamera()
        } else {
            AlertDialogUtility.withTwoActions(
                this,
                "Batal",
                "Ya",
                "Apakah anda yakin untuk keluar?",
                "warning.json"
            ) {
                val pm = PrefManagerEstate(this)
                val intent = Intent(this, MapsActivity::class.java).putExtra("est", pm.estate)
                    .putExtra("afd", pm.afdeling).putExtra("blok", pm.blok)
                    .putExtra("blokPlot", pm.blokPlot)
                startActivity(intent)
                finishAffinity()
            }
        }
    }
}