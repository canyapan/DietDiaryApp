package com.canyapan.dietdiaryapp.adapters;

import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.DrawableRes;
import android.support.annotation.NonNull;

public class EventTypeItem implements Parcelable {
    private final int mId;
    private final String mText;
    @DrawableRes
    private final int mIcon;

    public EventTypeItem(final int id, @NonNull final String text, @DrawableRes final int icon) {
        mId = id;
        mText = text;
        mIcon = icon;
    }

    //region Getters
    public int getId() {
        return mId;
    }

    @NonNull
    public String getText() {
        return mText;
    }

    @DrawableRes
    public int getIcon() {
        return mIcon;
    }
    //endregion

    //region Parcelable
    private EventTypeItem(Parcel in) {
        mId = in.readInt();
        mText = in.readString();
        mIcon = in.readInt();
    }

    public static final Creator<EventTypeItem> CREATOR = new Creator<EventTypeItem>() {
        @Override
        public EventTypeItem createFromParcel(Parcel in) {
            return new EventTypeItem(in);
        }

        @Override
        public EventTypeItem[] newArray(int size) {
            return new EventTypeItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(mId);
        dest.writeString(mText);
        dest.writeInt(mIcon);
    }
    //endregion
}
