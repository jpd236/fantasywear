package com.jeffpdavidson.fantasywear.api.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.annotation.TargetApi;
import android.content.Context;
import android.os.Bundle;
import android.support.test.runner.AndroidJUnit4;

import com.android.volley.NetworkResponse;
import com.android.volley.VolleyError;
import com.jeffpdavidson.fantasywear.Manifest.permission;
import com.jeffpdavidson.fantasywear.util.WireUtil;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

@RunWith(AndroidJUnit4.class)
public class AccountAuthenticatorTest {
    private static final Account ACCOUNT =
            new Account("12345", AccountAuthenticator.ACCOUNT_TYPE_YAHOO);

    @Mock private Context mMockContext;
    @Mock private AccountManager mMockAccountManager;
    private TestAccountAuthenticator mAuthenticator;

    @Before
    public void init() {
        MockitoAnnotations.initMocks(this);
        Mockito.when(mMockContext.getSystemService(Context.ACCOUNT_SERVICE))
                .thenReturn(mMockAccountManager);
        mAuthenticator = new TestAccountAuthenticator(mMockContext);
    }

    @Test(expected = SecurityException.class)
    public void getAuthTokenFromInvalidCaller() throws Exception {
        Bundle options = new Bundle();
        options.putInt(AccountManager.KEY_CALLER_PID, 12345);
        options.putInt(AccountManager.KEY_CALLER_UID, 54321);
        Mockito.doThrow(new SecurityException()).when(mMockContext)
                .enforcePermission(Mockito.eq(permission.USE_FANTASY_WEAR_ACCOUNTS),
                        Mockito.eq(12345), Mockito.eq(54321), Mockito.anyString());
        mAuthenticator.getAuthToken(
                null, ACCOUNT, AccountAuthenticator.TOKEN_TYPE_OAUTH, options);
    }


    @TargetApi(18)
    @Test
    public void getAuthTokenAfterClearingData() throws Exception {
        mAuthenticator.mToken = new Token.Builder().build();
        Bundle result = mAuthenticator.getAuthToken(
                null, ACCOUNT, AccountAuthenticator.TOKEN_TYPE_OAUTH, new Bundle());
        Assert.assertEquals(AccountManager.ERROR_CODE_BAD_AUTHENTICATION,
                result.getInt(AccountManager.KEY_ERROR_CODE));
        Mockito.verify(mMockAccountManager).removeAccount(ACCOUNT, null, null);
    }

    @TargetApi(18)
    @Test
    public void getAuthTokenAfterAuthorizationExpires() throws Exception {
        mAuthenticator.mToken = new Token.Builder()
                .authorization_expiration_time_sec((System.currentTimeMillis() / 1000) - 1)
                .build();
        Bundle result = mAuthenticator.getAuthToken(
                null, ACCOUNT, AccountAuthenticator.TOKEN_TYPE_OAUTH, new Bundle());
        Assert.assertEquals(AccountManager.ERROR_CODE_BAD_AUTHENTICATION,
                result.getInt(AccountManager.KEY_ERROR_CODE));
        Mockito.verify(mMockAccountManager).removeAccount(ACCOUNT, null, null);
    }

    @Test
    public void getAuthTokenWithValidCachedToken() throws Exception {
        long currentTimeSec = System.currentTimeMillis() / 1000;
        mAuthenticator.mToken = new Token.Builder()
                .authorization_expiration_time_sec(currentTimeSec + 1000)
                .expiration_time_sec(currentTimeSec + 1000)
                .build();
        Bundle result = mAuthenticator.getAuthToken(
                null, ACCOUNT, AccountAuthenticator.TOKEN_TYPE_OAUTH, new Bundle());
        Assert.assertEquals(ACCOUNT.name, result.getString(AccountManager.KEY_ACCOUNT_NAME));
        Assert.assertEquals(ACCOUNT.type, result.getString(AccountManager.KEY_ACCOUNT_TYPE));
        Assert.assertEquals(WireUtil.encodeToString(mAuthenticator.mToken),
                result.getString(AccountManager.KEY_AUTHTOKEN));
        Assert.assertFalse(mAuthenticator.mRefreshCalled);
        Assert.assertFalse(mAuthenticator.mUpdateCalled);
    }

