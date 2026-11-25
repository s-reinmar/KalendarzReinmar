package com.example.feature.notes

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.BaselineShift
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.NoteListStyle
import com.example.domain.model.NoteScriptStyle
import com.example.domain.model.NoteTextFormat
import com.example.domain.model.NoteTextSize

@Composable
internal fun NoteFormatToolbar(
    activeFormat: NoteTextFormat,
    activeListStyle: NoteListStyle?,
    onToggleBold: () -> Unit,
    onToggleItalic: () -> Unit,
    onToggleUnderline: () -> Unit,
    onToggleStrikethrough: () -> Unit,
    onToggleSuperscript: () -> Unit,
    onToggleSubscript: () -> Unit,
    onSelectTextSize: (NoteTextSize) -> Unit,
    onToggleListStyle: (NoteListStyle) -> Unit,
    onClearFormatting: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 2.dp,
        modifier = modifier
            .fillMaxWidth()
            .testTag("note_format_toolbar"),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Row 1: Inline formatting, superscript/subscript, and text sizes
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FormatTogglePill(
                    selected = activeFormat.bold,
                    contentDesc = stringResource(R.string.note_editor_format_bold_cd),
                    testTag = "format_bold_button",
                    onClick = onToggleBold,
                ) {
                    Text(
                        text = "B",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                    )
                }

                FormatTogglePill(
                    selected = activeFormat.italic,
                    contentDesc = stringResource(R.string.note_editor_format_italic_cd),
                    testTag = "format_italic_button",
                    onClick = onToggleItalic,
                ) {
                    Text(
                        text = "I",
                        fontFamily = FontFamily.SansSerif,
                        fontStyle = FontStyle.Italic,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                    )
                }

                FormatTogglePill(
                    selected = activeFormat.underline,
                    contentDesc = stringResource(R.string.note_editor_format_underline_cd),
                    testTag = "format_underline_button",
                    onClick = onToggleUnderline,
                ) {
                    Text(
                        text = "U",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.Underline,
                        fontSize = 15.sp,
                    )
                }

                FormatTogglePill(
                    selected = activeFormat.strikethrough,
                    contentDesc = stringResource(R.string.note_editor_format_strikethrough_cd),
                    testTag = "format_strike_button",
                    onClick = onToggleStrikethrough,
                ) {
                    Text(
                        text = "S",
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        textDecoration = TextDecoration.LineThrough,
                        fontSize = 15.sp,
                    )
                }

                ToolbarDivider()

                FormatTogglePill(
                    selected = activeFormat.script == NoteScriptStyle.SUPERSCRIPT,
                    contentDesc = stringResource(R.string.note_editor_format_superscript_cd),
                    testTag = "format_superscript_button",
                    onClick = onToggleSuperscript,
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("X")
                            withStyle(SpanStyle(fontSize = 10.sp, baselineShift = BaselineShift.Superscript)) {
                                append("2")
                            }
                        },
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }

                FormatTogglePill(
                    selected = activeFormat.script == NoteScriptStyle.SUBSCRIPT,
                    contentDesc = stringResource(R.string.note_editor_format_subscript_cd),
                    testTag = "format_subscript_button",
                    onClick = onToggleSubscript,
                ) {
                    Text(
                        text = buildAnnotatedString {
                            append("X")
                            withStyle(SpanStyle(fontSize = 10.sp, baselineShift = BaselineShift.Subscript)) {
                                append("2")
                            }
                        },
                        fontFamily = FontFamily.SansSerif,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                    )
                }

                ToolbarDivider()

                SizeOptionChip(
                    size = NoteTextSize.HEADING_1,
                    label = stringResource(R.string.note_size_h1),
                    contentDesc = stringResource(R.string.note_size_h1_cd),
                    selected = activeFormat.size == NoteTextSize.HEADING_1,
                    testTag = "format_size_h1",
                    onClick = { onSelectTextSize(NoteTextSize.HEADING_1) },
                )
                SizeOptionChip(
                    size = NoteTextSize.HEADING_2,
                    label = stringResource(R.string.note_size_h2),
                    contentDesc = stringResource(R.string.note_size_h2_cd),
                    selected = activeFormat.size == NoteTextSize.HEADING_2,
                    testTag = "format_size_h2",
                    onClick = { onSelectTextSize(NoteTextSize.HEADING_2) },
                )
                SizeOptionChip(
                    size = NoteTextSize.HEADING_3,
                    label = stringResource(R.string.note_size_h3),
                    contentDesc = stringResource(R.string.note_size_h3_cd),
                    selected = activeFormat.size == NoteTextSize.HEADING_3,
                    testTag = "format_size_h3",
                    onClick = { onSelectTextSize(NoteTextSize.HEADING_3) },
                )
                SizeOptionChip(
                    size = NoteTextSize.BODY,
                    label = stringResource(R.string.note_size_body),
                    contentDesc = stringResource(R.string.note_size_body_cd),
                    selected = activeFormat.size == NoteTextSize.BODY,
                    testTag = "format_size_body",
                    onClick = { onSelectTextSize(NoteTextSize.BODY) },
                )
                SizeOptionChip(
                    size = NoteTextSize.SMALL,
                    label = stringResource(R.string.note_size_small),
                    contentDesc = stringResource(R.string.note_size_small_cd),
                    selected = activeFormat.size == NoteTextSize.SMALL,
                    testTag = "format_size_small",
                    onClick = { onSelectTextSize(NoteTextSize.SMALL) },
                )
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
                modifier = Modifier.padding(horizontal = 12.dp),
            )

            // Row 2: Quick list builders & format reset
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ListStylePill(
                    symbol = "•",
                    label = stringResource(R.string.note_list_bullet),
                    contentDesc = stringResource(R.string.note_list_bullet_cd),
                    selected = activeListStyle == NoteListStyle.BULLET,
                    testTag = "format_list_bullet",
                    onClick = { onToggleListStyle(NoteListStyle.BULLET) },
                )
                ListStylePill(
                    symbol = "1.",
                    label = stringResource(R.string.note_list_numbered),
                    contentDesc = stringResource(R.string.note_list_numbered_cd),
                    selected = activeListStyle == NoteListStyle.NUMBERED,
                    testTag = "format_list_numbered",
                    onClick = { onToggleListStyle(NoteListStyle.NUMBERED) },
                )
                ListStylePill(
                    symbol = "☐",
                    label = stringResource(R.string.note_list_checklist),
                    contentDesc = stringResource(R.string.note_list_checklist_cd),
                    selected = activeListStyle == NoteListStyle.CHECKLIST,
                    testTag = "format_list_checklist",
                    onClick = { onToggleListStyle(NoteListStyle.CHECKLIST) },
                )
                ListStylePill(
                    symbol = "a)",
                    label = stringResource(R.string.note_list_lettered),
                    contentDesc = stringResource(R.string.note_list_lettered_cd),
                    selected = activeListStyle == NoteListStyle.LETTERED,
                    testTag = "format_list_lettered",
                    onClick = { onToggleListStyle(NoteListStyle.LETTERED) },
                )
                ListStylePill(
                    symbol = "–",
                    label = stringResource(R.string.note_list_dash),
                    contentDesc = stringResource(R.string.note_list_dash_cd),
                    selected = activeListStyle == NoteListStyle.DASH,
                    testTag = "format_list_dash",
                    onClick = { onToggleListStyle(NoteListStyle.DASH) },
                )

                ToolbarDivider()

                FormatTogglePill(
                    selected = false,
                    contentDesc = stringResource(R.string.note_editor_format_clear_cd),
                    testTag = "format_clear_button",
                    onClick = onClearFormatting,
                ) {
                    Text(
                        text = stringResource(R.string.note_editor_format_clear),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            ActiveFormatSummaryStrip(activeFormat = activeFormat)

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

@Composable
private fun ActiveFormatSummaryStrip(activeFormat: NoteTextFormat) {
    val activeLabels = buildList {
        when (activeFormat.size) {
            NoteTextSize.HEADING_1 -> add(stringResource(R.string.note_size_h1))
            NoteTextSize.HEADING_2 -> add(stringResource(R.string.note_size_h2))
            NoteTextSize.HEADING_3 -> add(stringResource(R.string.note_size_h3))
            NoteTextSize.SMALL -> add(stringResource(R.string.note_size_small))
            NoteTextSize.BODY -> Unit
        }
        if (activeFormat.bold) add(stringResource(R.string.note_editor_format_bold_cd))
        if (activeFormat.italic) add(stringResource(R.string.note_editor_format_italic_cd))
        if (activeFormat.underline) add(stringResource(R.string.note_editor_format_underline_cd))
        if (activeFormat.strikethrough) add(stringResource(R.string.note_editor_format_strikethrough_cd))
        when (activeFormat.script) {
            NoteScriptStyle.SUPERSCRIPT -> add(stringResource(R.string.note_editor_format_superscript_cd))
            NoteScriptStyle.SUBSCRIPT -> add(stringResource(R.string.note_editor_format_subscript_cd))
            NoteScriptStyle.NORMAL -> Unit
        }
    }
    if (activeLabels.isEmpty()) return

    Text(
        text = stringResource(
            R.string.note_editor_active_format_prefix,
            activeLabels.joinToString(" · "),
        ),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 16.dp, bottom = 6.dp)
            .testTag("note_active_format_summary"),
    )
}

@Composable
private fun ToolbarDivider() {
    VerticalDivider(
        modifier = Modifier
            .height(22.dp)
            .width(1.dp)
            .padding(horizontal = 2.dp),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun FormatTogglePill(
    selected: Boolean,
    contentDesc: String,
    testTag: String,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val containerColor = if (selected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (selected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurface
    }
    val borderColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.outlineVariant
    }

    Surface(
        shape = RoundedCornerShape(10.dp),
        color = containerColor,
        contentColor = contentColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = Modifier
            .widthIn(min = 44.dp)
            .heightIn(min = 40.dp)
            .clip(RoundedCornerShape(10.dp))
            .focusProperties { canFocus = false }
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                contentDescription = contentDesc
                this.selected = selected
            }
            .testTag(testTag),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
        ) {
            content()
        }
    }
}

@Composable
private fun SizeOptionChip(
    size: NoteTextSize,
    label: String,
    contentDesc: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    FormatTogglePill(
        selected = selected,
        contentDesc = contentDesc,
        testTag = testTag,
        onClick = onClick,
    ) {
        val weight = if (size.isHeading) FontWeight.Bold else FontWeight.Medium
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = weight,
        )
    }
}

@Composable
private fun ListStylePill(
    symbol: String,
    label: String,
    contentDesc: String,
    selected: Boolean,
    testTag: String,
    onClick: () -> Unit,
) {
    FormatTogglePill(
        selected = selected,
        contentDesc = contentDesc,
        testTag = testTag,
        onClick = onClick,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = symbol,
                style = MaterialTheme.typography.labelLarge,
                color = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
