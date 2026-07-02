package com.solupos.contenedor.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlin.math.max

data class TutorialStep(
    val targetBounds: Rect,
    val title: String,
    val description: String
)

/**
 * Overlay tipo "spotlight": oscurece la pantalla y perfora un círculo
 * transparente alrededor de targetBounds usando BlendMode.Clear sobre una
 * capa offscreen (si no se aísla en su propia capa, el Clear borraría todo
 * lo que hay debajo de la ventana, no solo el scrim de este Canvas).
 */
@Composable
fun TutorialOverlay(
    steps: List<TutorialStep>,
    onFinish: () -> Unit
) {
    if (steps.isEmpty()) {
        LaunchedEffect(Unit) { onFinish() }
        return
    }

    var stepIndex by remember { mutableStateOf(0) }
    val step = steps[stepIndex]
    val density = LocalDensity.current

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenHeightPx = with(density) { maxHeight.toPx() }
        val isLowerHalf = step.targetBounds.center.y > screenHeightPx / 2f

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer(alpha = 0.99f, compositingStrategy = CompositingStrategy.Offscreen)
        ) {
            drawRect(color = Color.Black.copy(alpha = 0.78f))
            val radius = (max(step.targetBounds.width, step.targetBounds.height) / 2f) +
                with(density) { 20.dp.toPx() }
            drawCircle(
                color = Color.Transparent,
                radius = radius,
                center = step.targetBounds.center,
                blendMode = BlendMode.Clear
            )
        }

        Card(
            modifier = Modifier
                .align(if (isLowerHalf) Alignment.TopCenter else Alignment.BottomCenter)
                .padding(24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(step.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(step.description, style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    TextButton(onClick = onFinish) { Text("Omitir") }
                    Button(onClick = {
                        if (stepIndex < steps.lastIndex) stepIndex++ else onFinish()
                    }) {
                        Text(if (stepIndex < steps.lastIndex) "Siguiente" else "Entendido")
                    }
                }
            }
        }
    }
}
