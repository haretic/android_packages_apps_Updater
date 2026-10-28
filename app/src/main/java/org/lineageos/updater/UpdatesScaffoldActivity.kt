/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

package org.lineageos.updater

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.settingslib.spa.framework.compose.LocalNavController
import com.android.settingslib.spa.framework.compose.NavControllerWrapper
import com.android.settingslib.spa.framework.theme.SettingsTheme
import com.android.settingslib.spa.widget.preference.Preference
import com.android.settingslib.spa.widget.preference.PreferenceModel
import com.android.settingslib.spa.widget.scaffold.SettingsScaffold
import com.android.settingslib.spa.widget.ui.Category
import org.lineageos.updater.controller.UpdaterController
import org.lineageos.updater.data.Update
import org.lineageos.updater.data.UpdateStatus
import org.lineageos.updater.deviceinfo.DeviceInfoBanner
import org.lineageos.updater.preferences.PreferencesActivity
import org.lineageos.updater.updates.UpdateList
import org.lineageos.updater.updates.action.AlertDialogState
import org.lineageos.updater.updates.action.UpdateActionDialog
import org.lineageos.updater.updates.action.UpdateActionHandler
import org.lineageos.updater.updates.state.UpdateItemStateMapper
import org.lineageos.updater.updatescheck.UpdatesCheck
import org.lineageos.updater.updatescheck.UpdatesCheckModel
import org.lineageos.updater.updatescheck.rememberUpdatesCheckUiState

abstract class UpdatesScaffoldActivity : ComponentActivity() {
    private val viewModel by viewModels<UpdatesViewModel>()
    private var activeUpdaterController: UpdaterController? by mutableStateOf(null)
    private var controllerStateVersion: Int by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
    }

    protected fun setupCompose() {
        setContent {
            val navController = remember {
                object : NavControllerWrapper {
                    override fun navigate(route: String, popUpCurrent: Boolean) {}
                    override fun navigateBack() = finish()
                }
            }

            CompositionLocalProvider(LocalNavController provides navController) {
                SettingsTheme {
                    val uiState by viewModel.uiState.collectAsState()
                    UpdatesScaffoldContent(
                        uiState = uiState,
                        updaterController = activeUpdaterController,
                        controllerStateVersion = controllerStateVersion,
                        onRefreshClick = { onRefreshClick() },
                        onLocalUpdateClick = { onLocalUpdateClick() },
                        onPreferencesClick = {
                            startActivity(
                                Intent(
                                    this@UpdatesScaffoldActivity,
                                    PreferencesActivity::class.java,
                                )
                            )
                        },
                        onControllerStateChanged = { notifyControllerStateChanged() },
                    )
                }
            }
        }
    }

    protected fun setUpdaterController(controller: UpdaterController?) {
        activeUpdaterController = controller
        notifyControllerStateChanged()
    }

    protected fun notifyControllerStateChanged() {
        controllerStateVersion++
    }

    open fun onRefreshClick() {}
    open fun onLocalUpdateClick() {}
    open fun exportUpdate(update: Update) {}
}

@Composable
private fun UpdatesScaffoldContent(
    uiState: UpdatesViewModel.UiState,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    onControllerStateChanged: () -> Unit,
) {
    val title = getTitleForUpdateStatus(uiState.updates)
    val compactLandscape = isCompactLandscape()
    val focusManager = LocalFocusManager.current
    val wheelFocusModifier = if (compactLandscape) {
        // The wheel emits D-pad up/down. Follow composition order across both panes,
        // including the toolbar, rather than jumping to the closest spatial target.
        Modifier.onPreviewKeyEvent { event ->
            if (event.type != KeyEventType.KeyDown) {
                false
            } else {
                when (event.key) {
                    Key.DirectionDown -> focusManager.moveFocus(FocusDirection.Next)
                    Key.DirectionUp -> focusManager.moveFocus(FocusDirection.Previous)
                    else -> false
                }
            }
        }
    } else {
        Modifier
    }

    val actionDialogState = remember { mutableStateOf<AlertDialogState?>(null) }
    actionDialogState.value?.let { dialog ->
        UpdateActionDialog(
            dialog = dialog,
            onDismiss = { actionDialogState.value = null },
        )
    }

    Box(modifier = wheelFocusModifier) {
        SettingsScaffold(title = title) { paddingValues ->
            if (isWideScreen()) {
                WideUpdatesScaffold(
                    compactLandscape = compactLandscape,
                    paddingValues = paddingValues,
                    updatesCheckModel = uiState.updatesCheckModel,
                    updates = uiState.updates,
                    updaterController = updaterController,
                    controllerStateVersion = controllerStateVersion,
                    showDialog = { actionDialogState.value = it },
                    onControllerStateChanged = onControllerStateChanged,
                    onRefreshClick = onRefreshClick,
                    onLocalUpdateClick = onLocalUpdateClick,
                    onPreferencesClick = onPreferencesClick,
                )
            } else {
                UpdatesScaffold(
                    paddingValues = paddingValues,
                    updatesCheckModel = uiState.updatesCheckModel,
                    updates = uiState.updates,
                    updaterController = updaterController,
                    controllerStateVersion = controllerStateVersion,
                    showDialog = { actionDialogState.value = it },
                    onControllerStateChanged = onControllerStateChanged,
                    onRefreshClick = onRefreshClick,
                    onLocalUpdateClick = onLocalUpdateClick,
                    onPreferencesClick = onPreferencesClick,
                )
            }
        }
    }
}

