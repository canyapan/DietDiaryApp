package com.canyapan.dietdiaryapp.fragments;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;

class BackupException extends Exception {

    private BackupException(String message) {
        super(message);
    }

    BackupException(@NonNull Context context, @StringRes int message) {
        this(context.getString(message));
    }
}
