/*
 * Copyright (C) 2016-2017 The SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package slim.action;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;

import slim.content.ServiceConstants;
import slim.action.IActionsService;

import org.slim.internal.statusbar.ISlimStatusBar;

public class ActionsManager {

    private final Context mContext;

    private static ActionsManager sInstance;
    private static IActionsService sService;

    private ActionsManager(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext != null) {
            mContext = appContext;
        } else {
            mContext = context;
        }

        sService = getService();
    }

    public synchronized static ActionsManager getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new ActionsManager(context);
        }
        return sInstance;
    }

    private static IActionsService getService() {
        if (sService != null) {
            return sService;
        }
        IBinder b = ServiceManager.getService(ServiceConstants.ACTIONS_SERVICE);
        if (b != null) {
            sService = IActionsService.Stub.asInterface(b);
            return sService;
        }
        return null;
    }

    public void registerSlimStatusBar(ISlimStatusBar bar) {
        try {
            getService().registerSlimStatusBar(bar);
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void showCustomIntentAfterKeyguard(Intent intent) {
        try {
            getService().showCustomIntentAfterKeyguard(intent);
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void toggleScreenshot() {
        try {
            getService().toggleScreenshot();
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void toggleLastApp() {
        try {
            getService().toggleLastApp();
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void toggleKillApp() {
        try {
            getService().toggleKillApp();
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void toggleGlobalMenu() {
        try {
            getService().toggleGlobalMenu();
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void startAssist(Bundle bundle) {
        try {
            getService().startAssist(bundle);
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void toggleSplitScreen() {
        try {
            getService().toggleSplitScreen();
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void toggleRecentApps() {
        try {
            getService().toggleRecentApps();
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void preloadRecentApps() {
        try {
            getService().preloadRecentApps();
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void cancelPreloadRecentApps() {
        try {
            getService().cancelPreloadRecentApps();
        } catch (RemoteException e) {
            // ignore
        }
    }

    public void reboot(String reason) {
        try {
            getService().reboot(reason);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}
