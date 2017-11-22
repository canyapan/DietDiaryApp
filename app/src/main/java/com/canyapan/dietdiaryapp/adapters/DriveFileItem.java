package com.canyapan.dietdiaryapp.adapters;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.drive.Metadata;

import org.joda.time.LocalDateTime;

import java.util.Locale;

import static com.canyapan.dietdiaryapp.services.DriveBackupService.DRIVE_KEY_APP_ID;
import static com.canyapan.dietdiaryapp.services.DriveBackupService.DRIVE_KEY_DEVICE_NAME;

public class DriveFileItem implements Parcelable {
    private final String mId;
    private final String mTitle;
    private final LocalDateTime mCreationDate;
    private final LocalDateTime mModificationDate;
    private final Long mSize;
    private final String mAppId;
    private final String mDeviceName;

    public DriveFileItem(final Metadata m) {
        mId = m.getDriveId().getResourceId();
        mTitle = m.getTitle();
        mCreationDate = new LocalDateTime(m.getCreatedDate());
        mModificationDate = new LocalDateTime(m.getModifiedDate());
        mSize = m.getFileSize();

        mAppId = m.getCustomProperties().get(DRIVE_KEY_APP_ID);
        mDeviceName = m.getCustomProperties().get(DRIVE_KEY_DEVICE_NAME);
    }

    //region Getters
    public String getId() {
        return mId;
    }

    String getTitle() {
        return mTitle;
    }

    LocalDateTime getCreationDate() {
        return mCreationDate;
    }

    LocalDateTime getModificationDate() {
        return mModificationDate;
    }

    String getAppId() {
        return mAppId;
    }

    String getDeviceName() {
        return mDeviceName;
    }

    Long getSize() {
        return mSize;
    }

    String getSizeHumanized() {
        if (1f < getSize() / 1024f) {
            return String.format(Locale.getDefault(), "%,.2fMB", (getSize() / 1024f / 1024f));
        } else {
            return String.format(Locale.getDefault(), "%,.2fKB", (getSize() / 1024f));
        }
    }
    //endregion

    //region Parcelable
    private DriveFileItem(Parcel in) {
        mId = in.readString();
        mTitle = in.readString();
        mCreationDate = (LocalDateTime) in.readSerializable();
        mModificationDate = (LocalDateTime) in.readSerializable();
        mSize = in.readLong();
        mAppId = in.readString();
        mDeviceName = in.readString();
    }

    public static final Creator<DriveFileItem> CREATOR = new Creator<DriveFileItem>() {
        @Override
        public DriveFileItem createFromParcel(Parcel in) {
            return new DriveFileItem(in);
        }

        @Override
        public DriveFileItem[] newArray(int size) {
            return new DriveFileItem[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mId);
        dest.writeString(mTitle);
        dest.writeSerializable(mCreationDate);
        dest.writeSerializable(mModificationDate);
        dest.writeLong(mSize);
        dest.writeString(mAppId);
        dest.writeString(mDeviceName);
    }
    //endregion
}
