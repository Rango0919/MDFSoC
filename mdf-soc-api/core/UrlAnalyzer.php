<?php

final class UrlAnalyzer
{
    private const MAX_REDIRECTS = 5;
    private const CONNECT_TIMEOUT = 5;
    private const TOTAL_TIMEOUT = 12;
    private const MAX_URL_LENGTH = 2048;
    private const MAX_RESPONSE_BYTES = 8192;
    private const USER_AGENT = 'MDF-SOC-UrlAnalyzer/1.0';

    public static function isValidUrl(string $url): bool
    {
        $parts = parse_url($url);
        if ($parts === false || empty($parts['host'])) {
            return false;
        }
        $scheme = strtolower((string) ($parts['scheme'] ?? ''));
        return in_array($scheme, ['http', 'https'], true);
    }

    public static function normalize(string $url): string
    {
        $url = self::decodeObfuscation(trim($url));
        if (preg_match('#^https?://\S+$#i', $url) !== 1) {
            $url = 'http://' . ltrim($url, '/');
        }
        return $url;
    }

    public static function decodeObfuscation(string $url): string
    {
        $url = trim($url);
        $url = (string) preg_replace_callback(
            '#^(?:%[0-9A-Fa-f]{2}){2,}(?::|%3[Aa])#',
            static fn (array $m): string => rawurldecode($m[0]),
            $url
        );
        $url = (string) preg_replace('#^hxxps://#i', 'https://', $url);
        $url = (string) preg_replace('#^hxxp://#i', 'http://', $url);
        $map = [
            '%2e' => '.', '%2E' => '.', '%2f' => '/', '%2F' => '/',
            '%3a' => ':', '%3A' => ':', '%40' => '@', '%5b' => '[', '%5B' => '[',
            '%5d' => ']', '%5D' => ']', '%3d' => '=', '%3D' => '=',
        ];
        $url = strtr($url, $map);
        $url = html_entity_decode($url, ENT_QUOTES | ENT_HTML5, 'UTF-8');
        $url = str_replace(['&#46;', '&#x2e;', '&#x2E;'], '.', $url);
        $url = preg_replace('#\[[\s.]*(?:\.|dot| Dot)[\s.]*\]#i', '.', (string) $url);
        $url = preg_replace('#\(\s*(?:\.|dot)\s*\)#i', '.', (string) $url);
        $url = preg_replace('#\{(?:\.|dot)\}#i', '.', (string) $url);
        $url = (string) preg_replace(
            '#(?<=[0-9A-Za-z])[\s._-]*dot[\s._-]*(?=[0-9A-Za-z])#i',
            '.',
            $url
        );
        return trim((string) $url);
    }

    public static function resolve(string $url): array
    {
        if (strlen($url) > self::MAX_URL_LENGTH) {
            return self::failure('URL exceeds the maximum supported length');
        }
        if (!self::isValidUrl($url)) {
            return self::failure('Only absolute http and https URLs can be analysed');
        }
        if (preg_match('#^[a-z][a-z0-9+.-]*://[^/@]*@#i', $url) === 1) {
            return self::failure('URLs containing embedded credentials are rejected');
        }

        $chain = [];
        $seen = [];
        $current = $url;
        $finalUrl = $url;
        $finalStatus = 0;

        for ($hop = 0; $hop <= self::MAX_REDIRECTS; $hop++) {
            if (isset($seen[$current])) {
                return self::failure('Redirect loop detected', $chain);
            }
            $seen[$current] = true;

            $guard = self::guardHost($current);
            if (isset($guard['error'])) {
                return self::failure($guard['error'], $chain);
            }
            $host = $guard['host'];
            $ips = $guard['ips'];

            $response = self::fetch($current, $ips);

            if ($response['location'] !== null) {
                $chain[] = [
                    'url' => $current,
                    'host' => $host,
                    'resolved_ips' => $ips,
                    'status' => $response['status'],
                ];
                if ($hop === self::MAX_REDIRECTS) {
                    return self::failure('Too many redirects', $chain);
                }
                $next = self::resolveUrl($current, $response['location']);
                if (!self::isValidUrl($next)) {
                    return self::failure('Redirected to an unsupported URL', $chain);
                }
                $current = $next;
                continue;
            }

            $finalStatus = $response['status'];
            $finalUrl = $current;
            $finalHost = $host;
            $finalIps = $ips;
            break;
        }

        if (!isset($finalHost)) {
            return self::failure('Unable to resolve the final URL', $chain);
        }

        return [
            'ok' => true,
            'error' => null,
            'requested_url' => $url,
            'final_url' => $finalUrl,
            'host' => $finalHost,
            'resolved_ips' => $finalIps,
            'redirect_chain' => $chain,
            'redirect_count' => count($chain),
            'final_status' => $finalStatus,
            'shortened' => $chain !== [] && self::isShortener(self::hostOf($url)),
            'reachable' => $finalStatus > 0,
        ];
    }

