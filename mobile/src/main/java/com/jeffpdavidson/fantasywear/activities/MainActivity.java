package com.jeffpdavidson.fantasywear.activities;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.jeffpdavidson.fantasywear.R;
import com.jeffpdavidson.fantasywear.annotations.VisibleForTesting;
import com.jeffpdavidson.fantasywear.api.auth.AccountAuthenticator;
import com.jeffpdavidson.fantasywear.api.model.League;
import com.jeffpdavidson.fantasywear.fragments.AccountInfoDialogFragment;
import com.jeffpdavidson.fantasywear.fragments.SyncIntervalDialogFragment;
import com.jeffpdavidson.fantasywear.storage.LeagueTable;
import com.jeffpdavidson.fantasywear.sync.SyncAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Main entry point for managing FantasyWear. */
public class MainActivity extends ActionBarActivity implements
        LoaderCallbacks<League[]>, OnClickListener, OnAccountsUpdateListener, OnItemClickListener {
    @VisibleForTesting
    static final int LOADER_LEAGUE = 1;

    private static final String SAVED_SHOWING_ACCOUNTS = "showing_accounts";

    private AccountManager mAccountManager;
    private ListFragment mListFragment;
    private LeagueAdapter mLeagueAdapter;
    private AccountAdapter mAccountAdapter;
    private Button mAddAccountButton;

    /**
     * If true, we are currently showing the accounts list.
     * Otherwise, we are showing the leagues list.
     */
    private boolean mShowingAccounts;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mListFragment = (ListFragment) getSupportFragmentManager().findFragmentById(R.id.list);
        mListFragment.getListView().setOnItemClickListener(this);

        mAddAccountButton = (Button) findViewById(R.id.add_account);
        mAddAccountButton.setOnClickListener(this);

        mLeagueAdapter = new LeagueAdapter();
        mAccountAdapter = new AccountAdapter();

        initAccountManager();

        if (savedInstanceState != null) {
            mShowingAccounts = savedInstanceState.getBoolean(SAVED_SHOWING_ACCOUNTS);
        }

        updateActiveList(false);

        getSupportLoaderManager().initLoader(LOADER_LEAGUE, null, this);
    }

    @Override
    public void onDestroy() {
        cleanupAccountManager();
        super.onDestroy();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SAVED_SHOWING_ACCOUNTS, mShowingAccounts);
    }

    @Override
    public Loader<League[]> onCreateLoader(int id, Bundle args) {
        if (id == LOADER_LEAGUE) {
            return new LeagueLoader(this);
        } else {
            throw new IllegalArgumentException("Unknown loader ID: " + id);
        }
    }

    @Override
    public void onLoadFinished(Loader<League[]> loader, League[] leagues) {
        // Update the adapters.
        mAccountAdapter.setLeagues(leagues);
        mLeagueAdapter.clear();
        mLeagueAdapter.addAll(leagues);

        // Update the UI with the new adapters.
        updateActiveList(true);
        supportInvalidateOptionsMenu();

        mAddAccountButton.setVisibility(View.VISIBLE);
    }

    @Override public void onLoaderReset(Loader<League[]> loader) {}

    @Override
    public void onClick(View v) {
        if (v == mAddAccountButton) {
            addAccount();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem item = menu.findItem(R.id.toggle_list);
        // Toggle menu item should offer the opposite view to what's currently showing.
        if (mShowingAccounts) {
            item.setTitle(R.string.leagues);
        } else {
            item.setTitle(R.string.accounts);
        }
        item.setEnabled(mListFragment.getListAdapter() != null);
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.toggle_list) {
            mShowingAccounts = !mShowingAccounts;
            // The toggle_list menu item is only enabled after loading completes.
            updateActiveList(true);
            supportInvalidateOptionsMenu();
            return true;
        } else if (itemId == R.id.refresh) {
            refresh();
            return true;
        } else if (itemId == R.id.sync_interval) {
            SyncIntervalDialogFragment.show(getSupportFragmentManager());
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        if (mShowingAccounts) {
            Account account = mAccountAdapter.getItem(position);
            AccountInfoDialogFragment.show(getSupportFragmentManager(), account,
                    mAccountAdapter.getLeagueNames(account));
        }
    }

    @Override
    public void onAccountsUpdated(Account[] accounts) {
        mAccountAdapter.clear();
        for (Account account : accounts) {
            if (AccountAuthenticator.ACCOUNT_TYPE_YAHOO.equals(account.type)) {
                mAccountAdapter.add(account);
            }
        }
    }

    private void updateActiveList(boolean loadComplete) {
        // Only set the adapter if loading has finished, since the presence of an adapter is what
        // signals the ListFragment to stop showing a progress bar.
        if (loadComplete) {
            mListFragment.setListAdapter(mShowingAccounts ? mAccountAdapter : mLeagueAdapter);
        }

        getSupportActionBar().setSubtitle(mShowingAccounts ? R.string.accounts : R.string.leagues);

        ListView listView = mListFragment.getListView();
        listView.setEnabled(mShowingAccounts);
        listView.setLongClickable(mShowingAccounts);
    }

    @VisibleForTesting
    protected void initAccountManager() {
        mAccountManager = AccountManager.get(this);
        mAccountManager.addOnAccountsUpdatedListener(this, null, true);
    }

    @VisibleForTesting
    protected void addAccount() {
        mAccountManager.addAccount(AccountAuthenticator.ACCOUNT_TYPE_YAHOO,
                AccountAuthenticator.TOKEN_TYPE_OAUTH, null, null, this, null, null);
    }

    @VisibleForTesting
    protected void cleanupAccountManager() {
        mAccountManager.removeOnAccountsUpdatedListener(this);
    }

    @VisibleForTesting
    protected void refresh() {
        SyncAdapter.requestManualSync(this);
    }

    private class LeagueAdapter extends ArrayAdapter<League> {
        LeagueAdapter() {
            super(MainActivity.this, 0);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        android.R.layout.simple_list_item_1, parent, false);
            }
            TextView text = (TextView) convertView.findViewById(android.R.id.text1);
            text.setText(getItem(position).league_name);
            return convertView;
        }
    }

    private class AccountAdapter extends ArrayAdapter<Account> {
        private final Map<String, ArrayList<String>> mAccountNameToLeagueNamesMap;

        AccountAdapter() {
            super(MainActivity.this, 0);
            mAccountNameToLeagueNamesMap = new HashMap<>();
            mListFragment.setEmptyText(getString(R.string.empty_text_no_account));
        }

        @Override
        public void clear() {
            super.clear();
            mListFragment.setEmptyText(getString(R.string.empty_text_no_account));
        }

        @Override
        public void add(Account account) {
            super.add(account);
            mListFragment.setEmptyText(getString(R.string.empty_text_no_leagues));
        }

        void setLeagues(League[] leagues) {
            // Build a map from account name to the leagues associated with that account.
            mAccountNameToLeagueNamesMap.clear();
            for (League league : leagues) {
                ArrayList<String> leagueList =
                        mAccountNameToLeagueNamesMap.get(league.account_name);
                if (leagueList == null) {
                    leagueList = new ArrayList<>();
                    mAccountNameToLeagueNamesMap.put(league.account_name, leagueList);
                }
                leagueList.add(league.league_name);
            }

            // Alphabetically sort the leagues for better presentation.
            for (ArrayList<String> leagueList : mAccountNameToLeagueNamesMap.values()) {
                Collections.sort(leagueList);
            }

            // Invalidate the data set because for each account item, we render the leagues on that
            // account, and these may have changed.
            notifyDataSetChanged();
        }

        ArrayList<String> getLeagueNames(Account account) {
            return mAccountNameToLeagueNamesMap.get(account.name);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = getLayoutInflater().inflate(
                        android.R.layout.simple_list_item_2, parent, false);
            }
            TextView text1 = (TextView) convertView.findViewById(android.R.id.text1);
            TextView text2 = (TextView) convertView.findViewById(android.R.id.text2);
            String accountName = getItem(position).name;
            List<String> leagues = mAccountNameToLeagueNamesMap.get(accountName);
            text1.setText(getString(R.string.yahoo_id, accountName));
            if (leagues == null || leagues.isEmpty()) {
                text2.setText(R.string.leagues_list_empty);
            } else {
                text2.setText(getString(R.string.leagues_list, TextUtils.join(
                        getString(R.string.league_name_separator), leagues)));
            }
            return convertView;
        }
    }

    @VisibleForTesting
    static class LeagueLoader extends AsyncTaskLoader<League[]> {
        private volatile League[] mLeagues;

        protected LeagueLoader(Context context) {
            super(context);
            context.getContentResolver().registerContentObserver(
                    LeagueTable.CONTENT_URI, false, new ForceLoadContentObserver());
        }

        @Override
        public void onStartLoading() {
            if (mLeagues != null) {
                deliverResult(mLeagues);
            }
            if (takeContentChanged() || mLeagues == null) {
                forceLoad();
            }
        }

        @Override
        public League[] loadInBackground() {
            return mLeagues = LeagueTable.getLeagues(getContext());
        }
    }
}
