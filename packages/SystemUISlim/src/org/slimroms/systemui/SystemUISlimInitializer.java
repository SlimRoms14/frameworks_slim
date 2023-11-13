package org.slimroms.systemui;

import android.content.Context;

import com.android.systemui.SystemUIInitializer;
import com.android.systemui.dagger.DaggerReferenceGlobalRootComponent;
import com.android.systemui.dagger.GlobalRootComponent;

public class SystemUISlimInitializer extends SystemUIInitializer {
    
    public SystemUISlimInitializer(Context context) {
        super(context);
    }

    @Override
    protected GlobalRootComponent.Builder getGlobalRootComponentBuilder() {
        return DaggerReferenceGlobalRootComponent.builder();
    }
}