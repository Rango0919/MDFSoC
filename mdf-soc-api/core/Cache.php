<?php

final class Cache
{
    public static function get(string $key, $default = null)
    {
        $pdo = Db::pdo();
        if ($pdo !== null) {
            $stmt = $pdo->prepare('SELECT value, expires_at FROM mdfsoc_cache WHERE cache_key = ?');
            $stmt->execute([$key]);
            $row = $stmt->fetch(PDO::FETCH_ASSOC);
            if (!$row) {
                return $default;
            }
            if ((int) $row['expires_at'] < time()) {
                $delete = $pdo->prepare('DELETE FROM mdfsoc_cache WHERE cache_key = ?');
                $delete->execute([$key]);
                return $default;
            }
            $value = json_decode($row['value'], true);
            return $value === null ? $default : $value;
        }

        $file = self::file($key);
        if (!is_file($file)) {
            return $default;
        }
        $entry = json_decode((string) file_get_contents($file), true);
        if (!is_array($entry) || !isset($entry['expires'], $entry['value'])) {
            return $default;
        }
        if ((int) $entry['expires'] < time()) {
            @unlink($file);
            return $default;
        }
        return $entry['value'];
    }

    public static function set(string $key, $value, int $ttl): void
    {
        $expires = time() + $ttl;

        $pdo = Db::pdo();
        if ($pdo !== null) {
            $stmt = $pdo->prepare(
                'INSERT INTO mdfsoc_cache (cache_key, value, expires_at) VALUES (?, ?, ?)
                 ON DUPLICATE KEY UPDATE value = VALUES(value), expires_at = VALUES(expires_at)'
            );
            $stmt->execute([$key, json_encode($value), $expires]);
            return;
        }

        $dir = Config::string('cache.dir');
        if (!is_dir($dir)) {
            @mkdir($dir, 0750, true);
        }
        file_put_contents(self::file($key), json_encode([
            'expires' => $expires,
            'value' => $value,
        ]), LOCK_EX);
    }

    private static function file(string $key): string
    {
        return rtrim(Config::string('cache.dir'), '/') . '/'
            . hash('sha256', $key) . '.json';
    }
}