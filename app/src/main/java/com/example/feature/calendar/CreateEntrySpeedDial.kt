package com.example.feature.calendar

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.CalendarEntryType
import com.example.ui.theme.AppIcons

@Composable
fun CreateEntrySpeedDialScrim(
    expanded: Boolean,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BackHandler(enabled = expanded, onBack = onDismiss)

    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(),
        exit = fadeOut(),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.42f))
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onDismiss,
                )
                .testTag("create_entry_speed_dial_scrim"),
        )
    }
}

@Composable
fun CreateEntrySpeedDialFab(
    expanded: Boolean,
    onToggle: () -> Unit,
    onSelectType: (CalendarEntryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    val rotation by animateFloatAsState(
        targetValue = if (expanded) 45f else 0f,
        animationSpec = spring(stiffness = 500f),
        label = "fab_rotation",
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }) + scaleIn(initialScale = 0.92f),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }) + scaleOut(targetScale = 0.92f),
        ) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CalendarEntryType.entries.forEach { entryType ->
                    SpeedDialEntryOption(
                        entryType = entryType,
                        onClick = { onSelectType(entryType) },
                    )
                }
            }
        }

        FloatingActionButton(
            onClick = onToggle,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.testTag("fab_add_event"),
        ) {
            Icon(
                painter = painterResource(AppIcons.add),
                contentDescription = if (expanded) {
                    stringResource(R.string.cd_close_create_menu)
                } else {
                    stringResource(R.string.calendar_add_event)
                },
                modifier = Modifier
                    .size(24.dp)
                    .rotate(rotation),
            )
        }
    }
}

@Composable
private fun SpeedDialEntryOption(
    entryType: CalendarEntryType,
    onClick: () -> Unit,
) {
    val palette = CalendarEntryVisuals.palette(entryType.defaultColorTag)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(onClick = onClick)
            .testTag("fab_create_type_${entryType.code.lowercase()}"),
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 4.dp,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.End,
            ) {
                Text(
                    text = stringResource(CalendarEntryVisuals.labelRes(entryType)),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = stringResource(CalendarEntryVisuals.subtitleRes(entryType)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        SmallFloatingActionButton(
            onClick = onClick,
            containerColor = palette.container,
            contentColor = palette.onContainer,
            modifier = Modifier.minimumInteractiveComponentSize(),
        ) {
            Icon(
                painter = painterResource(CalendarEntryVisuals.iconRes(entryType)),
                contentDescription = stringResource(CalendarEntryVisuals.labelRes(entryType)),
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
