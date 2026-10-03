<?php

final class RateLimiter
{
    public static function hit(string $bucket, int $max, int $window): void
    {
        $identity = $_SERVER['REMOTE_ADDR'] ?? 'unknown';
        $windowStart = intdiv(time(), $window) * $window;
        $key = 'rl:' . $bucket . ':' . $identity . ':' . $windowStart;

        $pdo = Db::pdo();
        if ($pdo !== null) {
            $upsert = $pdo->prepare(
                'INSERT INTO mdfsoc_rate_limits (limit_key, bucket, identity_key, window_start, count)
                 VALUES (?, ?, ?, ?, 1)
                 ON DUPLICATE KEY UPDATE count = count + 1'
            );
            $upsert->execute([$key, $bucket, $identity, $windowStart]);
            $select = $pdo->prepare('SELECT count FROM mdfsoc_rate_limits WHERE limit_key = ?');
            $select->execute([$key]);
            $count = (int) $select->fetchColumn();
        } else {
            $dir = rtrim(Config::string('cache.dir'), '/') . '/ratelimit';
            if (!is_dir($dir)) {
                @mkdir($dir, 0750, true);
            }
            $file = $dir . '/' . hash('sha256', $key) . '.json';
            $count = 1;
            if (is_file($file)) {
                $stored = json_decode((string) file_get_contents($file), true);
                if (is_array($stored) && isset($stored['window_start']) && (int) $stored['window_start'] === $windowStart) {
                    $count = (int) $stored['count'] + 1;
                }
            }
            file_put_contents($file, json_encode(['window_start' => $windowStart, 'count' => $count]), LOCK_EX);
        }

        if ($count > $max) {
            Logger::warning('Rate limit exceeded', ['bucket' => $bucket, 'identity' => $identity]);
            Response::error('rate_limited', 'Rate limit exceeded. Please retry later.', $bucket, 429);
        }
    }
}