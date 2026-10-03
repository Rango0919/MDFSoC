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
import com.example.mdfsoc.adapters.RecentAlertsAdapter;
import com.example.mdfsoc.models.DashboardData;
import com.example.mdfsoc.models.RecentAlert;
import com.example.mdfsoc.utils.Resource;
import com.example.mdfsoc.viewmodels.DashboardViewModel;
import com.google.android.material.snackbar.Snackbar;

public class DashboardFragment extends Fragment implements RecentAlertsAdapter.OnAlertClickListener {

    public interface OnNavigateListener {
        void onOpenAlertsRequested();
    }

    private DashboardViewModel viewModel;
    private OnNavigateListener navigateListener;
    private RecentAlertsAdapter recentAdapter;

    @Override
    public void onAttach(@NonNull android.content.Context context) {
        super.onAttach(context);
        if (context instanceof OnNavigateListener) {
            navigateListener = (OnNavigateListener) context;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dashboard, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        recentAdapter = new RecentAlertsAdapter(this);
        RecyclerView recentList = view.findViewById(R.id.recent_alerts_list);
        recentList.setLayoutManager(new LinearLayoutManager(requireContext()));
        recentList.setNestedScrollingEnabled(false);
        recentList.setAdapter(recentAdapter);

        viewModel = new ViewModelProvider(this).get(DashboardViewModel.class);

        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe =
                view.findViewById(R.id.swipe_refresh_dashboard);
        swipe.setOnRefreshListener(() -> {
            viewModel.refresh();
        });

        view.findViewById(R.id.btn_view_all).setOnClickListener(v -> {
            if (navigateListener != null) {
                navigateListener.onOpenAlertsRequested();
            } else {
                showMessage(getString(R.string.view_all_alerts), false);
            }
        });

        view.findViewById(R.id.btn_dashboard_retry).setOnClickListener(v -> viewModel.refresh());

        viewModel.getDashboardData().observe(getViewLifecycleOwner(), this::render);
        viewModel.refresh();
    }

    private void render(Resource<DashboardData> resource) {
        if (resource == null) {
            return;
        }
        View view = getView();
        if (view == null) {
            return;
        }

        androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipe =
                view.findViewById(R.id.swipe_refresh_dashboard);
        if (resource.isLoading()) {
            view.findViewById(R.id.loading_view).setVisibility(View.VISIBLE);
            view.findViewById(R.id.error_view).setVisibility(View.GONE);
            return;
        }
        swipe.setRefreshing(false);
        view.findViewById(R.id.loading_view).setVisibility(View.GONE);

        if (resource.getStatus() == Resource.Status.ERROR) {
            view.findViewById(R.id.error_view).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.error_message)).setText(resource.getMessage());
            return;
        }

        DashboardData data = resource.getData();
        view.findViewById(R.id.error_view).setVisibility(View.GONE);
        if (data == null) {
            showEmpty(view);
            return;
        }

        ((TextView) view.findViewById(R.id.stat_total)).setText(String.valueOf(data.getTotalAlerts()));
        ((TextView) view.findViewById(R.id.stat_critical)).setText(String.valueOf(data.getCriticalAlerts()));
        ((TextView) view.findViewById(R.id.stat_high)).setText(String.valueOf(data.getHighAlerts()));
        ((TextView) view.findViewById(R.id.stat_medium)).setText(String.valueOf(data.getMediumAlerts()));
        ((TextView) view.findViewById(R.id.stat_low)).setText(String.valueOf(data.getLowAlerts()));

        if (data.getRecentAlerts() == null || data.getRecentAlerts().isEmpty()) {
            showEmpty(view);
        } else {
            view.findViewById(R.id.empty_view).setVisibility(View.GONE);
        }
        recentAdapter.submitList(data.getRecentAlerts());
    }

    private void showEmpty(View view) {
        view.findViewById(R.id.empty_view).setVisibility(View.VISIBLE);
    }

    private void showMessage(String message, boolean dismissable) {
        View view = getView();
        if (view != null && dismissable) {
            Snackbar.make(view, message, Snackbar.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRecentAlertClick(RecentAlert alert) {
        Intent intent = new Intent(requireContext(), AlertDetailActivity.class);
        intent.putExtra("alert_id", alert.getId());
        startActivity(intent);
    }
}