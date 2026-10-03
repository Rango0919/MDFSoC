<?php

final class Mailer
{
    private array $cfg;

    public function __construct()
    {
        $this->cfg = (array) Config::get('email', []);
    }

    public function enabled(): bool
    {
        $user = (string) ($this->cfg['username'] ?? '');
        $pass = (string) ($this->cfg['password'] ?? '');
        return (bool) ($this->cfg['enabled'] ?? false)
            && $user !== '' && $pass !== ''
            && !str_contains($user, 'CHANGE-ME')
            && !str_contains($pass, 'CHANGE-ME')
            && $this->recipients() !== [];
    }

    public function recipients(): array
    {
        $list = (array) ($this->cfg['to_emails'] ?? []);
        return array_values(array_filter(array_map('trim', $list), static fn ($e) => $e !== ''));
    }

    public function notifyCriticalHigh(): bool
    {
        return (bool) ($this->cfg['notify_high_critical'] ?? true);
    }

    public function digestEnabled(): bool
    {
        return (bool) ($this->cfg['digest_enabled'] ?? true);
    }

    /** Sends an HTML email to every configured recipient. Returns number of sends that succeeded. */
    public function send(string $subject, string $htmlBody, ?string $textBody = null): int
    {
        if (!$this->enabled()) {
            return 0;
        }
        $host = (string) ($this->cfg['smtp_host'] ?? 'smtp.gmail.com');
        $port = (int) ($this->cfg['smtp_port'] ?? 587);
        $user = (string) $this->cfg['username'];
        $pass = (string) $this->cfg['password'];
        $from = (string) ($this->cfg['from_email'] ?? $user);
        if ($textBody === null) {
            $textBody = self::htmlToText($htmlBody);
        }
        $sent = 0;
        foreach ($this->recipients() as $to) {
            if ($this->smtpSend($host, $port, $user, $pass, $from, $to, $subject, $textBody, $htmlBody)) {
                $sent++;
            } else {
                Logger::error('Email send failed', ['to' => $to, 'subject' => $subject]);
            }
        }
        return $sent;
    }

    private function smtpSend(
        string $host,
        int $port,
        string $user,
        string $pass,
        string $from,
        string $to,
        string $subject,
        string $textBody,
        string $htmlBody
    ): bool {
        $socket = @fsockopen($host, $port, $errno, $errstr, 15);
        if ($socket === false) {
            Logger::error('SMTP connect failed', ['host' => $host, 'port' => $port, 'error' => $errstr, 'errno' => $errno]);
            return false;
        }
        stream_set_timeout($socket, 30);

        $steps = [
            "EHLO mdfsoc\r\n" => [220],
            "STARTTLS\r\n" => [220],
        ];
        foreach ($steps as $cmd => $expect) {
            if (!self::expect($socket, fgets($socket, 512), $expect)) {
                fclose($socket);
                return false;
            }
            if ($cmd === "STARTTLS\r\n") {
                $ok = @stream_socket_enable_crypto($socket, true, STREAM_CRYPTO_METHOD_TLS_CLIENT);
                if (!$ok) {
                    Logger::error('SMTP STARTTLS failed', ['host' => $host]);
                    fclose($socket);
                    return false;
                }
            }
        }

        if (!self::expect($socket, fgets($socket, 512), [250])) { // second EHLO over TLS
            fclose($socket);
            return false;
        }

        // AUTH LOGIN
        fwrite($socket, "AUTH LOGIN\r\n");
        $reply = fgets($socket, 512);
        if (!preg_match('/^3/', (string) $reply)) {
            Logger::error('SMTP auth not offered', ['reply' => trim((string) $reply)]);
            fclose($socket);
            return false;
        }
        fwrite($socket, base64_encode($user) . "\r\n");
        $reply = fgets($socket, 512);
        if (!preg_match('/^3/', (string) $reply)) {
            Logger::error('SMTP username rejected', ['reply' => trim((string) $reply)]);
            fclose($socket);
            return false;
        }
        fwrite($socket, base64_encode($pass) . "\r\n");
        if (!self::expect($socket, fgets($socket, 512), [235])) {
            fclose($socket);
            return false;
        }

        fwrite($socket, "MAIL FROM:<$from>\r\n");
        if (!self::expect($socket, fgets($socket, 512), [250])) {
            fclose($socket);
            return false;
        }
        fwrite($socket, "RCPT TO:<$to>\r\n");
        if (!self::expect($socket, fgets($socket, 512), [250])) {
            fclose($socket);
            return false;
        }
        fwrite($socket, "DATA\r\n");
        if (!self::expect($socket, fgets($socket, 512), [354])) {
            fclose($socket);
            return false;
        }

        $boundary = 'mdfsoc-' . bin2hex(random_bytes(8));
        $headers = [
            'From: ' . self::headerValue('MDF SOC', $from),
            'To: <' . $to . '>',
            'Subject: ' . self::encodeSubject($subject),
            'Date: ' . date('r'),
            'Message-ID: <' . bin2hex(random_bytes(12)) . '@mdfsoc>',
            'MIME-Version: 1.0',
            'Content-Type: multipart/alternative; boundary="' . $boundary . '"',
        ];
        $body = "--$boundary\r\n"
            . "Content-Type: text/plain; charset=UTF-8\r\n"
            . "Content-Transfer-Encoding: base64\r\n\r\n"
            . chunk_split(base64_encode($textBody))
            . "--$boundary\r\n"
            . "Content-Type: text/html; charset=UTF-8\r\n"
            . "Content-Transfer-Encoding: base64\r\n\r\n"
            . chunk_split(base64_encode($htmlBody))
            . "--$boundary--\r\n";

        fwrite($socket, implode("\r\n", $headers) . "\r\n\r\n" . $body . "\r\n.\r\n");
        $ok = self::expect($socket, fgets($socket, 512), [250]);
        fwrite($socket, "QUIT\r\n");
        fclose($socket);
        return $ok;
    }

    private static function expect($socket, $reply, array $codes): bool
    {
        if ($reply === false || $reply === '') {
            return false;
        }
        foreach ($codes as $code) {
            if (str_starts_with($reply, (string) $code)) {
                return true;
            }
        }
        Logger::error('SMTP unexpected reply', ['reply' => trim($reply)]);
        return false;
    }

    private static function htmlToText(string $html): string
    {
        $text = preg_replace('/<br\s*\/?>/i', "\n", $html);
        $text = preg_replace('/<\/p>/i', "\n", (string) $text);
        $text = preg_replace('/<\/(td|tr|li|h[1-6]|div)>/i', "\n", (string) $text);
        return trim((string) preg_replace('/[ \t]+/', ' ', (string) preg_replace('/\n\s*\n+/', "\n", (string) strip_tags((string) $text))));
    }

    private static function encodeSubject(string $subject): string
    {
        if (preg_match('/[^\x20-\x7E]/', $subject)) {
            return '=?UTF-8?B?' . base64_encode($subject) . '?=';
        }
        return $subject;
    }

    private static function headerValue(string $name, string $email): string
    {
        return '=?UTF-8?B?' . base64_encode($name) . '?= <' . $email . '>';
    }
}