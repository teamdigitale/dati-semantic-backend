[![License](https://img.shields.io/github/license/teamdigitale/dati-semantic-backend.svg)](https://github.com/teamdigitale/dati-semantic-backend/blob/main/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/teamdigitale/dati-semantic-backend.svg)](https://github.com/teamdigitale/dati-semantic-backend/issues)
[![Join the #design channel](https://img.shields.io/badge/Slack%20channel-%23design-blue.svg)](https://developersitalia.slack.com/messages/C7VPAUVB3/)
[![Get invited](https://slack.developers.italia.it/badge.svg)](https://slack.developers.italia.it/)
[![Dati on forum.italia.it](https://img.shields.io/badge/forum-dati-blue.svg)](https://forum.italia.it/c/dati/33)

# National Data Catalog (NDC) — Backend

Backend of the **National Data Catalog (NDC) for Semantic Interoperability**, a component of the **PDND** (*Piattaforma Digitale Nazionale Dati*, Italy's National Digital Data Platform). The service exposes a national catalog of semantic assets — ontologies, controlled vocabularies, schemas — produced by the Italian Public Administration, with automatic harvesting from public repositories, indexing, search, validation, and consultation APIs.

The backend is composed of:

- a **harvester** that periodically crawls the repositories published by catalog participants, downloading the semantic material and populating indexes and databases;
- a **triple store** (Virtuoso) queryable via SPARQL, where RDF material is loaded per graph;
- a **search index** (Elasticsearch) for full-text and semantic searches;
- a **REST API** (OpenAPI) for search, asset details, controlled vocabularies, on-demand validation, dashboards, and operational management.

---

## Table of contents

- [Tech stack](#tech-stack)
- [Prerequisites](#prerequisites)
- [Quick start](#quick-start)
- [Configuration](#configuration)
- [Common commands](#common-commands)
- [API](#api)
- [Asset validation](#asset-validation)
- [Harvesting](#harvesting)
- [Container and deployment](#container-and-deployment)
- [Code quality and security](#code-quality-and-security)
- [Contributing](#contributing)
- [License](#license)

---

## Tech stack

| Component | Version | Notes |
| :-- | :-- | :-- |
| Java | 17 | JDK 17 (CI), runtime distroless `gcr.io/distroless/java17-debian11:nonroot` |
| Spring Boot | 3.5.8 | Web, Data JPA, Data Elasticsearch, Security, Validation, Mail, Actuator |
| Apache Jena | 5.5.0 | RDF parsing/serialization, RIOT, query builder |
| Elasticsearch | 8.14.3 | Full-text and semantic search index |
| OpenLink Virtuoso | `tenforce/virtuoso` | SPARQL triple store |
| MySQL | 9.0 | Harvester metadata (jobs, runs, configuration, events) |
| Flyway | core + mysql | Schema migrations |
| Gradle | wrapper | Build, test, lint, scan |
| OpenAPI Generator | 7.7.0 | Generates interfaces from `openapi.yaml` |
| MapStruct, Lombok | — | DTO ↔ entity mapping, boilerplate reduction |
| Jib | 3.4.3 | Container image builds without a Dockerfile |
| OWASP dependency-check | 12.2.2 | Dependency vulnerability scanning |
| SpotBugs, Checkstyle, JaCoCo | — | Static analysis and coverage |

---

## Prerequisites

- **JDK 17** (Eclipse Temurin recommended)
- **Docker** + **Docker Compose** (for local dependencies: Virtuoso, Elasticsearch, MySQL)
- *(Optional)* **[just](https://github.com/casey/just)** for one-line commands (`justfile` included in the repo)
- *(Optional)* **GitHub Personal Access Token** if you exceed the anonymous GitHub API rate limit during harvesting (see [Configuration](#configuration))

---

## Quick start

From a shell at the repository root:

```bash
# 1. Start local dependencies (Virtuoso, Elasticsearch, MySQL)
just stack
# or: docker compose up -d

# 2. Build the application (without tests)
just build
# or: ./gradlew clean build -x test

# 3. Start the backend (the "local" profile is active by default)
just run
# or: ./gradlew bootRun
```

The service is reachable at `http://localhost:8080`. OpenAPI/Swagger UI: `http://localhost:8080/swagger-ui/index.html` (raw spec at `/api/v3/api-docs`).

Development credentials (Basic Auth) for the harvester's protected endpoints:

- user: `harv-user`
- password: `harv-password`

(Override via `HARVESTER_USER` / `HARVESTER_PASSWORD`.)

---

## Configuration

The `local` profile is active by default (`spring.profiles.active=local`). Main properties live in `src/main/resources/application.properties`; the most relevant environment variables are listed below.

### Datasource and external dependencies

| Variable | Default | Description |
| :-- | :-- | :-- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/dev_ndc_harvest` | MySQL JDBC URL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `root` / `example` | MySQL credentials |
| `ELASTICSEARCH_HOST` / `_PORT` / `_SCHEME` | `localhost` / `9200` / `https` | Elasticsearch endpoint |
| `ELASTICSEARCH_USERNAME` / `_PASSWORD` | `elastic` / `changeme` | Elasticsearch credentials |
| `VIRTUOSO_SPARQL` | — | Virtuoso SPARQL endpoint (e.g. `http://localhost:8890/sparql`) |
| `VIRTUOSO_SPARQL_GRAPH_STORE` | — | SPARQL Graph Store endpoint |
| `VIRTUOSO_USERNAME` / `_PASSWORD` | `dba` / `dba` | Virtuoso credentials |

### Harvesting

| Variable | Default | Description |
| :-- | :-- | :-- |
| `HARVESTER_USER` / `HARVESTER_PASSWORD` | `harv-user` / `harv-password` | Basic Auth for operational endpoints |
| `HARVESTER_REPOSITORIES` | — | Comma-separated list of repository URLs to harvest |
| `HARVESTER_ENDPOINT_ENABLED` | — | Enables harvest trigger endpoints |
| `harvester.folder.skip-words` | `scriptR2RML,sparql,deprecated` | Folders to skip (`contains` match, min 3 chars) |
| `harvester.ontology.scanner.skip-words` | `aligns,example` | Ontologies to skip |
| `harvester.controlled-vocabulary.scanner.skip-words` | `transparency-obligation-organization,transparency-obligation-administration` | Controlled vocabularies to skip |
| `GITHUB_PERSONAL_ACCESS_TOKEN` | — | GitHub PAT to bypass the public API rate limit (cookiecutter lookup, etc.) |

### Repository conformance check (cookiecutter)

Verifies that every harvested repository conforms to the official template [`teamdigitale/dati-semantic-cookiecutter`](https://github.com/teamdigitale/dati-semantic-cookiecutter).

| Variable | Default |
| :-- | :-- |
| `HARVESTER_CONFORMANCE_ENABLED` | `true` |
| `HARVESTER_CONFORMANCE_COOKIECUTTER_REPO` | `teamdigitale/dati-semantic-cookiecutter` |
| `HARVESTER_CONFORMANCE_COOKIECUTTER_BRANCH` | `main` |

### Vocabulary aggregation (CSV APIs)

The harvester periodically aggregates per-vocabulary SQLite databases into a single `vocabularies.db` file served over HTTP.

| Variable | Default |
| :-- | :-- |
| `HARVESTER_AGGREGATE_DB_PATH` | `/tmp/ndc-vocabularies.db` |
| `HARVESTER_AGGREGATE_DB_WORK_DIR` | `/tmp/ndc-csvapis-work` |
| `HARVESTER_AGGREGATE_DB_CACHE_MAX_AGE` | `86400` (seconds) |

### Mailer (alerting)

| Variable | Default |
| :-- | :-- |
| `ALERTER_MAIL_SENDER` | `servicedesk-schema@istat.it` |
| `ALERTER_MAIL_SENDER_FIXED_DELAY` | `1800000` ms (30 min) |
| `ALERTER_SMTP_SERVER` / `_PORT` | `mail.smtpbucket.com` / `8025` |
| `ALERTER_SMTP_USER` / `_PASSWORD` | `servicedesk-schema@istat.it` / — |
| `ALERTER_SMTP_AUTH` / `_STARTTLS` / `_SSL` | `true` / `true` / `false` |

The complete property list lives in `src/main/resources/application.properties`.

---

## Common commands

The project ships with a [`justfile`](./justfile) for the most frequent commands:

```bash
just                  # list available targets
just stack            # start Virtuoso, Elasticsearch, MySQL via docker compose
just down             # stop the dependencies
just logs             # tail the dependency logs
just clean            # ./gradlew clean + reset Docker volumes
just build            # build without tests
just test             # unit tests
just integration-test # integration tests (Testcontainers)
just check            # all verification tasks
just lint             # checkstyle + spotbugs
just coverage         # JaCoCo + threshold verification
just run              # start the backend (bootRun)
just openapi          # regenerate OpenAPI sources
just docker-build     # build a local container image (Jib)
just dep-check        # OWASP dependency-check
```

All commands can equivalently be invoked via `./gradlew <task>` (see `build.gradle`).

---

## API

The OpenAPI specification lives at [`src/main/resources/public/openapi.yaml`](./src/main/resources/public/openapi.yaml). Once the backend is running, Swagger UI is available at `http://localhost:8080/swagger-ui/index.html`.

### Main public endpoints

| Method | Path | Purpose |
| :-- | :-- | :-- |
| `GET` | `/status` | Application health/status |
| `GET` | `/semantic-assets` | Asset search (filters, pagination) |
| `GET` | `/semantic-assets/by-iri` | Asset details by IRI |
| `GET` | `/vocabularies` | List of controlled vocabularies |
| `GET` | `/vocabularies/{agencyId}/{keyConceptId}` | Vocabulary data |
| `GET` | `/vocabularies/{agencyId}/{keyConceptId}/{id}` | Single vocabulary item |
| `GET` | `/check-url` | URL reachability check |

### Validation endpoints (see [Asset validation](#asset-validation))

| Method | Path | Purpose |
| :-- | :-- | :-- |
| `POST` | `/validate` | One-shot asset validation |
| `POST` | `/validate/syntax` | RDF (Turtle) syntax validation |
| `POST` | `/validate/repo/{owner}/{repo}` | Submit a repository validation job |
| `POST` | `/validate/repo/{owner}/{repo}/{revision}` | Same, on a specific revision |
| `GET` | `/validate/repo/{validationId}` | Validation job status |

### Operational endpoints (Basic Auth)

| Method | Path | Purpose |
| :-- | :-- | :-- |
| `POST` | `/jobs/harvest` | Trigger harvest (requires `HARVESTER_ENDPOINT_ENABLED=true`) |
| `GET` | `/jobs/harvest/run` | List harvest job runs |
| `GET` | `/jobs/harvest/running` | Currently running harvest jobs |
| `POST` | `/jobs/clear` | Clear data for a given repository URL |
| `GET` | `/harvest/vocabularies.db` | Download the aggregated vocabularies SQLite DB |
| `GET/POST` | `/config/repository` | Manage registered repositories |
| `GET` | `/config/repository/{id}/validation-report` | Validation report for a repository |
| `GET` | `/config/repository/{id}/conformance` | Cookiecutter conformance report |
| `GET/POST` | `/config/{repoId}` | List/set per-repo configuration entries |
| `PUT/DELETE` | `/config/{repoId}/{configKey}` | Update/delete a single config entry |
| `GET/POST` | `/dashboard/...` | Dashboard data (raw, time/count aggregates) |
| `*` | `/event`, `/profile`, `/user` | Alerter events, profiles, and users |

---

## Asset validation

The backend exposes a validation pipeline triggered both during harvesting and on demand:

- **Syntax validation (RDF/Turtle)** — Apache Jena RIOT in streaming mode (zero `Model` allocation). Errors and warnings are returned as structured `line`/`col`/`message` entries. Endpoint: `POST /validate/syntax`.
- **DCAT-AP_IT metadata validation** — checks for `dct:license` (ERROR), valid `adms:status` (WARNING), and other mandatory metadata on distributions.
- **Repository conformance** — verifies the repository conforms to the [`teamdigitale/dati-semantic-cookiecutter`](https://github.com/teamdigitale/dati-semantic-cookiecutter) template: presence of CI workflows, `.pre-commit-config.yaml`, expected folder structure. Outcome persisted on `HARVESTER_RUN.CONFORMANCE_REPORT`.
- **Asynchronous repository validation** — `POST /validate/repo/{owner}/{repo}` accepts a job and returns a `validationId`; the client polls `GET /validate/repo/{validationId}` until status is `COMPLETED` or `FAILED`. See `just validate-repo-wait` for an end-to-end example.

All outcomes converge into a unified `ValidationReport` returned by the endpoints above.

---

## Harvesting

The harvester clones each registered repository (via JGit) and runs a sequence of stages tracked in the logs:

1. **Clone / fetch** of the repository (with branch and revision support).
2. **`SYNTAX_VALIDATION`** — RDF parsing; assets with invalid syntax are skipped without aborting the harvest run.
3. **Folder/ontology/vocabulary scan** — applying the configured `skip-words`.
4. **`CONFORMANCE_CHECK`** — cookiecutter template verification (lazy-loading reference files via the GitHub API, optionally with a PAT).
5. **RDF load** into Virtuoso (per graph) and indexing into Elasticsearch.
6. **`vocabularies.db` aggregation** — the aggregated SQLite DB of vocabularies is regenerated at the end of every run (ETag persisted in the `.aggregate-hash` sidecar).
7. **Outcome persistence** on MySQL (`HARVESTER_RUN`) and `ValidationReport` generation.

Repositories are registered in MySQL and managed via `/config/repository`. Manual triggering is exposed at `POST /jobs/harvest` (Basic Auth required, plus `HARVESTER_ENDPOINT_ENABLED=true`).

---

## Container and deployment

The container image is produced with [Jib](https://github.com/GoogleContainerTools/jib) (no `Dockerfile` required):

```bash
just docker-build
# or: ./gradlew jibDockerBuild
```

The base image is `gcr.io/distroless/java17-debian11:nonroot`. The service exposes ports `8080` (HTTP) and `8081` (management actuator).

For a **fully containerized stack** (backend + dependencies) the `backend` section in [`docker-compose.yaml`](./docker-compose.yaml) is commented out and can be enabled once the image is built; the main environment variables are documented inline.

Production deployment is managed via **Kubernetes** (see [`teamdigitale/dati-semantic-kubernetes`](https://github.com/teamdigitale/dati-semantic-kubernetes)).

---

## Code quality and security

- **Checkstyle** — config in [`config/checkstyle/`](./config/checkstyle/), runs on `main`, `test`, `integration` (`just lint`).
- **SpotBugs** — static analysis; runs alongside Checkstyle.
- **JaCoCo** — coverage with threshold verification (`just coverage`).
- **OWASP dependency-check** — dependency vulnerability scanning (`just dep-check`).
- **GitHub Dependabot** — enabled at the `teamdigitale` organization level.
- **Pre-commit hook** — Gradle checkstyle + spotbugs are invoked automatically; see [`config/hooks/pre-commit`](./config/hooks/pre-commit).

Analysis reports are written under `build/reports/`.

---

## Contributing

Before opening a PR:

1. Read the [Code of Conduct](./CODE_OF_CONDUCT.md) and the [CONTRIBUTING](./CONTRIBUTING.md) guide.
2. Create a branch from `main` with a `feat-*`, `fix-*`, or `docs-*` prefix.
3. Run `just check` locally (lint + tests).
4. Open the PR against `main`. CI checks are mandatory.

For **discussions and support**:

- Slack channel [#design](https://developersitalia.slack.com/messages/C7VPAUVB3/) on Developers Italia
- [dati](https://forum.italia.it/c/dati/33) category on forum.italia.it

### Additional documentation

- Official wiki: <https://github.com/teamdigitale/dati-semantic-backend/wiki>

---

## License

This project is released under the **GNU Affero General Public License v3.0 or later**. A copy of the license is available in [`LICENSE`](./LICENSE).
