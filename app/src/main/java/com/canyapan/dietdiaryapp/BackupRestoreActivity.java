package com.canyapan.dietdiaryapp;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.canyapan.dietdiaryapp.fragments.BackupFragment;
import com.canyapan.dietdiaryapp.fragments.RestoreFragment;

import org.joda.time.LocalDate;

import java.util.List;

public class BackupRestoreActivity extends AppCompatActivity
        implements RestoreFragment.OnFragmentInteractionListener,
        BackupFragment.OnFragmentInteractionListener {
    public static final int REQUEST_BACKUP_RESTORE = 2;
    private static final String TAG = "BackupRestoreActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewPager viewPager = findViewById(R.id.container);
        if (null != viewPager) {
            viewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));
        }

        TabLayout tabLayout = findViewById(R.id.tabs);
        if (null != tabLayout) {
            tabLayout.setupWithViewPager(viewPager);
            //noinspection ConstantConditions
            tabLayout.getTabAt(0).setIcon(R.drawable.tab_export);
            //noinspection ConstantConditions
            tabLayout.getTabAt(1).setIcon(R.drawable.tab_import);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Menu items are coming from the fragments.
        getMenuInflater().inflate(R.menu.menu_backup_restore, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            BackupRestoreActivity.this.finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackupComplete(Uri uri, LocalDate startDate, LocalDate endDate) {
        Log.d(TAG, "backed up.");

        // Broadcast the new created file. So, it will be available on usb data connection.
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    }

    @Override
    public void onShareComplete(Uri uri, LocalDate startDate, LocalDate endDate) {
        Log.d(TAG, "Shared.");
    }

    @Override
    public void onRestoreComplete(String path, LocalDate startDate, LocalDate endDate) {
        Log.d(TAG, "Restored.");
        setResult(Activity.RESULT_FIRST_USER);
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

    private class SectionsPagerAdapter extends FragmentPagerAdapter {

        SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return BackupFragment.newInstance();
                case 1:
                    return RestoreFragment.newInstance();
            }
            return null;
        }

        @Override
        public int getCount() {
            return 2;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            switch (position) {
                case 0:
                    return getString(R.string.backup);
                case 1:
                    return getString(R.string.restore);
            }
            return null;
        }
    }
}
