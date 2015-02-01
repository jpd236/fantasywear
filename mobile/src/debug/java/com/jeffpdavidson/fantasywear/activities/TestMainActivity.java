package com.jeffpdavidson.fantasywear.activities;

import android.accounts.Account;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.Loader;

import com.jeffpdavidson.fantasywear.api.model.League;
import com.jeffpdavidson.fantasywear.util.IntentExtraUtils;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Extension of {@link MainActivity} to facilitate more isolated testing. */
public class TestMainActivity extends MainActivity {
    private static final String EXTRA_LEAGUES = "leagues";
    private static final String EXTRA_ACCOUNTS = "accounts";

    private final CountDownLatch mLoadedLatch = new CountDownLatch(1);
    private final CountDownLatch mRefreshLatch = new CountDownLatch(1);
    private final CountDownLatch mAddAccountLatch = new CountDownLatch(1);
    private League[] mLeagues;
    private Account[] mAccounts;

    /** Return an Intent to start this activity with the given leagues and accounts. */
    static Intent makeIntent(League[] leagues, Account[] accounts) {
        Intent intent = new Intent();
        IntentExtraUtils.putExtra(intent, EXTRA_LEAGUES, leagues);
        intent.putExtra(EXTRA_ACCOUNTS, accounts);
        return intent;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        mLeagues = IntentExtraUtils.getMessageArrayExtra(intent, EXTRA_LEAGUES, League.class);
        mAccounts = IntentExtraUtils.getParcelableArrayExtra(intent, EXTRA_ACCOUNTS, Account.class);
        super.onCreate(savedInstanceState);
    }

    @Override
    public Loader<League[]> onCreateLoader(int id, Bundle args) {
        if (id == MainActivity.LOADER_LEAGUE) {
            return new TestLeagueLoader(this, mLeagues);
        }
        return super.onCreateLoader(id, args);
    }


    @Override
    public void onLoadFinished(Loader<League[]> loader, League[] leagues) {
        super.onLoadFinished(loader, leagues);
        mLoadedLatch.countDown();
    }

    /** Wait for the leagues to be loaded. */
    boolean awaitLoaded(long timeout, TimeUnit unit) throws InterruptedException {
        return mLoadedLatch.await(timeout, unit);
    }

    @Override
    protected void initAccountManager() {
        onAccountsUpdated(mAccounts);
    }

    @Override protected void cleanupAccountManager() {}

    @Override
    protected void refresh() {
        mRefreshLatch.countDown();
    }

    /** Wait for refresh to be called. */
    boolean awaitRefresh(long timeout, TimeUnit unit) throws InterruptedException {
        return mRefreshLatch.await(timeout, unit);
    }

    @Override
    protected void addAccount() {
        mAddAccountLatch.countDown();
    }

    /** Wait for the add account button to be clicked. */
    boolean awaitAddAccount(long timeout, TimeUnit unit) throws InterruptedException {
        return mAddAccountLatch.await(timeout, unit);
    }

    private static class TestLeagueLoader extends LeagueLoader {
        private final League[] mLeagues;

        TestLeagueLoader(Context context, League[] leagues) {
            super(context);
            mLeagues = leagues;
        }

        @Override
        public League[] loadInBackground() {
            return mLeagues;
        }
    }
}
