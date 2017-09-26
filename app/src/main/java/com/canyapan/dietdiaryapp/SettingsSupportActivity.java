package com.canyapan.dietdiaryapp;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;

import com.canyapan.dietdiaryapp.fragments.SettingsSupportFragment;

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
        // TODO set a JobScheduler like this
        // https://github.com/talCrafts/Udhari/blob/407395bbdc2c499ca9d96a1070662c14f2767f2b/app/src/main/java/org/talcrafts/udhari/SettingsActivity.java
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
