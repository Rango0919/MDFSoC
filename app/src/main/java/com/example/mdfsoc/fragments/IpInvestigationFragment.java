package com.example.mdfsoc.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mdfsoc.R;
import com.example.mdfsoc.models.IpReputation;
import com.example.mdfsoc.models.MitreInfo;
import com.example.mdfsoc.models.ThreatIntelligence;
import com.example.mdfsoc.models.VirusTotalInfo;
import com.example.mdfsoc.utils.Resource;
import com.example.mdfsoc.viewmodels.IpInvestigationViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;

public class IpInvestigationFragment extends Fragment {

    private IpInvestigationViewModel viewModel;
    private TextInputEditText ipInput;
    private String lastIp;
    private String pendingIp;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ip_investigation, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        ipInput = view.findViewById(R.id.input_ip);
        viewModel = new ViewModelProvider(this).get(IpInvestigationViewModel.class);

        view.findViewById(R.id.btn_investigate).setOnClickListener(v ->
                runInvestigation(ipInput.getText() == null ? "" : ipInput.getText().toString().trim()));

        view.findViewById(R.id.btn_ip_retry).setOnClickListener(v ->
                viewModel.investigate(lastIp));

        view.findViewById(R.id.value_vt_link).setOnClickListener(v -> {
            TextView vtLink = view.findViewById(R.id.value_vt_link);
            Object tag = vtLink.getTag();
            if (tag instanceof String && !((String) tag).isEmpty()) {
                openLink((String) tag);
            }
        });
        view.findViewById(R.id.value_mitre_link).setOnClickListener(v -> {
            TextView mitreLink = view.findViewById(R.id.value_mitre_link);
            Object tag = mitreLink.getTag();
            if (tag instanceof String && !((String) tag).isEmpty()) {
                openLink((String) tag);
            }
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), this::render);

