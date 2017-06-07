package com.canyapan.dietdiaryapp.fragments;

import android.content.res.Resources;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.models.Event;
import com.opencsv.CSVWriter;

import org.joda.time.LocalDate;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;

class BackupAsCSV extends BackupAsyncTask {
    private CSVWriter csvWriter = null;
    private String[] types = null,
            foodTypes = null,
            drinkTypes = null;

    BackupAsCSV(BackupFragment backupFragment, int destination) throws BackupException {
        super(backupFragment, destination);
    }

    @Override
    protected String getFileName(String prepend, LocalDate startDate, LocalDate endDate) {
        return MessageFormat.format("{0} {1} {2}.csv", prepend,
                startDate.toString("yyyy-MM-dd"),
                endDate.toString("yyyy-MM-dd"));
    }

    @Override
    protected void start(OutputStreamWriter outputStream, Resources resources) {
        csvWriter = new CSVWriter(outputStream);
        csvWriter.writeNext(resources.getStringArray(R.array.csv_headers));

        types = resources.getStringArray(R.array.spinner_event_types);
        foodTypes = resources.getStringArray(R.array.spinner_event_food_types);
        drinkTypes = resources.getStringArray(R.array.spinner_event_drink_types);
    }

    @Override
    protected void write(Event event) {
        String subType;
        switch (event.getType()) {
            case Event.TYPE_FOOD:
                subType = foodTypes[event.getSubType()];
                break;
            case Event.TYPE_DRINK:
                subType = drinkTypes[event.getSubType()];
                break;
            default:
                subType = "";
        }

        csvWriter.writeNext(new String[]{
                Long.toString(event.getID()),
                event.getDate().toString(DatabaseHelper.DB_DATE_FORMATTER),
                event.getTime().toString(DatabaseHelper.DB_TIME_FORMATTER),
                types[event.getType()],
                subType,
                event.getDescription()
        });
    }

    @Override
    protected void end() {
        if (null != csvWriter) {
            try {
                csvWriter.close();
            } catch (IOException ignore) {
            }
        }
    }
}
