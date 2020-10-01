package com.seventyseven.internetspeedindicator.Service;

import android.app.KeyguardManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.net.TrafficStats;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.seventyseven.internetspeedindicator.Utils.Settings;
import com.seventyseven.internetspeedindicator.Utils.NetworkUtil;
import com.seventyseven.internetspeedindicator.Utils.RetrieveData;
import com.seventyseven.internetspeedindicator.Utils.Speed;
import com.seventyseven.internetspeedindicator.Utils.StoredData;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;

public final class IndicatorService extends Service {
    private KeyguardManager mKeyguardManager;

    private long mLastRxBytes = 0;
    private long mLastTxBytes = 0;
    private long mLastTime = 0;

    private Speed mSpeed;

    private IndicatorNotification mIndicatorNotification;

    private boolean mNotificationCreated = false;

    private boolean mNotificationOnLockScreen;

    final private Handler mHandler = new Handler();

    public static final String MONTH_DATA = "monthdata";
    public static final String TODAY_DATA = "todaydata";
    Thread dataThread;

    private final Runnable mHandlerRunnable = new Runnable() {
        @Override
        public void run() {
            long currentRxBytes = TrafficStats.getTotalRxBytes();
            long currentTxBytes = TrafficStats.getTotalTxBytes();
            long usedRxBytes = currentRxBytes - mLastRxBytes;
            long usedTxBytes = currentTxBytes - mLastTxBytes;
            long currentTime = System.currentTimeMillis();
            long usedTime = currentTime - mLastTime;

            mLastRxBytes = currentRxBytes;
            mLastTxBytes = currentTxBytes;
            mLastTime = currentTime;

            mSpeed.calcSpeed(usedTime, usedRxBytes, usedTxBytes);

            mIndicatorNotification.updateNotification(mSpeed);

            mHandler.postDelayed(this, 1000);
        }
    };

