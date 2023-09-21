package com.srs.deficiencytracker.utilities

import android.content.Context
import android.graphics.Canvas
import android.graphics.Point
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker

class RotateMarker(context: Context, mapView: MapView) : Marker(mapView) {

    private var rotationAngle = 0f
    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (event.sensor.type == Sensor.TYPE_ORIENTATION) {
                rotationAngle = event.values[0]
                mapView.invalidate()
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

        }
    }

    init {
        val orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION)
        sensorManager.registerListener(sensorListener, orientationSensor, SensorManager.SENSOR_DELAY_NORMAL)
    }

    override fun draw(canvas: Canvas?, mapView: MapView?, shadow: Boolean) {
        if (!shadow) {
            val point = Point()
            mapView?.projection?.toPixels(this.mPosition, point)

            canvas?.save()
            canvas?.rotate(rotationAngle, point.x.toFloat(), point.y.toFloat())
            super.draw(canvas, mapView, shadow)
            canvas?.restore()
        }
    }

    fun getRotationAngle(): Float {
        return rotationAngle
    }
}