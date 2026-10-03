<?php

final class HttpClient
{
    public static function request(string $method, string $url, array $options = []): array
    {
        $timeout = $options['timeout'] ?? 20;
        $headers = $options['headers'] ?? [];
        $body = $options['body'] ?? null;

        $curl = curl_init();
        curl_setopt_array($curl, [
            CURLOPT_URL => $url,
            CURLOPT_RETURNTRANSFER => true,
            CURLOPT_FOLLOWLOCATION => true,
            CURLOPT_MAXREDIRS => 2,
            CURLOPT_CONNECTTIMEOUT => 10,
            CURLOPT_TIMEOUT => $timeout,
            CURLOPT_CUSTOMREQUEST => strtoupper($method),
            CURLOPT_HTTPHEADER => self::headerLines($headers),
        ]);

        if ($body !== null) {
            curl_setopt($curl, CURLOPT_POSTFIELDS, $body);
        }

        if (isset($options['verify_ssl'])) {
            if ($options['verify_ssl']) {
                curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, true);
                curl_setopt($curl, CURLOPT_SSL_VERIFYHOST, 2);
                if (!empty($options['ca_file']) && is_file($options['ca_file'])) {
                    curl_setopt($curl, CURLOPT_CAINFO, $options['ca_file']);
                }
            } else {
                curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, false);
                curl_setopt($curl, CURLOPT_SSL_VERIFYHOST, 0);
            }
        } else {
            curl_setopt($curl, CURLOPT_SSL_VERIFYPEER, true);
            curl_setopt($curl, CURLOPT_SSL_VERIFYHOST, 2);
        }

        $raw = curl_exec($curl);
        $errno = curl_errno($curl);
        $error = curl_error($curl);
        $status = (int) curl_getinfo($curl, CURLINFO_RESPONSE_CODE);
        curl_close($curl);

        if ($raw === false) {
            Logger::error('HTTP request failed', ['url' => $url, 'errno' => $errno, 'error' => $error]);
            return ['ok' => false, 'status' => 0, 'raw' => null, 'error' => $error];
        }

        return [
            'ok' => $status >= 200 && $status < 300,
            'status' => $status,
            'raw' => $raw,
            'error' => null,
        ];
    }

    public static function json(string $method, string $url, array $options = []): array
    {
        $options['headers'][] = 'Accept: application/json';
        $result = self::request($method, $url, $options);
        if ($result['raw'] === null) {
            return $result + ['data' => null];
        }
        $decoded = json_decode($result['raw'], true);
        return $result + ['data' => is_array($decoded) ? $decoded : null];
    }

    private static function headerLines(array $headers): array
    {
        $lines = [];
        foreach ($headers as $name => $value) {
            if (is_int($name)) {
                $lines[] = (string) $value;
            } else {
                $lines[] = $name . ': ' . $value;
            }
        }
        return $lines;
    }
}