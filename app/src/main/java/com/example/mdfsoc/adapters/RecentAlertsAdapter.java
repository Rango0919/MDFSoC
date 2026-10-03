package com.example.mdfsoc.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mdfsoc.R;
import com.example.mdfsoc.models.RecentAlert;
import com.example.mdfsoc.utils.SeverityUtils;

import java.util.ArrayList;
import java.util.List;

public class RecentAlertsAdapter extends RecyclerView.Adapter<RecentAlertsAdapter.ViewHolder> {

    public interface OnAlertClickListener {
        void onRecentAlertClick(RecentAlert alert);
    }

    private final List<RecentAlert> alerts = new ArrayList<>();
    private final OnAlertClickListener listener;

    public RecentAlertsAdapter(OnAlertClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alert, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.bind(alerts.get(position));
    }

    @Override
    public int getItemCount() {
        return alerts.size();
    }

    public void submitList(List<RecentAlert> items) {
        alerts.clear();
        if (items != null) {
            alerts.addAll(items);
        }
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        private final View severityBar;
        private final TextView severityBadge;
        private final TextView title;
        private final TextView sourceIp;
        private final TextView timestamp;
        private final TextView status;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            severityBar = itemView.findViewById(R.id.severity_bar);
            severityBadge = itemView.findViewById(R.id.severity_badge);
            title = itemView.findViewById(R.id.alert_title);
            sourceIp = itemView.findViewById(R.id.alert_source_ip);
            timestamp = itemView.findViewById(R.id.alert_timestamp);
            status = itemView.findViewById(R.id.alert_status);
        }

        void bind(RecentAlert alert) {
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onRecentAlertClick(alert);
                }
            });

            String severity = SeverityUtils.labelFor(alert.getSeverity());
            severityBadge.setText(severity);
            severityBadge.setTextColor(ContextCompat.getColor(
                    itemView.getContext(), R.color.soc_background));
            severityBadge.setBackgroundColor(
                    SeverityUtils.colorFor(itemView.getContext(), alert.getSeverity()));
            severityBar.setBackgroundColor(
                    SeverityUtils.colorFor(itemView.getContext(), alert.getSeverity()));

            title.setText(alert.getTitle());
            sourceIp.setText(alert.getSourceIp() != null ? alert.getSourceIp() : "-");
            timestamp.setText(alert.getTimestamp());
            status.setText(alert.getStatus() != null ? alert.getStatus() : "Active");
            status.setTextColor(
                    SeverityUtils.statusColor(itemView.getContext(), alert.getStatus()));
        }
    }
}