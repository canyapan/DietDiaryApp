package com.canyapan.dietdiaryapp.helpers;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.annotation.NonNull;
import android.support.v7.preference.PreferenceManager;
import android.text.format.DateFormat;

import com.canyapan.dietdiaryapp.fragments.SettingsSupportFragment;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

public class DateTimeHelper {
    public static final DateTimeFormatter DATE_FORMAT = DateTimeFormat.shortDate();
    public static final DateTimeFormatter TIME_FORMAT_AMPM = DateTimeFormat.forPattern("hh:mm a");
    public static final DateTimeFormatter TIME_FORMAT_24H = DateTimeFormat.forPattern("HH:mm");

    @NonNull
    public static String convertLocalDateTimeToString(@NonNull final Context context, @NonNull final LocalDateTime dateTime) {
        return convertLocalDateToString(dateTime.toLocalDate()) + " " + convertLocalTimeToString(context, dateTime.toLocalTime());
    }

    @NonNull
    public static String convertLocalDateToString(@NonNull final LocalDate date) {
        return date.toString(DATE_FORMAT);
    }

    public static boolean is24HourMode(@NonNull final Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        switch (preferences.getString(SettingsSupportFragment.KEY_GENERAL_CLOCK_MODE, "-1")) {
            case "0":
                return false;
            case "1":
                return true;
            default:
                return DateFormat.is24HourFormat(context);
        }
    }

    @NonNull
    public static String convertLocalTimeToString(@NonNull final Context context, @NonNull final LocalTime time) {
        return convertLocalTimeToString(is24HourMode(context), time);
    }

    @NonNull
    public static String convertLocalTimeToString(@NonNull final Context context, final int hour, final int minute) {
        return convertLocalTimeToString(is24HourMode(context), new LocalTime(hour, minute));
    }

    @NonNull
    public static String convertLocalTimeToString(final boolean is24HourMode, @NonNull final LocalTime time) {
        return time.toString(is24HourMode ? TIME_FORMAT_24H : TIME_FORMAT_AMPM);
    }
}
