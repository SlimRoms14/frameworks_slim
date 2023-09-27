package org.slimroms.systemui.statusbar

import android.content.Intent
import android.os.Bundle
import com.android.systemui.statusbar.CommandQueue
import com.android.systemui.statusbar.phone.CentralSurfaces
import org.slim.internal.statusbar.ISlimStatusBar

class SlimCommandQueue(
    private val centralSurfaces: CentralSurfaces,
    private val commandQueue: CommandQueue
): ISlimStatusBar.Stub() {
    override fun showCustomIntentAfterKeyguard(intent: Intent?) {
        centralSurfaces.startActivityDismissingKeyguard(intent, false, false)
    }

    override fun toggleScreenshot() {
        TODO("Not yet implemented")
    }

    override fun toggleLastApp() {
        TODO("Not yet implemented")
    }

    override fun toggleKillApp() {
        TODO("Not yet implemented")
    }

    override fun startAssist(bundle: Bundle?) {
        commandQueue.startAssist(bundle)
    }

    override fun toggleSplitScreen() {
        commandQueue.toggleSplitScreen()
    }

    override fun toggleRecentApps() {
        commandQueue.toggleRecentApps()
    }

    override fun preloadRecentApps() {
        commandQueue.preloadRecentApps()
    }

    override fun cancelPreloadRecentApps() {
        commandQueue.cancelPreloadRecentApps()
    }
}