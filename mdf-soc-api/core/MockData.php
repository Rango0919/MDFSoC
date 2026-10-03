<?php

final class MockData
{
    public static function enabled(): bool
    {
        return Config::bool('app.mock_mode', true);
    }

    public static function dashboard(): array
    {
        return [
            'total_alerts' => 247,
            'critical_alerts' => 12,
            'high_alerts' => 38,
            'medium_alerts' => 91,
            'low_alerts' => 106,
            'recent_alerts' => [
                self::recentAlert(125, 'critical', 'Brute Force Attack', '192.168.1.45', '2026-09-18 09:15:22', 'active'),
                self::recentAlert(124, 'high', 'SSH New User Connecting', '10.0.0.23', '2026-09-18 08:58:10', 'active'),
                self::recentAlert(123, 'medium', 'Suspicious Network Traffic', '172.16.4.9', '2026-09-18 08:31:47', 'in_progress'),
                self::recentAlert(122, 'critical', 'Malware Detected', '192.168.1.100', '2026-09-18 07:45:03', 'active'),
                self::recentAlert(121, 'low', 'Authentication Failure', '203.0.113.7', '2026-09-18 06:12:55', 'closed'),
            ],
        ];
    }

    public static function alertsPages(): array
    {
        $alerts = [
            self::alert(125, 'critical', 'Brute Force Attack', '192.168.1.45', '10.0.0.1', '2026-09-18 09:15:22', 'active', '5710', 10, 'agent-01', 'Multiple failed SSH login attempts detected in a short window.', 'T1110', 'Brute Force', 'Credential Access'),
            self::alert(124, 'high', 'SSH New User Connecting', '10.0.0.23', '10.0.0.1', '2026-09-18 08:58:10', 'active', '5753', 7, 'agent-01', 'A new user has successfully connected over SSH.', 'T1078', 'Valid Accounts', 'Defense Evasion'),
            self::alert(123, 'medium', 'Suspicious Network Traffic', '172.16.4.9', '10.0.0.1', '2026-09-18 08:31:47', 'in_progress', '5918', 6, 'agent-02', 'Traffic to a known command and control sinkhole detected.', 'T1090', 'Proxy', 'Command and Control'),
            self::alert(122, 'critical', 'Malware Detected', '192.168.1.100', '10.0.0.1', '2026-09-18 07:45:03', 'active', '87103', 15, 'agent-03', 'Signature match against known malware family.', 'T1204', 'User Execution', 'Execution'),
            self::alert(121, 'low', 'Authentication Failure', '203.0.113.7', '10.0.0.1', '2026-09-18 06:12:55', 'closed', '5501', 3, 'agent-01', 'A single authentication failure was recorded.', '', '', ''),
        ];
        return $alerts;
    }

    public static function alertDetail(int $id): ?array
    {
        foreach (self::alertsPages() as $alert) {
            if ((int) $alert['id'] === $id) {
                return $alert;
            }
        }
        return null;
    }

    public static function ipReputation(): array
    {
        return [
            'ip' => '8.8.8.8',
            'is_public' => true,
            'abuse_confidence_score' => 0,
            'total_reports' => 0,
            'country_code' => 'US',
            'country_name' => 'United States',
            'isp' => 'Google LLC',
            'domain' => 'dns.google',
            'usage_type' => 'Data Center/Web Hosting/Transit',
            'is_whitelisted' => true,
            'last_reported_at' => null,
            'num_distinct_users' => 0,
        ];
    }

    public static function ipReputationMalicious(string $ip): array
    {
        return [
            'ip' => $ip,
            'is_public' => true,
            'abuse_confidence_score' => 67,
            'total_reports' => 12,
            'country_code' => 'CN',
            'country_name' => 'China',
            'isp' => 'China Telecom',
            'domain' => 'chinatelecom-hn.cn',
            'usage_type' => 'Fixed Line ISP',
            'is_whitelisted' => false,
            'last_reported_at' => '2026-09-12T04:22:00+00:00',
            'num_distinct_users' => 8,
        ];
    }

    public static function threatIntelligence(string $ip): array
    {
        return [
            'ip' => $ip,
            'threat_status' => 'suspicious',
            'abuseipdb' => self::ipReputationMalicious($ip),
            'virustotal' => [
                'ip' => $ip,
                'malicious' => 1,
                'suspicious' => 2,
                'undetected' => 12,
                'harmless' => 22,
                'total_engine_verdicts' => 37,
                'reputation' => -4,
                'vt_link' => 'https://www.virustotal.com/gui/ip-address/' . $ip,
                'last_analysis' => '2026-09-17 22:14:00',
                'verdict' => 'clean',
            ],
            'mitre' => [
                'technique_id' => 'T1078',
                'technique_name' => 'Valid Accounts',
                'tactic' => 'Defense Evasion',
                'link' => 'https://attack.mitre.org/techniques/T1078/',
            ],
            'sources' => ['abuseipdb', 'virustotal', 'mitre'],
        ];
    }

    private static function recentAlert(int $id, string $severity, string $title, string $sourceIp, string $timestamp, string $status): array
    {
        return [
            'id' => $id,
            'severity' => $severity,
            'title' => $title,
            'source_ip' => $sourceIp,
            'timestamp' => $timestamp,
            'status' => $status,
        ];
    }

    private static function alert(int $id, string $severity, string $title, string $sourceIp, string $destIp, string $timestamp, string $status, string $ruleId, int $level, string $agent, string $description, string $techniqueId, string $techniqueName, string $tactic): array
    {
        return [
            'id' => $id,
            'severity' => $severity,
            'title' => $title,
            'source_ip' => $sourceIp,
            'dest_ip' => $destIp,
            'timestamp' => $timestamp,
            'description' => $description,
            'status' => $status,
            'rule_id' => $ruleId,
            'rule_level' => $level,
            'agent' => $agent,
            'investigation_status' => $status,
            'mitre' => $techniqueId !== ''
                ? [
                    'technique_id' => $techniqueId,
                    'technique_name' => $techniqueName,
                    'tactic' => $tactic,
                    'link' => 'https://attack.mitre.org/techniques/' . $techniqueId . '/',
                ]
                : null,
        ];
    }
}