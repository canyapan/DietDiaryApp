package com.canyapan.dietdiaryapp;

import android.content.Context;
import android.content.DialogInterface;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class AboutDialog extends AlertDialog {
    protected AboutDialog(Context context) {
        super(context);

        final LayoutInflater inflater = LayoutInflater.from(context);
        final View view = inflater.inflate(R.layout.dialog_about, (ViewGroup) null);
        setView(view);

        TextView ver = (TextView) view.findViewById(R.id.appVersion);
        ver.setText(BuildConfig.VERSION_NAME);

        setButton(BUTTON_POSITIVE, context.getString(android.R.string.ok), (DialogInterface.OnClickListener) null);
    }
}
