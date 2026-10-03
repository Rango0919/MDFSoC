<?php

final class Auth
{
    public static function tokenFromRequest(): ?string
    {
        $header = $_SERVER['HTTP_AUTHORIZATION'] ?? '';
        if (preg_match('/Bearer\s+(.+)/i', $header, $m)) {
            return trim($m[1]);
        }
        if (isset($_GET['token']) && is_string($_GET['token']) && $_GET['token'] !== '') {
            return $_GET['token'];
        }
        return null;
    }

    public static function token(): string
    {
        $token = self::tokenFromRequest();
        if ($token === null || $token === '') {
            Response::error('auth_error', 'Missing or empty access token', 'backend', 401);
        }
        return $token;
    }

    public static function issuedByUs(string $token): bool
    {
        $master = Config::string('auth.access_token');
        if ($master !== '' && hash_equals($master, $token)) {
            return true;
        }
        return self::verifySignature($token) !== null;
    }

    public static function issue(string $username): array
    {
        $expires = time() + Config::int('auth.token_ttl', 604800);
        $payload = self::base64Url(json_encode([
            'u' => $username,
            'exp' => $expires,
            'n' => bin2hex(random_bytes(8)),
        ], JSON_UNESCAPED_SLASHES));
        $signature = self::base64Url(hash_hmac('sha256', $payload, Config::get('auth.app_secret'), true));
        return [
            'token' => $payload . '.' . $signature,
            'expires_at' => date('c', $expires),
        ];
    }

    public static function verifySignature(string $token): ?array
    {
        $parts = explode('.', $token);
        if (count($parts) !== 2) {
            return null;
        }
        [$payload, $signature] = $parts;
        $expected = self::base64Url(hash_hmac('sha256', $payload, Config::get('auth.app_secret'), true));
        if (!hash_equals($expected, $signature)) {
            return null;
        }
        $decoded = json_decode(self::base64UrlDecode($payload), true);
        if (!is_array($decoded) || !isset($decoded['exp'])) {
            return null;
        }
        if ((int) $decoded['exp'] < time()) {
            return null;
        }
        return $decoded;
    }

    public static function login(string $username, string $password): bool
    {
        if ($username !== '' && hash_equals((string) Config::get('auth.admin_user', ''), $username)) {
            $hash = Config::string('auth.admin_password_hash', '');
            if ($hash !== '' && password_verify($password, $hash)) {
                return true;
            }
        }

        $pdo = Db::pdo();
        if ($pdo !== null) {
            $stmt = $pdo->prepare(
                'SELECT password_hash FROM mdfsoc_users WHERE username = ? AND active = 1 LIMIT 1'
            );
            $stmt->execute([$username]);
            $stored = $stmt->fetchColumn();
            if (is_string($stored) && $stored !== '' && password_verify($password, $stored)) {
                return true;
            }
        }
        return false;
    }

    public static function base64Url(string $value): string
    {
        return rtrim(strtr(base64_encode($value), '+/', '-_'), '=');
    }

    public static function base64UrlDecode(string $value): string
    {
        return base64_decode(strtr($value, '-_', '+/'), true) ?: '';
    }
}