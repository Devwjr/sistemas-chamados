#!/usr/bin/env bash
set -euo pipefail

repo_root=$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)
tmp=$(mktemp -d)
trap 'rm -rf "$tmp"' EXIT
mkdir -p "$tmp/bin" "$tmp/backups"

cat > "$tmp/bin/pg_dump" <<'SH'
#!/usr/bin/env bash
printf 'test-postgres-archive\n'
SH

cat > "$tmp/bin/age" <<'SH'
#!/usr/bin/env bash
case "${1:-}" in
  --encrypt)
    cat | sed 's/^/encrypted:/'
    ;;
  --decrypt)
    file=${@: -1}
    sed 's/^encrypted://' "$file"
    ;;
  *) exit 2 ;;
esac
SH

cat > "$tmp/bin/pg_restore" <<'SH'
#!/usr/bin/env bash
cat > "$RESTORE_CAPTURE"
SH
chmod +x "$tmp/bin/pg_dump" "$tmp/bin/age" "$tmp/bin/pg_restore"

export PATH="$tmp/bin:$PATH"
export PGHOST=127.0.0.1 PGPORT=5432 PGDATABASE=suporte PGUSER=nucleo
export PGPASSFILE="$tmp/pgpass" AGE_RECIPIENT=age1test BACKUP_DIR="$tmp/backups"
export RESTORE_CAPTURE="$tmp/restored.dump"
touch "$PGPASSFILE"

"$repo_root/deploy/backup-postgres.sh" > /dev/null
backup_file=$(find "$BACKUP_DIR" -type f -name '*.dump.age' -print -quit)
[[ -n "$backup_file" ]]
[[ "$(cat "$backup_file")" == 'encrypted:test-postgres-archive' ]]

export BACKUP_FILE="$backup_file" AGE_IDENTITY="$tmp/identity"
touch "$AGE_IDENTITY"
printf 'suporte\n' | "$repo_root/deploy/restore-postgres.sh" > /dev/null
[[ "$(cat "$RESTORE_CAPTURE")" == 'test-postgres-archive' ]]

rm -f "$RESTORE_CAPTURE"
if printf 'outro-banco\n' | "$repo_root/deploy/restore-postgres.sh" > /dev/null 2>&1; then
  printf 'A restauração deveria ter recusado um nome de banco incorreto.\n' >&2
  exit 1
fi
[[ ! -e "$RESTORE_CAPTURE" ]]
