/*
 * SPDX-FileCopyrightText: The Android Open Source Project
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package org.lineageos.updater.updates

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsSpace
import com.android.settingslib.spa.framework.theme.SettingsTheme

private val IconSize = 96.dp

@Composable
fun UpdateZeroStatePreference(
    modifier: Modifier = Modifier,
    text: String = "",
    description: String = "",
    compactLandscape: Boolean = false,
) {
    val colorScheme = MaterialTheme.colorScheme

    if (compactLandscape) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(
                imageVector = Icons.Filled.Verified,
                contentDescription = null,
                modifier = Modifier.size(56.dp),
                tint = colorScheme.primary,
            )
            Spacer(modifier = Modifier.height(4.dp))
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleSmall,
                )
            }
            if (description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }
        return
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = Icons.Filled.Verified,
            contentDescription = null,
            modifier = Modifier.size(IconSize),
            tint = colorScheme.primary,
        )
        Column(
            modifier = Modifier.padding(
                start = SettingsSpace.medium5,
                top = SettingsSpace.small4,
                end = SettingsSpace.medium5,
                bottom = SettingsSpace.small1,
            ),
            verticalArrangement = Arrangement.spacedBy(SettingsSpace.extraSmall2),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (text.isNotEmpty()) {
                Text(
                    text = text,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMediumEmphasized,
                )
            }
            if (description.isNotEmpty()) {
                Text(
                    text = description,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        }
    }
}

@UiModePreviews
@Composable
private fun UpdateZeroStatePreferencePreview() {
    SettingsTheme {
        UpdateZeroStatePreference(
            text = "No updates available",
            description = "New builds for this device will appear here when available.",
        )
    }
}

@Preview(name = "Rabbit R1 landscape status", widthDp = 275, heightDp = 122, showBackground = true)
@Composable
private fun UpdateZeroStatePreferenceLandscapePreview() {
    SettingsTheme {
        UpdateZeroStatePreference(
            text = "No updates available",
            description = "New builds for this device will appear here when available.",
            compactLandscape = true,
        )
    }
}
