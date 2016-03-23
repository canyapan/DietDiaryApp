package com.canyapan.dietdiaryapp.helpers;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.support.annotation.NonNull;
import android.util.DisplayMetrics;

import java.util.Locale;

public class ResourcesHelper {
    @NonNull
    public static Resources getEngResources(@NonNull final Context context) {
        return getResources(context, Locale.ENGLISH);
    }

    @NonNull
    public static Resources getResources(@NonNull final Context context, @NonNull final Locale locale) {
        Resources standardResources = context.getResources();

        AssetManager assets = standardResources.getAssets();
        DisplayMetrics metrics = standardResources.getDisplayMetrics();
        Configuration config = new Configuration(standardResources.getConfiguration());
        config.locale = locale;

        return new Resources(assets, metrics, config);
    }
}
