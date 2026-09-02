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

## Build-Integration in mesh-enterprise

Der Mesh-seitige Teil baut bereits im Reaktor mit. Für den **rag-service** als eigenständig
deploybare Einheit fehlt die Pipeline-Anbindung noch. Vorbild ist durchgehend `mcp-server`,
der denselben Weg schon gegangen ist.

### 1. Maven-Anbindung

**Erledigt** (`53deec18`): `content-rag/pom.xml` hängt am Parent
`com.gentics.mesh.enterprise:mesh-enterprise`, die eigene `<version>` ist entfallen, die
Module erben `3.3.0-SNAPSHOT`. Damit erfasst `MavenHelper.setVersion()` in der Jenkins-Stage
„Set Version" beim Release auch dieses Modul — sonst wäre es auf `0.1.0-SNAPSHOT` stehen
geblieben und `mvn deploy` hätte ein dauerhaft veraltetes Artefakt ins Nexus gelegt.

Offen bleibt:

- `mesh-bom` importieren wie in `mcp-server` und `connectors`, damit Jackson, slf4j und JUnit
  auf demselben Stand liegen wie im übrigen Reaktor.
- `<finalName>mesh-content-rag-service</finalName>` im `rag-service`, damit Dockerfile und
  Jenkinsfile die JAR ohne Versionsnummer im Namen finden — dieselbe Konvention wie
  `mcp-server`.
- `maven-surefire-plugin` im Modul deklarieren, damit die JUnit-5-Tests im Reaktor sicher
  ausgeführt werden.

Zwei Nebenwirkungen der Vererbung, die jetzt gelten und beim ersten vollständigen Build zu
prüfen sind:

- Das `pluginManagement` des Parents wird mitgeerbt. `<release>17</release>` passt, aber die
  `annotationProcessorPaths` mit `dagger-compiler` laufen damit auch über die
  Spring-Boot-Module. Meist folgenlos, kostet aber Build-Zeit; bei Bedarf im `rag-service`
  mit einem leeren `<annotationProcessorPaths/>` abschalten.
- Geerbtes `dependencyManagement` hat in Maven Vorrang vor einem im selben POM **importierten**
  BOM. Überlappende Einträge aus `mesh-enterprise` bzw. `com.gentics:maven-parent` können also
  die Versionen des Spring-Boot-BOM überstimmen. `mvn -pl content-rag/rag-service -am
  help:effective-pom` zeigt, was tatsächlich gewinnt.

### 2. Container-Image

`content-rag/rag-service/Dockerfile` nach dem Muster von `mcp-server/Dockerfile`:

- Basis `eclipse-temurin:17-jre-alpine`, davor eine Stage mit `apk upgrade --no-cache` —
  ohne die scheitert der Trivy-Scan in der Pipeline (`exitCode: 1`).
- Die JAR wird **nicht** im Image gebaut, sondern aus `target/` übernommen; gebaut hat sie
  die Stage „Maven Build".
- Non-root-User (UID/GID 10001), OCI-Labels, `ARG VERSION`,
  `JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+ExitOnOutOfMemoryError"`.
- Anders als beim MCP-Server: das hier ist ein HTTP-Dienst, also `EXPOSE 8090` und ein
  normaler Entrypoint — die stdio-Rücksichten aus dem MCP-Dockerfile entfallen.
- `.dockerignore` nicht vergessen.

### 3. Jenkinsfile.split

Drei Stellen, jeweils analog zu den `mcp-server`-Zeilen:

- **Docker Build**: `docker build --network=host -t <registry>/gentics/mesh-enterprise/content-rag:latest -t …:<version> --build-arg VERSION=<version>` im Modulverzeichnis.
- **Docker Build**, direkt danach: `scanImage` für das neue Image.
- **Deploy**: `docker push` für `:latest` und `:<version>`.

### 4. Laufzeit-Voraussetzungen im Deployment

