#!/usr/bin/env bash
set -euo pipefail
umask 077

: "${PGHOST:?Configure PGHOST}"
: "${PGDATABASE:?Configure PGDATABASE}"
: "${PGUSER:?Configure PGUSER}"
: "${PGPASSFILE:?Configure PGPASSFILE with a protected .pgpass file}"
: "${AGE_RECIPIENT:?Configure AGE_RECIPIENT with the off-site backup recipient key}"

backup_dir=${BACKUP_DIR:-/var/backups/nucleo}
mkdir -p "$backup_dir"
chmod 700 "$backup_dir"
timestamp=$(date -u +%Y%m%dT%H%M%SZ)
backup_file="$backup_dir/${PGDATABASE}-${timestamp}.dump.age"
partial_file="$backup_file.partial"
trap 'rm -f "$partial_file"' EXIT

pg_dump \
  --format=custom \
  --no-owner \
  --host="$PGHOST" \
  --port="${PGPORT:-5432}" \
  --username="$PGUSER" \
  --dbname="$PGDATABASE" \
  | age --encrypt --recipient "$AGE_RECIPIENT" > "$partial_file"

mv "$partial_file" "$backup_file"
trap - EXIT
printf 'Backup criado: %s\n' "$backup_file"
