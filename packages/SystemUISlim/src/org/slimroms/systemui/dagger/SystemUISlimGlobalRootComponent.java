package org.slimroms.systemui.dagger;

import com.android.systemui.dagger.GlobalModule;
import com.android.systemui.dagger.GlobalRootComponent;
import com.android.systemui.dagger.WMComponent;

import dagger.Component;

import javax.inject.Singleton;

@Singleton
@Component(modules = {
    GlobalModule.class
})
public interface SystemUISlimGlobalRootComponent extends GlobalRootComponent {
    @Component.Builder
    interface Builder extends GlobalRootComponent.Builder {
        SystemUISlimGlobalRootComponent build();
    }

    @Override
    WMComponent.Builder getWMComponentBuilder();

    @Override
    SystemUISlimComponent.Builder getSysUIComponent();
}