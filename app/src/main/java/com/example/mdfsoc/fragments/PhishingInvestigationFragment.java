package com.example.mdfsoc.fragments;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.mdfsoc.R;
import com.example.mdfsoc.models.RedirectHop;
import com.example.mdfsoc.models.UrlReputation;
import com.example.mdfsoc.models.VirusTotalUrlInfo;
import com.example.mdfsoc.utils.PhishingTextParser;
import com.example.mdfsoc.utils.Resource;
import com.example.mdfsoc.viewmodels.UrlInvestigationViewModel;
import com.google.android.material.textfield.TextInputEditText;

import java.util.List;
import java.util.Locale;

public class PhishingInvestigationFragment extends Fragment {

    public interface OnInvestigateIpListener {
        void onInvestigateIpRequested(String ip);
    }

    private static final String SAMPLE_MESSAGE =
            "Return-Path: <bounce@delivery-notice.example.org>\n"
                    + "Received: from mail.sender-net.example (unknown [45.137.22.198])\n"
                    + "\tby mx.corp.example with ESMTPS id abc123\n"
                    + "\tfor <user@corp.example>; Thu, 02 Oct 2026 09:14:22 +0000\n"
                    + "From: \"Account Verification\" <no-reply@paypa1-secure[.]com>\n"
                    + "Subject: Urgent: verify your mailbox\n"
                    + "\n"
                    + "Your mailbox expires today. Confirm your credentials here:\n"
                    + "hxxps://paypa1-secure[.]com/verify?session=8f21ac&redirect=%2Flogin\n"
                    + "\n"
                    + "Tracking: <a href=\"http://bit.ly/3xAmple\">Open tracking pixel</a>\n";

    private UrlInvestigationViewModel viewModel;
    private TextInputEditText messageInput;
    private String displayedRawUrl;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_phishing, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        messageInput = view.findViewById(R.id.input_message);
        viewModel = new ViewModelProvider(this).get(UrlInvestigationViewModel.class);

        view.findViewById(R.id.btn_extract).setOnClickListener(v -> extractArtifacts());
        view.findViewById(R.id.btn_sample).setOnClickListener(v -> {
            messageInput.setText(SAMPLE_MESSAGE);
            extractArtifacts();
        });
        view.findViewById(R.id.btn_url_retry).setOnClickListener(v -> viewModel.retry());

        TextView vtLink = view.findViewById(R.id.value_vt_link);
        vtLink.setOnClickListener(v -> {
            Object tag = vtLink.getTag();
            if (tag instanceof String && !((String) tag).isEmpty()) {
                openLink((String) tag);
            }
        });

