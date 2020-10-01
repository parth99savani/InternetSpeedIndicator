package com.seventyseven.internetspeedindicator.Adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.hookedonplay.decoviewlib.charts.SeriesItem;
import com.hookedonplay.decoviewlib.events.DecoEvent;
import com.seventyseven.internetspeedindicator.R;
import com.seventyseven.internetspeedindicator.Utils.DataInfo;

import java.text.DecimalFormat;
import java.util.List;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder> {

    private Context activity;
    private List<DataInfo> dataList;

    class HistoryViewHolder extends RecyclerView.ViewHolder {

        private TextView txtDate;
        private SeekBar seekBarUsage;
        private TextView txtMobile;
        private TextView txtWifi;
        private TextView txtTotal;

        HistoryViewHolder(View itemView) {
            super(itemView);

            txtDate = itemView.findViewById(R.id.txtDate);
            seekBarUsage = itemView.findViewById(R.id.seekBarUsage);
            txtMobile = itemView.findViewById(R.id.txtMobile);
            txtWifi = itemView.findViewById(R.id.txtWifi);
            txtTotal = itemView.findViewById(R.id.txtTotal);
        }
    }

    public HistoryAdapter(Context activity, List<DataInfo> dataList) {
        this.activity = activity;
        this.dataList = dataList;
    }

    public HistoryAdapter.HistoryViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new HistoryAdapter.HistoryViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_usage_history, parent, false));
    }

    public void onBindViewHolder(HistoryAdapter.HistoryViewHolder holder, int position) {
        DataInfo di = dataList.get(position);
        holder.txtDate.setText(di.date);
        holder.txtMobile.setText(di.mobile);
        holder.txtWifi.setText(di.wifi);
        holder.txtTotal.setText(di.total);

        holder.seekBarUsage.setEnabled(false);

        if (di.mobileData + di.wifiData > 0){
            holder.seekBarUsage.setMax((int) (di.mobileData + di.wifiData));
            holder.seekBarUsage.setProgress((int) di.mobileData);
            holder.seekBarUsage.setVisibility(View.VISIBLE);
        } else {
            holder.seekBarUsage.setVisibility(View.INVISIBLE);
        }
    }

    public int getItemCount() {
        return this.dataList.size();
    }
}

