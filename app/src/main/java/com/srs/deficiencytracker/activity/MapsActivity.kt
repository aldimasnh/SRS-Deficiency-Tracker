package com.srs.deficiencytracker.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.database.Cursor
import android.database.sqlite.SQLiteException
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
import com.bumptech.glide.Glide
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.LocationSettingsRequest
import com.google.android.gms.location.LocationSettingsStatusCodes
import com.google.android.gms.location.SettingsClient
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.srs.deficiencytracker.BuildConfig
import com.srs.deficiencytracker.MainActivity
import com.srs.deficiencytracker.R
import com.srs.deficiencytracker.database.PemupukanSQL
import com.srs.deficiencytracker.utilities.AlertDialogUtility
import com.srs.deficiencytracker.utilities.DashedLineOverlay
import com.srs.deficiencytracker.utilities.ModelMain
import com.srs.deficiencytracker.utilities.PrefManager
import com.srs.deficiencytracker.utilities.PrefManagerEstate
import com.srs.deficiencytracker.utilities.UpdateMan
import de.mateware.snacky.Snacky
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_maps.clLayoutVerif
import kotlinx.android.synthetic.main.activity_maps.fbActionMaps
import kotlinx.android.synthetic.main.activity_maps.fbBlok
import kotlinx.android.synthetic.main.activity_maps.fbCenterMaps
import kotlinx.android.synthetic.main.activity_maps.fbMenu1Maps
import kotlinx.android.synthetic.main.activity_maps.fbMenu2Maps
import kotlinx.android.synthetic.main.activity_maps.fbMultiple
import kotlinx.android.synthetic.main.activity_maps.fbPokokSembuh
import kotlinx.android.synthetic.main.activity_maps.fbSingle
import kotlinx.android.synthetic.main.activity_maps.loadingVerif
import kotlinx.android.synthetic.main.activity_maps.mapView
import kotlinx.android.synthetic.main.activity_maps.tvAccuracyMaps
import kotlinx.android.synthetic.main.activity_maps.tvBlokMaps
import kotlinx.android.synthetic.main.activity_maps.tvInfoAct
import kotlinx.android.synthetic.main.activity_maps.tvInfoBlok
import kotlinx.android.synthetic.main.activity_maps.tvInfoCM
import kotlinx.android.synthetic.main.activity_maps.tvInfoMulti
import kotlinx.android.synthetic.main.activity_maps.tvInfoSembuh
import kotlinx.android.synthetic.main.activity_maps.tvInfoSingle
import kotlinx.android.synthetic.main.activity_maps.tvJarakMaps
import kotlinx.android.synthetic.main.activity_maps.tvJmlPkMaps
import kotlinx.android.synthetic.main.activity_maps.tvKondisiMaps
import kotlinx.android.synthetic.main.activity_maps.tvPokokMaps
import kotlinx.android.synthetic.main.activity_maps.tvStatusMaps
import kotlinx.android.synthetic.main.activity_maps.tvTglFotoMaps
import kotlinx.android.synthetic.main.loading_file_layout.view.logoFileLoader
import kotlinx.android.synthetic.main.loading_file_layout.view.lottieFileLoader
import kotlinx.android.synthetic.main.loading_file_layout.view.tvHintFileLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

open class MapsActivity : AppCompatActivity() {
    private var modelMainList: MutableList<ModelMain> = ArrayList()
    private var lastClickedMarker: Marker? = null
    private lateinit var sensorManager: SensorManager
    private var previousDashedLine: DashedLineOverlay? = null
    private var currentlyClickedPolygon: Polygon? = null

    private var accuracyRange = 0
    private var minAccuracyGPS = 10
    private var minRangeAct = 12
    private var maxActBlok = 1500
    private var maxActTrees = 1
    private var modeAct = 1

    private var getIdPk = ""
    private var getEst = ""
    private var getAfd = ""
    private var getBlok = ""
    private var getBlokPlot = ""
    private var getPos = ""

    private var urlCategory = ""
    private var fixBlok = ""

    private val markerMap = mutableMapOf<String, Marker>()
    private val markerIds = ArrayList<Int>()
    private val markerAfd = ArrayList<String>()
    private val markerBlok = ArrayList<String>()
    private val markerCons = ArrayList<String>()
    private val markerStats = ArrayList<String>()

    private var perlakuanArray = ArrayList<String>()
    private var perlakuanIdArray = ArrayList<Int>()

    /* [location] */
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mSettingsClient: SettingsClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var mCurrentLocation: Location? = null
    private var mRequestingLocationUpdates: Boolean? = null
    private var mLastUpdateTime: String? = null //waktu terakhir update lokasi
    private var lat: Float? = null
    private var lon: Float? = null
    private var currentMarker: Marker? = null

    private var positionMaps = false
    private var firstGPS = false
    private var fixAccuracy = 0
    private var latPk: Double? = null
    private var lonPk: Double? = null

    private val coroutineScope = CoroutineScope(Dispatchers.Main)
    private var inserting = false

    private val jobTitles = listOf("Manager", "QC", "Head", "COO", "CEO", "PA")
    private var isProgrammer = false

    private val animations1 by lazy {
        Pair(
            AnimationUtils.loadAnimation(this, R.anim.rotate_open_button),
            AnimationUtils.loadAnimation(this, R.anim.rotate_close_button)
        )
    }

    private val animations2 by lazy {
        Pair(
            AnimationUtils.loadAnimation(this, R.anim.rotate_open_button),
            AnimationUtils.loadAnimation(this, R.anim.rotate_close_button)
        )
    }

