package com.canyapan.dietdiaryapp;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.crashlytics.android.Crashlytics;

import java.io.File;
import java.io.FileNotFoundException;

public class SharingSupportProvider extends ContentProvider {
    public static final String CONTENT_URI_PREFIX = "content://com.canyapan.dietdiaryapp.provider/";
    public static final String MIME_TYPE_CSV = "text/csv";
    public static final String MIME_TYPE_HTML = "text/html";
    public static final String MIME_TYPE_JSON = "application/json";
    private static final String TAG = "SharingSupportProvider";

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        if (uri.getPath().endsWith(".csv")) {
            return MIME_TYPE_CSV;
        } else if (uri.getPath().endsWith(".html")) {
            return MIME_TYPE_HTML;
        } else if (uri.getPath().endsWith(".json")) {
            return MIME_TYPE_JSON;
        }

        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    @Nullable
    @Override
    public ParcelFileDescriptor openFile(@NonNull Uri uri, @NonNull String mode) throws FileNotFoundException {
        //noinspection ConstantConditions
        File f = new File(getContext().getCacheDir(), uri.getPath());
        if (mode.equals("r") && (f.exists() && (f.getName().endsWith(".json") || f.getName().endsWith(".csv") || f.getName().endsWith(".html")))) {
            try {
                //noinspection ConstantConditions
                return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
            } catch (FileNotFoundException e) {
                if (BuildConfig.CRASHLYTICS_ENABLED) {
                    Crashlytics.logException(e);
                }
                Log.e(TAG, "Bad file " + uri);
            }
        }

        return super.openFile(uri, mode);
    }
}
