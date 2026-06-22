#!/usr/bin/env bash
# Démarre PostgreSQL (docker compose, depuis la racine) puis l'application
# Spring Boot en local (depuis backend/, via le Maven wrapper).
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
BACKEND="$ROOT/backend"

# JDK 25 (ADR-002). Sur macOS, on le résout via java_home si JAVA_HOME n'est pas déjà défini.
if [ -z "${JAVA_HOME:-}" ] && [ -x /usr/libexec/java_home ]; then
  JAVA_HOME="$(/usr/libexec/java_home -v 25)"; export JAVA_HOME
fi

# Version PostgreSQL = source de vérité unique (backend/pom.xml), injectée dans docker-compose.
POSTGRES_VERSION="$(cd "$BACKEND" && ./mvnw -q help:evaluate -Dexpression=postgres.image.version -DforceStdout)"
export POSTGRES_VERSION
echo "→ PostgreSQL ${POSTGRES_VERSION} (version depuis backend/pom.xml)"

echo "→ Démarrage de PostgreSQL (docker compose)…"
docker compose -f "$ROOT/docker-compose.yml" up -d postgres

echo "→ Lancement de l'application Spring Boot (profil local)…"
cd "$BACKEND" && ./mvnw spring-boot:run
