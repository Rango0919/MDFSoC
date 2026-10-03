<?php
/**
 * CLI email notifier for MDF SOC.
 *
 * Usage:
 *   php scripts/notify.php alerts   - email new critical/high alerts since last run
 *   php scripts/notify.php digest   - email a summary digest of current alert volumes
 *
 * Schedule with cron, e.g. (adjust path to your deploy location):
 *   * * * * *  php /path/to/mdf-soc-api/scripts/notify.php alerts >> logs/notify.log 2>&1
 *   15 9 * * * php /path/to/mdf-soc-api/scripts/notify.php digest >> logs/notify.log 2>&1
 */

require __DIR__ . '/../core/Bootstrap.php';
Bootstrap::cli();

$mode = $argv[1] ?? 'alerts';
$mailer = new Mailer();

$in = date('Y-m-d H:i:s');
if (!$mailer->enabled()) {
    fwrite(STDERR, "[$in] Email is not enabled (check config 'email' section).\n");
    exit(0);
}

$stateFile = __DIR__ . '/../storage/notify_state.json';
$state = [];
if (is_file($stateFile)) {
    $state = json_decode((string) file_get_contents($stateFile), true) ?: [];
}

$alerts = fetchAlerts();
if ($alerts === []) {
    fwrite(STDERR, "[$in] No alert source available (Wazuh disabled and mock off).\n");
    exit(0);
}

$sent = 0;

if ($mode === 'digest') {
    $today = date('Y-m-d');
    if (($state['digest_date'] ?? '') === $today) {
        fwrite(STDERR, "[$in] Digest already sent today.\n");
        exit(0);
    }
    $sent += sendDigest($mailer, $alerts);
    $state['digest_date'] = $today;
} else {
    if (!$mailer->notifyCriticalHigh()) {
        fwrite(STDOUT, "[$in] Alert notifications disabled in config.\n");
        exit(0);
    }
    $lastId = (int) ($state['last_alert_id'] ?? 0);
    $maxId = 0;
    $newAlerts = [];
    foreach ($alerts as $a) {
        $id = (int) ($a['id'] ?? 0);
        $maxId = max($maxId, $id);
        if ($id > $lastId && in_array($a['severity'] ?? '', ['critical', 'high'], true)) {
            $newAlerts[] = $a;
        }
    }
    if ($newAlerts !== []) {
        $sent += sendAlerts($mailer, $newAlerts);
    } else {
        fwrite(STDOUT, "[$in] No new critical/high alerts (last id $lastId, max id $maxId).\n");
    }
    if ($maxId > $lastId) {
        $state['last_alert_id'] = $maxId;
    }
}

file_put_contents($stateFile, json_encode($state, JSON_PRETTY_PRINT | JSON_UNESCAPED_SLASHES), LOCK_EX);
fwrite(STDOUT, "[$in] Done. Emails sent: $sent\n");

/** Pulls the current alert set from the best available source. */
function fetchAlerts(): array
{
    $wazuh = new WazuhService();
    if ($wazuh->enabled()) {
        try {
            return $wazuh->alerts('all', 100, 0)['alerts'] ?? [];
        } catch (Exception $e) {
            Logger::error('notify: Wazuh fetch failed', ['message' => $e->getMessage()]);
            return [];
        }
    }
    if (MockData::enabled()) {
        return MockData::alertsPages();
    }
    return [];
}

