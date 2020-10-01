package com.seventyseven.internetspeedindicator.Service;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;

import com.seventyseven.internetspeedindicator.Utils.Settings;

import java.util.Map;

public final class IndicatorServiceHelper {
    private static Intent getServiceIntent(Context context) {
        SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);

        Intent serviceIntent = new Intent(context, IndicatorService.class);

        // Add all preferences to intent
        for (Map.Entry<String, ?> entry : sharedPref.getAll().entrySet()) {
            if (entry.getValue() instanceof Boolean) {
                serviceIntent.putExtra(entry.getKey(), (boolean)(Object)entry.getValue());
            } else if (entry.getValue() instanceof String) {
                serviceIntent.putExtra(entry.getKey(), (String)entry.getValue());
            }
        }

        return serviceIntent;
    }

    public static void startService(Context context) {
//        context.startService(getServiceIntent(context));

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(getServiceIntent(context));
        } else {
            context.startService(getServiceIntent(context));
        }

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(Settings.KEY_INDICATOR_STARTED, true)
                .apply();
    }

    public static void stopService(Context context) {
        context.stopService(new Intent(context, IndicatorService.class));

        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(Settings.KEY_INDICATOR_STARTED, false)
                .apply();
    }
}
