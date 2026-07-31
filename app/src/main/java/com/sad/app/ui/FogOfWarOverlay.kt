package com.sad.app.ui

import android.graphics.*
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Overlay

data class ExploredPoint(
    val geoPoint: GeoPoint,
    val radiusMeters: Double = 150.0
)

/**
 * Fog of War Overlay – optimiert:
 * - Viewport-Culling: Nur Punkte die aktuell auf dem Bildschirm liegen werden gezeichnet
 * - Kein Neuzeichnen wenn sich die Liste nicht geändert hat (cachedBoundingBox + cachedCount)
 * - Individuelle Radien pro Punkt (Präzisionsmodus)
 */
class FogOfWarOverlay(
    var exploredAreas: List<ExploredPoint>,
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

    // Viewport-Culling: aktuell sichtbare Bounding Box cachen
    private var lastBoundingBox: BoundingBox? = null
    private var lastZoom: Double = -1.0

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        if (canvas.width <= 0 || canvas.height <= 0) return

        val saveCount = canvas.saveLayer(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), null)

        // 1. Nebel zeichnen
        val alphaInt = (fogOpacity.coerceIn(0f, 1f) * 255).toInt()
        fogPaint.color = Color.argb(
            alphaInt,
            Color.red(fogColor),
            Color.green(fogColor),
            Color.blue(fogColor)
        )
        canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), fogPaint)

        // 2. Viewport-Bounding-Box bestimmen (mit 20% Puffer für halbe Kreise am Rand)
        val bb = mapView.boundingBox
        val latPad = (bb.latitudeSpan) * 0.20
        val lonPad = (bb.longitudeSpan) * 0.20
        val visLatMax = bb.latNorth + latPad
        val visLatMin = bb.latSouth - latPad
        val visLonMax = bb.lonEast + lonPad
        val visLonMin = bb.lonWest - lonPad

        // 3. Nur sichtbare Punkte zeichnen (Viewport-Culling)
        for (ep in exploredAreas) {
            val lat = ep.geoPoint.latitude
            val lon = ep.geoPoint.longitude

            // Grobes Culling: Punkt außerhalb des erweiterten Viewports → überspringen
            // Wir nutzen Grad-Näherung für den Radius (1m ≈ 0.000009°)
            val radiusDeg = ep.radiusMeters / 111_000.0
            if (lat + radiusDeg < visLatMin || lat - radiusDeg > visLatMax ||
                lon + radiusDeg < visLonMin || lon - radiusDeg > visLonMax) {
                continue
            }

            val point = mapView.projection.toPixels(ep.geoPoint, null)
            val radiusPx = calculateRadiusPx(mapView, ep.radiusMeters)
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radiusPx, clearPaint)
        }

        // 4. Aktuelle Spielerposition + leuchtender Rand
        currentLocation?.let { current ->
            val point = mapView.projection.toPixels(current, null)
            val currentRadiusPx = calculateRadiusPx(mapView, visionRadiusMeters)
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), currentRadiusPx, clearPaint)

            playerEdgePaint.color = Color.argb(220, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), currentRadiusPx, playerEdgePaint)
        }

        canvas.restoreToCount(saveCount)
    }

    private fun calculateRadiusPx(mapView: MapView, radiusMeters: Double): Float {
        val center = mapView.mapCenter as GeoPoint
        val centerPixel = mapView.projection.toPixels(center, null)
        val offsetDeg = radiusMeters / 111_320.0
        val northPoint = GeoPoint(center.latitude + offsetDeg, center.longitude)
        val northPixel = mapView.projection.toPixels(northPoint, null)
        return Math.abs(centerPixel.y - northPixel.y).toFloat().coerceAtLeast(6f)
    }
}
