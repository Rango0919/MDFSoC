#!/usr/bin/env bash
# MDF SOC API - live deployment smoke test.
# Usage: BASE_URL=http://your-host:8080/api ./tests/live-smoke.sh
# Requires curl and a POSIX shell. Login credentials via SOC_USER/SOC_PASS env vars.

set -u

BASE_URL="${BASE_URL:-http://192.168.0.168:8080/api}"
SOC_USER="${SOC_USER:-soc}"
SOC_PASS="${SOC_PASS:-admin123}"

jget() { # jget <sed-expr> <file>  -- poor-man's json extraction
  sed -n "s/.*\"$1\":\"\([^\"]*\)\".*/\1/p" "$2" | head -1
}

req() { # req <label> <method> <path> [query]
  local label="$1" method="$2" path="$3" query="${4:-}"
  local url="$BASE_URL$path"
  local out code
  if [ "$method" = GET ]; then
    out=$(curl -s -m 15 -H "Authorization: Bearer $TOKEN" "${url}${query}")
  else
    out=$(curl -s -m 15 -X POST -H "Content-Type: application/json" -d '{"arg":"x"}' "$url")
  fi
  code=$(echo "$out" | sed -n 's/.*"success":\(true\|false\).*/\1/p' | head -1)
  printf '%-28s success=%-5s %s\n' "$label" "${code:-?}" "$(echo "$out" | head -c 120)"
}

echo "== MDF SOC live smoke test =="
echo "Base URL: $BASE_URL"

LOGIN=$(curl -s -m 15 -X POST -H "Content-Type: application/json" \
  -d "{\"username\":\"$SOC_USER\",\"password\":\"$SOC_PASS\"}" "$BASE_URL/login.php")
TOKEN=$(jget token <(echo "$LOGIN"))

if [ -z "$TOKEN" ]; then
  echo "LOGIN FAILED: $LOGIN"
  exit 1
fi
echo "Login ........ OK (token ${TOKEN:0:16}...)"

req "dashboard"       GET  "/dashboard.php"
req "alerts (all)"    GET  "/alerts.php"   "?severity=all&limit=3"
req "alert detail"    GET  "/alert.php"    "?id=122"
req "ip reputation"   GET  "/ip-reputation.php"   "?ip=8.8.8.8"
req "threat intel"    GET  "/threat-intelligence.php" "?ip=8.8.8.8"

echo "--- error paths ---"
curl -s -m 10 -H "Authorization: Bearer $TOKEN" "$BASE_URL/alert.php?id=99999"  -o /dev/null -w "alert 99999   HTTP %{http_code}\n"
curl -s -m 10 -H "Authorization: Bearer $TOKEN" "$BASE_URL/ip-reputation.php?ip=999.1.1.1" -o /dev/null -w "bad IP         HTTP %{http_code}\n"
curl -s -m 10 "$BASE_URL/dashboard.php"        -o /dev/null -w "no token       HTTP %{http_code}\n"

echo "Done."