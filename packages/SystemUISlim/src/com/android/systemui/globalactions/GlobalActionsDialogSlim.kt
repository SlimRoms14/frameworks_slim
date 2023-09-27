package com.android.systemui.globalactions

import android.annotation.SuppressLint
import android.app.IActivityManager
import android.app.admin.DevicePolicyManager
import android.app.trust.TrustManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.media.AudioManager
import android.os.Handler
import android.os.UserManager
import android.service.dreams.IDreamManager
import android.telecom.TelecomManager
import android.util.Log
import android.view.IWindowManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.TextView
import com.android.internal.logging.MetricsLogger
import com.android.internal.logging.UiEventLogger
import com.android.internal.statusbar.IStatusBarService
import com.android.internal.widget.LockPatternUtils
import com.android.keyguard.KeyguardUpdateMonitor
import com.android.systemui.R
import com.android.systemui.animation.DialogLaunchAnimator
import com.android.systemui.animation.Expandable
import com.android.systemui.broadcast.BroadcastDispatcher
import com.android.systemui.colorextraction.SysuiColorExtractor
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.plugins.GlobalActions.GlobalActionsManager
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.NotificationShadeWindowController
import com.android.systemui.statusbar.VibratorHelper
import com.android.systemui.statusbar.phone.CentralSurfaces
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.statusbar.policy.KeyguardStateController
import com.android.systemui.telephony.TelephonyListenerManager
import com.android.systemui.util.RingerModeTracker
import com.android.systemui.util.settings.GlobalSettings
import com.android.systemui.util.settings.SecureSettings
import slim.action.ActionsManager
import java.util.Optional
import java.util.concurrent.Executor
import javax.inject.Inject


