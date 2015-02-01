package com.jeffpdavidson.fantasywear.services;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi.DeleteDataItemsResult;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataItemBuffer;
import com.google.android.gms.wearable.Wearable;
import com.jeffpdavidson.fantasywear.api.auth.AccountAuthenticator;
import com.jeffpdavidson.fantasywear.log.FWLog;
import com.jeffpdavidson.fantasywear.protocol.LeagueData;
import com.jeffpdavidson.fantasywear.storage.TokenTable;
import com.jeffpdavidson.fantasywear.util.Constants;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/** Service which finds and cleans up removed accounts. */
public class CleanUnusedAccountsService extends IntentService {
    private static final String TAG = "CleanUnusedAcctsSvc";

    private GoogleApiClient mGoogleApiClient;

    public static void start(Context context) {
        Intent intent = new Intent(context, CleanUnusedAccountsService.class);
        context.startService(intent);
    }

    public CleanUnusedAccountsService() {
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
        FWLog.d("Cleaning unused accounts");

        // Obtain the current list of active accounts.
        AccountManager accountManager = AccountManager.get(this);
        Account[] accounts =
                accountManager.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE_YAHOO);

        // Remove any orphaned entries from the token table in our database.
        TokenTable.cleanUnusedTokens(this, accounts);

        // Attempt to clear any active notifications for missing accounts.
        if (!mGoogleApiClient.isConnected() && !mGoogleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS).isSuccess()) {
            FWLog.e("Unable to connect to wear, cannot remove stale notifications");
        }

        DataItemBuffer result = Wearable.DataApi.getDataItems(mGoogleApiClient)
                .await(Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
        try {
            if (!result.getStatus().isSuccess()) {
                FWLog.e("Unable to get data items, cannot remove stale notifications");
            }
            Set<String> accountNames = new HashSet<>();
            for (Account account : accounts) {
                accountNames.add(account.name);
            }
            for (DataItem item : result) {
                if (!LeagueData.isActiveLeagueDataItem(item, accountNames)) {
                    DeleteDataItemsResult deleteResult = Wearable.DataApi.deleteDataItems(
                            mGoogleApiClient, item.getUri()).await(
                            Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
                    FWLog.i("Clean league data for URI = %s, status = %s, numDeleted = %d",
                            item.getUri(), deleteResult.getStatus(), deleteResult.getNumDeleted());
                }
            }
        } finally {
            result.release();
        }
    }
}