- **Elasticsearch** — dieselbe Instanz wie die Mesh-Volltextsuche. `ES_INDEX_PREFIX` muss mit
  dem Präfix der Mesh-Suchkonfiguration übereinstimmen, sonst findet der Service die von Mesh
  geschriebenen Indizes nicht.
- **Inference-Server (TEI)** — die konfigurierte URL wird von **Elasticsearch** aufgerufen,
  nicht vom Service. Sie muss also aus Sicht des ES-Clusters auflösbar sein; eine
  Service-lokale Adresse führt hier zu einem Fehler, der erst beim Indexieren auffällt.
- **LLM-Endpunkt** je nach Deployment-Profil.
- Konfiguration ausschließlich über Umgebungsvariablen (`ES_URL`, `ES_INDEX_PREFIX`,
  `TEI_URL`, `LLM_*`), Secrets über den üblichen Weg — nichts davon ins Image.

Zu Health-Checks: der Service startet bewusst auch ohne erreichbares Elasticsearch, damit ein
kurz nicht verfügbarer Cluster keine Absturzschleife auslöst. `/actuator/health` eignet sich
deshalb als **Liveness**-Probe; eine Readiness-Probe, die erst bei erreichbarem Cluster grün
wird, ist noch zu ergänzen. `/actuator/prometheus` ist bereits aktiv.

### 5. Offen

- **Helm/Deployment**: im `helm-chart` von mesh-enterprise gibt es für den MCP-Server nichts;
  dessen Deployment liegt im CMP-Chart. Für den rag-service ist zu klären, wo er hingehört.
- **Cluster-Objekte**: Inference-Endpunkt, Ingest-Pipeline und Index-Template legt der Service
  beim Start selbst an (`rag.inference.bootstrap`). In Umgebungen, in denen der Service keine
  Cluster-Rechte bekommt, muss das stattdessen ein Provisionierungsschritt übernehmen —
  die Definitionen liefert `index-contract`.

`chunking` und `index-contract` brauchen dagegen **kein** eigenes Image: es sind Bibliotheken,
die über die Mesh-Abhängigkeit im Mesh-Artefakt landen. Sie müssen lediglich ins Nexus
deployt werden, was der Reaktor mit Punkt 1 automatisch erledigt.

## REST-API

| Methode | Pfad | Zweck |
|---|---|---|
| POST | `/api/rag/query` | Frage + Filter → Antwort + Quellen |
| GET | `/api/rag/status` | Chunk-Zähler und Chunks ohne Vektor |
| GET | `/api/rag/debug/chunks` | Quellen-/Chunk-Inspektion |

Zwei Endpunkte fehlen absichtlich. Der **Reindex** würde einen zweiten Schreibweg neben dem
Mesh-Sync aufmachen – siehe oben. Und **Feedback** wäre erst mit einer Referenz auf den
Audit-Eintrag der Antwort auswertbar; die gibt `RagAnswer` heute nicht heraus, und ohne eine
Oberfläche, die Feedback erhebt, gäbe es auch niemanden, der ihn aufruft. Wenn er kommt, dann
zusammen mit der Audit-ID in der Antwort und persistenten Stores.

## Stand

Implementiert: Modulstruktur, Domänenmodell, Index-Kontrakt (Benennung, Mapping mit
`int8_hnsw`-quantisiertem Vektorfeld, Ingest-Pipeline, Inference-Endpunkt), strukturbasiertes
Chunking mit Überlappung, Bootstrap der Cluster-Objekte, Hybrid-Retrieval (BM25 + kNN via RRF)
mit `_roleUuids`-Permission-Filter und Gültigkeitszeitraum-Filter, Status-Endpunkt.

Noch offen:

- **Mesh-seitiger Index-Handler** – der Schreibweg selbst, mit dem Mesh-Core-Team abzustimmen
- **`LlmProvider`** – EU-Managed bzw. lokal (Ollama/vLLM), offen bis Modellwahl (O4)
- **Persistente Store-Backends** für Config und Audit – derzeit In-Memory
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
