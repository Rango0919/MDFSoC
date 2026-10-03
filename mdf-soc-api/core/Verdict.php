<?php

final class Verdict
{
    public static function threatRisk(?array $abuse, ?array $vt): string
    {
        $score = (int) ($abuse['abuse_confidence_score'] ?? 0);
        $malicious = (int) ($vt['malicious'] ?? 0);
        $suspicious = (int) ($vt['suspicious'] ?? 0);
        $verdict = $vt['verdict'] ?? null;

        if ($malicious > 0 || $score >= 40) {
            return 'malicious';
        }
        if ($suspicious > 0 || $score >= 25) {
            return 'suspicious';
        }
        if ($abuse === null && $vt === null) {
            return 'unknown';
        }
        if (($verdict === 'clean' || $malicious === 0) && $score < 25) {
            return 'clean';
        }
        return 'unknown';
    }
}