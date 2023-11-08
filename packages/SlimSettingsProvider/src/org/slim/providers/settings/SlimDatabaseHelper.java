/**
 * Copyright (c) 2015, The CyanogenMod Project
 * Copyright (c) 2016-2017 SlimRoms Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.slim.providers.settings;

import android.Manifest;
import android.app.AppGlobals;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.UserInfo;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteDoneException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import android.os.Build;
import android.os.Environment;
import android.os.RemoteException;
import android.os.SystemProperties;
import android.os.UserHandle;
import android.os.UserManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import slim.provider.SlimSettings;

/**
 * The SlimDatabaseHelper allows creation of a database to store Slim specific settings for a user
 * in System, Secure, and Global tables.
 */
public class SlimDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = "SlimDatabaseHelper";
    private static final boolean LOCAL_LOGV = false;

    private static final String DATABASE_NAME = "slim_settings.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_SYSTEM = "system";
    public static final String TABLE_SECURE = "secure";
    public static final String TABLE_GLOBAL = "global";

    private static final String CREATE_TABLE_SQL_FORMAT = "CREATE TABLE %s (" +
            "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
            "name TEXT UNIQUE ON CONFLICT REPLACE," +
            "value TEXT" +
            ");)";

    private static final String CREATE_INDEX_SQL_FORMAT = "CREATE INDEX %sIndex%d ON %s (name);";

    private static final String DROP_TABLE_SQL_FORMAT = "DROP TABLE IF EXISTS %s;";

    private static final String DROP_INDEX_SQL_FORMAT = "DROP INDEX IF EXISTS %sIndex%d;";

    private static final String MCC_PROP_NAME = "ro.prebundled.mcc";

    private Context mContext;
    private int mUserHandle;
    private String mPublicSrcDir;

    /**
     * Gets the appropriate database path for a specific user
     * @param userId The database path for this user
     * @return The database path string
     */
    private static String dbNameForUser(Context context, int userId, String baseName) {
        if (userId == UserHandle.USER_OWNER) {
            return context.getDatabasePath(baseName).getPath();
        } else {
            // Place the database in the user-specific data tree so that it's
            // cleaned up automatically when the user is deleted.
            File databaseFile = new File(
                    Environment.getUserSystemDirectory(userId), baseName);
            return databaseFile.getPath();
        }
    }

    /**
     * Creates an instance of {@link SlimDatabaseHelper}
     * @param context
     * @param userId
     */
    public SlimDatabaseHelper(Context context, int userId) {
        super(context, dbNameForUser(context, userId, DATABASE_NAME), null, DATABASE_VERSION);
        mContext = context;
        mUserHandle = userId;

        try {
            String packageName = mContext.getPackageName();
            mPublicSrcDir = mContext.getPackageManager().getApplicationInfo(packageName, 0)
                    .publicSourceDir;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    /**
     * Creates System, Secure, and Global tables in the specified {@link SQLiteDatabase} and loads
     * default values into the created tables.
     * @param db The database.
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.beginTransaction();

        try {
            createDbTable(db, TABLE_SYSTEM);
            createDbTable(db, TABLE_SECURE);

            if (mUserHandle == UserHandle.USER_OWNER) {
                createDbTable(db, TABLE_GLOBAL);
            }

            loadSettings(db);

            db.setTransactionSuccessful();

            if (LOCAL_LOGV) Log.d(TAG, "Successfully created tables for slim settings db");
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Creates a table and index for the specified database and table name
     * @param db The {@link SQLiteDatabase} to create the table and index in.
     * @param tableName The name of the database table to create.
     */
    private void createDbTable(SQLiteDatabase db, String tableName) {
        if (LOCAL_LOGV) Log.d(TAG, "Creating table and index for: " + tableName);

        String createTableSql = String.format(CREATE_TABLE_SQL_FORMAT, tableName);
        db.execSQL(createTableSql);

        String createIndexSql = String.format(CREATE_INDEX_SQL_FORMAT, tableName, 1, tableName);
        db.execSQL(createIndexSql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (LOCAL_LOGV) Log.d(TAG, "Upgrading from version: " + oldVersion + " to " + newVersion);
        int upgradeVersion = oldVersion;

        // *** Remember to update DATABASE_VERSION above!

        if (upgradeVersion < newVersion) {
            Log.w(TAG, "Got stuck trying to upgrade db. Old version: " + oldVersion
                    + ", version stuck at: " +  upgradeVersion + ", new version: "
                            + newVersion + ". Must wipe the slim settings provider.");

            dropDbTable(db, TABLE_SYSTEM);
            dropDbTable(db, TABLE_SECURE);

            if (mUserHandle == UserHandle.USER_OWNER) {
                dropDbTable(db, TABLE_GLOBAL);
            }

            onCreate(db);
        }
    }

    /**
     * Drops the table and index for the specified database and table name
     * @param db The {@link SQLiteDatabase} to drop the table and index in.
     * @param tableName The name of the database table to drop.
     */
    private void dropDbTable(SQLiteDatabase db, String tableName) {
        if (LOCAL_LOGV) Log.d(TAG, "Dropping table and index for: " + tableName);

        String dropTableSql = String.format(DROP_TABLE_SQL_FORMAT, tableName);
        db.execSQL(dropTableSql);

        String dropIndexSql = String.format(DROP_INDEX_SQL_FORMAT, tableName, 1);
        db.execSQL(dropIndexSql);
    }

    /**
     * Loads default values for specific settings into the database.
     * @param db The {@link SQLiteDatabase} to insert into.
     */
    private void loadSettings(SQLiteDatabase db) {
        loadSystemSettings(db);
        loadSecureSettings(db);
        // The global table only exists for the 'owner' user
        if (mUserHandle == UserHandle.USER_OWNER) {
            loadGlobalSettings(db);
        }
    }

    private void loadSecureSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO secure(name,value)"
                    + " VALUES(?,?);");
            // Secure

        } finally {
            if (stmt != null) stmt.close();
        }
    }

    private void loadSystemSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO system(name,value)"
                    + " VALUES(?,?);");
            // System

        } finally {
            if (stmt != null) stmt.close();
        }
    }

    private void loadGlobalSettings(SQLiteDatabase db) {
        SQLiteStatement stmt = null;
        try {
            stmt = db.compileStatement("INSERT OR IGNORE INTO global(name,value)"
                    + " VALUES(?,?);");
        } finally {
            if (stmt != null) stmt.close();
        }
    }

    /**
     * Loads a string resource into a database table. If a conflict occurs, that value is not
     * inserted into the database table.
     * @param stmt The SQLLiteStatement (transaction) for this setting.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the string resource.
     */
    private void loadStringSetting(SQLiteStatement stmt, String name, int resId) {
        loadSetting(stmt, name, mContext.getResources().getString(resId));
    }

    /**
     * Loads a boolean resource into a database table. If a conflict occurs, that value is not
     * inserted into the database table.
     * @param stmt The SQLLiteStatement (transaction) for this setting.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the boolean resource.
     */
    private void loadBooleanSetting(SQLiteStatement stmt, String name, int resId) {
        loadSetting(stmt, name,
                mContext.getResources().getBoolean(resId) ? "1" : "0");
    }

    /**
     * Loads an integer resource into a database table. If a conflict occurs, that value is not
     * inserted into the database table.
     * @param stmt The SQLLiteStatement (transaction) for this setting.
     * @param name The name of the value to insert into the table.
     * @param resId The name of the integer resource.
     */
    private void loadIntegerSetting(SQLiteStatement stmt, String name, int resId) {
        loadSetting(stmt, name,
                Integer.toString(mContext.getResources().getInteger(resId)));
    }

    private void loadSetting(SQLiteStatement stmt, String key, Object value) {
        stmt.bindString(1, key);
        stmt.bindString(2, value.toString());
        stmt.execute();
    }
}
