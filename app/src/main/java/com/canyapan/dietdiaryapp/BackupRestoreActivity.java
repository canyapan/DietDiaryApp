package com.canyapan.dietdiaryapp;

import android.app.Activity;
import android.app.backup.BackupManager;
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
import android.view.Menu;
import android.view.MenuItem;

import com.canyapan.dietdiaryapp.fragments.ExportFragment;
import com.canyapan.dietdiaryapp.fragments.ImportFragment;

import org.joda.time.LocalDate;

import java.util.List;

public class BackupRestoreActivity extends AppCompatActivity
        implements ImportFragment.OnFragmentInteractionListener,
        ExportFragment.OnFragmentInteractionListener {
    public static final int REQUEST_BACKUP_RESTORE = 2;
    private static final String TAG = "BackupRestoreActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_backup_restore);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        ViewPager viewPager = (ViewPager) findViewById(R.id.container);
        if (null != viewPager) {
            viewPager.setAdapter(new SectionsPagerAdapter(getSupportFragmentManager()));
        }

        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
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
    public void onExported(Uri uri, LocalDate startDate, LocalDate endDate) {
        // Broadcast the new created file.
        sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, uri));
    }

    @Override
    public void onShared(Uri uri, LocalDate startDate, LocalDate endDate) {

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

    @Override
    public void onImportComplete(Uri uri, LocalDate startDate, LocalDate endDate) {
        setResult(Activity.RESULT_FIRST_USER);

        BackupManager backupManager = new BackupManager(this);
        backupManager.dataChanged();
    }

    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            switch (position) {
                case 0:
                    return ExportFragment.newInstance();
                case 1:
                    return ImportFragment.newInstance();
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
