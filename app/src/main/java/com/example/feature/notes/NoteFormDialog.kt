package com.example.feature.notes

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.domain.model.CalendarEvent
import com.example.domain.model.Note
import com.example.domain.model.NoteDraft
import com.example.domain.model.NoteRichDocument
import com.example.domain.service.NoteEditorSelection
import com.example.domain.service.NoteEditorState
import com.example.domain.service.NoteRichTextEditorEngine
import com.example.ui.theme.AppIcons

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteFormDialog(
    existingNote: Note? = null,
    availableEvents: List<CalendarEvent> = emptyList(),
    onDismiss: () -> Unit,
    onSave: (NoteDraft) -> Unit,
) {
    var title by remember(existingNote?.id) {
        mutableStateOf(existingNote?.title.orEmpty())
    }
    var editorState by remember(existingNote?.id) {
        mutableStateOf(
            NoteRichTextEditorEngine.initialState(
                existingNote?.document ?: NoteRichDocument(),
            )
        )
    }
    var composition by remember(existingNote?.id) {
        mutableStateOf<TextRange?>(null)
    }
    var attachedEventIds by remember(existingNote?.id) {
        mutableStateOf(existingNote?.attachedEventIds.orEmpty())
    }
    var showAttachmentPicker by remember { mutableStateOf(false) }
    var isBodyFocused by remember { mutableStateOf(false) }
    val bodyFocusRequester = remember { FocusRequester() }

    val eventsById = remember(availableEvents) { availableEvents.associateBy { it.id } }
    val attachedEvents = remember(attachedEventIds, eventsById) {
        attachedEventIds.mapNotNull { eventsById[it] }
    }

    val richColors = rememberNoteRichTextColors()
    val visualTransformation = remember(editorState.document.spans, richColors) {
        NoteRichVisualTransformation(
            spans = editorState.document.spans,
            colors = richColors,
        )
    }

    val textFieldValue = TextFieldValue(
        text = editorState.document.text,
        selection = TextRange(editorState.selection.start, editorState.selection.end),
        composition = composition,
    )

    val canSave = !editorState.document.isBlank

    fun applyToolbarAction(transform: (NoteEditorState) -> NoteEditorState) {
        composition = null
        editorState = transform(editorState)
        if (!isBodyFocused) {
            bodyFocusRequester.requestFocus()
        }
    }

    BackHandler(onBack = onDismiss)

    Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxSize()
            .testTag("note_editor_dialog"),
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopAppBar(
                        navigationIcon = {
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("note_cancel_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.note_editor_close_cd),
                                )
                            }
                        },
                        title = {
                            Text(
                                text = stringResource(
                                    if (existingNote == null) R.string.notes_add_note else R.string.notes_edit_note,
                                ),
                                style = MaterialTheme.typography.titleLarge,
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { showAttachmentPicker = true },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("note_topbar_attach_button"),
                            ) {
                                BadgedBox(
                                    badge = {
                                        if (attachedEvents.isNotEmpty()) {
                                            Badge {
                                                Text(attachedEvents.size.toString())
                                            }
                                        }
                                    },
                                ) {
                                    Icon(
                                        painter = painterResource(AppIcons.calendar),
                                        contentDescription = stringResource(R.string.note_editor_attach_events_cd),
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }

                            Button(
                                onClick = {
                                    val draft = NoteDraft(
                                        title = title,
                                        document = editorState.document,
                                        attachedEventIds = attachedEventIds,
                                    ).sanitized()
                                    if (draft != null) {
                                        onSave(draft)
                                    }
                                },
                                enabled = canSave,
                                modifier = Modifier
                                    .padding(end = 10.dp)
                                    .minimumInteractiveComponentSize()
                                    .testTag("note_save_button"),
                            ) {
                                Text(stringResource(R.string.action_save))
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background,
                            titleContentColor = MaterialTheme.colorScheme.onBackground,
                        ),
                        windowInsets = WindowInsets.statusBars,
                    )

                    NoteFormatToolbar(
                        activeFormat = editorState.activeFormat,
                        activeListStyle = editorState.activeListStyle,
                        onToggleBold = { applyToolbarAction(NoteRichTextEditorEngine::toggleBold) },
                        onToggleItalic = { applyToolbarAction(NoteRichTextEditorEngine::toggleItalic) },
                        onToggleUnderline = { applyToolbarAction(NoteRichTextEditorEngine::toggleUnderline) },
                        onToggleStrikethrough = { applyToolbarAction(NoteRichTextEditorEngine::toggleStrikethrough) },
                        onToggleSuperscript = { applyToolbarAction(NoteRichTextEditorEngine::toggleSuperscript) },
                        onToggleSubscript = { applyToolbarAction(NoteRichTextEditorEngine::toggleSubscript) },
                        onSelectTextSize = { size ->
                            applyToolbarAction { state -> NoteRichTextEditorEngine.applyTextSize(state, size) }
                        },
                        onToggleListStyle = { listStyle ->
                            applyToolbarAction { state -> NoteRichTextEditorEngine.toggleListStyle(state, listStyle) }
                        },
                        onClearFormatting = { applyToolbarAction(NoteRichTextEditorEngine::clearFormatting) },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .imePadding()
                    .navigationBarsPadding(),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .widthIn(max = 680.dp)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    // Editorial Title input
                    BasicTextField(
                        value = title,
                        onValueChange = { title = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.headlineSmall.copy(
                            color = MaterialTheme.colorScheme.onBackground,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(modifier = Modifier.fillMaxWidth()) {
                                if (title.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.notes_title_label),
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("note_title_input"),
                    )

                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                    // Live WYSIWYG Rich Text Body
                    BasicTextField(
                        value = textFieldValue,
                        onValueChange = { incoming ->
                            val updated = NoteRichTextEditorEngine.onTextChanged(
                                state = editorState,
                                newText = incoming.text,
                                newSelection = NoteEditorSelection(
                                    start = incoming.selection.start,
                                    end = incoming.selection.end,
                                ),
                            )
                            composition = if (updated.document.text == incoming.text) {
                                incoming.composition
                            } else {
                                null
                            }
                            editorState = updated
                        },
                        visualTransformation = visualTransformation,
                        textStyle = TextStyle(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                        ),
                        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                        decorationBox = { innerTextField ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 200.dp),
                            ) {
                                if (editorState.document.text.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.note_editor_placeholder),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                    )
                                }
                                innerTextField()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(bodyFocusRequester)
                            .onFocusChanged { isBodyFocused = it.isFocused }
                            .testTag("note_content_input"),
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Calendar event attachments section
                    NoteEditorAttachmentsSection(
                        attachedEvents = attachedEvents,
                        onOpenPicker = { showAttachmentPicker = true },
                        onRemoveEvent = { eventId ->
                            attachedEventIds = attachedEventIds.filterNot { it == eventId }
                        },
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }

    if (showAttachmentPicker) {
        NoteEventAttachmentPickerDialog(
            availableEvents = availableEvents,
            initialSelectedIds = attachedEventIds,
            onConfirm = { selected ->
                attachedEventIds = selected
                showAttachmentPicker = false
            },
            onDismiss = { showAttachmentPicker = false },
        )
    }
}
