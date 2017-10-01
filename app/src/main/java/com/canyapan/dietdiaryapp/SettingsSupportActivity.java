package com.canyapan.dietdiaryapp;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.canyapan.dietdiaryapp.fragments.SettingsSupportFragment;
import com.canyapan.dietdiaryapp.helpers.DailyReminderServiceHelper;
import com.canyapan.dietdiaryapp.helpers.DriveBackupServiceHelper;

import java.util.List;

import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_ACTIVE;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_ACTIVE;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME;

public class SettingsSupportActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    public static final String KEY_ACTIVATE_BACKUP_BOOLEAN = "ACTIVATE BACKUP";

    private Fragment preferenceFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_support);

        if (null == preferenceFragment) {
            int flags = 0;
            if (getIntent().getBooleanExtra(KEY_ACTIVATE_BACKUP_BOOLEAN, false)) {
                flags |= SettingsSupportFragment.FLAG_ACTIVATE_BACKUP;
            }

            preferenceFragment = SettingsSupportFragment.newInstance(flags);
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(android.R.id.list_container, preferenceFragment);
        transaction.commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Context context = getApplicationContext();

        switch (key) {
            case KEY_NOTIFICATIONS_ACTIVE:
            case KEY_NOTIFICATIONS_DAILY_REMAINDER:
            case KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME:
                if (sharedPreferences.getBoolean(KEY_NOTIFICATIONS_ACTIVE, false)
                        && sharedPreferences.getBoolean(KEY_NOTIFICATIONS_DAILY_REMAINDER, false)) {
                    DailyReminderServiceHelper.setup(context);
                } else {
                    DailyReminderServiceHelper.cancel(context);
                }
                break;
            case KEY_BACKUP_ACTIVE:
                if (sharedPreferences.getBoolean(KEY_BACKUP_ACTIVE, false)) {
                    DriveBackupServiceHelper.setup(context);
                } else {
                    DriveBackupServiceHelper.cancel(context);
                }
                break;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        List<Fragment> fragments = getSupportFragmentManager().getFragments();
        if (fragments != null) {
            for (Fragment fragment : fragments) {
                if (fragment != null) {
                    fragment.onRequestPermissionsResult(requestCode, permissions, grantResults);
                }
            }
        }
    }
}
