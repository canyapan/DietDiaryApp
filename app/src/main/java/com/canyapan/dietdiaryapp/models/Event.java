package com.canyapan.dietdiaryapp.models;

import android.os.Parcel;
import android.os.Parcelable;

import com.canyapan.dietdiaryapp.db.DatabaseHelper;

import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.text.MessageFormat;

public class Event implements Parcelable {
    public static final int TYPE_FOOD = 0;
    public static final int TYPE_DRINK = 1;
    public static final int TYPE_MED = 2;
    public static final int TYPE_SUPP = 3;
    public static final int TYPE_EXC = 4;
    public static final int TYPE_OTHER = 5;
    public static final Creator<Event> CREATOR = new Creator<Event>() {
        @Override
        public Event createFromParcel(Parcel in) {
            return new Event(in);
        }

        @Override
        public Event[] newArray(int size) {
            return new Event[size];
        }
    };
    private long mID;
    private LocalDate mDate;
    private LocalTime mTime;
    private int mType;
    private int mSubType;
    private String mDescription;

    public Event() {
        setID(-1);
    }

    public Event(long id, LocalDate date, LocalTime time, int type, int subType, String description) {
        setID(id);
        setDate(date);
        setTime(time);
        setType(type);
        setSubType(subType);
        setDescription(description);
    }

    protected Event(Parcel in) {
        mID = in.readLong();
        mDate = (LocalDate) in.readSerializable();
        mTime = (LocalTime) in.readSerializable();
        mType = in.readInt();
        mSubType = in.readInt();
        mDescription = in.readString();
    }

    public long getID() {
        return mID;
    }

    public void setID(long ID) {
        this.mID = ID;
    }

    public LocalDate getDate() {
        return mDate;
    }

    public void setDate(LocalDate date) {
        this.mDate = date;
    }

    public LocalTime getTime() {
        return mTime;
    }

    public void setTime(LocalTime time) {
        this.mTime = time;
    }

    public int getType() {
        return mType;
    }

    public void setType(int type) {
        this.mType = type;
    }

    public int getSubType() {
        return mSubType;
    }

    public void setSubType(int subType) {
        this.mSubType = subType;
    }

    public String getDescription() {
        return mDescription;
    }

    public void setDescription(String description) {
        this.mDescription = description;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(mID);
        dest.writeSerializable(mDate);
        dest.writeSerializable(mTime);
        dest.writeInt(mType);
        dest.writeInt(mSubType);
        dest.writeString(mDescription);
    }

    @Override
    public boolean equals(final Object obj) {
        if (null != obj && obj instanceof Event) {
            final Event event = (Event) obj;
            if (getID() == event.getID()
                    && getType() == event.getType()
                    && getSubType() == event.getSubType()
                    && getDate().equals(event.getDate())
                    && getTime().equals(event.getTime())
                    && getDescription().equals(event.getDescription()))
                return true;
        }

        return false;
    }

    @Override
    public String toString() {
        return MessageFormat.format("[ID]={0,number}, [Date]={1}, [Time]={2}, [Type]={3,number}, [SubType]={4,number}, [Description]={5}",
                getID(),
                getDate().toString(DatabaseHelper.DB_DATE_FORMATTER),
                getTime().toString(DatabaseHelper.DB_TIME_FORMATTER),
                getType(),
                getSubType(),
                getDescription());
    }
}
