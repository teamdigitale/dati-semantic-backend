# Usage: install `just` (https://github.com/casey/just)

set shell := ["bash", "-euo", "pipefail", "-c"]

default:
    @just --list

# Start local dependencies
stack:
    docker compose up -d

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

# Run the application locally
run:
    ./gradlew bootRun

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