    public static function isPublicIp(string $ip): bool
    {
        if (filter_var($ip, FILTER_VALIDATE_IP) === false) {
            return false;
        }
        if (str_contains($ip, ':')) {
            $packed = @inet_pton($ip);
            if ($packed === false || strlen($packed) !== 16) {
                return false;
            }
            if (substr($packed, 10, 2) === "\xff\xff") {
                $mapped = inet_ntop(substr($packed, 12));
                return $mapped !== false && self::isPublicIp($mapped);
            }
            $blockedV6 = [
                ['::', 128], ['::1', 128], ['fc00::', 7], ['fe80::', 10],
                ['fec0::', 10], ['ff00::', 8], ['100::', 64], ['2001:db8::', 32],
                ['64:ff9b::', 96],
            ];
            foreach ($blockedV6 as [$network, $bits]) {
                if (self::ipv6Matches($packed, (string) @inet_pton($network), $bits)) {
                    return false;
                }
            }
            return true;
        }

        $long = ip2long($ip);
        if ($long === false) {
            return false;
        }
        $blocked = [
            ['0.0.0.0', 8], ['10.0.0.0', 8], ['100.64.0.0', 10], ['127.0.0.0', 8],
            ['169.254.0.0', 16], ['172.16.0.0', 12], ['192.0.0.0', 24],
            ['192.0.2.0', 24], ['192.168.0.0', 16], ['198.18.0.0', 15],
            ['198.51.100.0', 24], ['203.0.113.0', 24], ['224.0.0.0', 4],
            ['240.0.0.0', 4],
        ];
        foreach ($blocked as [$network, $bits]) {
            $mask = -1 << (32 - $bits);
            if ((ip2long($network) & $mask) === ($long & $mask)) {
                return false;
            }
        }
        return true;
    }

    public static function isShortener(string $host): bool
    {
        $host = strtolower(trim($host));
        $known = [
            'bit.ly', 'tinyurl.com', 'goo.gl', 't.co', 'ow.ly', 'is.gd', 'buff.ly',
            'adf.ly', 'shorte.st', 'cutt.ly', 'rebrand.ly', 't.ly', 'shorturl.at',
            'tiny.cc', 'rb.gy', 's.id', 'bl.ink', 'lnkd.in', 'trib.al',
        ];
        if (in_array($host, $known, true)) {
            return true;
        }
        $parts = explode('.', $host);
        $count = count($parts);
        if ($count !== 2) {
            return false;
        }
        $generic = ['com', 'org', 'net', 'io', 'co', 'gov', 'edu', 'mil', 'int', 'info', 'biz'];
        return strlen($parts[0]) <= 6
            && !in_array($parts[1], $generic, true)
            && strlen($parts[1]) <= 4;
    }

    public static function hostOf(string $url): string
    {
        $host = parse_url($url, PHP_URL_HOST);
        return is_string($host) ? strtolower($host) : '';
    }

    private static function guardHost(string $url): array
    {
        $host = self::hostOf($url);
        if ($host === '') {
            return ['error' => 'URL has no host'];
        }

        if (filter_var($host, FILTER_VALIDATE_IP) !== false) {
            $ips = [$host];
        } else {
            $ips = self::resolveHost($host);
            if ($ips === []) {
                return ['error' => 'Could not resolve host ' . $host];
            }
        }

        foreach ($ips as $ip) {
            if (!self::isPublicIp($ip)) {
                return [
                    'error' => 'Refusing to fetch a non-public address (' . $ip . ')',
                    'host' => $host,
                    'ips' => $ips,
                ];
            }
        }

        return ['host' => $host, 'ips' => $ips];
    }

    private static function resolveHost(string $host): array
    {
        $ips = [];

        $ipv4 = @gethostbynamel($host);
        if (is_array($ipv4)) {
            $ips = $ipv4;
        }

        $records = @dns_get_record($host, DNS_AAAA);
        if (is_array($records)) {
            foreach ($records as $record) {
                if (!empty($record['ipv6'])) {
                    $ips[] = $record['ipv6'];
                }
            }
        }

        return array_values(array_unique($ips));
    }

