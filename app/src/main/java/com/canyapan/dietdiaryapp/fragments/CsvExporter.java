package com.canyapan.dietdiaryapp.fragments;

import org.joda.time.LocalDate;

import java.text.MessageFormat;

public class CsvExporter extends ExportAsyncTask {
    public CsvExporter(ExportFragment exportFragment, int destination) throws ExportException {
        super(exportFragment, destination);
    }

    @Override
    protected String getFileName(String prepend, LocalDate startDate, LocalDate endDate) {
        return MessageFormat.format("{0} {1} {2}.csv", prepend,
                startDate.toString("yyyy-MM-dd"),
                endDate.toString("yyyy-MM-dd"));
    }
}
