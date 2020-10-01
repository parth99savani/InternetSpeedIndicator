package com.seventyseven.internetspeedindicator;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.seventyseven.internetspeedindicator.Adapter.DataUsageAdapter;
import com.seventyseven.internetspeedindicator.Adapter.HistoryAdapter;
import com.seventyseven.internetspeedindicator.Service.IndicatorService;
import com.seventyseven.internetspeedindicator.Utils.DataInfo;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class UsageHistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerViewHistory;
    private HistoryAdapter historyAdapter;
    private List<DataInfo> monthData;
    private double total_mobile;
    private double total_wifi;
    private final SimpleDateFormat SDF = new SimpleDateFormat("MMM dd, yyyy");
    private static final String GIGABYTE = " GB";
    private static final String MEGABYTE = " MB";
    private String today_date = null;
    private final DecimalFormat df = new DecimalFormat("#.##");
    private double today_mobile = 0.0d;
    private double today_wifi = 0.0d;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_usage_history);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);

        recyclerViewHistory.setHasFixedSize(true);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerViewHistory.setLayoutManager(llm);
        recyclerViewHistory.getItemAnimator().setChangeDuration(0);
        monthData = createList(30);
        historyAdapter = new HistoryAdapter(this, monthData);
        recyclerViewHistory.setAdapter(historyAdapter);

    }

    private List<DataInfo> createList(int size) {
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

    private DataInfo todayData() {
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

    private List<String> dataFormate(double wifi, double mobile, double total) {
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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}