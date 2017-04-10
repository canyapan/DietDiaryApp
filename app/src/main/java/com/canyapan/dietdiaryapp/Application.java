package com.canyapan.dietdiaryapp;

import com.crashlytics.android.Crashlytics;

import io.fabric.sdk.android.Fabric;
import net.danlew.android.joda.JodaTimeAndroid;

public class Application extends android.app.Application {
    public static final String APP_DIR = "DietDiaryApp";

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.CRASHLYTICS_ENABLED) {
            Fabric.with(this, new Crashlytics());
        }

        JodaTimeAndroid.init(this);
    }
}
