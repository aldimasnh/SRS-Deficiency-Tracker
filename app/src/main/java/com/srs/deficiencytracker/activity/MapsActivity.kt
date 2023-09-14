package com.srs.deficiencytracker.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.srs.deficiencytracker.MainActivity
import com.srs.deficiencytracker.R
import com.srs.deficiencytracker.utilities.AlertDialogUtility
import com.srs.deficiencytracker.utilities.CustomInfoWindow
import com.srs.deficiencytracker.utilities.ModelMain
import com.srs.deficiencytracker.utilities.PrefManager
import kotlinx.android.synthetic.main.activity_maps.mapView
import org.json.JSONObject
import org.osmdroid.api.IGeoPoint
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.CustomZoomButtonsController
import org.osmdroid.views.MapController
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.OverlayItem
import org.osmdroid.views.overlay.Polygon
import java.io.File
import java.io.IOException

open class MapsActivity : AppCompatActivity() {
    var modelMainList: MutableList<ModelMain> = ArrayList()
    lateinit var mapController: MapController
    lateinit var overlayItem: ArrayList<OverlayItem>

    private var getEst = ""
    private var getAfd = ""
    private var urlCategory = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        getEst = getDataIntent("est")
        getAfd = getDataIntent("afd")
        urlCategory = PrefManager(this).dataReg!!

        val latlnValues = ArrayList<IGeoPoint>()
        val mapsPath = this.getExternalFilesDir(null)?.absolutePath + "/MAIN/maps" + urlCategory
        val fileMaps = File(mapsPath)
        if (fileMaps.exists()) {
            try {
                val readMaps = fileMaps.readText()
                val objMaps = JSONObject(readMaps)

                val estObjMaps = objMaps.getJSONObject(getEst)
                val afdObjMaps = estObjMaps.getJSONArray(getAfd)
                for (i in 0 until afdObjMaps.length()) {
                    val item = afdObjMaps.getJSONObject(i)
                    val latln = item.optString("latln", null)
                    if (latln != null) {
                        val splLatLn = latln.split("$").toTypedArray()
                        for (j in splLatLn.indices) {
                            if (splLatLn[j].isNotEmpty()) {
                                val fixLatLn = splLatLn[j].replace(" ", "").split(",").toTypedArray()
                                latlnValues.add(GeoPoint(fixLatLn[0].trim().toDouble(), fixLatLn[1].trim().toDouble()))
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

        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))

        val geoPoint = GeoPoint(-2.6068906, 111.6871446)
        mapView.setMultiTouchControls(true)
        mapView.controller.animateTo(geoPoint)
        mapView.setTileSource(TileSourceFactory.DEFAULT_TILE_SOURCE)
        mapView.zoomController.setVisibility(CustomZoomButtonsController.Visibility.NEVER)

        mapController = mapView.controller as MapController
        mapController.setCenter(geoPoint)
        mapController.zoomTo(15)

        // Create a polygon with the points
        val polygon = Polygon()
        polygon.points = latlnValues as MutableList<GeoPoint>
        polygon.fillPaint.color = 0x1523CB1F // Fill color (semi-transparent green)
        polygon.strokeColor = 0xFF000000.toInt() // Stroke color (black)
        polygon.strokeWidth = 2f

        mapView.overlayManager.add(polygon)

        getLocYellowTrees()
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
                    val afdObjMaps = estObjMaps.getJSONArray(getAfd)
                    for (i in 0 until afdObjMaps.length()) {
                        val item = afdObjMaps.getJSONObject(i)
                        val modelMain = ModelMain()
                        modelMain.idPk = item.getInt("id")
                        modelMain.blokPk = item.getString("blok")
                        modelMain.kondisiPk = item.getString("kondisi")
                        modelMain.statusPk = item.getString("status")
                        modelMain.tglPk = item.getString("tgl_foto_udara")
                        val fixLatLn = item.getString("latln").replace(" ", "").split(",").toTypedArray()
                        modelMain.latPk = fixLatLn[0].trim().toDouble()
                        modelMain.lonPk = fixLatLn[1].trim().toDouble()
                        modelMainList.add(modelMain)
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

    private fun initMarker(modelList: List<ModelMain>) {
        for (i in modelList.indices) {
            overlayItem = ArrayList()
            overlayItem.add(
                OverlayItem(
                    modelList[i].blokPk, modelList[i].statusPk, GeoPoint(
                        modelList[i].latPk, modelList[i].lonPk
                    )
                )
            )
            val info = ModelMain()
            info.estPk = getEst
            info.afdPk = getAfd
            info.idPk = modelList[i].idPk
            info.blokPk = modelList[i].blokPk
            info.kondisiPk = modelList[i].kondisiPk
            info.statusPk = modelList[i].statusPk
            info.tglPk = modelList[i].tglPk

//            Log.d("cekData", modelList[i].idPk.toString() + ": " + modelList[i].statusPk)
            val drawable = ContextCompat.getDrawable(this, R.drawable.ic_palm_tree_24)
            val color = ContextCompat.getColor(this, if (modelList[i].statusPk == "Sudah") R.color.chart_blue4 else R.color.chart_red4)
            drawable?.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)

            val marker = Marker(mapView)
            marker.icon = drawable
            marker.position = GeoPoint(modelList[i].latPk, modelList[i].lonPk)
            marker.relatedObject = info
            marker.infoWindow = CustomInfoWindow(mapView)
            marker.setOnMarkerClickListener { item, arg1 ->
                item.showInfoWindow()
                true
            }

            mapView.overlays.add(marker)
            mapView.invalidate()
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
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        if (mapView != null) {
            mapView.onResume()
        }
    }

    public override fun onPause() {
        super.onPause()
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this))
        if (mapView != null) {
            mapView.onPause()
        }
    }

    override fun onBackPressed() {
        AlertDialogUtility.withTwoActions(
            this,
            "Batal",
            "Ya",
            "Apakah anda yakin untuk keluar?",
            "warning.json"
        ) {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }
}