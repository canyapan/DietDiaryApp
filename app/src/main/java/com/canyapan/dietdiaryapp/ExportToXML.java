package com.canyapan.dietdiaryapp;

import android.content.res.Resources;
import android.util.Xml;

import com.canyapan.dietdiaryapp.models.Event;

import org.joda.time.LocalDate;
import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

class ExportToXML extends ExportAsyncTask {
    private static final String TAG = "ExportToXML";

    private XmlSerializer serializer;
    private String[] types = null,
            foodTypes = null,
            drinkTypes = null;

    ExportToXML(ExportActivity exportActivity, int destination, LocalDate fromDate, LocalDate toDate, OnExportListener listener) throws ExportException {
        super(exportActivity, destination, fromDate, toDate, listener);
    }

    @Override
    protected String getFileName(String prepend, LocalDate startDate, LocalDate endDate) {
        return MessageFormat.format("{0} {1} {2}.xml", prepend,
                startDate.toString("yyyy-MM-dd"),
                endDate.toString("yyyy-MM-dd"));
    }

    @Override
    protected void start(final OutputStream outputStream, final LocalDate fromDate, final LocalDate toDate) throws IOException {
        serializer = Xml.newSerializer();
        serializer.setOutput(outputStream, "UTF-8");

        serializer.startDocument("UTF-8", true);
        serializer.processingInstruction("xml-stylesheet type=\"text/xsl\" href=\"events.xsl\"");

        serializer.startTag(null, "events")
                .attribute(null, "startDate", fromDate.toString("yyyy-MM-dd"))
                .attribute(null, "endDate", toDate.toString("yyyy-MM-dd"));

        final Resources resources = getResources();
        types = resources.getStringArray(R.array.spinner_event_types);
        foodTypes = resources.getStringArray(R.array.spinner_event_food_types);
        drinkTypes = resources.getStringArray(R.array.spinner_event_drink_types);
    }

    @Override
    protected void write(final Event event, boolean newDay, final long index, final long count) throws IOException {
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

        if (newDay) {
            if (index != 0) { // NOT FIRST
                endDay();
            }

            startDay(event.getDate());
        }

        serializer.startTag(null, "event")
                .attribute(null, "id", String.valueOf(event.getID()))
                .attribute(null, "type", types[event.getType()])
                .attribute(null, "time", event.getTime().toString("HH:mm"))
                .startTag(null, "title").text(subType).endTag(null, "title")
                .startTag(null, "desc").text(event.getDescription()).endTag(null, "desc")
                .endTag(null, "event");

        if (index + 1 == count) { // LAST
            endDay();
        }
    }

    private void startDay(final LocalDate date) throws IOException {
        serializer.startTag(null, "day")
                .attribute(null, "date", date.toString("yyyy-MM-dd"));
    }

    private void endDay() throws IOException {
        serializer.endTag(null, "day");
    }

    @Override
    protected void end() throws IOException {
        serializer.endTag(null, "events")
                .endDocument();

        serializer.flush();
    }

}
