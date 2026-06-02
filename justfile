# Usage: install `just` (https://github.com/casey/just)

set shell := ["bash", "-euo", "pipefail", "-c"]

default:
    @just --list

# Avvia MySQL/Virtuoso/Elasticsearch e attende che siano ready (warn su Keycloak 8082)
infra-up:
    #!/usr/bin/env bash
    set -euo pipefail
    echo "[infra] docker compose up -d (MySQL, Virtuoso, Elasticsearch)"
    docker compose up -d

    echo "[infra] attendo MySQL 3306..."
    # NB: il check TCP passa appena il container apre la porta, ma MySQL impiega
    # qualche secondo in piu' a inizializzare e a rispondere al protocollo.
    # mysqladmin ping verifica un vero handshake.
    until docker compose exec -T mysql mysqladmin ping -h 127.0.0.1 -uroot -pexample --silent >/dev/null 2>&1; do sleep 1; done
    echo "[mysql] ready"

    echo "[infra] attendo Virtuoso 8890..."
    until curl -sf http://localhost:8890/sparql -o /dev/null 2>/dev/null \
       || curl -sf -u dba:dba http://localhost:8890/sparql-auth -o /dev/null 2>/dev/null \
       || (echo > /dev/tcp/127.0.0.1/8890) >/dev/null 2>&1; do sleep 1; done
    echo "[virtuoso] ready"

    echo "[infra] attendo Elasticsearch 9200 (HTTPS, ES 8.x security ON)..."
    # ES 8.x con security default risponde solo in TLS con certificato self-signed
    # e richiede auth. Credenziali hardcoded dal docker-compose: elastic/changeme.
    # -k per accettare il cert self-signed locale.
    until curl -sf -k -u elastic:changeme \
            "https://localhost:9200/_cluster/health?wait_for_status=yellow&timeout=2s" \
            -o /dev/null 2>/dev/null; do sleep 1; done
    echo "[elasticsearch] ready"

    echo "[infra] verifico Keycloak 8082 (gestito dal repo dati-semantic-admin)..."
    if curl -sf http://localhost:8082/realms/ndc/.well-known/openid-configuration -o /dev/null 2>/dev/null; then
        echo "[keycloak] ready"
    else
        echo "[keycloak] WARNING: non disponibile su 8082"
        echo "[keycloak]   per OAuth2 avviare 'just kc-up' nel repo dati-semantic-admin"
    fi

# Alias storico (back-compat).
stack: infra-up

# Stop local dependencies
down:
    docker compose down

# Tail local service logs
logs:
    docker compose logs -f

# Clean Gradle outputs and reset Docker volumes
clean:
    ./gradlew clean
    docker compose down -v

# Build application without tests
build:
    ./gradlew clean build -x test

# Run unit tests
test:
    ./gradlew test

# Run integration tests
integration-test:
    ./gradlew integrationTest

# Run all verification tasks
check:
    ./gradlew check

# Run static analysis tasks
lint:
    ./gradlew checkstyleMain checkstyleTest checkstyleIntegration spotbugsMain spotbugsTest spotbugsIntegration

# Generate JaCoCo coverage report and verify thresholds
coverage:
    ./gradlew test jacocoTestReport jacocoTestCoverageVerification

# Run the application locally (profilo local, basic+oauth2 entrambi attivi).
# Dipende da infra-up: aspetta MySQL/Virtuoso/ES ready prima di partire.
# Il JwtDecoder fa discovery lazy alla prima richiesta JWT, quindi il BE
# parte anche se Keycloak (su 8082, gestito dal repo dati-semantic-admin) non e' up.
run: infra-up
    SPRING_PROFILES_ACTIVE=local ./gradlew bootRun

# Variante: profilo local con sola Basic auth (oauth2 disabilitato).
run-basic: infra-up
    SPRING_PROFILES_ACTIVE=local HARVESTER_SECURITY_OAUTH2_ENABLED=false ./gradlew bootRun

# Variante: profilo local con solo OAuth2 (basic disabilitato).
run-oauth2: infra-up
    SPRING_PROFILES_ACTIVE=local HARVESTER_SECURITY_BASIC_ENABLED=false ./gradlew bootRun

# Submit repository validation job
validate-repo owner repo base_url="http://localhost:8080":
    curl -s -X POST "{{ base_url }}/validate/repo/{{ owner }}/{{ repo }}" | jq

# Poll repository validation job status
validate-repo-status validation_id base_url="http://localhost:8080":
    curl -s "{{ base_url }}/validate/repo/{{ validation_id }}" | jq

# Submit repository validation job and wait for completion
validate-repo-wait owner repo base_url="http://localhost:8080":
    #!/usr/bin/env bash
    RESPONSE=$(curl -s -X POST "{{ base_url }}/validate/repo/{{ owner }}/{{ repo }}")
    echo "$RESPONSE" | jq
    VALIDATION_ID=$(echo "$RESPONSE" | jq -r '.validationId')
    if [ "$VALIDATION_ID" = "null" ] || [ -z "$VALIDATION_ID" ]; then
        echo "Unable to extract validationId from response"
        exit 1
    fi
    while true; do
        STATUS_RESPONSE=$(curl -s "{{ base_url }}/validate/repo/${VALIDATION_ID}")
        echo "$STATUS_RESPONSE" | jq
        STATUS=$(echo "$STATUS_RESPONSE" | jq -r '.status')
        if [ "$STATUS" = "COMPLETED" ] || [ "$STATUS" = "FAILED" ]; then
            break
        fi
        sleep 2
    done

# Regenerate OpenAPI-generated sources
openapi:
    ./gradlew openApiGenerate

# Build container image in local Docker daemon
docker-build:
    ./gradlew jibDockerBuild

# Run OWASP dependency check
dep-check:
    ./gradlew dependencyCheckAnalyze

# No formatter is currently configured in Gradle
format:
    @echo "No code formatter is configured in build.gradle."
    @echo "Available code quality checks: just lint"
    @exit 1
