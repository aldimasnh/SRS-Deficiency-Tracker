package com.srs.deficiencytracker.activity

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.View
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
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
import com.srs.deficiencytracker.utilities.FileMan
import com.srs.deficiencytracker.utilities.ModelMain
import com.srs.deficiencytracker.utilities.PrefManager
import com.srs.deficiencytracker.utilities.PrefManagerEstate
import com.srs.deficiencytracker.utilities.UpdateMan
import de.mateware.snacky.Snacky
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_form_est.cvNext
import kotlinx.android.synthetic.main.activity_form_est.header_maps
import kotlinx.android.synthetic.main.activity_form_est.llMapsAfd
import kotlinx.android.synthetic.main.activity_form_est.llMapsBlok
import kotlinx.android.synthetic.main.activity_form_est.llMapsEst
import kotlinx.android.synthetic.main.activity_form_est.rb_maps1
import kotlinx.android.synthetic.main.activity_form_est.rb_maps2
import kotlinx.android.synthetic.main.activity_form_est.rb_maps3
import kotlinx.android.synthetic.main.activity_form_est.sp_afd_form
import kotlinx.android.synthetic.main.activity_form_est.sp_blok_form
import kotlinx.android.synthetic.main.activity_form_est.sp_est_form
import kotlinx.android.synthetic.main.header_form.tv_update_header_gudang
import kotlinx.android.synthetic.main.header_form.tv_ver_app_header_gudang
import kotlinx.android.synthetic.main.header_form.view.icLocationHeader
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.text.DateFormat
import java.util.Date
import java.util.Locale

open class FormMapsActivity : AppCompatActivity() {

    private var est = ""
    private var afd = ""
    private var blok = ""
    private var blokPlot = ""
    private var luas = ""
    private var sph = 0

    val pilihEstate = "Pilih Estate"
    val pilihAfdeling = "Pilih Afdeling"
    val pilihBlok = "Pilih Blok"

    private var countYellowTrees = ArrayList<String>()
    private var estateArrayList = ArrayList<String>()
    private var estateArray = ArrayList<String>()
    private var afdelingArray = ArrayList<String>()
    private var blokArray = ArrayList<String>()
    private var luasArray = ArrayList<String>()
    private var ttArray = ArrayList<String>()
    private var sphArray = ArrayList<String>()

    /* [location] */
    private var mFusedLocationClient: FusedLocationProviderClient? = null
    private var mSettingsClient: SettingsClient? = null
    private var mLocationRequest: LocationRequest? = null
    private var mLocationSettingsRequest: LocationSettingsRequest? = null
    private var mLocationCallback: LocationCallback? = null
    private var mCurrentLocation: Location? = null
    private var mRequestingLocationUpdates: Boolean? = null
    private var mLastUpdateTime: String? = null
    private var lat: Float? = null
    private var lon: Float? = null
    private var firstGPS = false

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        UpdateMan().hideStatusNavigationBar(window)
        setContentView(R.layout.activity_form_est)

        val pm = PrefManagerEstate(this@FormMapsActivity)
        updateValuesFromBundle(savedInstanceState)
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        mSettingsClient = LocationServices.getSettingsClient(this)
        createLocationCallback()
        createLocationRequest()
        buildLocationSettingsRequest()
        mRequestingLocationUpdates = true

        sp_est_form.text = pilihEstate
        sp_afd_form.text = pilihAfdeling
        sp_blok_form.text = pilihBlok

        rb_maps1.isChecked = true
        llMapsAfd.visibility = View.GONE
        llMapsBlok.visibility = View.GONE

