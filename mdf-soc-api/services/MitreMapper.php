<?php

final class MitreMapper
{
    public static function fromRule(array $rule): ?array
    {
        $mitre = $rule['mitre'] ?? null;
        if (!is_array($mitre)) {
            return null;
        }
        $ids = self::extractStrings($mitre['id'] ?? []);
        $tactic = self::extractStrings($mitre['tactic'] ?? []);

        if ($ids !== []) {
            $id = strtoupper((string) $ids[0]);
        } else {
            $techniques = self::extractStrings($mitre['technique'] ?? []);
            if ($techniques === []) {
                return null;
            }
            $technique = $techniques[0];
            if (preg_match('/^T\d{4}$/', $technique)) {
                $id = strtoupper($technique);
            } else {
                return [
                    'technique_id' => null,
                    'technique_name' => $technique,
                    'tactic' => $tactic[0] ?? null,
                    'link' => null,
                ];
            }
        }

        $meta = self::lookup($id);
        return [
            'technique_id' => $id,
            'technique_name' => $meta['name'] ?? $id,
            'tactic' => $tactic[0] ?? ($meta['tactic'] ?? null),
            'link' => 'https://attack.mitre.org/techniques/' . $id . '/',
        ];
    }

    public static function fromRisk(string $severity): ?array
    {
        if (Config::bool('mitre.local_map_enabled', true) === false) {
            return null;
        }
        $map = [
            'malicious' => 'T1078',
            'suspicious' => 'T1110',
            'clean' => null,
        ];
        $id = $map[$severity] ?? null;
        if ($id === null) {
            return null;
        }
        $meta = self::lookup($id);
        return [
            'technique_id' => $id,
            'technique_name' => $meta['name'] ?? $id,
            'tactic' => $meta['tactic'] ?? null,
            'link' => 'https://attack.mitre.org/techniques/' . $id . '/',
        ];
    }

    public static function lookup(string $techniqueId): ?array
    {
        $map = self::localMap();
        return $map[$techniqueId] ?? null;
    }

    private static function extractStrings($value): array
    {
        if ($value === null) {
            return [];
        }
        if (is_string($value)) {
            return [$value];
        }
        if (!is_array($value)) {
            return [];
        }
        $out = [];
        foreach ($value as $entry) {
            if (is_string($entry)) {
                $out[] = $entry;
            } elseif (is_array($entry) && isset($entry['id'])) {
                $out[] = $entry['id'];
            } elseif (is_array($entry) && isset($entry['name'])) {
                $out[] = $entry['name'];
            }
        }
        return $out;
    }

    private static function localMap(): array
    {
        return [
            'T1110' => ['name' => 'Brute Force', 'tactic' => 'Credential Access'],
            'T1078' => ['name' => 'Valid Accounts', 'tactic' => 'Defense Evasion'],
            'T1190' => ['name' => 'Exploit Public-Facing Application', 'tactic' => 'Initial Access'],
            'T1133' => ['name' => 'External Remote Services', 'tactic' => 'Initial Access'],
            'T1566' => ['name' => 'Phishing', 'tactic' => 'Initial Access'],
            'T1566.001' => ['name' => 'Phishing (Spearphishing Attachment)', 'tactic' => 'Initial Access'],
            'T1059' => ['name' => 'Command and Scripting Interpreter', 'tactic' => 'Execution'],
            'T1059.004' => ['name' => 'Command and Scripting Interpreter (Unix Shell)', 'tactic' => 'Execution'],
            'T1204' => ['name' => 'User Execution', 'tactic' => 'Execution'],
            'T1053' => ['name' => 'Scheduled Task/Job', 'tactic' => 'Execution'],
            'T1053.005' => ['name' => 'Scheduled Task/Job (Scheduled Task)', 'tactic' => 'Execution'],
            'T1027' => ['name' => 'Obfuscated Files or Information', 'tactic' => 'Defense Evasion'],
            'T1140' => ['name' => 'Deobfuscate/Decode Files or Information', 'tactic' => 'Defense Evasion'],
            'T1562' => ['name' => 'Impair Defenses', 'tactic' => 'Defense Evasion'],
            'T1003' => ['name' => 'OS Credential Dumping', 'tactic' => 'Credential Access'],
            'T1005' => ['name' => 'Data from Local System', 'tactic' => 'Collection'],
            'T1082' => ['name' => 'System Information Discovery', 'tactic' => 'Discovery'],
            'T1482' => ['name' => 'Domain Trust Discovery', 'tactic' => 'Discovery'],
            'T1090' => ['name' => 'Proxy', 'tactic' => 'Command and Control'],
            'T1105' => ['name' => 'Ingress Tool Transfer', 'tactic' => 'Command and Control'],
            'T1219' => ['name' => 'Remote Access Software', 'tactic' => 'Command and Control'],
            'T1041' => ['name' => 'Exfiltration Over C2 Channel', 'tactic' => 'Exfiltration'],
            'T1048' => ['name' => 'Exfiltration Over Alternative Protocol', 'tactic' => 'Exfiltration'],
        ];
    }
}