package com.sad.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/**
 * Stufenloser Zoom-Slider auf der rechten Seite – wie bei Snapchat.
 * Ziehen nach oben = mehr Zoom, nach unten = weniger Zoom.
 *
 * @param zoomLevel aktueller Zoom (0.0 bis 1.0 normalisiert)
 * @param onZoomChange callback wenn der Nutzer zieht
 */
@Composable
fun ZoomSlider(
    zoomLevel: Float, // 0f = weit raus, 1f = ganz rein
    onZoomChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val cyan = Color(0xFF00F3FF)
    val cyanDim = cyan.copy(alpha = 0.25f)
    val trackColor = Color(0xFF111122)

    var trackHeightPx by remember { mutableStateOf(1f) }
    val trackHeightDp = with(LocalDensity.current) { trackHeightPx.toDp() }

    Box(
        modifier = modifier
            .width(28.dp)
            .fillMaxHeight(0.45f)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor.copy(alpha = 0.85f))
            .onSizeChanged { trackHeightPx = it.height.toFloat() }
            .pointerInput(Unit) {
                detectVerticalDragGestures { _, dragAmount ->
                    // Drag nach oben (negativ) = zoom rein
                    val delta = -dragAmount / trackHeightPx
                    val newZoom = (zoomLevel + delta).coerceIn(0f, 1f)
                    onZoomChange(newZoom)
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Track: gefüllter Balken (unten nach thumb)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 10.dp)
        ) {
            // Hintergrund-Track
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxHeight()
                    .width(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(cyanDim)
            )

            // Gefüllter Teil (unten bis zur aktuellen Zoom-Position)
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxHeight(zoomLevel)
                    .width(2.dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(cyan, cyan.copy(alpha = 0.4f)),
                            startY = 0f,
                            endY = Float.POSITIVE_INFINITY
                        )
                    )
            )

            // Thumb – der Kreis der sich bewegt
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    // Position: oben wenn zoom=1, unten wenn zoom=0
                    .offset(y = (trackHeightDp - 24.dp) * (1f - zoomLevel))
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(Color.White, cyan)
                        )
                    )
            )
        }
    }
}
