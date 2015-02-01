package com.jeffpdavidson.fantasywear.sync;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.AbstractThreadedSyncAdapter;
import android.content.BroadcastReceiver;
import android.content.ContentProviderClient;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SyncResult;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.AuthFailureError;
import com.android.volley.ParseError;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageRequest;
import com.android.volley.toolbox.RequestFuture;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataApi.DataItemResult;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.jeffpdavidson.fantasywear.R;
import com.jeffpdavidson.fantasywear.annotations.VisibleForTesting;
import com.jeffpdavidson.fantasywear.api.Volley;
import com.jeffpdavidson.fantasywear.api.YahooClient;
import com.jeffpdavidson.fantasywear.api.auth.AccountAuthenticator;
import com.jeffpdavidson.fantasywear.api.model.League;
import com.jeffpdavidson.fantasywear.api.model.Matchup;
import com.jeffpdavidson.fantasywear.log.FWLog;
import com.jeffpdavidson.fantasywear.protocol.LeagueData;
import com.jeffpdavidson.fantasywear.storage.LeagueTable;
import com.jeffpdavidson.fantasywear.storage.Preferences;
import com.jeffpdavidson.fantasywear.storage.TokenTable;
import com.jeffpdavidson.fantasywear.util.Constants;
import com.jeffpdavidson.fantasywear.util.LocalBroadcasts;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * Sync adapter for FantasyWear data.
 *
 * Periodically fetches scores for all of the user's leagues, and updates Wear accordingly. Will
 * also fetch all leagues for the user's accounts daily or when a manual refresh is performed.
 */
public class SyncAdapter extends AbstractThreadedSyncAdapter {
    private static final int LEAGUE_REFRESH_PERIOD_SEC = 24 * 60 * 60; // 1 day
    private static final int INITIAL_SYNC_TRIES = 10;
    private static final int INITIAL_SYNC_PERIOD_MS = 10000;

    /** Request an expedited manual sync of all the accounts on the system. */
    public static void requestManualSync(Context context) {
        Bundle settings = new Bundle();
        settings.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
        settings.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
        for (Account account : AccountManager.get(context).getAccountsByType(
                AccountAuthenticator.ACCOUNT_TYPE_YAHOO)) {
            ContentResolver.requestSync(account, SyncProvider.AUTHORITY, settings);
        }
    }

    /** Set the interval for periodic syncs for all accounts. */
    public static void setPeriodicSyncIntervalSec(Context context, int intervalSec) {
        AccountManager am = AccountManager.get(context);
        for (Account account : am.getAccountsByType(AccountAuthenticator.ACCOUNT_TYPE_YAHOO)) {
            setPeriodicSyncIntervalSec(account, intervalSec);
        }
    }

    private static void setPeriodicSyncIntervalSec(Account account, int intervalSec) {
        ContentResolver.addPeriodicSync(account, SyncProvider.AUTHORITY, new Bundle(), intervalSec);
    }

    SyncAdapter(Context context) {
        super(context, true, true);
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority,
            ContentProviderClient provider, SyncResult syncResult) {
        FWLog.d("Performing FantasyWear sync");
        boolean isManualSync = extras.getBoolean(ContentResolver.SYNC_EXTRAS_MANUAL);
        try {
            onPerformSync(account, isManualSync, syncResult);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            FWLog.e("Interrupted while getting league information");
        } catch (ParseError e) {
            FWLog.e(e, "Parse error while getting league information");
            syncResult.stats.numParseExceptions++;
        } catch (AuthFailureError e) {
            FWLog.e(e, "Parse error while getting league information");
            syncResult.stats.numAuthExceptions++;
        } catch (VolleyError e) {
            FWLog.e(e, "Network error while getting league information");
            syncResult.stats.numIoExceptions++;
        } catch (IOException e) {
            FWLog.e(e, "Error communicating with Play Services while updating league information");
            syncResult.stats.numIoExceptions++;
        }
        FWLog.i("Sync complete");
    }

