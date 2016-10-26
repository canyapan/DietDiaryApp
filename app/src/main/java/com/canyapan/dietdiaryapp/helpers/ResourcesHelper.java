package com.canyapan.dietdiaryapp.helpers;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.LocaleList;
import android.support.annotation.DrawableRes;
import android.support.annotation.IdRes;
import android.support.annotation.NonNull;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            config.setLocales(LocaleList.forLanguageTags(Locale.ENGLISH.toLanguageTag()));
        } else {
            config.locale = locale;
        }

        return new Resources(assets, metrics, config);
    }

    public static void setLeftCompoundDrawable(@NonNull final View parent, @IdRes final int textView, @DrawableRes final int drawable) {
        setLeftCompoundDrawable(parent.getContext(), (TextView) parent.findViewById(textView), drawable);
    }

    public static void setLeftCompoundDrawable(@NonNull final Context context, @NonNull final TextView textView, @DrawableRes final int drawable) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            textView.setCompoundDrawablesWithIntrinsicBounds(
                    VectorDrawableCompat.create(context.getResources(), drawable, context.getTheme()), null, null, null);
        } else {
            textView.setCompoundDrawablesWithIntrinsicBounds(drawable, 0, 0, 0);
        }
    }
}
