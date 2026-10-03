<?php

require __DIR__ . '/../core/Bootstrap.php';

Bootstrap::run();
header('Content-Type: application/json; charset=utf-8');

$ip = Input::string('ip', '', 64);
if ($ip === '' || !filter_var($ip, FILTER_VALIDATE_IP)) {
    Response::error('validation_error', 'A valid IPv4 or IPv6 address is required', 'backend', 400);
}

$cacheKey = 'threat-intel:' . $ip;
$cached = Cache::get($cacheKey);
if ($cached !== null) {
    Response::success($cached['data'], $cached['warnings'] ?? [], 200);
}

$warnings = [];
$sources = [];
$abuse = null;
$vt = null;

$abuseService = new AbuseIPDBService();
if ($abuseService->enabled()) {
    try {
        $abuse = $abuseService->check($ip);
        $sources[] = 'abuseipdb';
    } catch (RateLimitException $e) {
        $warnings[] = $e->getMessage();
    } catch (Exception $e) {
        $warnings[] = 'AbuseIPDB unavailable: ' . ($e instanceof UpstreamException ? 'temporarily unreachable' : 'error');
        Logger::error('Threat intel AbuseIPDB failed', ['ip' => $ip, 'message' => $e->getMessage()]);
    }
}

$vtService = new VirusTotalService();
if ($vtService->enabled()) {
    try {
        $report = $vtService->ipReport($ip);
        if ($report !== null) {
            $vt = $report;
            $sources[] = 'virustotal';
        } else {
            $warnings[] = 'No VirusTotal report found for this IP';
        }
    } catch (RateLimitException $e) {
        $warnings[] = $e->getMessage();
    } catch (Exception $e) {
        $warnings[] = 'VirusTotal unavailable: temporarily unreachable';
        Logger::error('Threat intel VirusTotal failed', ['ip' => $ip, 'message' => $e->getMessage()]);
    }
}

if ($abuse === null && $vt === null) {
    if (MockData::enabled()) {
        $data = MockData::threatIntelligence($ip);
        $cached = ['data' => $data, 'warnings' => []];
        Cache::set($cacheKey, $cached, Config::int('cache.threat_intel_ttl', 1800));
        Response::success($data, [], 200);
    }
    Response::error('upstream_error', 'No threat intelligence sources available', 'backend', 502);
}

$risk = Verdict::threatRisk($abuse, $vt);
$mitre = MitreMapper::fromRisk($risk);

$data = [
    'ip' => $ip,
    'threat_status' => $risk,
    'abuseipdb' => $abuse,
    'virustotal' => $vt,
    'mitre' => $mitre,
    'sources' => $sources,
];

$cached = ['data' => $data, 'warnings' => $warnings];
Cache::set($cacheKey, $cached, Config::int('cache.threat_intel_ttl', 1800));
Response::success($data, $warnings, 200);