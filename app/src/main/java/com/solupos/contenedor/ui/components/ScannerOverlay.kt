package com.solupos.contenedor.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ScannerOverlay() {
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val scanLine by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "line"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        val boxSize = size.width * 0.7f
        val left = (size.width - boxSize) / 2f
        val top = (size.height - boxSize) / 2f
        val cornerLen = 48.dp.toPx()
        val stroke = 3.dp.toPx()
        val overlay = Color.Black.copy(alpha = 0.55f)

        // Oscurecer las 4 zonas fuera del recuadro
        drawRect(overlay, topLeft = Offset.Zero, size = Size(size.width, top))
        drawRect(overlay, topLeft = Offset(0f, top + boxSize), size = Size(size.width, size.height - top - boxSize))
        drawRect(overlay, topLeft = Offset(0f, top), size = Size(left, boxSize))
        drawRect(overlay, topLeft = Offset(left + boxSize, top), size = Size(size.width - left - boxSize, boxSize))

        // Esquinas blancas
        fun corner(x: Float, y: Float, dx: Float, dy: Float) {
            drawLine(Color.White, Offset(x, y), Offset(x + dx, y), stroke)
            drawLine(Color.White, Offset(x, y), Offset(x, y + dy), stroke)
        }
        corner(left, top, cornerLen, cornerLen)
        corner(left + boxSize, top, -cornerLen, cornerLen)
        corner(left, top + boxSize, cornerLen, -cornerLen)
        corner(left + boxSize, top + boxSize, -cornerLen, -cornerLen)

        // Línea animada
        val lineY = top + boxSize * scanLine
        drawLine(Color(0xFF1565C0), Offset(left + 8f, lineY), Offset(left + boxSize - 8f, lineY), 2.dp.toPx())
    }
}