        if (pendingIp != null && !pendingIp.isEmpty()) {
            String ip = pendingIp;
            pendingIp = null;
            runInvestigation(ip);
        }
    }

    public void setPendingIp(String ip) {
        pendingIp = ip;
        if (ipInput != null && !ip.isEmpty()) {
            runInvestigation(ip);
        }
    }

    private void runInvestigation(String ip) {
        if (ipInput == null || viewModel == null) {
            pendingIp = ip;
            return;
        }
        lastIp = ip;
        ipInput.setText(ip);
        viewModel.investigate(ip);
    }

    private void render(Resource<ThreatIntelligence> resource) {
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
            return;
        }
        view.findViewById(R.id.loading_view).setVisibility(View.GONE);

        if (resource.getStatus() == Resource.Status.ERROR) {
            view.findViewById(R.id.error_view).setVisibility(View.VISIBLE);
            ((TextView) view.findViewById(R.id.error_message)).setText(resource.getMessage());
            return;
        }
        view.findViewById(R.id.error_view).setVisibility(View.GONE);

        ThreatIntelligence ti = resource.getData();
        if (ti == null) {
            renderEmpty(view);
            return;
        }

        List<String> warnings = viewModel.getWarnings().getValue();
        TextView warningView = view.findViewById(R.id.warning_text);
        if (warnings == null || warnings.isEmpty()) {
            warningView.setVisibility(View.GONE);
        } else {
            warningView.setText(TextUtils.join("\n", warnings));
            warningView.setVisibility(View.VISIBLE);
        }

        if (ti.getSources() == null || ti.getSources().isEmpty()) {
            renderEmpty(view);
            return;
        }

        view.findViewById(R.id.result_card).setVisibility(View.VISIBLE);

        ((TextView) view.findViewById(R.id.result_ip)).setText(ti.getIp());

        TextView statusChip = view.findViewById(R.id.threat_status_chip);
        String status = ti.getThreatStatus() != null ? ti.getThreatStatus() : "unknown";
        statusChip.setText(statusLabel(status));
        statusChip.setBackgroundColor(ContextCompat.getColor(
                requireContext(), statusColor(status)));

        IpReputation abuse = ti.getAbuseIpdb();
        if (abuse != null) {
            ((TextView) view.findViewById(R.id.value_abuse_confidence)).setText(
                    abuse.getAbuseConfidenceScore() + "%");
            ((TextView) view.findViewById(R.id.value_reports)).setText(
                    String.valueOf(abuse.getTotalReports()));
            ((TextView) view.findViewById(R.id.value_country)).setText(notAvailable(abuse.getCountryLabel()));
            ((TextView) view.findViewById(R.id.value_isp)).setText(notAvailable(abuse.getIspLabel()));
        } else {
            setNotAvailable(view, R.id.value_abuse_confidence);
            setNotAvailable(view, R.id.value_reports);
            setNotAvailable(view, R.id.value_country);
            setNotAvailable(view, R.id.value_isp);
        }

        VirusTotalInfo vt = ti.getVirusTotal();
        TextView vtLink = view.findViewById(R.id.value_vt_link);
        if (vt != null) {
            int total = vt.getTotalEngineVerdicts() > 0 ? vt.getTotalEngineVerdicts()
                    : (vt.getMalicious() + vt.getSuspicious() + vt.getUndetected() + vt.getHarmless());
            ((TextView) view.findViewById(R.id.value_vt_detections)).setText(
                    vt.getMalicious() + " / " + total);
            if (vt.getVtLink() != null && !vt.getVtLink().isEmpty()) {
                vtLink.setTag(vt.getVtLink());
                vtLink.setVisibility(View.VISIBLE);
            } else {
                vtLink.setVisibility(View.GONE);
            }
        } else {
            setNotAvailable(view, R.id.value_vt_detections);
            vtLink.setVisibility(View.GONE);
        }

        MitreInfo mitre = ti.getMitre();
        TextView mitreLink = view.findViewById(R.id.value_mitre_link);
        if (mitre != null && mitre.getTechniqueId() != null) {
            String tactic = mitre.getTactic() != null
                    ? " (" + mitre.getTactic() + ")"
                    : "";
            ((TextView) view.findViewById(R.id.value_mitre)).setText(
                    mitre.getTechniqueId() + " \u2014 " + notAvailable(mitre.getTechniqueName()) + tactic);
            if (mitre.getLink() != null && !mitre.getLink().isEmpty()) {
                mitreLink.setTag(mitre.getLink());
                mitreLink.setVisibility(View.VISIBLE);
            } else {
                mitreLink.setVisibility(View.GONE);
            }
        } else {
            setNotAvailable(view, R.id.value_mitre);
            mitreLink.setVisibility(View.GONE);
        }

        ((TextView) view.findViewById(R.id.value_sources)).setText(
                "Sources: " + TextUtils.join(", ", ti.getSources()));
    }

    private void renderEmpty(View view) {
        view.findViewById(R.id.result_card).setVisibility(View.GONE);
        TextView warning = view.findViewById(R.id.warning_text);
        warning.setText(getString(R.string.empty_ip_result));
        warning.setVisibility(View.VISIBLE);
    }

    private String statusLabel(String status) {
        if ("malicious".equalsIgnoreCase(status)) {
            return getString(R.string.threat_malicious).toUpperCase();
        }
        if ("suspicious".equalsIgnoreCase(status)) {
            return getString(R.string.threat_suspicious).toUpperCase();
        }
        if ("clean".equalsIgnoreCase(status)) {
            return getString(R.string.threat_clean).toUpperCase();
        }
        return getString(R.string.threat_unknown).toUpperCase();
    }

    private int statusColor(String status) {
        if ("malicious".equalsIgnoreCase(status)) {
            return R.color.severity_critical;
        }
        if ("suspicious".equalsIgnoreCase(status)) {
            return R.color.severity_medium;
        }
        if ("clean".equalsIgnoreCase(status)) {
            return R.color.severity_info;
        }
        return R.color.soc_on_surface_variant;
    }

    private void setNotAvailable(View view, int id) {
        ((TextView) view.findViewById(id)).setText(getString(R.string.status_not_available));
    }

    private String notAvailable(String value) {
        return value != null && !value.isEmpty() ? value : getString(R.string.status_not_available);
    }

    private void openLink(String url) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
        } catch (android.content.ActivityNotFoundException ignored) {
        }
    }
}