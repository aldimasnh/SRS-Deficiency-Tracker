package com.srs.deficiencytracker.utilities

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

class TextPolygonOverlay(
    private val mapViews: MapView,
    private val center: GeoPoint,
    private val text: String
) : Overlay() {

    override fun draw(c: Canvas, mapView: MapView, shadow: Boolean) {
        if (!shadow) {
            val paint = Paint()
            paint.color = Color.BLACK
            paint.textSize = 25f
            paint.isAntiAlias = true
            paint.textAlign = Paint.Align.CENTER
            paint.isFakeBoldText = true

            val textBounds = Rect()
            paint.getTextBounds(text, 0, text.length, textBounds)

            val screenCoords = mapViews.projection.toPixels(center, null)
            val textX = screenCoords.x.toFloat()
            val textY = screenCoords.y.toFloat() + textBounds.height() / 2

            c.drawText(text, textX, textY, paint)
        }
    }
}