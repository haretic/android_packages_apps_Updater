/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.deviceinfo

import android.content.res.Configuration
import android.icu.text.DateFormat
import android.icu.util.TimeZone
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.booleanResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.debug.UiModePreviews
import com.android.settingslib.spa.framework.theme.SettingsDimension
import com.android.settingslib.spa.framework.theme.SettingsRadius
import com.android.settingslib.spa.framework.theme.SettingsShape.CornerExtraLarge1
import com.android.settingslib.spa.framework.theme.SettingsTheme
import org.lineageos.updater.R
import org.lineageos.updater.deviceinfo.actions.DeviceInfoActionButtons
import org.lineageos.updater.deviceinfo.actions.DeviceInfoTvAction
import org.lineageos.updater.util.StringUtil
import java.util.Date

@Composable
fun DeviceInfoBanner(
    modifier: Modifier = Modifier,
    compactLandscape: Boolean = false,
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val locale = remember(context, configuration.locales) { StringUtil.getCurrentLocale(context) }
    val buildVersion = remember { DeviceInfoUtils.buildVersion }
    val androidVersion = remember { DeviceInfoUtils.androidVersion }
    val buildDate = remember(locale) {
        DateFormat.getInstanceForSkeleton("MMMd", locale)
            .apply { timeZone = TimeZone.getTimeZone("UTC") }
            .format(Date(DeviceInfoUtils.buildDateTimestamp * 1000L))
    }
    val securityPatch = remember(locale) {
        StringUtil.formatSecurityPatch(context, DeviceInfoUtils.buildSecurityPatch)
    }

    DeviceInfoBanner(
        buildVersion = buildVersion,
        androidVersion = androidVersion,
        buildDate = buildDate,
        securityPatch = securityPatch,
        modifier = modifier,
        compactLandscape = compactLandscape,
    )
}

@Composable
fun DeviceInfoBanner(
    buildVersion: String,
    androidVersion: String,
    buildDate: String,
    securityPatch: String,
    modifier: Modifier = Modifier,
    compactLandscape: Boolean = false,
) {
    val uiMode = LocalConfiguration.current.uiMode
    val isTv = (uiMode and Configuration.UI_MODE_TYPE_MASK) == Configuration.UI_MODE_TYPE_TELEVISION
    val compact = compactLandscape || booleanResource(R.bool.config_compactHeader)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                if (compact) {
                    PaddingValues(
                        start = SettingsDimension.itemPaddingStart,
                        top = 4.dp,
                        end = SettingsDimension.itemPaddingEnd,
                    )
                } else {
                    SettingsDimension.itemPadding
                }
            )
    ) {
        UpdaterCard(
            buildVersion = buildVersion,
            androidVersion = androidVersion,
            buildDate = buildDate,
            securityPatch = securityPatch,
            modifier = Modifier.fillMaxWidth(),
            compact = compact,
            compactLandscape = compactLandscape,
            shape = if (isTv) {
                CornerExtraLarge1
            } else {
                CornerExtraLarge1.copy(
                    bottomStart = CornerSize(SettingsRadius.extraSmall2),
                    bottomEnd = CornerSize(SettingsRadius.extraSmall2),
                )
            },
        )

        if (!isTv) {
            if (compactLandscape) {
                Spacer(modifier = Modifier.height(8.dp))
            } else if (!compact) {
                Spacer(modifier = Modifier.height(SettingsDimension.paddingTiny))
            }
            DeviceInfoActionButtons(compact = compact, stacked = compactLandscape)
        } else {
            DeviceInfoTvAction()
        }
    }
}

@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_NO
)
@Preview(
    showBackground = true,
    uiMode = Configuration.UI_MODE_TYPE_TELEVISION or Configuration.UI_MODE_NIGHT_YES
)
@UiModePreviews
@Preview(name = "Rabbit R1 portrait", widthDp = 413, heightDp = 551, showBackground = true)
@Composable
private fun DeviceInfoBannerPreview() {
    SettingsTheme {
        DeviceInfoBanner(
            buildVersion = "24.0",
            androidVersion = "17",
            buildDate = "28 Oct",
            securityPatch = "Oct 2026",
        )
    }
}

@Preview(name = "Rabbit R1 landscape pane", widthDp = 275, heightDp = 301, showBackground = true)
@Composable
private fun DeviceInfoBannerLandscapePreview() {
    SettingsTheme {
        DeviceInfoBanner(
            buildVersion = "24.0",
            androidVersion = "17",
            buildDate = "28 Oct",
            securityPatch = "Oct 2026",
            compactLandscape = true,
        )
    }
}
