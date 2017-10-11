package com.canyapan.dietdiaryapp.fragments;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.DateTimeHelper;
import com.canyapan.dietdiaryapp.helpers.DriveBackupServiceHelper;
import com.canyapan.dietdiaryapp.preference.TimePreferenceCompat;
import com.canyapan.dietdiaryapp.preference.TimePreferenceDialogFragmentCompat;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveApi;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;

import org.joda.time.LocalTime;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import static android.app.Activity.RESULT_OK;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_APP_ID;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_ACTIVE_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_FILE_DRIVE_ID_STRING;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_FREQUENCY_STRING;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_LAST_BACKUP_TIMESTAMP_LONG;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_NOW_ACT;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_GENERAL_CLOCK_MODE_STRING;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_ACTIVE_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING;
import static com.canyapan.dietdiaryapp.services.DriveBackupService.DRIVE_KEY_APP_ID;
import static com.canyapan.dietdiaryapp.services.DriveBackupService.DRIVE_KEY_DEVICE_NAME;

public class SettingsSupportFragment extends PreferenceFragmentCompat
        implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = "Settings";
    private static final String DIALOG_FRAGMENT_TAG =
            "android.support.v7.preference.PreferenceFragment.DIALOG";

    public static final int FLAG_ACTIVATE_BACKUP = 1;
    private static final String KEY_ACTIVATE_BACKUP_BOOLEAN = "ACTIVATE BACKUP";

    private static final int REQUEST_ACCOUNTS = 1000;
    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private WeakReference<GoogleApiClient> mGoogleApiClientRef = null;

    public static SettingsSupportFragment newInstance(int flags) {
        Log.d(TAG, "newInstance");
        SettingsSupportFragment fragment = new SettingsSupportFragment();
        Bundle args = new Bundle();

        if ((flags & FLAG_ACTIVATE_BACKUP) != 0) {
            args.putBoolean(KEY_ACTIVATE_BACKUP_BOOLEAN, true);
        }

        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        if (null == mGoogleApiClientRef || null == mGoogleApiClientRef.get()) {
            mGoogleApiClientRef = new WeakReference<>(getGoogleApiClient());
        }

        boolean activateBackup = false;
        if (null != savedInstanceState) {
            activateBackup = savedInstanceState.getBoolean(KEY_ACTIVATE_BACKUP_BOOLEAN);
        } else if (getArguments() != null) {
            activateBackup = getArguments().getBoolean(KEY_ACTIVATE_BACKUP_BOOLEAN);
        }

        addPreferencesFromResource(R.xml.settings);

        bindPreferenceSummaryToValue(findPreference(KEY_GENERAL_CLOCK_MODE_STRING));
        bindPreferenceSummaryToValue(findPreference(KEY_NOTIFICATIONS_ACTIVE_BOOL));
        bindPreferenceSummaryToValue(findPreference(KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL));
        bindPreferenceSummaryToValue(findPreference(KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING));
        bindPreferenceSummaryToValue(findPreference(KEY_BACKUP_FREQUENCY_STRING));

        SwitchPreferenceCompat backupActivePref = (SwitchPreferenceCompat) findPreference(KEY_BACKUP_ACTIVE_BOOL);
        backupActivePref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if ((Boolean) newValue) { // changing to active
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (getActivity().checkSelfPermission(Manifest.permission.GET_ACCOUNTS)
                                != PackageManager.PERMISSION_GRANTED) {
                            getActivity().requestPermissions( // request permission
                                    new String[]{
                                            Manifest.permission.GET_ACCOUNTS
                                    }, REQUEST_ACCOUNTS);

                            return false;
                        }
                    }

                    // have the permission or version is lower
                    connectGoogleApiClient();

                    return false; // will be handled if drive connection successful.
                }

                // changing to passive
                disconnectGoogleApiClient();

                return true; // let it
            }
        });

        if (backupActivePref.isChecked()) {
            connectGoogleApiClient();
        } else if (activateBackup) {
            backupActivePref.callChangeListener(true); // This will fire the preference changed event, above.
            // But, it won't let to change it. It will control the change itself.
            // Therefore we won't try this here.
        }

        final Preference backupNowPref = findPreference(KEY_BACKUP_NOW_ACT);
        setPreferenceSummaryForBackupNowAction(backupNowPref);
        backupNowPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                DriveBackupServiceHelper.setupImmediate(getContext());
                return true;
            }
        });
        // Setup a listener to watch changes on last backup pref
        getPreferenceManager().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (key.equals(KEY_BACKUP_LAST_BACKUP_TIMESTAMP_LONG)) { // Listen last backup timestamp changes.
            final Preference preference = findPreference(KEY_BACKUP_NOW_ACT);
            setPreferenceSummaryForBackupNowAction(preference);
        }
    }

    private void setPreferenceSummaryForBackupNowAction(final Preference preference) {
        final long timestamp = getPreferenceManager().getSharedPreferences().getLong(KEY_BACKUP_LAST_BACKUP_TIMESTAMP_LONG, -1);
        if (timestamp < 0) {
            preference.setSummary(preference.getContext().getString(R.string.pref_title_backup_now_summary, preference.getContext().getString(R.string.pref_title_backup_now_summary_never)));
        } else {
            preference.setSummary(preference.getContext().getString(R.string.pref_title_backup_now_summary, DateUtils.getRelativeTimeSpanString(timestamp)));
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        final DialogFragment f = (DialogFragment) getFragmentManager()
                .findFragmentByTag(DIALOG_FRAGMENT_TAG);

        if (f != null) {
            f.setTargetFragment(this, 0);
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            if (resultCode == RESULT_OK) {
                connectGoogleApiClient();
            }
        }
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
                dialogFragment.show(getFragmentManager(), DIALOG_FRAGMENT_TAG);
            }
        } else {
            super.onDisplayPreferenceDialog(preference);
        }
    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
        Log.d(TAG, "Drive API connected.");

        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        String appID = preferences.getString(KEY_APP_ID, null);
        String driveFileID = preferences.getString(KEY_BACKUP_FILE_DRIVE_ID_STRING, null);

        if (null == driveFileID) { // This is the first time connecting to drive
            // 1- Check drive contents
            // 2- Ask user to choose a backup to restore if found any

            // Get backup files from drive.
            SortOrder sortOrder = new SortOrder.Builder()
                    .addSortDescending(SortableField.CREATED_DATE)
                    .build();

            Query query = new Query.Builder()
                    .addFilter(
                            Filters.and(
                                    Filters.eq(SearchableField.MIME_TYPE, "application/zip"),
                                    Filters.eq(SearchableField.TITLE, "backup.zip")
                            )
                    )
                    .setSortOrder(sortOrder)
                    .build();

            Drive.DriveApi.getAppFolder(mGoogleApiClientRef.get())
                    .queryChildren(mGoogleApiClientRef.get(), query)
                    .setResultCallback(new ResultCallback<DriveApi.MetadataBufferResult>() {
                        @Override
                        public void onResult(@NonNull DriveApi.MetadataBufferResult metadataBufferResult) {
                            if (!metadataBufferResult.getStatus().isSuccess()) {
                                Log.e(TAG, "Cannot query current backups. " + metadataBufferResult.getStatus().getStatusMessage());
                                return;
                            }

                            if (metadataBufferResult.getMetadataBuffer().getCount() != 0) {
                                // Found some backup files saved in the drive.
                                // show user if they want to restore from any of these backups

                                List<DriveFile> files = new ArrayList<>(metadataBufferResult.getMetadataBuffer().getCount());
                                for (Metadata m : metadataBufferResult.getMetadataBuffer()) {
                                    files.add(new DriveFile(m));
                                    Log.d(TAG, String.format(Locale.getDefault(), "%s %,.2fKB", m.getTitle(), (m.getFileSize() / 1024f)));
                                }

                                showSelectDriveBackupDialog(files);
                            } else {
                                // No backup found.. Activate the drive backup.
                                setDriveFileId("");
                                activateBackup();
                            }
                        }
                    });
        }
    }

    private void activateBackup() {
        final SwitchPreferenceCompat backupActivePref = (SwitchPreferenceCompat) findPreference(KEY_BACKUP_ACTIVE_BOOL);
        if (!backupActivePref.isChecked()) {
            backupActivePref.setChecked(true);
        }
    }

    private void setDriveFileId(String driveId) {
        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_BACKUP_FILE_DRIVE_ID_STRING, driveId);
        editor.apply();
    }

    private void showSelectDriveBackupDialog(List<DriveFile> files) {
        // Show a dialog to get user's choice
        final ArrayAdapter<DriveFile> arrayAdapter = new ArrayAdapter<>(getContext(), android.R.layout.select_dialog_singlechoice, files);

        new AlertDialog.Builder(getContext())
                .setTitle("Select a backup to restore:") // TODO
                .setNegativeButton("Do not restore", new DialogInterface.OnClickListener() { // TODO
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setDriveFileId("");
                        activateBackup();

                        dialog.dismiss();
                    }
                })
                .setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        final DriveFile file = arrayAdapter.getItem(which);
                        Log.d(TAG, file.getTitle() + " selected.");

                        //TODO import data @file in background


                        //TODO after that set setDriveFileId(file.getId());
                    }
                })
                .show();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Drive API connection suspended.");
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        if (!connectionResult.hasResolution()) {
            // show the localized error dialog.
            GoogleApiAvailability.getInstance().getErrorDialog(this.getActivity(),
                    connectionResult.getErrorCode(), 0).show();
            return;
        }

        try {
            connectionResult.startResolutionForResult(this.getActivity(), REQUEST_RESOLVE_ERROR);
        } catch (IntentSender.SendIntentException e) {
            Log.e(TAG, "Exception while starting resolution activity", e);
        }
        Log.e(TAG, "Drive API connection failed. " + connectionResult.toString());
    }

    @Override
    public void onStop() {
        disconnectGoogleApiClient();

        super.onStop();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_ACCOUNTS
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            connectGoogleApiClient(); // Permission granted, lets try to connect now.
        } else {
            Toast.makeText(getContext(), R.string.pref_backup_no_permission, Toast.LENGTH_LONG).show();
        }
    }

    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference instanceof ListPreference) {
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

                //TimePreferenceCompat timePreference = (TimePreferenceCompat) preference;
                preference.setSummary(DateTimeHelper.convertLocalTimeToString(preference.getContext(),
                        time.getHourOfDay(), time.getMinuteOfHour()));
            }
        } else {
            // For all other preferences, set the summary to the value's
            // simple string representation.
            if (value instanceof String) {
                preference.setSummary(value.toString());
            }
        }

        return true;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference tvTitle) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     */
    private void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(this);

        // Trigger the listener immediately with the preference's current value.
        this.onPreferenceChange(preference, getPreferenceManager()
                .getSharedPreferences()
                .getAll()
                .get(preference.getKey()));
    }

    @NonNull
    private GoogleApiClient getGoogleApiClient() {
        return new GoogleApiClient.Builder(this.getActivity())
                .addApi(Drive.API)
                .addScope(Drive.SCOPE_APPFOLDER)
                .addScope(Drive.SCOPE_FILE)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    private void connectGoogleApiClient() {
        if (null != mGoogleApiClientRef && null != mGoogleApiClientRef.get()) {
            if (!mGoogleApiClientRef.get().isConnecting() && !mGoogleApiClientRef.get().isConnected()) {
                mGoogleApiClientRef.get().connect();
            }
        }
    }

    private void disconnectGoogleApiClient() {
        if (null != mGoogleApiClientRef && null != mGoogleApiClientRef.get()) {
            if (mGoogleApiClientRef.get().isConnected() || mGoogleApiClientRef.get().isConnecting()) {
                mGoogleApiClientRef.get().disconnect();
            }
        }
    }

    private class DriveFile {
        private final String mId;
        private final String mTitle;
        private final Date mCreationDate;
        private final Date mModificationDate;
        private final Long mSize;
        private final String mAppId;
        private final String mDeviceName;

        DriveFile(final Metadata m) {
            mId = m.getDriveId().encodeToString();
            mTitle = m.getTitle();
            mCreationDate = m.getCreatedDate();
            mModificationDate = m.getModifiedDate();
            mSize = m.getFileSize();

            mAppId = m.getCustomProperties().get(DRIVE_KEY_APP_ID);
            mDeviceName = m.getCustomProperties().get(DRIVE_KEY_DEVICE_NAME);
        }

        @Override
        public String toString() {
            return getDeviceName();
        }

        public String getId() {
            return mId;
        }

        public String getTitle() {
            return mTitle;
        }

        public Date getCreationDate() {
            return mCreationDate;
        }

        public Date getModificationDate() {
            return mModificationDate;
        }

        public String getAppId() {
            return mAppId;
        }

        public String getDeviceName() {
            return mDeviceName;
        }

        public Long getSize() {
            return mSize;
        }

        public Long getSizeInKB() {
            return getSize() / 1024;
        }

        public Long getSizeInMB() {
            return getSizeInKB() / 1024;
        }
    }
}