    @Test
    public void getAuthTokenWithExpiredToken_refreshSucceeds() throws Exception {
        long currentTimeSec = System.currentTimeMillis() / 1000;
        mAuthenticator.mToken = new Token.Builder()
                .authorization_expiration_time_sec(currentTimeSec + 1000)
                .expiration_time_sec(currentTimeSec - 1)
                .build();
        mAuthenticator.mRefreshedToken = new Token.Builder(mAuthenticator.mToken)
                .expiration_time_sec(currentTimeSec + 100)
                .build();
        Bundle result = mAuthenticator.getAuthToken(
                null, ACCOUNT, AccountAuthenticator.TOKEN_TYPE_OAUTH, new Bundle());
        Assert.assertEquals(ACCOUNT.name, result.getString(AccountManager.KEY_ACCOUNT_NAME));
        Assert.assertEquals(ACCOUNT.type, result.getString(AccountManager.KEY_ACCOUNT_TYPE));
        Assert.assertEquals(WireUtil.encodeToString(mAuthenticator.mRefreshedToken),
                result.getString(AccountManager.KEY_AUTHTOKEN));
        Assert.assertTrue(mAuthenticator.mRefreshCalled);
        Assert.assertTrue(mAuthenticator.mUpdateCalled);
    }

    @Test(expected = NetworkErrorException.class)
    public void getAuthTokenWithExpiredToken_transientRefreshError() throws Exception {
        long currentTimeSec = System.currentTimeMillis() / 1000;
        mAuthenticator.mToken = new Token.Builder()
                .authorization_expiration_time_sec(currentTimeSec + 1000)
                .expiration_time_sec(currentTimeSec - 1)
                .build();
        mAuthenticator.mRefreshError = new VolleyError("error");
        mAuthenticator.getAuthToken(
                null, ACCOUNT, AccountAuthenticator.TOKEN_TYPE_OAUTH, new Bundle());
    }

    @TargetApi(18)
    @Test
    public void getAuthTokenWithExpiredToken_permanentRefreshError() throws Exception {
        long currentTimeSec = System.currentTimeMillis() / 1000;
        mAuthenticator.mToken = new Token.Builder()
                .authorization_expiration_time_sec(currentTimeSec + 1000)
                .expiration_time_sec(currentTimeSec - 1)
                .build();
        mAuthenticator.mRefreshError = new VolleyError(new NetworkResponse(401, null, null, false));
        Bundle result = mAuthenticator.getAuthToken(
                null, ACCOUNT, AccountAuthenticator.TOKEN_TYPE_OAUTH, new Bundle());
        Assert.assertEquals(AccountManager.ERROR_CODE_BAD_AUTHENTICATION,
                result.getInt(AccountManager.KEY_ERROR_CODE));
        Mockito.verify(mMockAccountManager).removeAccount(ACCOUNT, null, null);
    }

    private static class TestAccountAuthenticator extends AccountAuthenticator {
        Token mToken;
        Token mRefreshedToken;
        VolleyError mRefreshError;
        boolean mRefreshCalled;
        boolean mUpdateCalled;

        TestAccountAuthenticator(Context context) {
            super(context);
        }

        @Override
        protected Token getToken(Account account) {
            return mToken;
        }

        @Override
        protected Token refreshToken(Token token) throws VolleyError {
            mRefreshCalled = true;
            if (mRefreshError != null) {
                throw mRefreshError;
            }
            return mRefreshedToken;
        }

        @Override
        protected void updateToken(Account account, Token token) {
            mUpdateCalled = true;
        }
    }
}
