package com.example.mdfsoc.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.example.mdfsoc.R;
import com.example.mdfsoc.models.Alert;
import com.example.mdfsoc.models.MitreInfo;
import com.example.mdfsoc.utils.Resource;
import com.example.mdfsoc.utils.SeverityUtils;
import com.example.mdfsoc.viewmodels.AlertDetailViewModel;
import com.google.android.material.card.MaterialCardView;

public class AlertDetailActivity extends AppCompatActivity {

    private AlertDetailViewModel viewModel;
    private long alertId;

    private MaterialCardView headerCard;
    private MaterialCardView detailsCard;
    private MaterialCardView mitreCard;
    private MaterialCardView descriptionCard;
    private View loadingView;
    private View errorView;
    private TextView errorMessage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alert_detail);

        setSupportActionBar(findViewById(R.id.detail_toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        alertId = getIntent().getLongExtra("alert_id", -1);

        headerCard = findViewById(R.id.header_card);
        detailsCard = findViewById(R.id.details_card);
        mitreCard = findViewById(R.id.mitre_card);
        descriptionCard = findViewById(R.id.description_card);
        loadingView = findViewById(R.id.loading_view);
        errorView = findViewById(R.id.error_view);
        errorMessage = findViewById(R.id.error_message);

        viewModel = new ViewModelProvider(this).get(AlertDetailViewModel.class);

        findViewById(R.id.btn_detail_retry).setOnClickListener(v -> viewModel.load(alertId));

        viewModel.getAlertDetail().observe(this, this::render);
        viewModel.load(alertId);
    }

    private void render(Resource<Alert> resource) {
        if (resource == null) {
            return;
        }
        loadingView.setVisibility(resource.isLoading() ? View.VISIBLE : View.GONE);

        if (resource.getStatus() == Resource.Status.ERROR) {
            errorMessage.setText(resource.getMessage());
            errorView.setVisibility(View.VISIBLE);
            hideCards();
            return;
        }

        Alert alert = resource.getData();
        if (alert == null) {
            errorMessage.setText(getString(R.string.empty_alert_list));
            errorView.setVisibility(View.VISIBLE);
            hideCards();
            return;
        }

        errorView.setVisibility(View.GONE);

        headerCard.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.detail_title)).setText(alert.getTitle());
        ((TextView) findViewById(R.id.detail_id)).setText(
                getString(R.string.alert_scene) + " #" + alert.getId());

        TextView severity = findViewById(R.id.detail_severity);
        severity.setText(SeverityUtils.labelFor(alert.getSeverity()).toUpperCase());
        severity.setBackgroundColor(SeverityUtils.colorFor(this, alert.getSeverity()));

        String investigationStatus = alert.getInvestigationStatus() != null
                ? alert.getInvestigationStatus()
                : (alert.getStatus() != null ? alert.getStatus() : "open");
        TextView status = findViewById(R.id.detail_status);
        status.setText(getString(R.string.alert_status) + ": " + investigationStatus);
        status.setTextColor(SeverityUtils.statusColor(this, investigationStatus));

        detailsCard.setVisibility(View.VISIBLE);
        ((TextView) findViewById(R.id.detail_source_ip)).setText(valueOr(alert.getSourceIp()));
        ((TextView) findViewById(R.id.detail_dest_ip)).setText(valueOr(alert.getDestIp()));
        ((TextView) findViewById(R.id.detail_timestamp)).setText(valueOr(alert.getTimestamp()));
        ((TextView) findViewById(R.id.detail_agent)).setText(valueOr(alert.getAgent()));
        ((TextView) findViewById(R.id.detail_rule)).setText(
                "Rule " + valueOr(alert.getRuleId()) + " (level " + alert.getRuleLevel() + ")");
        ((TextView) findViewById(R.id.detail_investigation_status)).setText(
                SeverityUtils.labelFor(investigationStatus));

        MitreInfo mitre = alert.getMitre();
        if (mitre != null && mitre.getTechniqueId() != null && !mitre.getTechniqueId().isEmpty()) {
            mitreCard.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.mitre_technique)).setText(
                    mitre.getTechniqueId() + " \u2014 " + valueOr(mitre.getTechniqueName()));
            ((TextView) findViewById(R.id.mitre_tactic)).setText(
                    getString(R.string.alert_mitre_technique) + ": " + valueOr(mitre.getTactic()));
            TextView mitreLink = findViewById(R.id.mitre_link);
            String link = mitre.getLink();
            if (link != null && !link.isEmpty()) {
                mitreLink.setText(link);
                mitreLink.setVisibility(View.VISIBLE);
            } else {
                mitreLink.setVisibility(View.GONE);
            }
        } else {
            mitreCard.setVisibility(View.GONE);
        }

        String description = alert.getDescription();
        if (description != null && !description.isEmpty()) {
            descriptionCard.setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.detail_description)).setText(description);
        } else {
            descriptionCard.setVisibility(View.GONE);
        }
    }

    private void hideCards() {
        headerCard.setVisibility(View.GONE);
        detailsCard.setVisibility(View.GONE);
        mitreCard.setVisibility(View.GONE);
        descriptionCard.setVisibility(View.GONE);
    }

    private String valueOr(String value) {
        return value != null && !value.isEmpty() ? value : getString(R.string.status_not_available);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}