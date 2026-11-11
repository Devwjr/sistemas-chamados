#!/usr/bin/env bash
set -euo pipefail

: "${PGHOST:?Configure PGHOST}"
: "${PGDATABASE:?Configure PGDATABASE}"
: "${PGUSER:?Configure PGUSER}"
: "${PGPASSFILE:?Configure PGPASSFILE with a protected .pgpass file}"
: "${BACKUP_FILE:?Set BACKUP_FILE to the dump file to restore}"
: "${AGE_IDENTITY:?Set AGE_IDENTITY to the private key for this backup}"

if [[ ! -r "$BACKUP_FILE" ]]; then
  printf 'Backup não encontrado ou sem permissão de leitura: %s\n' "$BACKUP_FILE" >&2
  exit 1
fi

printf 'ATENÇÃO: os objetos do banco "%s" serão substituídos.\n' "$PGDATABASE" >&2
printf 'Digite o nome exato do banco para confirmar: ' >&2
read -r confirmation
if [[ "$confirmation" != "$PGDATABASE" ]]; then
  printf 'Restauração cancelada.\n' >&2
  exit 1
fi

age --decrypt --identity "$AGE_IDENTITY" "$BACKUP_FILE" \
  | pg_restore \
  --clean \
  --if-exists \
  --no-owner \
  --exit-on-error \
  --host="$PGHOST" \
  --port="${PGPORT:-5432}" \
  --username="$PGUSER" \
  --dbname="$PGDATABASE"

printf 'Restauração concluída no banco: %s\n' "$PGDATABASE"
