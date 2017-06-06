package com.canyapan.dietdiaryapp.helpers;

import android.app.DatePickerDialog;
import android.content.Context;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.view.ContextThemeWrapper;

/**
 * Samsung devices with android 5.x causes issues with DatePicketDialog.
 */
public class FixedDatePickerDialog extends DatePickerDialog {

    public FixedDatePickerDialog(@NonNull Context context, @Nullable OnDateSetListener listener, int year, int month, int dayOfMonth) {
        super(isBrokenSamsungDevice() ? new ContextThemeWrapper(context, android.R.style.Theme_Holo_Light_Dialog) : context, listener, year, month, dayOfMonth);
    }

    private static boolean isBrokenSamsungDevice() {
        return (Build.MANUFACTURER.equalsIgnoreCase("samsung")
                && isBetweenAndroidVersions(
                Build.VERSION_CODES.LOLLIPOP,
                Build.VERSION_CODES.LOLLIPOP_MR1));
    }

    private static boolean isBetweenAndroidVersions(int min, int max) {
        return Build.VERSION.SDK_INT >= min && Build.VERSION.SDK_INT <= max;
    }

}
