package org.slimroms.systemui.dagger;

import com.android.systemui.dagger.DefaultComponentBinder;
import com.android.systemui.dagger.DependencyProvider;
import com.android.systemui.dagger.SysUIComponent;
import com.android.systemui.dagger.SysUISingleton;
import com.android.systemui.keyguard.CustomizationProvider;
import com.android.systemui.statusbar.NotificationInsetsModule;
import com.android.systemui.statusbar.QsFrameTranslateModule;

import dagger.Subcomponent;

@SysUISingleton
@Subcomponent(
    modules = {
        DefaultComponentBinder.class,
        DependencyProvider.class,
        NotificationInsetsModule.class,
        QsFrameTranslateModule.class,
        SystemUISlimBinder.class,
        SystemUIModule.class,
        SystemUISlimCoreStartableModule.class,
        SystemUISlimModule.class
    }
)
interface SystemUISlimComponent extends SysUIComponent {
    @Subcomponent.Builder
    interface Builder extends SysUIComponent.Builder {
        SystemUISlimComponent build();
    }

    void inject(CustomizationProvider customizationProvider);
}