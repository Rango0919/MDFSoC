<?php

final class Input
{
    public static function string(string $key, string $default = '', int $maxLength = 1000): string
    {
        $value = $_GET[$key] ?? $_POST[$key] ?? null;
        if ($value === null) {
            return $default;
        }
        $value = (string) $value;
        if (strlen($value) > $maxLength) {
            $value = substr($value, 0, $maxLength);
        }
        return trim($value);
    }

    public static function int(string $key, int $default = 0, int $min = 0, int $max = PHP_INT_MAX): int
    {
        $value = $_GET[$key] ?? $_POST[$key] ?? $default;
        if (!is_numeric($value)) {
            return $default;
        }
        $value = (int) $value;
        return max($min, min($max, $value));
    }

    public static function jsonBody(): array
    {
        $raw = file_get_contents('php://input');
        if ($raw === false || $raw === '') {
            return [];
        }
        $decoded = json_decode($raw, true);
        return is_array($decoded) ? $decoded : [];
    }

    public static function clientIp(): string
    {
        $ip = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
        if (isset($_SERVER['HTTP_X_FORWARDED_FOR'])) {
            $forwarded = explode(',', (string) $_SERVER['HTTP_X_FORWARDED_FOR']);
            $candidate = trim((string) $forwarded[0]);
            if (filter_var($candidate, FILTER_VALIDATE_IP)) {
                $ip = $candidate;
            }
        }
        return (string) $ip;
    }
}