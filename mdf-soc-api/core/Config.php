<?php

final class Config
{
    private static ?array $values = null;

    public static function init(string $dir): void
    {
        $base = $dir . '/config.php';
        self::$values = is_file($base) ? require $base : [];
        $local = $dir . '/config.local.php';
        if (function_exists('is_file') && is_file($local)) {
            $override = require $local;
            if (is_array($override)) {
                self::$values = self::deepMerge(self::$values, $override);
            }
        }
        $hasSecret = self::$values['auth']['app_secret'] ?? '';
        if ($hasSecret === '') {
            self::$values['auth']['app_secret'] = self::loadOrCreateSecret($dir);
        }
    }

    private static function loadOrCreateSecret(string $configDir): string
    {
        $file = $configDir . '/../storage/.app_secret';
        if (is_file($file)) {
            $existing = trim((string) file_get_contents($file));
            if ($existing !== '') {
                return $existing;
            }
        }
        $secret = bin2hex(random_bytes(32));
        $storage = dirname($file);
        if (!is_dir($storage)) {
            @mkdir($storage, 0750, true);
        }
        @file_put_contents($file, $secret, LOCK_EX);
        @chmod($file, 0600);
        return $secret;
    }

    public static function get(string $key, $default = null)
    {
        $parts = explode('.', $key);
        $node = self::$values;
        foreach ($parts as $part) {
            if (!is_array($node) || !array_key_exists($part, $node)) {
                return $default;
            }
            $node = $node[$part];
        }
        return $node;
    }

    public static function bool(string $key, bool $default = false): bool
    {
        $value = self::get($key, $default);
        if (is_bool($value)) {
            return $value;
        }
        return in_array($value, ['1', 'true', 'on', 'yes'], true);
    }

    public static function int(string $key, int $default = 0): int
    {
        $value = self::get($key, $default);
        return is_numeric($value) ? (int) $value : $default;
    }

    public static function string(string $key, string $default = ''): string
    {
        $value = self::get($key, $default);
        return is_string($value) ? $value : (string) $value;
    }

    private static function deepMerge(array $base, array $override): array
    {
        foreach ($override as $key => $value) {
            if (is_array($value) && isset($base[$key]) && is_array($base[$key])) {
                $base[$key] = self::deepMerge($base[$key], $value);
            } else {
                $base[$key] = $value;
            }
        }
        return $base;
    }
}