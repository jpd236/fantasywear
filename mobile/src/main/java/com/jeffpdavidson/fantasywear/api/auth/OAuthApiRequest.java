package com.jeffpdavidson.fantasywear.api.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.content.Context;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.jeffpdavidson.fantasywear.api.Volley;
import com.jeffpdavidson.fantasywear.log.FWLog;
import com.jeffpdavidson.fantasywear.storage.TokenTable;
import com.jeffpdavidson.fantasywear.util.WireUtil;

import java.io.IOException;
import java.io.UnsupportedEncodingException;

/**
 * Volley request for an API which requires OAuth authentication.
 *
 * @param <T> the type of the response, as parsed in {@link #parseResponse}.
 */
public abstract class OAuthApiRequest<T> extends OAuthRequest<T> {
    private final Context mContext;
    private final AccountManager mAccountManager;
    private final Account mAccount;
    private Token mToken;

    public OAuthApiRequest(Context context, String url, Account account, Listener<T> listener,
            ErrorListener errorListener) {
        super(Method.GET, url, listener, errorListener);
        mContext = context;
        mAccountManager = AccountManager.get(context);
        mAccount = account;
    }

    @Override
    protected Token getToken() throws AuthFailureError {
        String tokenStr;
        try {
            tokenStr = mAccountManager.blockingGetAuthToken(
                    mAccount, AccountAuthenticator.TOKEN_TYPE_OAUTH, false /* notifyAuthFailure */);
        } catch (AuthenticatorException | OperationCanceledException | IOException e) {
            throw new AuthFailureError("Unable to obtain auth token", e);
        }

        mToken = WireUtil.decodeFromString(tokenStr, Token.class);
        if (mToken == null) {
            throw new AuthFailureError("Error decoding token string");
        }
        return mToken;
    }

    @Override
    protected final Response<T> parseNetworkResponse(NetworkResponse networkResponse) {
        String response;
        try {
            response = new String(networkResponse.data,
                    HttpHeaderParser.parseCharset(networkResponse.headers));
        } catch (UnsupportedEncodingException e) {
            response = new String(networkResponse.data);
        }

        T parsedResponse;
        try {
            parsedResponse = parseResponse(response);
        } catch (ParseError e) {
            return Response.error(e);
        }

        return Response.success(parsedResponse,
                HttpHeaderParser.parseCacheHeaders(networkResponse));
    }

    @Override
    protected VolleyError parseNetworkError(VolleyError error) {
        // Invalidate the token if we got a client error back. This will force a refresh on the next
        // request; if that fails, the account will be removed.
        if (Volley.isClientError(error.networkResponse)) {
            FWLog.e("Invalidating token due to HTTP client error code %d",
                    error.networkResponse.statusCode);
            TokenTable.invalidateToken(mContext, mAccount, mToken);
        }

        return super.parseNetworkError(error);
    }

    protected abstract T parseResponse(String response) throws ParseError;
}
