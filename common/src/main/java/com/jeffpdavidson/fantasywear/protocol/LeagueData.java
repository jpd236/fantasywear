package com.jeffpdavidson.fantasywear.protocol;

import android.accounts.Account;
import android.net.Uri;
import android.support.annotation.Nullable;

import com.google.android.gms.wearable.Asset;
import com.google.android.gms.wearable.DataItem;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.jeffpdavidson.fantasywear.api.model.League;
import com.jeffpdavidson.fantasywear.api.model.Matchup;
import com.jeffpdavidson.fantasywear.common.BuildConfig;
import com.jeffpdavidson.fantasywear.util.WireUtil;

import java.util.List;
import java.util.Set;

/** Helper methods for communication of league data between the host and wear devices. */
public final class LeagueData {
    private LeagueData() {}

    private static final String KEY_MATCHUP = "matchup";
    private static final String KEY_APP_VERSION = "app_version";
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_LOGO = "logo";
    private static final String KEY_OPPONENT_LOGO = "opponent_logo";

    /**
     * Obtain a {@link PutDataRequest} to update a league's current state.
     *
     * @param account the account which owns the team competing in the league
     * @param league a proto describing the league
     * @param matchup a proto describing the account's team's current matchup
     * @param logo an Asset containing the user's logo image. The image should be square with size
     *             {@link com.jeffpdavidson.fantasywear.common.R.dimen#logo_size}.
     * @param opponentLogo an Asset containing the user's opponent's logo image. The image should be
     *                     square with size
     *                     {@link com.jeffpdavidson.fantasywear.common.R.dimen#logo_size}.
     * @param forceUpdate if true, will ensure that listening devices see the update even if no
     *                    fields have changed since the last update. Use conservatively as this will
     */
    public static PutDataRequest getUpdateRequest(Account account, League league, Matchup matchup,
            Asset logo, Asset opponentLogo, boolean forceUpdate) {
        PutDataMapRequest request =
                PutDataMapRequest.create(getLeagueUri(account, league).toString());
        DataMap map = request.getDataMap();
        map.putInt(KEY_APP_VERSION, BuildConfig.VERSION_CODE);
        map.putString(KEY_MATCHUP, WireUtil.encodeToString(matchup));
        map.putAsset(KEY_LOGO, logo);
        map.putAsset(KEY_OPPONENT_LOGO, opponentLogo);
        if (forceUpdate) {
            // Play services suppresses updates if the payload exactly matches the last one, so when
            // forcing an update, include the current time to guarantee a unique payload.
            map.putLong(KEY_TIMESTAMP, System.currentTimeMillis());
        }
        return request.asPutDataRequest();
    }

    /** Extract the {@link Matchup} for a {@link DataMap} obtained in an update request. */
    public static Matchup getMatchup(DataMap dataMap) {
        return WireUtil.decodeFromString(dataMap.getString(KEY_MATCHUP), Matchup.class);
    }

    /**
     * Extract an {@link Asset} containing the user's logo for a {@link DataMap} obtained in an
     * update request.
     */
    public static Asset getLogo(DataMap dataMap) {
        return dataMap.getAsset(KEY_LOGO);
    }
    /**
     * Extract an {@link Asset} containing the user's opponent's logo for a {@link DataMap} obtained
     * in an update request.
     */
    public static Asset getOpponentLogo(DataMap dataMap) {
        return dataMap.getAsset(KEY_OPPONENT_LOGO);
    }

    /** Whether a {@link DataMap} obtained in an update request is for a manual sync. */
    public static boolean isManualSync(DataMap dataMap) {
        return dataMap.containsKey(KEY_TIMESTAMP);
    }

    /** Return the {@link Uri} to use when transmitting data for a league. */
    public static Uri getLeagueUri(Account account, League league) {
        // Format: league/<account name>/<league key>
        return new Uri.Builder()
                .appendPath(Paths.LEAGUE)
                .appendEncodedPath(account.name)
                .appendEncodedPath(league.league_key)
                .build();
    }

    /**
     * Returns an identifier which is unique for an account/league given a {@link Uri} obtained with
     * {@link #getLeagueUri}, or null for other URIs.
     */
    @Nullable
    public static String getTagIfMatches(Uri uri) {
        List<String> segments = uri.getPathSegments();
        if (isLeagueUri(segments)) {
            return String.format("%s/%s", segments.get(1), segments.get(2));
        }
        return null;
    }

    /** Return whether the given {@link DataItem} is for one of the given account names. */
    public static boolean isActiveLeagueDataItem(DataItem dataItem, Set<String> accountNames) {
        List<String> segments = dataItem.getUri().getPathSegments();
        return isLeagueUri(segments) && accountNames.contains(segments.get(1));
    }

    private static boolean isLeagueUri(List<String> segments) {
        // Format: league/<account name>/<league key>
        return segments.size() == 3 && Paths.LEAGUE.equals(segments.get(0));
    }
}
