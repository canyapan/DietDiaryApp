package com.canyapan.dietdiaryapp;

import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;

import com.canyapan.dietdiaryapp.helpers.DailyReminderServiceHelper;
import com.canyapan.dietdiaryapp.preference.PreferenceKeys;
import com.canyapan.dietdiaryapp.utils.Base62;
import com.crashlytics.android.Crashlytics;

import net.danlew.android.joda.JodaTimeAndroid;

import org.joda.time.DateTime;

import java.math.BigInteger;
import java.util.Random;

import io.fabric.sdk.android.Fabric;

public class Application extends android.app.Application {
    public static final String APP_DIR = "DietDiaryApp";

    @Override
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.CRASHLYTICS_ENABLED) {
            Fabric.with(this, new Crashlytics());
        }

        JodaTimeAndroid.init(this);

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (!preferences.contains(PreferenceKeys.KEY_APP_ID)) {
            String id = Base62.encode( // This will generate a time based alphanumeric 10 char value.
                    BigInteger.valueOf(DateTime.now().getMillis())                  // Time
                            .multiply(BigInteger.valueOf(10000))                    // Push 4 digit left
                            .add(BigInteger.valueOf(new Random().nextInt(9999)))    // Rand 0000:9999
            );

            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(PreferenceKeys.KEY_APP_ID, id);
            editor.apply();
        }

        DailyReminderServiceHelper.setup(this, preferences);
    }
}
