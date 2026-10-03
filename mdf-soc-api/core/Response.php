<?php

final class Response
{
    public static function success(array $data, array $warnings = [], int $status = 200): void
    {
        self::emit([
            'success' => true,
            'data' => $data,
            'warnings' => $warnings,
            'error' => null,
        ], $status);
    }

    public static function error(string $code, string $message, string $source = 'backend', int $status = 400): void
    {
        self::emit([
            'success' => false,
            'data' => null,
            'warnings' => [],
            'error' => [
                'code' => $code,
                'message' => $message,
                'source' => $source,
            ],
        ], $status);
    }

    public static function emit(array $payload, int $status): void
    {
        http_response_code($status);
        header('Content-Type: application/json; charset=utf-8');
        header('Cache-Control: no-store');
        echo json_encode($payload, JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE);
        exit;
    }
}