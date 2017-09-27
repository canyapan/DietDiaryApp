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

public class SettingsSupportActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Fragment preferenceFragment = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings_support);

        if (null == preferenceFragment) {
            preferenceFragment = new SettingsSupportFragment();
        }

        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        transaction.replace(android.R.id.list_container, preferenceFragment);
        transaction.commit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        final Context context = getApplicationContext();

        switch (key) {
            case SettingsSupportFragment.KEY_NOTIFICATIONS_ACTIVE:
            case SettingsSupportFragment.KEY_NOTIFICATIONS_DAILY_REMAINDER:
            case SettingsSupportFragment.KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME:
                if (sharedPreferences.getBoolean(SettingsSupportFragment.KEY_NOTIFICATIONS_ACTIVE, false)
                        && sharedPreferences.getBoolean(SettingsSupportFragment.KEY_NOTIFICATIONS_DAILY_REMAINDER, false)) {
                    DailyReminderServiceHelper.setup(context);
                } else {
                    DailyReminderServiceHelper.cancel(context);
                }
                break;
            case SettingsSupportFragment.KEY_BACKUP_ACTIVE:
                if (sharedPreferences.getBoolean(SettingsSupportFragment.KEY_BACKUP_ACTIVE, false)) {
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
