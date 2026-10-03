<?php

require __DIR__ . '/../core/Bootstrap.php';

Bootstrap::run();
header('Content-Type: application/json; charset=utf-8');

$ip = Input::string('ip', '', 64);
if ($ip === '' || !filter_var($ip, FILTER_VALIDATE_IP)) {
    Response::error('validation_error', 'A valid IPv4 or IPv6 address is required', 'backend', 400);
}

$warnings = [];
$cacheKey = 'abuseipdb:' . $ip;
$cached = Cache::get($cacheKey);
if ($cached !== null) {
    Response::success($cached, [], 200);
}

$service = new AbuseIPDBService();

if ($service->enabled()) {
    try {
        $data = $service->check($ip);
        Cache::set($cacheKey, $data, Config::int('cache.abuseipdb_ttl', 3600));
        Response::success($data, [], 200);
    } catch (RateLimitException $e) {
        Response::error('rate_limited', $e->getMessage(), 'abuseipdb', 429);
    } catch (Exception $e) {
        Logger::error('AbuseIPDB check failed', ['ip' => $ip, 'message' => $e->getMessage()]);
        if (MockData::enabled()) {
            $data = MockData::ipReputation();
            $warnings[] = 'AbuseIPDB unavailable, returning mock data';
            Response::success($data, $warnings, 200);
        }
        Response::error('upstream_error', $e->getMessage(), 'abuseipdb', 502);
    }
}

if (MockData::enabled()) {
    $warnings[] = 'AbuseIPDB is not enabled, returning mock data';
    Response::success(MockData::ipReputation(), $warnings, 200);
}

Response::error('upstream_error', 'AbuseIPDB is not enabled', 'abuseipdb', 502);