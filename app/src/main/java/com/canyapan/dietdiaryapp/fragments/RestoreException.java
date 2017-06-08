package com.canyapan.dietdiaryapp.fragments;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

class RestoreException extends Exception {

    RestoreException(String message) {
        super(message);
    }

    RestoreException(String message, Throwable cause) {
        super(message, cause);
    }

    RestoreException(@NonNull Context context, @StringRes int message) {
        this(context.getString(message));
    }

    RestoreException(@NonNull Context context, @StringRes int message, Throwable cause) {
        this(context.getString(message), cause);
    }
}
