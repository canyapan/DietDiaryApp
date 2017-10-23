package com.canyapan.dietdiaryapp;

import android.content.Context;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

class RestoreDialog extends AlertDialog {

    private final ProgressBar mProgressBar;

    RestoreDialog(Context context) {
        super(context);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_restore, (ViewGroup) null);
        setView(view);

        mProgressBar = view.findViewById(R.id.progressBar);
    }

}
