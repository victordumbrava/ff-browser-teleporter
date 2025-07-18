/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.fenix.translations

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import mozilla.components.compose.base.button.PrimaryButton
import org.mozilla.fenix.R
import org.mozilla.fenix.theme.FirefoxTheme

/**
 * Animation duration in milliseconds.
 * If it is set to a low number, the speed of the rotation will be higher.
 */
private const val ANIMATION_DURATION_MS = 2000

/**
 * Icon for Download indicator.
 *
 * @param icon [Painter] used to be displayed.
 * @param modifier [Modifier] to be applied to the icon layout.
 * @param tint Tint [Color] to be applied to the icon.
 * @param contentDescription Optional content description for the icon.
 */
@Composable
fun DownloadIconIndicator(
    icon: Painter,
    modifier: Modifier = Modifier,
    tint: Color = FirefoxTheme.colors.iconPrimary,
    contentDescription: String? = null,
) {
    Icon(
        painter = icon,
        modifier = modifier.then(
            Modifier
                .rotate(rotationAnimation()),
        ),
        contentDescription = contentDescription,
        tint = tint,
    )
}

/**
 * Icon for Download In Progress indicator.
 *
 * @param modifier [Modifier] to be applied to the icon layout.
 * @param icon [Painter] used to be displayed.
 * @param tint Tint [Color] to be applied to the icon.
 * @param contentDescription Optional content description for the icon.
 */
@Composable
fun DownloadInProgressIndicator(
    modifier: Modifier = Modifier,
    icon: Painter = painterResource(id = R.drawable.mozac_ic_stop_8),
    tint: Color = FirefoxTheme.colors.iconPrimary,
    contentDescription: String? = null,
) {
    Box(
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = modifier.size(8.dp),
            painter = icon,
            contentDescription = contentDescription,
            tint = tint,
        )
        CircularProgressIndicator(
            modifier = modifier.size(30.dp),
            color = FirefoxTheme.colors.layerAccent,
            strokeWidth = 2.dp,
            trackColor = FirefoxTheme.colors.actionTertiary,
            strokeCap = StrokeCap.Butt,
        )
    }
}

/**
 * Download indicator for translations screens.
 * It indicates that the download of the language file is in progress.
 *
 * @param text The button text to be displayed.
 * @param modifier [Modifier] to be applied to the layout.
 * @param contentDescription Content description to be applied to the button.
 * @param icon Optional [Painter] used to display an [Icon] before the button text.
 */
@Composable
fun DownloadIndicator(
    text: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    icon: Painter? = null,
) {
    PrimaryButton(
        text = text,
        modifier = modifier.then(
            Modifier
                .clearAndSetSemantics {
                    role = Role.Button
                    contentDescription?.let { this.contentDescription = contentDescription }
                }
                .wrapContentSize(),
        ),
        icon = icon,
        iconModifier = Modifier
            .rotate(rotationAnimation())
            .size(ButtonDefaults.IconSize),
        onClick = {},
    )
}

@Composable
internal fun rotationAnimation(): Float {
    val infiniteTransition = rememberInfiniteTransition()
    val angle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(ANIMATION_DURATION_MS, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
    )
    return angle
}

@Composable
@PreviewLightDark
private fun DownloadIconIndicatorPreview() {
    FirefoxTheme {
        Column(
            modifier = Modifier
                .background(FirefoxTheme.colors.layer1)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DownloadIconIndicator(
                icon = painterResource(id = R.drawable.mozac_ic_sync_24),
                contentDescription = stringResource(
                    id = R.string.translations_bottom_sheet_translating_in_progress,
                ),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun DownloadInProgressIndicatorPreview() {
    FirefoxTheme {
        Column(
            modifier = Modifier
                .background(FirefoxTheme.colors.layer1)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DownloadInProgressIndicator(
                contentDescription = stringResource(
                    id = R.string.translations_bottom_sheet_translating_in_progress,
                ),
            )
        }
    }
}

@Composable
@PreviewLightDark
private fun DownloadIndicatorPreview() {
    FirefoxTheme {
        Column(
            modifier = Modifier
                .background(FirefoxTheme.colors.layer1)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DownloadIndicator(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                text = stringResource(id = R.string.translations_bottom_sheet_translating_in_progress),
                contentDescription = stringResource(
                    id = R.string.translations_bottom_sheet_translating_in_progress_content_description,
                ),
                icon = painterResource(id = R.drawable.mozac_ic_sync_24),
            )
        }
    }
}
