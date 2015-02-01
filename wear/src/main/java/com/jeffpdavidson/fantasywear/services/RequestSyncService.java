package com.jeffpdavidson.fantasywear.services;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Wearable;
import com.jeffpdavidson.fantasywear.protocol.Paths;
import com.jeffpdavidson.fantasywear.util.MessageApiUtil;

/** Service to request a manual sync of all accounts. */
public class RequestSyncService extends IntentService {
    private static final String TAG = "RequestSyncService";

    private GoogleApiClient mGoogleApiClient;

    public static void start(Context context) {
        context.startService(new Intent(context, RequestSyncService.class));
    }

    public RequestSyncService() {
        super(TAG);
        setIntentRedelivery(true);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onDestroy() {
        mGoogleApiClient.disconnect();
        super.onDestroy();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        MessageApiUtil.sendMessage(mGoogleApiClient, Paths.SYNC, new byte[0]);
    }
}
