package com.canyapan.dietdiaryapp;

import android.content.res.Resources;

import com.canyapan.dietdiaryapp.models.Event;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

import org.joda.time.LocalDate;

import java.io.IOException;
import java.io.OutputStream;
import java.text.MessageFormat;

class ExportToPDF extends ExportAsyncTask {
    private static final String TAG = "ExportToPDF";
    private static final String YYYY_MM_DD = "yyyy-MM-dd";

    private Document document;
    private String[] types = null,
            foodTypes = null,
            drinkTypes = null;

    ExportToPDF(ExportActivity exportActivity, int destination, LocalDate fromDate, LocalDate toDate, OnExportListener listener) throws ExportException {
        super(exportActivity, destination, fromDate, toDate, listener);
    }

    @Override
    protected String getFileName(String prepend, LocalDate startDate, LocalDate endDate) {
        return MessageFormat.format("{0} {1} {2}.xml", prepend,
                startDate.toString(YYYY_MM_DD),
                endDate.toString(YYYY_MM_DD));
    }

    @Override
    protected void start(final OutputStream outputStream, final LocalDate fromDate, final LocalDate toDate) throws IOException, ExportException {
        document = new Document();

        try {
            PdfWriter.getInstance(document, outputStream);

            document.open();

            // Document Settings
            document.setPageSize(PageSize.A4);
            document.addCreationDate();
            document.addAuthor("DietDiaryApp");
            document.addCreator("DietDiaryApp");

            document.add(
                    new Paragraph(
                            MessageFormat.format("Diet Diary, {0} – {1}",
                                    fromDate.toString("dd/MM/yyyy"),
                                    toDate.toString("dd/MM/yyyy"))));
        } catch (DocumentException e) {
            throw new ExportException("Cannot open output stream.", e);
        }

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

        try {
            if (newDay) {
                final Paragraph day = new Paragraph(
                        event.getDate().toString());
                day.setSpacingBefore(2);
                document.add(day);
            }

            document.add(new Paragraph(event.getDescription()));

        } catch (DocumentException e) {

        }

        /*serializer.startTag(null, "event")
                .attribute(null, "id", String.valueOf(event.getID()))
                .attribute(null, "type", types[event.getType()])
                .attribute(null, "time", event.getTime().toString("HH:mm"))
                .startTag(null, "title").text(subType).endTag(null, "title")
                .startTag(null, "desc").text(event.getDescription()).endTag(null, "desc")
                .endTag(null, "event");*/
    }

    @Override
    protected void end() throws IOException, ExportException {
        document.close();
    }

}
