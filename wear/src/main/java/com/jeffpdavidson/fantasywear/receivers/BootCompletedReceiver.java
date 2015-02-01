package com.jeffpdavidson.fantasywear.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.jeffpdavidson.fantasywear.log.FWLog;
import com.jeffpdavidson.fantasywear.services.RequestSyncService;

/** Receiver to request a manual sync on boot, to restore notifications. */
public class BootCompletedReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            FWLog.i("Requesting manual sync due to device reboot");
            RequestSyncService.start(context);
        }
    }
}
