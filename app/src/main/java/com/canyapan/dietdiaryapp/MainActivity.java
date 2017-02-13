package com.canyapan.dietdiaryapp;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.DatePicker;

import com.canyapan.dietdiaryapp.fragments.CalendarFragment;
import com.canyapan.dietdiaryapp.models.Event;
import com.canyapan.dietdiaryapp.receivers.DailyAlarmReceiver;
import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.DateTimeFormatterBuilder;

/**
 * --- LIST NEXT VERSION
 * - add take picture button to create/edit view.
 * - user should be able to share photo to social networks.
 * - add XML export, validate XML by the help of XSD and generate HTML by the help of XSLT
 * - add reminders for water, snacks, medication, entering events.
 */
public class MainActivity extends AppCompatActivity implements
        NavigationView.OnNavigationItemSelectedListener,
        CalendarFragment.OnEventFragmentInteractionListener {
    private static final String TAG = "MainActivity";

    private static final String KEY_DATE_SERIALIZABLE = "DATE";
    private static final String KEY_FAB_SHOWN_BOOLEAN = "FAB";

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

    private LocalDate mSelectedDate;
    private Boolean mFab2Shown;

    private GoogleApiClient mClient;

    @Override
    public void onStart() {
        super.onStart();

        mClient.connect();
        Action viewAction = Action.newAction(Action.TYPE_VIEW, "Main Page",
                Uri.parse("android-app://com.canyapan.dietdiaryapp/android/dietdiaryapp")
        );

        AppIndex.AppIndexApi.start(mClient, viewAction);
    }

    @Override
    public void onStop() {
        super.onStop();

        Action viewAction = Action.newAction(Action.TYPE_VIEW, "Main Page",
                Uri.parse("android-app://com.canyapan.dietdiaryapp/android/dietdiaryapp")
        );
        AppIndex.AppIndexApi.end(mClient, viewAction);
        mClient.disconnect();
    }

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

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        mClient = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
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
            mDatePickerDialog = new DatePickerDialog(MainActivity.this,
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
}
