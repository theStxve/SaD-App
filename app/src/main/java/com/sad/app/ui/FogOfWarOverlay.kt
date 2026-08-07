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
 * Fog of War Overlay – stark optimiert:
 * - Zoom-LOD: Bei niedrigem Zoom werden Punkte gruppiert / übersprungen
 * - Viewport-Culling: Nur Punkte die aktuell auf dem Bildschirm liegen werden gezeichnet
 * - Kein teures saveLayer bei sehr niedrigem Zoom (nur Rechteck-Nebel)
 * - Reduzierter Sampling-Faktor bei niedrigem Zoom
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
        isAntiAlias = false  // Bei clipping nicht nötig, spart CPU
    }

    private val playerEdgePaint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        isAntiAlias = true
        maskFilter = BlurMaskFilter(10f, BlurMaskFilter.Blur.OUTER)
    }

    // Cached radius Pixel-Berechnung (nur invalidieren wenn sich Zoom ändert)
    private var lastZoom: Double = -1.0
    private var cachedPixelsPerMeter: Float = 1f

    override fun draw(canvas: Canvas, mapView: MapView, shadow: Boolean) {
        if (shadow) return
        if (canvas.width <= 0 || canvas.height <= 0) return

        val zoom = mapView.zoomLevelDouble

        // === LOD: Sehr niedriger Zoom (< 11) ===
        // Bei diesem Zoom sind Kreise sowieso kaum sichtbar -> einfach nur Nebelrechteck ohne Layer
        if (zoom < 11.0) {
            val alphaInt = (fogOpacity.coerceIn(0f, 1f) * 255 * 0.6f).toInt()
            fogPaint.color = Color.argb(
                alphaInt,
                Color.red(fogColor),
                Color.green(fogColor),
                Color.blue(fogColor)
            )
            canvas.drawRect(0f, 0f, canvas.width.toFloat(), canvas.height.toFloat(), fogPaint)
            return
        }

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

        // LOD-Parameter: Sampling-Rate und effektiver Radius je nach Zoom
        // zoom >= 15: alle Punkte, originaler Radius
        // zoom 13-15: jeden 2. Punkt, etwas größerer Radius
        // zoom 11-13: jeden 4. Punkt, deutlich größerer Radius (bessere Abdeckung bei weniger Punkten)
        val (sampleStep, radiusMultiplier) = when {
            zoom >= 15.0 -> Pair(1, 1.0)
            zoom >= 13.0 -> Pair(2, 1.6)
            else         -> Pair(4, 2.5)  // zoom 11-13
        }

        // Pixel-pro-Meter cachen (nur bei Zoom-Änderung neu berechnen)
        if (kotlin.math.abs(zoom - lastZoom) > 0.05) {
            lastZoom = zoom
            val center = mapView.mapCenter as GeoPoint
            val centerPixel = mapView.projection.toPixels(center, null)
            val offsetDeg = 100.0 / 111_320.0
            val northPoint = GeoPoint(center.latitude + offsetDeg, center.longitude)
            val northPixel = mapView.projection.toPixels(northPoint, null)
            val pxFor100m = Math.abs(centerPixel.y - northPixel.y).toFloat().coerceAtLeast(1f)
            cachedPixelsPerMeter = pxFor100m / 100f
        }

        // 3. Nur sichtbare Punkte zeichnen (Viewport-Culling + LOD-Sampling)
        val areas = exploredAreas
        var i = 0
        val size = areas.size
        while (i < size) {
            val ep = areas[i]
            i += sampleStep

            val lat = ep.geoPoint.latitude
            val lon = ep.geoPoint.longitude

            // Effektiver Radius mit LOD-Multiplikator
            val effectiveRadius = ep.radiusMeters * radiusMultiplier
            val radiusDeg = effectiveRadius / 111_000.0

            // Viewport-Culling
            if (lat + radiusDeg < visLatMin || lat - radiusDeg > visLatMax ||
                lon + radiusDeg < visLonMin || lon - radiusDeg > visLonMax) {
                continue
            }

            val point = mapView.projection.toPixels(ep.geoPoint, null)
            val radiusPx = (ep.radiusMeters * radiusMultiplier * cachedPixelsPerMeter).toFloat().coerceAtLeast(6f)
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), radiusPx, clearPaint)
        }

        // 4. Aktuelle Spielerposition + leuchtender Rand
        currentLocation?.let { current ->
            val point = mapView.projection.toPixels(current, null)
            val currentRadiusPx = (visionRadiusMeters * cachedPixelsPerMeter).toFloat().coerceAtLeast(6f)
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), currentRadiusPx, clearPaint)

            playerEdgePaint.color = Color.argb(220, Color.red(themeColor), Color.green(themeColor), Color.blue(themeColor))
            canvas.drawCircle(point.x.toFloat(), point.y.toFloat(), currentRadiusPx, playerEdgePaint)
        }

        canvas.restoreToCount(saveCount)
    }
}
