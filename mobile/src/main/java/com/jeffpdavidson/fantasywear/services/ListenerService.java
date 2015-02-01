package com.jeffpdavidson.fantasywear.services;

import android.net.Uri;

import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.WearableListenerService;
import com.jeffpdavidson.fantasywear.log.FWLog;
import com.jeffpdavidson.fantasywear.protocol.Paths;
import com.jeffpdavidson.fantasywear.sync.SyncAdapter;
import com.jeffpdavidson.fantasywear.util.LocalBroadcasts;

import java.io.UnsupportedEncodingException;

/**
 * {@link WearableListenerService} for the mobile device.
 *
 * Listens for {@link Paths#SYNC} requests sent from the wear device (e.g. due to reboot or app
 * upgrade, which cause the notification to disappear) and forces a sync to retrigger the
 * notification.
 */
public class ListenerService extends WearableListenerService {
    @Override
    public void onMessageReceived(MessageEvent message) {
        if (Paths.SYNC.equals(message.getPath())) {
            FWLog.i("Received a sync request; manually syncing all accounts");
            SyncAdapter.requestManualSync(this);
        } else if (Paths.ACK.equals(message.getPath())) {
            Uri uri;
            try {
                uri = Uri.parse(new String(message.getData(), "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                throw new IllegalStateException("UTF-8 must be a supported encoding", e);
            }
            FWLog.d("Received an ACK for URI %s", uri);
            LocalBroadcasts.sendAckBroadcast(this, uri);
        } else {
            FWLog.e("Unknown message path: %s", message.getPath());
        }
    }
}
