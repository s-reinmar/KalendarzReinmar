package com.example.feature.calendar

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.domain.model.EventColorTag
import java.util.Locale
import kotlin.math.roundToInt

private val QUICK_RGB_SWATCHES: List<EventColorTag.CustomRgb> = listOf(
    EventColorTag.CustomRgb(211, 47, 47),
    EventColorTag.CustomRgb(233, 30, 99),
    EventColorTag.CustomRgb(245, 124, 0),
    EventColorTag.CustomRgb(251, 192, 45),
    EventColorTag.CustomRgb(56, 142, 60),
    EventColorTag.CustomRgb(0, 137, 123),
    EventColorTag.CustomRgb(2, 136, 209),
    EventColorTag.CustomRgb(48, 63, 159),
    EventColorTag.CustomRgb(123, 31, 162),
    EventColorTag.CustomRgb(93, 64, 55),
    EventColorTag.CustomRgb(69, 90, 100),
    EventColorTag.CustomRgb(33, 33, 33),
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RgbColorPickerDialog(
    initialColor: EventColorTag,
    onDismiss: () -> Unit,
    onConfirm: (EventColorTag) -> Unit,
) {
    var red by remember(initialColor) { mutableIntStateOf(initialColor.red) }
    var green by remember(initialColor) { mutableIntStateOf(initialColor.green) }
    var blue by remember(initialColor) { mutableIntStateOf(initialColor.blue) }
    var hexInput by remember(initialColor) { mutableStateOf(initialColor.hexString) }
    var isHexInvalid by remember { mutableStateOf(false) }

    val activeTag = remember(red, green, blue) { EventColorTag.fromRgb(red, green, blue) }
    val previewPalette = CalendarEntryVisuals.palette(activeTag)
    val swatchColor = Color(red = red, green = green, blue = blue)
    val swatchOnColor = CalendarEntryVisuals.contrastingOnColor(red, green, blue)

    fun updateChannels(r: Int, g: Int, b: Int) {
        red = r.coerceIn(0, 255)
        green = g.coerceIn(0, 255)
        blue = b.coerceIn(0, 255)
        hexInput = String.format(Locale.US, "#%02X%02X%02X", red, green, blue)
        isHexInvalid = false
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .widthIn(max = 520.dp)
                .heightIn(max = 720.dp)
                .testTag("rgb_color_picker_dialog"),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(R.string.entry_color_picker_title),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )

                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = previewPalette.container,
                        border = BorderStroke(1.5.dp, previewPalette.accent.copy(alpha = 0.55f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rgb_color_preview"),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .background(swatchColor)
                                    .border(
                                        BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant),
                                        CircleShape,
                                    ),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = swatchOnColor,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.entry_color_preview_sample),
                                    style = MaterialTheme.typography.titleMedium,
                                    color = previewPalette.onContainer,
                                )
                                Text(
                                    text = "${activeTag.hexString}  •  RGB($red, $green, $blue)",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = previewPalette.onContainer.copy(alpha = 0.8f),
                                )
                            }
                        }
                    }

                    RgbChannelSlider(
                        label = stringResource(R.string.entry_color_channel_red),
                        value = red,
                        trackColor = Color(0xFFD32F2F),
                        testTag = "rgb_slider_red",
                        onValueChange = { updateChannels(it, green, blue) },
                    )

                    RgbChannelSlider(
                        label = stringResource(R.string.entry_color_channel_green),
                        value = green,
                        trackColor = Color(0xFF2E7D32),
                        testTag = "rgb_slider_green",
                        onValueChange = { updateChannels(red, it, blue) },
                    )

                    RgbChannelSlider(
                        label = stringResource(R.string.entry_color_channel_blue),
                        value = blue,
                        trackColor = Color(0xFF1976D2),
                        testTag = "rgb_slider_blue",
                        onValueChange = { updateChannels(red, green, it) },
                    )

                    OutlinedTextField(
                        value = hexInput,
                        onValueChange = { raw ->
                            val filtered = raw.uppercase(Locale.US)
                                .filter { it == '#' || it in '0'..'9' || it in 'A'..'F' }
                                .take(7)
                            hexInput = filtered
                            val parsed = EventColorTag.fromHexOrNull(filtered)
                            if (parsed != null) {
                                red = parsed.red
                                green = parsed.green
                                blue = parsed.blue
                                isHexInvalid = false
                            } else {
                                isHexInvalid = filtered.isNotEmpty()
                            }
                        },
                        label = { Text(stringResource(R.string.entry_color_hex_label)) },
                        isError = isHexInvalid,
                        supportingText = if (isHexInvalid) {
                            { Text(stringResource(R.string.entry_color_hex_error)) }
                        } else {
                            null
                        },
                        singleLine = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("rgb_hex_input"),
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = stringResource(R.string.entry_color_quick_swatches),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            QUICK_RGB_SWATCHES.forEach { swatch ->
                                val isCurrent = swatch.red == red && swatch.green == green && swatch.blue == blue
                                val color = Color(swatch.red, swatch.green, swatch.blue)
                                val checkTint = CalendarEntryVisuals.contrastingOnColor(
                                    swatch.red,
                                    swatch.green,
                                    swatch.blue,
                                )
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(CircleShape)
                                        .clickable(
                                            role = Role.RadioButton,
                                            onClick = { updateChannels(swatch.red, swatch.green, swatch.blue) },
                                        )
                                        .semantics { contentDescription = swatch.hexString }
                                        .testTag("rgb_quick_swatch_${swatch.hexString.removePrefix("#").lowercase(Locale.US)}"),
                                ) {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                            .then(
                                                if (isCurrent) {
                                                    Modifier.border(
                                                        BorderStroke(2.5.dp, MaterialTheme.colorScheme.onSurface),
                                                        CircleShape,
                                                    )
                                                } else {
                                                    Modifier.border(
                                                        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                                        CircleShape,
                                                    )
                                                }
                                            ),
                                    ) {
                                        if (isCurrent) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = checkTint,
                                                modifier = Modifier.size(16.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("rgb_picker_cancel_button"),
                    ) {
                        Text(stringResource(R.string.action_cancel))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = { onConfirm(activeTag) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("rgb_picker_confirm_button"),
                    ) {
                        Text(stringResource(R.string.entry_color_apply))
                    }
                }
            }
        }
    }
}

@Composable
private fun RgbChannelSlider(
    label: String,
    value: Int,
    trackColor: Color,
    testTag: String,
    onValueChange: (Int) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.labelLarge,
                color = trackColor,
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.roundToInt()) },
            valueRange = 0f..255f,
            colors = SliderDefaults.colors(
                thumbColor = trackColor,
                activeTrackColor = trackColor,
                inactiveTrackColor = trackColor.copy(alpha = 0.22f),
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag),
        )
    }
}
