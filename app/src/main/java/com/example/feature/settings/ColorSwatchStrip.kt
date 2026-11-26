package com.example.feature.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.HsvColor
import com.example.domain.service.ColorHarmonyEngine
import com.example.ui.theme.KalendarzReinmarTheme

@Composable
fun ColorSwatchStrip(
    swatches: List<HsvColor>,
    selectedSwatchIndex: Int,
    onSelectSwatch: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .testTag("color_swatch_strip"),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        val baseIndex = ColorHarmonyEngine.baseSwatchIndex(swatches.size)
        swatches.forEachIndexed { index, swatch ->
            val isSelected = index == selectedSwatchIndex
            val isBase = index == baseIndex
            val hex = swatch.toHex()
            val itemDescription = stringResource(
                R.string.settings_swatch_item_cd,
                index + 1,
                hex,
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(
                    width = if (isSelected) 2.dp else 1.dp,
                    color = if (isSelected) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
                    },
                ),
                tonalElevation = if (isSelected) 3.dp else 0.dp,
                modifier = Modifier
                    .weight(1f)
                    .minimumInteractiveComponentSize()
                    .clip(RoundedCornerShape(12.dp))
                    .semantics {
                        contentDescription = itemDescription
                        selected = isSelected
                    }
                    .clickable(
                        role = Role.RadioButton,
                        onClick = { onSelectSwatch(index) },
                    )
                    .testTag("color_swatch_$index"),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .background(swatch.toComposeColor()),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        if (isBase) {
                            Surface(
                                shape = RoundedCornerShape(bottomStart = 6.dp, bottomEnd = 6.dp),
                                color = swatch.contrastingContentColor().copy(alpha = 0.16f),
                            ) {
                                Text(
                                    text = stringResource(R.string.settings_swatch_base_tag),
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                    ),
                                    color = swatch.contrastingContentColor(),
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp),
                                )
                            }
                        }
                    }

                    Text(
                        text = hex,
                        style = MaterialTheme.typography.labelSmall.copy(
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        ),
                        color = if (isSelected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 2.dp, vertical = 6.dp),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ColorSwatchStripPreview() {
    KalendarzReinmarTheme {
        Surface {
            ColorSwatchStrip(
                swatches = ColorHarmonyEngine.defaultSnapshot().swatches,
                selectedSwatchIndex = ColorHarmonyEngine.BASE_SWATCH_INDEX,
                onSelectSwatch = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
