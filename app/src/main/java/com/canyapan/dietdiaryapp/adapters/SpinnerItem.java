package com.canyapan.dietdiaryapp.adapters;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;

import java.io.Serializable;

public class SpinnerItem implements Parcelable {
    private final String mText;
    @DrawableRes
    private final int mDrawable;
    private boolean mIsHint;
    private final Serializable mTag;

    public SpinnerItem(String text) {
        this(text, 0);
    }

    public SpinnerItem(String text, @DrawableRes int drawable) {
        this(text, drawable, false);
    }

    public SpinnerItem(String text, @DrawableRes int drawable, boolean isHint) {
        this(text, drawable, isHint, null);
    }

    public SpinnerItem(String text, @DrawableRes int drawable, boolean isHint, Serializable tag) {
        mText = text;
        mDrawable = drawable;
        mIsHint = isHint;
        mTag = tag;
    }

    protected SpinnerItem(Parcel in) {
        mText = in.readString();
        mDrawable = in.readInt();
        mIsHint = in.readInt() == 1;
        mTag = in.readSerializable();
    }

    public String getText() {
        return mText;
    }

    public Serializable getTag() {
        return mTag;
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

    public static final Creator<SpinnerItem> CREATOR = new Creator<SpinnerItem>() {
        @Override
        public SpinnerItem createFromParcel(Parcel in) {
            return new SpinnerItem(in);
        }

        @Override
        public SpinnerItem[] newArray(int size) {
            return new SpinnerItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mText);
        dest.writeInt(mDrawable);
        dest.writeInt(mIsHint ? 1 : 0);
        dest.writeSerializable(mTag);
    }
}
