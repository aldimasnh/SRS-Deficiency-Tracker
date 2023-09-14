package com.srs.deficiencytracker.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.leinardi.android.speeddial.SpeedDialActionItem
import com.leinardi.android.speeddial.SpeedDialView
import com.srs.deficiencytracker.MainActivity
import com.srs.deficiencytracker.R
import com.srs.deficiencytracker.utilities.AlertDialogUtility
import kotlinx.android.synthetic.main.activity_handling_form.header_hand
import kotlinx.android.synthetic.main.header_form.view.icLocationHeader
import kotlinx.android.synthetic.main.header_form.view.ic_gudang_wh
import kotlinx.android.synthetic.main.header_form.view.tv_gudang_main
import java.text.SimpleDateFormat
import java.util.Calendar

open class HandlingFormActivity : AppCompatActivity() {

    private var getId = ""
    private var getEst = ""
    private var getAfd = ""

    @SuppressLint("SetTextI18n")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_handling_form)

        getId = getDataIntent("id")
        getEst = getDataIntent("est")
        getAfd = getDataIntent("afd")

        Log.d("cekData", getId)
        Log.d("cekData", getEst)
        Log.d("cekData", getAfd)

        header_hand.icLocationHeader.visibility = View.GONE
        header_hand.tv_gudang_main.text = "PENANGANAN"
        header_hand.ic_gudang_wh.setImageResource(R.drawable.baseline_content_paste_24)

        setSpeedDial()
    }

    private fun createSD(string: String, id: Int, drawable: Int, color: Int) {
        val sdTaksasi: SpeedDialView = findViewById(R.id.sdHandSaveUpload)
        sdTaksasi.addActionItem(
            SpeedDialActionItem.Builder(id, drawable)
                .setLabel(string)
                .setFabSize(FloatingActionButton.SIZE_NORMAL)
                .setFabBackgroundColor(resources.getColor(color))
                .setFabImageTintColor(resources.getColor(R.color.white))
                .create()
        )
    }

    private fun setSpeedDial() {
        val sdTaksasi: SpeedDialView = findViewById(R.id.sdHandSaveUpload)

        createSD(
            "TAMBAH FOTO",
            R.id.openCamera,
            R.drawable.ic_baseline_add_a_photo_24,
            R.color.chart_blue4
        )
        createSD(
            "SIMPAN DATA",
            R.id.saveHandling,
            R.drawable.ic_save_black_24dp,
            R.color.green_basiccolor
        )

        sdTaksasi.setOnActionSelectedListener(SpeedDialView.OnActionSelectedListener { actionItem ->
            when (actionItem.id) {
                R.id.openCamera -> {
                    Log.d("cekData", "open camera ok")
                    sdTaksasi.close()
                    return@OnActionSelectedListener true
                }
                R.id.saveHandling -> {
                    Log.d("cekData", "save ok")
                    sdTaksasi.close()
                    return@OnActionSelectedListener true
                }
            }
            false
        })
    }

    private fun getDataIntent(data: String? = null): String {
        return try {
            intent.getStringExtra(data)!!
        } catch (e: Exception) {
            ""
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
            val intent = Intent(this, MapsActivity::class.java).putExtra("est", getEst).putExtra("afd", getAfd)
            startActivity(intent)
            finishAffinity()
        }
    }
}