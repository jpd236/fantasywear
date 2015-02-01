package com.jeffpdavidson.fantasywear.api.auth;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;

import com.android.volley.VolleyError;
import com.jeffpdavidson.fantasywear.BuildConfig;
import com.jeffpdavidson.fantasywear.R;
import com.jeffpdavidson.fantasywear.api.Volley;
import com.jeffpdavidson.fantasywear.fragments.RetriableErrorDialogFragment;
import com.jeffpdavidson.fantasywear.log.FWLog;
import com.jeffpdavidson.fantasywear.storage.TokenTable;
import com.jeffpdavidson.fantasywear.sync.SyncProvider;

/**
 * {@link AccountAuthenticatorActionBarActivity} for obtaining Yahoo OAuth authorization.
 *
 * NOTE: This activity is configured to handle orientation and screen size configuration changes
 * (e.g. rotations). This is safe as long as the activity has no resources which change depending on
 * configuration - which is likely as this is a simple WebView with a progress bar - and useful
 * since it is difficult to save WebView state, and easier to make AsyncTasks tied to the Activity
 * lifecycle rather than placing these in a retained fragment or loader.
 */
public class AuthActivity extends AccountAuthenticatorActionBarActivity
        implements RetriableErrorDialogFragment.Listener {
    private static final String TAG = "AuthActivity";

    /** Progress percentage shown while performing token requests. */
    private static final int TOKEN_PROGRESS = 10;

    public static Intent get(Context context) {
        return new Intent(context, AuthActivity.class);
    }

    private WebView mWebView;
    private ProgressBar mProgressBar;
    private Token mRequestToken;
    private AsyncTask<?, ?, ?> mTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        getSupportActionBar().setSubtitle(R.string.add_account);

        mProgressBar = (ProgressBar) findViewById(R.id.progress_bar);

        mWebView = (WebView) findViewById(R.id.auth_web_view);
        mWebView.setWebViewClient(mWebViewClient);
        mWebView.setWebChromeClient(mWebChromeClient);

        // Disable saving of any state as this WebView is used for one-time authentication.
        disableWebViewState(mWebView);

        startAuth();
    }

    @Override
    public void onDestroy() {
        if (mTask != null) {
            mTask.cancel(true /* mayInterruptIfRunning */);
            mTask = null;
        }
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            setResult(RESULT_CANCELED);
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void startAuth() {
        // Show partial progress while we obtain a request token.
        setActivityProgress(TOKEN_PROGRESS);

        mTask = new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                try {
                    mRequestToken = OAuthClient.blockingGetRequestToken(AuthActivity.this, TAG);
                } catch (VolleyError e) {
                    FWLog.e(e, "VolleyError fetching request token");
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    FWLog.e("Interrupted while fetching request token");
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void result) {
                setActivityProgress(100);

                if (mRequestToken != null) {
                    mWebView.loadUrl(mRequestToken.request_auth_url);
                } else {
                    RetriableErrorDialogFragment.showIfNeeded(getSupportFragmentManager(),
                            getString(R.string.auth_error));
                }
            }

            @Override
            protected void onCancelled(Void result) {
                Volley.getInstance(AuthActivity.this).getRequestQueue().cancelAll(TAG);
            }
        }.execute();
    }

    @Override
    public void onRetry() {
        startAuth();
    }

    @Override
    public void onCancel() {
        setResult(RESULT_CANCELED);
        finish();
    }

    @SuppressWarnings("deprecation")
    private static void disableWebViewState(WebView webView) {
        WebSettings webSettings = webView.getSettings();
        webSettings.setSaveFormData(false);
        // Deprecated because it will be impossible to set to true in the future.
        webSettings.setSavePassword(false);
        CookieManager cookieManager = CookieManager.getInstance();
        // Remove all cookies to avoid having a previous login appear here.
        // Deprecated in favor of removeAllCookies which is unavailable pre-L, but we don't care
        // about the callback anyway.
        cookieManager.removeAllCookie();
    }

    private final WebViewClient mWebViewClient = new WebViewClient() {
        @Override
        public void onReceivedError(WebView view, int errorCode, String description,
                String failingUrl) {
            FWLog.e("WebView error %d at %s: %s", errorCode, failingUrl, description);
            RetriableErrorDialogFragment.showIfNeeded(getSupportFragmentManager(),
                    getString(R.string.auth_error));
        }

        @Override
        public boolean shouldOverrideUrlLoading(WebView view, final String url) {
            if (!url.startsWith(BuildConfig.CALLBACK_URL)) {
                // Let WebView handle URL.
                return false;
            }

            // Show partial progress while we fetch the token.
            setActivityProgress(TOKEN_PROGRESS);

            // Exchange verifier for an access token.
            mTask = new AsyncTask<Void, Void, Bundle>() {
                @Override
                protected Bundle doInBackground(Void... params) {
                    try {
                        Token token = OAuthClient.blockingGetToken(
                                AuthActivity.this, TAG, mRequestToken, url);
                        AccountManager accountManager = AccountManager.get(AuthActivity.this);
                        Account account = new Account(
                                token.yahoo_guid, AccountAuthenticator.ACCOUNT_TYPE_YAHOO);
                        if (accountManager.addAccountExplicitly(account, null, null)) {
                            // Only insert token if the account doesn't already exist.
                            TokenTable.insertToken(AuthActivity.this, account, token);
                        }
                        Bundle result = new Bundle();
                        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                        result.putString(AccountManager.KEY_ACCOUNT_TYPE, account.type);

                        // Enable automatic sync. This should trigger an initial sync shortly, which
                        // will also enable subsequent. periodic syncs.
                        ContentResolver.setSyncAutomatically(account, SyncProvider.AUTHORITY, true);

                        return result;
                    } catch (VolleyError e) {
                        FWLog.e(e, "VolleyError fetching token");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        FWLog.e("Interrupted while fetching token");
                    }
                    return null;
                }

                @Override
                protected void onPostExecute(Bundle result) {
                    setActivityProgress(100);
                    if (result != null) {
                        setAccountAuthenticatorResult(result);
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        RetriableErrorDialogFragment.showIfNeeded(getSupportFragmentManager(),
                                getString(R.string.auth_error));
                    }
                }

                @Override
                protected void onCancelled(Bundle result) {
                    Volley.getInstance(AuthActivity.this).getRequestQueue().cancelAll(TAG);
                }
            }.execute();
            return true;
        }
    };

    private final WebChromeClient mWebChromeClient = new WebChromeClient() {
        @Override
        public void onProgressChanged(WebView webView, int progress) {
            setActivityProgress(progress);
        }
    };

    private void setActivityProgress(int progress) {
        mProgressBar.setVisibility(progress == 100 ? View.GONE : View.VISIBLE);
        mProgressBar.setProgress(progress);
    }
}
