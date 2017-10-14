package com.canyapan.dietdiaryapp.adapters;

import android.content.Context;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.canyapan.dietdiaryapp.R;

import java.util.List;

public class RestoreFileArrayAdapter extends ArrayAdapter<RestoreFileItem> {
    @LayoutRes
    private static final int RES_ITEM_LAYOUT = R.layout.spinner_dropdown_item;

    private final LayoutInflater mInflater;

    public RestoreFileArrayAdapter(final Context context, final List<RestoreFileItem> items) {
        super(context, RES_ITEM_LAYOUT, items);
        mInflater = LayoutInflater.from(context);
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final View v;
        if (convertView == null) {
            v = mInflater.inflate(RES_ITEM_LAYOUT, parent, false);
        } else {
            v = convertView;
        }

        final TextView tvText = v.findViewById(R.id.tvText);

        final RestoreFileItem item = getItem(position);

        if (position == 0) { // Load the hint
            tvText.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            tvText.setText("");
            tvText.setHint(item.getText());
        } else {
            tvText.setCompoundDrawablesWithIntrinsicBounds(R.drawable.file_restore, 0, 0, 0);
            tvText.setText(item.getText());
        }

        return v;
    }
}
