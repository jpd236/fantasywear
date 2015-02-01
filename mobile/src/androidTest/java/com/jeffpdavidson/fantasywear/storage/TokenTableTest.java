package com.jeffpdavidson.fantasywear.storage;

import android.accounts.Account;
import android.content.Context;
import android.test.AndroidTestCase;
import android.test.RenamingDelegatingContext;

import com.jeffpdavidson.fantasywear.api.auth.Token;
import com.jeffpdavidson.fantasywear.api.auth.Token.Builder;

public class TokenTableTest extends AndroidTestCase {
    private static final Account ACCOUNT = new Account("name", "type");
    private static final Account ACCOUNT2 = new Account("name2", "type");
    private static final Account ACCOUNT3 = new Account("name3", "type");

    private Context mTestContext;

    @Override
    public void setUp() throws Exception {
        super.setUp();
        mTestContext = new RenamingDelegatingContext(getContext(), "test");
    }

    @Override
    public void tearDown() throws Exception {
        TokenTable.clear(mTestContext);
    }

    public void testGetToken() {
        Token token = new Builder()
                .token("aaa")
                .token_secret("bbb")
                .expiration_time_sec(12345L)
                .authorization_expiration_time_sec(54321L)
                .build();
        TokenTable.insertToken(mTestContext, ACCOUNT, token);
        assertEquals(token, TokenTable.getToken(mTestContext, ACCOUNT));
    }

    public void testInvalidateToken() {
        Token token = new Builder()
                .token("aaa")
                .token_secret("bbb")
                .expiration_time_sec(12345L)
                .authorization_expiration_time_sec(54321L)
                .build();
        TokenTable.insertToken(mTestContext, ACCOUNT, token);
        TokenTable.invalidateToken(mTestContext, ACCOUNT, token);
        Token expectedToken = new Token.Builder(token).expiration_time_sec(0L).build();
        assertEquals(expectedToken, TokenTable.getToken(mTestContext, ACCOUNT));
    }

    public void testCleanUnusedTokens_clearAll() {
        Token token = new Builder()
                .token("aaa")
                .token_secret("bbb")
                .expiration_time_sec(12345L)
                .authorization_expiration_time_sec(54321L)
                .build();
        TokenTable.insertToken(mTestContext, ACCOUNT, token);
        TokenTable.cleanUnusedTokens(mTestContext, new Account[] {});
        assertEquals(new Token.Builder().build(), TokenTable.getToken(mTestContext, ACCOUNT));
    }

    public void testCleanUnusedToken_clearSome() {
        TokenTable.insertToken(mTestContext, ACCOUNT, new Token.Builder().token("1").build());
        TokenTable.insertToken(mTestContext, ACCOUNT2, new Token.Builder().token("2").build());
        TokenTable.insertToken(mTestContext, ACCOUNT3, new Token.Builder().token("3").build());
        TokenTable.cleanUnusedTokens(mTestContext, new Account[]{ACCOUNT, ACCOUNT3});
        assertEquals("1", TokenTable.getToken(mTestContext, ACCOUNT).token);
        assertEquals("3", TokenTable.getToken(mTestContext, ACCOUNT3).token);
        assertNull(TokenTable.getToken(mTestContext, ACCOUNT2).token);
    }
}
