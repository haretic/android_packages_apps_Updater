/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater.deviceinfo

import android.content.Context
import android.os.Build
import android.os.SystemProperties
import java.io.File
import com.android.settingslib.DeviceInfoUtils as SettingsLibDeviceInfoUtils

object DeviceInfoUtils : SettingsLibDeviceInfoUtils() {

    private const val PROP_AB_DEVICE = "ro.build.ab_update"
    private const val PROP_ALLOW_MAJOR_UPGRADES = "lineage.updater.allow_major_upgrades"
    private const val PROP_BUILD_DATE = "ro.build.date.utc"
    private const val PROP_BUILD_VERSION = "ro.lineage.build.version"
    private const val PROP_BUILD_VERSION_INCREMENTAL = "ro.build.version.incremental"
    private const val PROP_DEVICE = "ro.lineage.device"
    private const val PROP_NEXT_DEVICE = "ro.updater.next_device"
    private const val PROP_RELEASE_TYPE = "ro.lineage.releasetype"
    private const val PROP_UPDATER_ALLOW_DOWNGRADING = "lineage.updater.allow_downgrading"
    private const val PROP_UPDATER_URI = "lineage.updater.uri"
    private const val PROP_UPDATE_RECOVERY = "persist.vendor.recovery_update"
    private const val PROP_HARETIC_VERSION = "ro.haretic.version"
    private const val PROP_HARETIC_STYLE = "ro.haretic.style"
    private const val PROP_HARETIC_GAPPS = "ro.haretic.gapps"
    private const val MAGISK_PACKAGE = "com.topjohnwu.magisk"

    // Read-only
    val androidVersion: String = Build.VERSION.RELEASE

    val buildSecurityPatch: String = Build.VERSION.SECURITY_PATCH

    val sdkLevel: Int = Build.VERSION.SDK_INT

    @JvmStatic
    val buildDateTimestamp: Long = SystemProperties.getLong(PROP_BUILD_DATE, 0)

    /** haretic-YYYYMMDD-HHMM-<channel>, the version the image is named by, empty on other builds. */
    @JvmStatic
    val hareticVersion: String = SystemProperties.get(PROP_HARETIC_VERSION, "")

    /** The version shown: haretic's "YYYYMMDD-HHMM channel", LineageOS's number on other builds. */
    @JvmStatic
    val buildVersion: String = displayVersion(hareticVersion)
        .ifEmpty { SystemProperties.get(PROP_BUILD_VERSION, "") }

    @JvmStatic
    fun displayVersion(version: String): String = version.removePrefix("haretic-").let {
        if ('-' in it) it.substringBeforeLast('-') + " " + it.substringAfterLast('-') else it
    }

    /** haretic, or aosp for the plain LineageOS build. */
    @JvmStatic
    val hareticStyle: String = SystemProperties.get(PROP_HARETIC_STYLE, "haretic")

    /**
     * The build's variant as its packages are named: r1_b, then v (vanilla) or g (Google apps),
     * then N (not rooted) or S (rooted with Magisk, which the build itself doesn't know of).
     */
    @JvmStatic
    fun hareticVariant(context: Context): String {
        val gapps = SystemProperties.getBoolean(PROP_HARETIC_GAPPS, false)
        val rooted = File("/system/bin/magisk").exists() || runCatching {
            context.packageManager.getPackageInfo(MAGISK_PACKAGE, 0)
        }.isSuccess
        return "r1_b" + (if (gapps) "g" else "v") + (if (rooted) "S" else "N")
    }

    @JvmStatic
    val buildVersionIncremental: String = SystemProperties.get(PROP_BUILD_VERSION_INCREMENTAL, "")

    @JvmStatic
    val device: String = SystemProperties.get(PROP_NEXT_DEVICE, SystemProperties.get(PROP_DEVICE))

    @JvmStatic
    val isABDevice: Boolean = SystemProperties.getBoolean(PROP_AB_DEVICE, false)

    /** The channel: haretic's nightly, beta or release, else LineageOS's release type. */
    @JvmStatic
    val releaseType: String = hareticVersion.substringAfterLast('-', "")
        .ifEmpty { SystemProperties.get(PROP_RELEASE_TYPE) }

    // Mutable at runtime
    @JvmStatic
    val isDowngradingAllowed: Boolean
        get() = SystemProperties.getBoolean(PROP_UPDATER_ALLOW_DOWNGRADING, false)

    @JvmStatic
    val isMajorUpdateAllowed: Boolean
        get() = SystemProperties.getBoolean(PROP_ALLOW_MAJOR_UPGRADES, false)

    @JvmStatic
    var isRecoveryUpdateEnabled: Boolean
        get() = SystemProperties.getBoolean(PROP_UPDATE_RECOVERY, false)
        set(value) = SystemProperties.set(PROP_UPDATE_RECOVERY, value.toString())

    @JvmStatic
    val updaterUri: String
        get() = SystemProperties.get(PROP_UPDATER_URI, "")
}
