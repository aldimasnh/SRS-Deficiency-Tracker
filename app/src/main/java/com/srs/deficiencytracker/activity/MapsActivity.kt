package com.srs.deficiencytracker.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.BitmapDrawable
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.preference.PreferenceManager
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.DrawableCompat
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
import com.google.android.material.snackbar.Snackbar
import com.srs.deficiencytracker.BuildConfig
import com.srs.deficiencytracker.MainActivity
import com.srs.deficiencytracker.R
import com.srs.deficiencytracker.utilities.AlertDialogUtility
import com.srs.deficiencytracker.utilities.ModelMain
import com.srs.deficiencytracker.utilities.PrefManager
import de.mateware.snacky.Snacky
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_maps.cvCenterMaps
import kotlinx.android.synthetic.main.activity_maps.cvFUMaps
import kotlinx.android.synthetic.main.activity_maps.mapView
import kotlinx.android.synthetic.main.activity_maps.tvAccuracyMaps
import kotlinx.android.synthetic.main.activity_maps.tvBlokMaps
import kotlinx.android.synthetic.main.activity_maps.tvJarakMaps
import kotlinx.android.synthetic.main.activity_maps.tvJmlPkMaps
import kotlinx.android.synthetic.main.activity_maps.tvKondisiMaps
import kotlinx.android.synthetic.main.activity_maps.tvPokokMaps
import kotlinx.android.synthetic.main.activity_maps.tvStatusMaps
import kotlinx.android.synthetic.main.activity_maps.tvTglFotoMaps
import org.json.JSONObject
import org.osmdroid.api.IGeoPoint
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
import java.util.Date
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

open class MapsActivity : AppCompatActivity() {
    var modelMainList: MutableList<ModelMain> = ArrayList()
    private var lastClickedMarker: Marker? = null
    private lateinit var sensorManager: SensorManager

    private var getEst = ""
    private var getAfd = ""
    private var getBlok = ""
    private var getBlokPlot = ""
    private var urlCategory = ""
    private var fixBlok = ""

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
    private var testMarker: Marker? = null

    private var positionMaps = false
    private var firstGPS = false
    private var fixAccuracy = 0
    private var accuracyRange = 0
    private var latPk: Double? = null
    private var lonPk: Double? = null

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        updateValuesFromBundle(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)
        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()
        mRequestingLocationUpdates = true

        getEst = getDataIntent("est")
        getAfd = getDataIntent("afd")
        getBlok = getDataIntent("blok")
        getBlokPlot = getDataIntent("blokPlot")
        urlCategory = PrefManager(this).dataReg!!

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
            if (getBlokPlot.length == 5) {
                val sliced = getBlokPlot.substring(0, getBlokPlot.length - 2)
                fixBlok = sliced.replaceRange(1, 1, "0")
            } else if (getEst == "KTE" || getEst == "MKE" || getEst == "PKE" || getEst == "BSE" || getEst == "BWE" || getEst == "GDE") {
                if (getBlokPlot.length == 6 && getBlokPlot[0] == 'H') {
                    val sliced = getBlokPlot.substring(0, getBlokPlot.length - 2)
                    fixBlok = sliced[0] + sliced.substring(2)
                } else if (getBlokPlot.length == 6) {
                    fixBlok = getBlokPlot.substring(0, getBlokPlot.length - 3)
                }
            } else if (getEst == "BDE") {
                fixBlok = getBlokPlot.substring(0, getBlokPlot.length - 3)
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

        val latlnValues = ArrayList<IGeoPoint>()
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
                    if (fixBlok.isEmpty()) {
                        val indicesAfd = estObjMaps.getJSONObject(getAfd).keys()
                        for (index in indicesAfd) {
                            val blokObjMaps = estObjMaps.getJSONObject(getAfd).getJSONObject(index)
                            val splLatLn = blokObjMaps.getString("latln").split("$").toTypedArray()
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
                    } else {
                        val blokObjMaps = estObjMaps.getJSONObject(getAfd).getJSONObject(fixBlok)
                        val splLatLn = blokObjMaps.getString("latln").split("$").toTypedArray()
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
            } catch (e: Exception) {
                Log.d("ET", "Error: $e")
                e.printStackTrace()
            }
        } else {
            Log.d("ET", "The JSON file maps does not exist.")
        }

        var avgLat = 0.0
        var avgLon = 0.0

        for (geoPoint in latlnValues) {
            avgLat += geoPoint.latitude
            avgLon += geoPoint.longitude
        }

        avgLat /= latlnValues.size
        avgLon /= latlnValues.size

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        val geoPoint =
            if (firstGPS) GeoPoint(lat!!.toDouble(), lon!!.toDouble()) else GeoPoint(avgLat, avgLon)
        mapView.setMultiTouchControls(true)
        mapView.controller.animateTo(geoPoint)
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)
        mapView.setBuiltInZoomControls(true)
        mapView.controller.setZoom(15.0)

