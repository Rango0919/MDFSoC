<?php

require __DIR__ . '/../core/Bootstrap.php';

Bootstrap::run();
header('Content-Type: application/json; charset=utf-8');

$warnings = [];

$counts = null;
$recent = [];
$wazuh = new WazuhService();

if ($wazuh->enabled()) {
    try {
        $summary = $wazuh->summary();
        $counts = $summary;
        $page = $wazuh->alerts('all', 5, 0);
        $recent = $page['alerts'] ?? [];
    } catch (Exception $e) {
        $warnings[] = 'Wazuh is unavailable or timed out: ' . $e->getMessage();
        Logger::error('Dashboard Wazuh fetch failed', ['message' => $e->getMessage()]);
    }
}

if ($counts === null) {
    if (MockData::enabled()) {
        $counts = MockData::dashboard();
        $recent = $counts['recent_alerts'] ?? [];
        $warnings[] = 'Returning mock dashboard data (Wazuh not available)';
    } else {
        $counts = [
            'total_alerts' => 0,
            'critical_alerts' => 0,
            'high_alerts' => 0,
            'medium_alerts' => 0,
            'low_alerts' => 0,
        ];
        $recent = [];
        $warnings[] = 'Wazuh is disabled';
    }
}

$response = $counts;
$response['recent_alerts'] = $recent;

Response::success($response, $warnings);