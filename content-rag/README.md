# Gentics Mesh Content RAG

Retrieval-Augmented-Generation über Gentics-Mesh-Inhalte: semantische Suche und quellenbelegte,
generierte Antworten – ergänzend zum bestehenden MCP-Server. Grundlage ist das Konzept **v0.2**
(`Konzept_Gentics_Mesh_Content_RAG.md`).

## Architektur

Es gibt genau **einen Schreibweg** in den Index, und das ist der Standard-Sync von Gentics Mesh.

```
Gentics Mesh                          Elasticsearch                     rag-service
────────────                          ─────────────                     ───────────
Standard-Sync                                                           (nur lesend)
  ├─ node-…-Indizes  ───────────────▶ Volltextsuche (unverändert)
  └─ RAG-Index-Handler                                                  Query
       chunking      ───────────────▶ …-ragchunk-…                  ┌── Hybrid-Retrieval
       index-contract                   Ingest-Pipeline             │   Permission-Filter
                                        └─ Vektor ◀── TEI           └── LLM ─▶ Antwort
       Full-Sync + periodischer
       Korrektheits-Check
```

Drei Festlegungen tragen das:

**Mesh schreibt, über seinen eigenen Sync.** Kein Push-Pfad, kein eigener Indexer, kein
Reconciler auf RAG-Seite. Voll-Sync (`POST /api/v2/search/sync`) und der periodische
Index-Korrektheits-Check gelten damit unverändert auch für die RAG-Indizes. Das ist auch der
Grund, warum es im Service keinen Reindex-Endpunkt mehr gibt: ein zweiter Auslöser wäre ein
zweites Sync-Paradigma.

**Elasticsearch vektorisiert.** Eine Ingest-Pipeline hängt den Vektor beim Indexieren an. Damit
trägt weder Mesh noch der Service ML-Last, und der Sync-Durchsatz hängt nicht am Embedder – bei
einer Schema-Migration über Millionen Nodes wäre er sonst der limitierende Faktor. Bewusst
Pipeline und nicht `semantic_text`: letzteres würde den Text ein zweites Mal chunken, wort- statt
strukturbasiert, und damit das Chunking entlang von Überschriften und Feldgrenzen entwerten.

**Eigene Indizes in derselben Elasticsearch.** Die Node-Indizes der Volltextsuche bleiben
unangetastet. Gründe im Detail in `index-contract/…/package-info.java`; kurz: RAG braucht
Chunk- statt Node-Granularität, Mesh-Node-Indizes tragen die Schemaversion im Namen (jede
Migration erzwänge sonst einen kompletten Neuaufbau des Vektorbestands), und Vektoren dominieren
die Indexgröße – in eigenen Indizes bleiben Heap-Bedarf, Merge-Verhalten, Snapshot-Größe und
Recovery-Zeit der Suche unverändert, auch für Installationen ohne RAG.

Identisch bleibt, was identisch bleiben soll: dieselbe Elasticsearch, derselbe Schreibmechanismus
und dieselbe Permission-Semantik über `_roleUuids`.

## Module

| Modul | Artifact | Läuft in | Inhalt |
|---|---|---|---|
| `common` | `mesh-content-rag-common` | beiden | Domänenmodell (`Chunk`, `SourceContent`, `RagQuery/RagAnswer`), SPI (`Chunker`, `LlmProvider`, `Retriever`), Stores |
| `chunking` | `mesh-content-rag-chunking` | Mesh | strukturbasiertes Chunking, framework-frei |
| `index-contract` | `mesh-content-rag-index-contract` | beiden | Indexbenennung, Mapping, Ingest-Pipeline, Inference-Endpunkt, Chunk → ES-Dokument |
| `rag-service` | `mesh-content-rag-service` | eigenständig | REST-API (`/api/rag/*`), Hybrid-Retrieval, Antwortgenerierung |

`chunking` und `index-contract` haben bewusst **keine** Spring-, Elasticsearch- oder
JSON-Abhängigkeit: was in den Mesh-Prozess eingebunden wird, soll dort nichts mitbringen.
Definitionen werden als `Map`/`List` zurückgegeben, jede Seite serialisiert mit ihrem eigenen
Jackson.

## Was auf der Mesh-Seite noch zu implementieren ist

Ein zusätzlicher Index-Handler neben den bestehenden (node, tag, user …). Die vier Aufrufe:

```java
// 1. Indexname – ohne schemaVersionUuid, damit eine Migration den Index nicht wechselt
String index = RagIndexNaming.indexName(prefix, projectUuid, branchUuid, RagIndexNaming.PUBLISHED);

// 2. Chunks bilden (SourceContent aus dem Node-Container, wie für das Node-Dokument auch)
List<Chunk> chunks = new StructuralChunker(options).chunk(sourceContent);

// 3. Bulk-Upsert; die deterministischen IDs überschreiben die vorherige Fassung
for (Chunk c : chunks) {
    bulk.index(index, RagChunkDocuments.documentId(c), RagChunkDocuments.toSource(c));
}

// 4. Überzählige Chunks entfernen, falls der Node kürzer geworden ist
deleteByQuery(index, RagChunkDocuments.staleChunksQuery(groupKey, chunks.size()));
```

Beim Löschen oder Depublizieren eines Nodes genügt
`RagChunkDocuments.documentGroupQuery(groupKey)`.

Mapping und Pipeline muss der Handler nicht kennen: das Index-Template greift automatisch, sobald
ein Indexname auf das Pattern passt. Angelegt wird es beim Start des rag-service
(`RagIndexBootstrap`), zusammen mit Inference-Endpunkt und Pipeline – das hängt am
Embedding-Modell und ist damit RAG-Wissen, das im CMS nichts verloren hat.

Offen ist noch die Frage, welche Felder je Schema in den RAG-Index gehen (Allow-List) und wie die
No-RAG-Konvention (K4) ausgewertet wird; beides gehört an dieselbe Stelle im Sync-Pfad.

## Build

Voraussetzung: **JDK 17**, Maven 3.9+.

```bash
mvn clean verify
```

## Lokal starten

```bash
# 1) Infrastruktur (Elasticsearch + TEI-Embeddings)
docker compose up -d

# 2) RAG-Service – legt beim Start Inference-Endpunkt, Pipeline und Index-Template an
mvn -pl rag-service spring-boot:run
# Service auf http://localhost:8090, Health: /actuator/health
```

## REST-API

| Methode | Pfad | Zweck |
|---|---|---|
| POST | `/api/rag/query` | Frage + Filter → Antwort + Quellen |
| GET | `/api/rag/status` | Chunk-Zähler und Chunks ohne Vektor |
| GET | `/api/rag/debug/chunks` | Quellen-/Chunk-Inspektion |
| POST | `/api/rag/feedback` | Nutzerfeedback je Antwort |

Ein Reindex-Endpunkt fehlt absichtlich – siehe oben.

## Stand

Implementiert: Modulstruktur, Domänenmodell, Index-Kontrakt (Benennung, Mapping mit
`int8_hnsw`-quantisiertem Vektorfeld, Ingest-Pipeline, Inference-Endpunkt), strukturbasiertes
Chunking mit Überlappung, Bootstrap der Cluster-Objekte, Hybrid-Retrieval (BM25 + kNN via RRF)
mit `_roleUuids`-Permission-Filter und Gültigkeitszeitraum-Filter, Status-Endpunkt.

Noch offen:

- **Mesh-seitiger Index-Handler** – der Schreibweg selbst, mit dem Mesh-Core-Team abzustimmen
- **`LlmProvider`** – EU-Managed bzw. lokal (Ollama/vLLM), offen bis Modellwahl (O4)
- **Persistente Store-Backends** für Config, Audit und Feedback – derzeit In-Memory
- **Auth** vor der REST-API (O5)

## Zu klärendes Risiko: Elasticsearch-Lizenz

`dense_vector` samt kNN ist im Basic-Tier enthalten, die Inference-Seite nicht durchgängig – für
die Ziel-Version und die Kundenlizenzen ist zu prüfen, ob der Inference-Processor nutzbar ist.

Der Entwurf ist darauf vorbereitet: fällt die ES-seitige Vektorisierung weg, wird ausschließlich
`RagInferenceSetup` ersetzt. Der Vektor käme dann vom Schreiber, das Feld `embedding` bliebe
dasselbe. Dokumentformat, Mapping, Indexbenennung und die gesamte Query-Seite sind davon nicht
betroffen.

## Offene Entscheidungen aus dem Konzept

- **O4** Modellwahl: Embedding (lokal/CPU) und EU-LLM, Benchmark auf realem Content
- **O5** CMP-Auth: Token-/Rollen-Semantik, Mapping Rolle → `_roleUuids`
- **O9** Reuse-Umfang der bestehenden ES-Integration (Mapping, Permission-Filter, Full-Sync)

O8 (Push-Hook) ist mit diesem Umbau gegenstandslos: es gibt keinen Push-Pfad mehr.