        // Initialize sensor manager and sensors
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager

        for (coordinateList in coordinates) {
            val polygon = Polygon()
            polygon.points = coordinateList
            polygon.fillPaint.color = 0x1523CB1F // Fill color (semi-transparent green)
            polygon.strokeColor = 0xFF000000.toInt() // Stroke color (black)
            polygon.strokeWidth = 2f
            mapView.overlays.add(polygon)
        }
        mapView.invalidate()

        cvCenterMaps.setOnClickListener {
            if (firstGPS) {
                val geoPoint = GeoPoint(lat!!.toDouble(), lon!!.toDouble())
                mapView.controller.animateTo(geoPoint)
            } else {
                Toasty.warning(
                    this@MapsActivity,
                    "Titik GPS belum didapatkan!",
                    Toasty.LENGTH_LONG
                )
                    .show()
            }
        }

        getLocYellowTrees()
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
            val arrBelum = ArrayList<Int>()
            val arrSudah = ArrayList<Int>()
            val markerIds = ArrayList<Int>()

            for (j in modelList.indices) {
                if (modelList[j].statusPk == "Sudah") {
                    arrSudah.add(j)
                } else {
                    arrBelum.add(j)
                }
            }

            for (i in modelList.indices) {
                var selectedIcon = ContextCompat.getDrawable(this, R.drawable.baseline_close_circle_24)
                var drawable = ContextCompat.getDrawable(
                    this,
                    if (modelList[i].statusPk == "Sudah") R.drawable.baseline_circle_24 else R.drawable.ic_close
                )
                val color = ContextCompat.getColor(
                    this,
                    if (modelList[i].statusPk == "Sudah") {
                        R.color.green1
                    } else if (modelList[i].kondisiPk == "Pucat") {
                        R.color.yellow1
                    } else if (modelList[i].kondisiPk == "Ringan") {
                        R.color.dashboard
                    } else {
                        R.color.colorRed_A400
                    }
                )
                drawable?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
                selectedIcon?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

                val widthInPixels = if (modelList[i].statusPk == "Sudah") 50 else 100
                val heightInPixels = if (modelList[i].statusPk == "Sudah") 50 else 100
                val bitmap = Bitmap.createBitmap(widthInPixels, heightInPixels, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                selectedIcon?.setBounds(0, 0, widthInPixels, heightInPixels)
                selectedIcon?.draw(canvas)
                selectedIcon = BitmapDrawable(resources, bitmap)

                // Resize icon status sudah ditangani
                if (modelList[i].statusPk == "Sudah") {
                    drawable?.setBounds(0, 0, widthInPixels, heightInPixels)
                    drawable?.draw(canvas)
                    drawable = BitmapDrawable(resources, bitmap)
                }

                val marker = Marker(mapView)
                marker.icon = drawable
                marker.position = GeoPoint(modelList[i].latPk, modelList[i].lonPk)
                marker.title = modelList[i].idPk.toString()
                marker.snippet = "$getEst - ${modelList[i].afdPk}"

                val formattedDate = SimpleDateFormat("MMM yyyy", Locale("id", "ID"))
                tvTglFotoMaps.text = formattedDate.format(
                    SimpleDateFormat(
                        "yyyy-MM-dd HH:mm:ss",
                        Locale.ENGLISH
                    ).parse(modelList[i].tglPk)!!
                )
                tvJmlPkMaps.text = "${arrSudah.size}/${arrBelum.size}"
                tvBlokMaps.text = if (getBlok.isNotEmpty()) getBlok else "-"

                marker.setOnMarkerClickListener(object : Marker.OnMarkerClickListener {
                    override fun onMarkerClick(marker: Marker, mapView: MapView): Boolean {
                        if (lastClickedMarker != null) {
                            lastClickedMarker?.closeInfoWindow()
                        }

                        val markerId = modelList[i].idPk
                        if (modelList[i].statusPk == "Belum") {
                            if (markerIds.size < 20 || markerIds.contains(markerId)) {
                                if (markerIds.contains(markerId)) {
                                    markerIds.remove(markerId)
                                    marker.icon = drawable
                                } else {
                                    if (markerIds.size == 20) {
                                        markerIds.remove(markerId)
                                        marker.icon = drawable
                                    }
                                    markerIds.add(markerId)
                                    marker.icon = selectedIcon
                                }
                                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER)
                            } else {
                                Toasty.warning(this@MapsActivity, "Maksimal hanya dapat menangani 20 titik!").show()
                            }
                        }

                        Log.d("cekData", markerIds.toTypedArray().contentToString())

                        lastClickedMarker = marker
                        tvPokokMaps.text = "${markerIds.size}/20"
                        tvBlokMaps.text = modelList[i].blokPk
                        tvKondisiMaps.text = modelList[i].kondisiPk
                        tvStatusMaps.text = modelList[i].statusPk + " ditangani"

                        latPk = modelList[i].latPk
                        lonPk = modelList[i].lonPk
                        rangePos()

                        marker.showInfoWindow()

                        cvFUMaps.visibility = if (modelList[i].statusPk == "Belum") View.VISIBLE else View.GONE
                        cvFUMaps.setOnClickListener {
                            if (firstGPS) {
                                if (fixAccuracy > 10 || accuracyRange > 25) {
                                    AlertDialogUtility.alertDialog(
                                        this@MapsActivity,
                                        "GPS belum memenuhi syarat!",
                                        "warning.json"
                                    )
                                } else {
                                    val intent =
                                        Intent(
                                            this@MapsActivity,
                                            HandlingFormActivity::class.java
                                        )
                                            .putExtra("id", modelList[i].idPk.toString())
                                            .putExtra("est", getEst)
                                            .putExtra("afd", modelList[i].afdPk)
                                            .putExtra("blok", modelList[i].blokPk)
                                            .putExtra("kondisi", modelList[i].kondisiPk)
                                            .putExtra("gps", "GA")
                                    startActivity(intent)
                                    finishAffinity()
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

                        return true
                    }
                })

                mapView.overlays.add(marker)
                mapView.invalidate()
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
        sensorManager.registerListener(sensorListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL)
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
                            "co.id.ssms.mobilepro",
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
            }

            val accuracyInMeters = mCurrentLocation!!.accuracy
            fixAccuracy = ceil(accuracyInMeters).toInt()
            tvAccuracyMaps.text = "${fixAccuracy}m"
            if (fixAccuracy > 10) {
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

    private fun updateMarkerPosition(newLat: Double, newLng: Double) {
        try {
            currentMarker?.let {
                mapView.overlays.remove(it)
            }

            var originalDrawable = ContextCompat.getDrawable(this, R.drawable.baseline_navigation_24)
            val color = ContextCompat.getColor(this, R.color.colorPrimaryDark)
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
        }catch (e: Exception) {
            Toasty.error(this, "Error mapview:$e", Toasty.LENGTH_SHORT).show()
        }
    }

    private fun rangePos() {
        if (latPk != null) {
            val (rangeKm, rangeM) = rangeTreeAndCurrentPos(GeoPoint(latPk!!, lonPk!!),
                GeoPoint(lat!!.toDouble(), lon!!.toDouble()))
            accuracyRange = rangeM
            tvJarakMaps.text = rangeKm
            if (accuracyRange > 25) {
                tvJarakMaps.setTextColor(Color.RED)
            } else {
                tvJarakMaps.setTextColor(
                    ColorStateList.valueOf(resources.getColor(R.color.colorGreen_A400))
                )
            }
        }
    }

    private fun rangeTreeAndCurrentPos(point1: GeoPoint, point2: GeoPoint): Pair<String, Int> {
        val R = 6371 // Earth radius in kilometers

        val lat1 = Math.toRadians(point1.latitude)
        val lon1 = Math.toRadians(point1.longitude)
        val lat2 = Math.toRadians(point2.latitude)
        val lon2 = Math.toRadians(point2.longitude)

        val dlon = lon2 - lon1
        val dlat = lat2 - lat1

        val a = sin(dlat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dlon / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        val distanceInKilometers = R * c
        val distanceInMeters = distanceInKilometers * 1000

        val formattedDistanceKilometers = if (distanceInKilometers >= 1.0) {
            "${String.format("%.1f", distanceInKilometers)}km"
        } else {
            "${String.format("%.0f", distanceInKilometers * 1000)}m"
        }

        return formattedDistanceKilometers to distanceInMeters.toInt()
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