    @Override
    public void onSyncCanceled(@NonNull Thread thread) {
        super.onSyncCanceled(thread);
        Volley.getInstance(getContext()).getRequestQueue().cancelAll(SyncAdapter.class);
    }

    private void onPerformSync(Account account, boolean isManualSync, SyncResult syncResult)
            throws IOException, VolleyError, InterruptedException {
        long lastSyncTimeSec = TokenTable.getLastSyncTimeSec(getContext(), account);
        final League[] leagues;
        if (shouldRefreshLeagues(lastSyncTimeSec, isManualSync)) {
            leagues = YahooClient.blockingGetLeagues(getContext(), SyncAdapter.class, account);
            LeagueTable.updateLeagues(getContext(), account, leagues);
            FWLog.d("Updated leagues, found %d", leagues.length);
        } else {
            leagues = LeagueTable.getLeagues(getContext(), account);
        }

        GoogleApiClient googleApiClient = new GoogleApiClient.Builder(getContext())
                .addApi(Wearable.API)
                .build();
        final int syncIntervalSec;
        try {
            if (leagues.length == 0) {
                // We have no leagues; only sync to update leagues. We use half the full league
                // refresh interval to avoid having a periodic sync happen just before the interval
                // completes, causing us to miss a cycle.
                FWLog.i("No leagues found");
                syncIntervalSec = LEAGUE_REFRESH_PERIOD_SEC / 2;
            } else {
                for (League league : leagues) {
                    FWLog.d("Updating league %s", league);
                    syncLeague(googleApiClient, account, league, lastSyncTimeSec, isManualSync);
                    syncResult.stats.numUpdates++;
                    FWLog.d("Update succeeded for %s", league);
                }
                syncIntervalSec = Preferences.getSyncIntervalSec(getContext());
            }
        } finally {
            googleApiClient.disconnect();
        }

        TokenTable.setLastSyncTimeSec(getContext(), account, System.currentTimeMillis() / 1000);
        setPeriodicSyncIntervalSec(account, syncIntervalSec);
    }

