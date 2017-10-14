package com.canyapan.dietdiaryapp.adapters;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

public class RestoreFileItem implements Parcelable {
    private final String mText;
    private final Serializable mTag;

    public RestoreFileItem(@NonNull final String text, final Serializable tag) {
        mText = text;
        mTag = tag;
    }

    //region Getters
    @NonNull
    public String getText() {
        return mText;
    }

    @Nullable
    public Serializable getTag() {
        return mTag;
    }
    //endregion

    //region Parcelable
    private RestoreFileItem(Parcel in) {
        mText = in.readString();
        mTag = in.readSerializable();
    }

    public static final Creator<RestoreFileItem> CREATOR = new Creator<RestoreFileItem>() {
        @Override
        public RestoreFileItem createFromParcel(Parcel in) {
            return new RestoreFileItem(in);
        }

        @Override
        public RestoreFileItem[] newArray(int size) {
            return new RestoreFileItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mText);
        dest.writeSerializable(mTag);
    }
    //endregion
}
