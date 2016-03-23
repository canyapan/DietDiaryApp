package com.canyapan.dietdiaryapp.adapters;

import android.support.annotation.DrawableRes;

public class SpinnerItem {
    private final String mText;
    @DrawableRes
    private final int mDrawable;
    private boolean mIsHint;

    public SpinnerItem(String text) {
        this(text, 0);
    }

    public SpinnerItem(String text, @DrawableRes int drawable) {
        this(text, drawable, false);
    }

    public SpinnerItem(String text, @DrawableRes int drawable, boolean isHint) {
        mText = text;
        mDrawable = drawable;
        mIsHint = isHint;
    }

    public String getText() {
        return mText;
    }

    public int getDrawable() {
        return mDrawable;
    }

    public boolean isHint() {
        return mIsHint;
    }

    public void setHint() {
        mIsHint = true;
    }
}
