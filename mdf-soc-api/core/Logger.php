<?php

final class Logger
{
    public static function error(string $message, array $context = []): void
    {
        self::write('ERROR', $message, $context);
    }

    public static function warning(string $message, array $context = []): void
    {
        self::write('WARN', $message, $context);
    }

    public static function info(string $message, array $context = []): void
    {
        if (Config::bool('app.debug', false)) {
            self::write('INFO', $message, $context);
        }
    }

    private static function write(string $level, string $message, array $context): void
    {
        $dir = Config::string('log.dir', __DIR__ . '/../logs');
        if (!is_dir($dir)) {
            @mkdir($dir, 0750, true);
        }
        $line = sprintf(
            "[%s] [%s] %s %s%s",
            date('Y-m-d H:i:s'),
            $level,
            $message,
            $context !== [] ? json_encode(self::redact($context), JSON_UNESCAPED_SLASHES | JSON_UNESCAPED_UNICODE) : '',
            PHP_EOL
        );
        @file_put_contents($dir . '/' . date('Y-m-d') . '.log', $line, FILE_APPEND | LOCK_EX);
    }

    public static function redact(array $context): array
    {
        $sensitive = '/\b(password|pass|secret|token|authorization|api[_ -]?key|apikey)\b/i';
        foreach ($context as $key => $value) {
            if (is_array($value)) {
                $context[$key] = self::redact($value);
            } elseif (is_string($value) && preg_match($sensitive, (string) $key)) {
                $context[$key] = '***REDACTED***';
            }
        }
        return $context;
    }
}