# Gentics Mesh Content RAG

Retrieval-Augmented-Generation-System über Gentics-Mesh-Inhalte. Macht veröffentlichte
Mesh-Inhalte über semantische Suche und quellenbelegte, generierte Antworten zugänglich –
ergänzend zum bestehenden MCP-Server.

Grundlage ist das Konzept **v0.2** (`Konzept_Gentics_Mesh_Content_RAG.md` im Projektordner).
Dieses Repository enthält das **Projektgerüst** (Schritt 4 aus „Nächste Schritte" des Konzepts):
Maven-Multimodul, Kerninterfaces, REST-API-Skizze, Stub-Implementierungen und lokaler Stack.

## Architektur (Kurzform)

Hybrid-Deployment aus zwei deploybaren Einheiten plus Infrastruktur:

- **mesh-bridge** – schlanke Mesh-Integrationsschicht. Push von Änderungen + Rollen an den
  RAG-Indexer (Reuse der bestehenden Mesh-Elasticsearch-Search-Integration), Full-Sync-Trigger.
  Keine ML-Last im CMS-Prozess.
- **rag-service** – eigenständiger Spring-Boot-Service. Ingestion → Normalisierung → Chunking →
  Embedding → Indexer sowie Query → Hybrid-Retrieval → Permission-Filter → LLM → Antwort.
- **common** – framework-freies Domänenmodell, Kerninterfaces und Store-Abstraktionen.
- **Elasticsearch** – gemeinsame Such-/Vektor-Plattform (BM25 + kNN via RRF), zugleich
  Default-Speicher für Betriebsdaten (kein zusätzlicher Datastore).

## Module

| Modul | Artifact | Inhalt |
|---|---|---|
| `common` | `mesh-content-rag-common` | Modell (`Chunk`, `SourceContent`, `RagQuery/RagAnswer`, `ChunkState`, …), SPI (`EmbeddingProvider`, `LlmProvider`, `ContentReader`, `Chunker`, `Indexer`, `Retriever`), Stores (`SyncStateStore`, `ConfigStore`, `AuditStore`, `FeedbackStore`) |
| `mesh-bridge` | `mesh-content-rag-bridge` | Push-Kontrakt (`PushEvent`), `RagIngestionGateway`, `FullSyncTrigger` |
| `rag-service` | `mesh-content-rag-service` | Spring-Boot-App, REST-API (`/api/rag/*`), Orchestrierung, Stub-/In-Memory-Implementierungen |

## Build

Voraussetzung: **JDK 17**, Maven 3.9+.

```bash
mvn clean verify
```

## Lokal starten

```bash
# 1) Infrastruktur (Elasticsearch + TEI-Embeddings)
docker compose up -d

# 2) RAG-Service
mvn -pl rag-service spring-boot:run
# Service auf http://localhost:8090, Health: /actuator/health
```

## REST-API (Skizze, Konzept Abschnitt 9)

| Methode | Pfad | Zweck |
|---|---|---|
| POST | `/api/rag/query` | Frage + Filter → Antwort + Quellen (optional Streaming/Debug) |
| POST | `/api/rag/reindex` | Reindex Projekt (Full-Sync-Reuse) oder einzelner Node |
| GET | `/api/rag/status` | Index-/Sync-Status, Zähler, Fehler |
| GET | `/api/rag/debug/chunks` | Quellen-/Chunk-Inspektion |
| POST | `/api/rag/feedback` | Nutzerfeedback je Antwort |

## Status: Was ist Gerüst, was fehlt noch

Implementiert: Modulstruktur, Domänenmodell, Interfaces, REST-API, Ingestion-Orchestrierung
mit No-Op-Erkennung (`contentHash`), strukturbasiertes Chunking, In-Memory-Stores.

Neu konkret implementiert (echte ES-/HTTP-Anbindung):

- `ElasticsearchIndexer` → Index-pro-Projekt, dense_vector-Mapping, Delete-by-Query je
  Document-Group + Bulk-Index (K5/V3). ES-Zugriff über Low-Level-REST-Client (`ElasticsearchGateway`).
- `ElasticsearchRetriever` → Hybrid BM25 + kNN, manuelles RRF, Metadaten- und
  **`readRoleUuids`-Permission-Filter** exakt nach dokumentiertem Mesh-Modell inkl. `public` (K3),
  plus Gültigkeitszeitraum-Filter zur Query-Zeit (V1).
- `TeiEmbeddingProvider` → HuggingFace TEI `/embed`, e5-Passage/Query-Prefixe, Batching + Retry.

Noch **Stub** (markiert mit `SCAFFOLD` / `UnsupportedOperationException`):

- `ContentReader` → Mesh-GraphQL-Read, HTML→Markdown, Referenzauflösung (V7/V8)
- `LlmProvider` → EU-Managed bzw. lokal (Ollama/vLLM) — offen bis Modellwahl (O4)
- `FullSyncTrigger` → Anbindung an Mesh `POST /api/v2/search/sync` — offen bis Push-Hook (O8)
- Persistente Store-Backends (Elasticsearch-Indizes; optional JPA) — derzeit In-Memory

Damit ist der **Query-Pfad ab vorhandenem Index funktionsfähig** (Retrieval + Permission-Filter);
es fehlt für einen End-to-End-Lauf nur noch der `ContentReader` (Befüllung) und der `LlmProvider`
(Antwortgenerierung).

## Offene Entscheidungen vor weiterer Umsetzung (Konzept 16/17)

- **O8** Push-Hook: bestehende Mesh-Search-Integration erweitern vs. paralleler Push-Pfad via Plugin
- **O9** Reuse-Umfang: welche Teile der bestehenden ES-Integration (Mapping, Permission-Filter,
  Full-Sync) sind 1:1 übernehmbar → Code-Review der Search-Integration
- **O4** Modellwahl: konkretes Embedding (lokal/CPU) + EU-LLM, Benchmark auf realem Content
- **O5** CMP-Auth: Token-/Rollen-Semantik, Mapping Rolle → `readRoleUuids`

## MVP-Abgrenzung

Siehe Konzept Abschnitt 14 (veröffentlichte Nodes, Schema-/Feldauswahl, mehrsprachiges
Chunking, Embedding + Hybrid-Suche auf Elasticsearch, Metadatenfilter, Antwort mit Quellen,
No-RAG-Flag, manueller Reindex + Chunk-State, Basis-Monitoring, Rollen-Filter im Index).
