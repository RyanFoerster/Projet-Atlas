#!/usr/bin/env bash
# Réinitialise la base locale : supprime le volume et recrée un PostgreSQL vierge.
# Flyway rejouera les migrations au prochain démarrage de l'application.
set -euo pipefail
cd "$(dirname "$0")/.."

if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 25)"; export JAVA_HOME
fi

POSTGRES_VERSION="$(mvn -q help:evaluate -Dexpression=postgres.image.version -DforceStdout)"
export POSTGRES_VERSION
echo "→ PostgreSQL ${POSTGRES_VERSION} (version depuis pom.xml)"

echo "→ Suppression du volume et des containers…"
docker compose down -v

echo "→ Recréation d'un PostgreSQL vierge…"
docker compose up -d postgres

echo "→ Base réinitialisée. Lance l'app (ou scripts/dev-start.sh) pour rejouer les migrations Flyway."
