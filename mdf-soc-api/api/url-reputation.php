<?php

require __DIR__ . '/../core/Bootstrap.php';

Bootstrap::run();
header('Content-Type: application/json; charset=utf-8');

$raw = Input::string('url', '', 4096);
if ($raw === '') {
    Response::error('validation_error', 'A URL is required', 'backend', 400);
}

$url = UrlAnalyzer::normalize($raw);
if (!UrlAnalyzer::isValidUrl($url)) {
    Response::error('validation_error', 'Only absolute http and https URLs can be analysed', 'backend', 400);
}

$cacheKey = 'url-rep:' . hash('sha256', $url);
$cached = Cache::get($cacheKey);
if ($cached !== null) {
    Response::success($cached['data'], $cached['warnings'] ?? [], 200);
}

$warnings = [];
$sources = [];

$resolution = UrlAnalyzer::resolve($url);
if (!$resolution['ok'] && $resolution['redirect_chain'] === []) {
    Logger::error('URL resolution failed', ['url' => $url, 'error' => $resolution['error']]);
    Response::error('upstream_error', $resolution['error'] ?? 'URL could not be resolved', 'backend', 502);
}
if (!$resolution['ok']) {
    $warnings[] = 'Redirect chain incomplete: ' . $resolution['error'];
}
if ($resolution['shortened'] && $resolution['redirect_count'] > 0) {
    $warnings[] = 'URL was shortened and expanded to ' . $resolution['final_url'];
}

$finalUrl = $resolution['final_url'] ?? $url;

$vt = null;
$vtService = new VirusTotalService();
if ($vtService->enabled()) {
    try {
        $vt = $vtService->urlReport($url);
        if ($vt !== null) {
            $sources[] = 'virustotal';
            if ($resolution['final_url'] !== null && $resolution['final_url'] !== $url) {
                $finalReport = $vtService->urlReport($resolution['final_url']);
                if ($finalReport !== null) {
                    $vt['expanded_report'] = $finalReport;
                    $sources[] = 'virustotal_expanded';
                }
            }
        } else {
            $warnings[] = 'No VirusTotal report found for this URL';
        }
    } catch (RateLimitException $e) {
        $warnings[] = $e->getMessage();
    } catch (Exception $e) {
        $warnings[] = 'VirusTotal unavailable: temporarily unreachable';
        Logger::error('VirusTotal URL lookup failed', ['url' => $url, 'message' => $e->getMessage()]);
    }
}

if ($vt === null) {
    $risk = 'unknown';
}

$risk = UrlAnalyzer::isPublicIp(($resolution['resolved_ips'][0] ?? ''))
    ? Verdict::threatRisk(null, $vt)
    : 'unknown';

$data = [
    'url' => $url,
    'final_url' => $finalUrl,
    'host' => $resolution['host'],
    'resolved_ips' => $resolution['resolved_ips'] ?? [],
    'redirect_chain' => $resolution['redirect_chain'] ?? [],
    'redirect_count' => $resolution['redirect_count'] ?? 0,
    'shortened' => (bool) ($resolution['shortened'] ?? false),
    'reachable' => (bool) ($resolution['reachable'] ?? false),
    'final_status' => $resolution['final_status'] ?? 0,
    'threat_status' => $risk,
    'virustotal' => $vt,
    'sources' => $sources,
];

Cache::set($cacheKey, ['data' => $data, 'warnings' => $warnings], Config::int('cache.threat_intel_ttl', 1800));
Response::success($data, $warnings, 200);