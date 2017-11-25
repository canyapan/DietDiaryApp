package com.canyapan.dietdiaryapp.fragments;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.preference.ListPreference;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.support.v7.preference.SwitchPreferenceCompat;
import android.text.format.DateUtils;
import android.util.Log;
import android.widget.ProgressBar;

import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.adapters.DriveFileArrayAdapter;
import com.canyapan.dietdiaryapp.adapters.DriveFileItem;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.canyapan.dietdiaryapp.helpers.DateTimeHelper;
import com.canyapan.dietdiaryapp.helpers.DriveBackupServiceHelper;
import com.canyapan.dietdiaryapp.preference.TimePreferenceCompat;
import com.canyapan.dietdiaryapp.preference.TimePreferenceDialogFragmentCompat;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.android.gms.drive.Drive;
import com.google.android.gms.drive.DriveClient;
import com.google.android.gms.drive.DriveFolder;
import com.google.android.gms.drive.DriveResourceClient;
import com.google.android.gms.drive.Metadata;
import com.google.android.gms.drive.MetadataBuffer;
import com.google.android.gms.drive.query.Filters;
import com.google.android.gms.drive.query.Query;
import com.google.android.gms.drive.query.SearchableField;
import com.google.android.gms.drive.query.SortOrder;
import com.google.android.gms.drive.query.SortableField;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static android.app.Activity.RESULT_OK;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_ACTIVE_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_FILE_DRIVE_ID_STRING;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_FREQUENCY_STRING;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_LAST_BACKUP_TIMESTAMP_LONG;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_BACKUP_NOW_ACT;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_GENERAL_CLOCK_MODE_STRING;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_ACTIVE_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL;
import static com.canyapan.dietdiaryapp.preference.PreferenceKeys.KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING;

