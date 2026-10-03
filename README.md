# MDF SoC — Mobile Digital Forensics SOC

Android app plus PHP backend for a small Security Operations Center: live alert
triage from Wazuh, IP/URL reputation lookups, and a phishing-message analyzer that
pulls links and sender IPs out of raw email text.

```
┌────────────────────┐        HTTPS + Bearer token        ┌────────────────────┐
│  Android app       │  ─────────────────────────────────▶  │  mdf-soc-api (PHP) │
│  (Java, Retrofit)  │                                     │  no framework      │
└────────────────────┘                                     └─────────┬──────────┘
                                                                       │
                        ┌───────────────────┬──────────────────┬──────────┴─────────┐
                        ▼                   ▼                  ▼                    ▼
                   Wazuh API          AbuseIPDB API     VirusTotal API      MariaDB (optional)
                   alerts + counts    IP reputation     IP + URL reports    users/cache/limits
```

Everything ships disabled and running on mock data, so you can get the whole stack
up without any API keys. Turn each integration on when you have credentials.

## Features

**Android app** (`app/`, Java, MVVM + Retrofit/OkHttp + LiveData)

| Screen | What it does |
| --- | --- |
| Dashboard | Total/critical/high/medium/low alert counts plus the 5 most recent alerts |
| Alerts | Severity filter (all/critical/high/medium/low), pull-to-refresh, paginated list |
| Incident Detail | Rule ID, level, agent, src/dst IP, description, MITRE ATT&CK technique |
| Investigate | IP reputation from AbuseIPDB + VirusTotal, MITRE mapping, threat verdict |
| Phishing | Paste a raw email → extract deobfuscated links, header IPs and sender domains; open any link in the URL investigator; jump to Investigate with a host IP |

**Backend** (`mdf-soc-api/`, dependency-free PHP 8)

- HMAC-SHA256 bearer tokens (`payload.signature`) with expiry; optional static master token
- bcrypt login against `config` or the `mdfsoc_users` table
- Per-endpoint and per-IP rate limiting (file-backed, or MySQL when enabled)
- File/MySQL cache with per-source TTLs, so repeated lookups don't burn API quota
- Uniform JSON envelope (`success`/`data`/`warnings`/`error`) and error `source`
  (`backend`, `wazuh`, `abuseipdb`, `virustotal`) so the app can map failures to strings
- Degrades gracefully: when an upstream is down it returns partial data plus a
  `warnings[]` entry instead of a hard failure
- Optional SMTP alerts for new critical/high alerts and a daily digest

## Repository layout

```
app/                     Android application module
  src/main/java/.../     activities, fragments, adapters, viewmodels,
                         repositories, network, models, utils
  src/test/java/...      JUnit tests (parsers, validators, envelope parsing)
mdf-soc-api/             PHP backend (document root)
  api/                   one file per endpoint
  core/                  Config, Bootstrap, Auth, Cache, RateLimiter, Db,
                         HttpClient, Logger, Input, MockData, Verdict, Mailer,
                         UrlAnalyzer, Response, Exceptions
  services/              WazuhService, AbuseIPDBService, VirusTotalService,
                         MitreMapper
  config/                config.php (defaults) + config.local.php (your overrides)
  database/schema.sql    optional MySQL schema
  scripts/               start-all.sh, notify.php, send-test-email.php
  tests/                 runtime-tests.php, UrlAnalyzerTest.php, live-smoke.sh
  logs/ storage/         runtime output, not source
```

## Requirements

- Android: JDK 17+, Android Studio, or the bundled Gradle wrapper (Gradle 9.6, AGP 9.4)
- Device/emulator: minSdk 24 (Android 7.0)
- API: PHP 8.0+ with `curl` (upstream calls) and `pdo_mysql` (only if you enable the DB)
- Optional: MariaDB/MySQL for user accounts, cache and rate-limit counters

## Quick start

### 1. Backend

```bash
cd mdf-soc-api
cp config/config.local.sample.php config/config.local.php
php -S 0.0.0.0:8080 -t .
```

`scripts/start-all.sh` does the same but also boots a MariaDB instance from
`~/mariadb` and is safe to re-run after a reboot.

