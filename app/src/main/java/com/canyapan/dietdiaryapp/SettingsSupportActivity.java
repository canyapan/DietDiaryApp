package com.canyapan.dietdiaryapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import com.canyapan.dietdiaryapp.fragments.SettingsSupportFragment;
import com.canyapan.dietdiaryapp.helpers.DailyReminderServiceHelper;
import com.canyapan.dietdiaryapp.helpers.DriveBackupServiceHelper;

import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_ACTIVE_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_ACTIVE_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING;

public class SettingsSupportActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_ACTIVATE_BACKUP_BOOLEAN = "ACTIVATE BACKUP";

    private Fragment mPreferenceFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_support);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        if (null == mPreferenceFragment) {
            int flags = 0;
            if (getIntent().getBooleanExtra(KEY_ACTIVATE_BACKUP_BOOLEAN, false)) {
                flags |= SettingsSupportFragment.FLAG_ACTIVATE_BACKUP;
            }

            mPreferenceFragment = SettingsSupportFragment.newInstance(flags);
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(android.R.id.list_container, mPreferenceFragment);
        transaction.commit();
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Context context = getApplicationContext();

        switch (key) {
            case KEY_NOTIFICATIONS_ACTIVE_BOOL:
            case KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL:
            case KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING:
                if (sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ACTIVE_BOOL, false)
                        && sharedPreferences.getBoolean(KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL, false)) {
                    DailyReminderServiceHelper.setup(context);
                } else {
                    DailyReminderServiceHelper.cancel(context);
                }
                break;
            case KEY_BACKUP_ACTIVE_BOOL:
                if (sharedPreferences.getBoolean(KEY_BACKUP_ACTIVE_BOOL, false)) {
                    //TODO DriveBackupServiceHelper.setup(context);
                } else {
                    DriveBackupServiceHelper.cancel(context);
                }
                break;
        }

    }
}