public class SettingsSupportFragment extends PreferenceFragmentCompat
        implements Preference.OnPreferenceChangeListener, SharedPreferences.OnSharedPreferenceChangeListener, RestoreDialog.OnRestoreListener {

    private static final String TAG = "Settings";
    private static final String DIALOG_FRAGMENT_TAG =
            "android.support.v7.preference.PreferenceFragment.DIALOG";

    public static final int FLAG_ACTIVATE_BACKUP = 1;
    private static final String KEY_ACTIVATE_BACKUP_BOOLEAN = "ACTIVATE BACKUP";

    private static final int REQUEST_RESOLVE_ERROR = 1001;

    private RestoreDialog mRestoreDialog = null;
    private DriveClient mDriveClient = null;
    private DriveResourceClient mDriveResourceClient = null;

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
                    // have the permission or version is lower
                    loadDriveApiClients();

                    return false; // will be handled if drive connection successful.
                }

                return true; // let it
            }
        });

        if (!backupActivePref.isChecked() && activateBackup) {
            // This will fire the preference changed event, above.
            // But, it won't let to change it. It will control the change itself.
            // Therefore we won't try this here.
            backupActivePref.callChangeListener(true);
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

        if (null != mRestoreDialog && !mRestoreDialog.isEnded()) {
            mRestoreDialog.show();
        }

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
        if (timestamp < 0) { // Not backed up, yet.
            preference.setSummary(preference.getContext().getString(R.string.pref_title_backup_now_summary, preference.getContext().getString(R.string.pref_title_backup_now_summary_never)));
        } else { // Backed up at timestamp
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
    public void onDetach() {
        getPreferenceManager().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        super.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            if (resultCode == RESULT_OK) {
                Task<GoogleSignInAccount> getAccountTask =
                        GoogleSignIn.getSignedInAccountFromIntent(data);
                if (getAccountTask.isSuccessful()) {
                    initializeDriveClient(getAccountTask.getResult());
                } else {
                    Log.e(TAG, "Sign-in failed.");
                    // TODO show this
                }
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

    private void activateBackup() {
        final SwitchPreferenceCompat backupActivePref = (SwitchPreferenceCompat) findPreference(KEY_BACKUP_ACTIVE_BOOL);
        if (!backupActivePref.isChecked()) {
            backupActivePref.setChecked(true);
        }

        changeProgressBarVisibility(false);
    }

    private void changeProgressBarVisibility(boolean visible) {
        ProgressBar progressBar = getActivity().findViewById(R.id.toolbarProgressBar);
        progressBar.setVisibility(visible ? ProgressBar.VISIBLE : ProgressBar.GONE);
    }

    private void setDriveFileId(String driveId) {
        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(KEY_BACKUP_FILE_DRIVE_ID_STRING, driveId);
        editor.apply();
    }

    private void showSelectDriveBackupDialog(List<DriveFileItem> files) {
        // Show a dialog to get user's choice
        final DriveFileArrayAdapter arrayAdapter = new DriveFileArrayAdapter(getContext(), files);

        new AlertDialog.Builder(getContext())
                .setTitle(R.string.pref_backup_restore_title)
                .setNegativeButton(R.string.pref_backup_restore_not, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        Log.d(TAG, "Not restore selected");
                        setDriveFileId("");
                        activateBackup();

                        dialog.dismiss();
                    }
                }).setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                final DriveFileItem file = arrayAdapter.getItem(which);
                if (null != file) {
                    Log.d(TAG, file.getId() + " selected.");

                    // import data @file in background
                    try {
                        mRestoreDialog = new RestoreDialog(getContext(), mDriveClient, mDriveResourceClient, file.getId(), SettingsSupportFragment.this);
                        mRestoreDialog.show();
                    } catch (RestoreException e) {
                        e.printStackTrace();
                        mRestoreDialog = null;
                    }
                }
            }
        }).setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                changeProgressBarVisibility(false);
            }
        }).show();
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

    private void loadDriveApiClients() {
        Set<Scope> requiredScopes = new HashSet<>(2);
        requiredScopes.add(Drive.SCOPE_FILE);
        requiredScopes.add(Drive.SCOPE_APPFOLDER);

        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(getContext());
        if (signInAccount != null && signInAccount.getGrantedScopes().containsAll(requiredScopes)) {
            initializeDriveClient(signInAccount);
        } else {
            GoogleSignInOptions signInOptions =
                    new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                            .requestScopes(Drive.SCOPE_FILE)
                            .requestScopes(Drive.SCOPE_APPFOLDER)
                            .build();
            GoogleSignInClient googleSignInClient = GoogleSignIn.getClient(getActivity(), signInOptions);
            startActivityForResult(googleSignInClient.getSignInIntent(), REQUEST_RESOLVE_ERROR);
        }
    }

    private void initializeDriveClient(GoogleSignInAccount signInAccount) {
        mDriveClient = Drive.getDriveClient(getContext(), signInAccount);
        mDriveResourceClient = Drive.getDriveResourceClient(getContext(), signInAccount);

        onDriveClientReady();
    }

    private void onDriveClientReady() {
        Log.d(TAG, "Drive API is ready!");

        SharedPreferences preferences = getPreferenceManager().getSharedPreferences();
        String driveFileID = preferences.getString(KEY_BACKUP_FILE_DRIVE_ID_STRING, null);

        if (null == driveFileID) { // This is the first time connecting to drive
            // 1- Check drive contents
            // 2- Ask user to choose a backup to restore if found any
            mDriveResourceClient.getAppFolder()
                    .continueWithTask(new Continuation<DriveFolder, Task<MetadataBuffer>>() {
                        @Override
                        public Task<MetadataBuffer> then(@NonNull Task<DriveFolder> task) throws Exception {
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

                            return mDriveResourceClient.queryChildren(task.getResult(), query);
                        }
                    })
                    .addOnSuccessListener(new OnSuccessListener<MetadataBuffer>() {
                        @Override
                        public void onSuccess(MetadataBuffer metadata) {
                            if (metadata.getCount() != 0) {
                                // Found some backup files saved in the drive.
                                // show user if they want to restore from any of these backups

                                List<DriveFileItem> files = new ArrayList<>(metadata.getCount());
                                for (Metadata m : metadata) {
                                    files.add(new DriveFileItem(m));
                                    Log.d(TAG, String.format(Locale.getDefault(), "%s %,.2fKB", m.getDriveId().getResourceId(), (m.getFileSize() / 1024f)));
                                }

                                showSelectDriveBackupDialog(files);
                            } else {
                                // No backup found.. Activate the drive backup.
                                setDriveFileId("");
                                activateBackup();
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.e(TAG, "Cannot query current backups.", e);
                            //TODO: show a toast about failure
                        }
                    });
        } else {
            activateBackup();
        }

    }

    @Override
    public void onRestoreComplete(String driveId, LocalDate startDate, LocalDate endDate, long recordsInserted) {
        mRestoreDialog.dismiss();
        mRestoreDialog = null;

        // set DriveFileId so next updates will overwrite that drive id.
        setDriveFileId(driveId);
    }

    @Override
    public void onRestoreFailed(String tag, String message) {
        mRestoreDialog.dismiss();
        mRestoreDialog = null;
    }
}
