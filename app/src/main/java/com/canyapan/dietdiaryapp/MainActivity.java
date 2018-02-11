package com.canyapan.dietdiaryapp;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.DatePicker;

import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.db.EventHelper;
import com.canyapan.dietdiaryapp.fragments.CalendarFragment;
import com.canyapan.dietdiaryapp.helpers.FixedDatePickerDialog;
import com.canyapan.dietdiaryapp.models.Event;
import com.canyapan.dietdiaryapp.preference.PreferenceKeys;
import com.canyapan.dietdiaryapp.services.DriveBackupService;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        CalendarFragment.OnEventFragmentInteractionListener {
    private static final String TAG = "MainActivity";

    private static final String KEY_DATE_SERIALIZABLE = "DATE";
    private static final String KEY_FAB_SHOWN_BOOLEAN = "FAB";
    private static final String KEY_APP_RATE_DATE_STRING = "APP RATE DATE";
    private static final String KEY_APP_RATE_STATUS_INT = "APP RATE STATUS CODE";
    private static final String KEY_DRIVE_CONN_DATE_STRING = "DRIVE CONNECTION DATE";
    private static final String KEY_DRIVE_CONN_STATUS_INT = "DRIVE CONNECTION CODE";
    private static final String KEY_USER_TRANSLATE_DATE_STRING = "USER TRANSLATE DATE";
    private static final String KEY_USER_TRANSLATE_STATUS_INT = "USER TRANSLATE CODE";

    private static final int FLAG_STATUS_WAITING = 1;
    private static final int FLAG_STATUS_DONE = 2;
    private static final int FLAG_STATUS_NEVER_SHOW = 3;

    private static final DateTimeFormatter DATE_FORMATTER;

    static {
        // This formatter will be used to show selected date {@link mSelectedDate} on the top toolbar.
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        builder.appendDayOfWeekShortText();
        builder.appendLiteral(", ");
        builder.append(DateTimeFormat.mediumDate());
        DATE_FORMATTER = builder.toFormatter();
    }

    private FloatingActionButton mFab, mFabFood, mFabDrink, mFabMore;
    private WeakReference<CalendarFragment> mCalendarFragmentRef = null;

    private Animation mFab2AnimationShow, mFab2AnimationHide;
    private Animation mFabAnimationRotateFw, mFabAnimationRotateBw;

    private DatePickerDialog mDatePickerDialog;

    private LocalDate mSelectedDate;
    private Boolean mFab2Shown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.AppTheme_NoActionBar);
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            mSelectedDate = (LocalDate) savedInstanceState.getSerializable(KEY_DATE_SERIALIZABLE);
            mFab2Shown = savedInstanceState.getBoolean(KEY_FAB_SHOWN_BOOLEAN);
        } else {
            mSelectedDate = LocalDate.now();
            mFab2Shown = false;
        }

        setContentView(R.layout.activity_main);

        Toolbar toolbar = findViewById(R.id.toolbar);
        if (null != toolbar) {
            toolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePickerDialog();
                }
            });
            setSupportActionBar(toolbar);
        }

        mFabAnimationRotateFw = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab_rotate_fw);
        mFabAnimationRotateBw = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab_rotate_bw);
        mFab2AnimationShow = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab2_show);
        mFab2AnimationHide = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab2_hide);

        mFab = findViewById(R.id.fab);
        mFabFood = findViewById(R.id.fabFood);
        mFabDrink = findViewById(R.id.fabDrink);
        mFabMore = findViewById(R.id.fabMore);

        if (mFab2Shown) {
            mFab.setAnimation(mFabAnimationRotateFw);
            mFab.animate().setDuration(0);

            mFabFood.setAnimation(mFab2AnimationShow);
            mFabFood.animate().setDuration(0);

            mFabDrink.setAnimation(mFab2AnimationShow);
            mFabDrink.animate().setDuration(0);

            mFabMore.setAnimation(mFab2AnimationShow);
            mFabMore.animate().setDuration(0);
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (null != drawer) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
        }

        NavigationView mNavigationView = findViewById(R.id.nav_view);
        if (null != mNavigationView) {
            mNavigationView.setNavigationItemSelectedListener(this);
        }

        FragmentManager mFragmentManager = getSupportFragmentManager();
        if (null == mCalendarFragmentRef || null == mCalendarFragmentRef.get()) {
            CalendarFragment calendarFragment = (CalendarFragment) mFragmentManager.findFragmentByTag(CalendarFragment.TAG);

            if (null == calendarFragment) {
                calendarFragment = CalendarFragment.newInstance(mSelectedDate);
                mFragmentManager.beginTransaction()
                        .add(R.id.frame_layout, calendarFragment, CalendarFragment.TAG).commit();
            }

            mCalendarFragmentRef = new WeakReference<>(calendarFragment);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putSerializable(KEY_DATE_SERIALIZABLE, mSelectedDate);
        outState.putBoolean(KEY_FAB_SHOWN_BOOLEAN, mFab2Shown);

        Log.d(TAG, "Main activity instance variables saved.");
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CreateEditEventActivity.REQUEST_CREATE_EDIT:
                if (null != mCalendarFragmentRef && null != mCalendarFragmentRef.get()) {
                    mCalendarFragmentRef.get().handleCreateEditEvent(resultCode, data);
                }

                // Don't show too many dialogs at once.
                if (!checkDriveConnectionStatus()) { // check if a dialog is shown for drive connection.
                    if (!checkAppRateStatus()) { // Ask user to rate the app.
                        checkUserTranslateStatus(); // If already rated, ask user to help translations.
                    }
                }

                if (resultCode > 0) { // INSERTED OR DELETED OR UPDATED
                    setDataChangedStatus();
                }
                break;
            case BackupRestoreActivity.REQUEST_BACKUP_RESTORE:
                if (resultCode == Activity.RESULT_FIRST_USER) {
                    if (null != mCalendarFragmentRef && null != mCalendarFragmentRef.get()) {
                        mCalendarFragmentRef.get().goToDateForced(mSelectedDate);
                        setDataChangedStatus();
                    }
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (null != drawer && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    //region FAB events & methods
    public void onFabClicked(View view) {
        if (mFab2Shown) {
            mFab2Shown = false;
            mFab.startAnimation(mFabAnimationRotateBw);

            mFabFood.setClickable(false);
            mFabFood.startAnimation(mFab2AnimationHide);

            mFabDrink.setClickable(false);
            mFabDrink.startAnimation(mFab2AnimationHide);

            mFabMore.setClickable(false);
            mFabMore.startAnimation(mFab2AnimationHide);
        } else {
            mFab2Shown = true;
            mFab.startAnimation(mFabAnimationRotateFw);

            mFabMore.startAnimation(mFab2AnimationShow);
            mFabMore.setClickable(true);

            mFabDrink.startAnimation(mFab2AnimationShow);
            mFabDrink.setClickable(true);

            mFabFood.startAnimation(mFab2AnimationShow);
            mFabFood.setClickable(true);
        }
    }

    public void onFabEventClicked(View view) {
        onFabClicked(view);

        Event event = new Event();
        event.setDate(mSelectedDate);
        event.setType(Integer.parseInt(view.getTag().toString()));

        Intent intent = new Intent(MainActivity.this, CreateEditEventActivity.class)
                .putExtra(CreateEditEventActivity.KEY_EVENT_PARCELABLE, event);

        startActivityForResult(intent, CreateEditEventActivity.REQUEST_CREATE_EDIT);
    }
    //endregion

    //region Date Picker Dialog
    private void showDatePickerDialog() {
        if (null == mDatePickerDialog) {
            mDatePickerDialog = new FixedDatePickerDialog(MainActivity.this,
                    new DatePickerDialog.OnDateSetListener() {
                        @Override
                        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                            if (null != mCalendarFragmentRef && null != mCalendarFragmentRef.get()) {
                                mCalendarFragmentRef.get().goToDate(new LocalDate(year, monthOfYear + 1, dayOfMonth));
                            }
                        }
                    }, mSelectedDate.getYear(), mSelectedDate.getMonthOfYear() - 1, mSelectedDate.getDayOfMonth()
            );

            mDatePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, getString(android.R.string.ok), mDatePickerDialog);
            mDatePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, getString(android.R.string.cancel), mDatePickerDialog);
        } else {
            mDatePickerDialog.updateDate(mSelectedDate.getYear(), mSelectedDate.getMonthOfYear() - 1, mSelectedDate.getDayOfMonth());
        }

        mDatePickerDialog.show();
    }
    //endregion

    //region NavigationView.OnNavigationItemSelectedListener methods
    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.nav_home:
                Log.d(TAG, "Home item selected.");
                break;
            case R.id.nav_backup_restore:
                Log.d(TAG, "Backup/Restore item selected.");
                startActivityForResult(new Intent(MainActivity.this, BackupRestoreActivity.class), BackupRestoreActivity.REQUEST_BACKUP_RESTORE);
                break;
            case R.id.nav_export:
                Log.d(TAG, "Export item selected.");
                startActivity(new Intent(MainActivity.this, ExportActivity.class));
                break;
            case R.id.nav_settings:
                startActivity(new Intent(MainActivity.this, SettingsSupportActivity.class));
                break;
            case R.id.nav_about:
                new AboutDialog(this).show();
                break;
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (null != drawer) {
            drawer.closeDrawer(GravityCompat.START);
        }

        return true;
    }
    //endregion

    //region Fragment related methods
    @Override
    public void onDateChanged(LocalDate newDate) {
        Log.d(TAG, "Move from " + mSelectedDate + " to date " + newDate);
        mSelectedDate = newDate;

        //noinspection ConstantConditions
        getSupportActionBar().setTitle(newDate.toString(DATE_FORMATTER));
    }
    //endregion

    //region Drive Backup
    private void setDataChangedStatus() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        if (preferences.getBoolean(DriveBackupService.KEY_DATA_CHANGED_BOOLEAN, false)) {
            return; // Not required
        }

        final SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(DriveBackupService.KEY_DATA_CHANGED_BOOLEAN, true);
        editor.apply();
    }

    private boolean checkDriveConnectionStatus() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean backupStatus = preferences.getBoolean(PreferenceKeys.KEY_BACKUP_ACTIVE_BOOL, false);

        if (backupStatus) {
            return false; // Backup is already active.
        }

        int surveyStatus = preferences.getInt(KEY_DRIVE_CONN_STATUS_INT, FLAG_STATUS_WAITING);

        switch (surveyStatus) {
            case FLAG_STATUS_DONE:
            case FLAG_STATUS_NEVER_SHOW:
                return false; // Drive backup offer dialog is already shown. And user made their choice.
        }

        // case FLAG_STATUS_WAITING:
        // Check if user has been using the application for 3 days and entered at least 10 events.
        String date = preferences.getString(KEY_DRIVE_CONN_DATE_STRING, null);
        if (null == date) { // This is the first use.
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_DRIVE_CONN_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));
            editor.apply();

            showDriveConnectDialog();
            return true;
        }

        LocalDate startDate = DatabaseHelper.DB_DATE_FORMATTER.parseLocalDate(date);
        int days = Days.daysBetween(startDate, LocalDate.now()).getDays();
        if (days >= 3) { // It has been more than 3 days since last dialog
            int count = EventHelper.getEventCountByDateRange(this, startDate, LocalDate.now());

            if (count >= 10) { // There are more than 10 recorded events.
                showDriveConnectDialog(); // They may want to activate now.
                return true;
            }
        }

        return false;
    }

    private void showDriveConnectDialog() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        new AlertDialog.Builder(this)
                .setTitle(R.string.drive_dialog_title)
                .setMessage(R.string.drive_dialog_text)
                .setCancelable(true)
                .setPositiveButton(R.string.drive_dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDriveConnectionStatus(preferences, FLAG_STATUS_DONE);

                        // Open settings activity and activate drive backup
                        startActivity(new Intent(MainActivity.this, SettingsSupportActivity.class).putExtra(SettingsSupportActivity.KEY_ACTIVATE_BACKUP_BOOLEAN, true));
                    }
                })
                .setNeutralButton(R.string.drive_dialog_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDriveConnectionStatus(preferences, FLAG_STATUS_WAITING);
                    }
                })
                .setNegativeButton(R.string.drive_dialog_never, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDriveConnectionStatus(preferences, FLAG_STATUS_NEVER_SHOW);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        setDriveConnectionStatus(preferences, FLAG_STATUS_WAITING);
                    }
                }).show();
    }

    private void setDriveConnectionStatus(final SharedPreferences preferences, int status) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_DRIVE_CONN_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));

        if (status > 1) {
            editor.putInt(KEY_DRIVE_CONN_STATUS_INT, status);
        }

        editor.apply();
    }
    //endregion

    //region Rate App
    private boolean checkAppRateStatus() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int surveyStatus = preferences.getInt(KEY_APP_RATE_STATUS_INT, FLAG_STATUS_WAITING);

        switch (surveyStatus) {
            case FLAG_STATUS_DONE:
            case FLAG_STATUS_NEVER_SHOW:
                return false;
        }

        // case FLAG_APP_RATE_STATUS_WAITING:
        // Check if user has been using the application for 3 days and entered at least 10 events.
        String date = preferences.getString(KEY_APP_RATE_DATE_STRING, null);
        if (null == date) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_APP_RATE_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));
            editor.apply();
            return false;
        }

        LocalDate startDate = DatabaseHelper.DB_DATE_FORMATTER.parseLocalDate(date);
        int days = Days.daysBetween(startDate, LocalDate.now()).getDays();
        if (days >= 3) {
            int count = EventHelper.getEventCountByDateRange(this, startDate, LocalDate.now());

            if (count >= 15) {
                showAppRateDialog();

                return true;
            }
        }

        return false;
    }

    private void showAppRateDialog() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        new AlertDialog.Builder(this)
                .setTitle(R.string.rate_dialog_title)
                .setMessage(R.string.rate_dialog_text)
                .setCancelable(true)
                .setPositiveButton(R.string.rate_dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setAppRateStatus(preferences, FLAG_STATUS_DONE);

                        openPlayStore();
                    }
                })
                .setNeutralButton(R.string.rate_dialog_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setAppRateStatus(preferences, FLAG_STATUS_WAITING);
                    }
                })
                .setNegativeButton(R.string.rate_dialog_never, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setAppRateStatus(preferences, FLAG_STATUS_NEVER_SHOW);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        setAppRateStatus(preferences, FLAG_STATUS_WAITING);
                    }
                }).show();
    }

    private void setAppRateStatus(final SharedPreferences preferences, int status) {
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_APP_RATE_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));

        if (status > 1) {
            editor.putInt(KEY_APP_RATE_STATUS_INT, status);
        }

        editor.apply();
    }

    private void openPlayStore() {
        final Uri uri = Uri.parse("market://details?id=" + getPackageName());
        final Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
        // To count with Play market backstack, After pressing back button,
        // to taken back to our application, we need to add following flags to intent.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_NEW_DOCUMENT |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        } else {
            //noinspection deprecation
            goToMarket.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY |
                    Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET |
                    Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        }

        try {
            startActivity(goToMarket);
        } catch (ActivityNotFoundException e) {
            final Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
            startActivity(intent);
        }
    }
    //endregion

    //region User Translate
    private boolean checkUserTranslateStatus() {
        // Check user device language and show this if only required.
        // ISO 639-2 language codes of already translated languages
        // https://www.loc.gov/standards/iso639-2/php/code_list.php
        Set<String> alreadyTranslated = new HashSet<>(Arrays.asList("eng", "tur", "jpn", "fra", "ger", "dut", "nob", "pol"));
        String languageCode = Locale.getDefault().getISO3Language();
        if (alreadyTranslated.contains(languageCode)) {
            return false;
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int surveyStatus = preferences.getInt(KEY_USER_TRANSLATE_STATUS_INT, FLAG_STATUS_WAITING);

        switch (surveyStatus) {
            case FLAG_STATUS_DONE:
            case FLAG_STATUS_NEVER_SHOW:
                return false;
        }

        // case FLAG_APP_RATE_STATUS_WAITING:
        // Check if user has been using the application for 7 days and entered at least 50 events.
        String date = preferences.getString(KEY_USER_TRANSLATE_DATE_STRING, null);
        if (null == date) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_USER_TRANSLATE_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));
            editor.apply();
            return false;
        }

        LocalDate startDate = DatabaseHelper.DB_DATE_FORMATTER.parseLocalDate(date);
        int days = Days.daysBetween(startDate, LocalDate.now()).getDays();
        if (days >= 7) {
            int count = EventHelper.getEventCountByDateRange(this, startDate, LocalDate.now());

            if (count >= 50) {
                showUserTranslateDialog();
                return true;
            }
        }

        return false;
    }

    private void showUserTranslateDialog() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        new AlertDialog.Builder(this)
                .setTitle(R.string.translate_dialog_title)
                .setMessage(R.string.translate_dialog_text)
                .setCancelable(true)
                .setPositiveButton(R.string.translate_dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setUserTranslateStatus(preferences, FLAG_STATUS_DONE);

                        openWeblate();
                    }
                })
                .setNeutralButton(R.string.translate_dialog_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setUserTranslateStatus(preferences, FLAG_STATUS_WAITING);
                    }
                })
                .setNegativeButton(R.string.translate_dialog_never, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setUserTranslateStatus(preferences, FLAG_STATUS_NEVER_SHOW);
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        setUserTranslateStatus(preferences, FLAG_STATUS_WAITING);
                    }
                }).show();
    }

    private void setUserTranslateStatus(final SharedPreferences preferences, int status) {
        final SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_USER_TRANSLATE_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));

        if (status > 1) {
            editor.putInt(KEY_USER_TRANSLATE_STATUS_INT, status);
        }

        editor.apply();
    }

    private void openWeblate() {
        final Uri uri = Uri.parse("https://hosted.weblate.org/projects/diet-diary/strings/");
        final Intent goToWeblate = new Intent(Intent.ACTION_VIEW, uri);
        startActivity(goToWeblate);
    }
    //endregion
}