Out of the box `app.mock_mode` is `true` and every integration is `enabled: false`, so
every endpoint except `login.php` already answers from mock data — no Wazuh, no API
keys, no database. Login is *not* mocked, so set a password hash before you can sign in
(the default `config.php` leaves it empty and `login.php` will reject everything):

```bash
php -r 'echo password_hash("choose-a-strong-password", PASSWORD_DEFAULT), PHP_EOL;'
```

Paste the hash into `auth.admin_password_hash` in `config/config.local.php`. The
same value can live in `mdfsoc_users` if you prefer the database path.

Sanity check:

```bash
TOKEN=$(curl -s -X POST -H 'Content-Type: application/json' \
  -d '{"username":"soc","password":"choose-a-strong-password"}' \
  http://127.0.0.1:8080/api/login.php | sed -n 's/.*"token":"\([^"]*\)".*/\1/p')

curl -s -H "Authorization: Bearer $TOKEN" http://127.0.0.1:8080/api/dashboard.php
```

### 2. Android app

The base URL is a `buildConfigField` in `app/build.gradle.kts`:

```kotlin
debug   { buildConfigField("String", "API_BASE_URL", "\"http://127.0.0.1:8080/api/\"") }
release { buildConfigField("String", "API_BASE_URL", "\"https://your-server.example.com/mdf-soc-api/api/\"") }
```

`127.0.0.1` inside an emulator refers to the emulator itself. For a backend running
on your workstation use `http://10.0.2.2:8080/api/`; for a backend on the LAN use its
IP, e.g. `http://192.168.0.168:8080/api/`.

Cleartext HTTP is blocked by default. Each host you want to reach over plain HTTP must
be allow-listed in `app/src/main/res/xml/network_security_config.xml` (it already lists
`10.0.2.2`, `192.168.0.168`, `localhost` and `127.0.0.1`). Release builds should point
at an HTTPS host instead, which then needs no exception.

```bash
./gradlew :app:installDebug      # or open the project in Android Studio and press Run
```

Sign in with the credentials you configured; the token is stored in
`EncryptedSharedPreferences` and attached by `AuthInterceptor` on every request.

## Configuration

`config/config.php` holds safe defaults and is meant to stay in version control.
`config/config.local.php` is deep-merged over it — only put overrides there, and never
commit it. Start from `config/config.local.sample.php`.

| Section | Keys that matter |
| --- | --- |
| `app` | `env`, `debug`, `https_enforce` (off for local dev), `mock_mode` |
| `auth` | `app_secret`, `access_token` (static master token), `admin_user`, `admin_password_hash`, `token_ttl`, `public_endpoints` |
| `rate_limit` | `max`/`window` per API call, `login_max`/`login_window` for `login.php` |
| `cache` | `dir` plus TTLs: `alert_ttl`, `token_ttl`, `abuseipdb_ttl`, `threat_intel_ttl`, `mitre_ttl` |
| `db` | `enabled`, `host`, `port`, `name`, `user`, `pass` |
| `wazuh` | `enabled`, `api_url`, `user`, `pass`, `ca_file`, `verify_ssl`, `alerts_endpoint` |
| `abuseipdb` / `virustotal` | `enabled`, `api_key` |
| `mitre` | `local_map_enabled`, optional `stix_url` |
| `email` | `enabled`, `smtp_host`, `smtp_port`, `username`, `password`, `from_email`, `to_emails` |

`auth.app_secret` signs tokens. If you leave it empty, one is generated on first run and
persisted to `storage/.app_secret` (mode 0600) — convenient locally, but set it
explicitly in production so every node shares the same secret.

### MySQL (optional)

```bash
mysql -u root -p < database/schema.sql
```

Then set `db.enabled: true` and the credentials, and replace the
`REPLACE_WITH_PASSWORD_HASH` placeholder row:

```sql
UPDATE mdfsoc_users SET password_hash = '<bcrypt hash>' WHERE username = 'soc';
```

With the database enabled, cache entries and rate-limit counters move from
`storage/cache/*.json` into the `mdfsoc_cache` and `mdfsoc_rate_limits` tables.

### Integrations

- **Wazuh** — basic-auth against `/security/user/authenticate`; the JWT is cached and
  refreshed once on a 401. Alerts are normalized and MITRE-mapped from rule metadata.
  `wazuh.ca_file` is there for self-signed manager certs.
