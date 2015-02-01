package com.jeffpdavidson.fantasywear.storage;

import android.accounts.Account;
import android.content.Context;
import android.test.AndroidTestCase;
import android.test.MoreAsserts;
import android.test.RenamingDelegatingContext;

import com.jeffpdavidson.fantasywear.api.auth.Token;
import com.jeffpdavidson.fantasywear.api.model.League;

public class LeagueTableTest extends AndroidTestCase {
    private static final Account ACCOUNT = new Account("name", "type");
    private static final Account ACCOUNT2 = new Account("name2", "type");

    private Context mTestContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mTestContext = new RenamingDelegatingContext(getContext(), "test");
        TokenTable.insertToken(mTestContext, ACCOUNT, new Token.Builder().build());
        TokenTable.insertToken(mTestContext, ACCOUNT2, new Token.Builder().build());
    }

    @Override
    public void tearDown() throws Exception {
        LeagueTable.clear(mTestContext);
        TokenTable.clear(mTestContext);
    }

    public void testUpdateLeagues_initialInsert() {
        League[] leagues = new League[] {
                new League.Builder()
                        .account_name(ACCOUNT.name)
                        .league_key("key")
                        .league_name("name")
                        .build(),
        };
        LeagueTable.updateLeagues(mTestContext, ACCOUNT, leagues);
        MoreAsserts.assertEquals(leagues, LeagueTable.getLeagues(mTestContext, ACCOUNT));
    }

    public void testUpdateLeagues_clearAll() {
        League[] leagues = new League[] {
                new League.Builder()
                        .account_name(ACCOUNT.name)
                        .league_key("key")
                        .league_name("name")
                        .build(),
        };
        League[] leagues2 = new League[] {
                new League.Builder()
                        .account_name(ACCOUNT2.name)
                        .league_key("key")
                        .league_name("name")
                        .build(),
        };
        League[] noLeagues = new League[] {};
        LeagueTable.updateLeagues(mTestContext, ACCOUNT, leagues);
        LeagueTable.updateLeagues(mTestContext, ACCOUNT2, leagues2);
        LeagueTable.updateLeagues(mTestContext, ACCOUNT, noLeagues);
        MoreAsserts.assertEquals(noLeagues, LeagueTable.getLeagues(mTestContext, ACCOUNT));
        MoreAsserts.assertEquals(leagues2, LeagueTable.getLeagues(mTestContext, ACCOUNT2));
    }

    public void testUpdateLeagues_clearSome() {
        League[] leagues = new League[] {
                new League.Builder()
                        .account_name(ACCOUNT.name)
                        .league_key("key")
                        .league_name("name")
                        .build(),
                new League.Builder()
                        .account_name(ACCOUNT.name)
                        .league_key("key2")
                        .league_name("name")
                        .build(),
        };
        League[] leagues2 = new League[] {
                new League.Builder()
                        .account_name(ACCOUNT2.name)
                        .league_key("key")
                        .league_name("name")
                        .build(),
        };
        League[] updatedLeagues = new League[] {
                new League.Builder()
                        .account_name(ACCOUNT.name)
                        .league_key("key")
                        .league_name("name")
                        .build(),
                new League.Builder()
                        .account_name(ACCOUNT.name)
                        .league_key("key3")
                        .league_name("name")
                        .build(),
        };
        LeagueTable.updateLeagues(mTestContext, ACCOUNT, leagues);
        LeagueTable.updateLeagues(mTestContext, ACCOUNT2, leagues2);
        LeagueTable.updateLeagues(mTestContext, ACCOUNT, updatedLeagues);
        MoreAsserts.assertEquals(updatedLeagues, LeagueTable.getLeagues(mTestContext, ACCOUNT));
        MoreAsserts.assertEquals(leagues2, LeagueTable.getLeagues(mTestContext, ACCOUNT2));
    }

    public void testDeleteAccount() {
        League[] leagues = new League[] {
                new League.Builder()
                        .account_name(ACCOUNT.name)
                        .league_key("key")
                        .league_name("name")
                        .build(),
        };
        LeagueTable.updateLeagues(mTestContext, ACCOUNT, leagues);
        TokenTable.clear(mTestContext);
        MoreAsserts.assertEquals(new League[] {}, LeagueTable.getLeagues(mTestContext, ACCOUNT));
    }
}
