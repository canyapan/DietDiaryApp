package com.canyapan.dietdiaryapp.fragments;

import android.content.res.Resources;
import android.util.JsonWriter;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.models.Event;

import org.joda.time.LocalDate;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.MessageFormat;

class BackupAsJSON extends BackupAsyncTask {
    private static final String KEY_ID = "ID";
    private static final String KEY_DATE = "Date";
    private static final String KEY_TIME = "Time";
    private static final String KEY_TYPE = "Type";
    private static final String KEY_TITLE = "Title";
    private static final String KEY_DESC = "Description";

    private JsonWriter writer = null;
    private String[] types = null,
            foodTypes = null,
            drinkTypes = null;

    BackupAsJSON(BackupFragment backupFragment, int destination) throws BackupException {
        super(backupFragment, destination);
    }

    @Override
    protected String getFileName(String prepend, LocalDate startDate, LocalDate endDate) {
        return MessageFormat.format("{0} {1} {2}.json", prepend,
                startDate.toString("yyyy-MM-dd"),
                endDate.toString("yyyy-MM-dd"));
    }

    @Override
    protected void start(OutputStreamWriter outputStream, Resources resources) throws IOException {
        writer = new JsonWriter(outputStream);
        writer.setIndent("  ");

        writer.beginObject();
        writer.name("App").value("DietDiaryApp");
        writer.name("Ver").value(1);
        writer.name("Events").beginArray();

        types = resources.getStringArray(R.array.spinner_event_types);
        foodTypes = resources.getStringArray(R.array.spinner_event_food_types);
        drinkTypes = resources.getStringArray(R.array.spinner_event_drink_types);
    }

    @Override
    protected void write(Event event) throws IOException {
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

        writer.beginObject();
        writer.name(KEY_DATE).value(event.getDate().toString(DatabaseHelper.DB_DATE_FORMATTER));
        writer.name(KEY_TIME).value(event.getTime().toString(DatabaseHelper.DB_TIME_FORMATTER));
        writer.name(KEY_TYPE).value(types[event.getType()]);
        writer.name(KEY_TITLE).value(subType);
        writer.name(KEY_DESC).value(event.getDescription());
        writer.endObject();
    }

    @Override
    protected void end() throws IOException {
        writer.endArray();
        writer.endObject();
        writer.close();
    }
}
