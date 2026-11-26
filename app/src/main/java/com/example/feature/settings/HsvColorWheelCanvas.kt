package com.example.feature.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.HsvColor
import com.example.domain.service.ColorHarmonyEngine
import com.example.ui.theme.KalendarzReinmarTheme
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.sin

@Composable
fun HsvColorWheelCanvas(
    swatches: List<HsvColor>,
    selectedSwatchIndex: Int,
    onSwatchDragged: (index: Int, hue: Float, saturation: Float) -> Unit,
    onGestureFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestSwatches by rememberUpdatedState(swatches)
    val latestSelectedIndex by rememberUpdatedState(selectedSwatchIndex)
    val latestOnDragged by rememberUpdatedState(onSwatchDragged)
    val latestOnFinished by rememberUpdatedState(onGestureFinished)

    val wheelDescription = stringResource(R.string.settings_wheel_cd)
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f)
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 296.dp)
                .aspectRatio(1f)
                .semantics { contentDescription = wheelDescription }
                .testTag("hsv_color_wheel_canvas")
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val paddingPx = 22.dp.toPx()
                        val wheelRadius = (min(size.width, size.height) / 2f - paddingPx)
                            .coerceAtLeast(1f)
                        val hitRadiusPx = 36.dp.toPx()

                        val targetIndex = findClosestHandleIndex(
                            touch = down.position,
                            center = center,
                            wheelRadius = wheelRadius,
                            swatches = latestSwatches,
                            hitRadiusPx = hitRadiusPx,
                        ) ?: latestSelectedIndex.coerceIn(0, (latestSwatches.size - 1).coerceAtLeast(0))

                        val initialPoint = offsetToHueSaturation(down.position, center, wheelRadius)
                        down.consume()
                        latestOnDragged(targetIndex, initialPoint.first, initialPoint.second)

                        drag(down.id) { change ->
                            change.consume()
                            val point = offsetToHueSaturation(change.position, center, wheelRadius)
                            latestOnDragged(targetIndex, point.first, point.second)
                        }
                        latestOnFinished()
                    }
                },
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val paddingPx = 22.dp.toPx()
            val wheelRadius = (min(size.width, size.height) / 2f - paddingPx).coerceAtLeast(1f)

            drawHsvSpectrumDisc(
                center = center,
                radius = wheelRadius,
                outlineColor = outlineColor,
            )

            val baseIndex = ColorHarmonyEngine.baseSwatchIndex(swatches.size)

            // Draw spokes from center to every handle
            swatches.forEachIndexed { index, swatch ->
                val handlePos = hueSaturationToOffset(swatch, center, wheelRadius)
                val isBase = index == baseIndex
                val isSelected = index == selectedSwatchIndex
                val strokeWidth = when {
                    isSelected -> 2.8.dp.toPx()
                    isBase -> 2.2.dp.toPx()
                    else -> 1.5.dp.toPx()
                }
                val spokeAlpha = when {
                    isSelected -> 0.92f
                    isBase -> 0.80f
                    else -> 0.58f
                }
                drawLine(
                    color = Color.Black.copy(alpha = 0.28f),
                    start = center,
                    end = handlePos,
                    strokeWidth = strokeWidth + 1.5.dp.toPx(),
                )
                drawLine(
                    color = Color.White.copy(alpha = spokeAlpha),
                    start = center,
                    end = handlePos,
                    strokeWidth = strokeWidth,
                )
            }

            // Center hub anchor
            drawCircle(
                color = Color.Black.copy(alpha = 0.25f),
                radius = 5.5.dp.toPx(),
                center = center,
            )
            drawCircle(
                color = surfaceColor,
                radius = 4.dp.toPx(),
                center = center,
            )

            // Draw non-selected handles first, then selected handle on top
            val drawOrder = swatches.indices.sortedBy { index ->
                when (index) {
                    selectedSwatchIndex -> 2
                    baseIndex -> 1
                    else -> 0
                }
            }

            drawOrder.forEach { index ->
                val swatch = swatches[index]
                val pos = hueSaturationToOffset(swatch, center, wheelRadius)
                drawSwatchHandle(
                    position = pos,
                    swatch = swatch,
                    isBase = index == baseIndex,
                    isSelected = index == selectedSwatchIndex,
                )
            }
        }
    }
}

