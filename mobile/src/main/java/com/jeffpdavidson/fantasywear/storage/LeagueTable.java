package com.jeffpdavidson.fantasywear.storage;

import android.accounts.Account;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import com.jeffpdavidson.fantasywear.annotations.VisibleForTesting;
import com.jeffpdavidson.fantasywear.api.model.League;
import com.jeffpdavidson.fantasywear.api.model.League.Builder;
import com.jeffpdavidson.fantasywear.sync.SyncProvider;

/**
 * SQLite table which contains metadata about each of the user's leagues.
 */
public final class LeagueTable {
    public static final Uri CONTENT_URI =
            Uri.parse("content://" + SyncProvider.AUTHORITY + "/leagues");

    private static final String TABLE_NAME = "leagues";
    private static final String COLUMN_ACCOUNT_NAME = "account_name";
    private static final String COLUMN_LEAGUE_KEY = "league_key";
    private static final String COLUMN_LEAGUE_NAME = "league_name";

    private LeagueTable() {}

    static void createTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_NAME + " " + "("
                + COLUMN_ACCOUNT_NAME + " TEXT REFERENCES " + TokenTable.TABLE_NAME
                        + "(" + TokenTable.COLUMN_ACCOUNT_NAME + ") ON DELETE CASCADE, "
                + COLUMN_LEAGUE_KEY + " TEXT, "
                + COLUMN_LEAGUE_NAME + " TEXT, "
                + "PRIMARY KEY(" + COLUMN_ACCOUNT_NAME + "," + COLUMN_LEAGUE_KEY + ")"
                + ");");
    }

    private static final String[] LEAGUE_COLUMNS = new String[] {
            COLUMN_ACCOUNT_NAME,
            COLUMN_LEAGUE_KEY,
            COLUMN_LEAGUE_NAME,
    };

    /** Get all leagues for all accounts on the device. */
    public static League[] getLeagues(Context context) {
        return getLeagues(context, null, null);
    }

    /** Get all leagues for the provided account. */
    public static League[] getLeagues(Context context, Account account) {
        return getLeagues(context, COLUMN_ACCOUNT_NAME + "=?", new String[] { account.name });
    }

    private static League[] getLeagues(Context context, String selection, String[] selectionArgs) {
        SQLiteDatabase db = FWSQLiteDatabase.getInstance(context).getReadableDatabase();
        Cursor cur = null;
        try {
            cur = db.query(TABLE_NAME, LEAGUE_COLUMNS, selection, selectionArgs, null, null, null);
            League[] leagues = new League[cur.getCount()];
            int i = 0;
            while (cur.moveToNext()) {
                leagues[i++] = new Builder()
                        .account_name(cur.getString(0))
                        .league_key(cur.getString(1))
                        .league_name(cur.getString(2))
                        .build();
            }
            return leagues;
        } finally {
            if (cur != null) {
                cur.close();
            }
        }
    }

    /** Update all leagues for the provided account. Stale leagues are removed. */
    public static void updateLeagues(Context context, Account account, League[] leagues) {
        SQLiteDatabase db = FWSQLiteDatabase.getInstance(context).getWritableDatabase();
        db.beginTransaction();
        try {
            // Upsert the provided leagues.
            for (League league : leagues) {
                ContentValues values = new ContentValues();
                values.put(COLUMN_ACCOUNT_NAME, account.name);
                values.put(COLUMN_LEAGUE_KEY, league.league_key);
                values.put(COLUMN_LEAGUE_NAME, league.league_name);
                // Safe as the provided leagues include data for all columns.
                db.insertWithOnConflict(TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }

            // Clear any leagues not provided.
            StringBuilder where = new StringBuilder(COLUMN_ACCOUNT_NAME + "=?");
            if (leagues.length == 0) {
                // Clear all rows for the given account.
                db.delete(TABLE_NAME, where.toString(), new String[] { account.name });
            } else {
                // Delete rows whose league key is not one of the ones we just upserted.
                where.append(" AND " + COLUMN_LEAGUE_KEY + " NOT IN (");
                String[] whereArgs = new String[leagues.length + 1];
                whereArgs[0] = account.name;
                for (int i = 0; i < leagues.length; i++) {
                    if (i > 0) {
                        where.append(',');
                    }
                    where.append('?');
                    whereArgs[i + 1] = leagues[i].league_key;
                }
                where.append(')');
                db.delete(TABLE_NAME, where.toString(), whereArgs);
            }
            db.setTransactionSuccessful();
            notifyChange(context);
        } finally {
            db.endTransaction();
        }
    }

    @VisibleForTesting
    static void clear(Context context) {
        FWSQLiteDatabase.getInstance(context).getWritableDatabase().delete(TABLE_NAME, null, null);
    }

    static void notifyChange(Context context) {
        context.getContentResolver().notifyChange(CONTENT_URI, null);
    }
}
