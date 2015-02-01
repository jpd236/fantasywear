package com.jeffpdavidson.fantasywear.api.auth;

import android.net.Uri;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.toolbox.HttpHeaderParser;
import com.jeffpdavidson.fantasywear.annotations.VisibleForTesting;
import com.jeffpdavidson.fantasywear.log.FWLog;

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;

/**
 * Volley request for performing OAuth token negotiation.
 *
 * A token must be provided. This can be an empty token when obtaining a request token; the request
 * token when obtaining an auth token; or, an expired auth token when refreshing a token. The
 * response data is the token obtained by the request.
 */
public class OAuthTokenRequest extends OAuthRequest<Token> {
    private static final Uri BASE_ENDPOINT = Uri.parse("https://api.login.yahoo.com/oauth/v2/");

    private static final String KEY_TOKEN = "oauth_token";
    private static final String KEY_TOKEN_SECRET = "oauth_token_secret";
    private static final String KEY_SESSION_HANDLE = "oauth_session_handle";
    private static final String KEY_EXPIRES_IN = "oauth_expires_in";
    private static final String KEY_AUTHORIZATION_EXPIRES_IN = "oauth_authorization_expires_in";
    private static final String KEY_REQUEST_AUTH_URL = "xoauth_request_auth_url";
    private static final String KEY_YAHOO_GUID = "xoauth_yahoo_guid";

    private static final Pattern PATTERN_AMPERSAND = Pattern.compile("&");
    private static final Pattern PATTERN_EQUALS = Pattern.compile("=");

    private final Token mPreviousToken;
    private final Map<String, String> mExtraHeaders;

    public OAuthTokenRequest(String endpoint, Token previousToken, Map<String, String> extraHeaders,
            Listener<Token> listener, ErrorListener errorListener) {
        super(Method.POST, Uri.withAppendedPath(BASE_ENDPOINT, endpoint).toString(), listener,
                errorListener);
        mPreviousToken = previousToken;
        mExtraHeaders = extraHeaders;
    }

    @Override
    protected SortedMap<String, String> getOAuthHeaders(Token token) throws AuthFailureError {
        SortedMap<String, String> headers = super.getOAuthHeaders(token);
        headers.putAll(mExtraHeaders);
        return headers;
    }

    @Override
    protected Token getToken() {
        return mPreviousToken;
    }

    @Override
    protected Response<Token> parseNetworkResponse(NetworkResponse networkResponse) {
        String data;
        try {
            data = new String(networkResponse.data,
                    HttpHeaderParser.parseCharset(networkResponse.headers));
        } catch (UnsupportedEncodingException e) {
            return Response.error(new ParseError(e));
        }

        Token.Builder token = new Token.Builder();
        for (String keyValuePair : TextUtils.split(data, PATTERN_AMPERSAND)) {
            String[] splitKeyValuePair = TextUtils.split(keyValuePair, PATTERN_EQUALS);
            if (splitKeyValuePair.length == 2) {
                addParameterToToken(token, splitKeyValuePair[0],
                        ParameterEncoding.decode(splitKeyValuePair[1]));
            } else {
                FWLog.e("Skipping malformed response parameter: %s", keyValuePair);
            }
        }

        return Response.success(token.build(), HttpHeaderParser.parseCacheHeaders(networkResponse));
    }

    private void addParameterToToken(Token.Builder token, String key, String value) {
        switch (key) {
            case KEY_TOKEN:
                token.token = value;
                break;
            case KEY_TOKEN_SECRET:
                token.token_secret = value;
                break;
            case KEY_SESSION_HANDLE:
                token.session_handle = value;
                break;
            case KEY_EXPIRES_IN:
                token.expiration_time_sec = calculateExpirationTime(value);
                break;
            case KEY_AUTHORIZATION_EXPIRES_IN:
                token.authorization_expiration_time_sec = calculateExpirationTime(value);
                break;
            case KEY_REQUEST_AUTH_URL:
                token.request_auth_url = value;
                break;
            case KEY_YAHOO_GUID:
                token.yahoo_guid = value;
                break;
        }
    }

    private long calculateExpirationTime(String expiresInSec) {
        return TimeUnit.MILLISECONDS.toSeconds(currentTimeMillis()) + Long.parseLong(expiresInSec);
    }

    @VisibleForTesting
    protected long currentTimeMillis() {
        return System.currentTimeMillis();
    }
}
