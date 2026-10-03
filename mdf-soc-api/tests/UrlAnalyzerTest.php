<?php

declare(strict_types=1);

require_once __DIR__ . '/../core/UrlAnalyzer.php';

$passed = 0;
$failed = 0;

function check(string $label, bool $condition): void
{
    global $passed, $failed;
    if ($condition) {
        $passed++;
        return;
    }
    $failed++;
    fwrite(STDERR, "FAIL: {$label}\n");
}

function checkEq(string $label, mixed $expected, mixed $actual): void
{
    global $passed, $failed;
    if ($expected === $actual) {
        $passed++;
        return;
    }
    $failed++;
    fwrite(STDERR, "FAIL: {$label}\n  expected: " . var_export($expected, true)
        . "\n  actual:   " . var_export($actual, true) . "\n");
}

$publicIpv4 = ['8.8.8.8', '1.1.1.1', '93.184.216.34', '203.0.114.1', '172.32.0.1', '11.0.0.1'];
foreach ($publicIpv4 as $ip) {
    check("public IPv4 {$ip} allowed", UrlAnalyzer::isPublicIp($ip));
}

$blockedIpv4 = [
    '0.0.0.0', '0.1.2.3', '10.0.0.1', '10.255.255.255', '100.64.0.1', '100.127.255.255',
    '127.0.0.1', '127.1.2.3', '169.254.0.1', '169.254.169.254', '172.16.0.1', '172.31.255.255',
    '192.0.0.1', '192.0.2.5', '192.168.1.1', '198.18.0.1', '198.19.255.255',
    '198.51.100.7', '203.0.113.9', '224.0.0.1', '239.255.255.255', '240.0.0.1', '255.255.255.255',
];
foreach ($blockedIpv4 as $ip) {
    check("blocked IPv4 {$ip}", !UrlAnalyzer::isPublicIp($ip));
}

$publicIpv6 = ['2001:4860:4860::8888', '2606:4700:4700::1111', '2a00:1450:4001:80f::200e'];
foreach ($publicIpv6 as $ip) {
    check("public IPv6 {$ip} allowed", UrlAnalyzer::isPublicIp($ip));
}

$blockedIpv6 = [
    '::', '::1', 'fc00::1', 'fd12:3456:789a::1', 'FD00::1', 'fe80::1', 'febf::1',
    'fec0::1', 'ff00::1', 'ff02::1', '100::1', '2001:db8::1', '64:ff9b::1',
    '::ffff:127.0.0.1', '::ffff:7f00:1', '::FFFF:127.0.0.1', '::ffff:169.254.169.254',
    '::ffff:10.0.0.1', '::ffff:192.168.0.1', '0:0:0:0:0:ffff:127.0.0.1',
];
foreach ($blockedIpv6 as $ip) {
    check("blocked IPv6 {$ip}", !UrlAnalyzer::isPublicIp($ip));
}

check('invalid input rejected', !UrlAnalyzer::isPublicIp('not-an-ip'));
check('empty input rejected', !UrlAnalyzer::isPublicIp(''));

checkEq(
    'hxxps deobfuscation',
    'https://paypa1-secure.com/verify',
    UrlAnalyzer::normalize('hxxps://paypa1-secure[.]com/verify')
);
checkEq(
    'bracketed dot deobfuscation',
    'http://evil.example/a',
    UrlAnalyzer::normalize('hxxp://evil[.]example/a')
);
checkEq(
    'entity deobfuscation',
    'https://evil.example/a',
    UrlAnalyzer::normalize('https&#58;//evil.example/a')
);
checkEq(
    'percent-encoded scheme deobfuscation',
    'http://evil.example/a',
    UrlAnalyzer::normalize('%68%74%74%70%3a%2f%2fevil.example%2fa')
);
checkEq(
    'bare host gets scheme',
    'http://evil.example/a',
    UrlAnalyzer::normalize('evil.example/a')
);

