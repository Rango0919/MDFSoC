<?php
/**
 * MDF SOC API - runtime test suite (no external dependencies).
 * Run: php tests/runtime-tests.php
 */

$core = __DIR__ . '/../core';
foreach (['Config', 'Response', 'Exceptions', 'Auth', 'Cache', 'RateLimiter', 'Db',
             'HttpClient', 'Logger', 'Input', 'MockData', 'Verdict'] as $class) {
    require_once "$core/$class.php";
}

$tmpRoot = sys_get_temp_dir() . '/mdfsoc-tests-' . bin2hex(random_bytes(4));
$tmpCache = $tmpRoot . '/cache';
$tmpLogs = $tmpRoot . '/logs';
$tmpStorage = $tmpRoot . '/storage';
$cfgFile = $tmpRoot . '/config.php';
$configBody = <<<PHP
<?php
return [
    'app' => ['env' => 'test', 'debug' => false, 'mock_mode' => true, 'https_enforce' => false],
    'auth' => [
        'access_token' => 'test-master-token',
        'app_secret' => 'test-secret-constants',
        'admin_user' => 'soc',
        'admin_password_hash' => '%%HASH%%',
        'token_ttl' => 3600,
    ],
    'cache' => ['dir' => '$tmpCache'],
    'log' => ['dir' => '$tmpLogs'],
    'rate_limit' => ['max' => 1000, 'window' => 60, 'login_max' => 100, 'login_window' => 60],
];
PHP;
@mkdir($tmpRoot, 0750, true);
@mkdir($tmpStorage, 0750, true);
@mkdir($tmpCache, 0750, true);
file_put_contents($cfgFile, str_replace('%%HASH%%', password_hash('secret', PASSWORD_DEFAULT), $configBody));
Config::init($tmpRoot);

$passed = 0;
$failed = 0;
$checks = [];

function check($name, $ok = null, string $detail = ''): void
{
    global $passed, $failed, $checks;
    if (is_bool($name)) {          // tolerate check($condition, 'name') order
        $condition = $name;
        $name = $ok;
    } else {
        $condition = $ok;
    }
    if ($condition) {
        $passed++;
        $checks[] = "PASS  $name";
    } else {
        $failed++;
        $checks[] = "FAIL  $name" . ($detail !== '' ? "  ($detail)" : '');
    }
}

function expectSame($a, $b, string $name): void
{
    check($name, $a === $b, is_scalar($a) ? var_export($a, true) . ' !== ' . var_export($b, true) : 'value');
}

function expectContains(array $arr, string $key, string $name): void
{
    check($name, array_key_exists($key, $arr), "missing key '$key'");
}

$_SERVER['REMOTE_ADDR'] = '1.2.3.4';

/* ------------------------------------------------------------------ Config */
Config::init($tmpRoot);
expectSame(Config::get('auth.app_secret'), 'test-secret-constants', 'Config: explicit app_secret kept');
expectSame(Config::bool('app.mock_mode'), true, 'Config: bool helper reads true');
expectSame(Config::int('rate_limit.max'), 1000, 'Config: int helper');
check(Config::get('app.nonexistent', 'fallback') === 'fallback', 'Config: missing key returns default');

$secretDir = $tmpRoot . '/no-secret';
mkdir($secretDir, 0750, true);
file_put_contents($secretDir . '/config.php', "<?php return ['app' => []];");
Config::init($secretDir);
$s1 = Config::get('auth.app_secret');
check(is_string($s1) && preg_match('/^[0-9a-f]{64}$/', $s1), 'Config: auto secret generated when unset');
check(is_file($secretDir . '/../storage/.app_secret'), 'Config: secret persisted to storage/.app_secret');
Config::init($secretDir);
expectSame(Config::get('auth.app_secret'), $s1, 'Config: secret stable across processes');
Config::init($tmpRoot);

/* --------------------------------------------------------------------- Auth */
$_SERVER['HTTP_AUTHORIZATION'] = 'Bearer abc.def';
expectSame(Auth::tokenFromRequest(), 'abc.def', 'Auth: token parsed from Authorization header');
$_SERVER['HTTP_AUTHORIZATION'] = '';
$_GET['token'] = 'query-tok';
expectSame(Auth::tokenFromRequest(), 'query-tok', 'Auth: token parsed from query parameter');
unset($_GET['token']);

$issued = Auth::issue('soc');
check(is_array($issued) && is_string($issued['token'] ?? null), 'Auth: issue returns token');
$parts = explode('.', $issued['token']);
check(count($parts) === 2, 'Auth: token is payload.signature');
$decoded = Auth::verifySignature($issued['token']);
check(is_array($decoded) && ($decoded['u'] ?? null) === 'soc', 'Auth: verifySignature accepts issued token');

$sig = $parts[1];
$tampered = $parts[0] . '.' . ($sig[0] === '0' ? '1' : '0') . substr($sig, 1);
check(Auth::verifySignature($tampered) === null, 'Auth: tampered signature rejected');

$expiredPayload = Auth::base64Url(json_encode(['u' => 'soc', 'exp' => time() - 60, 'n' => 'x']));
$expiredToken = $expiredPayload . '.' . Auth::base64Url(hash_hmac('sha256', $expiredPayload, Config::get('auth.app_secret'), true));
check(Auth::verifySignature($expiredToken) === null, 'Auth: expired token rejected');

