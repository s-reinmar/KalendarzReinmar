package com.example.feature.map

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.model.GeocodingResult

@Composable
fun OsmSearchBar(
    searchState: PlaceSearchUiState,
    onQueryChanged: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onClear: () -> Unit,
    onResultSelected: (GeocodingResult) -> Unit,
    modifier: Modifier = Modifier,
    testTagPrefix: String = "osm_search",
) {
    val keyboardController = LocalSoftwareKeyboardController.current

    fun submit() {
        keyboardController?.hide()
        onSearchSubmit()
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        OutlinedTextField(
            value = searchState.query,
            onValueChange = onQueryChanged,
            placeholder = {
                Text(
                    text = stringResource(R.string.map_search_placeholder),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Search,
                    contentDescription = stringResource(R.string.map_search_action),
                    tint = MaterialTheme.colorScheme.primary,
                )
            },
            trailingIcon = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (searchState.isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .padding(end = 8.dp)
                                .size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else if (searchState.query.isNotEmpty()) {
                        IconButton(
                            onClick = onClear,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${testTagPrefix}_clear_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.map_search_clear),
                            )
                        }
                        IconButton(
                            onClick = { submit() },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("${testTagPrefix}_submit_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Search,
                                contentDescription = stringResource(R.string.map_search_action),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { submit() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("${testTagPrefix}_input"),
        )

        when (val status = searchState.status) {
            is PlaceSearchStatus.Results -> {
                SearchResultsDropdown(
                    results = status.items,
                    onResultSelected = onResultSelected,
                    testTagPrefix = testTagPrefix,
                )
            }
            PlaceSearchStatus.Empty -> {
                SearchStatusText(
                    text = stringResource(R.string.map_search_no_results),
                    testTag = "${testTagPrefix}_status_text",
                )
            }
            PlaceSearchStatus.Error -> {
                SearchStatusText(
                    text = stringResource(R.string.map_search_error),
                    testTag = "${testTagPrefix}_status_text",
                )
            }
            PlaceSearchStatus.Idle, PlaceSearchStatus.Loading -> Unit
        }
    }
}

@Composable
private fun SearchResultsDropdown(
    results: List<GeocodingResult>,
    onResultSelected: (GeocodingResult) -> Unit,
    testTagPrefix: String,
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 4.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("${testTagPrefix}_results_card"),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 200.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            results.forEachIndexed { idx, item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResultSelected(item) }
                        .padding(horizontal = 14.dp, vertical = 12.dp)
                        .testTag("${testTagPrefix}_result_$idx"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.shortName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = item.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
                if (idx < results.lastIndex) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
private fun SearchStatusText(text: String, testTag: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(horizontal = 4.dp)
            .testTag(testTag),
    )
}