$resolveUrl = new ReflectionMethod(UrlAnalyzer::class, 'resolveUrl');
checkEq(
    'relative path redirect',
    'http://a.example/dir/b',
    $resolveUrl->invoke(null, 'http://a.example/dir/page.html', 'b')
);
checkEq(
    'absolute path redirect',
    'http://a.example/z',
    $resolveUrl->invoke(null, 'http://a.example/dir/page.html', '/z')
);
checkEq(
    'protocol relative redirect',
    'http://b.example/z',
    $resolveUrl->invoke(null, 'http://a.example/dir/page.html', '//b.example/z')
);
checkEq(
    'dot segment redirect',
    'http://a.example/c',
    $resolveUrl->invoke(null, 'http://a.example/dir/page.html', '../c')
);
checkEq(
    'query only redirect',
    'http://a.example/dir/page.html?q=1',
    $resolveUrl->invoke(null, 'http://a.example/dir/page.html', '?q=1')
);
checkEq(
    'port preserved in redirect',
    'http://a.example:8080/z',
    $resolveUrl->invoke(null, 'http://a.example:8080/dir/page.html', '/z')
);

$guardHost = new ReflectionMethod(UrlAnalyzer::class, 'guardHost');
$guard = $guardHost->invoke(null, 'http://127.0.0.1/');
check('guard rejects loopback host', isset($guard['error']));

$pinned = new ReflectionMethod(UrlAnalyzer::class, 'pinnedResolveOption');
checkEq(
    'pins validated ipv4',
    'evil.example:443:93.184.216.34',
    $pinned->invoke(null, 'evil.example', 443, ['93.184.216.34'])
);
checkEq(
    'brackets pinned ipv6',
    'evil.example:80:[2001:4860:4860::8888]',
    $pinned->invoke(null, 'evil.example', 80, ['2001:4860:4860::8888'])
);
checkEq(
    'no pinning for ip literal host',
    null,
    $pinned->invoke(null, '93.184.216.34', 80, ['93.184.216.34'])
);

check('shortener detected', UrlAnalyzer::isShortener('bit.ly'));
check('shortener tld detected', UrlAnalyzer::isShortener('a1.vc'));
check('shortener subdomain is not flagged', !UrlAnalyzer::isShortener('a1.bc.vc'));
check('normal host not shortener', !UrlAnalyzer::isShortener('paypa1-secure.com'));
check('www host not shortener', !UrlAnalyzer::isShortener('www.eicar.org'));
check('government host not shortener', !UrlAnalyzer::isShortener('secure.gov'));
check('subdomain host not shortener', !UrlAnalyzer::isShortener('login.paypa1-secure.com'));

$rejected = [
    'http://127.0.0.1/' => 'loopback',
    'http://169.254.169.254/latest/meta-data/' => 'cloud metadata',
    'http://10.0.0.5/' => 'private rfc1918',
    'http://192.168.1.1/' => 'private rfc1918',
    'http://[::1]/' => 'ipv6 loopback',
    'http://[fd00::1]/' => 'ipv6 unique local',
    'http://[fe80::1]/' => 'ipv6 link local',
    'http://user:pass@example.com/' => 'embedded credentials',
    'ftp://example.com/' => 'unsupported scheme',
    'file:///etc/passwd' => 'unsupported scheme',
];
foreach ($rejected as $url => $label) {
    $result = UrlAnalyzer::resolve($url);
    check("rejects {$label} ({$url})", $result['ok'] === false && $result['error'] !== null);
}

$live = UrlAnalyzer::resolve('http://example.com/');
if ($live['ok']) {
    check('live host has public ips', !empty($live['resolved_ips']));
    check('live ips are public', count(array_filter(
        $live['resolved_ips'],
        static fn (string $ip): bool => !UrlAnalyzer::isPublicIp($ip)
    )) === 0);
    checkEq('live host name', 'example.com', $live['host']);
    check('live status received', $live['final_status'] > 0);
} else {
    fwrite(STDERR, "SKIP: live network check unavailable ({$live['error']})\n");
}

$redirect = UrlAnalyzer::resolve('http://github.com/');
if ($redirect['ok']) {
    check('follows a real redirect', $redirect['redirect_count'] >= 1);
    check('redirect chain has hops', count($redirect['redirect_chain']) >= 1);
    check('final url differs or is same host', is_string($redirect['final_url']));
} else {
    fwrite(STDERR, "SKIP: redirect check unavailable ({$redirect['error']})\n");
}

echo "passed: {$passed}, failed: {$failed}\n";
exit($failed === 0 ? 0 : 1);
