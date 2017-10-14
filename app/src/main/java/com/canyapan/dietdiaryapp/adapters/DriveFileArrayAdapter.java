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

import org.joda.time.format.DateTimeFormat;

import java.util.List;

public class DriveFileArrayAdapter extends ArrayAdapter<DriveFileItem> {
    @LayoutRes
    private static final int RES_ITEM_LAYOUT = R.layout.drive_file_item;

    private final LayoutInflater mInflater;

    public DriveFileArrayAdapter(final Context context, final List<DriveFileItem> items) {
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

        final TextView tvDeviceName = v.findViewById(R.id.tvDeviceName);
        final TextView tvDate = v.findViewById(R.id.tvDate);
        final TextView tvSize = v.findViewById(R.id.tvSize);

        final DriveFileItem item = getItem(position);
        tvDeviceName.setText(item.getDeviceName());
        tvDate.setText(DateTimeFormat.shortDateTime().print(item.getModificationDate()));
        tvSize.setText(item.getSizeHumanized());

        return v;
    }
}