@Composable
private fun WideUpdatesScaffold(
    compactLandscape: Boolean,
    paddingValues: PaddingValues,
    updatesCheckModel: UpdatesCheckModel,
    updates: List<Update>,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    showDialog: (AlertDialogState) -> Unit,
    onControllerStateChanged: () -> Unit,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
) {
    val layoutDirection = LocalLayoutDirection.current

    Row(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = paddingValues.calculateTopPadding(),
                start = paddingValues.calculateStartPadding(layoutDirection),
                end = paddingValues.calculateEndPadding(layoutDirection),
            )
    ) {
        UpdatesInformationPane(
            compactLandscape = compactLandscape,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(if (compactLandscape) Modifier.focusGroup() else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(bottom = paddingValues.calculateBottomPadding()),
        )
        UpdatesActionPane(
            model = updatesCheckModel,
            updates = updates,
            updaterController = updaterController,
            controllerStateVersion = controllerStateVersion,
            showDialog = showDialog,
            onControllerStateChanged = onControllerStateChanged,
            onRefreshClick = onRefreshClick,
            onLocalUpdateClick = onLocalUpdateClick,
            onPreferencesClick = onPreferencesClick,
            compactLandscape = compactLandscape,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .then(if (compactLandscape) Modifier.focusGroup() else Modifier)
                .verticalScroll(rememberScrollState())
                .padding(bottom = paddingValues.calculateBottomPadding()),
        )
    }
}

@Composable
private fun UpdatesScaffold(
    paddingValues: PaddingValues,
    updatesCheckModel: UpdatesCheckModel,
    updates: List<Update>,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    showDialog: (AlertDialogState) -> Unit,
    onControllerStateChanged: () -> Unit,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(paddingValues)
            .verticalScroll(rememberScrollState())
    ) {
        UpdatesInformationPane()
        UpdatesActionPane(
            model = updatesCheckModel,
            updates = updates,
            updaterController = updaterController,
            controllerStateVersion = controllerStateVersion,
            showDialog = showDialog,
            onControllerStateChanged = onControllerStateChanged,
            onRefreshClick = onRefreshClick,
            onLocalUpdateClick = onLocalUpdateClick,
            onPreferencesClick = onPreferencesClick,
        )
    }
}

@Composable
private fun UpdatesInformationPane(
    modifier: Modifier = Modifier,
    compactLandscape: Boolean = false,
) {
    DeviceInfoBanner(modifier = modifier, compactLandscape = compactLandscape)
}

