<?php

final class Db
{
    private static ?PDO $pdo = null;
    private static bool $initialized = false;

    public static function pdo(): ?PDO
    {
        if (self::$initialized) {
            return self::$pdo;
        }
        self::$initialized = true;

        if (!Config::bool('db.enabled', false)) {
            return null;
        }
        $host = Config::string('db.host', '127.0.0.1');
        $port = Config::int('db.port', 3306);
        $name = Config::string('db.name', 'mdfsoc');
        $user = Config::string('db.user', '');
        $pass = Config::string('db.pass', '');
        $dsn = "mysql:host={$host};port={$port};dbname={$name};charset=utf8mb4";

        try {
            self::$pdo = new PDO($dsn, $user, $pass, [
                PDO::ATTR_ERRMODE => PDO::ERRMODE_EXCEPTION,
                PDO::ATTR_DEFAULT_FETCH_MODE => PDO::FETCH_ASSOC,
                PDO::ATTR_EMULATE_PREPARES => false,
            ]);
        } catch (PDOException $e) {
            Logger::error('Database connection failed', ['message' => $e->getMessage()]);
            self::$pdo = null;
        }
        return self::$pdo;
    }

    public static function enabled(): bool
    {
        return Db::pdo() !== null;
    }
}