private fun DrawScope.drawHsvSpectrumDisc(
    center: Offset,
    radius: Float,
    outlineColor: Color,
) {
    // Soft outer halo
    drawCircle(
        color = Color.Black.copy(alpha = 0.08f),
        radius = radius + 3.dp.toPx(),
        center = center,
    )

    // Smooth 360-degree Hue sweep gradient
    drawCircle(
        brush = Brush.sweepGradient(
            colors = HUE_RING_COLORS,
            center = center,
        ),
        radius = radius,
        center = center,
    )

    // White radial desaturation core (center S = 0 -> edge S = 1)
    drawCircle(
        brush = Brush.radialGradient(
            colorStops = arrayOf(
                0.00f to Color.White,
                0.15f to Color.White.copy(alpha = 0.90f),
                0.55f to Color.White.copy(alpha = 0.35f),
                1.00f to Color.Transparent,
            ),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )

    // Subtle precision saturation rings
    val guideStroke = Stroke(width = 0.8.dp.toPx())
    drawCircle(
        color = Color.Black.copy(alpha = 0.08f),
        radius = radius * 0.33f,
        center = center,
        style = guideStroke,
    )
    drawCircle(
        color = Color.Black.copy(alpha = 0.08f),
        radius = radius * 0.66f,
        center = center,
        style = guideStroke,
    )

    // Crisp outer rim
    drawCircle(
        color = outlineColor,
        radius = radius,
        center = center,
        style = Stroke(width = 1.5.dp.toPx()),
    )
}

private fun DrawScope.drawSwatchHandle(
    position: Offset,
    swatch: HsvColor,
    isBase: Boolean,
    isSelected: Boolean,
) {
    val handleRadius = when {
        isSelected -> 15.dp.toPx()
        isBase -> 13.5.dp.toPx()
        else -> 11.dp.toPx()
    }

    // Drop shadow
    drawCircle(
        color = Color.Black.copy(alpha = if (isSelected) 0.36f else 0.24f),
        radius = handleRadius + 3.dp.toPx(),
        center = position + Offset(0f, 1.5.dp.toPx()),
    )

    // Extra double outer accent ring for Base color (Adobe Color style)
    if (isBase) {
        drawCircle(
            color = Color.White,
            radius = handleRadius + 4.dp.toPx(),
            center = position,
            style = Stroke(width = 2.dp.toPx()),
        )
        // Small top pointer triangle to highlight Base handle clearly
        val tipY = position.y - handleRadius - 9.dp.toPx()
        val baseY = position.y - handleRadius - 4.dp.toPx()
        val halfW = 4.dp.toPx()
        val pointerPath = Path().apply {
            moveTo(position.x, tipY)
            lineTo(position.x - halfW, baseY)
            lineTo(position.x + halfW, baseY)
            close()
        }
        drawPath(path = pointerPath, color = Color.Black.copy(alpha = 0.55f))
        drawPath(path = pointerPath, color = Color.White)
    }

    // Active selection halo ring
    if (isSelected) {
        drawCircle(
            color = Color.Black.copy(alpha = 0.65f),
            radius = handleRadius + 2.5.dp.toPx(),
            center = position,
            style = Stroke(width = 2.dp.toPx()),
        )
    }

    // White crisp handle collar
    drawCircle(
        color = Color.White,
        radius = handleRadius,
        center = position,
    )

    // Actual swatch color fill (including Value)
    val borderWidth = if (isSelected) 3.dp.toPx() else 2.4.dp.toPx()
    drawCircle(
        color = swatch.toComposeColor(),
        radius = (handleRadius - borderWidth).coerceAtLeast(2f),
        center = position,
    )

    // Inner dot for base handle so it stays distinguishable even if selected
    if (isBase) {
        drawCircle(
            color = swatch.contrastingContentColor().copy(alpha = 0.9f),
            radius = 2.5.dp.toPx(),
            center = position,
        )
    }
}

private fun hueSaturationToOffset(
    swatch: HsvColor,
    center: Offset,
    wheelRadius: Float,
): Offset {
    val angleRad = (swatch.hue.toDouble() * PI / 180.0)
    val distance = swatch.saturation.coerceIn(0f, 1f) * wheelRadius
    return Offset(
        x = center.x + (cos(angleRad) * distance).toFloat(),
        y = center.y + (sin(angleRad) * distance).toFloat(),
    )
}

private fun offsetToHueSaturation(
    position: Offset,
    center: Offset,
    wheelRadius: Float,
): Pair<Float, Float> {
    val dx = position.x - center.x
    val dy = position.y - center.y
    val distance = hypot(dx, dy)
    val saturation = (distance / wheelRadius).coerceIn(0f, 1f)
    val angleDeg = (atan2(dy.toDouble(), dx.toDouble()) * 180.0 / PI).toFloat()
    val hue = HsvColor.normalizeHue(angleDeg)
    return hue to saturation
}

private fun findClosestHandleIndex(
    touch: Offset,
    center: Offset,
    wheelRadius: Float,
    swatches: List<HsvColor>,
    hitRadiusPx: Float,
): Int? {
    var bestIndex: Int? = null
    var bestDistance = hitRadiusPx
    swatches.forEachIndexed { index, swatch ->
        val pos = hueSaturationToOffset(swatch, center, wheelRadius)
        val dist = hypot(touch.x - pos.x, touch.y - pos.y)
        if (dist <= bestDistance) {
            bestDistance = dist
            bestIndex = index
        }
    }
    return bestIndex
}

private val HUE_RING_COLORS: List<Color> = listOf(
    Color(0xFFFF0000), // 0°   Red
    Color(0xFFFF8000), // 30°  Orange
    Color(0xFFFFFF00), // 60°  Yellow
    Color(0xFF80FF00), // 90°  Chartreuse
    Color(0xFF00FF00), // 120° Green
    Color(0xFF00FF80), // 150° Spring Green
    Color(0xFF00FFFF), // 180° Cyan
    Color(0xFF0080FF), // 210° Azure
    Color(0xFF0000FF), // 240° Blue
    Color(0xFF8000FF), // 270° Violet
    Color(0xFFFF00FF), // 300° Magenta
    Color(0xFFFF0080), // 330° Rose
    Color(0xFFFF0000), // 360° Red
)

@Preview(showBackground = true)
@Composable
private fun HsvColorWheelCanvasPreview() {
    KalendarzReinmarTheme {
        Surface {
            HsvColorWheelCanvas(
                swatches = ColorHarmonyEngine.defaultSnapshot().swatches,
                selectedSwatchIndex = ColorHarmonyEngine.BASE_SWATCH_INDEX,
                onSwatchDragged = { _, _, _ -> },
                onGestureFinished = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
