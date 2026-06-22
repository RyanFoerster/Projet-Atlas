#!/usr/bin/env bash
# Démarre PostgreSQL (docker compose) puis l'application Spring Boot en local.
set -euo pipefail
cd "$(dirname "$0")/.."

# JDK 25 (ADR-002). Sur macOS, on le résout via java_home si JAVA_HOME n'est pas déjà défini.
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 25)"; export JAVA_HOME
fi

# Version PostgreSQL = source de vérité unique (pom.xml), injectée dans docker-compose.
POSTGRES_VERSION="$(mvn -q help:evaluate -Dexpression=postgres.image.version -DforceStdout)"
export POSTGRES_VERSION
echo "→ PostgreSQL ${POSTGRES_VERSION} (version depuis pom.xml)"

echo "→ Démarrage de PostgreSQL (docker compose)…"
docker compose up -d postgres

echo "→ Lancement de l'application Spring Boot (profil local)…"
mvn spring-boot:run