internal class GlobalActionsDialogSlim @Inject constructor(
    context: Context,
    private val windowManagerFuncs: GlobalActionsManager,
    audioManager: AudioManager,
    iDreamManager: IDreamManager,
    devicePolicyManager: DevicePolicyManager,
    private val lockPatternUtils: LockPatternUtils,
    broadcastDispatcher: BroadcastDispatcher,
    telephonyListenerManager: TelephonyListenerManager,
    globalSettings: GlobalSettings,
    secureSettings: SecureSettings,
    vibrator: VibratorHelper,
    @Main resources: Resources,
    configurationController: ConfigurationController,
    userTracker: UserTracker,
    keyguardStateController: KeyguardStateController,
    private val userManager: UserManager,
    trustManager: TrustManager,
    iActivityManager: IActivityManager,
    telecomManager: TelecomManager?,
    metricsLogger: MetricsLogger,
    private val colorExtractor: SysuiColorExtractor,
    private val statusBarService: IStatusBarService,
    notificationShadeWindowController: NotificationShadeWindowController,
    iWindowManager: IWindowManager,
    @Background backgroundExecutor: Executor,
    private val uiEventLogger: UiEventLogger,
    ringerModeTracker: RingerModeTracker,
    @Main handler: Handler,
    packageManager: PackageManager,
    centralSurfacesOptional: Optional<CentralSurfaces>,
    keyguardUpdateMonitor: KeyguardUpdateMonitor,
    private val dialogLaunchAnimator: DialogLaunchAnimator,
    private val actionsManager: ActionsManager
): GlobalActionsDialogLite(
    context,
    windowManagerFuncs,
    audioManager,
    iDreamManager,
    devicePolicyManager,
    lockPatternUtils,
    broadcastDispatcher,
    telephonyListenerManager,
    globalSettings,
    secureSettings,
    vibrator,
    resources,
    configurationController,
    userTracker,
    keyguardStateController,
    userManager,
    trustManager,
    iActivityManager,
    telecomManager,
    metricsLogger,
    colorExtractor,
    statusBarService,
    notificationShadeWindowController,
    iWindowManager,
    backgroundExecutor,
    uiEventLogger,
    ringerModeTracker,
    handler,
    packageManager,
    centralSurfacesOptional,
    keyguardUpdateMonitor,
    dialogLaunchAnimator
) {

    private var dialog: SlimActionsDialog? = null
    private val adapter = SlimAdapter()
    private val powerOptionsAdapter = SlimPowerOptionsAdapter()

    private var keyguardShowing = false

    private val restartItems = arrayListOf<Action>()

    private val restartOptionsAdapter = RestartOptionsAdapter()

    private fun getRestartActions(): Array<String> {
        return mResources.getStringArray(R.array.config_restartActionsList)
    }

    @SuppressLint("VisibleForTests")
    override fun shouldShowAction(action: Action): Boolean {
        return super.shouldShowAction(action)
    }

    private fun addIfShouldShowAction(actions: ArrayList<Action>, action: Action) {
        if (shouldShowAction(action)) {
            actions.add(action)
        }
    }

    override fun showOrHideDialog(keyguardShowing: Boolean, isDeviceProvisioned: Boolean, expandable: Expandable?) {
        this.keyguardShowing = keyguardShowing
        super.showOrHideDialog(keyguardShowing, isDeviceProvisioned, expandable)
    }

    override fun createDialog(): ActionsDialogLite {
        initDialogItems()
        return SlimActionsDialog(
            context = context,
            themeRes = R.style.Theme_SystemUI_Dialog_GlobalActionsLite,
            adapter = adapter,
            overflowAdapter = mOverflowAdapter,
            sysuiColorExtractor = colorExtractor,
            statusBarService = statusBarService,
            notificationShadeWindowController = mNotificationShadeWindowController,
            onRefreshCallback = this::onRefresh,
            keyguardShowing = keyguardShowing,
            powerAdapter = powerOptionsAdapter,
            restartOptionsAdapter = restartOptionsAdapter,
            uiEventLogger = uiEventLogger,
            centralSurfacesOptional = centralSurfaces,
            keyguardUpdateMonitor = keyguardUpdateMonitor,
            lockPatternUtils = lockPatternUtils
        ).also { dialog ->
            dialog.setOnDismissListener(this)
            dialog.setOnShowListener(this)
            this.dialog = dialog
        }
    }

    @SuppressLint("VisibleForTests")
    override fun createActionItems() {
        super.createActionItems()

        restartItems.clear()

        val restartActions = getRestartActions()

        val addedRestartKeys = arrayListOf<String>()

        val restartAction = RestartAction("advanced")
        val restartSystemAction = RestartAction("system")
        val restartRecoveryAction = RestartAction("recovery")
        val restartBootloaderAction = RestartAction("bootloader")
        val restartDownloadAction = RestartAction("download")
        val restartFastbootAction = RestartAction("fastboot")

        var restartIndex = -1
        for (item in mItems) {
            if (item.messageResId ==
                    com.android.internal.R.string.global_action_restart) {
                restartIndex = mItems.indexOf(item)
            }
        }

        mItems.removeAt(restartIndex)
        mItems.add(restartAction)

        for (action in restartActions) {
            if (addedRestartKeys.contains(action)) {
                continue
            }
            if (RESTART_ACTION_KEY_RESTART == action) {
                addIfShouldShowAction(restartItems, restartSystemAction)
            } else if (RESTART_ACTION_KEY_RESTART_RECOVERY == action) {
                addIfShouldShowAction(restartItems, restartRecoveryAction)
            } else if (RESTART_ACTION_KEY_RESTART_BOOTLOADER == action) {
                addIfShouldShowAction(restartItems, restartBootloaderAction)
            } else if (RESTART_ACTION_KEY_RESTART_DOWNLOAD == action) {
                addIfShouldShowAction(restartItems, restartDownloadAction)
            } else if (RESTART_ACTION_KEY_RESTART_FASTBOOT == action) {
                addIfShouldShowAction(restartItems, restartFastbootAction)
            }
            addedRestartKeys.add(action)
        }
    }

    inner class RestartAction(private val action: String): SinglePressAction(
        when (action) {
            "recovery" -> R.drawable.ic_lock_restart_recovery
            "bootloader" -> R.drawable.ic_lock_restart_bootloader
            "fastboot" -> R.drawable.ic_lock_restart_fastboot
            "download" -> R.drawable.ic_lock_restart_bootloader
            else -> com.android.internal.R.drawable.ic_restart
        },
        when (action) {
            "advanced" -> R.string.global_action_restart_more
            "system" -> R.string.global_action_restart_system
            "recovery" -> R.string.global_action_restart_recovery
            "bootloader" -> R.string.global_action_restart_bootloader
            "fastboot" -> R.string.global_action_restart_fastboot
            "download" -> R.string.global_action_restart_download
            else -> com.android.internal.R.string.global_action_restart
        }
    ), LongPressAction {
        @SuppressLint("VisibleForTests")
        override fun onPress() {
            Log.d("TEST", "RestartAction-onPress - $action")
            uiEventLogger.log(GlobalActionsEvent.GA_REBOOT_PRESS)
            when (action) {
                "advanced" -> {
                    dialog?.showRestartOptionsMenu()
                }
                "system" -> {
                    windowManagerFuncs.reboot(false)
                }
                else -> {
                    actionsManager.reboot(action)
                }
            }
        }

        override fun showDuringKeyguard(): Boolean {
            return true;
        }

        override fun showBeforeProvisioning(): Boolean {
            return true;
        }

        @SuppressLint("VisibleForTests")
        override fun onLongPress(): Boolean {
            uiEventLogger.log(GlobalActionsEvent.GA_REBOOT_LONG_PRESS)
            if (!userManager.hasUserRestriction(UserManager.DISALLOW_SAFE_BOOT)) {
                windowManagerFuncs.reboot(true)
                return true
            }
            return false
        }
    }

    /**
     * An action that also supports long press.
     */
    private interface LongPressAction : Action {
        fun onLongPress(): Boolean
    }


    /**
     * A single press action maintains no state, just responds to a press and takes an action.
     */
    abstract inner class SinglePressAction : Action {
        private val mIconResId: Int
        private val mIcon: Drawable?
        private val mMessageResId: Int
        private val mMessage: CharSequence?

        protected constructor(iconResId: Int, messageResId: Int) {
            mIconResId = iconResId
            mMessageResId = messageResId
            mMessage = null
            mIcon = null
        }

        protected constructor(iconResId: Int, icon: Drawable?, message: CharSequence) {
            mIconResId = iconResId
            mMessageResId = 0
            mMessage = message
            mIcon = icon
        }

        override fun isEnabled(): Boolean {
            return true
        }

        val status: String?
            get() = null

        abstract override fun onPress()
        override fun getLabelForAccessibility(context: Context): CharSequence {
            return mMessage ?: context.getString(mMessageResId)
        }

        override fun getMessageResId(): Int {
            return mMessageResId
        }

        override fun getMessage(): CharSequence? {
            return mMessage
        }

        @SuppressLint("UseCompatLoadingForDrawables")
        override fun getIcon(context: Context): Drawable {
            return mIcon ?: context.getDrawable(mIconResId)!!
        }

        override fun create(
            context: Context, convertView: View, parent: ViewGroup, inflater: LayoutInflater
        ): View {
            val v: View = inflater.inflate(gridItemLayoutResource, parent, false /* attach */)
            // ConstraintLayout flow needs an ID to reference
            v.id = View.generateViewId()
            val icon = v.findViewById<ImageView>(com.android.internal.R.id.icon)
            val messageView = v.findViewById<TextView>(com.android.internal.R.id.message)
            messageView.isSelected = true // necessary for marquee to work
            icon.setImageDrawable(getIcon(context))
            icon.scaleType = ScaleType.CENTER_CROP
            if (mMessage != null) {
                messageView.text = mMessage
            } else {
                messageView.setText(mMessageResId)
            }
            return v
        }
    }

    inner class SlimAdapter: MyAdapter() {
        override fun onClickItem(position: Int) {
            val item = getItem(position)
            if (item is RestartAction) {
                item.onPress()
            } else {
                super.onClickItem(position)
            }
        }
    }

    open inner class SlimPowerOptionsAdapter: MyPowerOptionsAdapter() {
        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View? {
            val action = getItem(position) ?: return null
            val viewLayoutResource: Int = R.layout.global_actions_grid_item_lite
            val view = convertView?:
                    LayoutInflater.from(context).inflate(viewLayoutResource, parent, false)

            view.setOnClickListener { onClickItem(position) }
            if (action is LongPressAction) {
                view.setOnLongClickListener { onLongClickItem(position) }
            }

            val icon = view.findViewById<ImageView>(com.android.internal.R.id.icon)
            val message = view.findViewById<TextView>(com.android.internal.R.id.message)

            message.isSelected = true

            icon.setImageDrawable(action.getIcon(context))
            icon.scaleType = ScaleType.CENTER_CROP

            if (action.message != null) {
                message.text = action.message
            } else {
                message.setText(action.messageResId)
            }

            return view
        }

        private fun onLongClickItem(position: Int): Boolean {
            val action = getItem(position)
            if (action is LongPressAction) {
                dialogLaunchAnimator.disableAllCurrentDialogsExitAnimations()
                dialog?.dismiss()
                action.onLongPress()
                return true
            }
            return false
        }

        private fun onClickItem(position: Int) {
            val action = getItem(position)
            if (action !is RestartAction) {
                dialogLaunchAnimator.disableAllCurrentDialogsExitAnimations()
                dialog?.dismiss()
            }
            action.onPress()
        }
    }

    inner class RestartOptionsAdapter: SlimPowerOptionsAdapter() {
        override fun getCount() = restartItems.size
        override fun getItem(position: Int) = restartItems[position]
    }

    companion object {
        private const val RESTART_ACTION_KEY_RESTART = "restart"
        private const val RESTART_ACTION_KEY_RESTART_RECOVERY = "restart_recovery"
        private const val RESTART_ACTION_KEY_RESTART_BOOTLOADER = "restart_bootloader"
        private const val RESTART_ACTION_KEY_RESTART_DOWNLOAD = "restart_download"
        private const val RESTART_ACTION_KEY_RESTART_FASTBOOT = "restart_fastboot"
    }
}