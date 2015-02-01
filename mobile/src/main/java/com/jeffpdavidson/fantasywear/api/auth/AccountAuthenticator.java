package com.jeffpdavidson.fantasywear.api.auth;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;

import com.android.volley.VolleyError;
import com.jeffpdavidson.fantasywear.Manifest.permission;
import com.jeffpdavidson.fantasywear.R;
import com.jeffpdavidson.fantasywear.annotations.VisibleForTesting;
import com.jeffpdavidson.fantasywear.api.Volley;
import com.jeffpdavidson.fantasywear.log.FWLog;
import com.jeffpdavidson.fantasywear.storage.TokenTable;
import com.jeffpdavidson.fantasywear.util.WireUtil;

/**
 * Android authenticator for Yahoo Fantasy Sports accounts.
 *
 * Account names are Yahoo GUIDs; account types are {@link #ACCOUNT_TYPE_YAHOO}. The only supported
 * token is of type {@link #TOKEN_TYPE_OAUTH}, which returns a {@link Token} proto that enables
 * OAuth 1.0a authentication. This proto is encoded as a string that can be decoded with
 * {@link WireUtil#decodeFromString}.
 *
 * The methods in this class should not be used directly; instead, use {@link AccountManager} with
 * the {@link #ACCOUNT_TYPE_YAHOO} account type.
 */
public class AccountAuthenticator extends AbstractAccountAuthenticator {
    // Must keep in sync with res/xml/authenticator.xml
    public static final String ACCOUNT_TYPE_YAHOO =
            "com.jeffpdavidson.fantasywear.ACCOUNT_TYPE_YAHOO";
    public static final String TOKEN_TYPE_OAUTH = "oauth";

    /** Service exposing {@link AccountAuthenticator}'s Binder interface. */
    public static class AccountAuthenticatorService extends Service {
        private static AccountAuthenticator sAuthenticator;

        @Override
        public IBinder onBind(Intent intent) {
            if (sAuthenticator == null) {
                sAuthenticator = new AccountAuthenticator(this);
            }
            return sAuthenticator.getIBinder();
        }
    }

    private final AccountManager mAccountManager;
    private final Context mContext;

    AccountAuthenticator(Context context) {
        super(context);
        mAccountManager = AccountManager.get(context);
        mContext = context;
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
            String authTokenType, String[] requiredFeatures, Bundle options)
            throws NetworkErrorException {
        enforcePermission(options);

        Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, AuthActivity.get(mContext));
        return bundle;
    }

    /**
     * Obtain an auth token for the given account.
     *
     * If we've lost the ability to generate tokens, we have no choice but to remove the account, as
     * we cannot force a re-authentication and guarantee that the user is the same.
     *
     * Since this authenticator may not be used by other applications, we assume that the caller is
     * on a background thread and accordingly may block for database operations or network requests.
     * The method is synchronized to avoid refreshing a token twice during simultaneous requests.
     */
    @Override
    public synchronized Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        enforcePermission(options);

        // Fetch the current token.
        Token token = getToken(account);

        long currentTimeSec = System.currentTimeMillis() / 1000;
        if (token.authorization_expiration_time_sec == null ||
                currentTimeSec > token.authorization_expiration_time_sec) {
            // Either we have no saved account info, or we no longer have authorization to
            // automatically refresh old tokens. Either way, this is an unrecoverable state of
            // the account, so remove it and return an error.
            FWLog.e("Token authorization lost; removing account");
            return handleLostAuthorization(account);
        } else if (token.expiration_time_sec == null ||
                currentTimeSec > token.expiration_time_sec) {
            FWLog.v("Refreshing expired or invalidated auth token");
            try {
                token = refreshToken(token);
            } catch (VolleyError e) {
                if (!Volley.isClientError(e.networkResponse)) {
                    throw new NetworkErrorException("Server or network error refreshing token", e);
                } else {
                    // Server indicated we aren't able to refresh tokens. Invalidate account.
                    return handleLostAuthorization(account);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return makeErrorBundle(AccountManager.ERROR_CODE_CANCELED,
                        "Interrupted while refreshing token");
            }

            // Refresh succeeded - save data for future use.
            updateToken(account, token);
        }

        // At this point, we must have an unexpired token. Return it.
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);
        result.putString(AccountManager.KEY_AUTHTOKEN, WireUtil.encodeToString(token));
        return result;
    }

    /**
     * Ensure that the caller is FantasyWear itself (or the system, which has all permissions).
     *
     * This checks {@link permission#USE_FANTASY_WEAR_ACCOUNTS} which is a permission that requires
     * the same signature as this app. Other apps should not be able to obtain the OAuth credentials
     * managed by FantasyWear.
     *
     * Note that this method will not function properly until API 14, because the caller PID/UID
     * were not passed in as extras to both addAccount and getAuthToken until then.
     */
    private void enforcePermission(Bundle options) {
        mContext.enforcePermission(permission.USE_FANTASY_WEAR_ACCOUNTS,
                options.getInt(AccountManager.KEY_CALLER_PID),
                options.getInt(AccountManager.KEY_CALLER_UID),
                "Other applications may not use FantasyWear accounts");
    }

    @SuppressLint("InlinedApi")
    private Bundle handleLostAuthorization(Account account) {
        mAccountManager.removeAccount(account, null, null);
        // This error code was added in API 18, but it is inlined at compile time and it does not
        // matter if we return it on earlier API versions.
        return makeErrorBundle(AccountManager.ERROR_CODE_BAD_AUTHENTICATION, "Lost authorization");
    }

    private static Bundle makeErrorBundle(int errorCode, String errorMsg) {
        Bundle result = new Bundle();
        result.putInt(AccountManager.KEY_ERROR_CODE, errorCode);
        result.putString(AccountManager.KEY_ERROR_MESSAGE, errorMsg);
        return result;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return mContext.getString(R.string.app_name);
    }

    @VisibleForTesting
    protected Token getToken(Account account) {
        return TokenTable.getToken(mContext, account);
    }

    @VisibleForTesting
    protected Token refreshToken(Token token) throws VolleyError, InterruptedException {
        return OAuthClient.blockingRefreshToken(mContext, AccountAuthenticator.class, token);
    }

    @VisibleForTesting
    protected void updateToken(Account account, Token token) {
        TokenTable.updateToken(mContext, account, token);
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
            Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException("confirmCredentials is not supported");
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
            String authTokenType, Bundle options) throws NetworkErrorException {
        throw new UnsupportedOperationException("updateCredentials is not supported");
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
            String[] features) throws NetworkErrorException {
        throw new UnsupportedOperationException("hasFeatures is not supported");
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        throw new UnsupportedOperationException("editProperties is not supported");
    }
}
