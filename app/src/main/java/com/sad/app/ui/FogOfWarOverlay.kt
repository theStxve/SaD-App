package com.sad.app.ui

import android.graphics.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Fog of War Overlay:
 * - Verdeckt unerkundete Gebiete mit einem Nebelschleier
 * - Erkundete Punkte werden sauber freigeschnitten
 * - Spieler-Standort erhält einen leuchtenden Neon-Rand
 */
class FogOfWarOverlay(
    var exploredAreas: List<GeoPoint>,
    var currentLocation: GeoPoint?,
    var themeColor: Int = Color.parseColor("#00F3FF"),
    var fogOpacity: Float = 0.85f,
    var visionRadiusMeters: Double = 150.0,
    var fogColor: Int = Color.parseColor("#0A0A14")
) : Overlay() {

    private val fogPaint = Paint().apply {
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    private val playerEdgePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
        maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.OUTER)
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        if (canvas.width <= 0 || canvas.height <= 0) return

        val radiusPx = calculateRadiusPx(mapView)

        val saveCount = canvas.saveLayer(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), null)

        // 1. Nebel mit konfigurierbarer Opazität über gesamte Karte legen
        val alphaInt = (fogOpacity.coerceIn(0f, 1f) * 255).toInt()
        fogPaint.color = Color.argb(
            alphaInt,
            Color.red(fogColor),
            Color.green(fogColor),
            Color.blue(fogColor)
        )
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), fogPaint)

        // 2. Für jeden erkundeten Punkt ein Loch freischneiden
        for (geoPoint in exploredAreas) {
            val point = mapView.projection.toPixels(geoPoint, null)
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radiusPx, clearPaint)
        }

        // 3. Aktuelles Nebel-Loch & leuchtender Rand um den Spieler
        currentLocation?.let { current ->
            val point = mapView.projection.toPixels(current, null)
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radiusPx, clearPaint)

            playerEdgePaint.color = Color.argb(220, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radiusPx, playerEdgePaint)
        }

        canvas.restoreToCount(saveCount)
    }

    private fun calculateRadiusPx(mapView: MapView): Float {
        val center = mapView.mapCenter as GeoPoint
        val centerPixel = mapView.projection.toPixels(center, null)

        val offsetDeg = visionRadiusMeters / 111320.0
        val northPoint = GeoPoint(center.latitude + offsetDeg, center.longitude)
        val northPixel = mapView.projection.toPixels(northPoint, null)

        return Math.abs(centerPixel.y - northPixel.y).toFloat().coerceAtLeast(10f)
    }
}
