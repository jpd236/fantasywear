package com.jeffpdavidson.fantasywear.util;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi.SendMessageResult;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi.GetConnectedNodesResult;
import com.google.android.gms.wearable.Wearable;
import com.jeffpdavidson.fantasywear.log.FWLog;

import java.util.concurrent.TimeUnit;

/** Utility class for sending messages via the wearable MessageApi. */
public final class MessageApiUtil {
    private MessageApiUtil() {}

    /** Send a message to the given path with the given data to all connected nodes. */
    public static void sendMessage(GoogleApiClient googleApiClient, String path, byte[] data) {
        if (!googleApiClient.isConnected()) {
            ConnectionResult result = googleApiClient.blockingConnect(
                    Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (!result.isSuccess()) {
                FWLog.e("Failed to connect to GoogleApiClient");
                return;
            }
        }

        GetConnectedNodesResult result = Wearable.NodeApi.getConnectedNodes(googleApiClient)
                .await(Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
        if (!result.getStatus().isSuccess()) {
            FWLog.e("Failed to get connected nodes");
            return;
        }

        // No easy way to get the device responsible for sending up league info, so just blast the
        // sync request to all connected devices.
        for (Node node : result.getNodes()) {
            FWLog.d("Sending %s request to %s", path, node.getDisplayName());
            SendMessageResult sendResult = Wearable.MessageApi.sendMessage(
                    googleApiClient, node.getId(), path, data)
                    .await(Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
            if (sendResult.getStatus().isSuccess()) {
                FWLog.d("%s request to %s successful", path, node.getDisplayName());
            } else {
                FWLog.e("Error sending %s request to %s, status = %s", path, node.getDisplayName(),
                        sendResult.getStatus());
            }
        }
    }
}
