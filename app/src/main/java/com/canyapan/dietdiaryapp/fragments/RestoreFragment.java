package com.canyapan.dietdiaryapp.fragments;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
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
import com.canyapan.dietdiaryapp.R;
import com.canyapan.dietdiaryapp.adapters.SpinnerArrayAdapter;
import com.canyapan.dietdiaryapp.adapters.SpinnerItem;
import com.canyapan.dietdiaryapp.db.DatabaseHelper;
import com.crashlytics.android.Crashlytics;

import org.apache.commons.lang3.ArrayUtils;
import org.joda.time.LocalDate;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;

public class RestoreFragment extends Fragment {
    public static final String TAG = "RestoreFragment";
    private static final String KEY_SELECTED_FILE_INDEX_INT = "SELECTED FILE";
    private static final String KEY_FILES_PARCELABLE = "FILES";
    private static final int REQUEST_EXTERNAL_STORAGE = 20;

    protected OnFragmentInteractionListener mListener;

    protected LinearLayout mLinearLayout;
    private Spinner mSpinner;

    private ArrayList<SpinnerItem> mSpinnerItems = null;

    protected DatabaseHelper mDatabaseHelper;
    protected ProgressDialog mProgressDialog;
    protected RestoreAsyncTask mAsyncTask = null;

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

        mDatabaseHelper = new DatabaseHelper(getContext());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mLinearLayout = (LinearLayout) inflater.inflate(R.layout.fragment_import_linearlayout, container, false);

        mSpinner = (Spinner) mLinearLayout.findViewById(R.id.spFiles);

        if (savedInstanceState != null) {
            mSpinnerItems = savedInstanceState.getParcelableArrayList(KEY_FILES_PARCELABLE);
            int selectedIndex = savedInstanceState.getInt(KEY_SELECTED_FILE_INDEX_INT);

            mSpinner.setAdapter(new SpinnerArrayAdapter(getContext(), mSpinnerItems));
            mSpinner.setSelection(selectedIndex);
        }

        if (null == mSpinnerItems) {
            loadSpinnerItems();
        }

        if (null != mAsyncTask) {
            // To start progress dialog again.
            mAsyncTask.onPreExecute();
        }

        return mLinearLayout;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelableArrayList(KEY_FILES_PARCELABLE, mSpinnerItems);
        outState.putInt(KEY_SELECTED_FILE_INDEX_INT, mSpinner.getSelectedItemPosition());
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_import_fragment, menu);

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
                    mAsyncTask = (RestoreAsyncTask) new RestoreAsyncTask(this, (File) mSpinnerItems.get(mSpinner.getSelectedItemPosition()).getTag()).execute();
                } catch (RestoreException e) {
                    Crashlytics.logException(e);
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
        mSpinnerItems.add(new SpinnerItem(getString(R.string.import_spinner_hint), R.drawable.tab_import, true));
        for (File f : files) {
            mSpinnerItems.add(new SpinnerItem(f.getName(), f.getName().toLowerCase().endsWith(".csv") ?
                    R.drawable.file_delimited : R.drawable.file, false, f));
        }

        mSpinner.setAdapter(new SpinnerArrayAdapter(getContext(), mSpinnerItems));
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
            public boolean accept(File dir, String filename) {
                return filename.toLowerCase().endsWith(".csv");
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
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            loadSpinnerItems();
        } else {
            Toast.makeText(getContext(), R.string.import_no_permission, Toast.LENGTH_LONG).show();
        }
    }

    public interface OnFragmentInteractionListener {
        void onImportComplete(Uri uri, LocalDate startDate, LocalDate endDate);
    }

}
