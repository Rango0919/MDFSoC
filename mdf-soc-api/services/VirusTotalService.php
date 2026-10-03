<?php

final class VirusTotalService
{
    private array $cfg;

    public function __construct()
    {
        $this->cfg = (array) Config::get('virustotal', []);
    }

    public function enabled(): bool
    {
        return (bool) ($this->cfg['enabled'] ?? false) && $this->cfg['api_key'] !== '';
    }

    public function ipReport(string $ip): ?array
    {
        $base = rtrim((string) ($this->cfg['endpoint'] ?? 'https://www.virustotal.com/api/v3'), '/');
        $url = $base . '/ip_addresses/' . rawurlencode($ip);

        $options = [
            'timeout' => (int) ($this->cfg['timeout'] ?? 25),
            'verify_ssl' => true,
            'headers' => ['x-apikey: ' . $this->cfg['api_key']],
        ];

        $result = HttpClient::request('GET', $url, $options);
        return $this->parse($result, $ip);
    }

    public function urlReport(string $url): ?array
    {
        $base = rtrim((string) ($this->cfg['endpoint'] ?? 'https://www.virustotal.com/api/v3'), '/');
        $id = $this->urlId($url);
        $endpoint = $base . '/urls/' . $id;

        $options = [
            'timeout' => (int) ($this->cfg['timeout'] ?? 25),
            'verify_ssl' => true,
            'headers' => ['x-apikey: ' . $this->cfg['api_key']],
        ];

        $result = HttpClient::request('GET', $endpoint, $options);
        return $this->parseUrl($result, $url);
    }

    private function urlId(string $url): string
    {
        $packed = hash('sha256', $url, true);
        return strtr(base64_encode($packed), '+/', '-_');
    }

    private function parseUrl(array $result, string $url): ?array
    {
        if ($result['status'] === 429) {
            throw new RateLimitException('VirusTotal rate limit reached. Please retry later.', 'virustotal');
        }
        if ($result['status'] === 0) {
            throw new UpstreamException('VirusTotal is unreachable or timed out', 'virustotal');
        }
        if ($result['status'] === 404) {
            return null;
        }
        if ($result['status'] === 400) {
            return null;
        }
        $body = json_decode((string) ($result['raw'] ?? ''), true);
        if (!is_array($body) || !isset($body['data']['attributes'])) {
            throw new UpstreamException('VirusTotal returned an invalid response', 'virustotal');
        }

        $attrs = $body['data']['attributes'];
        $stats = $attrs['last_analysis_stats'] ?? [];
        $malicious = (int) ($stats['malicious'] ?? 0);
        $suspicious = (int) ($stats['suspicious'] ?? 0);
        $harmless = (int) ($stats['harmless'] ?? 0);
        $undetected = (int) ($stats['undetected'] ?? 0);
        $timeout = (int) ($stats['timeout'] ?? 0);

        $lastDate = $attrs['last_analysis_date'] ?? null;
        $lastAnalysis = $lastDate ? date('Y-m-d H:i:s', (int) $lastDate) : null;

        $redirects = [];
        $chain = $attrs['redirection_chain'] ?? '';
        if (is_string($chain) && $chain !== '') {
            foreach (explode(',', $chain) as $hop) {
                $hop = trim($hop);
                if ($hop !== '') {
                    $redirects[] = $hop;
                }
            }
        }

        $categories = $attrs['categories'] ?? [];
        $categoryValues = [];
        if (is_array($categories)) {
            foreach ($categories as $values) {
                if (is_string($values) && $values !== '') {
                    $categoryValues[] = $values;
                }
            }
        }

        return [
            'url' => $url,
            'final_url' => is_string($attrs['final_url'] ?? null) && $attrs['final_url'] !== ''
                ? $attrs['final_url']
                : $url,
            'malicious' => $malicious,
            'suspicious' => $suspicious,
            'undetected' => $undetected,
            'harmless' => $harmless,
            'total_engine_verdicts' => $malicious + $suspicious + $harmless + $undetected + $timeout,
            'reputation' => (int) ($attrs['reputation'] ?? 0),
            'categories' => array_values(array_unique($categoryValues)),
            'redirect_chain' => $redirects,
            'vt_link' => 'https://www.virustotal.com/gui/url/' . $this->urlId($url),
            'last_analysis' => $lastAnalysis,
            'verdict' => $malicious > 0 ? 'malicious' : ($suspicious > 0 ? 'suspicious' : 'clean'),
        ];
    }

    private function parse(array $result, string $ip): ?array
    {
        if ($result['status'] === 429) {
            throw new RateLimitException('VirusTotal rate limit reached. Please retry later.', 'virustotal');
        }
        if ($result['status'] === 0) {
            throw new UpstreamException('VirusTotal is unreachable or timed out', 'virustotal');
        }
        if ($result['status'] === 404) {
            return null;
        }
        $body = json_decode((string) ($result['raw'] ?? ''), true);
        if (!is_array($body) || !isset($body['data']['attributes'])) {
            throw new UpstreamException('VirusTotal returned an invalid response', 'virustotal');
        }

        $attrs = $body['data']['attributes'];
        $stats = $attrs['last_analysis_stats'] ?? [];
        $malicious = (int) ($stats['malicious'] ?? 0);
        $suspicious = (int) ($stats['suspicious'] ?? 0);
        $harmless = (int) ($stats['harmless'] ?? 0);
        $undetected = (int) ($stats['undetected'] ?? 0);
        $timeout = (int) ($stats['timeout'] ?? 0);

        $lastDate = $attrs['last_analysis_date'] ?? null;
        $lastAnalysis = $lastDate
            ? date('Y-m-d H:i:s', (int) $lastDate)
            : null;

        return [
            'ip' => $ip,
            'malicious' => $malicious,
            'suspicious' => $suspicious,
            'undetected' => $undetected,
            'harmless' => $harmless,
            'total_engine_verdicts' => $malicious + $suspicious + $harmless + $undetected + $timeout,
            'reputation' => (int) ($attrs['reputation'] ?? 0),
            'vt_link' => 'https://www.virustotal.com/gui/ip-address/' . rawurlencode($ip),
            'last_analysis' => $lastAnalysis,
            'verdict' => $malicious > 0 ? 'malicious' : ($suspicious > 0 ? 'suspicious' : 'clean'),
        ];
    }
}