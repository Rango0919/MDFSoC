<?php

final class AbuseIPDBService
{
    private array $cfg;

    public function __construct()
    {
        $this->cfg = (array) Config::get('abuseipdb', []);
    }

    public function enabled(): bool
    {
        return (bool) ($this->cfg['enabled'] ?? false) && $this->cfg['api_key'] !== '';
    }

    public function check(string $ip): array
    {
        $endpoint = (string) ($this->cfg['endpoint'] ?? 'https://api.abuseipdb.com/api/v2/check');
        $query = http_build_query([
            'ipAddress' => $ip,
            'maxAgeInDays' => (int) ($this->cfg['max_age_days'] ?? 90),
        ]);

        $options = [
            'timeout' => (int) ($this->cfg['timeout'] ?? 20),
            'verify_ssl' => true,
            'headers' => [
                'Key: ' . $this->cfg['api_key'],
                'Accept: application/json',
            ],
        ];

        $result = HttpClient::request('GET', $endpoint . '?' . $query, $options);
        return $this->parse($result, $ip);
    }

    private function parse(array $result, string $ip): array
    {
        if ($result['status'] === 429) {
            throw new RateLimitException('AbuseIPDB rate limit reached. Please retry later.', 'abuseipdb');
        }
        if ($result['status'] === 0) {
            throw new UpstreamException('AbuseIPDB is unreachable or timed out', 'abuseipdb');
        }
        if ($result['status'] === 404) {
            throw new UpstreamException('No AbuseIPDB report found for this IP', 'abuseipdb');
        }
        $body = json_decode((string) ($result['raw'] ?? ''), true);
        if (!is_array($body) || !isset($body['data'])) {
            throw new UpstreamException('AbuseIPDB returned an invalid response', 'abuseipdb');
        }

        $d = $body['data'];
        return [
            'ip' => $d['ipAddress'] ?? $ip,
            'is_public' => (bool) ($d['isPublic'] ?? true),
            'abuse_confidence_score' => (int) ($d['abuseConfidenceScore'] ?? 0),
            'total_reports' => (int) ($d['totalReports'] ?? 0),
            'country_code' => $d['countryCode'] ?? null,
            'country_name' => $d['countryName'] ?? null,
            'isp' => $d['isp'] ?? null,
            'domain' => $d['domain'] ?? null,
            'usage_type' => $d['usageType'] ?? null,
            'is_whitelisted' => $d['isWhitelisted'] ?? null,
            'last_reported_at' => $d['lastReportedAt'] ?? null,
            'num_distinct_users' => (int) ($d['numDistinctUsers'] ?? 0),
        ];
    }
}