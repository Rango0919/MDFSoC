<?php

require __DIR__ . '/../core/Bootstrap.php';

Bootstrap::run();

header('Content-Type: application/json; charset=utf-8');

$method = strtoupper($_SERVER['REQUEST_METHOD'] ?? '');
if ($method !== 'POST') {
    Response::error('method_not_allowed', 'Only POST requests are accepted', 'backend', 405);
}

RateLimiter::hit('login', Config::int('rate_limit.login_max', 20), Config::int('rate_limit.login_window', 600));

$body = Input::jsonBody();
$username = trim((string) ($body['username'] ?? ''));
$password = (string) ($body['password'] ?? '');

if ($username === '' || $password === '') {
    Response::error('validation_error', 'Username and password are required', 'backend', 400);
}

$success = Auth::login($username, $password);
if (!$success) {
    Logger::warning('Login failed', ['username' => $username, 'ip' => Input::clientIp()]);
    Response::error('auth_error', 'Invalid username or password', 'backend', 401);
}

Logger::info('Login successful', ['username' => $username, 'ip' => Input::clientIp()]);
$session = Auth::issue($username);
Response::success($session, [], 200);