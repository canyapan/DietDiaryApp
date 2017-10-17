package com.canyapan.dietdiaryapp.fragments;

import android.content.ContentValues;
import android.content.res.Resources;
import android.util.JsonReader;
import android.util.Log;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.utils.TimeBasedRandomGenerator;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.HashMap;

class RestoreFromJSON extends RestoreAsyncTask {
    private JsonReader reader = null;
    private HashMap<String, Integer>
            typesMap = null,
            foodTypesMap = null,
            drinkTypesMap = null;
    private long index = 0;

    RestoreFromJSON(RestoreFragment restoreFragment, File file) throws RestoreException {
        super(restoreFragment, file);
    }

    @Override
    protected void checkFile(File file) throws IOException {
        if (!file.getName().toLowerCase().endsWith(".json")) {
            Log.e(RestoreFragment.TAG, "File is not a JSON.");
            throw new IOException(getExceptionText(R.string.restore_file_not_json));
        }
    }

    @Override
    protected void start(InputStreamReader inputStream, Resources resources) throws IOException {
        reader = new JsonReader(inputStream);

        String[] types = resources.getStringArray(R.array.spinner_event_types);
        typesMap = new HashMap<>(types.length);
        for (int i = 0; i < types.length; i++) {
            typesMap.put(types[i], i);
        }

        String[] foodTypes = resources.getStringArray(R.array.spinner_event_food_types);
        foodTypesMap = new HashMap<>(types.length);
        for (int i = 0; i < foodTypes.length; i++) {
            foodTypesMap.put(foodTypes[i], i);
        }

        String[] drinkTypes = resources.getStringArray(R.array.spinner_event_drink_types);
        drinkTypesMap = new HashMap<>(types.length);
        for (int i = 0; i < drinkTypes.length; i++) {
            drinkTypesMap.put(drinkTypes[i], i);
        }

        reader.beginObject();
        while (reader.hasNext()) {
            switch (reader.nextName()) {
                case "App":
                    break;
                case "Ver":
                    break;
                case "Events":
                    reader.beginArray();
                    return;
                default:
                    reader.skipValue();
            }
        }
    }

    @Override
    protected ContentValues readNext() throws IOException {
        if (!reader.hasNext()) {
            return null;
        }

        reader.beginObject();
        try {
            return parseRecord();
        } catch (IOException e) {
            throw new IOException(getExceptionText(R.string.restore_json_corrupted), e);
        } finally {
            reader.endObject();
            index++;
        }
    }

    private ContentValues parseRecord() throws IOException {
        Long id;
        LocalDate date;
        LocalTime time;
        Integer type, subType;
        String temp = null;

        try {
            id = reader.nextLong();
        } catch (IOException e) {
            Log.e(RestoreFragment.TAG, MessageFormat.format("JSON id cannot be parsed. record: {0}",
                    index));
            throw e;
        }

        try {
            temp = reader.nextString();
            date = LocalDate.parse(temp, DatabaseHelper.DB_DATE_FORMATTER);

            updateDateRange(date); // sorry for this, but I didn't want to parse date on super, again. :(
        } catch (IllegalArgumentException e) {
            Log.e(RestoreFragment.TAG, MessageFormat.format("JSON date cannot be parsed. record: {0} date: {1}",
                    index, temp));
            throw e;
        }

        try {
            temp = reader.nextString();
            time = LocalTime.parse(temp, DatabaseHelper.DB_TIME_FORMATTER);
        } catch (IllegalArgumentException e) {
            Log.e(RestoreFragment.TAG, MessageFormat.format("JSON time cannot be parsed. record: {0} time: {1}",
                    index, temp));
            throw e;
        }

        temp = reader.nextString();
        type = typesMap.get(temp);
        if (null == type) {
            Log.e(RestoreFragment.TAG, MessageFormat.format("JSON type cannot be identified. record: {0} type: {1}",
                    index, temp));
            throw new IOException("Type cannot be identified. " + temp);
        } else {
            temp = reader.nextString();
            switch (type) {
                case 0:
                    subType = foodTypesMap.get(temp);
                    break;
                case 1:
                    subType = drinkTypesMap.get(temp);
                    break;
                default:
                    subType = 0;
            }

            if (null == subType) {
                Log.e(RestoreFragment.TAG, MessageFormat.format("JSON subtype cannot be identified. record: {0} type: {1} subtype: {2}",
                        index, type, temp));
                throw new IOException("SubType cannot be identified. " + temp);
            }
        }

        if (id < 1000000000000L) {
            id = TimeBasedRandomGenerator.generateLong(date.toLocalDateTime(time).toDateTime());
        }

        ContentValues values = new ContentValues();

        values.put(DatabaseHelper.DBC_EVENT_ROW_ID, id);
        values.put(DatabaseHelper.DBC_EVENT_DATE, date.toString(DatabaseHelper.DB_DATE_FORMATTER));
        values.put(DatabaseHelper.DBC_EVENT_TIME, time.toString(DatabaseHelper.DB_TIME_FORMATTER));
        values.put(DatabaseHelper.DBC_EVENT_TYPE, type);
        values.put(DatabaseHelper.DBC_EVENT_SUBTYPE, subType);
        values.put(DatabaseHelper.DBC_EVENT_DESC, reader.nextString());

        return values;
    }

    @Override
    protected void end() {
        if (null != reader) {
            try {
                reader.close();
            } catch (IOException ignore) {
            }
        }
    }
}