    private val animationsDrop1 by lazy {
        Pair(
            AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim),
            AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim)
        )
    }

    private val animationsDrop2 by lazy {
        Pair(
            AnimationUtils.loadAnimation(this, R.anim.from_bottom_anim),
            AnimationUtils.loadAnimation(this, R.anim.to_bottom_anim)
        )
    }

    private val tvInfoData1 by lazy { arrayOf(tvInfoAct, tvInfoCM) }
    private val buttonData1 by lazy {
        Triple(
            arrayOf(fbActionMaps, fbCenterMaps),
            arrayOf(true, true),
            fbMenu1Maps
        )
    }

    private val tvInfoData2 by lazy { arrayOf(tvInfoSembuh, tvInfoBlok, tvInfoMulti, tvInfoSingle) }
    private val buttonData2 by lazy {
        Triple(
            arrayOf(fbPokokSembuh, fbBlok, fbMultiple, fbSingle),
            arrayOf(true, true, true, true),
            fbMenu2Maps
        )
    }

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateMan().transparentStatusNavBar(window)
        setContentView(R.layout.activity_maps)

        modeAct = 1
        setColorFButton()

        getIdPk = getDataIntent("idPk")
        getEst = getDataIntent("est")
        getAfd = getDataIntent("afd")
        getBlok = getDataIntent("blok")
        getBlokPlot = getDataIntent("blokPlot")
        getPos = getDataIntent("pos")
        urlCategory = PrefManager(this).dataReg!!

        val prefManager = PrefManager(this)
        isProgrammer = prefManager.jabatan == "Programmer"

        val availableRAM = getAvailableRAM(this)
        if (availableRAM > 8 || Build.MODEL.contains("A32")) {
            minRangeAct = 9
        }

        if (jobTitles.any { prefManager.jabatan?.contains(it) == true }) {
            fbActionMaps.setImageResource(R.drawable.baseline_checklist_24)
        }

        Glide.with(this)//GLIDE LOGO FOR LOADING LAYOUT
            .load(R.drawable.logo_png_white)
            .into(loadingVerif.logoFileLoader)
        Glide.with(this)//GLIDE LOGO FOR LOADING LAYOUT
            .load(R.drawable.ssms_green)
            .into(loadingVerif.logoFileLoader)
        loadingVerif.lottieFileLoader.setAnimation("loading_circle.json")//ANIMATION WITH LOTTIE FOR LOADING LAYOUT
        @Suppress("DEPRECATION")
        loadingVerif.lottieFileLoader.loop(true)
        loadingVerif.lottieFileLoader.playAnimation()
        loadingVerif.tvHintFileLoader.text = "Mohon tunggu, sedang memproses"

        getListPupuk()
        updateValuesFromBundle(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)
        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()
        mRequestingLocationUpdates = true

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.setMultiTouchControls(true)
        mapView.setBuiltInZoomControls(true)

        // Initialize sensor manager and sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        if (getEst == "NBE") {
            if (getBlokPlot.length == 5) {
                if (getBlokPlot.contains("D") && getAfd.contains("OC")) {
                    fixBlok = getBlokPlot[0] + "0" + getBlokPlot.substring(1, 3)
                } else {
                    fixBlok = getBlokPlot.substring(0, getBlokPlot.length - 2)
                }
            } else if (getBlokPlot.length == 4) {
                fixBlok = getBlokPlot[0] + getBlokPlot.substring(2)
            }
        } else {
            if (getBlokPlot.startsWith("P")) {
                val sliced = getBlokPlot.substring(0, getBlokPlot.length - 2)
                fixBlok = sliced + "0" + getBlokPlot.takeLast(2)
            } else if (getBlokPlot.length == 5) {
                val sliced = getBlokPlot.substring(0, getBlokPlot.length - 2)
                fixBlok = sliced.replaceRange(1, 1, "0")
            } else if (getEst == "KTE" || getEst == "MKE" || getEst == "PKE" || getEst == "BSE" || getEst == "BWE" || getEst == "GDE") {
                if (getBlokPlot.length == 6 && getBlokPlot[0] == 'H') {
                    val sliced = getBlokPlot.substring(0, getBlokPlot.length - 2)
                    fixBlok = sliced[0] + sliced.substring(2)
                } else if (getBlokPlot.length == 6) {
                    fixBlok = getBlokPlot.substring(0, getBlokPlot.length - 3)
                }
            } else if (getBlokPlot.length == 8) {
                val replace = getBlokPlot.replaceRange(1, 2, "")
                val sliced = replace.substring(0, replace.length - 2)
                fixBlok = getBlokPlot.substring(0, getBlokPlot.length - 3)
            } else if (getBlokPlot.length == 7) {
                val replace = getBlokPlot.replaceRange(1, 2, "")
                val sliced = replace.substring(0, replace.length - 2)
                fixBlok = getBlokPlot.substring(0, getBlokPlot.length - 3)
            } else if (getBlokPlot.length == 6) {
                val replace = getBlokPlot.replaceRange(1, 2, "")
                val sliced = replace.substring(0, replace.length - 2)
                fixBlok = sliced.replaceRange(1, 1, "0")
            } else if (getBlokPlot.length == 3) {
                val sliced = getBlokPlot
                fixBlok = sliced.replaceRange(1, 1, "0")
            } else if (getBlokPlot.contains("CBI") && getBlokPlot.length == 9) {
                val sliced = getBlokPlot.substring(0, getBlokPlot.length - 6)
                fixBlok = sliced.replaceRange(1, 1, "0")
            } else if (getBlokPlot.contains("CBI")) {
                fixBlok = getBlokPlot.substring(0, getBlokPlot.length - 4)
            } else if (getBlokPlot.contains("CB")) {
                val replace = getBlokPlot.replaceRange(1, 2, "")
                val sliced = replace.substring(0, replace.length - 3)
                fixBlok = sliced.replaceRange(1, 1, "0")
            } else if (PrefManager(this).idReg == 7 || PrefManager(this).idReg == 8) {
                fixBlok = getBlokPlot.substring(0, 3)
            } else if (PrefManager(this).idReg == 10 || PrefManager(this).idReg == 11) {
                fixBlok = getBlokPlot.substring(0, 4)
            }
        }

        val blokArray = ArrayList<String>()
        val latlnValues = ArrayList<GeoPoint>()
        val coordinates = mutableListOf<List<GeoPoint>>()
        var currentCoordinateList = mutableListOf<GeoPoint>()
        val mapsPath = this.getExternalFilesDir(null)?.absolutePath + "/MAIN/maps" + urlCategory
        val fileMaps = File(mapsPath)
        if (fileMaps.exists()) {
            try {
                val readMaps = fileMaps.readText()
                val objMaps = JSONObject(readMaps)
                val estObjMaps = objMaps.getJSONObject(getEst)
                if (getAfd.isEmpty()) {
                    val afdKeys = estObjMaps.keys()
                    for (afdKey in afdKeys) {
                        val afdObjMaps = estObjMaps.getJSONObject(afdKey)
                        val indicesAfd = afdObjMaps.keys()
                        for (index in indicesAfd) {
                            val blokObjMaps = afdObjMaps.getJSONObject(index)
                            val splLatLn = blokObjMaps.getString("latln").split("$").toTypedArray()
                            if (index.isNotEmpty()) {
                                blokArray.add(index)
                                for (item in splLatLn) {
                                    if (item.isBlank()) {
                                        if (currentCoordinateList.isNotEmpty()) {
                                            coordinates.add(currentCoordinateList.toList())
                                            currentCoordinateList.clear()
                                        }
                                    } else {
                                        val parts = item.split(", ")
                                        if (parts.size == 2) {
                                            val latitude = parts[0].toDouble()
                                            val longitude = parts[1].toDouble()
                                            val geoPoint = GeoPoint(latitude, longitude)

                                            latlnValues.add(geoPoint) // To get all latlon and be get a average of lat lon to centered maps
                                            currentCoordinateList.add(geoPoint)
                                        }
                                    }
                                }

                                if (currentCoordinateList.isNotEmpty()) {
                                    coordinates.add(currentCoordinateList.toList())
                                }
                            }
                        }
                    }
                } else {
                    if (fixBlok.isEmpty()) {
                        val indicesAfd = estObjMaps.getJSONObject(getAfd).keys()
                        for (index in indicesAfd) {
                            val blokObjMaps = estObjMaps.getJSONObject(getAfd).getJSONObject(index)
                            val splLatLn = blokObjMaps.getString("latln").split("$").toTypedArray()
                            if (index.isNotEmpty()) {
                                blokArray.add(index)
                                for (item in splLatLn) {
                                    if (item.isBlank()) {
                                        if (currentCoordinateList.isNotEmpty()) {
                                            coordinates.add(currentCoordinateList.toList())
                                            currentCoordinateList.clear()
                                        }
                                    } else {
                                        val parts = item.split(", ")
                                        if (parts.size == 2) {
                                            val latitude = parts[0].toDouble()
                                            val longitude = parts[1].toDouble()
                                            val geoPoint = GeoPoint(latitude, longitude)

                                            latlnValues.add(geoPoint) // To get all latlon and be get a average of lat lon to centered maps
                                            currentCoordinateList.add(geoPoint)
                                        }
                                    }
                                }

                                if (currentCoordinateList.isNotEmpty()) {
                                    coordinates.add(currentCoordinateList.toList())
                                }
                            }
                        }
                    } else {
                        val blokObjMaps = estObjMaps.getJSONObject(getAfd).getJSONObject(fixBlok)
                        val splLatLn = blokObjMaps.getString("latln").split("$").toTypedArray()
                        if (fixBlok.isNotEmpty()) {
                            blokArray.add(fixBlok)
                            for (item in splLatLn) {
                                if (item.isBlank()) {
                                    if (currentCoordinateList.isNotEmpty()) {
                                        coordinates.add(currentCoordinateList.toList())
                                        currentCoordinateList.clear()
                                    }
                                } else {
                                    val parts = item.split(", ")
                                    if (parts.size == 2) {
                                        val latitude = parts[0].toDouble()
                                        val longitude = parts[1].toDouble()
                                        val geoPoint = GeoPoint(latitude, longitude)

                                        latlnValues.add(geoPoint) // To get all latlon and be get a average of lat lon to centered maps
                                        currentCoordinateList.add(geoPoint)
                                    }
                                }
                            }

                            if (currentCoordinateList.isNotEmpty()) {
                                coordinates.add(currentCoordinateList.toList())
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("ET", "Error: $e")
                e.printStackTrace()
            }
        } else {
            Log.d("ET", "The JSON file maps does not exist.")
        }

        val mapsCenter = calculateCenter(latlnValues)
        val geoPoint =
            if (firstGPS) {
                GeoPoint(lat!!.toDouble(), lon!!.toDouble())
            } else if (mapsCenter != null) {
                mapsCenter
            } else {
                if (getPos.isNotEmpty()) {
                    val currentPos = getPos.split("$")
                    GeoPoint(currentPos[0].toDouble(), currentPos[1].toDouble())
                } else {
                    GeoPoint(0.0, 0.0)
                }
            }
        mapView.controller.animateTo(geoPoint)
        mapView.controller.setZoom(15.0)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        for (i in coordinates.indices) {
            val polygon = Polygon()
            polygon.points = coordinates[i]
            polygon.fillPaint.color = 0x1523CB1F
            polygon.strokeColor = 0xFF000000.toInt()
            polygon.strokeWidth = 2f

            val markerText = Marker(mapView)
            markerText.position = calculateCenter(coordinates[i])
            markerText.setTextIcon(blokArray[i])
            mapView.overlays.add(markerText)

            mapView.overlays.add(polygon)

            polygon.setOnClickListener { poly, mv, ep ->
                if (modeAct == 3) {
                    if (firstGPS) {
                        if (currentlyClickedPolygon != poly) {
                            val geopointUser = GeoPoint(lat!!.toDouble(), lon!!.toDouble())
                            val isUserInsidePolygon =
                                isPointInsidePolygon(geopointUser, coordinates[i])

                            if (isProgrammer || isUserInsidePolygon) {
                                markerIds.clear()
                                markerAfd.clear()
                                markerBlok.clear()
                                markerCons.clear()
                                markerStats.clear()

                                if (currentlyClickedPolygon != null) {
                                    currentlyClickedPolygon!!.fillPaint.color = 0x1523CB1F
                                }

                                for (j in modelMainList.indices) {
                                    val geopointPk =
                                        GeoPoint(modelMainList[j].latPk, modelMainList[j].lonPk)
                                    val isMarkerInsidePolygon =
                                        isPointInsidePolygon(geopointPk, coordinates[i])

                                    val polyId = modelMainList[j].idPk
                                    val polyAfdl = modelMainList[j].afdPk
                                    val polyBloks = modelMainList[j].blokPk
                                    val polyCon = modelMainList[j].kondisiPk
                                    val polyStat = modelMainList[j].statusPk

                                    fun procesInsidePoly() {
                                        markerIds.add(polyId)
                                        markerAfd.add(polyAfdl)
                                        markerBlok.add(polyBloks)
                                        markerCons.add(polyCon)
                                        markerStats.add(polyStat)
                                    }

                                    if (isMarkerInsidePolygon) {
                                        if (jobTitles.any {
                                                prefManager.jabatan?.contains(
                                                    it
                                                ) == true
                                            }) {
                                            if (modeAct != 4) {
                                                if (polyStat == "Sudah" || (polyStat != "Terverifikasi" && polyCon == "Sembuh")) {
                                                    procesInsidePoly()
                                                }
                                            }
                                        } else {
                                            if ((polyCon == "Berat" || polyCon == "Ringan" || polyCon == "Pucat") && polyStat == "Belum") {
                                                procesInsidePoly()
                                            }
                                        }
                                    }
                                }

                                if (isProgrammer || markerIds.size >= maxActBlok) {
                                    poly.fillPaint.color =
                                        ContextCompat.getColor(
                                            this@MapsActivity,
                                            R.color.colorPrimaryDark
                                        )
                                    currentlyClickedPolygon = poly
                                } else {
                                    currentlyClickedPolygon = null
                                    markerIds.clear()
                                    markerAfd.clear()
                                    markerBlok.clear()
                                    markerCons.clear()
                                    markerStats.clear()
                                    Toasty.warning(
                                        this@MapsActivity,
                                        "Maaf, penanganan blok hanya dapat dilakukan jika titik melebihi $maxActBlok!",
                                        Toasty.LENGTH_LONG
                                    )
                                        .show()
                                }
                                tvPokokMaps.text =
                                    if (markerIds.isNotEmpty()) "${markerIds.size}/${markerIds.size}" else "-"
                            } else {
                                Toasty.warning(
                                    this@MapsActivity,
                                    "Anda belum berada di blok ${blokArray[i]}!",
                                    Toasty.LENGTH_LONG
                                )
                                    .show()
                            }
                        } else {
                            Toasty.warning(
                                this@MapsActivity,
                                "Anda telah memilih blok ${blokArray[i]}!",
                                Toasty.LENGTH_LONG
                            )
                                .show()
                        }
                    } else {
                        Toasty.warning(
                            this@MapsActivity,
                            "Titik GPS belum didapatkan!",
                            Toasty.LENGTH_LONG
                        )
                            .show()
                    }
                }

                return@setOnClickListener true
            }

            mapView.invalidate()
        }

        fbMenu1Maps.setOnClickListener { onAddButtonClicked(buttonData1, tvInfoData1) }
        fbMenu2Maps.setOnClickListener { onAddButtonClicked(buttonData2, tvInfoData2) }

        fbActionMaps.setOnClickListener {
            if (firstGPS) {
                if (markerIds.isEmpty()) {
                    Toasty.warning(
                        this@MapsActivity,
                        "Silakan pilih pokok kuning terlebih dahulu!",
                        Toasty.LENGTH_LONG
                    )
                        .show()
                } else if (!isProgrammer && (fixAccuracy > minAccuracyGPS || accuracyRange > minRangeAct)) {
                    Toasty.warning(
                        this@MapsActivity,
                        "GPS belum memenuhi syarat!",
                        Toasty.LENGTH_LONG
                    )
                        .show()
                } else if (jobTitles.any { prefManager.jabatan?.contains(it) == true } && modeAct != 4) {
                    AlertDialogUtility.withTwoActions(
                        this@MapsActivity,
                        "Batal",
                        "Ya",
                        "Apakah anda yakin untuk melakukan verifikasi?",
                        "warning.json"
                    ) {
                        inserting = true
                        clLayoutVerif.visibility = View.VISIBLE

                        coroutineScope.launch {
                            withContext(Dispatchers.IO) {
                                insertData()
                            }

                            withContext(Dispatchers.Main) {
                                clLayoutVerif.visibility = View.GONE
                                inserting = false
                            }
                        }
                    }
                } else {
                    AlertDialogUtility.withTwoActions(
                        this@MapsActivity,
                        "Batal",
                        "Ya",
                        "Apakah anda yakin untuk melakukan penanganan?",
                        "warning.json"
                    ) {
                        stopLocationUpdates()
                        mapView.onPause()

                        val intent =
                            Intent(
                                this@MapsActivity,
                                HandlingFormActivity::class.java
                            )
                                .putExtra("id", getCommaSeparatedExtraData(markerIds))
                                .putExtra("est", getEst)
                                .putExtra("afd", getCommaSeparatedExtraData(markerAfd))
                                .putExtra("blok", getCommaSeparatedExtraData(markerBlok))
                                .putExtra("kondisi", getCommaSeparatedExtraData(markerCons))
                                .putExtra("status", getCommaSeparatedExtraData(markerStats))
                                .putExtra("mode", modeAct.toString())
                                .putExtra("gps", "GA")
                        startActivity(intent)
                        finishAffinity()
                    }
                }
            } else {
                Toasty.warning(
                    this@MapsActivity,
                    "Titik GPS belum didapatkan!",
                    Toasty.LENGTH_LONG
                )
                    .show()
            }

            onAddButtonClicked(buttonData1, tvInfoData1)
        }

        fbCenterMaps.setOnClickListener {
            if (firstGPS) {
                val centerPoint = GeoPoint(lat!!.toDouble(), lon!!.toDouble())
                mapView.controller.animateTo(centerPoint)
            } else {
                Toasty.warning(
                    this@MapsActivity,
                    "Titik GPS belum didapatkan!",
                    Toasty.LENGTH_LONG
                )
                    .show()
            }

            onAddButtonClicked(buttonData1, tvInfoData1)
        }

        fbPokokSembuh.setOnClickListener {
            if (modeAct == 4) {
                Toasty.warning(
                    this@MapsActivity,
                    "Anda telah memilih pilihan ini.",
                    Toasty.LENGTH_LONG
                )
                    .show()
            } else {
                AlertDialogUtility.withTwoActions(
                    this@MapsActivity,
                    "Batal",
                    "Ya",
                    "Apakah anda ingin melakukan verifikasi pokok sembuh?",
                    "warning.json"
                ) {
                    if (modeAct == 2) {
                        if (jobTitles.any { PrefManager(this).jabatan?.contains(it) == true }) resetMarker(
                            false
                        ) else resetMarker(true)
                    } else if (modeAct == 3) {
                        if (currentlyClickedPolygon != null) {
                            markerIds.clear()
                            markerAfd.clear()
                            markerBlok.clear()
                            markerCons.clear()
                            markerStats.clear()
                            currentlyClickedPolygon!!.fillPaint.color = 0x1523CB1F
                            currentlyClickedPolygon = null
                        }
                    }

                    if (jobTitles.any { PrefManager(this).jabatan?.contains(it) == true }) {
                        resetMarker(false)
                    }

                    maxActTrees = 1
                    tvPokokMaps.text =
                        if (markerIds.isNotEmpty()) "${markerIds.size}/$maxActTrees" else "-"
                    modeAct = 4

                    Toasty.success(
                        this@MapsActivity,
                        "Verifikasi pokok sembuh berhasil dipilih!",
                        Toasty.LENGTH_LONG
                    )
                        .show()
                    setColorFButton()
                }
            }

            onAddButtonClicked(buttonData2, tvInfoData2)
        }

        fbBlok.setOnClickListener {
            if (modeAct == 3) {
                Toasty.warning(
                    this@MapsActivity,
                    "Anda telah memilih pilihan ini.",
                    Toasty.LENGTH_LONG
                )
                    .show()
            } else {
                AlertDialogUtility.withTwoActions(
                    this@MapsActivity,
                    "Batal",
                    "Ya",
                    "Apakah anda ingin melakukan penanganan berdasarkan blok?",
                    "warning.json"
                ) {
                    resetMarker(false)

                    maxActTrees = 1
                    tvPokokMaps.text =
                        if (markerIds.isNotEmpty()) "${markerIds.size}/${markerIds.size}" else "-"
                    modeAct = 3

                    Toasty.success(
                        this@MapsActivity,
                        "Penanganan berdasarkan blok berhasil dipilih!",
                        Toasty.LENGTH_LONG
                    )
                        .show()
                    setColorFButton()
                }
            }

            onAddButtonClicked(buttonData2, tvInfoData2)
        }

        fbMultiple.setOnClickListener {
            if (modeAct == 2) {
                Toasty.warning(
                    this@MapsActivity,
                    "Anda telah memilih pilihan ini.",
                    Toasty.LENGTH_LONG
                )
                    .show()
            } else {
                AlertDialogUtility.withTwoActions(
                    this@MapsActivity,
                    "Batal",
                    "Ya",
                    "Apakah anda ingin melakukan penanganan beberapa titik?",
                    "warning.json"
                ) {
                    if (modeAct == 3) {
                        if (currentlyClickedPolygon != null) {
                            markerIds.clear()
                            markerAfd.clear()
                            markerBlok.clear()
                            markerCons.clear()
                            markerStats.clear()
                            currentlyClickedPolygon!!.fillPaint.color = 0x1523CB1F
                            currentlyClickedPolygon = null
                        }
                    }

                    if (jobTitles.any { PrefManager(this).jabatan?.contains(it) == true }) {
                        resetMarker(false)
                    }

                    maxActTrees = 5
                    tvPokokMaps.text =
                        if (markerIds.isNotEmpty()) "${markerIds.size}/$maxActTrees" else "-"
                    modeAct = 2

                    Toasty.success(
                        this@MapsActivity,
                        "Penanganan beberapa titik berhasil dipilih!",
                        Toasty.LENGTH_LONG
                    )
                        .show()
                    setColorFButton()
                }
            }

            onAddButtonClicked(buttonData2, tvInfoData2)
        }

        fbSingle.setOnClickListener {
            if (modeAct == 1) {
                Toasty.warning(
                    this@MapsActivity,
                    "Anda telah memilih pilihan ini.",
                    Toasty.LENGTH_LONG
                )
                    .show()
            } else {
                AlertDialogUtility.withTwoActions(
                    this@MapsActivity,
                    "Batal",
                    "Ya",
                    "Apakah anda ingin melakukan penanganan per titik?",
                    "warning.json"
                ) {
                    if (modeAct == 2) {
                        if (jobTitles.any { PrefManager(this).jabatan?.contains(it) == true }) resetMarker(
                            false
                        ) else resetMarker(true)
                    } else if (modeAct == 3) {
                        if (currentlyClickedPolygon != null) {
                            markerIds.clear()
                            markerAfd.clear()
                            markerBlok.clear()
                            markerCons.clear()
                            markerStats.clear()
                            currentlyClickedPolygon!!.fillPaint.color = 0x1523CB1F
                            currentlyClickedPolygon = null
                        }
                    }

                    if (jobTitles.any { PrefManager(this).jabatan?.contains(it) == true }) {
                        resetMarker(false)
                    }

                    maxActTrees = 1
                    tvPokokMaps.text =
                        if (markerIds.isNotEmpty()) "${markerIds.size}/$maxActTrees" else "-"
                    modeAct = 1

                    Toasty.success(
                        this@MapsActivity,
                        "Penanganan per titik berhasil dipilih!",
                        Toasty.LENGTH_LONG
                    )
                        .show()
                    setColorFButton()
                }
            }

            onAddButtonClicked(buttonData2, tvInfoData2)
        }

        getLocYellowTrees()
    }

    private fun isPointInsidePolygon(point: GeoPoint, polygon: List<GeoPoint>): Boolean {
        var inside = false
        val x = point.longitude
        val y = point.latitude

        for (i in 0 until polygon.size - 1) {
            val xi = polygon[i].longitude
            val yi = polygon[i].latitude
            val xi1 = polygon[i + 1].longitude
            val yi1 = polygon[i + 1].latitude

            if (((yi <= y && y < yi1) || (yi1 <= y && y < yi)) &&
                (x < (xi1 - xi) * (y - yi) / (yi1 - yi) + xi)
            ) {
                inside = !inside
            }
        }

        return inside
    }

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                val deviceOrientation = event.values[0]
                val mapOrientation = (360 - deviceOrientation) % 360
                mapView.mapOrientation = mapOrientation
                mapView.invalidate()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    private fun resetMarker(single: Boolean) {
        if (markerIds.isNotEmpty()) {
            var exceptLastId =
                if (single) markerIds.dropLast(1).toTypedArray() else markerIds.toTypedArray()
            var exceptLastCon =
                if (single) markerCons.dropLast(1).toTypedArray() else markerCons.toTypedArray()
            var exceptLastStat =
                if (single) markerStats.dropLast(1).toTypedArray() else markerStats.toTypedArray()

            if (single) {
                val lastElementId = markerIds.last()
                markerIds.clear()
                markerIds.add(lastElementId)

                val lastElementAfd = markerAfd.last()
                markerAfd.clear()
                markerAfd.add(lastElementAfd)

                val lastElementBlok = markerBlok.last()
                markerBlok.clear()
                markerBlok.add(lastElementBlok)

                val lastElementCon = markerCons.last()
                markerCons.clear()
                markerCons.add(lastElementCon)

                val lastElementStat = markerStats.last()
                markerStats.clear()
                markerStats.add(lastElementStat)
            } else {
                markerIds.clear()
                markerAfd.clear()
                markerBlok.clear()
                markerCons.clear()
                markerStats.clear()
            }

            var drawable: Drawable? = null
            for (j in exceptLastId.indices) {
                for (myMarker in markerMap.values) {
                    if (myMarker.id == exceptLastId[j].toString()) {
                        drawable = ContextCompat.getDrawable(
                            this,
                            if (exceptLastStat[j] == "Terverifikasi") R.drawable.baseline_check_circle_24 else if (exceptLastStat[j] == "Sudah" || exceptLastCon[j] == "Sembuh") R.drawable.baseline_circle_24 else R.drawable.ic_close
                        )
                        val color = ContextCompat.getColor(
                            this,
                            if (exceptLastStat[j] == "Terverifikasi") {
                                R.color.colorGreen_A400
                            } else if (exceptLastStat[j] == "Sudah") {
                                R.color.green1
                            } else if (exceptLastCon[j] == "Pucat") {
                                R.color.grey_default
                            } else if (exceptLastCon[j] == "Ringan") {
                                R.color.dashboard
                            } else if (exceptLastCon[j] == "Berat") {
                                R.color.colorRed_A400
                            } else {
                                R.color.blue1
                            }
                        )
                        drawable?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

                        myMarker.icon = drawable
                        mapView.invalidate()
                    }
                }
            }
        }
    }

    private fun getLocYellowTrees() {
        try {
            val pkPath = this.getExternalFilesDir(null)?.absolutePath + "/MAIN/pk" + urlCategory
            val fileMaps = File(pkPath)
            if (fileMaps.exists()) {
                try {
                    val readMaps = fileMaps.readText()
                    val objMaps = JSONObject(readMaps)

                    val estObjMaps = objMaps.getJSONObject(getEst)
                    if (getAfd.isEmpty()) {
                        for (indexEst in estObjMaps.keys()) {
                            var itemEst = estObjMaps.getJSONObject(indexEst)
                            for (indexAfd in itemEst.keys()) {
                                var itemAfd = itemEst.getJSONObject(indexAfd)
                                for (index in itemAfd.keys()) {
                                    val item = itemAfd.getJSONObject(index)
                                    val modelMain = ModelMain()
                                    modelMain.idPk = index.toInt()
                                    modelMain.afdPk = indexEst
                                    modelMain.blokPk = indexAfd
                                    modelMain.kondisiPk = item.getString("kondisi")
                                    modelMain.statusPk = item.getString("status")
                                    modelMain.tglPk = item.getString("tgl_foto_udara")
                                    modelMain.perlakuanPk = try {
                                        item.getString("perlakuan")
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    modelMain.tglPerlakuanPk = try {
                                        item.getString("tanggal")
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    val fixLatLn =
                                        item.getString("latln").replace(" ", "").split(",")
                                            .toTypedArray()
                                    modelMain.latPk = fixLatLn[0].trim().toDouble()
                                    modelMain.lonPk = fixLatLn[1].trim().toDouble()
                                    modelMainList.add(modelMain)
                                }
                            }
                        }
                    } else {
                        val afdObjMaps = estObjMaps.getJSONObject(getAfd)
                        if (getBlok.isEmpty()) {
                            for (indexAfd in afdObjMaps.keys()) {
                                var itemAfd = afdObjMaps.getJSONObject(indexAfd)
                                for (index in itemAfd.keys()) {
                                    val item = itemAfd.getJSONObject(index)
                                    val modelMain = ModelMain()
                                    modelMain.idPk = index.toInt()
                                    modelMain.afdPk = getAfd
                                    modelMain.blokPk = indexAfd
                                    modelMain.kondisiPk = item.getString("kondisi")
                                    modelMain.statusPk = item.getString("status")
                                    modelMain.tglPk = item.getString("tgl_foto_udara")
                                    modelMain.perlakuanPk = try {
                                        item.getString("perlakuan")
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    modelMain.tglPerlakuanPk = try {
                                        item.getString("tanggal")
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    val fixLatLn =
                                        item.getString("latln").replace(" ", "").split(",")
                                            .toTypedArray()
                                    modelMain.latPk = fixLatLn[0].trim().toDouble()
                                    modelMain.lonPk = fixLatLn[1].trim().toDouble()
                                    modelMainList.add(modelMain)
                                }
                            }
                        } else {
                            val blokObjMaps = afdObjMaps.getJSONObject(getBlok)
                            for (index in blokObjMaps.keys()) {
                                val item = blokObjMaps.getJSONObject(index)
                                val modelMain = ModelMain()
                                modelMain.idPk = index.toInt()
                                modelMain.afdPk = getAfd
                                modelMain.blokPk = getBlok
                                modelMain.kondisiPk = item.getString("kondisi")
                                modelMain.statusPk = item.getString("status")
                                modelMain.tglPk = item.getString("tgl_foto_udara")
                                modelMain.perlakuanPk = try {
                                    item.getString("perlakuan")
                                } catch (e: Exception) {
                                    ""
                                }
                                modelMain.tglPerlakuanPk = try {
                                    item.getString("tanggal")
                                } catch (e: Exception) {
                                    ""
                                }
                                val fixLatLn =
                                    item.getString("latln").replace(" ", "").split(",")
                                        .toTypedArray()
                                modelMain.latPk = fixLatLn[0].trim().toDouble()
                                modelMain.lonPk = fixLatLn[1].trim().toDouble()
                                modelMainList.add(modelMain)
                            }
                        }
                    }
                    initMarker(modelMainList)
                } catch (e: Exception) {
                    Log.d("ET", "Error: $e")
                    e.printStackTrace()
                }
            } else {
                Log.d("ET", "The JSON file maps does not exist.")
            }
        } catch (e: IOException) {
            Log.d("ET", "Error: $e")
        }
    }

    @SuppressLint("SetTextI18n", "ResourceType")
    private fun initMarker(modelList: List<ModelMain>) {
        runOnUiThread {
            val prefManager = PrefManager(this)

            val arrBelum = ArrayList<Int>()
            val arrSudah = ArrayList<Int>()

            for (j in modelList.indices) {
                if (modelList[j].statusPk == "Sudah") {
                    arrSudah.add(j)
                } else {
                    arrBelum.add(j)
                }
            }

            for (i in modelList.indices) {
                var selectedIcon =
                    ContextCompat.getDrawable(
                        this,
                        if (modelList[i].statusPk == "Sudah" || modelList[i].kondisiPk == "Sembuh") R.drawable.baseline_check_circle_24 else R.drawable.baseline_close_circle_24
                    )
                var drawable = ContextCompat.getDrawable(
                    this,
                    if (modelList[i].statusPk == "Terverifikasi") R.drawable.baseline_check_circle_24 else if (modelList[i].statusPk == "Sudah" || modelList[i].kondisiPk == "Sembuh") R.drawable.baseline_circle_24 else R.drawable.ic_close
                )
                val color = ContextCompat.getColor(
                    this,
                    if (modelList[i].statusPk == "Terverifikasi") {
                        R.color.colorGreen_A400
                    } else if (modelList[i].statusPk == "Sudah") {
                        R.color.green1
                    } else if (modelList[i].kondisiPk == "Pucat") {
                        R.color.grey_default
                    } else if (modelList[i].kondisiPk == "Ringan") {
                        R.color.dashboard
                    } else if (modelList[i].kondisiPk == "Berat") {
                        R.color.colorRed_A400
                    } else {
                        R.color.blue1
                    }
                )
                drawable?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                selectedIcon?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

                val widthInPixels = 100
                val heightInPixels = 100
                val bitmap =
                    Bitmap.createBitmap(widthInPixels, heightInPixels, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                selectedIcon?.setBounds(0, 0, widthInPixels, heightInPixels)
                selectedIcon?.draw(canvas)
                selectedIcon = BitmapDrawable(resources, bitmap)

                val resultAction = ArrayList<String>()
                val splitPerlakuan = modelList[i].perlakuanPk.split("$")
                for (j in perlakuanIdArray.indices) {
                    for (k in splitPerlakuan.indices) {
                        if (splitPerlakuan[k] == perlakuanIdArray[j].toString()) {
                            resultAction.add("- " + perlakuanArray[j])
                        }
                    }
                }

                var inputDate = modelList[i].tglPerlakuanPk
                val inputFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale("id", "ID"))
                val outputFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))

                try {
                    val dateParse = inputFormat.parse(inputDate)
                    inputDate = outputFormat.format(dateParse)
                } catch (e: Exception) {
                    e.printStackTrace()
                }

                val snpAction =  if (resultAction.isNotEmpty()) {
                    "<br>Jenis perlakuan:<br>" + resultAction.toTypedArray().contentToString()
                        .replace("[", "").replace("]", "").replace(
                            ", ",
                            "<br>"
                        )
                } else {
                    ""
                }
                val snpDate = if (inputDate.isNotEmpty()) {
                    "<br>Tanggal perlakuan:<br>$inputDate"
                } else {
                    ""
                }
                val snippetAction = if (snpAction.isNotEmpty() && snpDate.isNotEmpty()) {
                    "$snpAction<br>$snpDate"
                } else {
                    "$snpAction$snpDate"
                }

                val yellowMarkers = Marker(mapView)
                yellowMarkers.id = modelList[i].idPk.toString()
                yellowMarkers.icon = drawable
                yellowMarkers.position = GeoPoint(modelList[i].latPk, modelList[i].lonPk)
                yellowMarkers.title = if ((modelList[i].statusPk == "Belum" && modelList[i].kondisiPk != "Sembuh") || snippetAction.isEmpty()) {
                    modelList[i].idPk.toString()
                } else {
                    modelList[i].idPk.toString() + "\n$getEst - ${modelList[i].afdPk}"
                }
                yellowMarkers.snippet = if ((modelList[i].statusPk == "Belum" && modelList[i].kondisiPk != "Sembuh") || snippetAction.isEmpty()) {
                    "$getEst - ${modelList[i].afdPk}"
                } else {
                    snippetAction
                }
                yellowMarkers.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                val formattedDate = SimpleDateFormat("MMM yyyy", Locale("id", "ID"))
                tvTglFotoMaps.text = formattedDate.format(
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.ENGLISH
                    ).parse(modelList[i].tglPk)!!
                )
                tvJmlPkMaps.text = "${arrSudah.size}/${arrBelum.size}"
                tvBlokMaps.text = if (getBlok.isNotEmpty()) getBlok else "-"

                yellowMarkers.setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
                    override fun onMarkerClick(marker: Marker, mapView: MapView): Boolean {
                        if (modeAct == 1 || modeAct == 2 || modeAct == 4) {
                            if (marker == lastClickedMarker) {
                                mapView.overlays.remove(previousDashedLine)
                                lastClickedMarker?.closeInfoWindow()
                                previousDashedLine = null
                                lastClickedMarker = null
                                latPk = null
                                lonPk = null
                                tvJarakMaps.text = "-"
                                tvJarakMaps.setTextColor(Color.BLACK)
                            } else {
                                val latCurrent = try {
                                    lat!!.toDouble()
                                } catch (e: Exception) {
                                    0.0
                                }
                                val lonCurrent = try {
                                    lon!!.toDouble()
                                } catch (e: Exception) {
                                    0.0
                                }

                                lastClickedMarker = marker

                                latPk = modelList[i].latPk
                                lonPk = modelList[i].lonPk

                                createDashLineOverlay(latCurrent, lonCurrent, latPk!!, lonPk!!)
                                rangePos()

                                marker.showInfoWindow()
                            }

                            marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)

                            val markerId = modelList[i].idPk
                            val markerAfdl = modelList[i].afdPk
                            val markerBloks = modelList[i].blokPk
                            val markerCon = modelList[i].kondisiPk
                            val markerStat = modelList[i].statusPk

                            fun processMarker() {
                                if (isProgrammer || (accuracyRange <= minRangeAct)) {
                                    if (markerIds.size < maxActTrees || markerIds.contains(markerId)) {
                                        if (markerIds.contains(markerId)) {
                                            markerIds.remove(markerId)
                                            markerAfd.remove(markerAfdl)
                                            markerBlok.remove(markerBloks)
                                            markerCons.remove(markerCon)
                                            markerStats.remove(markerStat)
                                            marker.icon = drawable
                                        } else {
                                            if (markerIds.size == maxActTrees) {
                                                markerIds.remove(markerId)
                                                markerAfd.remove(markerAfdl)
                                                markerBlok.remove(markerBloks)
                                                markerCons.remove(markerCon)
                                                markerStats.remove(markerStat)
                                                marker.icon = drawable
                                            }
                                            markerIds.add(markerId)
                                            markerAfd.add(markerAfdl)
                                            markerBlok.add(markerBloks)
                                            markerCons.add(markerCon)
                                            markerStats.add(markerStat)
                                            marker.icon = selectedIcon
                                        }
                                    } else {
                                        if (marker == lastClickedMarker) {
                                            val textMax =
                                                if (jobTitles.any { prefManager.jabatan?.contains(it) == true }) "memverifikasi" else "menangani"
                                            Toasty.warning(
                                                this@MapsActivity,
                                                "Maksimal hanya dapat $textMax $maxActTrees titik!"
                                            ).show()
                                        }
                                    }
                                } else {
                                    Toasty.warning(
                                        this@MapsActivity,
                                        "Anda belum berada di dekat titik!"
                                    ).show()
                                }
                            }

                            if (jobTitles.any { prefManager.jabatan?.contains(it) == true }) {
                                if (modeAct != 4) {
                                    if (markerStat == "Sudah" || (markerStat != "Terverifikasi" && markerCon == "Sembuh")) {
                                        processMarker()
                                    }
                                } else {
                                    if ((markerCon == "Berat" || markerCon == "Ringan" || markerCon == "Pucat") && markerStat == "Belum") {
                                        processMarker()
                                    }
                                }
                            } else {
                                if ((markerCon == "Berat" || markerCon == "Ringan" || markerCon == "Pucat") && markerStat == "Belum") {
                                    processMarker()
                                }
                            }

                            tvBlokMaps.text = markerBloks
                            tvKondisiMaps.text = if (marker == lastClickedMarker) markerCon else "-"
                            tvStatusMaps.text = if (marker == lastClickedMarker) {
                                markerStat + if (markerStat != "Terverifikasi") " ditangani" else ""
                            } else {
                                "-"
                            }
                            tvPokokMaps.text = "${markerIds.size}/$maxActTrees"

                            mapView.invalidate()
                        } else {
                            if (modeAct == 3) {
                                Toasty.warning(
                                    this@MapsActivity,
                                    "Jenis penanganan telah dipilih berdasarkan blok!"
                                ).show()
                            } else {
                                Toasty.warning(
                                    this@MapsActivity,
                                    "Silakan pilih jenis penanganan terlebih dahulu!"
                                ).show()
                            }
                        }

                        return true
                    }
                })

                mapView.overlays.add(yellowMarkers)
                mapView.invalidate()

                markerMap[yellowMarkers.id] = yellowMarkers
            }
        }
    }

    private fun getCommaSeparatedExtraData(list: List<Any>): String {
        return list.joinToString("$") { it.toString() }
    }

    private fun getAvailableRAM(context: Context): Long {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memoryInfo = ActivityManager.MemoryInfo()
        activityManager.getMemoryInfo(memoryInfo)
        return memoryInfo.availMem / (1024 * 1024 * 1024)
    }

    private fun insertData() {
        val pm = PrefManagerEstate(this)
        val pmPrevCons = try {
            pm.prevCons!!
        } catch (e: Exception) {
            ""
        }
        val arrPrevCons = ArrayList<String>()
        if (pmPrevCons.isNotEmpty()) {
            val splitPrevCons = pm.prevCons!!.replace("[", "").replace("]", "").replace(" ", "").split(",")
            for (a in splitPrevCons.indices) {
                arrPrevCons.add(splitPrevCons[a])
            }
        }

        val databaseHandler = PemupukanSQL(this)
        val splitIdPk = getCommaSeparatedExtraData(markerIds).split("$")
        val splitAfdPk = getCommaSeparatedExtraData(markerAfd).split("$")
        val splitBlokPk = getCommaSeparatedExtraData(markerBlok).split("$")
        val splitConsPk = getCommaSeparatedExtraData(markerCons).split("$")
        val splitStatsPk = getCommaSeparatedExtraData(markerStats).split("$")
        for (a in splitIdPk.indices) {
            if (splitIdPk[a].isNotEmpty()) {
                val dateNow = SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(
                    Calendar.getInstance().time
                )

                val status = databaseHandler.addPokokKuning(
                    idPk = splitIdPk[a].toInt(),
                    estate = getEst,
                    afdeling = splitAfdPk[a],
                    blok = splitBlokPk[a],
                    status = "Terverifikasi",
                    kondisi = splitConsPk[a],
                    datetime = dateNow,
                    jenisPupukId = "",
                    dosis = "",
                    foto = "",
                    komen = "",
                    app_ver = "${BuildConfig.VERSION_NAME};${Build.VERSION.RELEASE};${Build.MODEL};GA"
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

                            val estObjMaps =
                                objMaps.getJSONObject(getEst)
                            val afdObjMaps =
                                estObjMaps.getJSONObject(splitAfdPk[a])
                            val blokObjMaps =
                                afdObjMaps.getJSONObject(splitBlokPk[a])
                            for (index in blokObjMaps.keys()) {
                                val item =
                                    blokObjMaps.getJSONObject(index)
                                if (splitIdPk[a] == index) {
                                    if (splitStatsPk[a] == "Sudah" || splitConsPk[a] == "Sembuh") {
                                        arrPrevCons.removeIf { it.contains(splitIdPk[a]) }
                                        val tglAct = if (item.has("tanggal")) {
                                            "$${item.getString("tanggal").replace(" ", "|")}"
                                        } else {
                                            "$"
                                        }
                                        arrPrevCons.add("${splitIdPk[a]}$${item.getString("kondisi")}$${item.getString("status")}$tglAct")
                                    }

                                    item.put("status", "Terverifikasi")
                                    item.put("tanggal", dateNow)
                                }
                            }
                            fileMaps.writeText(objMaps.toString())

                            if (a == splitIdPk.size - 1) {
                                inserting = false
                                runOnUiThread {
                                    stopLocationUpdates()
                                    if (mapView != null) {
                                        mapView.onPause()
                                    }

                                    AlertDialogUtility.withSingleAction(
                                        this,
                                        "OK",
                                        "Data pokok kuning berhasil disimpan!",
                                        "success.json"
                                    ) {
                                        if (arrPrevCons.isNotEmpty()) {
                                            pm.prevCons =
                                                arrPrevCons.toTypedArray().contentToString()
                                        }

                                        Toasty.info(this, "Mohon tunggu, sedang memproses peta kembali..", Toasty.LENGTH_LONG).show()

                                        val intent =
                                            Intent(this, MapsActivity::class.java).putExtra("est", getEst)
                                                .putExtra("afd", getAfd)
                                                .putExtra("blok", getBlok)
                                                .putExtra("blokPlot", getBlokPlot)
                                                .putExtra("pos", "$lat$$lon")
                                        startActivity(intent)
                                        finishAffinity()
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            inserting = false
                            runOnUiThread {
                                Toasty.warning(
                                    this@MapsActivity,
                                    "Terjadi kesalahan, hubungi pengembang. Error: $e",
                                    Toasty.LENGTH_LONG
                                )
                                    .show()
                            }
                            e.printStackTrace()
                            break
                        }
                    } else {
                        inserting = false
                        runOnUiThread {
                            Toasty.warning(
                                this@MapsActivity,
                                "File JSON tidak ditemukan!",
                                Toasty.LENGTH_LONG
                            )
                                .show()
                        }
                        break
                    }
                } else {
                    inserting = false
                    runOnUiThread {
                        AlertDialogUtility.alertDialog(
                            this,
                            "Terjadi kesalahan, hubungi pengembang",
                            "warning.json"
                        )
                    }
                    break
                }
            }
        }
    }

    private fun getDataIntent(data: String? = null): String {
        return try {
            intent.getStringExtra(data)!!
        } catch (e: Exception) {
            ""
        }
    }

    private fun onAddButtonClicked(data: Triple<Array<FloatingActionButton>, Array<Boolean>, View>, data1: Array<TextView>) {
        val (buttons, isClickable, menuButton) = data
        val (rotateOpen, rotateClose) = if (menuButton == fbMenu1Maps) animations1 else animations2
        val (fromBottom, toBottom) = if (menuButton == fbMenu1Maps) animationsDrop1 else animationsDrop2

        buttons.forEachIndexed { index, button ->
            isClickable[index] = !isClickable[index]

            if (!isClickable[index]) {
                button.visibility = View.VISIBLE
                data1[index].visibility = View.VISIBLE
                button.isClickable = true
                button.startAnimation(fromBottom)
                data1[index].startAnimation(fromBottom)
            } else {
                button.visibility = View.GONE
                data1[index].visibility = View.GONE
                button.isClickable = false
                button.startAnimation(toBottom)
                data1[index].startAnimation(toBottom)
            }
        }

        menuButton.startAnimation(if (!isClickable[0]) rotateOpen else rotateClose)
    }

    private fun setColorFButton() {
        fbMenu1Maps.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.colorPurple)
        fbCenterMaps.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.chart_blue4)
        fbActionMaps.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.green_basiccolor)

        fbMenu2Maps.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.colorRed_A400)
        fbPokokSembuh.backgroundTintList =
            ContextCompat.getColorStateList(
                this,
                if (modeAct == 4) R.color.grey_default else R.color.darkGreenColor
            )
        fbBlok.backgroundTintList =
            ContextCompat.getColorStateList(
                this,
                if (modeAct == 3) R.color.grey_default else R.color.blokAct
            )
        fbMultiple.backgroundTintList =
            ContextCompat.getColorStateList(
                this,
                if (modeAct == 2) R.color.grey_default else R.color.dashboard
            )
        fbSingle.backgroundTintList =
            ContextCompat.getColorStateList(
                this,
                if (modeAct == 1) R.color.grey_default else R.color.singleAct
            )
    }

    public override fun onResume() {
        super.onResume()
        updateLocationUI()
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        if (mapView != null) {
            mapView.onResume()
        }
        if (!mRequestingLocationUpdates!! && checkPermissions()) {
            startLocationUpdates()
        } else if (!checkPermissions()) {
            requestPermissions()
        }
        val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        sensorManager.registerListener(
            sensorListener,
            orientationSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )
    }

    public override fun onPause() {
        super.onPause()
        stopLocationUpdates()
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        if (mapView != null) {
            mapView.onPause()
        }
        sensorManager.unregisterListener(sensorListener)
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        if (mapView != null) {
            mapView.onPause()
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        AlertDialogUtility.withTwoActions(
            this,
            "Batal",
            "Ya",
            "Apakah anda yakin untuk keluar?",
            "warning.json"
        ) {
            stopLocationUpdates()
            if (mapView != null) {
                mapView.onPause()
            }

            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    public override fun onSaveInstanceState(savedInstanceState: Bundle) {
        savedInstanceState.putBoolean(
            KEY_REQUESTING_LOCATION_UPDATES,
            mRequestingLocationUpdates!!
        )
        savedInstanceState.putParcelable(KEY_LOCATION, mCurrentLocation)
        savedInstanceState.putString(KEY_LAST_UPDATED_TIME_STRING, mLastUpdateTime)
        super.onSaveInstanceState(savedInstanceState)
    }

    @Suppress("SameParameterValue")
    private fun showSnackbar(
        mainTextStringId: Int, actionStringId: Int,
        listener: View.OnClickListener
    ) {
        Snackbar.make(
            findViewById(android.R.id.content),
            mainTextStringId.toString(),
            Snackbar.LENGTH_INDEFINITE
        )
            .setAction(getString(actionStringId), listener).show()
    }

    private fun checkPermissions(): Boolean {
        val permissionState = ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (permissionState == PackageManager.PERMISSION_GRANTED) {
            startLocationUpdates()
        }
        return permissionState == PackageManager.PERMISSION_GRANTED
    }

    private fun requestPermissions() {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (shouldProvideRationale) {
            Log.i("currentPos", "Displaying permission rationale to provide additional context.")
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
            Snacky.builder()
                .setView(findViewById(R.id.mapsYellowTrees))
                .setText("Perizinan lokasi diperlukan")
                .centerText()
                .setDuration(Snacky.LENGTH_LONG)
                .error()
                .show()
        } else {
            Log.i("currentPos", "Requesting permission")
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                REQUEST_PERMISSIONS_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        Log.i("currentPos", "onRequestPermissionResult")
        if (requestCode == REQUEST_PERMISSIONS_REQUEST_CODE) {
            if (grantResults.isEmpty()) {
                Log.i("currentPos", "User interaction was cancelled.")
            } else if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                if (mRequestingLocationUpdates!!) {
                    Log.i(
                        "currentPos",
                        "Permission granted, updates requested, starting location updates"
                    )
                    startLocationUpdates()
                }
            } else {
                showSnackbar(
                    R.string.permission_denied_explanation,
                    R.string.settings, View.OnClickListener {
                        val intent = Intent()
                        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
                        val uri = Uri.fromParts(
                            "com.srs.deficiencytracker",
                            BuildConfig.APPLICATION_ID, null
                        )
                        intent.data = uri
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        startActivity(intent)
                    })
            }
        }
    }

    private fun updateValuesFromBundle(savedInstanceState: Bundle?) {
        if (savedInstanceState != null) {
            if (savedInstanceState.keySet().contains(KEY_REQUESTING_LOCATION_UPDATES)) {
                mRequestingLocationUpdates = savedInstanceState.getBoolean(
                    KEY_REQUESTING_LOCATION_UPDATES
                )
            }
            if (savedInstanceState.keySet().contains(KEY_LOCATION)) {
                mCurrentLocation = savedInstanceState.getParcelable(KEY_LOCATION)
            }
            if (savedInstanceState.keySet().contains(KEY_LAST_UPDATED_TIME_STRING)) {
                mLastUpdateTime = savedInstanceState.getString(KEY_LAST_UPDATED_TIME_STRING)
            }
            updateLocationUI()
        }
    }

    private fun createLocationRequest() {
        mLocationRequest = LocationRequest()
        mLocationRequest!!.interval =
            UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.fastestInterval =
            FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS
        mLocationRequest!!.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }

    private fun createLocationCallback() {
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                mCurrentLocation = locationResult.lastLocation
                mLastUpdateTime = DateFormat.getTimeInstance().format(Date())
                updateLocationUI()
            }
        }
    }

    private fun buildLocationSettingsRequest() {
        val builder = LocationSettingsRequest.Builder()
        builder.addLocationRequest(mLocationRequest!!)
        mLocationSettingsRequest = builder.build()
    }

    private fun updateLocationUI() {
        if (mCurrentLocation != null) {
            if (!firstGPS) {
                lat = try {
                    String.format(Locale.ENGLISH, "%s", mCurrentLocation!!.latitude).toFloat()
                } catch (e: Exception) {
                    null
                }
                lon = try {
                    String.format(Locale.ENGLISH, "%s", mCurrentLocation!!.longitude).toFloat()
                } catch (e: Exception) {
                    null
                }
                firstGPS = true
                positionMaps = true
            }

            if (positionMaps) {
                lat = try {
                    String.format(Locale.ENGLISH, "%s", mCurrentLocation!!.latitude).toFloat()
                } catch (e: Exception) {
                    0f
                }
                lon = try {
                    String.format(Locale.ENGLISH, "%s", mCurrentLocation!!.longitude).toFloat()
                } catch (e: Exception) {
                    0f
                }

                updateMarkerPosition(lat!!.toDouble(), lon!!.toDouble())
                if (latPk != null) {
                    createDashLineOverlay(lat!!.toDouble(), lon!!.toDouble(), latPk!!, lonPk!!)
                }
            }

            val accuracyInMeters = mCurrentLocation!!.accuracy
            fixAccuracy = ceil(accuracyInMeters).toInt()
            tvAccuracyMaps.text = "${fixAccuracy}m"
            if (fixAccuracy > minAccuracyGPS) {
                tvAccuracyMaps.setTextColor(Color.RED)
            } else {
                tvAccuracyMaps.setTextColor(
                    ColorStateList.valueOf(resources.getColor(R.color.colorGreen_A400))
                )
            }

            rangePos()
        }
    }

    private fun stopLocationUpdates() {
        if (!mRequestingLocationUpdates!!) {
            return
        }
        mFusedLocationClient!!.removeLocationUpdates(mLocationCallback)
            .addOnCompleteListener(this) {
                mRequestingLocationUpdates = false
            }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        mSettingsClient!!.checkLocationSettings(mLocationSettingsRequest)
            .addOnSuccessListener(this) {
                Log.i("currentPos", "All location settings are satisfied.")
                mFusedLocationClient!!.requestLocationUpdates(
                    mLocationRequest,
                    mLocationCallback, Looper.myLooper()
                )
                updateLocationUI()
            }
            .addOnFailureListener(this) { e ->
                when ((e as ApiException).statusCode) {
                    LocationSettingsStatusCodes.RESOLUTION_REQUIRED -> {
                        Log.i(
                            "currentPos",
                            "Location settings are not satisfied. Attempting to upgrade " +
                                    "location settings "
                        )
                        try {
                            val rae = e as ResolvableApiException
                            rae.startResolutionForResult(
                                this,
                                REQUEST_CHECK_SETTINGS
                            )
                        } catch (sie: IntentSender.SendIntentException) {
                            Log.i("currentPos", "PendingIntent unable to execute request.")
                        }
                    }

                    LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE -> {
                        val errorMessage = "Location settings are inadequate, and cannot be " +
                                "fixed here. Fix in Settings."
                        Log.e("currentPos", errorMessage)
                        Toasty.error(this, errorMessage, Toast.LENGTH_LONG).show()
                        mRequestingLocationUpdates = false
                    }
                }
                updateLocationUI()
            }
    }

    private fun createDashLineOverlay(
        newLat: Double,
        newLng: Double,
        latPk: Double,
        lonPk: Double
    ) {
        try {
            previousDashedLine?.let {
                mapView.overlays.remove(it)
            }

            val startPoint = GeoPoint(newLat, newLng)
            val endPoint = GeoPoint(latPk, lonPk)

            val points = listOf(startPoint, endPoint)
            val dashedLineOverlay = DashedLineOverlay(points, mapView)

            previousDashedLine = dashedLineOverlay

            mapView.overlays.add(dashedLineOverlay)
            mapView.invalidate()
        } catch (e: Exception) {
            Log.e("ET", "Error dash line: $e")
        }
    }

    private fun updateMarkerPosition(newLat: Double, newLng: Double) {
        try {
            currentMarker?.let {
                mapView.overlays.remove(it)
            }

            var originalDrawable =
                ContextCompat.getDrawable(this, R.drawable.baseline_navigation_24)
            val color = ContextCompat.getColor(this, R.color.blue1)
            originalDrawable?.let {
                DrawableCompat.setTint(it, color)
            }

            val widthInPixels = 100
            val heightInPixels = 100
            val bitmap = Bitmap.createBitmap(widthInPixels, heightInPixels, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            originalDrawable?.setBounds(0, 0, widthInPixels, heightInPixels)
            originalDrawable?.draw(canvas)
            originalDrawable = BitmapDrawable(resources, bitmap)

            val newMarker = Marker(mapView)
            newMarker.icon = originalDrawable
            newMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
            newMarker.position = GeoPoint(newLat, newLng)
            newMarker.rotation = 0f

            newMarker.setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
                override fun onMarkerClick(marker: Marker, mapView: MapView): Boolean {
                    return true
                }
            })

            mapView.overlays.add(newMarker)
            currentMarker = newMarker

            mapView.invalidate()
        } catch (e: Exception) {
            Log.e("ET", "Error mapview: $e")
        }
    }

    @SuppressLint("SetTextI18n")
    private fun rangePos() {
        if (latPk != null) {
            val rangeUserToPk = rangeTreeAndCurrentPos(
                GeoPoint(latPk!!, lonPk!!),
                GeoPoint(lat!!.toDouble(), lon!!.toDouble())
            )
            accuracyRange = rangeUserToPk.toInt()
            tvJarakMaps.text = if (rangeUserToPk >= 1000) {
                "%.1f".format((rangeUserToPk / 1000.0)) + "km"
            } else {
                "$accuracyRange" + "m"
            }
            if (accuracyRange > minRangeAct) {
                tvJarakMaps.setTextColor(Color.RED)
            } else {
                tvJarakMaps.setTextColor(
                    ColorStateList.valueOf(resources.getColor(R.color.colorGreen_A400))
                )
            }
        }
    }

    private fun rangeTreeAndCurrentPos(point1: GeoPoint, point2: GeoPoint): Double {
        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dlon = lon2 - lon1
        val dlat = lat2 - lat1

        val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return c * 6371000
    }

    @SuppressLint("Range")
    private fun getListPupuk() {
        val selectQuery =
            "SELECT  * FROM ${PemupukanSQL.db_tabPupuk} ORDER BY ${PemupukanSQL.db_namaPupuk} ASC"
        val db = PemupukanSQL(this).readableDatabase
        val i: Cursor?
        try {
            i = db.rawQuery(selectQuery, null)
            if (i.moveToFirst()) {
                do {
                    perlakuanArray.add(
                        try {
                            i.getString(i.getColumnIndex(PemupukanSQL.db_namaPupuk))
                        } catch (e: Exception) {
                            ""
                        }
                    )
                    perlakuanIdArray.add(
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

    private fun calculateCenter(geoPoints: List<GeoPoint>): GeoPoint? {
        if (geoPoints.isEmpty()) {
            return null
        }

        var latMax = 0.0
        var latMin = 0.0
        var lonMax = 0.0
        var lonMin = 0.0
        var first = true

        for (geoBlok in geoPoints) {
            if (first) {
                latMax = geoBlok.latitude
                latMin = geoBlok.latitude
                lonMax = geoBlok.longitude
                lonMin = geoBlok.longitude
                first = false
            } else {
                if (latMax < geoBlok.latitude) {
                    latMax = geoBlok.latitude
                } else if (latMin > geoBlok.latitude) {
                    latMin = geoBlok.latitude
                }

                if (lonMax < geoBlok.longitude) {
                    lonMax = geoBlok.longitude
                } else if (lonMin > geoBlok.longitude) {
                    lonMin = geoBlok.longitude
                }
            }
        }

        val latCen = (latMax + latMin) / 2
        val lonCen = (lonMax + lonMin) / 2

        return GeoPoint(latCen, lonCen)
    }

    companion object {
        const val REQUEST_PERMISSIONS_REQUEST_CODE = 34
        const val REQUEST_CHECK_SETTINGS = 0x1
        const val UPDATE_INTERVAL_IN_MILLISECONDS: Long = 10000
        const val FASTEST_UPDATE_INTERVAL_IN_MILLISECONDS =
            UPDATE_INTERVAL_IN_MILLISECONDS / 2
        const val KEY_REQUESTING_LOCATION_UPDATES = "requesting-location-updates"
        const val KEY_LOCATION = "location"
        const val KEY_LAST_UPDATED_TIME_STRING = "last-updated-time-string"
    }
}