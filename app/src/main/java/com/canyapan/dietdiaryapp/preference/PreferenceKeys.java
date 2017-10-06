package com.canyapan.dietdiaryapp.preference;

public class PreferenceKeys {
    public static final String KEY_APP_ID = "Application ID";
    public static final String KEY_GENERAL_CLOCK_MODE_STRING = "General Settings - Clock Mode"; // -1, 0, 1
    public static final String KEY_NOTIFICATIONS_ACTIVE_BOOL = "Notification Settings - Active";
    public static final String KEY_NOTIFICATIONS_DAILY_REMAINDER_BOOL = "Notification Settings - Daily Remainder - Active";
    public static final String KEY_NOTIFICATIONS_DAILY_REMAINDER_TIME_STRING = "Notification Settings - Daily Remainder - Time"; // 19:00
    public static final String KEY_BACKUP_ACTIVE_BOOL = "Backup Settings - Active";
    public static final String KEY_BACKUP_FREQUENCY_STRING = "Backup Settings - Frequency"; // -1, 0, 1, 2
    public static final String KEY_BACKUP_WIFI_ONLY_BOOL = "Backup Settings - Wifi Only";
    public static final String KEY_BACKUP_NOW_ACT = "Backup Settings - Backup Now"; // Doesn't keep any data
    public static final String KEY_BACKUP_LAST_BACKUP_TIMESTAMP_LONG = "Backup Settings - Last Backup";
    public static final String KEY_BACKUP_FILE_DRIVE_ID_STRING = "Backup Settings - Drive ID";

    private PreferenceKeys() {
    }
}
