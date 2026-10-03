<?php

require __DIR__ . '/../core/Bootstrap.php';

Bootstrap::run();
header('Content-Type: application/json; charset=utf-8');

$severity = Input::string('severity', 'all', 10);
$allowed = ['all', 'critical', 'high', 'medium', 'low'];
if (!in_array($severity, $allowed, true)) {
    Response::error('validation_error', 'Severity must be one of: ' . implode(', ', $allowed), 'backend', 400);
}

$limit = Input::int('limit', 50, 1, 200);
$offset = Input::int('offset', 0, 0, 100000);

$warnings = [];
$alerts = [];
$total = 0;
$wazuh = new WazuhService();

if ($wazuh->enabled()) {
    try {
        $page = $wazuh->alerts($severity, $limit, $offset);
        $alerts = $page['alerts'] ?? [];
        $total = (int) ($page['total'] ?? count($alerts));
    } catch (Exception $e) {
        $warnings[] = 'Wazuh is unavailable or timed out: ' . $e->getMessage();
        Logger::error('Alerts fetch failed', ['severity' => $severity, 'message' => $e->getMessage()]);
    }
}

if ($alerts === [] && MockData::enabled()) {
    $all = MockData::alertsPages();
    $filtered = [];
    foreach ($all as $alert) {
        if ($severity === 'all' || $alert['severity'] === $severity) {
            $filtered[] = $alert;
        }
    }
    $alerts = array_slice($filtered, $offset, $limit);
    $total = count($filtered);
    if (count($warnings) === 0) {
        $warnings[] = 'Returning mock alert data (Wazuh not available)';
    }
}

Response::success([
    'alerts' => $alerts,
    'total' => $total,
    'page' => intdiv($offset, $limit),
    'limit' => $limit,
], $warnings);