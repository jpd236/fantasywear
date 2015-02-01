package com.jeffpdavidson.fantasywear.util;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.content.LocalBroadcastManager;

/** Utility class for sending local broadcasts for communication within this app. */
public final class LocalBroadcasts {
    private LocalBroadcasts() {}

    /** Action associated with {@link #sendAckBroadcast}. */
    public static final String ACTION_ACK = "ack";

    /** Send a broadcast to note that an attached wear device has acked an update to this URI. */
    public static void sendAckBroadcast(Context context, Uri uri) {
        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(ACTION_ACK, uri));
    }
}
