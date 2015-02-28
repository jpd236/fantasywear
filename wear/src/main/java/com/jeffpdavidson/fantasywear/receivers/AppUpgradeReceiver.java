package com.jeffpdavidson.fantasywear.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jeffpdavidson.fantasywear.log.FWLog;
import com.jeffpdavidson.fantasywear.services.RequestSyncService;

/** Receiver to request a manual sync whenever this app is upgraded, to restore notifications. */
public class AppUpgradeReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_MY_PACKAGE_REPLACED.equals(intent.getAction())) {
            FWLog.i("Requesting manual sync due to package replacement");
            RequestSyncService.start(context);
        }
    }
}