    private static function fetch(string $url, array $validatedIps): array
    {
        $parts = parse_url($url);
        $host = strtolower((string) ($parts['host'] ?? ''));
        $scheme = strtolower((string) ($parts['scheme'] ?? 'http'));
        $port = isset($parts['port']) ? (int) $parts['port'] : ($scheme === 'https' ? 443 : 80);
        $pinned = self::pinnedResolveOption($host, $port, $validatedIps);

        $body = '';
        $truncated = false;
        $maxBytes = self::MAX_RESPONSE_BYTES;

        $curl = curl_init();
        $options = [
            CURLOPT_URL => $url,
            CURLOPT_FOLLOWLOCATION => false,
            CURLOPT_HEADER => true,
            CURLOPT_NOBODY => false,
            CURLOPT_CONNECTTIMEOUT => self::CONNECT_TIMEOUT,
            CURLOPT_TIMEOUT => self::TOTAL_TIMEOUT,
            CURLOPT_USERAGENT => self::USER_AGENT,
            CURLOPT_PROTOCOLS => CURLPROTO_HTTP | CURLPROTO_HTTPS,
            CURLOPT_REDIR_PROTOCOLS => CURLPROTO_HTTP | CURLPROTO_HTTPS,
            CURLOPT_SSL_VERIFYPEER => true,
            CURLOPT_SSL_VERIFYHOST => 2,
            CURLOPT_WRITEFUNCTION => function ($handle, $chunk) use (&$body, &$truncated, $maxBytes) {
                $remaining = $maxBytes - strlen($body);
                if ($remaining <= 0) {
                    $truncated = true;
                    return 0;
                }
                if (strlen($chunk) > $remaining) {
                    $body .= substr($chunk, 0, $remaining);
                    $truncated = true;
                    return 0;
                }
                $body .= $chunk;
                return strlen($chunk);
            },
        ];
        if ($pinned !== null) {
            $options[CURLOPT_RESOLVE] = [$pinned];
        }
        curl_setopt_array($curl, $options);

        $raw = curl_exec($curl);
        $status = (int) curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
        $headerSize = (int) curl_getinfo($curl, CURLINFO_HEADER_SIZE);
        $error = curl_error($curl);

        $location = null;
        if ($status >= 300 && $status < 400 && $headerSize > 0) {
            $headers = substr($body, 0, $headerSize);
            if (preg_match('/^location:\s*(.+)$/im', $headers, $matches) === 1) {
                $location = trim($matches[1]);
            }
        }

        return [
            'status' => $status,
            'location' => $location,
            'error' => $truncated && $status > 0 ? null : $error,
            'truncated' => $truncated,
        ];
    }

    private static function pinnedResolveOption(string $host, int $port, array $validatedIps): ?string
    {
        if ($host === '' || $validatedIps === []) {
            return null;
        }
        if (filter_var($host, FILTER_VALIDATE_IP) !== false) {
            return null;
        }
        $ip = (string) $validatedIps[0];
        if (filter_var($ip, FILTER_VALIDATE_IP) === false) {
            return null;
        }
        if (str_contains($ip, ':')) {
            $ip = '[' . $ip . ']';
        }
        return $host . ':' . $port . ':' . $ip;
    }

    private static function resolveUrl(string $base, string $relative): string
    {
        $relative = trim($relative);
        if ($relative === '') {
            return $base;
        }
        if (preg_match('#^[a-z][a-z0-9+.-]*:#i', $relative) === 1) {
            return $relative;
        }
        if (str_starts_with($relative, '//')) {
            $scheme = parse_url($base, PHP_URL_SCHEME) ?: 'http';
            return $scheme . ':' . $relative;
        }

        $parts = parse_url($base);
        $scheme = $parts['scheme'] ?? 'http';
        $authority = $parts['host'] ?? '';
        if (!empty($parts['port'])) {
            $authority .= ':' . $parts['port'];
        }
        if (str_starts_with($relative, '/')) {
            return $scheme . '://' . $authority . self::normalizePath($relative);
        }
        if (str_starts_with($relative, '?')) {
            $path = $parts['path'] ?? '/';
            return $scheme . '://' . $authority . $path . $relative;
        }
        if (str_starts_with($relative, '#')) {
            $path = $parts['path'] ?? '/';
            return $scheme . '://' . $authority . $path . $relative;
        }

        $path = $parts['path'] ?? '/';
        $slash = strrpos($path, '/');
        $dir = $slash === false ? '/' : substr($path, 0, $slash + 1);
        if ($dir === '') {
            $dir = '/';
        }
        return $scheme . '://' . $authority . self::normalizePath($dir . $relative);
    }

    private static function normalizePath(string $path): string
    {
        $segments = [];
        foreach (explode('/', $path) as $index => $segment) {
            if ($segment === '.') {
                continue;
            }
            if ($segment === '..') {
                array_pop($segments);
                continue;
            }
            $segments[] = $segment;
        }
        $normalized = implode('/', $segments);
        if ($normalized === '' || $normalized[0] !== '/') {
            $normalized = '/' . ltrim($normalized, '/');
        }
        return $normalized;
    }

    private static function ipv6Matches(string $packed, string $network, int $bits): bool
    {
        if (strlen($network) !== 16 || $bits <= 0 || $bits > 128) {
            return false;
        }
        $bytes = intdiv($bits, 8);
        $remainder = $bits % 8;
        if ($bytes > 0 && strncmp($packed, $network, $bytes) !== 0) {
            return false;
        }
        if ($remainder === 0) {
            return true;
        }
        $mask = (~((1 << (8 - $remainder)) - 1)) & 0xff;
        return (ord($packed[$bytes]) & $mask) === (ord($network[$bytes]) & $mask);
    }

    private static function failure(string $message, array $chain = []): array
    {
        return [
            'ok' => false,
            'error' => $message,
            'requested_url' => null,
            'final_url' => null,
            'host' => null,
            'resolved_ips' => [],
            'redirect_chain' => $chain,
            'redirect_count' => count($chain),
            'final_status' => 0,
            'shortened' => false,
            'reachable' => false,
        ];
    }
}