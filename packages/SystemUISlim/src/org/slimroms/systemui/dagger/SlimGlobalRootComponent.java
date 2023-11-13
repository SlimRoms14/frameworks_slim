package org.slimroms.systemui.dagger;

import com.android.systemui.dagger.GlobalModule;
import com.android.systemui.dagger.GlobalRootComponent;

import javax.inject.Singleton;

import dagger.Component;

@Singleton
@Component(modules = {GlobalModule.class})
public interface SlimGlobalRootComponent extends GlobalRootComponent {

    /**
     * Builder for SlimGlobalRootComponent
     */
    @Component.Builder
    interface Builder extends GlobalRootComponent.Builder {
        SlimGlobalRootComponent build();
    }

    @Override
    SysUIComponentSlim.Builder getSysUIComponent();
}