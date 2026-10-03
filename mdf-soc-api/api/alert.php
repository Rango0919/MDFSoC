<?php

require __DIR__ . '/../core/Bootstrap.php';

Bootstrap::run();
header('Content-Type: application/json; charset=utf-8');

$id = Input::int('id', 0, 1, 999999999);
if ($id === 0) {
    Response::error('validation_error', 'A valid alert id is required', 'backend', 400);
}

$warnings = [];
$wazuh = new WazuhService();

try {
    if ($wazuh->enabled()) {
        $alert = $wazuh->alertById($id);
        if ($alert === null) {
            $alert = MockData::enabled() ? MockData::alertDetail($id) : null;
        }
    } else {
        $alert = MockData::enabled() ? MockData::alertDetail($id) : null;
        if (MockData::enabled()) {
            $warnings[] = 'Returning mock alert data (Wazuh not available)';
        }
    }
} catch (Exception $e) {
    Logger::error('Alert detail fetch failed', ['id' => $id, 'message' => $e->getMessage()]);
    if (MockData::enabled()) {
        $alert = MockData::alertDetail($id);
        $warnings[] = 'Wazuh is unavailable, returning mock data';
    } else {
        Response::error('upstream_error', 'Wazuh is unavailable or timed out', 'wazuh', 502);
    }
}

if ($alert === null) {
    Response::error('not_found', 'Alert not found', 'backend', 404);
}

Response::success($alert, $warnings);