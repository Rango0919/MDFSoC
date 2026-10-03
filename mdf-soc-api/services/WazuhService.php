<?php

final class WazuhService
{
    private array $cfg;

    public function __construct()
    {
        $this->cfg = (array) Config::get('wazuh', []);
    }

    public function enabled(): bool
    {
        return (bool) ($this->cfg['enabled'] ?? false);
    }

    public function summary(): array
    {
        $severities = ['critical_alerts' => 'critical', 'high_alerts' => 'high', 'medium_alerts' => 'medium', 'low_alerts' => 'low'];
        $counts = ['total_alerts' => 0, 'critical_alerts' => 0, 'high_alerts' => 0, 'medium_alerts' => 0, 'low_alerts' => 0];
        foreach ($severities as $field => $severity) {
            $items = $this->alerts($severity, 1, 0);
            $counts[$field] = (int) ($items['total'] ?? 0);
            $counts['total_alerts'] += $counts[$field];
        }
        return $counts;
    }

    public function alerts(string $severity, int $limit, int $offset): array
    {
        $query = [];
        $q = $this->severityQuery($severity);
        if ($q !== '') {
            $query['q'] = $q;
        }
        $query['limit'] = $limit > 0 ? $limit : 50;
        $query['offset'] = $offset > 0 ? $offset : 0;
        $query['sort'] = '-timestamp';

        $cacheKey = 'wazuh:alerts:' . md5(json_encode($query));
        $cached = Cache::get($cacheKey);
        if ($cached !== null) {
            return $cached;
        }

        $body = $this->api('GET', $this->cfg['alerts_endpoint'], $query);
        $items = $body['data']['affected_items'] ?? [];
        $total = (int) ($body['data']['total_affected_items'] ?? count($items));

        $alerts = [];
        foreach ($items as $item) {
            if (is_array($item)) {
                $alerts[] = $this->normalize($item);
            }
        }

        $result = ['alerts' => $alerts, 'total' => $total];
        Cache::set($cacheKey, $result, Config::int('cache.alert_ttl', 60));
        return $result;
    }

    public function alertById(int $id): ?array
    {
        if ($id <= 0) {
            return null;
        }
        $body = $this->api('GET', $this->cfg['alerts_endpoint'], [
            'q' => 'id=' . $id,
            'limit' => 1,
        ]);
        $items = $body['data']['affected_items'] ?? [];
        foreach ($items as $item) {
            if (is_array($item) && (int) ($item['id'] ?? 0) === $id) {
                return $this->normalize($item);
            }
        }
        return null;
    }

    public function normalize(array $item): array
    {
        $rule = is_array($item['rule'] ?? null) ? $item['rule'] : [];
        $agent = is_array($item['agent'] ?? null) ? $item['agent'] : [];
        $data = is_array($item['data'] ?? null) ? $item['data'] : [];

        $level = (int) ($rule['level'] ?? 0);
        $title = $rule['description'] ?? ($data['description'] ?? 'Wazuh alert');
        $status = $item['status'] ?? 'active';

        return [
            'id' => (int) ($item['id'] ?? 0),
            'severity' => self::severityLabel($level),
            'title' => (string) $title,
            'source_ip' => $data['srcip'] ?? ($data['src_ip'] ?? null),
            'dest_ip' => $data['dstip'] ?? ($data['dst_ip'] ?? null),
            'timestamp' => $item['timestamp'] ?? null,
            'description' => $data['description'] ?? $rule['description'] ?? null,
            'status' => $status,
            'rule_id' => (string) ($rule['id'] ?? ''),
            'rule_level' => $level,
            'agent' => $agent['name'] ?? $agent['id'] ?? null,
            'investigation_status' => $status,
            'mitre' => MitreMapper::fromRule($rule),
            'mitre_technique' => null,
        ];
    }

    public static function severityLabel(int $level): string
    {
        if ($level >= 13) {
            return 'critical';
        }
        if ($level >= 10) {
            return 'high';
        }
        if ($level >= 7) {
            return 'medium';
        }
        return 'low';
    }

    private function severityQuery(string $severity): string
    {
        switch ($severity) {
            case 'critical':
                return 'rule.level>=13';
            case 'high':
                return 'rule.level>=10;rule.level<13';
            case 'medium':
                return 'rule.level>=7;rule.level<10';
            case 'low':
                return 'rule.level>=3;rule.level<7';
            default:
                return '';
        }
    }

    private function api(string $method, string $path, array $query = [], int $retries = 1): array
    {
        $base = rtrim((string) ($this->cfg['api_url'] ?? ''), '/');
        if ($base === '') {
            throw new UpstreamException('Wazuh is not configured', 'wazuh');
        }
        $url = $base . $path;
        if ($query !== []) {
            $url .= '?' . http_build_query($query);
        }

        $options = [
            'timeout' => (int) ($this->cfg['timeout'] ?? 25),
            'verify_ssl' => (bool) ($this->cfg['verify_ssl'] ?? true),
            'ca_file' => (string) ($this->cfg['ca_file'] ?? ''),
            'headers' => ['Authorization: Bearer ' . $this->token()],
        ];

        $result = HttpClient::request($method, $url, $options);
        if ($result['ok'] === false && $result['status'] === 401 && $retries > 0) {
            Cache::set('wazuh:token', '', 1);
            return $this->api($method, $path, $query, $retries - 1);
        }

        $body = json_decode((string) ($result['raw'] ?? ''), true);
        if (!is_array($body)) {
            if ($result['status'] === 0) {
                throw new UpstreamException('Wazuh is unreachable or timed out', 'wazuh');
            }
            throw new UpstreamException('Wazuh returned an invalid response', 'wazuh');
        }

        $error = $body['error'] ?? 0;
        if ($error !== 0 && $error !== null) {
            if (($error === 1400 || $error === 1401) && $retries > 0) {
                Cache::set('wazuh:token', '', 1);
                return $this->api($method, $path, $query, $retries - 1);
            }
            throw new UpstreamException('Wazuh error ' . $error . ': ' . ($body['message'] ?? 'unknown'), 'wazuh');
        }

        return $body;
    }

    private function token(): string
    {
        $cached = Cache::get('wazuh:token');
        if (is_string($cached) && $cached !== '') {
            return $cached;
        }
        $base = rtrim((string) ($this->cfg['api_url'] ?? ''), '/');
        $options = [
            'timeout' => (int) ($this->cfg['timeout'] ?? 25),
            'verify_ssl' => (bool) ($this->cfg['verify_ssl'] ?? false),
            'ca_file' => (string) ($this->cfg['ca_file'] ?? ''),
            'headers' => [
                'Authorization: Basic ' . base64_encode($this->cfg['user'] . ':' . $this->cfg['pass']),
                'Content-Type: application/json',
            ],
            'body' => '{}',
        ];
        $result = HttpClient::request('POST', $base . '/security/user/authenticate', $options);
        $body = json_decode((string) ($result['raw'] ?? ''), true);
        if (!is_array($body) || !isset($body['data']['token'])) {
            throw new UpstreamException('Wazuh authentication failed', 'wazuh');
        }
        $token = (string) $body['data']['token'];
        Cache::set('wazuh:token', $token, Config::int('cache.token_ttl', 3540));
        return $token;
    }
}