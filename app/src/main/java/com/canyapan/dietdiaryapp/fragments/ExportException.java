package com.canyapan.dietdiaryapp.fragments;

import android.content.Context;
import android.support.annotation.StringRes;

class ExportException extends Exception {

    ExportException(String message) {
        super(message);
    }

    ExportException(Context context, @StringRes int message) {
        this(context.getString(message));
    }
}
