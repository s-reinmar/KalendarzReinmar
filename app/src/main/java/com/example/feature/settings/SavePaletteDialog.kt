package com.example.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.ColorHarmonyRule
import com.example.domain.model.HsvColor
import com.example.domain.service.ColorHarmonyEngine
import com.example.ui.theme.KalendarzReinmarTheme

@Composable
fun SavePaletteDialog(
    activeRule: ColorHarmonyRule,
    swatches: List<HsvColor>,
    onDismiss: () -> Unit,
    onConfirmSave: (String) -> Unit,
) {
    var paletteName by rememberSaveable { mutableStateOf("") }
    var showBlankError by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.settings_save_dialog_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = stringResource(
                        R.string.settings_color_wheel_subtitle,
                        stringResource(activeRule.labelRes()),
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(28.dp)
                        .clip(RoundedCornerShape(10.dp)),
                ) {
                    swatches.forEach { swatch ->
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(28.dp)
                                .background(swatch.toComposeColor()),
                        )
                    }
                }

                OutlinedTextField(
                    value = paletteName,
                    onValueChange = { updated ->
                        paletteName = updated
                        if (showBlankError && updated.isNotBlank()) {
                            showBlankError = false
                        }
                    },
                    label = { Text(stringResource(R.string.settings_save_dialog_name_label)) },
                    placeholder = { Text(stringResource(R.string.settings_save_dialog_name_placeholder)) },
                    singleLine = true,
                    isError = showBlankError,
                    supportingText = if (showBlankError) {
                        { Text(stringResource(R.string.settings_save_dialog_name_error)) }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("save_palette_name_input"),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = paletteName.trim()
                    if (trimmed.isEmpty()) {
                        showBlankError = true
                    } else {
                        onConfirmSave(trimmed)
                    }
                },
                modifier = Modifier.testTag("confirm_save_palette_button"),
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_save_palette_button"),
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        },
    )
}

@Preview(showBackground = true)
@Composable
private fun SavePaletteDialogPreview() {
    KalendarzReinmarTheme {
        SavePaletteDialog(
            activeRule = ColorHarmonyRule.ANALOGOUS,
            swatches = ColorHarmonyEngine.defaultSnapshot().swatches,
            onDismiss = {},
            onConfirmSave = {},
        )
    }
}
