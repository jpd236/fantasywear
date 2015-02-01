package com.jeffpdavidson.fantasywear.api;

import android.accounts.Account;
import android.content.Context;
import android.net.Uri;

import com.android.volley.ParseError;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.jeffpdavidson.fantasywear.api.auth.OAuthApiRequest;
import com.jeffpdavidson.fantasywear.api.model.League;
import com.jeffpdavidson.fantasywear.api.model.Matchup;
import com.jeffpdavidson.fantasywear.api.parser.LeagueParser;
import com.jeffpdavidson.fantasywear.api.parser.ScoreboardParser;
import com.jeffpdavidson.fantasywear.util.Util;

import java.io.StringReader;

/**
 * Client for making calls to Yahoo's fantasy APIs.
 */
public final class YahooClient {
    private abstract static class YahooApiRequest<T> extends OAuthApiRequest<T> {
        private static final Uri BASE_ENDPOINT =
                Uri.parse("http://fantasysports.yahooapis.com/fantasy/v2/");

        public YahooApiRequest(Context context, String endpoint, Account account,
                Listener<T> listener, ErrorListener errorListener) {
            super(context, Uri.withAppendedPath(BASE_ENDPOINT, endpoint).toString(), account,
                    listener, errorListener);
        }
    }

    private YahooClient() {}

    /** Get all active leagues for the given user. */
    public static League[] blockingGetLeagues(Context context, Object tag, Account account)
            throws VolleyError, InterruptedException {
        Util.assertNotOnMainThread();
        RequestFuture<League[]> future = RequestFuture.newFuture();
        YahooApiRequest<League[]> request = new YahooApiRequest<League[]>(context,
                "users;use_login=1/games;is_available=1/leagues", account, future, future) {
            @Override
            protected League[] parseResponse(String response) throws ParseError {
                return LeagueParser.parseXml(new StringReader(response));
            }
        };
        request.setTag(tag);
        return Volley.makeBlockingRequest(context, request, future);
    }

    /** Get the current matchup for the given user and league. */
    public static Matchup blockingGetMatchup(Context context, Object tag, Account account,
            League league) throws VolleyError, InterruptedException {
        Util.assertNotOnMainThread();
        RequestFuture<Matchup> future = RequestFuture.newFuture();
        YahooApiRequest<Matchup> request = new YahooApiRequest<Matchup>(context,
                "league/" + league.league_key + "/scoreboard", account, future, future) {
            @Override
            protected Matchup parseResponse(String response) throws ParseError {
                return ScoreboardParser.parseXml(new StringReader(response));
            }
        };
        request.setTag(tag);
        return Volley.makeBlockingRequest(context, request, future);
    }
}
