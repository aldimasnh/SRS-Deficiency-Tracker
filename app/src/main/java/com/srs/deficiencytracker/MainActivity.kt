package com.srs.deficiencytracker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.karumi.dexter.Dexter
import com.karumi.dexter.MultiplePermissionsReport
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.multi.MultiplePermissionsListener
import com.srs.deficiencytracker.activity.FormMapsActivity
import com.srs.deficiencytracker.activity.MapsActivity
import com.srs.deficiencytracker.activity.WebViewActivity
import com.srs.deficiencytracker.database.PemupukanSQL
import com.srs.deficiencytracker.utilities.AlertDialogUtility
import com.srs.deficiencytracker.utilities.Login
import com.srs.deficiencytracker.utilities.PrefManager
import es.dmoral.toasty.Toasty
import kotlinx.android.synthetic.main.activity_main.dashboardAndIdk
import kotlinx.android.synthetic.main.activity_main.mapsAndPkKuning
import kotlinx.android.synthetic.main.activity_main.profile_main
import kotlinx.android.synthetic.main.activity_main.treeAndBlok
import kotlinx.android.synthetic.main.header.tv_tanggal
import kotlinx.android.synthetic.main.header.tv_ver_header
import kotlinx.android.synthetic.main.header.view.tvKeluar
import kotlinx.android.synthetic.main.header.view.tvUser
import kotlinx.android.synthetic.main.icon_grid.view.cv_grid2
import kotlinx.android.synthetic.main.icon_grid.view.leftIconDescription
import kotlinx.android.synthetic.main.icon_grid.view.leftIconSrc
import kotlinx.android.synthetic.main.icon_grid.view.leftIconTap
import kotlinx.android.synthetic.main.icon_grid.view.rightIconDescription
import kotlinx.android.synthetic.main.icon_grid.view.rightIconSrc
import kotlinx.android.synthetic.main.icon_grid.view.rightIconTap
import java.io.File

class MainActivity : AppCompatActivity() {
    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        tv_ver_header.text = "App ver: ${BuildConfig.VERSION_NAME}"
        tv_tanggal.text = "Up: ${PrefManager(this).lastUpdate}"

        mapsAndPkKuning.leftIconSrc.setImageResource(R.drawable.baseline_map_24)
        mapsAndPkKuning.rightIconSrc.setImageResource(R.drawable.ic_cloud_upload_black_24dp)
        mapsAndPkKuning.rightIconSrc.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.grey_default)

        treeAndBlok.leftIconSrc.setImageResource(R.drawable.ic_palm_tree_24)
        treeAndBlok.rightIconSrc.setImageResource(R.drawable.baseline_update_24)
        treeAndBlok.leftIconSrc.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.grey_default)
        treeAndBlok.rightIconSrc.backgroundTintList =
            ContextCompat.getColorStateList(this, R.color.grey_default)

        dashboardAndIdk.leftIconSrc.setImageResource(R.drawable.baseline_dashboard_24)

        mapsAndPkKuning.leftIconDescription.text = "CEK PETA"
        mapsAndPkKuning.rightIconDescription.text = "UNGGAH DATA"
        treeAndBlok.leftIconDescription.text = "SINKRONISASI POKOK KUNING"
        treeAndBlok.rightIconDescription.text = "SINKRONISASI PETA BLOK"
        dashboardAndIdk.leftIconDescription.text = "DASHBOARD"

        dashboardAndIdk.cv_grid2.visibility = View.GONE

        mapsAndPkKuning.leftIconTap.setOnClickListener {
            val intent = Intent(this, FormMapsActivity::class.java)
            startActivity(intent)
        }
        mapsAndPkKuning.rightIconTap.setOnClickListener {

        }
        treeAndBlok.leftIconTap.setOnClickListener {

        }
        treeAndBlok.rightIconTap.setOnClickListener {

        }
        dashboardAndIdk.leftIconTap.setOnClickListener {
            val intent = Intent(this, WebViewActivity::class.java)
            startActivity(intent)
        }

        profile_main.tvUser.text =
            "${PrefManager(this).name}" //setting nama dari shared preferences
        profile_main.tvKeluar.setOnClickListener {
            AlertDialogUtility.withTwoActions(
                this,
                "Batal",
                "Logout",
                "Apakah anda yakin untuk logout dari apikasi MobilePro?",
                "warning.json"
            ) {
                finishAffinity() //buat keluar
                val intent = Intent(this, Login::class.java)

                val prefManager =
                    PrefManager(this)

                //clear json
                val dataReg = prefManager.dataReg
                val filesToDelete = listOf("MAIN/${dataReg}", "MAIN/maps${dataReg}", "MAIN/pk${dataReg}")
                for (filePath in filesToDelete) {
                    val file = File(this.getExternalFilesDir(null)!!.absolutePath + "/" + filePath)
                    if (file.exists()) {
                        file.delete()
                    }
                }

                //clear prefmanager
                prefManager.session = false
                prefManager.isFirstTimeLaunch = true
                prefManager.name = null
                prefManager.departemen = null
                prefManager.jabatan = null
                prefManager.nohp = null
                prefManager.email = null
                prefManager.userid = null
                prefManager.lokasi = null
                prefManager.lokasi_kerja = null
                prefManager.akses = null
                prefManager.afdeling = null
                prefManager.password = null
                prefManager.reg = null
                prefManager.dataReg = null
                prefManager.version = 0
                prefManager.versionQC = 0

                //clear database
                PemupukanSQL(this).deleteDb()

                startActivity(intent)
            }
        }

        checkGeneralPermissions(this, this)
    }

    override fun onBackPressed() {
        AlertDialogUtility.withTwoActions(
            this,
            "Batal",
            "Keluar",
            "Apakah anda yakin untuk keluar dari apikasi MobilePro?",
            "warning.json"
        ) {
            finishAffinity()
        }
    }

    fun checkGeneralPermissions(context: Context, activity: Activity) {
        val shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(
            activity,
            Manifest.permission.CAMERA
        )
        Dexter.withContext(context)
            .withPermissions(
                Manifest.permission.MANAGE_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.WRITE_SECURE_SETTINGS,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.CAMERA,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.INTERNET,
                Manifest.permission.READ_PHONE_STATE
            ).withListener(object : MultiplePermissionsListener {
                override fun onPermissionsChecked(report: MultiplePermissionsReport) {
                }

                override fun onPermissionRationaleShouldBeShown(
                    permissions: List<PermissionRequest>,
                    token: PermissionToken
                ) {
                    if (shouldProvideRationale) {
                        //TO DO
                    }
                }
            }).check()

    }
}