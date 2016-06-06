package com.canyapan.dietdiaryapp.fragments;

import android.content.Context;
import android.support.annotation.StringRes;

class ExportException extends Exception {

    public ExportException(String message) {
        super(message);
    }

    public ExportException(Context context, @StringRes int message) {
        this(context.getString(message));
    }
}
