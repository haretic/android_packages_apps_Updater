/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.deviceinfo.actions

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsShape.CornerExtraLarge1
import com.android.settingslib.spa.framework.theme.SettingsShape.CornerExtraSmall2
import com.android.settingslib.spa.framework.theme.SettingsSpace.small1
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.lineageos.updater.R
import org.lineageos.updater.deviceinfo.DeviceInfoUtils

@Composable
fun DeviceInfoActionButtons(
    modifier: Modifier = Modifier,
    compact: Boolean = false,
    stacked: Boolean = false,
) {
    val context = LocalContext.current

    val contentPadding = if (compact) {
        PaddingValues(horizontal = 8.dp, vertical = 4.dp)
    } else {
        ButtonDefaults.TextButtonContentPadding
    }
    val buttonColors = ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurface,
    )
    val buttons: @Composable (Modifier) -> Unit = { buttonModifier ->
        TextButton(
            onClick = {
                val url = context.getString(R.string.menu_changelog_url, DeviceInfoUtils.device)
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            },
            colors = buttonColors,
            modifier = buttonModifier,
            contentPadding = contentPadding,
        ) {
            Text(text = stringResource(R.string.show_changelog))
        }
        TextButton(
            onClick = {
                val url = context.getString(R.string.report_issue_url)
                val intent = Intent(Intent.ACTION_VIEW, url.toUri())
                context.startActivity(intent)
            },
            colors = buttonColors,
            modifier = buttonModifier,
            contentPadding = contentPadding,
        ) {
            Text(text = stringResource(R.string.report_issues))
        }
    }

    if (stacked) {
        Column(
            modifier = modifier.fillMaxWidth().focusGroup(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            buttons(Modifier.fillMaxWidth().heightIn(min = 48.dp))
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (compact) {
                        Modifier.focusGroup()
                    } else {
                        Modifier
                            .clip(
                                CornerExtraLarge1.copy(
                                    topStart = CornerExtraSmall2.topStart,
                                    topEnd = CornerExtraSmall2.topEnd,
                                )
                            )
                            .background(MaterialTheme.colorScheme.surfaceBright)
                            .padding(SettingsDimension.itemPaddingAround)
                    }
                ),
            horizontalArrangement = Arrangement.spacedBy(if (compact) 4.dp else small1),
        ) {
            // Share the width so both actions remain reachable with longer translations.
            buttons(if (compact) Modifier.weight(1f) else Modifier)
        }
    }
}

@UiModePreviews
@Composable
private fun DeviceInfoActionButtonsPreview() {
    SettingsTheme {
        DeviceInfoActionButtons(
            modifier = Modifier.padding(SettingsDimension.itemPadding),
        )
    }
}