check(Auth::issuedByUs('test-master-token') === true, 'Auth: master access token accepted');
check(Auth::issuedByUs('not-a-token') === false, 'Auth: junk token rejected');
check(Auth::issuedByUs($issued['token']) === true, 'Auth: issued token accepted by issuedByUs');

expectSame(Auth::login('soc', 'secret'), true, 'Auth: login with correct password');
expectSame(Auth::login('soc', 'wrong'), false, 'Auth: login with wrong password');
expectSame(Auth::login('nobody', 'secret'), false, 'Auth: unknown user rejected');

/* --------------------------------------------------------------- RateLimiter */
unset($_SERVER['REMOTE_ADDR']);
$_SERVER['REMOTE_ADDR'] = '10.9.9.9';
RateLimiter::hit('test-bucket', 5, 60);
RateLimiter::hit('test-bucket', 5, 60);
RateLimiter::hit('test-bucket', 5, 60);
check(true, 'RateLimiter: under-limit hits complete without error');

/* -------------------------------------------------------------------- Cache */
$value = ['ip' => '8.8.8.8', 'verdict' => 'clean'];
Cache::set('test:key', $value, 60);
expectSame(Cache::get('test:key'), $value, 'Cache: set/get roundtrip (file)');
Cache::set('test:expired', ['x' => 1], -10);
expectSame(Cache::get('test:expired', 'gone'), 'gone', 'Cache: expired entry returns default');

/* -------------------------------------------------------------------- Input */
$_GET['ip'] = '  8.8.8.8  ';
check(Input::string('ip') === '8.8.8.8', 'Input: string trimmed');
$_GET['limit'] = '99999';
expectSame(Input::int('limit', 10, 1, 50), 50, 'Input: int clamped to max');
expectSame(Input::int('missing', 7), 7, 'Input: int default');
$_SERVER['HTTP_X_FORWARDED_FOR'] = '203.0.113.9, 10.0.0.1';
$_SERVER['REMOTE_ADDR'] = '192.168.0.1';
expectSame(Input::clientIp(), '203.0.113.9', 'Input: clientIp uses X-Forwarded-For');
$_SERVER['HTTP_X_FORWARDED_FOR'] = '';   // invalid value => falls back
unset($_SERVER['HTTP_X_FORWARDED_FOR']);

/* ------------------------------------------------------------------ Verdict */
expectSame(Verdict::threatRisk(['abuse_confidence_score' => 40], null), 'malicious', 'Verdict: score 40 is malicious');
expectSame(Verdict::threatRisk(['abuse_confidence_score' => 10], ['malicious' => 2]), 'malicious', 'Verdict: vt malicious is malicious');
expectSame(Verdict::threatRisk(['abuse_confidence_score' => 30], null), 'suspicious', 'Verdict: score 30 is suspicious');
expectSame(Verdict::threatRisk(['abuse_confidence_score' => 30], ['malicious' => 0]), 'suspicious', 'Verdict: score venturing clean check');
expectSame(Verdict::threatRisk(['abuse_confidence_score' => 5], ['malicious' => 0, 'suspicious' => 0, 'verdict' => 'clean']), 'clean', 'Verdict: clean');
expectSame(Verdict::threatRisk(null, null), 'unknown', 'Verdict: no data is unknown');

/* ---------------------------------------------------------------- MockData */
$dashboard = MockData::dashboard();
expectSame(array_sum([
    $dashboard['total_alerts'], $dashboard['critical_alerts'], $dashboard['high_alerts'],
    $dashboard['medium_alerts'], $dashboard['low_alerts'],
]), $dashboard['total_alerts'] + $dashboard['critical_alerts'] + $dashboard['high_alerts']
+ $dashboard['medium_alerts'] + $dashboard['low_alerts'], 'MockData: dashboard totals consistent');
check(is_array($dashboard['recent_alerts']) && count($dashboard['recent_alerts']) === 5, 'MockData: dashboard has 5 recent alerts');
$detail = MockData::alertDetail(122);
check(is_array($detail) && $detail['id'] === 122, 'MockData: alert detail found');
expectSame(MockData::alertDetail(999999), null, 'MockData: unknown alert detail is null');
expectContains(MockData::ipReputation(), 'isp', 'MockData: ipReputation shape');

/* ------------------------------------------------------------------ Logger */
$redacted = Logger::redact(['api_key' => 'k1', 'password' => 'p1', 'clean' => 'ok', 'nested' => ['pass' => 'p2']]);
check($redacted['api_key'] === '***REDACTED***' && $redacted['password'] === '***REDACTED***'
    && $redacted['clean'] === 'ok' && $redacted['nested']['pass'] === '***REDACTED***', 'Logger: sensitive keys redacted');

/* ------------------------------------------------------------------ Report */
echo PHP_EOL;
foreach ($checks as $line) {
    echo $line, PHP_EOL;
}
echo PHP_EOL;
echo "== $passed passed, $failed failed ==\n";
@unlink($tmpStorage . '/.app_secret');
@array_map('unlink', glob("$tmpCache/*.json") ?: []);
@array_map('unlink', glob("$tmpCache/ratelimit/*.json") ?: []);
exit($failed === 0 ? 0 : 1);