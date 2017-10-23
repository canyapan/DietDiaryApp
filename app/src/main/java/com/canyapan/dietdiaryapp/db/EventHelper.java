package com.canyapan.dietdiaryapp.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.canyapan.dietdiaryapp.models.Event;
import com.canyapan.dietdiaryapp.utils.TimeBasedRandomGenerator;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.text.MessageFormat;
import java.util.ArrayList;

@SuppressWarnings({"WeakerAccess", "unused"})
public class EventHelper {
    private static final String TAG = "EventHelper";

    public static Event parse(@NonNull final Cursor cursor) {
        return new Event(
                cursor.getLong(0), // ROWID
                LocalDate.parse(cursor.getString(1), DatabaseHelper.DB_DATE_FORMATTER), // DATE
                LocalTime.parse(cursor.getString(2), DatabaseHelper.DB_TIME_FORMATTER), // TIME
                cursor.getInt(3), // TYPE
                cursor.getInt(4), // SUBTYPE
                cursor.getString(5) // DESCRIPTION
        );
    }

    public static String[] getDatabaseColumns() {
        return new String[]{
                DatabaseHelper.DBC_EVENT_ROW_ID,
                DatabaseHelper.DBC_EVENT_DATE,
                DatabaseHelper.DBC_EVENT_TIME,
                DatabaseHelper.DBC_EVENT_TYPE,
                DatabaseHelper.DBC_EVENT_SUBTYPE,
                DatabaseHelper.DBC_EVENT_DESC
        };
    }

    private static ContentValues getContentValues(@NonNull final Event event) {
        return getContentValues(event, false);
    }

    private static ContentValues getContentValues(@NonNull final Event event,
                                                  final boolean includeID) {
        final ContentValues values = new ContentValues(includeID ? 6 : 5);

        if (includeID) {
            values.put(DatabaseHelper.DBC_EVENT_ROW_ID, event.getID());
        }

        values.put(DatabaseHelper.DBC_EVENT_DATE, event.getDate().toString(DatabaseHelper.DB_DATE_FORMATTER));
        values.put(DatabaseHelper.DBC_EVENT_TIME, event.getTime().toString(DatabaseHelper.DB_TIME_FORMATTER));
        values.put(DatabaseHelper.DBC_EVENT_TYPE, event.getType());
        values.put(DatabaseHelper.DBC_EVENT_SUBTYPE, event.getSubType());
        values.put(DatabaseHelper.DBC_EVENT_DESC, event.getDescription());

        return values;
    }

    public static boolean insert(@NonNull final Context context,
                                 @NonNull final Event event)
            throws SQLiteException {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);

