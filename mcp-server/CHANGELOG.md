# Changelog

Alle relevanten Änderungen werden hier dokumentiert.

Format: [Keep a Changelog](https://keepachangelog.com/de/1.1.0/).

Seit dem Umzug in `mesh-enterprise` (siehe Eintrag unten) folgt die Versionierung
der Mesh-Version – ein eigenes Semver für den MCP-Server wird nicht mehr geführt.

## [Unreleased]

### Added
- **API-Key-Authentifizierung gegen Mesh** (`MESH_API_KEY`) als Alternative zu
  `MESH_USERNAME` + `MESH_PASSWORD`. Der Key (langlebiges JWT aus Mesh) wird per
  `MeshRestClient.setAPIKey()` als Bearer-Header an jede Anfrage gehängt – kein
  Login-Roundtrip, kein Passwort im Deployment. Sind beide Varianten gesetzt,
  gewinnt der API-Key (mit Warnung im Log). Beide Wege werden beim Start gegen
  Mesh verifiziert, damit falsche Credentials sofort auffallen.

### Changed
- **Java-Baseline von 21 auf 17** gesenkt – identisch zum `mesh-enterprise`-Parent
  (`<release>17</release>`). Der Modul-eigene `maven-compiler-plugin`-Block entfällt,
  das Level kommt jetzt aus dem Parent. Der HTTP-Transport nutzt statt eines
  Virtual-Thread-Executors einen beschränkten Plattform-Thread-Pool
  (`max(16, MCP_MESH_MAX_CONCURRENT × 2)`), damit weiterhin `MeshCallGuard` und
  nicht der Pool die Concurrency begrenzt. Docker-Images und Jenkins-JDK auf 17.

### Fixed
- **Rate-Limit war wirkungslos**: `RateLimiter.acquirePermission(int)` liefert in
  Resilience4j 2.x ein `boolean` und wirft *kein* `RequestNotPermitted`. Der
  Rückgabewert wurde ignoriert, die `catch`-Blöcke waren toter Code – weder das
  globale noch das Per-Token-Limit hat je eine Anfrage abgelehnt. Bulkhead und
  Circuit Breaker waren nicht betroffen.
- Testabhängigkeiten (`junit-jupiter`, `mockito-junit-jupiter`) hatten keine
  Version: `mesh-bom` verwaltet nur `mockito-core` (2.18.3). Das Modul war damit
  nicht baubar. Jetzt explizit gepinnt (JUnit 5.10.2 / Mockito 5.7.0).
- Use-Cases deklarierten `throws Exception`, was durch den `Supplier` in
  `MeshCallGuard.execute()` nicht durchgereicht werden kann – der `ToolDispatcher`
  hat deshalb nie kompiliert. Serialisierung läuft jetzt über `ToolResult.json()`
  und wirft `AppError(INTERNAL_ERROR)`.
- **Umzug nach `mesh-enterprise`** als Submodul unter `mcp-server/`.
  Versionierung übernimmt der Mesh-Enterprise-Parent-POM
  (`<parent>`/`${revision}`); eigene Semver entfällt.
- `mesh-rest-client`-Version kommt jetzt aus dem `<dependencyManagement>`
  des Parents – damit folgt MCP automatisch jedem Mesh-Release.
- Container-Image: Namespace gewechselt zu `repo.gentics.com/mesh/mcp-server`,
  Image-Tag = Mesh-Version. Dockerfile nimmt `VERSION` als Build-ARG.
- JAR-`finalName` ohne Versions-Suffix (`mesh-mcp-server.jar`), damit
  Jenkins/Dockerfile stabile Pfade haben.
- Server-Version wird zur Laufzeit aus dem JAR-Manifest gelesen
  (`Implementation-Version`); kein Hardcoding mehr in `Main`.

### Removed
- **Eigenes Helm-Chart** entfällt. Deployment läuft über das Helm-Chart
  von Mesh als Sub-Chart (`mcpServer:`-Werte dort dokumentiert).
- **GitHub-Actions-Workflows** (`.github/workflows/`) entfernt – CI läuft
  im Gentics-internen Jenkins. Neu: `Jenkinsfile` am Modul-Root.
- Lokale Dev-Infra (`docker-compose.yml`, `docker/mesh/`, `drivers/`)
  entfernt – mesh-enterprise hält den autoritativen Dev-Stack.
- Eigene `<repositories>`-Sektion im POM raus (Parent definiert
  `maven.gentics.com`).

### Added
- `Jenkinsfile` (declarative pipeline): Maven Build + Tests,
  Docker-Build mit VERSION aus POM, Push nach `repo.gentics.com` auf
  `main`/`release/*`/Tags.
- **Mesh-Schutz-Layer** (`MeshCallGuard`) — protect Mesh from being killed
  by runaway LLM tool-call loops:
  - Token-Bucket-Rate-Limit pro Bearer-Token + global, mit Tool-Gewichtung
    (`mesh_run_graphql` kostet 5 Permits, `mesh_list_projects` nur 1).
  - Bulkhead: maximal `MCP_MESH_MAX_CONCURRENT` parallele in-flight Mesh-Calls.
  - Circuit Breaker: bei zu vielen Mesh-Fehlern öffnet der Breaker und gibt
    für die Cooldown-Dauer sofort 503 zurück, statt Mesh weiter zu belasten.
    Client-side Faults (Validation, Forbidden, NotFound) zählen nicht.
  - Per-Request-Cap: `MCP_GRAPHQL_MAX_QUERY_LENGTH` lehnt pathologisch lange
    GraphQL-Queries ab, bevor Mesh sie sieht.
  - Neue Error-Codes: `RATE_LIMITED` (HTTP 429), `BUSY` (429), `CIRCUIT_OPEN`
    (503) — mit `Retry-After`-Header.
  - Neue Prometheus-Metriken: `mcp_rate_limit_rejected_total`,
    `mcp_concurrency_rejected_total`, `mcp_circuit_breaker_rejected_total`,
    `mcp_circuit_breaker_state`.
- Resilience4j (Rate-Limiter, Bulkhead, Circuit-Breaker) als Dependencies.

## [0.2.0] – 2026-06-02

### Removed
- TypeScript-Implementierung vollständig entfernt (Node-Toolchain,
  `src/`-TS-Quellen, `test/`-Vitest-Suites, `package.json`,
  `pnpm-lock.yaml`, `tsconfig.json`, `vitest.config.ts`, ESLint/Prettier-
  Configs, alte TS-Demo-Scripts, TS-Showcase-Server, TS-orientierte
  Root-`README.md`, `DOCS.md`, `docs.html`).
- Repo-Layout flachgeklopft: ehemaliges `java-server/`-Unterverzeichnis
  ist jetzt der Repo-Root. Pfade in CI-Workflows, Helm-Chart und Dockerfile
  ohne Prefix.

### Added
- HTTP-Transport (`MCP_TRANSPORT=http` oder CLI-Flag `--http`) als gleichwertige
  Alternative zu stdio – ermöglicht Remote-Anbindung an OpenAI Agents SDK,
  LangChain, n8n, Open WebUI, Cursor/Continue und private/self-hosted LLMs.
- Bearer-Token-Authentifizierung im HTTP-Modus (`MCP_AUTH_TOKEN`); konstante
  Vergleichszeit über `MessageDigest.isEqual`. `AuthValidator`-Interface als
  Erweiterungspunkt für OAuth 2.1 / JWT in v0.3.
- Operationale HTTP-Endpoints: `GET /healthz`, `GET /readyz`,
  `GET /metrics` (Prometheus-Textformat, eigene Lightweight-Registry).
- Konfigurierbares CORS (`MCP_CORS_ORIGINS`) inkl. Wildcard-Modus.
- Helm-Chart erweitert um Transport-Branching: optionaler `Service`,
  optionaler `Ingress`, HTTP-Liveness/Readiness, Auth-Secret-Erzeugung
  bzw. Referenz auf `existingAuthSecret`, NetworkPolicy-Ingress-Regeln.
- `ToolDispatcher` als transport-agnostische Registry; sowohl stdio als
  auch HTTP nutzen denselben Dispatch-Pfad – Tool-Surface ist garantiert
  identisch über beide Transports.

### Changed
- Default-Image-Repository im Helm-Chart auf `repo.gentics.com/...`
  umgestellt.
- Versionsstring im `serverInfo` der MCP-Initialize-Antwort auf `0.2.0`.
- Chart-Beschreibung und Keywords spiegeln Multi-Transport wider.

### Removed
- Keine Breaking Changes für bestehende stdio-Clients (Claude Desktop & Co.
  funktionieren ohne Konfigurationsänderung).

### Lizenz
- Dieses Release ist **lizenzrechtlich noch nicht final**. Die OCI-Labels
  weisen Apache-2.0 aus; eine offizielle `LICENSE`-Datei folgt vor dem
  öffentlichen Release. Bis dahin gilt die Verteilung als intern.

## [0.1.0] – 2026-03-30

### Added
- Erstes Release der Java-Implementierung mit stdio-Transport.
- Sechs MCP-Tools: `mesh_list_projects`, `mesh_list_schemas`,
  `mesh_get_node`, `mesh_search_nodes`, `mesh_get_webroot`,
  `mesh_run_graphql`.
- Policy-Layer (Project-/Schema-/Language-Allowlist, Read-Only-Default,
  GraphQL-Mutation-Block, Depth-Limit).
- Docker-Build (Multi-Stage, Non-Root, Eclipse Temurin 21).
- Helm-Chart (stdio-only).