    private final BroadcastReceiver mScreenBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }

            if (intent.getAction().equals(Intent.ACTION_SCREEN_OFF)) {
                pauseNotifying();
                if (!mNotificationOnLockScreen) {
                    mIndicatorNotification.hideNotification();
                }
                mIndicatorNotification.updateNotification(mSpeed);
            } else if (intent.getAction().equals(Intent.ACTION_SCREEN_ON)) {
                if (mNotificationOnLockScreen || !mKeyguardManager.isKeyguardLocked()) {
                    mIndicatorNotification.showNotification();
                    restartNotifying();
                }
            } else if (intent.getAction().equals(Intent.ACTION_USER_PRESENT)) {
                mIndicatorNotification.showNotification();
                restartNotifying();
            }
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public void onDestroy() {
        super.onDestroy();

        Log.v("parth","onDestroy");

        pauseNotifying();
        unregisterReceiver(mScreenBroadcastReceiver);

        removeNotification();
    }

    public void onCreate() {
        super.onCreate();

        mLastRxBytes = TrafficStats.getTotalRxBytes();
        mLastTxBytes = TrafficStats.getTotalTxBytes();
        mLastTime = System.currentTimeMillis();

        mSpeed = new Speed(this);

        mKeyguardManager = (KeyguardManager)getSystemService(Context.KEYGUARD_SERVICE);

        mIndicatorNotification = new IndicatorNotification(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handleConfigChange(intent.getExtras());

        SharedPreferences sp_day = getSharedPreferences(TODAY_DATA, 0);
        if (!sp_day.contains("today_date")) {
            SharedPreferences.Editor editor_day = sp_day.edit();
            editor_day.putString("today_date", new SimpleDateFormat("MMM dd, yyyy").format(Calendar.getInstance().getTime()));
            editor_day.apply();
        }
        this.dataThread = new Thread(new MyThreadClass(startId));
        this.dataThread.setName("showNotification");
        this.dataThread.start();
        if (!StoredData.isSetData) {
            StoredData.setZero();
        }

        Log.v("parth","onStartCommand");

        createNotification();

        restartNotifying();

        return START_STICKY;
    }

    private void createNotification() {
        if (!mNotificationCreated) {
            mIndicatorNotification.start(this);
            mNotificationCreated = true;
        }
    }

    private void removeNotification() {
        if (mNotificationCreated) {
            mIndicatorNotification.stop(this);
            mNotificationCreated = false;
        }
    }

    private void pauseNotifying() {
        mHandler.removeCallbacks(mHandlerRunnable);
    }

    private void restartNotifying() {
        mHandler.removeCallbacks(mHandlerRunnable);
        mHandler.post(mHandlerRunnable);
    }

    private void handleConfigChange(Bundle config) {
        // Show/Hide on lock screen
        IntentFilter screenBroadcastIntentFilter = new IntentFilter();
        screenBroadcastIntentFilter.addAction(Intent.ACTION_SCREEN_ON);
        screenBroadcastIntentFilter.addAction(Intent.ACTION_SCREEN_OFF);

        mNotificationOnLockScreen = config.getBoolean(Settings.KEY_NOTIFICATION_ON_LOCK_SCREEN, false);

        if (!mNotificationOnLockScreen) {
            screenBroadcastIntentFilter.addAction(Intent.ACTION_USER_PRESENT);
            screenBroadcastIntentFilter.setPriority(999);
        }

        if (mNotificationCreated) {
            unregisterReceiver(mScreenBroadcastReceiver);
        }
        registerReceiver(mScreenBroadcastReceiver, screenBroadcastIntentFilter);

        // Speed unit, bps or Bps
        boolean isSpeedUnitBits = config.getString(Settings.KEY_INDICATOR_SPEED_UNIT, "Bps").equals("bps");
        mSpeed.setIsSpeedUnitBits(isSpeedUnitBits);

        // Pass it to notification
        mIndicatorNotification.handleConfigChange(config);
    }

    private final class MyThreadClass implements Runnable {
        int service_id;

        MyThreadClass(int service_id) {
            this.service_id = service_id;
        }

        public void run() {
            int i = 0;
            synchronized (this) {
                while (IndicatorService.this.dataThread.getName().equals("showNotification")) {
                    IndicatorService.this.getData();
                    try {
                        wait(1000);
                        i++;
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public void getData() {
        String network_status = NetworkUtil.getConnectivityStatusString(getApplicationContext());
        List<Long> allData = RetrieveData.findData();
        Long mDownload = allData.get(0);
        Long mUpload = allData.get(1);
        long receiveData = mDownload + mUpload;
        storedData(mDownload, mUpload);

        long wifiData = 0;
        long mobileData = 0;
        long totalData;
        if (network_status.equals("wifi_enabled")) {
            totalData = receiveData;
            wifiData = receiveData;
        } else if (network_status.equals("mobile_enabled")) {
            totalData = receiveData;
            mobileData = receiveData;
        }
        Calendar ca = Calendar.getInstance();
        String tDate = new SimpleDateFormat("MMM dd, yyyy").format(ca.getTime());
        SharedPreferences sp_day = getSharedPreferences(TODAY_DATA, 0);
        String saved_date = sp_day.getString("today_date", "empty");
        if (saved_date.equals(tDate)) {
            long saved_mobileData = sp_day.getLong("MOBILE_DATA", 0);
            long saved_wifiData = sp_day.getLong("WIFI_DATA", 0);
            SharedPreferences.Editor day_editor = sp_day.edit();
            day_editor.putLong("MOBILE_DATA", mobileData + saved_mobileData);
            day_editor.putLong("WIFI_DATA", wifiData + saved_wifiData);
            day_editor.apply();
            Log.v("parth","saved_date");
            return;
        }
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("WIFI_DATA", sp_day.getLong("WIFI_DATA", 0));
            jsonObject.put("MOBILE_DATA", sp_day.getLong("MOBILE_DATA", 0));
            SharedPreferences.Editor month_editor = getSharedPreferences(MONTH_DATA, 0).edit();
            month_editor.putString(saved_date, jsonObject.toString());
            month_editor.apply();
            SharedPreferences.Editor day_editor = getSharedPreferences(TODAY_DATA, 0).edit();
            day_editor.clear();
            day_editor.putString("today_date", tDate);
            day_editor.apply();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void storedData(Long mDownload, Long mUpload) {
        StoredData.downloadSpeed = mDownload;
        StoredData.uploadSpeed = mUpload;
        if (StoredData.isSetData) {
            StoredData.downloadList.remove(0);
            StoredData.uploadList.remove(0);
            StoredData.downloadList.add(mDownload);
            StoredData.uploadList.add(mUpload);
        }
        StringBuilder append = new StringBuilder().append("test ");
        Log.e("storeddata", append.append(String.valueOf(StoredData.downloadList.size())).toString());
    }
}
