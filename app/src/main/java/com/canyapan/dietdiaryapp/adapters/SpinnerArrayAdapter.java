package com.canyapan.dietdiaryapp.adapters;

import android.content.Context;
import android.content.res.TypedArray;
import android.support.annotation.ArrayRes;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.canyapan.dietdiaryapp.R;

import java.util.List;

public class SpinnerArrayAdapter extends ArrayAdapter<SpinnerItem> {
    @LayoutRes
    private final int mLayoutResource;
    private final LayoutInflater mInflater;
    private final int mOffset;

    public SpinnerArrayAdapter(final Context context, @ArrayRes int texts, @ArrayRes int icons, boolean hasHint, int offset) {
        super(context, R.layout.spinner_dropdown_item);
        final String[] arrTexts = context.getResources().getStringArray(texts);
        final TypedArray arrIcons = icons > 0 ? context.getResources().obtainTypedArray(icons) : null;
        mOffset = offset;

        if (offset >= arrTexts.length) {
            throw new IllegalArgumentException("Offset >= Array.length");
        } else if (offset < 0) {
            throw new IllegalArgumentException("Offset < 0");
        }

        for (int i = offset; i < arrTexts.length; i++) {
            //noinspection ResourceType
            add(new SpinnerItem(arrTexts[i], ((null != arrIcons) ? arrIcons.getResourceId(i, 0) : 0)));
        }

        if (null != arrIcons) {
            arrIcons.recycle();
        }

        if (hasHint) {
            getItem(0).setHint();
        }

        mInflater = LayoutInflater.from(context);
        mLayoutResource = R.layout.spinner_dropdown_item;
    }

    public SpinnerArrayAdapter(final Context context, List<SpinnerItem> items) {
        super(context, R.layout.spinner_dropdown_item, items);
        mInflater = LayoutInflater.from(context);
        mLayoutResource = R.layout.spinner_dropdown_item;
        mOffset = 0;
    }

    public int getOffset() {
        return mOffset;
    }

    @Override
    public View getDropDownView(int position, View convertView, @NonNull ViewGroup parent) {
        return getView(position, convertView, parent);
    }

    @Override
    public boolean isEnabled(int position) {
        return position != 0 || !getItem(position).isHint();
    }

    @Override
    @NonNull
    public View getView(int position, View convertView, @NonNull ViewGroup parent) {
        final LinearLayout layout;

        if (convertView == null) {
            layout = (LinearLayout) mInflater.inflate(mLayoutResource, parent, false);
        } else {
            layout = (LinearLayout) convertView;
        }

        final ImageView imageView = (ImageView) layout.findViewById(R.id.ivIcon);
        final TextView textView = (TextView) layout.findViewById(R.id.tvText);

        final SpinnerItem item = getItem(position);

        imageView.setImageResource(item.getDrawable());

        if (item.isHint()) { // Load the hint
            textView.setText("");
            textView.setHint(item.getText());
        } else {
            textView.setText(item.getText());
        }

        return layout;
    }
}
