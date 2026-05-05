[![License](https://img.shields.io/github/license/teamdigitale/dati-semantic-backend.svg)](https://github.com/teamdigitale/dati-semantic-backend/blob/main/LICENSE)
[![GitHub issues](https://img.shields.io/github/issues/teamdigitale/dati-semantic-backend.svg)](https://github.com/teamdigitale/dati-semantic-backend/issues)
[![Join the #design channel](https://img.shields.io/badge/Slack%20channel-%23design-blue.svg)](https://developersitalia.slack.com/messages/C7VPAUVB3/)
[![Get invited](https://slack.developers.italia.it/badge.svg)](https://slack.developers.italia.it/)
[![Dati on forum.italia.it](https://img.shields.io/badge/forum-dati-blue.svg)](https://forum.italia.it/c/dati/33)

# National Data Catalog (NDC) — Backend

Backend del **National Data Catalog (NDC) for Semantic Interoperability**, componente della **PDND** (Piattaforma Digitale Nazionale Dati). Il servizio espone un catalogo nazionale degli asset semantici (ontologie, vocabolari controllati, schemi) prodotti dalle Pubbliche Amministrazioni italiane, con harvesting automatico dai repository pubblici, indicizzazione, ricerca, validazione e API di consultazione.

Il backend si compone di:

- un **harvester** che esplora periodicamente i repository pubblicati dai partecipanti al catalogo, scaricando il materiale semantico e popolando indici e database;
- un **triple store** (Virtuoso) interrogabile via SPARQL, dove il materiale RDF viene caricato per grafo;
- un **indice di ricerca** (Elasticsearch) per le ricerche full-text e semantiche;
- una **API REST** (OpenAPI) per ricerca, dettaglio asset, vocabolari controllati, validazione on-demand, dashboard e gestione operativa.

---

## Indice

- [Stack tecnologico](#stack-tecnologico)
- [Prerequisiti](#prerequisiti)
- [Avvio rapido](#avvio-rapido)
- [Configurazione](#configurazione)
- [Comandi principali](#comandi-principali)
- [API](#api)
- [Validazione asset](#validazione-asset)
- [Harvesting](#harvesting)
- [Container e deploy](#container-e-deploy)
- [Qualità del codice e sicurezza](#qualità-del-codice-e-sicurezza)
- [Contribuire](#contribuire)
- [Licenza](#licenza)

---

## Stack tecnologico

| Componente | Versione | Note |
| :-- | :-- | :-- |
| Java | 17 | JDK 17 (CI), runtime distroless `gcr.io/distroless/java17-debian11:nonroot` |
| Spring Boot | 3.5.8 | Web, Data JPA, Data Elasticsearch, Security, Validation, Mail, Actuator |
| Apache Jena | 5.5.0 | Parsing/serializzazione RDF, RIOT, query builder |
| Elasticsearch | 8.14.3 | Indice di ricerca full-text e semantico |
| OpenLink Virtuoso | `tenforce/virtuoso` | Triple store SPARQL |
| MySQL | 9.0 | Metadata harvester (job, run, configurazioni, eventi) |
| Flyway | core + mysql | Migrazioni schema |
| Gradle | wrapper | Build, test, lint, scan |
| OpenAPI Generator | 7.7.0 | Generazione interfacce dallo `openapi.yaml` |
| MapStruct, Lombok | — | Mapping DTO ↔ entity, boilerplate |
| Jib | 3.4.3 | Build immagine container senza Dockerfile |
| OWASP dependency-check | 12.2.2 | Scan vulnerabilità dipendenze |
| SpotBugs, Checkstyle, JaCoCo | — | Analisi statica e coverage |

---

## Prerequisiti

- **JDK 17** (Eclipse Temurin consigliato)
- **Docker** + **Docker Compose** (per le dipendenze locali: Virtuoso, Elasticsearch, MySQL)
- *(Opzionale)* **[just](https://github.com/casey/just)** per i comandi a riga singola (`justfile` incluso nel repo)
- *(Opzionale)* **GitHub Personal Access Token** se si supera il rate limit anonimo dell'API GitHub durante l'harvesting (vedi [Configurazione](#configurazione))

---

## Avvio rapido

Da una shell nella root del repo:

```bash
# 1. Avvia le dipendenze locali (Virtuoso, Elasticsearch, MySQL)
just stack
# in alternativa: docker compose up -d

# 2. Build dell'applicazione (senza test)
just build
# in alternativa: ./gradlew clean build -x test

# 3. Avvia il backend (profilo "local" attivo per default)
just run
# in alternativa: ./gradlew bootRun
```

Il servizio è raggiungibile su `http://localhost:8080`. Documentazione OpenAPI/Swagger UI: `http://localhost:8080/swagger-ui/index.html` (specifica grezza su `/api/v3/api-docs`).

Credenziali di sviluppo (Basic Auth) per gli endpoint protetti dell'harvester:

- utente: `harv-user`
- password: `harv-password`

(Configurabili tramite `HARVESTER_USER` / `HARVESTER_PASSWORD`.)

---

## Configurazione

Il profilo `local` è attivo per default (`spring.profiles.active=local`). Le proprietà principali sono in `src/main/resources/application.properties`; di seguito le variabili d'ambiente più rilevanti.

### Datasource e dipendenze esterne

| Variabile | Default | Descrizione |
| :-- | :-- | :-- |
| `SPRING_DATASOURCE_URL` | `jdbc:mysql://localhost:3306/dev_ndc_harvest` | JDBC URL MySQL |
| `SPRING_DATASOURCE_USERNAME` / `_PASSWORD` | `root` / `example` | Credenziali MySQL |
| `ELASTICSEARCH_HOST` / `_PORT` / `_SCHEME` | `localhost` / `9200` / `https` | Endpoint Elasticsearch |
| `ELASTICSEARCH_USERNAME` / `_PASSWORD` | `elastic` / `changeme` | Credenziali Elasticsearch |
| `VIRTUOSO_SPARQL` | — | Endpoint SPARQL Virtuoso (es. `http://localhost:8890/sparql`) |
| `VIRTUOSO_SPARQL_GRAPH_STORE` | — | Endpoint SPARQL Graph Store |
| `VIRTUOSO_USERNAME` / `_PASSWORD` | `dba` / `dba` | Credenziali Virtuoso |

### Harvesting

| Variabile | Default | Descrizione |
| :-- | :-- | :-- |
| `HARVESTER_USER` / `HARVESTER_PASSWORD` | `harv-user` / `harv-password` | Basic Auth per endpoint operativi |
| `HARVESTER_REPOSITORIES` | — | Elenco URL repo da harvestare (separati da virgola) |
| `HARVESTER_ENDPOINT_ENABLED` | — | Abilita gli endpoint di trigger harvesting |
| `harvester.folder.skip-words` | `scriptR2RML,sparql,deprecated` | Cartelle da ignorare (match per `contains`, min 3 char) |
| `harvester.ontology.scanner.skip-words` | `aligns,example` | Skip ontologie |
| `harvester.controlled-vocabulary.scanner.skip-words` | `transparency-obligation-organization,transparency-obligation-administration` | Skip vocabolari |
| `GITHUB_PERSONAL_ACCESS_TOKEN` | — | PAT GitHub per superare il rate limit dell'API pubblica (lettura cookiecutter, ecc.) |

### Conformance check (cookiecutter)

Verifica che ogni repository harvested aderisca al template ufficiale [`teamdigitale/dati-semantic-cookiecutter`](https://github.com/teamdigitale/dati-semantic-cookiecutter).

| Variabile | Default |
| :-- | :-- |
| `HARVESTER_CONFORMANCE_ENABLED` | `true` |
| `HARVESTER_CONFORMANCE_COOKIECUTTER_REPO` | `teamdigitale/dati-semantic-cookiecutter` |
| `HARVESTER_CONFORMANCE_COOKIECUTTER_BRANCH` | `main` |

### Aggregazione vocabolari (CSV APIs)

L'harvester aggrega periodicamente i database SQLite dei vocabolari in un singolo file `vocabularies.db` esposto su HTTP.

| Variabile | Default |
| :-- | :-- |
| `HARVESTER_AGGREGATE_DB_PATH` | `/tmp/ndc-vocabularies.db` |
| `HARVESTER_AGGREGATE_DB_WORK_DIR` | `/tmp/ndc-csvapis-work` |
| `HARVESTER_AGGREGATE_DB_CACHE_MAX_AGE` | `86400` (secondi) |

### Mailer (alerting)

| Variabile | Default |
| :-- | :-- |
| `ALERTER_MAIL_SENDER` | `servicedesk-schema@istat.it` |
| `ALERTER_MAIL_SENDER_FIXED_DELAY` | `1800000` ms (30 min) |
| `ALERTER_SMTP_SERVER` / `_PORT` | `mail.smtpbucket.com` / `8025` |
| `ALERTER_SMTP_USER` / `_PASSWORD` | `servicedesk-schema@istat.it` / — |
| `ALERTER_SMTP_AUTH` / `_STARTTLS` / `_SSL` | `true` / `true` / `false` |

L'elenco completo delle proprietà è in `src/main/resources/application.properties`.

---

## Comandi principali

Il progetto include un [`justfile`](./justfile) per i comandi più comuni:

```bash
just                  # elenca i target disponibili
just stack            # avvia Virtuoso, Elasticsearch, MySQL via docker compose
just down             # ferma le dipendenze
just logs             # tail dei log delle dipendenze
just clean            # ./gradlew clean + reset volumi Docker
just build            # build senza test
just test             # unit test
just integration-test # test di integrazione (Testcontainers)
just check            # tutte le task di verifica
just lint             # checkstyle + spotbugs
just coverage         # JaCoCo + verifica soglie
just run              # avvia il backend (bootRun)
just openapi          # rigenera i sorgenti OpenAPI
just docker-build     # build immagine container locale (Jib)
just dep-check        # OWASP dependency-check
```

In alternativa, tutti i comandi sono invocabili direttamente con `./gradlew <task>` (vedi `build.gradle`).

---

## API

La specifica OpenAPI è in [`src/main/resources/public/openapi.yaml`](./src/main/resources/public/openapi.yaml). Una volta avviato il backend la Swagger UI è disponibile su `http://localhost:8080/swagger-ui/index.html`.

### Endpoint pubblici principali

| Metodo | Path | Scopo |
| :-- | :-- | :-- |
| `GET` | `/status` | Health/status applicativo |
| `GET` | `/semantic-assets` | Ricerca asset (filtri, paginazione) |
| `GET` | `/semantic-assets/by-iri` | Dettaglio asset per IRI |
| `GET` | `/vocabularies` | Lista vocabolari controllati |
| `GET` | `/vocabularies/{agencyId}/{keyConceptId}` | Dati di un vocabolario |
| `GET` | `/vocabularies/{agencyId}/{keyConceptId}/{id}` | Singolo elemento |
| `GET` | `/check-url` | Verifica raggiungibilità URL |

### Endpoint di validazione (vedi [Validazione asset](#validazione-asset))

| Metodo | Path | Scopo |
| :-- | :-- | :-- |
| `POST` | `/validate` | Validazione one-shot di un asset |
| `POST` | `/validate/syntax` | Validazione sintattica RDF (Turtle) |
| `POST` | `/validate/repo/{owner}/{repo}` | Avvia job di validazione su repo GitHub |
| `POST` | `/validate/repo/{owner}/{repo}/{revision}` | Idem, su revisione specifica |
| `GET` | `/validate/repo/{validationId}` | Stato del job di validazione |

### Endpoint operativi (Basic Auth)

| Metodo | Path | Scopo |
| :-- | :-- | :-- |
| `POST` | `/harvest` | Trigger harvest (richiede `HARVESTER_ENDPOINT_ENABLED=true`) |
| `GET` | `/harvest/running` | Stato job di harvesting |
| `GET` | `/harvest/vocabularies.db` | Download del DB SQLite aggregato dei vocabolari |
| `GET/POST` | `/config/repository` | Gestione repository censiti |
| `GET/POST` | `/config/{repoId}/{configKey}` | Configurazione per repo |
| `GET/POST` | `/dashboard/...` | Dati di dashboard (raw, aggregati per tempo/conteggio) |
| `*` | `/event`, `/profile`, `/user` | Eventi, profili, utenti |

---

## Validazione asset

Il backend espone una pipeline di validazione attivabile sia in fase di harvesting sia on-demand:

- **Validazione sintattica (RDF/Turtle)** — Apache Jena RIOT in modalità streaming (zero allocazione di Model). Errori e warning sono restituiti con `line`/`col`/`message` strutturati. Endpoint: `POST /validate/syntax`.
- **Validazione metadati DCAT-AP_IT** — controllo presenza di `dct:license` (ERROR), `adms:status` valido (WARNING), e altri metadati obbligatori sulle distribuzioni.
- **Conformance del repository** — verifica che il repo aderisca al template [`teamdigitale/dati-semantic-cookiecutter`](https://github.com/teamdigitale/dati-semantic-cookiecutter): presenza di workflow CI, `.pre-commit-config.yaml`, struttura cartelle attesa. Esito persistito su `HARVESTER_RUN.CONFORMANCE_REPORT`.
- **Validazione asincrona di un repository** — `POST /validate/repo/{owner}/{repo}` accetta un job, restituisce un `validationId`, e il client può fare polling su `GET /validate/repo/{validationId}` fino a stato `COMPLETED` o `FAILED`. Vedi `just validate-repo-wait` per un esempio end-to-end.

Tutti gli esiti convergono in un `ValidationReport` unificato consultabile dagli endpoint sopra.

---

## Harvesting

L'harvester clona ogni repository censito (via JGit) ed esegue una sequenza di stage tracciati nei log:

1. **Clone / fetch** del repository (con supporto a branch e revisione).
2. **`SYNTAX_VALIDATION`** — parsing RDF; gli asset con sintassi non valida sono skippati senza interrompere l'harvest.
3. **Scansione folder/ontology/vocabulary** — applicando le `skip-words` configurate.
4. **`CONFORMANCE_CHECK`** — verifica template cookiecutter (lazy-loading dei file di riferimento via GitHub API, opzionalmente con PAT).
5. **Caricamento RDF** in Virtuoso (per grafo) e indicizzazione in Elasticsearch.
6. **Aggregazione `vocabularies.db`** — al termine di ogni run viene rigenerato il DB SQLite aggregato dei vocabolari (ETag persistito su sidecar `.aggregate-hash`).
7. **Persistenza dell'esito** su MySQL (`HARVESTER_RUN`) e generazione del `ValidationReport`.

I repository sono censiti in MySQL e gestibili via `/config/repository`. Il trigger manuale è esposto su `POST /harvest` (richiede Basic Auth e `HARVESTER_ENDPOINT_ENABLED=true`).

---

## Container e deploy

L'immagine container è prodotta con [Jib](https://github.com/GoogleContainerTools/jib) (nessun `Dockerfile` necessario):

```bash
just docker-build
# in alternativa: ./gradlew jibDockerBuild
```

L'immagine base è `gcr.io/distroless/java17-debian11:nonroot`. Il servizio espone le porte `8080` (HTTP) e `8081` (management actuator).

Per lo **stack completo in container** (backend + dipendenze) la sezione `backend` di [`docker-compose.yaml`](./docker-compose.yaml) è commentata e può essere abilitata dopo aver costruito l'immagine; le variabili d'ambiente principali sono già documentate inline.

Il deploy in produzione è gestito via **Kubernetes** (vedi [`teamdigitale/dati-semantic-kubernetes`](https://github.com/teamdigitale/dati-semantic-kubernetes)).

---

## Qualità del codice e sicurezza

- **Checkstyle** — configurazione in [`config/checkstyle/`](./config/checkstyle/), eseguita su `main`, `test`, `integration` (`just lint`).
- **SpotBugs** — analisi statica; eseguita insieme a Checkstyle.
- **JaCoCo** — coverage con verifica soglie (`just coverage`).
- **OWASP dependency-check** — scan vulnerabilità delle dipendenze (`just dep-check`).
- **GitHub Dependabot** — abilitato a livello di organizzazione `teamdigitale`.
- **Pre-commit hooks** — Gradle checkstyle + spotbugs sono invocati automaticamente; vedi `scripts/`.

I report di analisi finiscono in `build/reports/`.

---

## Contribuire

Prima di aprire una PR:

1. Leggi la [Code of Conduct](./CODE_OF_CONDUCT.md) e il [CONTRIBUTING](./CONTRIBUTING.md).
2. Crea un branch dal `main` con prefisso `feat-*`, `fix-*` o `docs-*`.
3. Esegui localmente `just check` (lint + test).
4. Apri la PR verso `main`. Le verifiche CI sono obbligatorie.

Per **discussioni e supporto**:

- canale Slack [#design](https://developersitalia.slack.com/messages/C7VPAUVB3/) di Developers Italia
- categoria [dati](https://forum.italia.it/c/dati/33) sul forum.italia.it

### Documentazione aggiuntiva

- Wiki ufficiale: <https://github.com/teamdigitale/dati-semantic-backend/wiki>
- Demo harvesting su branch / revisione: [`demo-harvesting-branch-curl.md`](./demo-harvesting-branch-curl.md)
- Roadmap features: [`FEATURES.md`](./FEATURES.md)

---

## Licenza

Questo progetto è rilasciato sotto licenza **GNU Affero General Public License v3.0 o successive**. Una copia della licenza è disponibile in [`LICENSE`](./LICENSE).
