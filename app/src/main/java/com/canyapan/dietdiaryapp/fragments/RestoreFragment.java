package com.canyapan.dietdiaryapp.fragments;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.Toast;

import com.canyapan.dietdiaryapp.Application;
import com.canyapan.dietdiaryapp.BuildConfig;
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.adapters.RestoreFileArrayAdapter;
import com.canyapan.dietdiaryapp.adapters.RestoreFileItem;
import com.crashlytics.android.Crashlytics;

import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.LocalDate;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class RestoreFragment extends Fragment implements RestoreDialog.OnRestoreListener {
    public static final String TAG = "RestoreFragment";
    private static final String KEY_SELECTED_FILE_INDEX_INT = "SELECTED FILE";
    private static final String KEY_FILES_PARCELABLE = "FILES";
    private static final int REQUEST_EXTERNAL_STORAGE = 20;

    protected OnFragmentInteractionListener mListener;

    protected LinearLayout mLinearLayout;
    private Spinner mSpinner;

    private ArrayList<RestoreFileItem> mSpinnerItems = null;

    protected RestoreDialog mRestoreDialog;

    public RestoreFragment() {
    }

    public static RestoreFragment newInstance() {
        return new RestoreFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLinearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_restore_linearlayout, container, false);

        mSpinner = mLinearLayout.findViewById(R.id.spFiles);

        if (savedInstanceState != null) {
            mSpinnerItems = savedInstanceState.getParcelableArrayList(KEY_FILES_PARCELABLE);
            int selectedIndex = savedInstanceState.getInt(KEY_SELECTED_FILE_INDEX_INT);

            mSpinner.setAdapter(new RestoreFileArrayAdapter(getContext(), mSpinnerItems));
            mSpinner.setSelection(selectedIndex);
        }

        if (null == mSpinnerItems) {
            loadSpinnerItems();
        }

        if (null != mRestoreDialog) {
            // Show dialog again. Probably switched orientation
            mRestoreDialog.show();
        }

        return mLinearLayout;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(KEY_FILES_PARCELABLE, mSpinnerItems);
        outState.putInt(KEY_SELECTED_FILE_INDEX_INT, mSpinner.getSelectedItemPosition());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_restore_fragment, menu);

        Log.d(TAG, "External storage state: " + Environment.getExternalStorageState());
        if (!Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)
                && !Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED_READ_ONLY)) {
            menu.findItem(R.id.action_save).setEnabled(false);
        }

        super.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                if (null == mSpinnerItems || mSpinnerItems.size() == 1) {
                    // Fail safety.
                    return true;
                }

                if (mSpinner.getSelectedItemPosition() == 0) {
                    // Skip the select helper.
                    return true;
                }

                try {
                    File f = (File) mSpinnerItems.get(mSpinner.getSelectedItemPosition()).getTag();
                    mRestoreDialog = new RestoreDialog(getContext(), f, this);
                } catch (RestoreException e) {
                    if (BuildConfig.CRASHLYTICS_ENABLED) {
                        Crashlytics.logException(e);
                    }
                    Log.e(TAG, "Import from external storage unsuccessful.", e);
                }

                return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void loadSpinnerItems() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getActivity().checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                getActivity().requestPermissions(
                        new String[]{
                                Manifest.permission.READ_EXTERNAL_STORAGE
                        }, REQUEST_EXTERNAL_STORAGE);

                return;
            }
        }

        Log.d(TAG, "Loading items...");
        File[] files = getSupportedFiles();

        mSpinnerItems = new ArrayList<>(files.length);
        mSpinnerItems.add(new RestoreFileItem(getString(R.string.restore_spinner_hint), null));
        for (File f : files) {
            mSpinnerItems.add(new RestoreFileItem(f.getName(), f));
        }

        mSpinner.setAdapter(new RestoreFileArrayAdapter(getContext(), mSpinnerItems));
    }

    private File[] getSupportedFiles() {
        final File dirApp = new File(
                Environment.getExternalStorageDirectory(),
                Application.APP_DIR);

        return ArrayUtils.addAll(getSupportedFiles(dirApp), getSupportedFiles(Environment.getExternalStorageDirectory()));
    }

    private File[] getSupportedFiles(File dir) {
        return dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String fileName) {
                fileName = fileName.toLowerCase();
                return fileName.endsWith(".json") || fileName.endsWith(".csv");
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnFragmentInteractionListener) {
            mListener = (OnFragmentInteractionListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFragmentInteractionListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mListener = null;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == REQUEST_EXTERNAL_STORAGE
                && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSpinnerItems();
        } else {
            Toast.makeText(getContext(), R.string.restore_no_permission, Toast.LENGTH_LONG).show();
        }
    }

    @Override
    public void onRestoreComplete(String path, LocalDate startDate, LocalDate endDate, long recordsInserted) {
        if (mRestoreDialog.isShowing()) {
            mRestoreDialog.dismiss();
        }

        final File f = new File(path);
        Snackbar.make(mLinearLayout, getString(R.string.restore_successful, f.getName()), Snackbar.LENGTH_SHORT).show();

        mListener.onRestoreComplete(path, startDate, endDate);
    }

    @Override
    public void onRestoreFailed(String path, String message) {
        if (mRestoreDialog.isShowing()) {
            mRestoreDialog.dismiss();
        }

        Snackbar.make(mLinearLayout, message, Snackbar.LENGTH_INDEFINITE)
                .setAction(R.string.snack_bar_dismiss, null).show();
    }

    public interface OnFragmentInteractionListener {
        void onRestoreComplete(String path, LocalDate startDate, LocalDate endDate);
    }

}
