<?php
/**
 * Sends a test email through the configured SMTP relay.
 * Usage: php scripts/send-test-email.php
 */

require __DIR__ . '/../core/Bootstrap.php';
Bootstrap::cli();

$mailer = new Mailer();
if (!$mailer->enabled()) {
    fwrite(STDERR, "Email is not enabled. Fill the 'email' section in config/config.local.php "
        . "with a Gmail address + App Password first.\n");
    exit(1);
}

$n = $mailer->send(
    'MDF SOC test email - ' . date('Y-m-d H:i'),
    '<html><body style="font-family:Arial,sans-serif"><h2>MDF SOC</h2>'
        . '<p>This is a test email. If you can read this, SMTP is configured correctly.</p>'
        . '<p>Sent at ' . date('Y-m-d H:i:s') . '.</p></body></html>'
);

echo $n > 0
    ? "OK - test email sent to " . count($mailer->recipients()) . " recipient(s).\n"
    : "FAILED - no email was sent. See logs for SMTP errors.\n";
exit($n > 0 ? 0 : 1);