# Gentics Mesh MCP Server

> Model-Context-Protocol-Adapter für [Gentics Mesh CMS](https://www.gentics.com/mesh/).
> Java 17, stdio + HTTP-Transport, identische Tool-Surface über beide Transports.
>
> **Submodul von `mesh-enterprise`** (Pfad im Reactor: `mcp-server/`).
> Versionierung, Helm-Chart, Container-Registry und Release-Kadenz folgen dem
> `mesh-enterprise`-Parent.

📖 **[Developer Documentation](./docs.html)** – ausführliche HTML-Doku im Gentics-CI mit Tool-API, Transport-Vergleich, Config-Referenz und Integration-Cookbook. Im Browser öffnen: `open docs.html`.

| Transport | Wofür                                                                            |
|-----------|----------------------------------------------------------------------------------|
| `stdio`   | Lokale Child-Prozess-Clients: **Claude Desktop**, Cursor, Continue, Zed          |
| `http`    | Remote-Clients: **OpenAI Agents SDK**, LangChain, n8n, Open WebUI, eigene LLM-Stacks |

Die Tool-Surface ist in beiden Modi identisch (gemeinsamer `ToolDispatcher`).

## Voraussetzungen

| Anforderung   | Version                                                              |
|---------------|----------------------------------------------------------------------|
| Java          | ≥ 17 (Baseline des `mesh-enterprise`-Parents)                        |
| Maven         | ≥ 3.9                                                                |
| Gentics Mesh  | Version aus `mesh-enterprise`-Parent (`mesh-rest-client` geerbt)     |

## Build im Reactor von mesh-enterprise

```bash
# Vom mesh-enterprise-Repo-Root:
mvn -B -ntp -pl mcp-server -am verify

# Resultat:
#   mcp-server/target/mesh-mcp-server.jar   (geshadete Fat-JAR)
```

`finalName` ist bewusst `mesh-mcp-server` ohne Versions-Suffix – der Image-Tag
trägt die Version über das `VERSION`-Build-Arg im Dockerfile.

## Lokal starten

```bash
cp .env.example .env
# Pflichtfelder editieren: MESH_BASE_URL, MESH_ALLOWED_PROJECTS
# und die Mesh-Authentifizierung: MESH_API_KEY *oder* MESH_USERNAME + MESH_PASSWORD
export $(grep -v '^#' .env | xargs)

# stdio (default)
java -jar target/mesh-mcp-server.jar

# HTTP
MCP_TRANSPORT=http MCP_AUTH_TOKEN=secret-please-rotate \
  java -jar target/mesh-mcp-server.jar
```

## Mesh-Authentifizierung

Zwei Varianten, eine davon ist Pflicht:

| Variante | ENV | Verhalten |
|----------|-----|-----------|
| **API-Key** (empfohlen für Prod) | `MESH_API_KEY` | Von Mesh ausgestelltes langlebiges JWT, wird als Bearer-Header an jede Anfrage gehängt. Kein Login-Roundtrip, kein Passwort im Deployment. |
| Username/Passwort | `MESH_USERNAME` + `MESH_PASSWORD` | Login beim Start, Client hält das JWT der Session. |

Sind beide gesetzt, gewinnt der API-Key (der Server loggt eine Warnung). Beide
Varianten werden beim Start gegen Mesh verifiziert – falsche Credentials brechen
den Start ab, statt erst beim ersten Tool-Call zu scheitern.

## Transport-Modi im Detail

### stdio

Der Server liest JSON-RPC von stdin, schreibt nach stdout. Genau ein Client pro Prozess. Wird vom Client (z.B. Claude Desktop) als Child-Prozess gestartet. Keine Auth nötig – das Spawning impliziert Vertrauen.

### HTTP

Endpoint: `POST http://<host>:<port>/mcp` mit JSON-RPC 2.0 im Body.
Auth: `Authorization: Bearer <MCP_AUTH_TOKEN>`.
Zusätzlich: `GET /healthz`, `GET /readyz`, `GET /metrics`.

| ENV                   | Default     | Beschreibung                                                  |
|-----------------------|-------------|---------------------------------------------------------------|
| `MCP_TRANSPORT`       | `stdio`     | `stdio` oder `http`                                           |
| `MCP_HTTP_HOST`       | `0.0.0.0`   | Bind-Host                                                     |
| `MCP_HTTP_PORT`       | `3001`      | Bind-Port                                                     |
| `MCP_AUTH_TOKEN`      | –           | Pflicht im HTTP-Modus (außer Auth ist explizit deaktiviert)   |
| `MCP_AUTH_DISABLED`   | `false`     | `true` nur wenn ein Reverse-Proxy die Auth übernimmt          |
| `MCP_CORS_ORIGINS`    | –           | Kommagetrennte Origin-Allowlist; `*` erlaubt alles            |

CLI-Flags `--http` / `--stdio` überschreiben `MCP_TRANSPORT`.

> **Hinweis zu Streamable HTTP:** Aktuell Request/Response über `POST /mcp` (für alle Tool-Calls völlig ausreichend). Server-initiierte SSE-Streams für Progress-Events sind als Folge-Iteration geplant.

## Mesh-Schutz (Rate-Limit / Bulkhead / Circuit Breaker)

Damit ein LLM mit einer Retry-Schleife Mesh nicht überlasten kann, schützt
`MeshCallGuard` jeden Tool-Call in drei Schichten:

| Layer | Was | Default | Bei Überschreitung |
|---|---|---|---|
| Rate-Limit (Token-Bucket) | Pro Bearer-Token + global, gewichtet pro Tool (graphql=5, search=3, get=2, list=1) | 20/s pro Token, 50/s global | `RATE_LIMITED` → HTTP 429 + `Retry-After` |
| Bulkhead | Max gleichzeitige in-flight Mesh-Calls | 10 | `BUSY` → HTTP 429 + `Retry-After` |
| Circuit Breaker | Öffnet bei ≥ 50 % Fehlerquote über 20-Call-Fenster | 30 s Cooldown | `CIRCUIT_OPEN` → HTTP 503 + `Retry-After` |

Zusätzlich kappen Per-Request-Caps Einzel-Calls: `MESH_GRAPHQL_MAX_DEPTH=6`,
`MCP_GRAPHQL_MAX_QUERY_LENGTH=4000`, `mesh_search_nodes.limit` max 20.

Verhalten: **Fail-Fast** — kein Warten, kein Pufferspeicher. Clients sehen 429/503
mit `Retry-After` und müssen exponentiell zurückwarten. Client-side Faults
(VALIDATION_ERROR, FORBIDDEN, NOT_FOUND) zählen nicht gegen den Circuit Breaker.
Prometheus-Metriken (`mcp_rate_limit_rejected_total`, `mcp_concurrency_rejected_total`,
`mcp_circuit_breaker_state`) machen Limit-Treffer sichtbar.

Vollständige Tuning-Tabelle: siehe `.env.example` und `docs.html`.

## Deployment

Der MCP-Server wird **mit dem Mesh-Helm-Chart** ausgeliefert (siehe
`mesh-enterprise/helm-chart/`, Section `mcpServer:` in `values.yaml`).
Default ist `mcpServer.enabled=false`. Container-Image:

```
repo.gentics.com/mesh/mcp-server:<mesh-version>
```

Aktivierung im Mesh-Release:

```yaml
# values.yaml fürs Mesh-Chart
mcpServer:
  enabled: true
  image:
    repository: repo.gentics.com/mesh/mcp-server
    pullSecret: docker-gentics-com
  transport:
    mode: http
    auth:
      token: "REPLACE-WITH-OPENSSL-RAND-BASE64-32"
  mesh:
    username: "admin"
    password: "YOURPW"            # in Prod: existingSecret
    allowedProjects: "demo"
  ingress:
    enabled: true
    hosts:
      - host: mesh-mcp.example.com
        paths: ["/"]
```

Vollständige Werte: `mesh-enterprise/helm-chart/values.yaml` (Section
`mcpServer:`) und `mesh-enterprise/helm-chart/example-values.yaml`.

Lokaler Container-Build (für Smoke-Tests):

```bash
VERSION=$(mvn -q help:evaluate -Dexpression=project.version -DforceStdout)
docker build --build-arg VERSION="$VERSION" -t mesh-mcp-server:dev .
```

## Verfügbare Tools

| Tool                  | Beschreibung                                             |
|-----------------------|----------------------------------------------------------|
| `mesh_list_projects`  | Alle erlaubten Projekte                                  |
| `mesh_list_schemas`   | Schemas eines Projekts                                   |
| `mesh_get_node`       | Einzelner Node per UUID                                  |
| `mesh_search_nodes`   | Volltextsuche (benötigt Elasticsearch)                   |
| `mesh_get_webroot`    | Node per URL-Pfad                                        |
| `mesh_run_graphql`    | Read-only GraphQL-Query                                  |

Vollständige Schemas und Beispiel-Responses: siehe `docs.html`.

## Client-Cookbook (Auszug)

Siehe `docs.html` für die vollständigen Snippets (Claude Desktop, Cursor/Continue/Zed, OpenAI Responses API, LangChain MCP-Adapters, n8n, Open WebUI, vLLM/TGI).

```bash
# Quick curl gegen die HTTP-Variante:
TOKEN=secret-please-rotate
curl -s http://localhost:3001/mcp \
  -H "Authorization: Bearer $TOKEN" \
  -H 'Content-Type: application/json' \
  -d '{"jsonrpc":"2.0","id":1,"method":"tools/list"}' | jq
```

## Architektur

```
Main.java
  └── MeshConfig.load()          ← Env-Validierung inkl. Transport-Felder
  └── MeshClient                 ← Mesh-Operationen
        ├── MeshRestClient
        ├── MeshGraphqlClient
        └── MeshMapper
  └── ToolDispatcher             ← transport-agnostisch (6 Tools)
        ├── ListProjectsUseCase
        ├── ListSchemasUseCase
        ├── GetNodeUseCase
        ├── SearchNodesUseCase
        ├── GetWebrootUseCase
        └── RunGraphqlUseCase
  └── McpServerRunner (stdio)    ← MCP-SDK + Stdio
  └── HttpMcpServer  (http)      ← JDK HttpServer + JSON-RPC + Auth + CORS + Metrics
        ├── BearerTokenValidator (constant-time)
        └── Metrics              (Prometheus-Textformat)
```

## Tests

```bash
mvn -B -ntp -pl mcp-server test
```

Drei dedizierte Tests decken die HTTP-Schicht ab:
`BearerTokenValidatorTest`, `MetricsTest`, `HttpMcpServerDispatchTest`.

## CI

`Jenkinsfile` definiert die Modul-Pipeline (Build, Test, Docker, Push nach
`repo.gentics.com`). Eingebunden über die Jenkins-Multibranch-Konfiguration
von `mesh-enterprise`.

## Lizenz

Wird über `mesh-enterprise` geregelt – keine eigene `LICENSE`-Datei nötig.
