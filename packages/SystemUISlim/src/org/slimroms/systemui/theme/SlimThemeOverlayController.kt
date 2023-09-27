package org.slimroms.systemui.theme

import android.app.WallpaperManager
import android.content.Context
import android.content.om.FabricatedOverlay
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Handler
import android.os.UserManager
import android.util.TypedValue
import androidx.core.graphics.ColorUtils
import androidx.core.graphics.toColorInt
import com.android.systemui.R
import com.android.systemui.broadcast.BroadcastDispatcher
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.dagger.qualifiers.Main
import com.android.systemui.dump.DumpManager
import com.android.systemui.flags.FeatureFlags
import com.android.systemui.keyguard.WakefulnessLifecycle
import com.android.systemui.monet.Style
import com.android.systemui.settings.UserTracker
import com.android.systemui.statusbar.policy.ConfigurationController
import com.android.systemui.statusbar.policy.ConfigurationController.ConfigurationListener
import com.android.systemui.statusbar.policy.DeviceProvisionedController
import com.android.systemui.theme.ThemeOverlayApplier
import com.android.systemui.theme.ThemeOverlayController
import com.android.systemui.util.settings.SecureSettings
import org.slimroms.systemui.monet.SlimColorScheme
import java.util.concurrent.Executor
import javax.inject.Inject

class SlimThemeOverlayController @Inject constructor(
    private val context: Context,
    broadcastDispatcher: BroadcastDispatcher,
    @Background bgHandler: Handler,
    @Main mainExecutor: Executor,
    @Background bgExecutor: Executor,
    themeOverlayApplier: ThemeOverlayApplier,
    secureSettings: SecureSettings,
    wallpaperManager: WallpaperManager,
    userManager: UserManager,
    deviceProvisionedController: DeviceProvisionedController,
    userTracker: UserTracker,
    dumpManager: DumpManager,
    featureFlags: FeatureFlags,
    @Main resources: Resources,
    wakefulnessLifecycle: WakefulnessLifecycle,
    private val configurationController: ConfigurationController
) : ThemeOverlayController(
    context,
    broadcastDispatcher,
    bgHandler,
    mainExecutor,
    bgExecutor,
    themeOverlayApplier,
    secureSettings,
    wallpaperManager,
    userManager,
    deviceProvisionedController,
    userTracker,
    dumpManager,
    featureFlags,
    resources,
    wakefulnessLifecycle
) {

    private val configurationListener = object : ConfigurationListener {
        override fun onUiModeChanged() {
            reevaluateSystemTheme(true)
        }
    }

    override fun start() {
        super.start()
        configurationController.addCallback(configurationListener)
    }

    override fun getOverlay(primaryColor: Int, type: Int, style: Style): FabricatedOverlay {
        val nightMode = (context.resources.configuration.uiMode
                and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES

        val defaultAccentColor = context.getColor(R.color.default_color_accent)
        val defaultNeutralColor = context.getColor(R.color.default_color_neutral)

        val colorScheme = SlimColorScheme(defaultAccentColor,
            defaultNeutralColor, nightMode, style)
        val colorShades = if (type == ACCENT) {
            colorScheme.allAccentColors
        } else {
            colorScheme.allNeutralColors
        }

        val name = if (type == ACCENT) "accent" else "neutral"

        val paletteSize = colorScheme.accent1.size

        val overlay = FabricatedOverlay.Builder(
            "com.android.systemui", name, "android")

        for (i in colorShades.indices) {
            val luminosity = i % paletteSize
            val paletteIndex = i / paletteSize + 1
            val resourceName = when (luminosity) {
                0 -> "android:color/system_$name${paletteIndex}_10"
                1 -> "android:color/system_$name${paletteIndex}_50"
                else -> "android:color/system_$name${paletteIndex}_${luminosity - 1}00"
            }
            overlay.setResourceValue(resourceName, TypedValue.TYPE_INT_COLOR_ARGB8,
                ColorUtils.setAlphaComponent(colorShades[i], 0xFF))
        }

        return overlay.build()
    }
}