    private void syncLeague(GoogleApiClient googleApiClient, Account account, League league,
            long lastSyncTimeSec, boolean isManualSync)
            throws IOException, VolleyError, InterruptedException {
        Matchup matchup =
                YahooClient.blockingGetMatchup(getContext(), SyncAdapter.class, account, league);

        // Fetch the logos for each team. Note that Volley will use cached versions of the bitmaps
        // if present and not expired.
        RequestFuture<Bitmap> logoFuture = RequestFuture.newFuture();
        RequestFuture<Bitmap> oppLogoFuture = RequestFuture.newFuture();
        int logoSize = getContext().getResources().getDimensionPixelSize(R.dimen.logo_size);
        ImageRequest logoReq = new ImageRequest(matchup.my_team.logo_url,
                logoFuture, logoSize, logoSize, Bitmap.Config.ARGB_8888, logoFuture);
        ImageRequest oppLogoReq = new ImageRequest(matchup.opponent_team.logo_url,
                oppLogoFuture, logoSize, logoSize, Bitmap.Config.ARGB_8888, oppLogoFuture);
        Volley.getInstance(getContext()).getRequestQueue().add(logoReq);
        Volley.getInstance(getContext()).getRequestQueue().add(oppLogoReq);
        Asset logo;
        Asset oppLogo;
        try {
            logo = getLogoAssetForBitmap(logoFuture.get(), logoSize);
            oppLogo = getLogoAssetForBitmap(oppLogoFuture.get(), logoSize);
        } catch (ExecutionException e) {
            throw (VolleyError) e.getCause();
        }

        // Push the updated scores to connected wearable devices.
        if (!googleApiClient.isConnected() && !googleApiClient.blockingConnect(
                Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS).isSuccess()) {
            throw new IOException("Unable to connect to GoogleApiClient");
        }
        // Force an update for all manual syncs and the first sync (to cover the corner case of an
        // uninstall and reinstall where the old data is still cached).
        boolean isInitialSync = lastSyncTimeSec == 0L;

        if (!isInitialSync) {
            sendUpdateToWear(googleApiClient, account, league, matchup, logo, oppLogo,
                    isManualSync);
        } else {
            // It appears that on the initial sync, the first n updates get queued but never
            // delivered, only for all of them to come at once after the n+1st. To work around this,
            // repeatedly attempt to send updates every few seconds, until the wear device
            // acknowledges the update.
            final String path = LeagueData.getLeagueUri(account, league).getPath();
            final CountDownLatch latch = new CountDownLatch(1);
            IntentFilter filter = new IntentFilter(LocalBroadcasts.ACTION_ACK);
            filter.addDataScheme("wear");
            BroadcastReceiver receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (path.equals(intent.getData().getPath())) {
                        FWLog.v("Received matching ACK from wear device for %s", path);
                        latch.countDown();
                    } else {
                        FWLog.w("Unexpected ACK: %s", intent.getData().getPath());
                    }
                }
            };
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getContext());
            lbm.registerReceiver(receiver, filter);
            try {
                for (int i = 0; i < INITIAL_SYNC_TRIES; i++) {
                    sendUpdateToWear(
                            googleApiClient, account, league, matchup, logo, oppLogo, true);
                    if (latch.await(INITIAL_SYNC_PERIOD_MS, TimeUnit.MILLISECONDS)) {
                        FWLog.v("ACK received after %d tries, sync successful", i);
                        break;
                    } else {
                        FWLog.v("ACK not received after %d tries", i);
                    }
                }
                if (latch.getCount() != 0) {
                    FWLog.e("Did not receive ACK from wear device, giving up.");
                }
            } finally {
                lbm.unregisterReceiver(receiver);
            }
        }
    }

    private static void sendUpdateToWear(GoogleApiClient googleApiClient, Account account,
            League league, Matchup matchup, Asset logo, Asset oppLogo, boolean forceUpdate)
            throws IOException {
        PutDataRequest request = LeagueData.getUpdateRequest(account, league, matchup,
                logo, oppLogo, forceUpdate);
        DataItemResult result = Wearable.DataApi
                .putDataItem(googleApiClient, request)
                .await(Constants.GOOGLE_API_CLIENT_TIMEOUT_SEC, TimeUnit.SECONDS);
        if (!result.getStatus().isSuccess()) {
            throw new IOException("Failed to send new scores to Wear, status = "
                    + result.getStatus());
        }
    }

    @VisibleForTesting
    public static Asset getLogoAssetForBitmap(Bitmap bitmap, int size) {
        // If the bitmap is less than the desired size, scale it up.
        if (bitmap.getWidth() < size) {
            bitmap = Bitmap.createScaledBitmap(bitmap, size, size, true);
        }
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, byteStream);
        return Asset.createFromBytes(byteStream.toByteArray());
    }

    private boolean shouldRefreshLeagues(long lastSyncTimeSec, boolean isManualSync) {
        // Always refresh leagues on manual syncs.
        if (isManualSync) {
            FWLog.d("Refreshing leagues because a manual sync was requested");
            return true;
        }

        // Otherwise, refresh the league if LEAGUE_REFRESH_PERIOD_SEC seconds have elapsed since the
        // last league sync.
        long currentTimeSec = System.currentTimeMillis() / 1000;
        if (lastSyncTimeSec > currentTimeSec
                || currentTimeSec - lastSyncTimeSec > LEAGUE_REFRESH_PERIOD_SEC) {
            FWLog.d("Refreshing leagues: lastSyncTimeSec = %d", lastSyncTimeSec);
            return true;
        }

        FWLog.d("Not refreshing leagues");
        return false;
    }

    public static class SyncService extends Service {
        private static SyncAdapter sSyncAdapter;

        @Override
        public IBinder onBind(Intent intent) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapter(this);
            }
            return sSyncAdapter.getSyncAdapterBinder();
        }
    }
}
