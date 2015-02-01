package com.jeffpdavidson.fantasywear.activities;

import android.accounts.Account;
import android.content.Intent;
import android.content.res.Configuration;
import android.test.ActivityInstrumentationTestCase2;

import com.jeffpdavidson.fantasywear.R;
import com.jeffpdavidson.fantasywear.api.auth.AccountAuthenticator;
import com.jeffpdavidson.fantasywear.api.model.League;
import com.jeffpdavidson.fantasywear.fragments.AccountInfoDialogFragment;
import com.jeffpdavidson.fantasywear.fragments.SyncIntervalDialogFragment;
import com.robotium.solo.Condition;
import com.robotium.solo.Solo;

import java.util.concurrent.TimeUnit;

/**
 * UI tests for FantasyWear's main activity.
 *
 * NOTE: This test will fail if the screen is off or locked. Also, for simplicity, this test uses
 * JUnit3 as ActivityInstrumentationTestCase2 does not work as well with JUnit4.
 */
public class MainActivityTest extends ActivityInstrumentationTestCase2<TestMainActivity> {
    private static final int WAIT_TIMEOUT_MS = 10000;

    private static final Account ACCOUNT_ABCDE =
            new Account("ABCDE", AccountAuthenticator.ACCOUNT_TYPE_YAHOO);
    private static final Account ACCOUNT_FGHIJ =
            new Account("FGHIJ", AccountAuthenticator.ACCOUNT_TYPE_YAHOO);
    private static final Account ACCOUNT_KLMNO =
            new Account("KLMNO", AccountAuthenticator.ACCOUNT_TYPE_YAHOO);

    private static final League LEAGUE_ABCDE_1 = new League.Builder()
            .account_name("ABCDE")
            .league_key("1")
            .league_name("League 1")
            .build();
    private static final League LEAGUE_ABCDE_2 = new League.Builder()
            .account_name("ABCDE")
            .league_key("1")
            .league_name("League 2")
            .build();
    private static final League LEAGUE_FGHIJ_3 = new League.Builder()
            .account_name("FGHIJ")
            .league_key("3")
            .league_name("League 3")
            .build();

    private Solo mSolo;

    public MainActivityTest() {
        super(TestMainActivity.class);
    }

    /** Verify the empty text that appears when no accounts are present. */
    public void testNoAccounts() throws Exception {
        startActivity(new League[] {}, new Account[] {});
        assertTrue(mSolo.searchText(getString(R.string.empty_text_no_account, true)));
    }

    /** Verify the empty text that appears when there are no leagues for the present accounts. */
    public void testNoLeaguesForAccount() throws Exception {
        startActivity(new League[]{}, new Account[]{ACCOUNT_ABCDE});
        assertTrue(mSolo.searchText(getString(R.string.empty_text_no_leagues, true)));
    }

    /** Verify that the list of leagues is rendered correctly. */
    public void testLeagueList() throws Exception {
        startActivity(new League[]{LEAGUE_ABCDE_1, LEAGUE_ABCDE_2, LEAGUE_FGHIJ_3},
                new Account[]{ACCOUNT_ABCDE, ACCOUNT_FGHIJ, ACCOUNT_KLMNO});
        assertEquals(getString(R.string.leagues),
                getActivity().getSupportActionBar().getSubtitle());
        assertTrue(mSolo.searchText("League 1", true));
        assertTrue(mSolo.searchText("League 2", true));
        assertTrue(mSolo.searchText("League 3", true));
    }

    /** Verify that the account list can be opened and is rendered correctly. */
    public void testAccountList() throws Exception {
        startActivity(new League[]{LEAGUE_ABCDE_1, LEAGUE_ABCDE_2, LEAGUE_FGHIJ_3},
                new Account[]{ACCOUNT_ABCDE, ACCOUNT_FGHIJ, ACCOUNT_KLMNO});
        openAccounts();
        assertTrue(mSolo.searchText("ABCDE", true));
        assertTrue(mSolo.searchText(getString(R.string.leagues_list,
                "League 1" + getString(R.string.league_name_separator) + "League 2"), true));
        assertTrue(mSolo.searchText("FGHIJ", true));
        assertTrue(mSolo.searchText(getString(R.string.leagues_list, "League 3"), true));
        assertTrue(mSolo.searchText("KLMNO", true));
        assertTrue(mSolo.searchText(getString(R.string.leagues_list_empty), true));
    }

    /** Verify that the add account button is functional. */
    public void testAddAccount() throws Exception {
        startActivity(new League[] {}, new Account[] {});
        mSolo.clickOnButton(getString(R.string.add_account));
        assertTrue(getActivity().awaitAddAccount(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /** Verify that clicking an account launches the info dialog. */
    public void testShowAccountInfo() throws Exception {
        startActivity(new League[]{LEAGUE_ABCDE_1}, new Account[]{ACCOUNT_ABCDE});
        openAccounts();
        mSolo.clickInList(0);
        assertTrue(mSolo.waitForFragmentByTag(AccountInfoDialogFragment.TAG));
    }

    /** Verify that the refresh menu item is functional. */
    public void testRefresh() throws Exception {
        startActivity(new League[]{}, new Account[]{});
        mSolo.clickOnMenuItem(getString(R.string.refresh));
        assertTrue(getActivity().awaitRefresh(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    /** Verify that clicking the sync interval menu item launches the preference dialog. */
    public void testSetSyncInterval() throws Exception {
        startActivity(new League[]{}, new Account[]{});
        mSolo.clickOnMenuItem(getString(R.string.sync_interval));
        assertTrue(mSolo.waitForFragmentByTag(SyncIntervalDialogFragment.TAG, WAIT_TIMEOUT_MS));
    }

    /** Verify that the account list is still shown after a screen rotation. */
    public void testAccountListScreenRotation() throws Exception {
        startActivity(new League[] { LEAGUE_ABCDE_1 },
                new Account[] { ACCOUNT_ABCDE });
        openAccounts();
        int orientation = getActivity().getResources().getConfiguration().orientation;
        if (orientation == Configuration.ORIENTATION_PORTRAIT) {
            mSolo.setActivityOrientation(Solo.LANDSCAPE);
        } else if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
            mSolo.setActivityOrientation(Solo.PORTRAIT);
        } else {
            fail("Unknown orientation: " + orientation);
        }
        setActivity(mSolo.getCurrentActivity());
        getActivity().awaitLoaded(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        assertEquals(getString(R.string.accounts),
                getActivity().getSupportActionBar().getSubtitle());
        assertTrue(mSolo.searchText("ABCDE", true));
        assertTrue(mSolo.searchText(getString(R.string.leagues_list, "League 1"), true));
    }

    private void startActivity(League[] leagues, Account[] accounts) throws InterruptedException {
        Intent intent = TestMainActivity.makeIntent(leagues, accounts);
        setActivityIntent(intent);
        TestMainActivity activity = getActivity();
        mSolo = new Solo(getInstrumentation(), activity);
        assertTrue(activity.awaitLoaded(WAIT_TIMEOUT_MS, TimeUnit.MILLISECONDS));
    }

    private void openAccounts() {
        mSolo.clickOnMenuItem(getString(R.string.accounts));
        assertTrue(mSolo.waitForCondition(new Condition() {
            @Override
            public boolean isSatisfied() {
                return getString(R.string.accounts).equals(
                        getActivity().getSupportActionBar().getSubtitle());
            }
        }, WAIT_TIMEOUT_MS));
    }

    private String getString(int resId, Object... formatArgs) {
        return getActivity().getString(resId, formatArgs);
    }
}
