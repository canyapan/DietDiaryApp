package com.canyapan.dietdiaryapp.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.canyapan.dietdiaryapp.helpers.DailyReminderServiceHelper;


public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED")) {
            DailyReminderServiceHelper.setup(context);
        }
    }
}
