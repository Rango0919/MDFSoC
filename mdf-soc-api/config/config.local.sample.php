<?php

return [

    'app' => [
        'env' => 'production',
        'debug' => false,
        'https_enforce' => true,
        'mock_mode' => false,
    ],

    'auth' => [
        'access_token' => 'CHANGE-ME-to-a-long-random-string',
        'app_secret' => 'CHANGE-ME-to-another-long-random-string',
        'admin_user' => 'soc',
        'admin_password_hash' => 'CHANGE-ME-php-password_hash-output',
        'token_ttl' => 3600,
    ],

    'rate_limit' => [
        'max' => 300,
        'window' => 60,
        'login_max' => 10,
        'login_window' => 60,
    ],

    // MySQL/MariaDB. Create the schema from database/schema.sql first (see README).
    'db' => [
        'enabled' => true,
        'host' => '127.0.0.1',
        'port' => 3306,
        'name' => 'mdfsoc',
        'user' => 'CHANGE-ME',
        'pass' => 'CHANGE-ME',
    ],

    'wazuh' => [
        'enabled' => true,
        'api_url' => 'https://wazuh.example.local:55000',
        'user' => 'mdfsoc',
        'pass' => 'CHANGE-ME',
        'verify_ssl' => true,
        'ca_file' => __DIR__ . '/../certs/wazuh-ca.pem',
    ],

    'abuseipdb' => [
        'enabled' => true,
        'api_key' => 'CHANGE-ME',
    ],

    'virustotal' => [
        'enabled' => true,
        'api_key' => 'CHANGE-ME',
    ],

    'email' => [
        'enabled' => true,
        'smtp_host' => 'smtp.gmail.com',
        'smtp_port' => 587,
        'username' => 'CHANGE-ME@gmail.com',
        'password' => 'CHANGE-ME-gmail-app-password',
        'from_email' => 'CHANGE-ME@gmail.com',
        'to_emails' => ['CHANGE-ME@gmail.com'],
        'notify_high_critical' => true,
        'digest_enabled' => true,
    ],

];