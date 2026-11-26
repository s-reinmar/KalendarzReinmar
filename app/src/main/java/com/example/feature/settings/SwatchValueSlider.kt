package com.example.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.HsvColor
import com.example.domain.service.ColorHarmonyEngine
import com.example.ui.theme.KalendarzReinmarTheme
import kotlin.math.roundToInt

@Composable
fun SwatchValueSlider(
    activeSwatch: HsvColor,
    selectedSwatchIndex: Int,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    modifier: Modifier = Modifier,
    baseSwatchIndex: Int = ColorHarmonyEngine.BASE_SWATCH_INDEX,
) {
    val swatchNumber = selectedSwatchIndex + 1
    val isBase = selectedSwatchIndex == baseSwatchIndex
    val percent = (activeSwatch.value * 100f).roundToInt().coerceIn(0, 100)
    val fullBrightColor = activeSwatch.withValue(1f).toComposeColor()
    val currentColor = activeSwatch.toComposeColor()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = if (isBase) {
                    stringResource(R.string.settings_value_slider_base_label, swatchNumber)
                } else {
                    stringResource(R.string.settings_value_slider_label, swatchNumber)
                },
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = currentColor,
                    contentColor = activeSwatch.contrastingContentColor(),
                ) {
                    Text(
                        text = activeSwatch.toHex(),
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.SemiBold,
                        ),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                    )
                }
                Text(
                    text = stringResource(R.string.settings_value_percent, percent),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Visual brightness ramp from V=0 (black) to V=1 (full hue/saturation)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(10.dp)
                .clip(RoundedCornerShape(999.dp))
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.Black, fullBrightColor),
                    ),
                )
                .border(
                    width = 0.75.dp,
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(999.dp),
                ),
        )

        Slider(
            value = activeSwatch.value.coerceIn(0f, 1f),
            onValueChange = onValueChange,
            onValueChangeFinished = onValueChangeFinished,
            valueRange = 0f..1f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("swatch_value_slider"),
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SwatchValueSliderPreview() {
    KalendarzReinmarTheme {
        Surface {
            SwatchValueSlider(
                activeSwatch = ColorHarmonyEngine.defaultBaseColor,
                selectedSwatchIndex = ColorHarmonyEngine.BASE_SWATCH_INDEX,
                onValueChange = {},
                onValueChangeFinished = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
