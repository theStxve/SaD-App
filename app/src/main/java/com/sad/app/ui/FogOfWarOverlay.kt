package com.sad.app.ui

import android.graphics.*
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

/**
 * Fog of War wie in Strategiespielen:
 * - Alles beginnt schwarz/verdeckt
 * - Wo du hinläufst, wird permanent aufgedeckt
 * - Der Radius bleibt geografisch konstant beim Zoomen
 */
class FogOfWarOverlay(
    var exploredAreas: List<GeoPoint>,
    var currentLocation: GeoPoint?,
    private val visionRadiusMeters: Double = 150.0
) : Overlay() {

    private val fogPaint = Paint().apply {
        color = Color.argb(220, 8, 8, 16)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val clearPaint = Paint().apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR)
        isAntiAlias = true
    }

    private val edgePaint = Paint().apply {
        color = Color.argb(200, 0, 243, 255)
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
        maskFilter = BlurMaskFilter(14f, BlurMaskFilter.Blur.OUTER)
    }

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        if (canvas.width <= 0 || canvas.height <= 0) return

        // Pixel-Radius aus geografischen Metern berechnen:
        val radiusPx = calculateRadiusPx(mapView)

        // Besser: saveLayer nutzen, anstatt 60x pro Sekunde eine riesige Bitmap zu erzeugen!
        val saveCount = canvas.saveLayer(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), null)

        // 1. Gesamten Layer mit Nebel füllen (die ganze Karte ist verdeckt)
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), fogPaint)

        // 2. Für jeden erkundeten Punkt: Loch in den Nebel schneiden
        for (geoPoint in exploredAreas) {
            val point = mapView.projection.toPixels(geoPoint, null)
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radiusPx, clearPaint)
        }

        // 3. Neon-Rand und aktives Nebel-Loch immer exakt um die AKTUELLE Position
        // So wirkt das Aufdecken zu 100% flüssig, selbst wenn die Datenbank noch nicht gespeichert hat
        currentLocation?.let { current ->
            val point = mapView.projection.toPixels(current, null)
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radiusPx, clearPaint) // <- Das fehlte!
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radiusPx, edgePaint)
        }

        // 4. Nebel-Layer mit dem darunterliegenden Canvas mergen
        canvas.restoreToCount(saveCount)
    }

    private fun calculateRadiusPx(mapView: MapView): Float {
        // Referenzpunkt in der Mitte der Karte
        val center = mapView.mapCenter as GeoPoint
        val centerPixel = mapView.projection.toPixels(center, null)

        // Punkt ~visionRadiusMeters Meter nördlich davon
        // 1 Grad Breitengrad ≈ 111320 Meter
        val offsetDeg = visionRadiusMeters / 111320.0
        val northPoint = GeoPoint(center.latitude + offsetDeg, center.longitude)
        val northPixel = mapView.projection.toPixels(northPoint, null)

        // Pixelabstand = unser Radius
        return Math.abs(centerPixel.y - northPixel.y).toFloat().coerceAtLeast(20f)
    }
}
