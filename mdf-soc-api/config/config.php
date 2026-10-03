<?php

return [

    'app' => [
        'name' => 'mdf-soc-api',
        'env' => 'production',
        'debug' => false,
        'https_enforce' => true,
        'mock_mode' => true,
    ],

    'cors' => [
        'allowed_origins' => [],
    ],

    'auth' => [
        'access_token' => '',
        'app_secret' => '',
        'admin_user' => 'soc',
        'admin_password_hash' => '',
        'token_ttl' => 604800,
        'public_endpoints' => ['login.php'],
    ],

    'rate_limit' => [
        'max' => 300,
        'window' => 60,
        'login_max' => 20,
        'login_window' => 600,
    ],

    'cache' => [
        'dir' => __DIR__ . '/../storage/cache',
        'alert_ttl' => 60,
        'token_ttl' => 3540,
        'abuseipdb_ttl' => 3600,
        'threat_intel_ttl' => 1800,
        'mitre_ttl' => 604800,
    ],

    'log' => [
        'dir' => __DIR__ . '/../logs',
    ],

    'db' => [
        'enabled' => false,
        'host' => '127.0.0.1',
        'port' => 3306,
        'name' => 'mdfsoc',
        'user' => 'mdfsoc',
        'pass' => '',
    ],

    'wazuh' => [
        'enabled' => false,
        'api_url' => 'https://wazuh.example.local:55000',
        'user' => 'mdfsoc',
        'pass' => '',
        'alerts_endpoint' => '/security/alerts',
        'verify_ssl' => true,
        'ca_file' => '',
        'timeout' => 25,
    ],

    'abuseipdb' => [
        'enabled' => false,
        'api_key' => '',
        'endpoint' => 'https://api.abuseipdb.com/api/v2/check',
        'timeout' => 20,
        'max_age_days' => 90,
    ],

    'virustotal' => [
        'enabled' => false,
        'api_key' => '',
        'endpoint' => 'https://www.virustotal.com/api/v3',
        'timeout' => 25,
    ],

    'mitre' => [
        'stix_url' => '',
        'local_map_enabled' => true,
    ],

];