        viewModel.getResult().observe(getViewLifecycleOwner(), this::render);
    }

    private void extractArtifacts() {
        String text = messageInput.getText() == null ? "" : messageInput.getText().toString();
        PhishingTextParser.Extraction extraction = PhishingTextParser.extract(text);
        renderExtraction(extraction);
    }

    private void renderExtraction(PhishingTextParser.Extraction extraction) {
        View view = getView();
        if (view == null) {
            return;
        }
        TextView warning = view.findViewById(R.id.warning_text);
        LinearLayout urlsContainer = view.findViewById(R.id.extracted_urls_container);
        LinearLayout ipsContainer = view.findViewById(R.id.extracted_ips_container);

        urlsContainer.removeAllViews();
        ipsContainer.removeAllViews();

        if (extraction.isEmpty()) {
            view.findViewById(R.id.extraction_card).setVisibility(View.GONE);
            warning.setText(R.string.no_artifacts_found);
            warning.setVisibility(View.VISIBLE);
            return;
        }

        view.findViewById(R.id.extraction_card).setVisibility(View.VISIBLE);
        if (!extraction.getUrls().isEmpty()) {
            addSectionHeader(urlsContainer, getString(R.string.label_urls_found));
        }
        if (!extraction.getHeaderIps().isEmpty()) {
            addSectionHeader(ipsContainer, getString(R.string.label_header_ips));
        }

        StringBuilder notes = new StringBuilder();
        for (PhishingTextParser.FoundUrl found : extraction.getUrls()) {
            notes.append(addUrlRow(urlsContainer, found));
        }
        for (String ip : extraction.getHeaderIps()) {
            addHeaderIpRow(ipsContainer, ip);
        }
        if (!extraction.isHeadersDetected() && !extraction.getHeaderIps().isEmpty()) {
            notes.append(getString(R.string.ip_source_body_warning));
        }
        if (!extraction.getSenderDomains().isEmpty()) {
            notes.append(getString(R.string.sender_domains,
                    TextUtils.join(", ", extraction.getSenderDomains())));
        }

        if (notes.length() == 0) {
            warning.setVisibility(View.GONE);
        } else {
            warning.setText(notes.toString());
            warning.setVisibility(View.VISIBLE);
        }
    }

    private void addSectionHeader(LinearLayout container, String label) {
        TextView header = new TextView(requireContext());
        header.setText(label);
        header.setTextColor(ContextCompat.getColor(requireContext(), R.color.soc_primary));
        header.setTextSize(12);
        header.setTypeface(header.getTypeface(), android.graphics.Typeface.BOLD);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.bottomMargin = 4;
        header.setLayoutParams(params);
        container.addView(header);
    }

    private String addUrlRow(LinearLayout container, PhishingTextParser.FoundUrl found) {
        TextView row = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 4;
        row.setLayoutParams(params);
        row.setPadding(12, 10, 12, 10);
        row.setBackgroundResource(R.drawable.bg_card_outline);
        row.setTextColor(ContextCompat.getColor(requireContext(), R.color.soc_on_surface));
        row.setTextSize(13);
        row.setTextIsSelectable(false);
        row.setText(found.getDecoded());
        row.setOnClickListener(v -> {
            displayedRawUrl = found.getRaw();
            viewModel.investigate(found.getDecoded());
        });
        container.addView(row);

        if (!found.wasObfuscated()) {
            return "";
        }
        return getString(R.string.url_deobfuscated, found.getRaw(), found.getDecoded()) + "\n";
    }

    private void addHeaderIpRow(LinearLayout container, String ip) {
        TextView row = new TextView(requireContext());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.topMargin = 4;
        row.setLayoutParams(params);
        row.setPadding(12, 10, 12, 10);
        row.setBackgroundResource(R.drawable.bg_card_outline);
        row.setTextColor(ContextCompat.getColor(requireContext(), R.color.soc_on_surface));
        row.setTextSize(13);
        row.setText(getString(R.string.header_ip_row, ip));
        row.setOnClickListener(v -> requestIpInvestigation(ip));
        container.addView(row);
    }

    private void requestIpInvestigation(String ip) {
        if (getActivity() instanceof OnInvestigateIpListener) {
            ((OnInvestigateIpListener) getActivity()).onInvestigateIpRequested(ip);
        }
    }

    private void render(Resource<UrlReputation> resource) {
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

        UrlReputation reputation = resource.getData();
        if (reputation == null) {
            view.findViewById(R.id.result_card).setVisibility(View.GONE);
            return;
        }

        view.findViewById(R.id.result_card).setVisibility(View.VISIBLE);
        ((TextView) view.findViewById(R.id.result_host))
                .setText(notAvailable(reputation.getHost()));

        TextView statusChip = view.findViewById(R.id.threat_status_chip);
        String status = reputation.getThreatStatus() != null
                ? reputation.getThreatStatus() : "unknown";
        statusChip.setText(statusLabel(status).toUpperCase(Locale.ROOT));
        statusChip.setBackgroundColor(ContextCompat.getColor(requireContext(), statusColor(status)));

        TextView original = view.findViewById(R.id.result_original_url);
        if (displayedRawUrl != null && !displayedRawUrl.equals(reputation.getUrl())) {
            original.setText(getString(R.string.url_as_written, displayedRawUrl));
            original.setVisibility(View.VISIBLE);
        } else {
            original.setVisibility(View.GONE);
        }

        TextView finalUrl = view.findViewById(R.id.result_final_url);
        if (reputation.wasRedirected()) {
            finalUrl.setText(getString(R.string.url_final, reputation.getFinalUrl()));
            finalUrl.setVisibility(View.VISIBLE);
        } else {
            finalUrl.setVisibility(View.GONE);
        }

        List<String> ips = reputation.getResolvedIps();
        ((TextView) view.findViewById(R.id.value_resolved_ips)).setText(
                ips == null || ips.isEmpty()
                        ? getString(R.string.status_not_available)
                        : TextUtils.join(", ", ips));

        VirusTotalUrlInfo vt = reputation.getVirusTotal();
        TextView vtLink = view.findViewById(R.id.value_vt_link);
        TextView detections = view.findViewById(R.id.value_vt_detections);
        if (vt != null) {
            int total = vt.getTotalEngineVerdicts() > 0 ? vt.getTotalEngineVerdicts()
                    : (vt.getMalicious() + vt.getSuspicious() + vt.getUndetected() + vt.getHarmless());
            detections.setText(getString(R.string.detections_ratio,
                    vt.getMalicious(), total));
            List<String> categories = vt.getCategories();
            View categoriesRow = view.findViewById(R.id.categories_row);
            if (categories != null && !categories.isEmpty()) {
                ((TextView) view.findViewById(R.id.value_categories))
                        .setText(TextUtils.join(", ", categories));
                categoriesRow.setVisibility(View.VISIBLE);
            } else {
                categoriesRow.setVisibility(View.GONE);
            }
            if (vt.getVtLink() != null && !vt.getVtLink().isEmpty()) {
                vtLink.setTag(vt.getVtLink());
                vtLink.setVisibility(View.VISIBLE);
            } else {
                vtLink.setVisibility(View.GONE);
            }
        } else {
            detections.setText(R.string.status_not_available);
            view.findViewById(R.id.categories_row).setVisibility(View.GONE);
            vtLink.setVisibility(View.GONE);
        }

        ((TextView) view.findViewById(R.id.value_redirects)).setText(
                getString(R.string.redirect_count, reputation.getRedirectCount()));

        TextView chain = view.findViewById(R.id.redirect_chain_text);
        List<RedirectHop> hops = reputation.getRedirectChain();
        if (hops == null || hops.isEmpty()) {
            chain.setVisibility(View.GONE);
        } else {
            StringBuilder text = new StringBuilder(getString(R.string.redirect_chain_header));
            int index = 1;
            for (RedirectHop hop : hops) {
                text.append('\n').append(index++).append(". ")
                        .append(notAvailable(hop.getUrl()))
                        .append("  \u2192  ").append(hop.getStatus());
            }
            chain.setText(text.toString());
            chain.setVisibility(View.VISIBLE);
        }

        String primaryIp = reputation.getPrimaryIp();
        View investigateIp = view.findViewById(R.id.btn_investigate_ip);
        if (primaryIp != null && !primaryIp.isEmpty()) {
            investigateIp.setTag(primaryIp);
            investigateIp.setVisibility(View.VISIBLE);
        } else {
            investigateIp.setVisibility(View.GONE);
        }
        investigateIp.setOnClickListener(v -> {
            Object tag = investigateIp.getTag();
            if (tag instanceof String) {
                requestIpInvestigation((String) tag);
            }
        });

        List<String> sources = reputation.getSources();
        TextView sourcesView = view.findViewById(R.id.value_sources);
        if (sources == null || sources.isEmpty()) {
            sourcesView.setVisibility(View.GONE);
        } else {
            sourcesView.setText(getString(R.string.sources_line, TextUtils.join(", ", sources)));
            sourcesView.setVisibility(View.VISIBLE);
        }
    }

    private String statusLabel(String status) {
        if ("malicious".equalsIgnoreCase(status)) {
            return getString(R.string.threat_malicious);
        }
        if ("suspicious".equalsIgnoreCase(status)) {
            return getString(R.string.threat_suspicious);
        }
        if ("clean".equalsIgnoreCase(status)) {
            return getString(R.string.threat_clean);
        }
        return getString(R.string.threat_unknown);
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