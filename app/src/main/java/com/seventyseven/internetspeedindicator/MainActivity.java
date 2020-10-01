package com.seventyseven.internetspeedindicator;

import android.content.Intent;
import android.content.IntentSender;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.snackbar.Snackbar;
import com.google.android.play.core.appupdate.AppUpdateInfo;
import com.google.android.play.core.appupdate.AppUpdateManager;
import com.google.android.play.core.appupdate.AppUpdateManagerFactory;
import com.google.android.play.core.install.InstallState;
import com.google.android.play.core.install.InstallStateUpdatedListener;
import com.google.android.play.core.install.model.AppUpdateType;
import com.google.android.play.core.install.model.InstallStatus;
import com.google.android.play.core.install.model.UpdateAvailability;
import com.google.android.play.core.review.ReviewInfo;
import com.google.android.play.core.review.ReviewManager;
import com.google.android.play.core.review.ReviewManagerFactory;
import com.google.android.play.core.tasks.OnCompleteListener;
import com.google.android.play.core.tasks.OnSuccessListener;
import com.google.android.play.core.tasks.Task;
import com.seventyseven.internetspeedindicator.Adapter.DataUsageAdapter;
import com.seventyseven.internetspeedindicator.Dialog.PermissionDialog;
import com.seventyseven.internetspeedindicator.Service.IndicatorService;
import com.seventyseven.internetspeedindicator.Utils.DataInfo;
import com.seventyseven.internetspeedindicator.Utils.Settings;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String GIGABYTE = " GB";
    private static final String MEGABYTE = " MB";
    private final SimpleDateFormat SDF = new SimpleDateFormat("MMM dd, yyyy");
    private DataUsageAdapter dataUsageAdapter;
    private Thread dataUpdate;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private double today_mobile = 0.0d;
    private double today_wifi = 0.0d;
    private double total_mobile;
    private double total_wifi;
    private Handler vHandler = new Handler();
    private String today_date = null;
    private TextView txtTodayTotal;
    private TextView txtUnitTodayTotal;
    private TextView txtMobileToday;
    private TextView txtWifiToday;
    private RecyclerView recyclerViewHome;
    private TextView txtMobile30Day;
    private TextView txtWifi30Day;
    private TextView txtTotal30Day;
    List<DataInfo> monthData;
    private SharedPreferences mSharedPref;
    private ReviewManager manager;
    private ReviewInfo reviewInfo;
    private static final int REQ_CODE_VERSION_UPDATE = 530;
    private AppUpdateManager appUpdateManager;
    private InstallStateUpdatedListener installStateUpdatedListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getSupportActionBar().setElevation(0f);

        checkForAppUpdate();

        //Review
        manager = ReviewManagerFactory.create(this);
        Task<ReviewInfo> request = manager.requestReviewFlow();
        request.addOnCompleteListener(new OnCompleteListener<ReviewInfo>() {
            @Override
            public void onComplete(@NonNull Task<ReviewInfo> task) {
                if (task.isSuccessful()) {
                    // We can get the ReviewInfo object
                    reviewInfo = task.getResult();
                } else {
                    // There was some problem, continue regardless of the result.
                }
            }
        });

        txtTodayTotal = findViewById(R.id.txtTodayTotal);
        txtUnitTodayTotal = findViewById(R.id.txtUnitTodayTotal);
        txtMobileToday = findViewById(R.id.txtMobileToday);
        txtWifiToday = findViewById(R.id.txtWifiToday);
        recyclerViewHome = findViewById(R.id.recyclerViewHome);
        txtMobile30Day = findViewById(R.id.txtMobile30Day);
        txtWifi30Day = findViewById(R.id.txtWifi30Day);
        txtTotal30Day = findViewById(R.id.txtTotal30Day);

        recyclerViewHome.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.HORIZONTAL);
        recyclerViewHome.setLayoutManager(llm);
        recyclerViewHome.getItemAnimator().setChangeDuration(0);
        monthData = createList(30);
        dataUsageAdapter = new DataUsageAdapter(this, monthData);
        recyclerViewHome.setAdapter(dataUsageAdapter);
        totalData();
        clearExtraData();
        liveData();

        mSharedPref = PreferenceManager.getDefaultSharedPreferences(this);

        if (!mSharedPref.getBoolean(Settings.PERMISSION_ALLOWED, false)) {
            PermissionDialog permissionDialog = PermissionDialog.newInstance();
            permissionDialog.show(getSupportFragmentManager(), "Permission Bottom Sheet Dialog Fragment");
            permissionDialog.setCancelable(false);
        }

    }

    public void liveData() {
        dataUpdate = new Thread(new Runnable() {
            public void run() {
                while (!dataUpdate.getName().equals("stopped")) {
                    final String temp_today = SDF.format(Calendar.getInstance().getTime());
                    vHandler.post(new Runnable() {
                        public void run() {
                            if (temp_today.equals(today_date)) {
                                monthData.set(0, todayData());
                                setTodayData();
                                //dataUsageAdapter.notifyItemChanged(0);
                                Log.e("datechange", temp_today);
                            } else {
                                today_wifi = 0.0d;
                                today_mobile = 0.0d;
                                monthData = createList(30);
                                dataUsageAdapter.dataList = monthData;
                                //dataUsageAdapter.notifyDataSetChanged();
                                monthData.set(0, todayData());
                                setTodayData();
                                //dataUsageAdapter.notifyItemChanged(0);
                            }
                            totalData();
                        }
                    });
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        });
        dataUpdate.setName("started");
        dataUpdate.start();
    }

    private void setTodayData() {
        if ((monthData.get(0).wifiData + monthData.get(0).mobileData) < 1024.0d) {
            txtTodayTotal.setText(df.format((monthData.get(0).wifiData + monthData.get(0).mobileData))+"");
            txtUnitTodayTotal.setText(MEGABYTE);
        } else {
            txtTodayTotal.setText(df.format((monthData.get(0).wifiData + monthData.get(0).mobileData) / 1024.0d)+"");
            txtUnitTodayTotal.setText(GIGABYTE);
        }
        txtMobileToday.setText(monthData.get(0).mobile);
        txtWifiToday.setText(monthData.get(0).wifi);
    }

    public List<DataInfo> createList(int size) {
        List<DataInfo> result = new ArrayList();
        total_wifi = 0.0d;
        total_mobile = 0.0d;
        String wifi = "0";
        String mobile = "0";
        String total = "0";
        SharedPreferences sp_month = this.getSharedPreferences(IndicatorService.MONTH_DATA, 0);
        for (int i = 1; i <= size; i++) {
            if (i == 1) {
                result.add(todayData());
            } else {
                Calendar calendar = Calendar.getInstance();
                calendar.add(5, 1 - i);
                String mDate = SDF.format(calendar.getTime());
                List<String> allData = new ArrayList();
                double wTemp = 0.0d;
                double mTemp = 0.0d;
                if (sp_month.contains(mDate)) {
                    try {
                        JSONObject jSONObject = new JSONObject(sp_month.getString(mDate, null));
                        wifi = jSONObject.getString("WIFI_DATA");
                        wTemp = ((double) Long.parseLong(wifi)) / 1048576.0d;
                        mTemp = ((double) Long.parseLong(jSONObject.getString("MOBILE_DATA"))) / 1048576.0d;
                        allData = dataFormate(wTemp, mTemp, wTemp + mTemp);
                        total_wifi += wTemp;
                        total_mobile += mTemp;
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    allData = dataFormate(0.0d, 0.0d, 0.0d);
                }
                DataInfo dataInfo = new DataInfo();
                dataInfo.date = mDate;
                dataInfo.wifi = allData.get(0);
                dataInfo.mobile = allData.get(1);
                dataInfo.total = allData.get(2);
                dataInfo.mobileData =  mTemp;
                dataInfo.wifiData =  wTemp;
                result.add(dataInfo);
            }
        }
        return result;
    }

    public DataInfo todayData() {
        List<DataInfo> listToday = new ArrayList();
        today_date = SDF.format(Calendar.getInstance().getTime());
        double wTemp = 0.0d;
        double mTemp = 0.0d;
        try {
            SharedPreferences sp = this.getSharedPreferences(IndicatorService.TODAY_DATA, 0);
            wTemp = ((double) sp.getLong("WIFI_DATA", 0)) / 1048576.0d;
            mTemp = ((double) sp.getLong("MOBILE_DATA", 0)) / 1048576.0d;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("crashed", "hello");
        }
        List<String> allData = dataFormate(wTemp, mTemp, wTemp + mTemp);

        total_wifi += wTemp - today_wifi;
        total_mobile += mTemp - today_mobile;
        today_wifi = wTemp;
        today_mobile = mTemp;
        DataInfo dataInfo = new DataInfo();
        dataInfo.date = "Today";
        dataInfo.wifi = allData.get(0);
        dataInfo.mobile = allData.get(1);
        dataInfo.total = allData.get(2);
        dataInfo.mobileData = mTemp;
        dataInfo.wifiData = wTemp;
        listToday.add(dataInfo);
        return dataInfo;
    }

    public void totalData() {
        List<String> total = dataFormate(total_wifi, total_mobile, total_wifi + total_mobile);
        txtWifi30Day.setText(total.get(0));
        txtMobile30Day.setText(total.get(1));
        txtTotal30Day.setText(total.get(2));
    }

    public List<String> dataFormate(double wifi, double mobile, double total) {
        List<String> allData = new ArrayList();
        if (wifi < 1024.0d) {
            allData.add(df.format(wifi) + MEGABYTE);
        } else {
            allData.add(df.format(wifi / 1024.0d) + GIGABYTE);
        }
        if (mobile < 1024.0d) {
            allData.add(df.format(mobile) + MEGABYTE);
        } else {
            allData.add(df.format(mobile / 1024.0d) + GIGABYTE);
        }
        if (total < 1024.0d) {
            allData.add(df.format(total) + MEGABYTE);
        } else {
            allData.add(df.format(total / 1024.0d) + GIGABYTE);
        }
        return allData;
    }

    void clearExtraData() {
        SharedPreferences sp_month = this.getSharedPreferences(IndicatorService.MONTH_DATA, 0);
        SharedPreferences.Editor editor = sp_month.edit();
        for (int i = 40; i <= 1000; i++) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(5, 1 - i);
            String mDate = SDF.format(calendar.getTime());
            if (sp_month.contains(mDate)) {
                editor.remove(mDate);
            }
        }
        editor.apply();
    }

    public void onPause() {
        super.onPause();
        dataUpdate.setName("stopped");
        Log.e("astatus", "onPause");
    }

    public void onResume() {
        super.onResume();
        dataUpdate.setName("started");
        Log.e("astatus", "onResume");
        Log.e("astatus", dataUpdate.getState().toString());
        if (!dataUpdate.isAlive()) {
            liveData();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main_option_menu, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.settings:
                //settings
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void checkForAppUpdate() {
        // Creates instance of the manager.
        appUpdateManager = AppUpdateManagerFactory.create(this);

        // Returns an intent object that you use to check for an update.
        Task<AppUpdateInfo> appUpdateInfoTask = appUpdateManager.getAppUpdateInfo();

        // Create a listener to track request state updates.
        installStateUpdatedListener = new InstallStateUpdatedListener() {
            @Override
            public void onStateUpdate(InstallState installState) {
                // Show module progress, log state, or install the update.
                if (installState.installStatus() == InstallStatus.DOWNLOADED)
                    // After the update is downloaded, show a notification
                    // and request user confirmation to restart the app.
                    popupSnackbarForCompleteUpdateAndUnregister();
            }
        };

        // Checks that the platform will allow the specified type of update.
        appUpdateInfoTask.addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
            @Override
            public void onSuccess(AppUpdateInfo appUpdateInfo) {
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE) {
                    // Request the update.
                    if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)) {

                        // Before starting an update, register a listener for updates.
                        appUpdateManager.registerListener(installStateUpdatedListener);
                        // Start an update.
                        startAppUpdateFlexible(appUpdateInfo);
                    } else if (appUpdateInfo.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) ) {
                        // Start an update.
                        startAppUpdateImmediate(appUpdateInfo);
                    }
                }
            }
        });

    }

    private void startAppUpdateImmediate(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.IMMEDIATE,
                    // The current activity making the update request.
                    this,
                    // Include a request code to later monitor this update request.
                    MainActivity.REQ_CODE_VERSION_UPDATE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
        }
    }

    private void startAppUpdateFlexible(AppUpdateInfo appUpdateInfo) {
        try {
            appUpdateManager.startUpdateFlowForResult(
                    appUpdateInfo,
                    AppUpdateType.FLEXIBLE,
                    // The current activity making the update request.
                    this,
                    // Include a request code to later monitor this update request.
                    MainActivity.REQ_CODE_VERSION_UPDATE);
        } catch (IntentSender.SendIntentException e) {
            e.printStackTrace();
            unregisterInstallStateUpdListener();
        }
    }

    private void popupSnackbarForCompleteUpdateAndUnregister() {
        View view = findViewById(android.R.id.content);
        Snackbar snackbar =
                Snackbar.make(view, "Download an Update", Snackbar.LENGTH_INDEFINITE);
        snackbar.setAction("Restart", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                appUpdateManager.completeUpdate();
            }
        });
        snackbar.setActionTextColor(getResources().getColor(R.color.colorPrimary));
        snackbar.show();

        unregisterInstallStateUpdListener();
    }

    private void unregisterInstallStateUpdListener() {
        if (appUpdateManager != null && installStateUpdatedListener != null)
            appUpdateManager.unregisterListener(installStateUpdatedListener);
    }

    private void checkNewAppVersionState() {
        appUpdateManager
                .getAppUpdateInfo()
                .addOnSuccessListener(new OnSuccessListener<AppUpdateInfo>() {
                    @Override
                    public void onSuccess(AppUpdateInfo appUpdateInfo) {
                        //FLEXIBLE:
                        // If the update is downloaded but not installed,
                        // notify the user to complete the update.
                        if (appUpdateInfo.installStatus() == InstallStatus.DOWNLOADED) {
                            popupSnackbarForCompleteUpdateAndUnregister();
                        }

                        //IMMEDIATE:
                        if (appUpdateInfo.updateAvailability()
                                == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS) {
                            // If an in-app update is already running, resume the update.
                            startAppUpdateImmediate(appUpdateInfo);
                        }
                    }
                });

    }

    @Override
    protected void onDestroy() {
        unregisterInstallStateUpdListener();
        super.onDestroy();
    }

    @Override
    public void onActivityResult(int requestCode, final int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);

        switch (requestCode) {

            case REQ_CODE_VERSION_UPDATE:
                if (resultCode != RESULT_OK) { //RESULT_OK / RESULT_CANCELED / RESULT_IN_APP_UPDATE_FAILED
                    Log.d("MainActivity","Update flow failed! Result code: " + resultCode);
                    // If the update is cancelled or fails,
                    // you can request to start the update again.
                    unregisterInstallStateUpdListener();
                }

                break;
        }
    }

    @Override
    public void onBackPressed() {
        Task<Void> flow = manager.launchReviewFlow(this, reviewInfo);
        flow.addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                MainActivity.super.onBackPressed();
            }
        });
    }
}