        try {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            return insert(db, event);
        } finally {
            databaseHelper.close();
        }
    }

    public static boolean insert(@NonNull final SQLiteDatabase writableDatabase,
                                 @NonNull final Event event)
            throws SQLiteException {
        if (event.getID() == -1) {
            event.setID(TimeBasedRandomGenerator.generateLong());
        }

        long id = writableDatabase.insert(DatabaseHelper.DBT_EVENT, DatabaseHelper.DBC_EVENT_DESC,
                EventHelper.getContentValues(event, true));

        if (id >= 0) {
            event.setID(id);
            return true;
        }

        return false;
    }

    public static boolean update(@NonNull final Context context,
                                 @NonNull final Event event)
            throws SQLiteException {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);

        try {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            return update(db, event);
        } finally {
            databaseHelper.close();
        }
    }

    public static boolean update(@NonNull final SQLiteDatabase writableDatabase,
                                 @NonNull final Event event) {
        return writableDatabase.update(DatabaseHelper.DBT_EVENT, EventHelper.getContentValues(event),
                DatabaseHelper.DBC_EVENT_ROW_ID + " == ?",
                new String[]{Long.toString(event.getID())}) > 0;
    }

    public static boolean delete(@NonNull final Context context,
                                 @NonNull final Event event)
            throws SQLiteException {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);

        try {
            SQLiteDatabase db = databaseHelper.getWritableDatabase();
            return delete(db, event);
        } finally {
            databaseHelper.close();
        }
    }

    public static boolean delete(@NonNull final SQLiteDatabase writableDatabase,
                                 @NonNull final Event event)
            throws SQLiteException {
        return writableDatabase.delete(DatabaseHelper.DBT_EVENT,
                DatabaseHelper.DBC_EVENT_ROW_ID + " == ?",
                new String[]{Long.toString(event.getID())}) > 0;
    }

    @Nullable
    public static Event getEventByID(@NonNull final Context context,
                                     final long id) {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);

        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            return getEventByID(db, id);
        } finally {
            databaseHelper.close();
        }
    }

    @Nullable
    public static Event getEventByID(@NonNull final SQLiteDatabase readableDatabase,
                                     final long id)
            throws SQLiteException {
        Cursor cursor = null;
        try {
            cursor = readableDatabase.query(DatabaseHelper.DBT_EVENT, getDatabaseColumns(),
                    DatabaseHelper.DBC_EVENT_ROW_ID + " == ?", new String[]{Long.toString(id)},
                    null, null, null);

            if (cursor.moveToFirst()) {
                return parse(cursor);
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return null;
    }

    @Nullable
    public static ArrayList<Event> getEventByDate(@NonNull final Context context,
                                                  @NonNull final LocalDate date) throws SQLiteException {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);

        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            return getEventByDate(db, date);
        } finally {
            databaseHelper.close();
        }
    }

    @Nullable
    public static ArrayList<Event> getEventByDate(@NonNull final SQLiteDatabase readableDatabase,
                                                  @NonNull final LocalDate date)
            throws SQLiteException {
        Cursor cursor = null;
        try {
            cursor = readableDatabase.query(DatabaseHelper.DBT_EVENT, EventHelper.getDatabaseColumns(),
                    DatabaseHelper.DBC_EVENT_DATE + " = ?", new String[]{date.toString(DatabaseHelper.DB_DATE_FORMATTER)},
                    null, null, DatabaseHelper.DBC_EVENT_TIME + "," + DatabaseHelper.DBC_EVENT_ROW_ID);

            ArrayList<Event> list;
            if (cursor.moveToFirst()) {
                Log.d(TAG, MessageFormat.format("There are {0,number,integer} records on {1}.", cursor.getCount(), date));
                list = new ArrayList<>(cursor.getCount());

                do {
                    list.add(parse(cursor));
                } while (cursor.moveToNext());

                return list;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return null;
    }

    @Nullable
    public static ArrayList<Event> getEventByDateRange(@NonNull final Context context,
                                                       @NonNull final LocalDate startDate,
                                                       @NonNull final LocalDate endDate)
            throws SQLiteException, IllegalArgumentException {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);

        try {
            SQLiteDatabase db = databaseHelper.getReadableDatabase();
            return getEventByDateRange(db, startDate, endDate);
        } finally {
            databaseHelper.close();
        }
    }

    @Nullable
    public static ArrayList<Event> getEventByDateRange(@NonNull final SQLiteDatabase readableDatabase,
                                                       @NonNull final LocalDate startDate,
                                                       @NonNull final LocalDate endDate)
            throws SQLiteException, IllegalArgumentException {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("startDate must be <= endDate");
        }

        Cursor cursor = null;
        try {
            cursor = readableDatabase.query(DatabaseHelper.DBT_EVENT, EventHelper.getDatabaseColumns(),
                    DatabaseHelper.DBC_EVENT_DATE + " >= ? AND " + DatabaseHelper.DBC_EVENT_DATE + " <= ?",
                    new String[]{startDate.toString(DatabaseHelper.DB_DATE_FORMATTER), endDate.toString(DatabaseHelper.DB_DATE_FORMATTER)},
                    null, null, DatabaseHelper.DBC_EVENT_DATE + "," + DatabaseHelper.DBC_EVENT_TIME + "," + DatabaseHelper.DBC_EVENT_ROW_ID);

            ArrayList<Event> list;
            if (cursor.moveToFirst()) {
                Log.d(TAG, MessageFormat.format("There are {0,number,integer} records between {1} and {2}.", cursor.getCount(), startDate, endDate));
                list = new ArrayList<>(cursor.getCount());

                do {
                    list.add(parse(cursor));
                } while (cursor.moveToNext());

                return list;
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return null;
    }

    public static int hasEventToday(@NonNull final Context context) throws SQLiteException {
        DatabaseHelper databaseHelper = new DatabaseHelper(context);

        try {
            final SQLiteDatabase db = databaseHelper.getReadableDatabase();
            return hasEventToday(db);
        } finally {
            databaseHelper.close();
        }
    }

    public static int hasEventToday(@NonNull final SQLiteDatabase readableDatabase)
            throws SQLiteException {
        Cursor cursor = null;
        try {
            cursor = readableDatabase.query(DatabaseHelper.DBT_EVENT, new String[]{"COUNT(*)",},
                    DatabaseHelper.DBC_EVENT_DATE + " = ?", new String[]{LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER)},
                    null, null, null);

            if (cursor.moveToFirst()) {
                return cursor.getInt(0);
            }
        } finally {
            if (null != cursor) {
                cursor.close();
            }
        }

        return 0;
    }

    @Nullable
    public static Cursor getCursorOfDescriptionsByPartial(@NonNull final Context context,
                                                          @NonNull final String partialDescription)
            throws SQLiteException {
        LocalDate aMonthBefore = LocalDate.now().minusMonths(1);

        final DatabaseHelper dbHelper = new DatabaseHelper(context);
        final SQLiteDatabase readableDatabase = dbHelper.getReadableDatabase();

        return readableDatabase.query(DatabaseHelper.DBT_EVENT,
                new String[]{
                        DatabaseHelper.DBC_EVENT_ROW_ID + " _id",
                        DatabaseHelper.DBC_EVENT_DATE,
                        DatabaseHelper.DBC_EVENT_TIME,
                        DatabaseHelper.DBC_EVENT_TYPE,
                        DatabaseHelper.DBC_EVENT_SUBTYPE,
                        DatabaseHelper.DBC_EVENT_DESC
                },
                DatabaseHelper.DBC_EVENT_DATE + " >= ? AND " + DatabaseHelper.DBC_EVENT_DESC + " LIKE ? COLLATE NOCASE",
                new String[]{aMonthBefore.toString(DatabaseHelper.DB_DATE_FORMATTER), partialDescription + "%"},
                DatabaseHelper.DBC_EVENT_DESC + " COLLATE NOCASE", null,
                DatabaseHelper.DBC_EVENT_DATE + " DESC," + DatabaseHelper.DBC_EVENT_TIME + "," + DatabaseHelper.DBC_EVENT_ROW_ID);
    }
}
