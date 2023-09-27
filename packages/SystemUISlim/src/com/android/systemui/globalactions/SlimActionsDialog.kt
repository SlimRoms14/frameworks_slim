package com.android.systemui.globalactions

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import com.android.internal.logging.UiEventLogger
import com.android.internal.statusbar.IStatusBarService
import com.android.internal.widget.LockPatternUtils
import com.android.keyguard.KeyguardUpdateMonitor
import com.android.systemui.colorextraction.SysuiColorExtractor
import com.android.systemui.globalactions.GlobalActionsDialogLite.ActionsDialogLite
import com.android.systemui.globalactions.GlobalActionsDialogLite.MyAdapter
import com.android.systemui.globalactions.GlobalActionsDialogLite.MyOverflowAdapter
import com.android.systemui.globalactions.GlobalActionsDialogLite.MyPowerOptionsAdapter
import com.android.systemui.statusbar.NotificationShadeWindowController
import com.android.systemui.statusbar.phone.CentralSurfaces
import java.util.Optional

@SuppressLint("VisibleForTests")
internal class SlimActionsDialog(
    context: Context,
    themeRes: Int,
    adapter: MyAdapter,
    overflowAdapter: MyOverflowAdapter,
    sysuiColorExtractor: SysuiColorExtractor,
    statusBarService: IStatusBarService,
    notificationShadeWindowController: NotificationShadeWindowController,
    onRefreshCallback: Runnable,
    keyguardShowing: Boolean,
    powerAdapter: MyPowerOptionsAdapter,
    private val restartOptionsAdapter: GlobalActionsDialogSlim.RestartOptionsAdapter,
    uiEventLogger: UiEventLogger,
    centralSurfacesOptional: Optional<CentralSurfaces>,
    keyguardUpdateMonitor: KeyguardUpdateMonitor,
    lockPatternUtils: LockPatternUtils
): ActionsDialogLite(
    context,
    themeRes,
    adapter,
    overflowAdapter,
    sysuiColorExtractor,
    statusBarService,
    notificationShadeWindowController,
    onRefreshCallback,
    keyguardShowing,
    powerAdapter,
    uiEventLogger,
    centralSurfacesOptional,
    keyguardUpdateMonitor,
    lockPatternUtils
) {

    private var restartOptionsDialog: Dialog? = null
    private var powerOptionsDialog: Dialog? = null

    fun showRestartOptionsMenu() {
        restartOptionsDialog = SlimGlobalActionsPowerDialog.create(context, restartOptionsAdapter)
        restartOptionsDialog?.show()
    }

    override fun showPowerOptionsMenu() {
        powerOptionsDialog = SlimGlobalActionsPowerDialog.create(context, mPowerOptionsAdapter)
        powerOptionsDialog?.show()
    }

    override fun dismiss() {
        super.dismiss()
        dismissRestartOptions()
        dismissPowerOptionsDialog()
    }

    private fun dismissPowerOptionsDialog() {
        powerOptionsDialog?.dismiss()
    }

    private fun dismissRestartOptions() {
        restartOptionsDialog?.dismiss()
    }

    override fun refreshDialog() {
        super.refreshDialog()
        dismissRestartOptions()
        dismissPowerOptionsDialog()
    }
}