function sendAlerts(Mailer $mailer, array $alerts): int
{
    $lines = '';
    foreach ($alerts as $a) {
        $sev = strtoupper($a['severity'] ?? '?');
        $src = $a['source_ip'] ?? '-';
        $dst = $a['dest_ip'] ?? '-';
        $lines .= "<tr><td style='padding:6px 8px;border-bottom:1px solid #eee;color:"
            . htmlspecialchars(severityColor($a['severity'] ?? ''), ENT_QUOTES) . ";font-weight:bold'>"
            . htmlspecialchars($sev) . "</td>"
            . "<td style='padding:6px 8px;border-bottom:1px solid #eee'>" . htmlspecialchars($a['title'] ?? '') . "</td>"
            . "<td style='padding:6px 8px;border-bottom:1px solid #eee'>" . htmlspecialchars((string) $src) . "</td>"
            . "<td style='padding:6px 8px;border-bottom:1px solid #eee'>" . htmlspecialchars((string) $dst) . "</td>"
            . "<td style='padding:6px 8px;border-bottom:1px solid #eee'>"
            . htmlspecialchars((string) ($a['timestamp'] ?? '-')) . "</td></tr>";
    }
    $n = count($alerts);
    $html = "<html><body style='font-family:Arial,sans-serif;color:#222'>"
        . "<h2>MDF SOC - $n new high/critical alert(s)</h2>"
        . ($n === 1 ? "<p>A new alert requires your attention.</p>" : "<p>New alerts require your attention.</p>")
        . "<table style='border-collapse:collapse;font-size:13px'>"
        . "<tr style='background:#f5f5f5'><th style='padding:6px 8px;text-align:left'>Severity</th>"
        . "<th style='padding:6px 8px;text-align:left'>Title</th><th style='padding:6px 8px;text-align:left'>Source</th>"
        . "<th style='padding:6px 8px;text-align:left'>Dest</th><th style='padding:6px 8px;text-align:left'>Time</th></tr>"
        . $lines . "</table></body></html>";
    return $mailer->send("MDF SOC: $n new high/critical alert(s)", $html);
}

function sendDigest(Mailer $mailer, array $alerts): int
{
    $counts = ['total' => count($alerts), 'critical' => 0, 'high' => 0, 'medium' => 0, 'low' => 0];
    foreach ($alerts as $a) {
        $s = $a['severity'] ?? 'low';
        if (isset($counts[$s])) {
            $counts[$s]++;
        }
    }
    $recent = array_slice($alerts, 0, 10);
    $rows = '';
    foreach ($recent as $a) {
        $rows .= "<tr><td style='padding:4px 8px;border-bottom:1px solid #eee'>"
            . htmlspecialchars((string) ($a['timestamp'] ?? '-')) . "</td>"
            . "<td style='padding:4px 8px;border-bottom:1px solid #eee;color:"
            . htmlspecialchars(severityColor($a['severity'] ?? ''), ENT_QUOTES) . "'>"
            . htmlspecialchars(strtoupper($a['severity'] ?? '?')) . "</td>"
            . "<td style='padding:4px 8px;border-bottom:1px solid #eee'>"
            . htmlspecialchars($a['title'] ?? '') . "</td></tr>";
    }
    $html = "<html><body style='font-family:Arial,sans-serif;color:#222'>"
        . "<h2>MDF SOC - Daily Digest</h2>"
        . "<p>Alert volumes as of " . date('Y-m-d H:i') . ":</p>"
        . "<table style='border-collapse:collapse;font-size:13px'><tr style='background:#f5f5f5'>"
        . "<th style='padding:6px 8px;text-align:left'>Critical</th><td style='padding:6px 8px'>{$counts['critical']}</td></tr>"
        . "<tr><th style='padding:6px 8px;text-align:left'>High</th><td style='padding:6px 8px'>{$counts['high']}</td></tr>"
        . "<tr style='background:#fafafa'><th style='padding:6px 8px;text-align:left'>Medium</th><td style='padding:6px 8px'>{$counts['medium']}</td></tr>"
        . "<tr><th style='padding:6px 8px;text-align:left'>Low</th><td style='padding:6px 8px'>{$counts['low']}</td></tr>"
        . "<tr style='background:#f5f5f5'><th style='padding:6px 8px;text-align:left'>Total (window)</th><td style='padding:6px 8px'>{$counts['total']}</td></tr>"
        . "</table><h3>Recent alerts</h3>"
        . "<table style='border-collapse:collapse;font-size:13px'>"
        . "<tr style='background:#f5f5f5'><th style='padding:4px 8px;text-align:left'>Time</th>"
        . "<th style='padding:4px 8px;text-align:left'>Severity</th><th style='padding:4px 8px;text-align:left'>Title</th></tr>"
        . $rows . "</table></body></html>";
    return $mailer->send('MDF SOC daily digest - ' . date('Y-m-d'), $html);
}

function severityColor(string $severity): string
{
    switch ($severity) {
        case 'critical':
            return '#c62828';
        case 'high':
            return '#ef6c00';
        case 'medium':
            return '#f9a825';
        default:
            return '#2e7d32';
    }
}