- **AbuseIPDB** — IP reputation and abuse-confidence score, cached for an hour.
- **VirusTotal** — IP and URL reports; URL lookups also cover the expanded destination
  when the original link redirects.
- **MITRE ATT&CK** — mapped locally from rule IDs and threat verdicts via
  `services/MitreMapper.php`; no network call unless `mitre.stix_url` is set.

## API

Base path: `mdf-soc-api/api/`. Every response uses the same envelope:

```json
{ "success": true, "data": { }, "warnings": [], "error": null }
```

```json
{ "success": false, "data": null, "warnings": [],
  "error": { "code": "auth_error", "message": "…", "source": "backend" } }
```

All endpoints except `login.php` require `Authorization: Bearer <token>`.

| Endpoint | Method | Parameters | Returns |
| --- | --- | --- | --- |
| `login.php` | POST | JSON `{username, password}` | `{token, expires_at}` |
| `dashboard.php` | GET | — | alert counts + 5 recent alerts |
| `alerts.php` | GET | `severity`, `limit` (≤200), `offset` | `{alerts, total, page, limit}` |
| `alert.php` | GET | `id` | single alert with MITRE data |
| `ip-reputation.php` | GET | `ip` | AbuseIPDB report |
| `threat-intelligence.php` | GET | `ip` | AbuseIPDB + VirusTotal + verdict + MITRE |
| `url-reputation.php` | GET | `url` | redirect chain, resolved IPs, VirusTotal |

Tokens are also accepted as a `?token=` query parameter, which is handy for `curl` and
link-based lookups. `https_enforce` blocks plain HTTP for non-loopback clients, so
terminate TLS in front of the API in production.

## Tests

```bash
php mdf-soc-api/tests/runtime-tests.php     # config/auth/cache/limits/verdict/mock/logging
php mdf-soc-api/tests/UrlAnalyzerTest.php   # URL normalization and redirect handling
./gradlew :app:testDebugUnitTest            # parsers, IP validation, envelope parsing

BASE_URL=http://127.0.0.1:8080/api \
SOC_USER=soc SOC_PASS=… \
  ./mdf-soc-api/tests/live-smoke.sh         # end-to-end against a running stack
```

`runtime-tests.php` runs against a temporary config and cache directory, so it never
touches your real settings. `live-smoke.sh` also checks the error paths (unknown alert,
malformed IP, missing token).

## Email notifications

```bash
php scripts/send-test-email.php                    # verify SMTP
php scripts/notify.php alerts                      # new critical/high since last run
php scripts/notify.php digest                      # once-a-day volume summary
```

Progress is tracked in `storage/notify_state.json`, so `alerts` is safe to run every
few minutes from cron without re-sending:

```cron
*/5  * * * *  php /path/to/mdf-soc-api/scripts/notify.php alerts >> logs/notify.log 2>&1
15 9 * * *  php /path/to/mdf-soc-api/scripts/notify.php digest >> logs/notify.log 2>&1
```

Gmail requires an App Password rather than the account password.

## Security notes

- Keep `config/config.local.php`, `storage/`, `logs/` and `local.properties` out of
  version control — the Android signing config, API keys, SMTP password and generated
  app secret all live there. The repo's root `.gitignore` covers `local.properties` and
  build output; add an ignore for `mdf-soc-api/config/config.local.php`,
  `mdf-soc-api/storage/` and `mdf-soc-api/logs/` before sharing the project, and purge
  them from history if they were already pushed.
- `adminer.php` is a database admin UI. Keep it out of production document roots.
- API responses set `nosniff`, `X-Frame-Options: DENY`, `no-referrer` and a restrictive
  `Permissions-Policy`; `config/`, `core/`, `services/`, `logs/` and `storage/` return
  404 through `.htaccess` under Apache.
- Logger output is redacted before writing, so `api_key`/`password`-style fields are
  stored as `***REDACTED***`.
- Rate limits are per client IP; put the API behind a reverse proxy with TLS and real
  client-IP forwarding (`Input::clientIp()` honours `X-Forwarded-For`) if you expect
  more than a handful of analysts.

## License

Not published. All rights reserved.