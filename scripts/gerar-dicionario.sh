#!/usr/bin/env bash
# Gera docs/situacao-1/dados/dicionario-de-dados.md a partir do catálogo do PostgreSQL
# (o banco precisa estar com as migrations Flyway aplicadas — basta subir a aplicação uma vez).
# Uso: PGHOST=localhost PGUSER=cupcakes PGPASSWORD=cupcakes PGDATABASE=cupcakes scripts/gerar-dicionario.sh
set -euo pipefail
OUT="$(dirname "$0")/../docs/situacao-1/dados/dicionario-de-dados.md"
PSQL="psql -X -A -t -F|"

{
echo "# Dicionário de Dados"
echo
echo "> Gerado automaticamente por \`scripts/gerar-dicionario.sh\` a partir do banco criado pelas migrations Flyway"
echo "> (\`src/main/resources/db/migration\`). As descrições vêm dos \`COMMENT ON\` da migration V3."
echo
for T in $($PSQL -c "select table_name from information_schema.tables where table_schema='public' and table_type='BASE TABLE' and table_name <> 'flyway_schema_history' order by table_name"); do
  echo "## \`$T\`"
  echo
  echo "$($PSQL -c "select obj_description('public.$T'::regclass)")"
  echo
  echo "| Coluna | Tipo | Nulo | Padrão | Chave | Descrição |"
  echo "|--------|------|------|--------|-------|-----------|"
  $PSQL -c "
    select c.column_name,
           case when c.data_type in ('character varying') then 'VARCHAR(' || c.character_maximum_length || ')'
                when c.data_type = 'numeric' then 'NUMERIC(' || c.numeric_precision || ',' || c.numeric_scale || ')'
                when c.data_type = 'timestamp with time zone' then 'TIMESTAMPTZ'
                else upper(c.data_type) end,
           case when c.is_nullable = 'YES' then 'sim' else 'não' end,
           coalesce(case when c.is_identity = 'YES' then 'identity' else c.column_default end, ''),
           coalesce((select string_agg(case tc.constraint_type when 'PRIMARY KEY' then 'PK' when 'FOREIGN KEY' then 'FK' when 'UNIQUE' then 'UK' end, ', ' order by tc.constraint_type)
                     from information_schema.key_column_usage k
                     join information_schema.table_constraints tc on tc.constraint_name = k.constraint_name and tc.table_name = k.table_name
                     where k.table_name = c.table_name and k.column_name = c.column_name), ''),
           coalesce(col_description(('public.' || c.table_name)::regclass, c.ordinal_position), '')
    from information_schema.columns c
    where c.table_schema = 'public' and c.table_name = '$T'
    order by c.ordinal_position" | sed 's/|/ | /g; s/^/| /; s/$/ |/'
  echo
  CK=$($PSQL -c "select conname || ': ' || pg_get_constraintdef(oid) from pg_constraint where conrelid = 'public.$T'::regclass and contype in ('c','f','u') order by conname")
  if [ -n "$CK" ]; then
    echo "**Restrições:**"
    echo
    echo "$CK" | sed 's/^/- `/; s/$/`/'
    echo
  fi
done
} > "$OUT"
echo "Gerado: $OUT"
