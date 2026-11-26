package com.example.feature.shell

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.theme.KalendarzReinmarTheme

@Composable
internal fun AppCopyrightFooter(
    onOpenAuthorProfile: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val prefix = stringResource(R.string.footer_copyright_prefix)
    val authorHandle = stringResource(R.string.footer_author_handle)
    val suffix = stringResource(R.string.footer_copyright_suffix)
    val githubUrl = stringResource(R.string.footer_github_url)
    val linkContentDescription = stringResource(R.string.cd_footer_github_link)

    val linkColor = MaterialTheme.colorScheme.primary
    val mutedTextColor = MaterialTheme.colorScheme.onSurfaceVariant

    val footerText = buildAnnotatedString {
        withStyle(SpanStyle(color = mutedTextColor)) {
            append(prefix)
            append(' ')
        }
        withStyle(
            SpanStyle(
                color = linkColor,
                fontWeight = FontWeight.SemiBold,
                textDecoration = TextDecoration.Underline,
            ),
        ) {
            append(authorHandle)
        }
        withStyle(SpanStyle(color = mutedTextColor)) {
            append(suffix)
        }
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("app_copyright_footer"),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f),
            )
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .minimumInteractiveComponentSize()
                    .clickable(
                        role = Role.Button,
                        onClick = { onOpenAuthorProfile(githubUrl) },
                    )
                    .semantics {
                        contentDescription = linkContentDescription
                    }
                    .padding(horizontal = 16.dp, vertical = 10.dp)
                    .testTag("app_copyright_github_link"),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = footerText,
                    style = MaterialTheme.typography.labelMedium,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AppCopyrightFooterPreview() {
    KalendarzReinmarTheme {
        AppCopyrightFooter(onOpenAuthorProfile = {})
    }
}