        rb_maps1.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                afd = ""
                blok = ""
                blokPlot = ""
                sp_afd_form.text = pilihAfdeling
                sp_blok_form.text = pilihBlok
                llMapsEst.visibility = View.VISIBLE
                llMapsAfd.visibility = View.GONE
                llMapsBlok.visibility = View.GONE
            }
        }

        rb_maps2.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                blok = ""
                blokPlot = ""
                sp_blok_form.text = pilihBlok
                llMapsEst.visibility = View.VISIBLE
                llMapsAfd.visibility = View.VISIBLE
                llMapsBlok.visibility = View.GONE
            }
        }

        rb_maps3.setOnCheckedChangeListener { compoundButton, b ->
            if (b) {
                llMapsEst.visibility = View.VISIBLE
                llMapsAfd.visibility = View.VISIBLE
                llMapsBlok.visibility = View.VISIBLE
            }
        }

        var urlCategory = PrefManager(this).dataReg!!
        val obj = JSONObject(
            FileMan().onlineInputStream(
                urlCategory,
                this,
                tv_update_header_gudang,
                tv_ver_app_header_gudang
            )
        )

        val dataBloKArray = obj.getJSONArray("Blok")
        val userArrayLabel = obj.getJSONArray("DataLabelJson")
        Log.d("testreg", userArrayLabel.toString())
        val beforeSplitTag = userArrayLabel.getJSONObject(0).getJSONArray("tag")
        val beforeSplitEst = userArrayLabel.getJSONObject(0).getJSONArray("est")

        var labelArray = ArrayList<String>()
        for (i in 0 until beforeSplitTag.length()) {
            labelArray.add(beforeSplitTag.getString(i))
        }

        var estArray = ArrayList<String>()
        for (i in 0 until beforeSplitEst.length()) {
            estArray.add(beforeSplitEst.getString(i))
        }

        var indexEst = 0
        var indexAFd = 0
        var indexBlok = 0
        var indexLuas = 0
        var indexTT = 0
        var indexSPH = 0

        for (i in labelArray.indices) {
            if ("estate" == labelArray[i]) {
                indexEst = i
            }
            if ("afd baru" == labelArray[i]) {
                indexAFd = i
            }
            if ("blok" == labelArray[i]) {
                indexBlok = i
            }
            if ("luas" == labelArray[i]) {
                indexLuas = i
            }
            if ("tt" == labelArray[i]) {
                indexTT = i
            }
            if ("sph" == labelArray[i]) {
                indexSPH = i
            }
        }

        for (i in 0 until dataBloKArray.length()) {
            val estString = try {
                estArray[dataBloKArray.getJSONObject(i).getString(indexEst.toString()).toInt()]
            } catch (e: Exception) {
                "null"
            }
            if (!estateArrayList.contains(estString) && !estString.contains("TKD") && !estString.contains(
                    "CWS"
                )
            ) {
                estateArrayList.add(estString)
            }
            estateArray.add(estString)
            afdelingArray.add(
                try {
                    dataBloKArray.getJSONObject(i).getString(indexAFd.toString())
                } catch (e: Exception) {
                    "null"
                }
            )
            blokArray.add(
                try {
                    dataBloKArray.getJSONObject(i).getString(indexBlok.toString())
                } catch (e: Exception) {
                    "null"
                }
            )
            ttArray.add(
                try {
                    dataBloKArray.getJSONObject(i).getString(indexTT.toString())
                } catch (e: Exception) {
                    "null"
                }
            )
            luasArray.add(
                try {
                    dataBloKArray.getJSONObject(i).getString(indexLuas.toString())
                } catch (e: Exception) {
                    "null"
                }
            )
            sphArray.add(
                try {
                    dataBloKArray.getJSONObject(i).getString(indexSPH.toString())
                } catch (e: Exception) {
                    "null"
                }
            )
        }
        sp_est_form.setItems(estateArrayList.sorted())
        sp_est_form.text = pilihEstate

        val blokPlotArrayList = ArrayList<String>()
        val blokArrayList = ArrayList<String>()
        val afdelingArrayList = ArrayList<String>()
        val luasArrayList = ArrayList<String>()
        val sphArrayList = ArrayList<String>()

        sp_est_form.setOnItemSelectedListener { view, position, id, item ->
            afdelingArrayList.clear()
            blokPlotArrayList.clear()
            blokArrayList.clear()
            luasArrayList.clear()
            sphArrayList.clear()

            est = item.toString()

            for (i in this.estateArray.indices) {
                if (!afdelingArrayList.contains(afdelingArray[i]) && item == estateArray[i]) {
                    afdelingArrayList.add(afdelingArray[i])
                }
            }
            if (afdelingArrayList.isEmpty()) {
                Toasty.info(this, "$item tidak memiliki afdeling inti!").show()
            } else {
                sp_afd_form.setItems(afdelingArrayList.sorted())
            }
            sp_afd_form.text = pilihAfdeling
            sp_blok_form.text = pilihBlok
        }


        sp_afd_form.setOnItemSelectedListener { view, position, id, item ->
            if (est.isEmpty()) est = pm.estate.toString()
            afd = item.toString()
            blokPlotArrayList.clear()
            blokArrayList.clear()
            luasArrayList.clear()
            sphArrayList.clear()
            for (i in blokArray.indices) {
                val namaBlok = "${blokArray[i]}${ttArray[i].substring(2, ttArray[i].lastIndex + 1)}"
                if (!blokArrayList.contains(blokArray[i]) && item == afdelingArray[i] && est == estateArray[i]) {
                    blokArrayList.add(blokArray[i])
                    blokPlotArrayList.add(namaBlok)
                    luasArrayList.add(luasArray[i])
                    sphArrayList.add(sphArray[i])
                }
            }

            val pkPath = this.getExternalFilesDir(null)?.absolutePath + "/MAIN/pk" + urlCategory
            val fileMaps = File(pkPath)
            if (fileMaps.exists()) {
                try {
                    val readMaps = fileMaps.readText()
                    val objMaps = JSONObject(readMaps)
                    val estObjMaps = objMaps.getJSONObject(est)
                    val afdObjMaps = estObjMaps.getJSONObject(afd)
                    for (indexAfd in afdObjMaps.keys()) {
                        if (!blokArrayList.contains(indexAfd)) {
                            blokArrayList.add(indexAfd)
                            blokPlotArrayList.add(indexAfd)
                        }
                    }
                } catch (e: Exception) {
                    Log.d("ET", "Error: $e")
                    e.printStackTrace()
                }
            }

            blokArrayList.sort()
            blokPlotArrayList.sort()
            sp_blok_form.setItems(blokArrayList)
            sp_blok_form.text = pilihBlok
        }

        sp_blok_form.setOnItemSelectedListener { view, position, id, item ->
            blok = item.toString()
            blokPlot = try {
                blokPlotArrayList[position]
            } catch (e: Exception) {
                ""
            }
            luas = try {
                luasArrayList[position].toFloat().toString()
            } catch (e: Exception) {
                Log.e("ET", "Error: $e")
                "0"
            }
            sph = try {
                sphArrayList[position].toInt()
            } catch (e: Exception) {
                Log.e("ET", "Error: $e")
                0
            }
            pm.luas = luas
            pm.sph = sph.toString().toInt()
        }

        if (!pm.estate.isNullOrEmpty()) {
            afdelingArrayList.clear()
            blokPlotArrayList.clear()
            blokArrayList.clear()
            sphArrayList.clear()
            luasArrayList.clear()

            for (i in this.estateArray.indices) {
                if (!afdelingArrayList.contains(afdelingArray[i]) && pm.estate == estateArray[i]) {
                    afdelingArrayList.add(afdelingArray[i])
                }
            }

            if (afdelingArrayList.isEmpty()) {
                Toasty.info(this, "$est tidak memiliki afdeling inti!").show()
            } else {
                sp_afd_form.setItems(afdelingArrayList.sorted())
                sp_afd_form.text = pilihAfdeling
            }

            sp_est_form.text = pm.estate
            est = pm.estate!!
            rb_maps1.isChecked = true

            if (!pm.afdeling.isNullOrEmpty()) {
                sp_afd_form.text = pm.afdeling
                afd = pm.afdeling!!
                rb_maps2.isChecked = true

                for (i in blokArray.indices) {
                    val namaBlok = "${blokArray[i]}${ttArray[i].substring(2, ttArray[i].lastIndex + 1)}"
                    if (!blokArrayList.contains(blokArray[i]) && pm.afdeling == afdelingArray[i] && pm.estate == estateArray[i]) {
                        blokArrayList.add(blokArray[i])
                        blokPlotArrayList.add(namaBlok)
                        luasArrayList.add(luasArray[i])
                        sphArrayList.add(sphArray[i])
                    }
                }

                val pkPath = this.getExternalFilesDir(null)?.absolutePath + "/MAIN/pk" + urlCategory
                val fileMaps = File(pkPath)
                if (fileMaps.exists()) {
                    try {
                        val readMaps = fileMaps.readText()
                        val objMaps = JSONObject(readMaps)
                        val estObjMaps = objMaps.getJSONObject(est)
                        val afdObjMaps = estObjMaps.getJSONObject(afd)
                        for (indexAfd in afdObjMaps.keys()) {
                            if (!blokArrayList.contains(indexAfd)) {
                                blokArrayList.add(indexAfd)
                                blokPlotArrayList.add(indexAfd)
                            }
                        }
                    } catch (e: Exception) {
                        Log.d("ET", "Error: $e")
                        e.printStackTrace()
                    }
                }

                blokArrayList.sort()
                blokPlotArrayList.sort()
                sp_blok_form.setItems(blokArrayList)
                sp_blok_form.text = pilihBlok

                if (!pm.blok.isNullOrEmpty()) {
                    sp_blok_form.text = pm.blok
                    blok = pm.blok!!
                    blokPlot = pm.blokPlot!!
                    rb_maps3.isChecked = true
                }
            }

            sph = pm.sph
            luas = pm.luas!!
        }

        cvNext.setOnClickListener {
            try {
                countYellowTrees.clear()

                val pkPath =
                    this.getExternalFilesDir(null)?.absolutePath + "/MAIN/pk" + PrefManager(this).dataReg!!
                val fileMaps = File(pkPath)
                if (fileMaps.exists()) {
                    try {
                        val readMaps = fileMaps.readText()
                        val objMaps = JSONObject(readMaps)

                        val estObjMaps = objMaps.getJSONObject(est)
                        if (afd.isEmpty()) {
                            for (indexEst in estObjMaps.keys()) {
                                var itemEst = estObjMaps.getJSONObject(indexEst)
                                for (indexAfd in itemEst.keys()) {
                                    var itemAfd = itemEst.getJSONObject(indexAfd)
                                    for (index in itemAfd.keys()) {
                                        countYellowTrees.add(index)
                                    }
                                }
                            }
                        } else {
                            val afdObjMaps = estObjMaps.getJSONObject(afd)
                            if (blok.isEmpty()) {
                                for (indexAfd in afdObjMaps.keys()) {
                                    var itemAfd = afdObjMaps.getJSONObject(indexAfd)
                                    for (index in itemAfd.keys()) {
                                        countYellowTrees.add(index)
                                    }
                                }
                            } else {
                                val blokObjMaps = afdObjMaps.getJSONObject(blok)
                                for (index in blokObjMaps.keys()) {
                                    countYellowTrees.add(index)
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
            } catch (e: IOException) {
                Log.d("ET", "Error: $e")
            }

            if (rb_maps1.isChecked && est.isEmpty()) {
                Toasty.warning(this, "Silahkan isi data terlebih dahulu!", Toasty.LENGTH_LONG)
                    .show()
            } else if (rb_maps2.isChecked && (est.isEmpty() || afd.isEmpty())) {
                Toasty.warning(this, "Silahkan isi data terlebih dahulu!", Toasty.LENGTH_LONG)
                    .show()
            } else if (rb_maps3.isChecked && (est.isEmpty() || afd.isEmpty() || blok.isEmpty())) {
                Toasty.warning(this, "Silahkan isi data terlebih dahulu!", Toasty.LENGTH_LONG)
                    .show()
            } else {
                if (countYellowTrees.isEmpty()) {
                    Toasty.warning(this, "Tidak ada data pokok kuning!", Toasty.LENGTH_LONG).show()
                } else {
                    if (firstGPS) {
                        stopLocationUpdates()

                        pm.estate = est
                        pm.afdeling = afd
                        pm.blok = blok
                        pm.blokPlot = blokPlot

                        Toasty.info(this, "Mohon tunggu, sedang memproses peta..", Toasty.LENGTH_LONG).show()
                        cvNext.visibility = View.GONE

                        val intent =
                            Intent(this, MapsActivity::class.java).putExtra("est", est)
                                .putExtra("afd", afd)
                                .putExtra("blok", blok)
                                .putExtra("blokPlot", blokPlot)
                                .putExtra("pos", "$lat$$lon")
                        startActivity(intent)
                        finishAffinity()
                    } else {
                        Toasty.warning(this, "Titik GPS belum didapatkan!", Toasty.LENGTH_LONG)
                            .show()
                    }
                }
            }
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
            stopLocationUpdates()
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finishAffinity()
        }
    }

    public override fun onResume() {
        super.onResume()
        if (!mRequestingLocationUpdates!! && checkPermissions()) {
            startLocationUpdates()
        } else if (!checkPermissions()) {
            requestPermissions()
        }
        updateLocationUI()
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
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
            }

            val icLocation: ImageView = header_maps.icLocationHeader as ImageView
            icLocation.setImageResource(R.drawable.ic_location_on_black_24dp)
            icLocation.imageTintList =
                ColorStateList.valueOf(resources.getColor(R.color.colorGreen_A400))
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