@Composable
private fun UpdatesActionPane(
    model: UpdatesCheckModel,
    updates: List<Update>,
    updaterController: UpdaterController?,
    controllerStateVersion: Int,
    showDialog: (AlertDialogState) -> Unit,
    onControllerStateChanged: () -> Unit,
    onRefreshClick: () -> Unit,
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    modifier: Modifier = Modifier,
    compactLandscape: Boolean = false,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val activity = context as UpdatesScaffoldActivity
    val networkMonitor =
        remember { (context.applicationContext as UpdaterApplication).networkMonitor }
    val networkState by networkMonitor.networkState.collectAsState(
        initial = networkMonitor.currentNetworkState,
    )
    val userPreferencesRepository =
        remember { (context.applicationContext as UpdaterApplication).userPreferencesRepository }
    val streamUpdatesEnabled by userPreferencesRepository.streamUpdatesFlow.collectAsState(
        initial = true,
    )

    val updateItems = remember(
        updates,
        updaterController,
        networkState,
        streamUpdatesEnabled,
        controllerStateVersion,
    ) {
        val controller = updaterController ?: return@remember emptyList()
        val mapper = UpdateItemStateMapper(context, controller, streamUpdatesEnabled)
        updates.mapNotNull { update ->
            controller.getUpdate(update.downloadId)?.let {
                mapper.map(it, networkState)
            }
        }
    }

    val actionHandler = remember(updaterController) {
        updaterController?.let { controller ->
            UpdateActionHandler(
                activity = activity,
                updaterController = controller,
                exportUpdate = { update -> activity.exportUpdate(update) },
                showDialog = showDialog,
            )
        }
    }
    val updatesCheckUiState = rememberUpdatesCheckUiState(model.state)

    Column(modifier = modifier) {
        UpdatesCheck(
            model = model,
            uiState = updatesCheckUiState,
            onCheckClick = onRefreshClick,
        )
        UpdateList(
            items = updateItems,
            compactLandscape = compactLandscape,
            isUpdatesCheckStatusVisible = updatesCheckUiState.isStatusVisible,
            onAction = { action, downloadId ->
                val controller = updaterController ?: return@UpdateList
                val update = controller.getUpdate(downloadId) ?: return@UpdateList
                actionHandler?.perform(action, update)
                onControllerStateChanged()
            },
        )
        UpdatesFooter(
            onLocalUpdateClick = onLocalUpdateClick,
            onPreferencesClick = onPreferencesClick,
            compactLandscape = compactLandscape,
        )
    }
}

@Composable
private fun isWideScreen(): Boolean {
    val minWideScreenWidth = 600.dp
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize

    // A short landscape window also gets the panes side by side, the status beside the banner
    val minLandscapeWidth = 480.dp
    return with(density) {
        val width = windowSize.width.toDp()
        width >= minWideScreenWidth ||
                (windowSize.width > windowSize.height && width >= minLandscapeWidth)
    }
}

@Composable
private fun isCompactLandscape(): Boolean {
    val density = LocalDensity.current
    val windowSize = LocalWindowInfo.current.containerSize

    // Only the extra landscape case introduced by isWideScreen(): keep the
    // existing >=600dp tablet layout and all portrait windows unchanged.
    return with(density) {
        val width = windowSize.width.toDp()
        windowSize.width > windowSize.height && width >= 480.dp && width < 600.dp
    }
}

@Composable
private fun UpdatesFooter(
    onLocalUpdateClick: () -> Unit,
    onPreferencesClick: () -> Unit,
    compactLandscape: Boolean = false,
) {
    if (compactLandscape) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(top = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            FilledTonalButton(
                onClick = onLocalUpdateClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                colors = ButtonDefaults.filledTonalButtonColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                ),
            ) {
                Text(text = stringResource(R.string.local_update_import))
            }
            TextButton(
                onClick = onPreferencesClick,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
            ) {
                Text(text = stringResource(R.string.menu_preferences))
            }
        }
        return
    }

    val localUpdateSummary = stringResource(R.string.local_update_import_summary)
    val preferencesSummary = stringResource(R.string.preferences_summary)

    Category {
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.local_update_import)
            override val summary = { localUpdateSummary }
            override val onClick = onLocalUpdateClick
        })
        Preference(object : PreferenceModel {
            override val title = stringResource(R.string.menu_preferences)
            override val summary = { preferencesSummary }
            override val onClick = onPreferencesClick
        })
    }
}

@Composable
private fun getTitleForUpdateStatus(updates: List<Update>): String = when {
    updates.any { it.status == UpdateStatus.UPDATED_NEED_REBOOT } ->
        stringResource(R.string.installing_update_finished)

    updates.any { it.status == UpdateStatus.INSTALLATION_FAILED } ->
        stringResource(R.string.installing_update_error)

    updates.any {
        it.status == UpdateStatus.INSTALLING ||
                it.status == UpdateStatus.INSTALLATION_SUSPENDED
    } -> stringResource(R.string.installing_update)

    else -> stringResource(R.string.display_name)
}
