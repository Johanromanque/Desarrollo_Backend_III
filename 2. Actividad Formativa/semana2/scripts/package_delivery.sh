#!/bin/sh

set -eu

if [ "$#" -ne 1 ]; then
    echo "Uso: $0 Exp1_S2_GrupoX" >&2
    exit 2
fi

archive_name=$1
case "$archive_name" in
    *[!A-Za-z0-9._-]*|'')
        echo "El nombre solo puede contener letras, numeros, punto, guion y guion bajo." >&2
        exit 2
        ;;
esac

script_dir=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
project_dir=$(CDPATH= cd -- "$script_dir/.." && pwd)
output_file="$project_dir/../$archive_name.zip"

if [ -e "$output_file" ]; then
    echo "El archivo ya existe y no sera sobrescrito: $output_file" >&2
    exit 1
fi

cd "$project_dir"

zip -r "$output_file" \
    .mvn \
    pom.xml \
    mvnw \
    mvnw.cmd \
    src \
    scripts \
    README.md \
    EVIDENCIAS_EJECUCION.md \
    INFORME_PLAN_CORRECCIONES_CODEX.md \
    Markdown/README.md \
    -x 'Wallet_*' 'wallet/*' '*.p12' '*.sso' 'tnsnames.ora' 'sqlnet.ora' \
       'target/*' 'logs/*' '.git/*' '.idea/*' '.vscode/*' '*.log'

echo "Entrega creada sin Wallet ni credenciales: $output_file"
