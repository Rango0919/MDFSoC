<?php

return [

    'app' => [
        'env' => 'development',
        'debug' => true,
        'https_enforce' => false,
        // Set mock_mode=false once you have real upstream services configured below.
        'mock_mode' => true,
    ],

    'auth' => [
        // Change these to long random strings before going anywhere near production.
        'access_token' => 'change-me-to-a-long-random-string',
        'app_secret' => 'change-me-to-another-long-random-string',
        'admin_user' => 'soc',
        'admin_password_hash' => '$2y$12$lGt3P3F9BGN5kfh/CJrRgu6x.pd1xu9sxTWamsbnOvvUgysHfNozu',
    ],

    'rate_limit' => [
        'max' => 100000,
        'window' => 60,
        'login_max' => 1000,
        'login_window' => 60,
    ],

    // Local MariaDB (see ~/mariadb on this machine). Created from database/schema.sql
    // with app user mdfsoc. Change the password if you open this beyond localhost.
    'db' => [
        'enabled' => true,
        'host' => '127.0.0.1',
        'port' => 3306,
        'name' => 'mdfsoc',
        'user' => 'mdfsoc',
        'pass' => 'Mdf_S0c_Db!2026',
    ],

    'wazuh' => [
        'enabled' => false,
        'api_url' => 'https://wazuh.example.local:55000',
        'user' => 'mdfsoc',
        'pass' => 'CHANGE-ME',
        'verify_ssl' => true,
        // For self-signed Wazuh certs, point at a pinned CA bundle instead of lowering verify_ssl.
        'ca_file' => __DIR__ . '/../certs/wazuh-ca.pem',
    ],

    'abuseipdb' => [
        'enabled' => true,
        'api_key' => 'aa049e1b173818f9b5b8360bec17ea4bcb8019bc8a1aa56636b10daf333e6a9ef7d0259a38689994',
    ],

    'virustotal' => [
        'enabled' => true,
        'api_key' => 'd84543536fbe5c6a8672cb0f49d5be3338e194c9c54e422fb4e1a956e04048a5',
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