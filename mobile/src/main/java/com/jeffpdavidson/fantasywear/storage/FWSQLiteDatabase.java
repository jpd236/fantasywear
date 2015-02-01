package com.jeffpdavidson.fantasywear.storage;

import android.annotation.TargetApi;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Build;

/** Data store for FantasyWear. */
public class FWSQLiteDatabase extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "fantasywear.db";
    private static final int DATABASE_VERSION = 1;

    private static volatile FWSQLiteDatabase sInstance;

    private FWSQLiteDatabase(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static FWSQLiteDatabase getInstance(Context context) {
        if (sInstance == null) {
            synchronized (FWSQLiteDatabase.class) {
                if (sInstance == null) {
                    sInstance = new FWSQLiteDatabase(context);
                }
            }
        }
        return sInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        TokenTable.createTable(db);
        LeagueTable.createTable(db);
    }

    @TargetApi(16)
    @Override
    public void onConfigure(SQLiteDatabase db) {
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onOpen(SQLiteDatabase db) {
        // onConfigure and SQLiteDatabase#setForeignKeyConstraintsEnabled were added in API 16.
        // Perform the manual command in onOpen for earlier Android versions.
        if (Build.VERSION.SDK_INT < 16) {
            db.execSQL("PRAGMA foreign_keys = ON;");
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new UnsupportedOperationException("DB version changes are not implemented");
    }
}
