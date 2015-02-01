package com.jeffpdavidson.fantasywear.protocol;

import android.accounts.Account;
import android.net.Uri;
import android.support.test.runner.AndroidJUnit4;

import com.google.android.gms.wearable.DataItem;
import com.jeffpdavidson.fantasywear.api.model.League;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.util.HashSet;
import java.util.Set;

@RunWith(AndroidJUnit4.class)
public class LeagueDataTest {
    private static final Uri URI = LeagueData.getLeagueUri(new Account("ACCOUNT", "TYPE"),
            new League.Builder().league_key("LEAGUE").build());

    @Mock private DataItem mMockDataItem;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void getTagIfMatchesForValidUri() {
        Assert.assertEquals("ACCOUNT/LEAGUE", LeagueData.getTagIfMatches(URI));
    }

    @Test
    public void getTagIfMatchesForInvalidUri() {
        Assert.assertNull(LeagueData.getTagIfMatches(Uri.parse("test/ACCOUNT/LEAGUE")));
        Assert.assertNull(LeagueData.getTagIfMatches(Uri.parse("league/ACCOUNT/LEAGUE/EXTRA")));
    }

    @Test
    public void isActiveLeagueDataItemForActiveUri() {
        Mockito.when(mMockDataItem.getUri()).thenReturn(URI);
        Set<String> accountNames = new HashSet<>();
        accountNames.add("ACCOUNT");
        Assert.assertTrue(LeagueData.isActiveLeagueDataItem(mMockDataItem, accountNames));
    }

    @Test
    public void isActiveLeagueDataItemForInvalidUri() {
        Mockito.when(mMockDataItem.getUri()).thenReturn(Uri.parse("invalid"));
        Set<String> accountNames = new HashSet<>();
        accountNames.add("ACCOUNT");
        Assert.assertFalse(LeagueData.isActiveLeagueDataItem(mMockDataItem, accountNames));
    }

    @Test
    public void isActiveLeagueDataItemForMissingAccount() {
        Mockito.when(mMockDataItem.getUri()).thenReturn(URI);
        Set<String> accountNames = new HashSet<>();
        accountNames.add("ACCOUNT2");
        Assert.assertFalse(LeagueData.isActiveLeagueDataItem(mMockDataItem, accountNames));
    }
}
