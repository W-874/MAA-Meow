package com.aliothmoon.maameow.presentation.view.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.graphics.PathParser
import kotlin.math.min

@Composable
fun PallasMedal(
    debugActive: Boolean,
    modifier: Modifier = Modifier,
    medalSize: Dp = 40.dp,
) {
    val shape = MaterialTheme.shapes.medium
    val normalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val medalColor = if (debugActive) MaterialTheme.colorScheme.primary else normalColor
    val path = remember {
        runCatching {
            PathParser.createPathFromPathData(HANGOVER_PATH_DATA).asComposePath()
        }.getOrElse { Path() }
    }

    Surface(
        shape = shape,
        color = if (debugActive) {
            MaterialTheme.colorScheme.primaryContainer
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        modifier = modifier
            .size(medalSize)
            .border(1.dp, medalColor, shape),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(6.dp)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val bounds = path.getBounds()
                if (bounds.width <= 0f || bounds.height <= 0f) return@Canvas
                val canvasW = this.size.width
                val canvasH = this.size.height
                val scale = min(canvasW / bounds.width, canvasH / bounds.height)
                val dx = (canvasW - bounds.width * scale) / 2f - bounds.left * scale
                val dy = (canvasH - bounds.height * scale) / 2f - bounds.top * scale
                withTransform({
                    translate(dx, dy)
                    scale(scale, scale, pivot = Offset.Zero)
                }) {
                    drawPath(path = path, color = medalColor)
                }
            }
        }
    }
}
