package com.seventyseven.internetspeedindicator;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;

public class SplashActivity extends AppCompatActivity {

    private DecoView dynamicArcView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        dynamicArcView = findViewById(R.id.dynamicArcView);

        int mBackIndex;
        int mSeries1Index;
        SeriesItem seriesItem = new SeriesItem.Builder(Color.parseColor("#FFFFB248"))
                .setRange(0, 100, 0)
                .setInitialVisibility(true)
                .build();

        mBackIndex = dynamicArcView.addSeries(seriesItem);

        SeriesItem seriesItem2 = new SeriesItem.Builder(Color.parseColor("#FFFF247E"))
                .setRange(0, 100, 0)
                .setInitialVisibility(false)
                .build();

        mSeries1Index = dynamicArcView.addSeries(seriesItem2);

        dynamicArcView.executeReset();

        dynamicArcView.addEvent(new DecoEvent.Builder(100)
                .setIndex(mBackIndex)
                .setDuration(1000)
                .setDelay(100)
                .build());

        dynamicArcView.addEvent(new DecoEvent.Builder(30)
                .setIndex(mSeries1Index)
                .setDelay(1000)
                .build());

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                startActivity(new Intent(SplashActivity.this, MainActivity.class));
                finish();
            }
        }, 2100);
    }
}