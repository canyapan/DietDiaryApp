package com.canyapan.dietdiaryapp.fragments;

import android.content.ContentValues;
import android.content.res.Resources;
import android.util.Log;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.opencsv.CSVReader;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;

class RestoreFromJSON extends RestoreAsyncTask {
    private CSVReader reader = null;
    private HashMap<String, Integer>
            typesMap = null,
            foodTypesMap = null,
            drinkTypesMap = null;
    private String[] csvHeaders;

    RestoreFromJSON(RestoreFragment restoreFragment, File file) throws RestoreException {
        super(restoreFragment, file);
    }

    @Override
    protected void checkFile(File file) throws IOException {
        if (!file.getName().toLowerCase().endsWith(".json")) {
            Log.e(RestoreFragment.TAG, "File is not a JSON.");
            throw new IOException(getExceptionText(R.string.restore_file_not_csv));
        }
    }

    @Override
    protected void start(InputStreamReader inputStream, Resources resources) throws IOException {
        reader = new CSVReader(inputStream);

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

        csvHeaders = resources.getStringArray(R.array.csv_headers);

        checkHeaders();
    }

    private void checkHeaders() throws IOException {
        String[] record = reader.readNext();

        if (!Arrays.equals(csvHeaders, record)) {
            Log.e(RestoreFragment.TAG, "CSV headers does not match.");
            throw new IOException(getExceptionText(R.string.restore_csv_corrupted));
        }
    }

    @Override
    protected ContentValues readNext() throws IOException {
        String[] record;
        if ((record = reader.readNext()) == null) {
            return null;
        }

        return parseRecord(record, reader.getRecordsRead());
    }

    private ContentValues parseRecord(final String[] record, final long index) throws IOException {
        Long id;
        LocalDate date;
        LocalTime time;
        Integer type, subType;

        try {
            id = Long.parseLong(record[0]);
        } catch (NumberFormatException e) {
            Log.e(RestoreFragment.TAG, MessageFormat.format("CSV id cannot be parsed. record: {0} id: {1}",
                    index, record[0]));
            throw new IOException(getExceptionText(R.string.restore_csv_corrupted), e);
        }

        try {
            date = LocalDate.parse(record[1], DatabaseHelper.DB_DATE_FORMATTER);

            updateDateRange(date); // sorry for this, but I didn't want to parse date on super, again. :(
        } catch (IllegalArgumentException e) {
            Log.e(RestoreFragment.TAG, MessageFormat.format("CSV date cannot be parsed. record: {0} date: {1}",
                    index, record[1]));
            throw new IOException(getExceptionText(R.string.restore_csv_corrupted), e);
        }

        try {
            time = LocalTime.parse(record[2], DatabaseHelper.DB_TIME_FORMATTER);
        } catch (IllegalArgumentException e) {
            Log.d(RestoreFragment.TAG, MessageFormat.format("CSV time cannot be parsed. record: {0} time: {1}",
                    index, record[2]));
            throw new IOException(getExceptionText(R.string.restore_csv_corrupted));
        }

        type = typesMap.get(record[3]);
        if (null == type) {
            Log.e(RestoreFragment.TAG, MessageFormat.format("CSV type cannot be identified. record: {0} type: {1}",
                    index, record[3]));
            throw new IOException(getExceptionText(R.string.restore_csv_corrupted));
        } else {
            switch (type) {
                case 0:
                    subType = foodTypesMap.get(record[4]);
                    break;
                case 1:
                    subType = drinkTypesMap.get(record[4]);
                    break;
                default:
                    subType = 0;
            }

            if (null == subType) {
                Log.d(RestoreFragment.TAG, MessageFormat.format("CSV subtype cannot be identified. record: {0} type: {1} subtype: {2}",
                        index, record[3], record[4]));
                throw new IOException(getExceptionText(R.string.restore_csv_corrupted));
            }
        }

        ContentValues values = new ContentValues();

        values.put(DatabaseHelper.DBC_EVENT_ROW_ID, id);
        values.put(DatabaseHelper.DBC_EVENT_DATE, date.toString(DatabaseHelper.DB_DATE_FORMATTER));
        values.put(DatabaseHelper.DBC_EVENT_TIME, time.toString(DatabaseHelper.DB_TIME_FORMATTER));
        values.put(DatabaseHelper.DBC_EVENT_TYPE, type);
        values.put(DatabaseHelper.DBC_EVENT_SUBTYPE, subType);
        values.put(DatabaseHelper.DBC_EVENT_DESC, record[5]);

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
