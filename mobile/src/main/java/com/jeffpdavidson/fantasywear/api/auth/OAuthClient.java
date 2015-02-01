package com.jeffpdavidson.fantasywear.api.auth;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;

import com.android.volley.AuthFailureError;
import com.android.volley.RequestQueue;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.jeffpdavidson.fantasywear.BuildConfig;
import com.jeffpdavidson.fantasywear.api.Volley;
import com.jeffpdavidson.fantasywear.util.Util;

import java.util.HashMap;
import java.util.Map;

/**
 * Client for performing OAuth authentication calls.
 *
 * Methods in this class are blocking and must be called from a background thread. Requests are made
 * with Volley, and in-flight requests may be cancelled by obtaining the request queue with
 * {@link Volley#getRequestQueue()} and calling {@link RequestQueue#cancelAll(Object)} with the tag
 * provided to the request method.
 */
final class OAuthClient {
    private static final String ENDPOINT_GET_REQUEST_TOKEN = "get_request_token";
    private static final String ENDPOINT_GET_TOKEN = "get_token";

    private static final String KEY_CALLBACK = "oauth_callback";
    private static final String KEY_LANG_PREF = "xoauth_lang_pref";
    private static final String KEY_VERIFIER = "oauth_verifier";
    private static final String KEY_SESSION_HANDLE = "oauth_session_handle";

    private OAuthClient() {}

    /** Obtain an OAuth request token. */
    @TargetApi(21)
    public static Token blockingGetRequestToken(Context context, Object tag)
            throws VolleyError, InterruptedException {
        Util.assertNotOnMainThread();
        RequestFuture<Token> future = RequestFuture.newFuture();
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put(KEY_CALLBACK, BuildConfig.CALLBACK_URL);
        if (Build.VERSION.SDK_INT >= 21) {
            extraHeaders.put(KEY_LANG_PREF,
                    context.getResources().getConfiguration().locale.toLanguageTag());
        }
        // We use an empty token here because this is the initial request.
        OAuthTokenRequest request = new OAuthTokenRequest(ENDPOINT_GET_REQUEST_TOKEN,
                new Token.Builder().build(), extraHeaders, future, future);
        request.setTag(tag);
        return Volley.makeBlockingRequest(context, request, future);
    }

    /**
     * Exchange a request token and an OAuth verifier (attached to the given callback uri) for an
     * auth token.
     */
    public static Token blockingGetToken(Context context, Object tag, Token requestToken,
            String callbackUri) throws VolleyError, InterruptedException {
        Util.assertNotOnMainThread();
        RequestFuture<Token> future = RequestFuture.newFuture();
        Map<String, String> extraHeaders = new HashMap<>();
        Uri uri = Uri.parse(callbackUri);
        String verifier = uri.getQueryParameter(KEY_VERIFIER);
        if (TextUtils.isEmpty(verifier)) {
            throw new AuthFailureError("Provided callbackUri has no verifier: " + callbackUri);
        }
        extraHeaders.put(KEY_VERIFIER, verifier);
        OAuthTokenRequest request = new OAuthTokenRequest(
                ENDPOINT_GET_TOKEN, requestToken, extraHeaders, future, future);
        request.setTag(tag);
        return Volley.makeBlockingRequest(context, request, future);
    }

    /** Refresh an expired auth token. */
    public static Token blockingRefreshToken(Context context, Object tag, Token token)
            throws VolleyError, InterruptedException {
        Util.assertNotOnMainThread();
        RequestFuture<Token> future = RequestFuture.newFuture();
        Map<String, String> extraHeaders = new HashMap<>();
        extraHeaders.put(KEY_SESSION_HANDLE, token.session_handle);
        OAuthTokenRequest request = new OAuthTokenRequest(
                ENDPOINT_GET_TOKEN, token, extraHeaders, future, future);
        request.setTag(tag);
        return Volley.makeBlockingRequest(context, request, future);
    }
}
