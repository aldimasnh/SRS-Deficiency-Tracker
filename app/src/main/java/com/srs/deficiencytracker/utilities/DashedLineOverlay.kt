package com.srs.deficiencytracker.utilities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PathEffect
import org.osmdroid.api.IGeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class DashedLineOverlay(private val points: List<IGeoPoint>, private val mapView: MapView) : Overlay() {

    private val paint = Paint()
    private val path = Path()

    init {
        paint.color = Color.BLACK // Line color (you can change this)
        paint.strokeWidth = 3f // Line width (you can change this)
        paint.style = Paint.Style.STROKE
        val dashEffect: PathEffect = DashPathEffect(floatArrayOf(10f, 10f), 0f)
        paint.pathEffect = dashEffect
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (!shadow) {
            path.reset()
            var isFirst = true
            for (geoPoint in points) {
                val screenPoint = mapView?.projection?.toPixels(geoPoint, null)
                if (isFirst) {
                    path.moveTo(screenPoint?.x?.toFloat() ?: 0f, screenPoint?.y?.toFloat() ?: 0f)
                    isFirst = false
                } else {
                    path.lineTo(screenPoint?.x?.toFloat() ?: 0f, screenPoint?.y?.toFloat() ?: 0f)
                }
            }
            canvas?.drawPath(path, paint)
        }
        super.draw(canvas, mapView, shadow)
    }
}