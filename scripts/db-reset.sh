#!/usr/bin/env bash
# Réinitialise la base locale : supprime le volume et recrée un PostgreSQL vierge.
# Flyway rejouera les migrations au prochain démarrage de l'application.
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND="$ROOT/backend"

if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 25)"; export JAVA_HOME
fi

# Version PostgreSQL = source de vérité unique (backend/pom.xml).
POSTGRES_VERSION="$(cd "$BACKEND" && ./mvnw -q help:evaluate -Dexpression=postgres.image.version -DforceStdout)"
export POSTGRES_VERSION
echo "→ PostgreSQL ${POSTGRES_VERSION} (version depuis backend/pom.xml)"

echo "→ Suppression du volume et des containers…"
docker compose -f "$ROOT/docker-compose.yml" down -v

echo "→ Recréation d'un PostgreSQL vierge…"
docker compose -f "$ROOT/docker-compose.yml" up -d postgres

echo "→ Base réinitialisée. Lance l'app (ou scripts/dev-start.sh) pour rejouer les migrations Flyway."
