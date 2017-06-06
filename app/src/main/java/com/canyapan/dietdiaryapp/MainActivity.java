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
import android.support.v7.app.ActionBar;
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
import com.canyapan.dietdiaryapp.receivers.DailyAlarmReceiver;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

import java.util.ArrayList;

/**
 * --- LIST NEXT VERSION
 * - add take picture button to create/edit view.
 * - user should be able to share photo to social networks.
 * - add XML export, validate XML by the help of XSD and generate HTML by the help of XSLT
 * - add reminders for water, snacks, medication, entering events.
 * - add timed notifications
 * - add saving data to cloud option
 */
public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        CalendarFragment.OnEventFragmentInteractionListener {
    private static final String TAG = "MainActivity";

    private static final String KEY_DATE_SERIALIZABLE = "DATE";
    private static final String KEY_FAB_SHOWN_BOOLEAN = "FAB";
    private static final String KEY_APP_RATE_DATE_STRING = "APP RATE DATE";
    private static final String KEY_APP_RATE_STATUS_INT = "APP RATE STATUS CODE";

    private static final int FLAG_APP_RATE_STATUS_WAITING = 1;
    private static final int FLAG_APP_RATE_STATUS_DONE = 2;
    private static final int FLAG_APP_RATE_STATUS_NEVER_SHOW = 3;

    private static final DateTimeFormatter DATE_FORMATTER;

    static {
        DateTimeFormatterBuilder builder = new DateTimeFormatterBuilder();
        builder.appendDayOfWeekShortText();
        builder.appendLiteral(", ");
        builder.append(DateTimeFormat.mediumDate());
        DATE_FORMATTER = builder.toFormatter();
    }

    private FloatingActionButton mFab, mFabFood, mFabDrink, mFabMore;
    private ActionBar mActionBar;
    private CalendarFragment mCalendarFragment = null;

    private Animation mFab2AnimationShow, mFab2AnimationHide;
    private Animation mFabAnimationRotateFw, mFabAnimationRotateBw;

    private DatePickerDialog mDatePickerDialog;
    private AlertDialog mRateDialog;

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

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (null != toolbar) {
            toolbar.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    showDatePickerDialog();
                }
            });
            setSupportActionBar(toolbar);
        }
        mActionBar = getSupportActionBar();

        mFabAnimationRotateFw = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab_rotate_fw);
        mFabAnimationRotateBw = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab_rotate_bw);
        mFab2AnimationShow = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab2_show);
        mFab2AnimationHide = AnimationUtils.loadAnimation(MainActivity.this, R.anim.fab2_hide);

        mFab = (FloatingActionButton) findViewById(R.id.fab);
        mFabFood = (FloatingActionButton) findViewById(R.id.fabFood);
        mFabDrink = (FloatingActionButton) findViewById(R.id.fabDrink);
        mFabMore = (FloatingActionButton) findViewById(R.id.fabMore);

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

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (null != drawer) {
            ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                    this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
            drawer.addDrawerListener(toggle);
            toggle.syncState();
        }

        NavigationView mNavigationView = (NavigationView) findViewById(R.id.nav_view);
        if (null != mNavigationView) {
            mNavigationView.setNavigationItemSelectedListener(this);
        }

        FragmentManager mFragmentManager = getSupportFragmentManager();
        if (null == mCalendarFragment) {
            mCalendarFragment = (CalendarFragment) mFragmentManager.findFragmentByTag(CalendarFragment.TAG);

            if (null == mCalendarFragment) {
                mCalendarFragment = CalendarFragment.newInstance(mSelectedDate);
                mFragmentManager.beginTransaction()
                        .add(R.id.frame_layout, mCalendarFragment, CalendarFragment.TAG).commit();
            }
        }

        DailyAlarmReceiver.register(MainActivity.this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(KEY_DATE_SERIALIZABLE, mSelectedDate);
        outState.putBoolean(KEY_FAB_SHOWN_BOOLEAN, mFab2Shown);

        Log.d(TAG, "Main activity instance variables saved.");
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CreateEditEventActivity.REQUEST_CREATE_EDIT:
                mCalendarFragment.handleCreateEditEvent(resultCode, data);

                // Ask user to rate the app.
                checkAppRateStatus();
                break;
            case BackupRestoreActivity.REQUEST_BACKUP_RESTORE:
                if (resultCode == Activity.RESULT_FIRST_USER) {
                    mCalendarFragment.goToDateForced(mSelectedDate);
                }
                break;
        }
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
                            mCalendarFragment.goToDate(new LocalDate(year, monthOfYear + 1, dayOfMonth));
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
            case R.id.nav_settings:
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
            case R.id.nav_about:
                new AboutDialog(this).show();
                break;
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
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
        mActionBar.setTitle(newDate.toString(DATE_FORMATTER));
    }
    //endregion

    //region Rate App
    private void checkAppRateStatus() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        int surveyStatus = preferences.getInt(KEY_APP_RATE_STATUS_INT, FLAG_APP_RATE_STATUS_WAITING);

        switch (surveyStatus) {
            case FLAG_APP_RATE_STATUS_DONE:
            case FLAG_APP_RATE_STATUS_NEVER_SHOW:
                return;
        }

        // case FLAG_APP_RATE_STATUS_WAITING:
        // Check if user has been using the application for 3 days and entered at least 10 events.
        String date = preferences.getString(KEY_APP_RATE_DATE_STRING, null);
        if (null == date) {
            SharedPreferences.Editor editor = preferences.edit();
            editor.putString(KEY_APP_RATE_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));
            editor.apply();
            return;
        }

        LocalDate startDate = DatabaseHelper.DB_DATE_FORMATTER.parseLocalDate(date);
        int days = Days.daysBetween(startDate, LocalDate.now()).getDays();
        if (days >= 3) {
            ArrayList<Event> events = EventHelper.getEventByDateRange(this, startDate, LocalDate.now());

            if (null != events && events.size() >= 15) {
                showAppRateDialog();
            }
        }
    }

    private void showAppRateDialog() {
        final SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

        mRateDialog = new AlertDialog.Builder(this)
                .setTitle(R.string.rate_dialog_title)
                .setMessage(R.string.rate_dialog_text)
                .setCancelable(true)
                .setPositiveButton(R.string.rate_dialog_yes, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt(KEY_APP_RATE_STATUS_INT, FLAG_APP_RATE_STATUS_DONE);
                        editor.putString(KEY_APP_RATE_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));
                        editor.apply();

                        openPlayStore();
                    }
                })
                .setNeutralButton(R.string.rate_dialog_later, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(KEY_APP_RATE_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));
                        editor.apply();
                    }
                })
                .setNegativeButton(R.string.rate_dialog_never, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putInt(KEY_APP_RATE_STATUS_INT, FLAG_APP_RATE_STATUS_NEVER_SHOW);
                        editor.putString(KEY_APP_RATE_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));
                        editor.apply();
                    }
                })
                .setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        SharedPreferences.Editor editor = preferences.edit();
                        editor.putString(KEY_APP_RATE_DATE_STRING, LocalDate.now().toString(DatabaseHelper.DB_DATE_FORMATTER));
                        editor.apply();
                    }
                })
                .setOnDismissListener(new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        mRateDialog = null;
                    }
                }).show();
    }

    private void openPlayStore() {
        Uri uri = Uri.parse("market://details?id=" + getPackageName());
        Intent goToMarket = new Intent(Intent.ACTION_VIEW, uri);
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
            startActivity(new Intent(Intent.ACTION_VIEW,
                    Uri.parse("http://play.google.com/store/apps/details?id=" + getPackageName())));
        }
    }
    //endregion
}
