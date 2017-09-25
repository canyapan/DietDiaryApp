package com.canyapan.dietdiaryapp.fragments;

import android.content.IntentSender;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.text.format.DateUtils;
import android.util.Log;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.DateTimeHelper;
import com.canyapan.dietdiaryapp.preference.TimePreferenceCompat;
import com.canyapan.dietdiaryapp.preference.TimePreferenceDialogFragmentCompat;
import com.canyapan.dietdiaryapp.receivers.DailyAlarmReceiver;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.drive.Drive;

import org.joda.time.LocalDateTime;
import org.joda.time.LocalTime;

import java.lang.ref.WeakReference;

public class SettingsSupportFragment extends PreferenceFragmentCompat
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {

    private static final String TAG = "Settings";
    private static final String DIALOG_FRAGMENT_TAG =
            "android.support.v7.preference.PreferenceFragment.DIALOG";

    public static final String KEY_GENERAL_CLOCK_MODE = "general_clock_mode";
    public static final String KEY_NOTIFICATIONS_ACTIVE = "notifications_active";
    public static final String KEY_NOTIFICATIONS_DAILY_REMAINDER = "notifications_daily_remainder";
    public static final String KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME = "notifications_daily_remainder_time";
    public static final String KEY_BACKUP_ACTIVE = "backup_active";
    public static final String KEY_BACKUP_FREQUENCY = "backup_frequency";
    public static final String KEY_BACKUP_TIME = "backup_time";
    public static final String KEY_BACKUP_NOW = "backup_now";

    private WeakReference<GoogleApiClient> mGoogleApiClientRef = null;

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static final Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {

            if (preference.getKey().equals(KEY_BACKUP_NOW)) {
                if (null == value) {
                    preference.setSummary(preference.getContext().getString(R.string.pref_title_backup_now_summary, preference.getContext().getString(R.string.pref_title_backup_now_summary_never)));
                } else {
                    long time;
                    if (value instanceof String) {
                        time = Long.valueOf((String) value);
                    } else if (value instanceof Long) {
                        time = (long) value;
                    } else {
                        return false;
                    }

                    preference.setSummary(preference.getContext().getString(R.string.pref_title_backup_now_summary, DateUtils.getRelativeTimeSpanString(time)));
                }
            } else if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                if (null != value) {
                    int index = listPreference.findIndexOfValue(value.toString());

                    CharSequence summary;
                    if (index >= 0) {
                        summary = listPreference.getEntries()[index];
                    } else if (null != listPreference.getValue()) {
                        summary = listPreference.getValue();
                    } else {
                        summary = null;
                    }

                    // Set the summary to reflect the new value.
                    preference.setSummary(summary);
                }
            } else if (preference instanceof TimePreferenceCompat) {
                if (null != value && value instanceof String) {
                    LocalTime time = LocalTime.parse((String) value, DatabaseHelper.DB_TIME_FORMATTER);

                    TimePreferenceCompat timePreference = (TimePreferenceCompat) preference;
                    preference.setSummary(DateTimeHelper.convertLocalTimeToString(preference.getContext(),
                            time.getHourOfDay(), time.getMinuteOfHour()));

                    DailyAlarmReceiver.register(preference.getContext(), timePreference.getHour(), timePreference.getMinute());
                }
            } else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                if (value instanceof String) {
                    preference.setSummary(value.toString());
                } else if (value instanceof Boolean) {
                    switch (preference.getKey()) {
                        case KEY_NOTIFICATIONS_ACTIVE:
                        case KEY_NOTIFICATIONS_DAILY_REMAINDER:
                            if (value.equals(false)) {
                                DailyAlarmReceiver.cancel(preference.getContext());
                            } else {
                                DailyAlarmReceiver.register(preference.getContext());
                            }
                            break;
                    }
                }
            }

            return true;
        }
    };

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (null == mGoogleApiClientRef || null == mGoogleApiClientRef.get()) {
            mGoogleApiClientRef = new WeakReference<>(getGoogleApiClient());
        }

        addPreferencesFromResource(R.xml.settings);

        bindPreferenceSummaryToValue(findPreference(KEY_GENERAL_CLOCK_MODE));
        bindPreferenceSummaryToValue(findPreference(KEY_NOTIFICATIONS_ACTIVE));
        bindPreferenceSummaryToValue(findPreference(KEY_NOTIFICATIONS_DAILY_REMAINDER));
        bindPreferenceSummaryToValue(findPreference(KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME));
        bindPreferenceSummaryToValue(findPreference(KEY_BACKUP_FREQUENCY));
        bindPreferenceSummaryToValue(findPreference(KEY_BACKUP_TIME));

        SwitchPreferenceCompat backupActivePref = (SwitchPreferenceCompat) findPreference(KEY_BACKUP_ACTIVE);
        backupActivePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (null != mGoogleApiClientRef && null != mGoogleApiClientRef.get()) {
                    mGoogleApiClientRef.get().connect();
                    return true;
                }

                return false;
            }
        });

        Preference backupNowPref = findPreference(KEY_BACKUP_NOW);
        bindPreferenceSummaryToValue(backupNowPref);
        backupNowPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                long now = LocalDateTime.now().toDateTime().getMillis();
                if (preference.callChangeListener(now)) {
                    //SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
                    //SharedPreferences.Editor editor = sharedPreferences.edit();
                    //editor.putString(preference.getKey(), String.valueOf(now));
                    //editor.apply();
                    Log.d(TAG, "backup now save disabled for now!");
                }

                return true;
            }
        });
    }

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {

        // check if dialog is already showing
        if (getFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }

        if (preference instanceof TimePreferenceCompat) {
            final DialogFragment dialogFragment = TimePreferenceDialogFragmentCompat.newInstance(preference.getKey());
            if (dialogFragment != null) {
                dialogFragment.setTargetFragment(this, 0);
                dialogFragment.show(this.getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }
        } else {
            super.onDisplayPreferenceDialog(preference);
        }

    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference tvTitle) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                PreferenceManager
                        .getDefaultSharedPreferences(preference.getContext())
                        .getAll().get(preference.getKey()));
    }

    private GoogleApiClient getGoogleApiClient() {
        return new GoogleApiClient.Builder(this.getActivity())
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Drive API connected.");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Drive API connection suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (!connectionResult.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this.getActivity(), connectionResult.getErrorCode(), 0).show();
            return;
        }

        try {
            connectionResult.startResolutionForResult(this.getActivity(), 1234);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
        Log.e(TAG, "Drive API connection failed. " + connectionResult.toString());
    }

    @Override
    public void onStop() {
        if (null != mGoogleApiClientRef && null != mGoogleApiClientRef.get()) {
            mGoogleApiClientRef.get().disconnect();
        }

        super.onStop();
    }
}
