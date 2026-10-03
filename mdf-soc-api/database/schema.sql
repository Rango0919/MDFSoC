CREATE DATABASE IF NOT EXISTS mdfsoc
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE mdfsoc;

CREATE TABLE IF NOT EXISTS mdfsoc_users (
    id INT UNSIGNED AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS mdfsoc_alerts (
    id BIGINT UNSIGNED PRIMARY KEY,
    severity VARCHAR(16) NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_ip VARCHAR(64) NULL,
    dest_ip VARCHAR(64) NULL,
    alert_timestamp VARCHAR(64) NULL,
    description TEXT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'active',
    rule_id VARCHAR(32) NULL,
    rule_level INT NULL,
    agent VARCHAR(128) NULL,
    mitre JSON NULL,
    raw JSON NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_severity (severity),
    KEY idx_timestamp (alert_timestamp)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS mdfsoc_cache (
    cache_key VARCHAR(255) PRIMARY KEY,
    value MEDIUMTEXT NOT NULL,
    expires_at INT UNSIGNED NOT NULL,
    KEY idx_expires (expires_at)
) ENGINE=InnoDB;

CREATE TABLE IF NOT EXISTS mdfsoc_rate_limits (
    limit_key VARCHAR(255) PRIMARY KEY,
    bucket VARCHAR(64) NOT NULL,
    identity_key VARCHAR(64) NOT NULL,
    window_start BIGINT NOT NULL,
    count INT UNSIGNED NOT NULL,
    KEY idx_bucket_window (bucket, window_start)
) ENGINE=InnoDB;

INSERT INTO mdfsoc_users (username, password_hash, active) VALUES ('soc', 'REPLACE_WITH_PASSWORD_HASH', 1)
ON DUPLICATE KEY UPDATE username = username;