package com.seventyseven.internetspeedindicator.Adapter;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.recyclerview.widget.RecyclerView;

import com.hookedonplay.decoviewlib.DecoView;
import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;
import com.seventyseven.internetspeedindicator.R;
import com.seventyseven.internetspeedindicator.UsageHistoryActivity;
import com.seventyseven.internetspeedindicator.Utils.DataInfo;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DataUsageAdapter extends RecyclerView.Adapter<DataUsageAdapter.DataUsageViewHolder> {
    private Context activity;
    public List<DataInfo> dataList;
    private static final String GIGABYTE = " GB";
    private static final String MEGABYTE = " MB";
    private final DecimalFormat df = new DecimalFormat("#.##");

    class DataUsageViewHolder extends RecyclerView.ViewHolder {

        private TextView txtDate;
        private TextView textTotal;
        private TextView textGB;
        private DecoView dynamicArcView;
        private TextView txtMobile;
        private TextView txtWifi;
        private ImageButton btnLoadMore;

        DataUsageViewHolder(View itemView) {
            super(itemView);

            txtDate = itemView.findViewById(R.id.txtDate);
            textTotal = itemView.findViewById(R.id.textTotal);
            textGB = itemView.findViewById(R.id.textGB);
            dynamicArcView = itemView.findViewById(R.id.dynamicArcView);
            txtMobile = itemView.findViewById(R.id.txtMobile);
            txtWifi = itemView.findViewById(R.id.txtWifi);
            btnLoadMore = itemView.findViewById(R.id.btnLoadMore);
        }
    }

    public DataUsageAdapter(Context activity, List<DataInfo> dataList) {
        this.activity = activity;
        List<DataInfo> newList = new ArrayList<>();
        for (int i=0; i<dataList.size(); i++){
            if (i>=1 && i<=4){
                newList.add(dataList.get(i));
            }
        }
        this.dataList = newList;
    }

//    public DataUsageAdapter.DataUsageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
//        return new DataUsageAdapter.DataUsageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_home, parent, false));
//    }

    @Override
    public DataUsageAdapter.DataUsageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View itemView;

        if (viewType == R.layout.item_history_home) {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_history_home, parent, false);
        } else {
            itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_load_more, parent, false);
        }

        return new DataUsageAdapter.DataUsageViewHolder(itemView);
    }

    public void onBindViewHolder(DataUsageAdapter.DataUsageViewHolder holder, int position) {

        if (position == dataList.size()) {
            holder.btnLoadMore.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    activity.startActivity(new Intent(activity, UsageHistoryActivity.class));
                }
            });
        } else {

            DataInfo di = dataList.get(position);
            holder.txtDate.setText(di.date);
            holder.txtMobile.setText(di.mobile);
            holder.txtWifi.setText(di.wifi);
            if ((di.wifiData + di.mobileData) < 1024.0d) {
                holder.textTotal.setText(df.format((di.wifiData + di.mobileData)) + "");
                holder.textGB.setText(MEGABYTE);
            } else {
                holder.textTotal.setText(df.format((di.wifiData + di.mobileData) / 1024.0d) + "");
                holder.textGB.setText(GIGABYTE);
            }

            int mBackIndex;
            int mSeries1Index;
            SeriesItem seriesItem = new SeriesItem.Builder(Color.parseColor("#FFFFB248"))
                    .setRange(0, (float) (di.wifiData + di.mobileData) + 1, 0)
                    .setInitialVisibility(true)
                    .build();

            mBackIndex = holder.dynamicArcView.addSeries(seriesItem);

            SeriesItem seriesItem2 = new SeriesItem.Builder(Color.parseColor("#FFFF247E"))
                    .setRange(0, (float) (di.wifiData + di.mobileData) + 1, 0)
                    .setInitialVisibility(false)
                    .build();

            mSeries1Index = holder.dynamicArcView.addSeries(seriesItem2);

            holder.dynamicArcView.executeReset();

            holder.dynamicArcView.addEvent(new DecoEvent.Builder((float) (di.wifiData + di.mobileData))
                    .setIndex(mBackIndex)
                    .setDuration(3000)
                    .setDelay(100)
                    .build());

            holder.dynamicArcView.addEvent(new DecoEvent.Builder((float) di.mobileData)
                    .setIndex(mSeries1Index)
                    .setDelay(2000)
                    .build());
        }


    }

    public void updateData(List<DataInfo> temp) {
        this.dataList = temp;
        notifyDataSetChanged();
    }

//    public int getItemCount() {
//        return this.dataList.size();
//    }

    @Override
    public int getItemViewType(int position) {
        return (position == dataList.size()) ? R.layout.item_load_more : R.layout.item_history_home;
    }

    @Override
    public int getItemCount() {
        return dataList.size() + 1;
    }
}
