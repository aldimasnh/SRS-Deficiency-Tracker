package com.srs.deficiencytracker.utilities

import android.annotation.SuppressLint
import android.content.Intent
import android.util.Log
import android.view.View
import androidx.core.content.ContextCompat
import com.srs.deficiencytracker.R
import com.srs.deficiencytracker.activity.HandlingFormActivity
import kotlinx.android.synthetic.main.layout_tooltip.view.imageClose
import kotlinx.android.synthetic.main.layout_tooltip.view.imageLocation
import kotlinx.android.synthetic.main.layout_tooltip.view.mbFU
import kotlinx.android.synthetic.main.layout_tooltip.view.rlInfo
import kotlinx.android.synthetic.main.layout_tooltip.view.tvBlok
import kotlinx.android.synthetic.main.layout_tooltip.view.tvKondisi
import kotlinx.android.synthetic.main.layout_tooltip.view.tvStatus
import kotlinx.android.synthetic.main.layout_tooltip.view.tvTgl
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.infowindow.InfoWindow

class CustomInfoWindow(mapView: MapView?) : InfoWindow(R.layout.layout_tooltip, mapView) {

    override fun onClose() {
        //by default, do nothing
    }

    @SuppressLint("SetTextI18n")
    override fun onOpen(item: Any) {
        val marker = item as Marker
        val infoWindowData = marker.relatedObject as ModelMain

        val rlInfo = mView.rlInfo
        val imageLocation = mView.imageLocation
        val tvBlok = mView.tvBlok
        val tvKondisi = mView.tvKondisi
        val tvStatus = mView.tvStatus
        val tvTgl = mView.tvTgl
        val mbFU = mView.mbFU
        val imageClose = mView.imageClose

        if (infoWindowData.statusPk == "Belum") {
            rlInfo.backgroundTintList =
                ContextCompat.getColorStateList(mapView.context, R.color.chart_red4)
            imageLocation.imageTintList =
                ContextCompat.getColorStateList(mapView.context, R.color.chart_red4)
            imageClose.imageTintList =
                ContextCompat.getColorStateList(mapView.context, R.color.chart_red4)
            mbFU.backgroundTintList =
                ContextCompat.getColorStateList(mapView.context, R.color.chart_red4)
        }

        tvBlok.text = infoWindowData.blokPk
        tvKondisi.text = "Kondisi : " + infoWindowData.kondisiPk
        tvStatus.text = "Status Penanganan : " + infoWindowData.statusPk
        tvTgl.text = "Update : " + infoWindowData.tglPk

        imageClose.setOnClickListener {
            marker.closeInfoWindow()
        }

        mbFU.visibility = if (infoWindowData.statusPk == "Belum") View.VISIBLE else View.GONE
        mbFU.setOnClickListener {
            val intent = Intent(mapView?.context, HandlingFormActivity::class.java)
                .putExtra("id", infoWindowData.idPk.toString())
                .putExtra("est", infoWindowData.estPk)
                .putExtra("afd", infoWindowData.afdPk)
            mapView?.context?.startActivity(intent)
        }
    }
}