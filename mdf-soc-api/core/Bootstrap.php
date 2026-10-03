<?php

final class Bootstrap
{
    public static function run(): void
    {
        self::files();
        Config::init(__DIR__ . '/../config');
        self::errors();
        self::headers();
        self::https();

        $script = basename($_SERVER['SCRIPT_NAME'] ?? '');
        $public = (array) Config::get('auth.public_endpoints', ['login.php']);

        if (!in_array($script, $public, true)) {
            $endpoint = $script !== '' ? $script : 'unknown';
            RateLimiter::hit(
                $endpoint,
                Config::int('rate_limit.max', 300),
                Config::int('rate_limit.window', 60)
            );
            $token = Auth::token();
            if (!Auth::issuedByUs($token)) {
                Response::error('auth_error', 'Invalid or expired access token', 'backend', 401);
            }
        }
    }

    public static function cli(): void
    {
        self::files();
        Config::init(__DIR__ . '/../config');
        self::errors();
    }

    private static function files(): void
    {
        $core = [
            'Config', 'Response', 'Exceptions', 'Auth', 'Cache',
            'RateLimiter', 'Db', 'HttpClient', 'Logger', 'Input', 'MockData', 'Verdict', 'Mailer',
            'UrlAnalyzer',
        ];
        foreach ($core as $class) {
            require_once __DIR__ . '/' . $class . '.php';
        }
        $services = glob(__DIR__ . '/../services/*.php') ?: [];
        foreach ($services as $file) {
            require_once $file;
        }
    }

    private static function errors(): void
    {
        set_error_handler(static function ($severity, $message, $file, $line) {
            if (!(error_reporting() & $severity)) {
                return false;
            }
            Logger::error('PHP error', [
                'severity' => $severity,
                'message' => $message,
                'file' => $file,
                'line' => $line,
            ]);
            return true;
        });

        set_exception_handler(static function (Throwable $e) {
            if ($e instanceof ApiException) {
                Logger::error('Request failed', ['code' => $e->errorCode, 'source' => $e->errorSource, 'message' => $e->getMessage()]);
                Response::error($e->errorCode, $e->getMessage(), $e->errorSource, $e->httpStatus);
            }
            Logger::error('Unhandled exception', [
                'class' => get_class($e),
                'message' => $e->getMessage(),
                'file' => $e->getFile(),
                'line' => $e->getLine(),
            ]);
            Response::error('server_error', 'Internal server error', 'backend', 500);
        });
    }

    private static function headers(): void
    {
        header('X-Content-Type-Options: nosniff');
        header('X-Frame-Options: DENY');
        header('Referrer-Policy: no-referrer');
        header('Permissions-Policy: geolocation=(), microphone=(), camera=()');

        $origins = (array) Config::get('cors.allowed_origins', []);
        if ($origins !== []) {
            $requestOrigin = $_SERVER['HTTP_ORIGIN'] ?? '';
            if (in_array('*', $origins, true) || in_array($requestOrigin, $origins, true)) {
                header('Access-Control-Allow-Origin: ' . ($requestOrigin ?: '*'));
                header('Access-Control-Allow-Methods: GET, POST, OPTIONS');
                header('Access-Control-Allow-Headers: Authorization, Content-Type');
                if (($_SERVER['REQUEST_METHOD'] ?? '') === 'OPTIONS') {
                    http_response_code(204);
                    exit;
                }
            }
        }
    }

    private static function https(): void
    {
        if (!Config::bool('app.https_enforce', true)) {
            return;
        }
        $isHttps = (!empty($_SERVER['HTTPS']) && $_SERVER['HTTPS'] !== 'off')
            || (isset($_SERVER['SERVER_PORT']) && (int) $_SERVER['SERVER_PORT'] === 443);
        if ($isHttps) {
            return;
        }
        if (in_array(Input::clientIp(), ['127.0.0.1', '::1', 'localhost'], true)) {
            return;
        }
        Response::error('https_required', 'HTTPS is required', 'backend', 403);
    }
}