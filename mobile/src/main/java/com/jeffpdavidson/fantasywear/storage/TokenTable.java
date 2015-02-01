package com.jeffpdavidson.fantasywear.storage;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.jeffpdavidson.fantasywear.annotations.VisibleForTesting;
import com.jeffpdavidson.fantasywear.api.auth.Token;

/**
 * SQLite table which contains the user's auth credentials.
 *
 * This class should generally not be used directly to obtain and manage auth tokens; instead, use
 * AccountManager's methods, which call through to here.
 */
public final class TokenTable {
    static final String TABLE_NAME = "tokens";
    static final String COLUMN_ACCOUNT_NAME = "account_name";
    private static final String COLUMN_TOKEN = "token";
    private static final String COLUMN_TOKEN_SECRET = "token_secret";
    private static final String COLUMN_SESSION_HANDLE = "session_handle";
    private static final String COLUMN_EXPIRATION_TIME_SEC = "expiration_time_sec";
    private static final String COLUMN_AUTHORIZATION_EXPIRATION_TIME_SEC =
            "authorization_expiration_time_sec";
    private static final String COLUMN_LAST_SYNC_TIME_SEC = "last_sync_time_sec";

    private TokenTable() {}

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " " + "("
                + COLUMN_ACCOUNT_NAME + " TEXT PRIMARY KEY, "
                + COLUMN_TOKEN + " TEXT, "
                + COLUMN_TOKEN_SECRET + " TEXT, "
                + COLUMN_SESSION_HANDLE + " TEXT, "
                + COLUMN_EXPIRATION_TIME_SEC + " INTEGER, "
                + COLUMN_AUTHORIZATION_EXPIRATION_TIME_SEC + " INTEGER, "
                + COLUMN_LAST_SYNC_TIME_SEC + " INTEGER"
                + ");");
    }

    private static final String[] TOKEN_COLUMNS = new String[] {
            COLUMN_TOKEN,
            COLUMN_TOKEN_SECRET,
            COLUMN_SESSION_HANDLE,
            COLUMN_EXPIRATION_TIME_SEC,
            COLUMN_AUTHORIZATION_EXPIRATION_TIME_SEC,
    };

    public static Token getToken(Context context, Account account) {
        Token.Builder tokenBuilder = new Token.Builder();

        SQLiteDatabase db = FWSQLiteDatabase.getInstance(context).getReadableDatabase();
        Cursor cur = null;
        try {
            cur = db.query(TABLE_NAME, TOKEN_COLUMNS, COLUMN_ACCOUNT_NAME + "=?",
                    new String[]{account.name}, null, null, null);
            if (cur.moveToFirst()) {
                tokenBuilder.token(cur.getString(0))
                        .token_secret(cur.getString(1))
                        .session_handle(cur.getString(2))
                        .expiration_time_sec(cur.getLong(3))
                        .authorization_expiration_time_sec(cur.getLong(4));
            }
            return tokenBuilder.build();
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    public static void insertToken(Context context, Account account, Token token) {
        SQLiteDatabase db = FWSQLiteDatabase.getInstance(context).getWritableDatabase();
        ContentValues values = toContentValues(account, token);
        db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static void updateToken(Context context, Account account, Token token) {
        SQLiteDatabase db = FWSQLiteDatabase.getInstance(context).getWritableDatabase();
        ContentValues values = toContentValues(account, token);
        db.update(TABLE_NAME, values, COLUMN_ACCOUNT_NAME + "=?", new String[] { account.name });
    }

    public static void invalidateToken(Context context, Account account, Token token) {
        SQLiteDatabase db = FWSQLiteDatabase.getInstance(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_EXPIRATION_TIME_SEC, 0L);
        db.update(TABLE_NAME, values, COLUMN_ACCOUNT_NAME + "=? AND " + COLUMN_TOKEN + "=? AND "
                        + COLUMN_TOKEN_SECRET + "=?",
                new String[] { account.name, token.token, token.token_secret });
    }

    public static void cleanUnusedTokens(Context context, Account[] currentAccounts) {
        SQLiteDatabase db = FWSQLiteDatabase.getInstance(context).getWritableDatabase();
        if (currentAccounts.length == 0) {
            // Clear all rows.
            db.delete(TABLE_NAME, null, null);
        } else {
            StringBuilder where = new StringBuilder(COLUMN_ACCOUNT_NAME + " NOT IN (");
            String[] whereArgs = new String[currentAccounts.length];
            for (int i = 0; i < currentAccounts.length; i++) {
                if (i > 0) {
                    where.append(',');
                }
                where.append('?');
                whereArgs[i] = currentAccounts[i].name;
            }
            where.append(')');
            db.delete(TABLE_NAME, where.toString(), whereArgs);
        }

        // Since deleting rows from this table can delete leagues by cascade, notify any observers
        // of the league table that the data may have changed.
        LeagueTable.notifyChange(context);
    }

    public static long getLastSyncTimeSec(Context context, Account account) {
        SQLiteDatabase db = FWSQLiteDatabase.getInstance(context).getReadableDatabase();
        Cursor cur = null;
        try {
            cur = db.query(TABLE_NAME, new String[] { COLUMN_LAST_SYNC_TIME_SEC },
                    COLUMN_ACCOUNT_NAME + "=?", new String[] { account.name }, null, null, null);
            if (cur.moveToFirst()) {
                return cur.getLong(0);
            }
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
        return 0;
    }

    public static void setLastSyncTimeSec(Context context, Account account, long lastSyncTimeSec) {
        SQLiteDatabase db = FWSQLiteDatabase.getInstance(context).getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COLUMN_LAST_SYNC_TIME_SEC, lastSyncTimeSec);
        db.update(TABLE_NAME, values, COLUMN_ACCOUNT_NAME + "=?", new String[] { account.name });
    }

    @VisibleForTesting
    static void clear(Context context) {
        FWSQLiteDatabase.getInstance(context).getWritableDatabase().delete(TABLE_NAME, null, null);
    }

    private static ContentValues toContentValues(Account account, Token token) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_ACCOUNT_NAME, account.name);
        values.put(COLUMN_TOKEN, token.token);
        values.put(COLUMN_TOKEN_SECRET, token.token_secret);
        values.put(COLUMN_SESSION_HANDLE, token.session_handle);
        values.put(COLUMN_EXPIRATION_TIME_SEC, token.expiration_time_sec);
        values.put(COLUMN_AUTHORIZATION_EXPIRATION_TIME_SEC,
                token.authorization_expiration_time_sec);
        return values;
    }
}
