package com.example.mdfsoc.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.example.mdfsoc.R;
import com.example.mdfsoc.fragments.AlertsFragment;
import com.example.mdfsoc.fragments.DashboardFragment;
import com.example.mdfsoc.fragments.IpInvestigationFragment;
import com.example.mdfsoc.fragments.PhishingInvestigationFragment;
import com.example.mdfsoc.utils.SessionManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.appbar.MaterialToolbar;

public class MainActivity extends AppCompatActivity
        implements DashboardFragment.OnNavigateListener,
        PhishingInvestigationFragment.OnInvestigateIpListener {

    private static final String TAG_DASHBOARD = "dashboard";
    private static final String TAG_ALERTS = "alerts";
    private static final String TAG_INVESTIGATE = "investigate";
    private static final String TAG_PHISHING = "phishing";

    private SessionManager session;
    private Fragment dashboardFragment;
    private Fragment alertsFragment;
    private Fragment investigateFragment;
    private Fragment phishingFragment;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        session = SessionManager.getInstance(this);
        setContentView(R.layout.activity_main);

        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setOnItemSelectedListener(this::onNavItemSelected);

        ensureLoggedIn();
        if (savedInstanceState == null) {
            switchTo(TAG_DASHBOARD);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ensureLoggedIn();
    }

    private void ensureLoggedIn() {
        if (!session.isLoggedIn()) {
            startActivity(new Intent(this, LoginActivity.class));
        }
    }

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.nav_alerts) {
            switchTo(TAG_ALERTS);
        } else if (id == R.id.nav_investigate) {
            switchTo(TAG_INVESTIGATE);
        } else if (id == R.id.nav_phishing) {
            switchTo(TAG_PHISHING);
        } else {
            switchTo(TAG_DASHBOARD);
        }
        return true;
    }

    private void switchTo(String tag) {
        Fragment target;
        switch (tag) {
            case TAG_ALERTS:
                target = getFragment(TAG_ALERTS, alertsFragment, new AlertsFragment());
                alertsFragment = target;
                break;
            case TAG_INVESTIGATE:
                target = getFragment(TAG_INVESTIGATE, investigateFragment, new IpInvestigationFragment());
                investigateFragment = target;
                break;
            case TAG_PHISHING:
                target = getFragment(TAG_PHISHING, phishingFragment, new PhishingInvestigationFragment());
                phishingFragment = target;
                break;
            case TAG_DASHBOARD:
            default:
                target = getFragment(TAG_DASHBOARD, dashboardFragment, new DashboardFragment());
                dashboardFragment = target;
                break;
        }

        String title;
        if (TAG_ALERTS.equals(tag)) {
            title = getString(R.string.title_alerts);
        } else if (TAG_INVESTIGATE.equals(tag)) {
            title = getString(R.string.action_investigate);
        } else if (TAG_PHISHING.equals(tag)) {
            title = getString(R.string.title_phishing);
        } else {
            title = getString(R.string.nav_dashboard);
        }
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        if (toolbar != null) {
            toolbar.setTitle(title);
        }

        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, target, tag)
                .commit();
    }

    private Fragment getFragment(String tag, Fragment cached, Fragment fallback) {
        if (cached != null && cached.isAdded()) {
            return cached;
        }
        Fragment existing = getSupportFragmentManager().findFragmentByTag(tag);
        return existing != null ? existing : fallback;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_sign_out) {
            session.clear();
            rerouteToLogin();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void rerouteToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    public void onInvestigateIpRequested(String ip) {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_investigate);
        if (investigateFragment instanceof IpInvestigationFragment) {
            ((IpInvestigationFragment) investigateFragment).setPendingIp(ip);
        }
    }

    @Override
    public void onOpenAlertsRequested() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_nav);
        bottomNav.setSelectedItemId(R.id.nav_alerts);
    }
}