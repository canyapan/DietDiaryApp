package com.canyapan.dietdiaryapp.db;

import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.v7.preference.PreferenceManager;
import android.util.Log;

import com.canyapan.dietdiaryapp.preference.PreferenceKeys;

import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final DateTimeFormatter DB_DATE_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd");
    public static final DateTimeFormatter DB_TIME_FORMATTER = DateTimeFormat.forPattern("HH:mm");
    public static final String DBT_EVENT = "[Event]",
            DBC_EVENT_ROW_ID = "[ROWID]",
            DBC_EVENT_DATE = "[Date]",
            DBC_EVENT_TIME = "[Time]",
            DBC_EVENT_TYPE = "[TypeKey]",
            DBC_EVENT_SUBTYPE = "[SubTypeKey]",
            DBC_EVENT_DESC = "[Description]";
    private static final String TAG = "DatabaseHelper";
    private static final String DATABASE_NAME = "Events.db";
    private static final int DATABASE_VERSION = 6;
    private static final String SQL_CREATE_DBT_EVENT =
            "CREATE TABLE " + DBT_EVENT + " (" +
                    DBC_EVENT_DATE + " DATE NOT NULL, " +
                    DBC_EVENT_TIME + " TIME NOT NULL, " +
                    DBC_EVENT_TYPE + " INTEGER NOT NULL DEFAULT 5, " +
                    DBC_EVENT_SUBTYPE + " INTEGER NOT NULL DEFAULT 0, " +
                    DBC_EVENT_DESC + " TEXT (1000) );";
    private static final String SQL_CREATE_DBI_EVENT_DATE =
            "CREATE INDEX [EventDateIndex] ON " + DBT_EVENT + " (" + DBC_EVENT_DATE + " ASC);";
    private static final String SQL_CREATE_DBI_EVENT_TYPE =
            "CREATE INDEX [EventTypeIndex] ON " + DBT_EVENT + " (" + DBC_EVENT_TYPE + " ASC);";

    private Context mContext = null;

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = context;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        try {
            db.beginTransaction();

            db.execSQL(SQL_CREATE_DBT_EVENT);
            db.execSQL(SQL_CREATE_DBI_EVENT_DATE);
            db.execSQL(SQL_CREATE_DBI_EVENT_TYPE);

            //db.execSQL("CREATE TABLE [EventType] ([Name] TEXT (50) PRIMARY KEY UNIQUE NOT NULL, [Custom] BOOLEAN (0) NOT NULL DEFAULT (1), [Order] INTEGER NOT NULL DEFAULT (99), [Parent] TEXT (50) REFERENCES [EventType] ([Name]) ON DELETE CASCADE ON UPDATE CASCADE);");
            /*db.execSQL("INSERT INTO [EventType] ([Name], [Custom], [Order], [Parent]) VALUES " +
                            "('Food', 0, 10, NULL)," +
                            "('Drink', 0, 11, NULL)," +
                            "('Activity', 0, 20, NULL)," +
                            "('Exercise', 0, 21, NULL)," +
                            "('Supplements', 0, 30, NULL)," +
                            "('Medication', 0, 31, NULL)," +
                            "('Breakfast', 0, 10, 'Food')," +
                            "('Launch', 0, 11, 'Food')," +
                            "('Dinner', 0, 12, 'Food')," +
                            "('Snack', 0, 13, 'Food')," +
                            "('Coffee', 0, 10, 'Drink')," +
                            "('Tea', 0, 11, 'Drink')," +
                            "('Water', 0, 12, 'Drink')," +
                            "('Alcohol', 0, 13, 'Drink')," +
                            "('Fruit Juice', 0, 14, 'Drink')," +
                            "('Energy Drink', 0, 15, 'Drink')," +
                            "('Soda', 0, 16, 'Drink');");*/

            Log.d(TAG, "Database " + DATABASE_NAME + " created.");
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        switch (oldVersion) {
            case 1:
                try {
                    db.beginTransaction();

                    db.execSQL("ALTER TABLE " + DBT_EVENT + " ADD COLUMN " + DBC_EVENT_TYPE + " INTEGER NOT NULL DEFAULT 5;");
                    db.execSQL("ALTER TABLE " + DBT_EVENT + " ADD COLUMN " + DBC_EVENT_SUBTYPE + " INTEGER NOT NULL DEFAULT 0;");
                    db.execSQL("DROP INDEX IF EXISTS [EventTypeIndex];");
                    db.execSQL("CREATE INDEX [EventTypeIndex] ON " + DBT_EVENT + " (" + DBC_EVENT_TYPE + " ASC);");
                    migrate1To2Helper1(db, "[Type]", DBC_EVENT_TYPE, "FOOD", 0);
                    migrate1To2Helper1(db, "[Type]", DBC_EVENT_TYPE, "DRINK", 1);
                    migrate1To2Helper1(db, "[Type]", DBC_EVENT_TYPE, "MED", 2);
                    migrate1To2Helper1(db, "[Title]", DBC_EVENT_SUBTYPE, "Food", 0);
                    migrate1To2Helper1(db, "[Title]", DBC_EVENT_SUBTYPE, "Drink", 0);
                    migrate1To2Helper1(db, "[Title]", DBC_EVENT_SUBTYPE, "Breakfast", 1);

                    Log.d(TAG, "Database version updated from 1 to 2.");
                    db.setTransactionSuccessful();
                } catch (SQLException e) {
                    Log.e(TAG, "An exception occurred while database updating from 1 to 2.", e);
                    throw e;
                } finally {
                    if (db.inTransaction()) {
                        db.endTransaction();
                    }
                }
            case 2:
                try {
                    db.beginTransaction();

                    db.execSQL("ALTER TABLE " + DBT_EVENT + " RENAME TO [EVENT1];");
                    db.execSQL("DROP INDEX [EventDateIndex];");
                    db.execSQL("DROP INDEX [EventTypeIndex];");

                    db.execSQL(SQL_CREATE_DBT_EVENT);
                    db.execSQL(SQL_CREATE_DBI_EVENT_DATE);
                    db.execSQL(SQL_CREATE_DBI_EVENT_TYPE);

                    db.execSQL("INSERT INTO " + DBT_EVENT + " SELECT [Date], [Time], [TypeKey], [SubTypeKey], [Description] FROM [EVENT1];");
                    db.execSQL("DROP TABLE [EVENT1];");

                    Log.d(TAG, "Database version updated from 2 to 3.");
                    db.setTransactionSuccessful();
                } catch (SQLException e) {
                    Log.e(TAG, "An exception occurred while database updating from 2 to 3.", e);
                    throw e;
                } finally {
                    if (db.inTransaction()) {
                        db.endTransaction();
                    }
                }
            case 3:
            case 4:
                try {
                    db.beginTransaction();
                    ContentValues values = new ContentValues();
                    values.put(DBC_EVENT_TYPE, 0);
                    db.update(DBT_EVENT, values, DBC_EVENT_TYPE + " == ?", new String[]{Integer.toString(-1),});
                    db.setTransactionSuccessful();
                } catch (SQLException e) {
                    Log.e(TAG, "An exception occurred while database updating from 2 to 4.", e);
                    throw e;
                } finally {
                    if (db.inTransaction()) {
                        db.endTransaction();
                    }
                }
                //db.execSQL("ALTER TABLE [Event]....;");
                //db.delete(DBT_EVENT, null, null); // DELETE EVERYTHING!!!
                break;
            case 5:
                // Settings keys has been changed. And this is the easiest way to apply this for the previous versions.
                SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(mContext);
                SharedPreferences.Editor editor = preferences.edit();
                if (preferences.contains("general_clock_mode")) {
                    editor.putString(PreferenceKeys.KEY_GENERAL_CLOCK_MODE_STRING,
                            preferences.getString("general_clock_mode", "-1"));
                    editor.remove("general_clock_mode");
                }
                if (preferences.contains("notifications_active")) {
                    editor.putBoolean(PreferenceKeys.KEY_NOTIFICATIONS_ACTIVE_BOOL,
                            preferences.getBoolean("notifications_active", true));
                    editor.remove("notifications_active");
                }
                if (preferences.contains("notifications_daily_remainder")) {
                    editor.putBoolean(PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL,
                            preferences.getBoolean("notifications_daily_remainder", true));
                    editor.remove("notifications_daily_remainder");
                }
                if (preferences.contains("notifications_daily_remainder_time")) {
                    editor.putString(PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING,
                            preferences.getString("notifications_daily_remainder_time", "19:00"));
                    editor.remove("notifications_daily_remainder_time");
                }
                editor.apply();
                break;
            default:
                break;
        }
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);

        if (!db.isReadOnly()) {
            db.execSQL("PRAGMA foreign_keys = on;");
        }
    }

    private void migrate1To2Helper1(SQLiteDatabase db, String oldColumn, String newColumn, String oldValue, int newValue) {
        ContentValues values = new ContentValues();
        values.put(newColumn, newValue);
        db.update(DBT_EVENT, values, oldColumn + " == ?", new String[]{oldValue,});
    }
}
