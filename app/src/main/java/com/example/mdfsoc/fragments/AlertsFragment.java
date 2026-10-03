package com.example.mdfsoc.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.mdfsoc.R;
import com.example.mdfsoc.activities.AlertDetailActivity;
import com.example.mdfsoc.adapters.AlertsAdapter;
import com.example.mdfsoc.models.Alert;
import com.example.mdfsoc.models.AlertsPage;
import com.example.mdfsoc.utils.Resource;
import com.example.mdfsoc.viewmodels.AlertsViewModel;
import com.google.android.material.chip.ChipGroup;

public class AlertsFragment extends Fragment implements AlertsAdapter.OnAlertClickListener {

    private AlertsViewModel viewModel;
    private AlertsAdapter adapter;
    private ChipGroup chipGroup;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_alerts, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        adapter = new AlertsAdapter(this);
        RecyclerView alertList = view.findViewById(R.id.alerts_list);
        alertList.setLayoutManager(new LinearLayoutManager(requireContext()));
        alertList.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(AlertsViewModel.class);

        chipGroup = view.findViewById(R.id.severity_filter);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds == null || checkedIds.isEmpty()) {
                return;
            }
            viewModel.loadAlerts(severityForChip(checkedIds.get(0)));
        });

        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe =
                view.findViewById(R.id.swipe_refresh_alerts);
        swipe.setOnRefreshListener(() -> viewModel.loadAlerts(viewModel.getCurrentSeverity()));

        view.findViewById(R.id.btn_alerts_retry).setOnClickListener(v ->
                viewModel.loadAlerts(viewModel.getCurrentSeverity()));

        viewModel.getAlerts().observe(getViewLifecycleOwner(), this::render);
        viewModel.loadAlerts("all");
    }

    private String severityForChip(int chipId) {
        if (chipId == R.id.chip_critical) {
            return "critical";
        }
        if (chipId == R.id.chip_high) {
            return "high";
        }
        if (chipId == R.id.chip_medium) {
            return "medium";
        }
        if (chipId == R.id.chip_low) {
            return "low";
        }
        return "all";
    }

    private void render(Resource<AlertsPage> resource) {
        if (resource == null) {
            return;
        }
        View view = getView();
        if (view == null) {
            return;
        }

        if (resource.isLoading()) {
            view.findViewById(R.id.loading_view).setVisibility(View.VISIBLE);
            view.findViewById(R.id.error_view).setVisibility(View.GONE);
            view.findViewById(R.id.empty_view).setVisibility(View.GONE);
            return;
        }

        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe =
                view.findViewById(R.id.swipe_refresh_alerts);
        swipe.setRefreshing(false);
        view.findViewById(R.id.loading_view).setVisibility(View.GONE);

        if (resource.getStatus() == Resource.Status.ERROR) {
            view.findViewById(R.id.error_view).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.error_message)).setText(resource.getMessage());
            view.findViewById(R.id.empty_view).setVisibility(View.GONE);
            return;
        }

        view.findViewById(R.id.error_view).setVisibility(View.GONE);

        AlertsPage page = resource.getData();
        if (page == null || page.getAlerts() == null || page.getAlerts().isEmpty()) {
            TextView empty = view.findViewById(R.id.empty_message);
            empty.setText(viewModel.getCurrentSeverity().equals("all")
                    ? getString(R.string.empty_alert_list)
                    : getString(R.string.empty_severity_filter));
            view.findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
            adapter.submitList(null);
            return;
        }

        view.findViewById(R.id.empty_view).setVisibility(View.GONE);
        adapter.submitList(page.getAlerts());
    }

    @Override
    public void onAlertClick(Alert alert) {
        Intent intent = new Intent(requireContext(), AlertDetailActivity.class);
        intent.putExtra("alert_id", alert.getId());
        startActivity(intent);
    }
}