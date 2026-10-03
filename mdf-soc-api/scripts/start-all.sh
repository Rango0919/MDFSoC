#!/usr/bin/env bash
#
# Starts the MDF SOC stack (MariaDB + PHP API) if it isn't already running.
# Safe to re-run after a reboot. Uses persistent installs in ~/mariadb and ~/bin/php
# so nothing is lost on reboot (unlike the old /tmp-based setup).
#
set -u

MDB_ROOT="$HOME/mariadb"
PHP_BIN="$HOME/bin/php"
DEFAULTS_OK=1

check() { command -v "$1" >/dev/null 2>&1; }

if [ ! -x "$PHP_BIN" ]; then
  echo "PHP binary missing at $PHP_BIN (run the PHP setup steps first)."
  exit 1
fi

# --- 1. MariaDB ---------------------------------------------------------
PORT=$(ss -tln 2>/dev/null | awk '{print $4}' | grep -q ':3306$' && echo up || echo down)
if [ "$PORT" = "down" ]; then
  echo "Starting MariaDB ..."
  mkdir -p "$MDB_ROOT"
  setsid nohup "$MDB_ROOT/root/usr/sbin/mariadbd" --no-defaults \
    --basedir="$MDB_ROOT/root/usr" \
    --datadir="$MDB_ROOT/data" \
    --socket="$MDB_ROOT/mysql.sock" \
    --port=3306 --bind-address=127.0.0.1 \
    --pid-file="$MDB_ROOT/mariadb.pid" \
    --log-error="$MDB_ROOT/mariadb.err" \
    --user="${USER:-$(id -un)}" </dev/null >/dev/null 2>&1 &
  for i in 1 2 3 4 5 6 7 8 9 10; do
    ss -tln 2>/dev/null | grep -q ':3306$' && break
    sleep 1
  done
else
  echo "MariaDB already running (port 3306)."
fi

API_PORT=$(ss -tln 2>/dev/null | awk '{print $4}' | grep -q ':8080$' && echo up || echo down)
if [ "$API_PORT" = "down" ]; then
  echo "Starting API server on :8080 ..."
  cd "$(dirname "$0")/.." || exit 1
  setsid nohup "$PHP_BIN" -S 0.0.0.0:8080 -t . >"$MDB_ROOT/api_server.log" 2>&1 &
  for i in 1 2 3 4 5; do
    ss -tln 2>/dev/null | grep -q ':8080$' && break
    sleep 1
  done
else
  echo "API server already running (port 8080)."
fi

echo "Done. Stack status:"
ss -tln | grep -E ':3306|:8080'
echo "API log:  tail -f $MDB_ROOT/api_server.log"
echo "DB  log:  tail -f $MDB_ROOT/mariadb.err"