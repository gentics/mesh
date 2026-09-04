# Mesh-seitiger RAG-Index-Handler

Ergänzung zu `content-rag/README.md`, Abschnitt „Der Mesh-seitige Sync".

Geprüft gegen: `mesh-enterprise` @ `f-gpu-2581` (`a2ba4748`, mesh.version `3.3.0-SNAPSHOT`) und
`gentics/mesh` @ `f-gpu-2581` (aus `dev`, `de01ed9d85`), Modul `elasticsearch`.

## Stand (04.09.2026)

Umgesetzt ist der Weg **(c)** aus Abschnitt 1: zwei generische Dagger-Multibindings im OSS-Mesh,
die Handler in mesh-enterprise.

| Wo | Was |
| --- | --- |
| `gentics/mesh` @ `f-gpu-2581` | `@Multibinds Set<IndexHandler<?>>` und `Set<EventHandler>` in `CommonBindModule`; `IndexHandlerRegistryImpl` und `MainEventHandler` hängen die Sets an ihre Listen an — kein RAG-Wissen im OSS-Teil |
| `content-rag/mesh-sync` | Feldauswahl (`RagFieldSelector`), HTML→Markdown (`HtmlToMarkdown`), `RagSourceContentBuilder`, `RagChunkWriter` und die beiden Handler |
| `mesh-enterprise-core` | `EnterpriseSearchModule` bindet die Handler in die Sets und stellt die Parameter bereit |

**Noch nicht gebaut.** Aus Cowork ist kein Maven-Build möglich (kein Netz zu Maven Central,
lokal nur ein JRE). Verifiziert sind die Mesh-freien Teile mit `javac --release 17 -Xlint:all
-Werror` und einem Probe-Main über alle Regelfälle; die Signaturen der Mesh-API sind gegen die
OSS-Quellen geprüft, aber nicht compiliert. Der erste Reaktor-Build ist der eigentliche Test —
erwartbare Nacharbeit: Dao-Methodennamen im `RagSourceContentBuilder` und die Generics in
`RagChunkIndexHandler`.

---

## 0. Korrektur einer naheliegenden Fehlentscheidung

Ohne die README würde man den Handler als reinen `EventHandler` bauen — das ist die Stelle, an
der Mesh Node-Änderungen in Elasticsearch-Requests umsetzt, und `IndexHandler` sieht auf den
ersten Blick nach viel Interface für nichts aus.

Das wäre hier falsch. Die README legt fest:

> Voll-Sync (`POST /api/v2/search/sync`) und der periodische Index-Korrektheits-Check gelten
> damit unverändert auch für die RAG-Indizes.

Beide laufen ausschließlich über `IndexHandlerRegistry.getHandlers()`
(`SyncEventHandler`, `CheckIndicesHandler`, `AdminIndexHandler`). Wer dort nicht registriert
ist, wird von `/search/sync` und `/search/status` nicht gesehen. Die Zusage aus dem Konzept
verlangt also **beides** — genau wie auf der Node-Seite, wo `NodeIndexHandlerImpl` (Lebenszyklus
und Sync) und `NodeContentEventHandler` (Deltas) nebeneinander stehen:

| Klasse | Aufgabe |
| --- | --- |
| `RagChunkIndexHandler implements IndexHandler<HibNodeFieldContainer>` | `init()`, `syncIndices()`, `check()`, `filterUnknownIndices()`, `getMetrics()` — Voll-Sync und Aufräumen |
| `RagChunkEventHandler implements EventHandler` | Node-Events → Bulk-Upsert + `delete_by_query` — der Delta-Pfad |

Beide teilen dieselbe Kernmethode `upsert(container, project, branch)`, damit Delta- und
Voll-Indexierung nicht auseinanderlaufen können.

---

## 1. Wo der Code hinkommt — die eigentliche Architekturfrage

`RagIndexNaming`, `RagChunkDocuments`, `StructuralChunker`, `Chunk` und `SourceContent` liegen in
`mesh-content-rag-index-contract`, `-chunking` und `-common`, also im **mesh-enterprise**-Reaktor.
`IndexHandlerRegistryImpl` und `MainEventHandler` liegen im **OSS**-Modul `elasticsearch`, beide
mit fest verdrahteter `@Inject`-Konstruktorliste:

```java
// OSS: IndexHandlerRegistryImpl
public IndexHandlerRegistryImpl(NodeIndexHandlerImpl …, UserIndexHandlerImpl …, /* 9 Handler */) { … }
@Override public List<IndexHandler<?>> getHandlers() { return Arrays.asList(nodeIndexHandler, …); }

// OSS: MainEventHandler
private Map<MeshEvent, List<EventHandler>> createHandlers() {
    return Stream.of(syncEventHandler, …, nodeContentEventHandler, …)
        .collect(toMultiMap(EventHandler::handledEvents));
}
```

Der Handler kann damit nicht in mesh-enterprise liegen und sich selbst anmelden. Drei Wege:

**(a) Handler in OSS mesh.** Dann müsste OSS mesh auf `mesh-content-rag-*` compilieren — ein
Enterprise-Reaktor-Artefakt in der OSS-Abhängigkeitskette. Falsche Richtung.

**(b) `content-rag`-Bibliotheken nach OSS mesh verschieben.** `common`, `chunking` und
`index-contract` sind framework-frei und hätten dort keine Fremdabhängigkeit im Schlepptau. Aber
RAG wäre damit ein OSS-Feature, und die Modulgrenze aus der README („`chunking` und
`index-contract` laufen in beiden") würde neu gezogen.

**(c) Zwei Dagger-Multibindings in OSS mesh, Handler in mesh-enterprise.** *Empfohlen.*
OSS bekommt zwei generische Erweiterungspunkte ohne einen Krümel RAG-Wissen:

```java
// OSS: core/…/dagger/module/CommonBindModule.java  (ist schon eine abstrakte @Module-Klasse)
@Multibinds abstract Set<EventHandler> customSearchEventHandlers();
@Multibinds abstract Set<IndexHandler<?>> customIndexHandlers();
```

```java
// OSS: MainEventHandler — ein Konstruktorparameter, eine Zeile
this.customHandlers = customHandlers;
…
return Stream.concat(Stream.of(syncEventHandler, …), customHandlers.stream())
    .collect(toMultiMap(EventHandler::handledEvents));

// OSS: IndexHandlerRegistryImpl — analog
return Stream.concat(Stream.of(nodeIndexHandler, …), customIndexHandlers.stream()).toList();
```

```java
// mesh-enterprise: core/…/enterprise/dagger/EnterpriseSearchModule.java
@Module
public abstract class EnterpriseSearchModule {
    @Binds @IntoSet abstract EventHandler ragChunkEventHandler(RagChunkEventHandler h);
    @Binds @IntoSet abstract IndexHandler<?> ragChunkIndexHandler(RagChunkIndexHandler h);
}
```
und in `EnterpriseHibernateMeshComponent` in die `modules`-Liste aufnehmen.

`@IntoSet`-Multibindings sind in Mesh bereits etabliert (`DebugInfoProviderModule`,
`HibernateModule`), das ist also kein neues Muster. Der OSS-Teil ist klein, generisch und
RAG-frei — genau die Sorte Änderung, die sich mit dem Core-Team („mit dem Mesh-Core-Team
abzustimmen") verhandeln lässt.

Neues Modul `content-rag/mesh-sync` (Artifact `mesh-content-rag-mesh-sync`) mit Abhängigkeit auf
`mesh-content-rag-common`, `-chunking`, `-index-contract`, `mesh-elasticsearch` und `mesh-mdm-api`;
`mesh-enterprise-core` zieht es. Damit bleiben `mesh-elasticsearch`-Importe aus
`mesh-enterprise-core` heraus.

---

## 2. Die vier Aufrufe in Mesh-Typen

### 2.1 Indexname — Präfix genau einmal

`RagIndexNaming.indexName(prefix, …)` setzt das Präfix selbst davor. Mesh führt Indexnamen
intern aber **unpräfigiert** und setzt das Präfix erst beim Request-Bau:

```java
// OSS: MeshHelper
public CreateDocumentRequest createDocumentRequest(String index, String id, JsonObject doc, Compliance c) {
    return new CreateDocumentRequest(index, prefixIndexName(index), id, doc, c, NOOP);
}
```

`CreateDocumentRequest` trägt beide Namen: `index` (unpräfigiert, für den Einzeldokument-Pfad, wo
`ElasticSearchProvider.storeDocument` selbst `installationPrefix()` ergänzt) und
`transformedIndex` (präfigiert, für die Bulk-Preamble). Also:

```java
// leerer Präfix – MeshHelper ergänzt ihn
String index = RagIndexNaming.indexName("", projectUuid, branchUuid, RagIndexNaming.PUBLISHED);
```

Für den rohen `delete_by_query` (2.4) gilt das Gegenteil: dort braucht es
`provider.installationPrefix() + index`.

### 2.2 `SourceContent` bauen — **Markdown, nicht Plaintext**

`SourceContent.markdownByField` ist dokumentiert als „normalisierter Text je Feldpfad (HTML
bereits nach Markdown gewandelt)". Der `StructuralChunker` chunkt entlang von Überschriften
(`isHeading`) und Absätzen — mit Jsoup-Plaintext hätte er nichts, woran er sich orientieren kann,
und das strukturbasierte Chunking wäre faktisch längenbasiert.

`NodeContainerTransformer.addFields` ist die Vorlage für den Feld-Durchlauf (Typunterscheidung
über `FieldTypes`, rekursiver Abstieg in Micronodes und Listen), aber **nicht** für die
Textgewinnung: dort steht `Jsoup.parse(value).text()`. Für RAG braucht es HTML → Markdown.
Kandidat: `flexmark-html2md-converter` — der müsste in `content-rag/mesh-sync` als Abhängigkeit
dazu, wodurch dieses Modul (anders als `chunking` und `index-contract`) nicht mehr
abhängigkeitsfrei ist. Das ist vertretbar, weil es ohnehin nur im Mesh-Prozess läuft, sollte aber
bewusst entschieden werden. Alternative: eine kleine eigene Wandlung für die Teilmenge, die
zählt (h1–h6, p, ul/ol/li, strong/em, a, code/pre, table) — weniger Abhängigkeit, mehr Eigencode.

`_roleUuids` **muss der Schreiber füllen**, weil die Leseseite fail-closed arbeitet („Ein Chunk
ohne `_roleUuids` wird nie gefunden"). Die Logik liegt in
`NodeContainerTransformer.addPermissionInfo`: `READ_PERM` plus, bei `PUBLISHED`, zusätzlich
`READ_PUBLISHED_PERM`, jeweils über `roleDao.getRolesWithPerm(node, …)` und den
`DataHolderContext`-Cache. Für RAG identisch übernehmen — schon damit K3 („dieselbe
Filterlogik") wirklich stimmt.

### 2.3 Bulk-Upsert

```java
private Flowable<SearchRequest> upsert(HibNodeFieldContainer container, HibProject project,
                                       HibBranch branch, ContainerType type) {
    if (type != PUBLISHED) {
        return Flowable.empty();                  // MVP: nur RagIndexNaming.PUBLISHED
    }
    String index = RagIndexNaming.indexName("", project.getUuid(), branch.getUuid(),
                                            RagIndexNaming.PUBLISHED);
    SourceContent source = sourceBuilder.build(container, project, branch, type);
    if (source == null) {
        return Flowable.just(purge(index, groupKeyOf(container, branch)));   // nichts RAG-bares
    }
    List<Chunk> chunks = chunker.chunk(source);   // StructuralChunker, @Singleton, zustandslos
    if (chunks.isEmpty()) {
        return Flowable.just(purge(index, source.groupId().asKey()));
    }
    Flowable<SearchRequest> upserts = Flowable.fromIterable(chunks)
        .map(chunk -> helper.createDocumentRequest(
            index,
            RagChunkDocuments.documentId(chunk),                  // == chunk.chunkId()
            new JsonObject(RagChunkDocuments.toSource(chunk)),    // Map<String,Object> → JsonObject
            compliance));
    return upserts.concatWith(Flowable.just(
        deleteStale(index, source.groupId().asKey(), chunks.size())));
}
```

`RagChunkDocuments.toSource` gibt bewusst eine `Map` zurück, damit `index-contract` ohne
JSON-Bibliothek auskommt — `new JsonObject(map)` ist die Brücke auf der Mesh-Seite. Die
`Instant`-Werte sind darin schon als ISO-Strings enthalten, `JsonObject` muss also nichts
konvertieren.

### 2.4 `delete_by_query`

Mesh hat dafür keinen Request-Typ (`common-api/…/search/request/` kennt Create, Update, Delete,
Bulk, CreateIndex, DropIndex, ReIndex). `SearchProvider.getClient()` gibt aber den rohen Client
heraus, und der hat ein generisches `postBuilder(path, body)` — dasselbe Muster wie
`ElasticSearchProvider.reIndex()` für `_reindex` und `RoleDeletedEventHandler` für `_search`:

```java
private SearchRequest deleteByQuery(String index, Map<String, Object> query) {
    return SearchRequest.create(provider -> Completable.defer(() -> {
        ElasticsearchClient<JsonObject> client = provider.getClient();
        if (client == null) {
            return Completable.complete();      // DevNull-/Tracking-Provider in Tests
        }
        String fullIndex = provider.installationPrefix() + index;
        JsonObject body = new JsonObject().put("query", new JsonObject(query));
        return client.postBuilder(fullIndex + "/_delete_by_query", body)
            .addQueryParameter("conflicts", "proceed")
            .addQueryParameter("ignore_unavailable", "true")   // Index existiert evtl. noch nicht
            .async()
            .ignoreElement();
    }));
}

private SearchRequest deleteStale(String index, String groupKey, int kept) {
    return deleteByQuery(index, RagChunkDocuments.staleChunksQuery(groupKey, kept));
}

private SearchRequest purge(String index, String groupKey) {
    return deleteByQuery(index, RagChunkDocuments.documentGroupQuery(groupKey));
}
```

**Die Reihenfolge stimmt von selbst.** Der `BulkOperator` führt zwei Queues: `Bulkable` und
Non-Bulkable. Ein Non-Bulkable-Request löst einen Flush aus und wird erst nach den gebündelten
abgearbeitet — die Chunk-Upserts sind also geschrieben, wenn der `delete_by_query` läuft. Und
weil `staleChunksQuery` auf `chunkIndex >= chunkCount` filtert, trifft sie ausschließlich
Dokumente der vorherigen, längeren Fassung; ein noch nicht erfolgter Refresh ist damit
unschädlich.

**Kostenhinweis:** ein `delete_by_query` pro geändertem Node ist bei einem Voll-Sync über
Millionen Nodes teuer — genau das, was `staleChunksQuery` laut Javadoc vermeiden soll. Lässt sich
sparen: der Handler kennt aus dem *vorherigen* Zustand nichts, aber `chunkIndex` ist
deterministisch. Wenn im `SourceContent` bzw. am Node die letzte Chunk-Anzahl je Group mitgeführt
würde (z. B. als eigenes Feld im Chunk 0 oder über eine Vorab-Zählung im Voll-Sync), könnte der
Aufruf entfallen, solange die Anzahl gleich oder gewachsen ist. Für den Delta-Pfad würde ich das
zunächst **nicht** optimieren, für `syncIndices()` aber messen.

---

## 3. `RagChunkIndexHandler` — was die Interface-Methoden liefern

```java
@Override public String getType() { return RagIndexNaming.INFIX; }   // "ragchunk"
```

| Methode | Umsetzung | Begründung |
| --- | --- | --- |
| `getIndices()` | **leere Map** | `init()` und `check()` iterieren `getIndices()`; eine leere Map macht beide zu No-ops, und das Index-Template aus `RagIndexBootstrap` bleibt allein zuständig. **Nicht** `Optional.empty()` als Wert eintragen — das bedeutet in `AbstractIndexHandler.init()` ausdrücklich `deleteIndex(…)`. |
| `init()` | `Completable.complete()` | Index entsteht implizit beim ersten Schreiben über das Template |
| `check()` | `Completable.complete()` | Mapping-Prüfung gehört zum Template, nicht zu Mesh |
| `getMappingProvider()` | `null` bzw. ein No-op-Provider | wird nur über `getIndices()` gebraucht, das leer ist |
| `syncIndices(pattern)` | eigene Implementierung: über `ProjectDao` → `BranchDao` → `ContentDao` iterieren, je Container `upsert(…)`, Indexname gegen `pattern` prüfen | erfüllt die Zusage „Voll-Sync gilt auch für RAG"; der bucketbasierte Diff aus `AbstractIndexHandler` passt nicht, weil er ein Dokument pro Element annimmt |
| `filterUnknownIndices(indices)` | aktive `ragchunk-<project>-<branch>-published`-Namen aus DB bilden, `indices.stream().filter(i -> i.startsWith(getType()))` und die aktiven abziehen | räumt Indizes gelöschter Branches/Projekte auf; ohne diese Methode bleiben sie liegen, weil jeder andere Handler mit `startsWith(getType())` auf seinen eigenen Typ filtert |
| `accepts(Class)` | `false` | `IndexHandlerRegistryImpl.getForClass()` liefert den **ersten** Handler, der zusagt. Sagt der RAG-Handler zu `HibNodeFieldContainer` zu, verdeckt er je nach Reihenfolge `NodeIndexHandlerImpl`. |
| `elementLoader()`, `elementsLoader()`, `loadAllElements()`, `getElementClass()` | minimal / `HibNodeFieldContainer` | nur von der Basisklasse und `getForClass` gebraucht; da wir `AbstractIndexHandler` nicht erben und `accepts()` `false` liefert, bleiben sie ungenutzt |
| `getIndicesForSearch(ac)` | `Set.of()` | die Suche läuft über den rag-service, nicht über Mesh `/search` |
| `generateVersion(element, dhc)` | Delegation an `NodeContainerTransformer` oder `ChunkIds.contentHash(…)` | nur für Diff-Sync relevant |
| `getMetrics()` | `SyncMetersFactory.createSyncMetric("ragchunk")` | damit `/search/status` den RAG-Index mitzählt |
| `getTotalCountFromGraph()` | Anzahl indexierbarer Container | Fortschrittsanzeige |

`IndexHandler` direkt implementieren, **nicht** `AbstractIndexHandler` erben: dessen
`diffAndSync`/`loadVersionsFromIndex` gehen von einem Dokument pro Element mit `uuid`- und
`version`-Feld aus. RAG-Chunk-Dokumente haben beides nicht.

---

## 4. Der Event-Pfad

```java
@Override
public Collection<MeshEvent> handledEvents() {
    return Arrays.asList(
        NODE_CONTENT_CREATED, NODE_UPDATED, NODE_PUBLISHED,        // upsert
        NODE_CONTENT_DELETED, NODE_UNPUBLISHED, NODE_DELETED,      // purge
        NODE_MOVED);                                               // path/sourceUrl ändern sich
}
```

Registriert wird er neben `nodeContentEventHandler` — `MainEventHandler.createHandlers()` baut
über `toMultiMap` eine **Multimap**, mehrere Handler dürfen dasselbe Event bearbeiten.

Drei Unterschiede zu `NodeContentEventHandler`, die bewusst sind:

1. **Schema-Migration löst kein Delete aus.** `NodeContentEventHandler.migrationUpdate()` löscht
   die Dokumente der alten Schemaversion, weil der Node-Index die `schemaVersionUuid` im Namen
   trägt. Der RAG-Index tut das ausdrücklich nicht — hier genügt ein Upsert in denselben Index.
   Genau der Punkt, den `RagIndexNaming` im Javadoc als Grund für die Namensform nennt.
2. **`NODE_MOVED` gehört dazu.** `path` und `sourceUrl` stehen im Chunk-Dokument; nach einem
   Verschieben sind sie falsch. `NodeMoveEventHandler` ist die Vorlage.
3. **Nur `PUBLISHED`.** `RagIndexNaming.DRAFT` existiert, ist aber laut README nicht MVP. Der
   Filter gehört an eine Stelle (`upsert`), damit ein späteres Einschalten eine Zeile ist.

`EventCauseHelper.isProjectDeleteCause(message)` → `Flowable.empty()`: beim Projektlöschen wird
der Index als Ganzes verworfen, einzelne Deletes wären verschwendet.

---

## 5. Allow-List und No-RAG (K4) — umgesetzt in `content-rag/mesh-sync`

Beide Fragen entscheiden dasselbe: welcher Inhalt das CMS überhaupt Richtung RAG-Index verlässt.
Sie gehören deshalb an dieselbe Stelle und **vor** die Normalisierung — ausgeschlossener Inhalt
wird gar nicht erst nach Markdown gewandelt, nicht gechunkt und nicht geschrieben. Eine Filterung
im rag-service wäre die falsche Richtung: dorthin müsste der Inhalt erst gelangen.

```
Node-Container ──▶ RagFieldSelector.selectionFor(schemaVersion)   ← Allow-List + K4, gecacht
                        │
                        ├─ leer ──────────▶ purge(documentGroupQuery)   ← No-RAG auf Node-Ebene
                        │
                        ▼
                   RagFieldTexts.add(fieldPath, markdown)   ← einziger Eingang für Text
                        │                                     (HTML→Markdown nur für akzeptierte Felder)
                        ▼
                   SourceContent ─▶ StructuralChunker ─▶ Bulk-Upsert + staleChunksQuery
```

### 5.1 Woher die Angaben kommen

Mesh hat die passenden Stellen schon, es braucht keine Modelländerung und keine Migration:

* `FieldSchemaContainer.getElasticsearch()` — freies JSON **pro Schema**
* `FieldSchema.getElasticsearch()` — freies JSON **pro Feld**
* `FieldSchema.isNoIndex()` — bestehendes Opt-out, das `isMappingRequired()` auswertet

Diese Settings sind mit der Schemaversion versioniert, pro Projekt setzbar und über die
bestehende Schema-API pflegbar. `MeshOptions` hätte die falsche Granularität — eine Allow-List
ist pro Schema, nicht pro Installation.

### 5.2 Die Regeln

Zuerst das Schema, dann das Feld; das erste zutreffende Ergebnis gilt.

| # | Regel | Bedeutung |
| --- | --- | --- |
| 1 | Schema: `"elasticsearch": { "rag": false }` | ganzes Schema draußen — **harter Ausschluss** |
| 2 | Feld: `"elasticsearch": { "rag": false }` | **No-RAG (K4)** auf Feldebene |
| 3 | Feld: `isNoIndex() == true` | implizites No-RAG: was nicht suchbar ist, ist auch nicht RAG-bar |
| 4 | Feld: `"elasticsearch": { "rag": true }` | ausdrückliche Freigabe, auch bei enger Allow-List |
| 5 | Schema: `"elasticsearch": { "rag": { "fields": ["title", "content"] } }` | Allow-List je Schema; zusätzlich `"exclude": [...]` und `"mode": "all"\|"none"` |
| 6 | Schema: `"elasticsearch": { "rag": true }` | alle textführenden Felder |
| 7 | Default | `search.rag.defaultMode`, vorgabegemäß `NONE` — nichts ohne ausdrückliche Freigabe |

Eine Abweichung von der ersten Skizze, bewusst: **Regel 1 steht vor der Feldfreigabe.** Ein
Schema-`rag: false` gewinnt also auch gegen ein Feld mit `rag: true`. Ein Opt-out, das eine
vergessene Feldangabe aushebeln kann, ist kein Opt-out — und K4 ist genau dafür da.

Über alle Regeln hinweg gilt ein Typfilter: `string`, `html`, `list` und `micronode` tragen Text,
`binary`/`s3binary` nur bei `includeBinaries` oder ausdrücklicher Feldfreigabe. `number`,
`boolean`, `date` und `node` bleiben draußen — ein Node-Feld wäre Referenzauflösung (V7) und damit
ein eigener Schritt.

### 5.3 No-RAG auf Node-Ebene fällt dabei ab

Liefert die Auswahl kein Feld — Schema ausgeschlossen, oder alle Felder einzeln —, entsteht kein
`SourceContent`. Der Handler räumt dann eine eventuell vorhandene Document-Group per
`RagChunkDocuments.documentGroupQuery(groupKey)` ab (der `purge`-Zweig aus 2.3). Der Ausschluss
wirkt damit **rückwirkend** und ohne zweiten Codepfad: ein Schema, das nachträglich auf
`rag: false` gesetzt wird, räumt seine Chunks beim nächsten Sync selbst weg.

Eine Tag-Family-Konvention (`rag:exclude` am Node, Konzept O1) ist damit bewusst **nicht**
umgesetzt: sie kostet eine Tag-Lesung je Node im Sync-Pfad, zwei weitere Events
(`NODE_TAGGED`/`NODE_UNTAGGED`) und einen Sonderfall beim Umbenennen und Löschen des Tags. Wenn
redaktioneller Ausschluss je Node später gebraucht wird, ist die Stelle dafür dieselbe
(`RagFieldSelector` → leere Auswahl) und die Erweiterung klein.

### 5.4 Was im Repo liegt

Modul `content-rag/mesh-sync` (Artefakt `mesh-content-rag-mesh-sync`), framework-frei, ohne
Abhängigkeit auf `mesh-elasticsearch` — die Auswahlregeln sind der inhaltlich kniffligste Teil und
bleiben so ohne laufendes Mesh testbar:

| Klasse | Aufgabe |
| --- | --- |
| `RagFieldSelector` | wertet die Regeln aus, Cache je `schemaVersionUuid` (`invalidate`/`invalidateAll` für Tests und Restore), threadsicher |
| `RagSchemaDescriptor`, `RagFieldDescriptor` | eigene, minimale Sicht auf Schema und Feld — der Adapter füllt sie aus der Mesh-Schema-API |
| `RagSchemaSelection` | Ergebnis: ausgewählte Felder **in Schemareihenfolge**, `includes(fieldPath)` für Unterfelder |
| `RagFieldTexts` | Sammler für die normalisierten Feldtexte; setzt die Auswahl durch und hält die Reihenfolge |
| `RagFieldPaths` | Feldpfade `feld/index/unterfeld`, `rootOf` für die Vererbung auf Unterfelder |
| `RagSyncOptions`, `RagSelectionMode` | Konfiguration: `enabled`, `defaultMode`, `includeBinaries` |

Dazu `RagFieldSelectorTest` und `RagFieldTextsTest` mit allen Regelfällen aus 5.2.

### 5.5 Der Adapter auf die Mesh-Seite

Fehlt noch und ist der einzige Teil, der Mesh-Typen braucht — rund zwanzig Zeilen:

```java
// mesh-sync (mit Mesh-Abhängigkeit), aufgerufen aus RagSourceContentBuilder
static RagSchemaDescriptor describe(HibSchemaVersion version) {
    SchemaModel model = version.getSchema();
    List<RagFieldDescriptor> fields = new ArrayList<>();
    for (FieldSchema field : model.getFields()) {
        fields.add(new RagFieldDescriptor(
                field.getName(),
                field.getType(),                       // FieldTypes#toString, bereits kleingeschrieben
                field.isNoIndex(),
                toMap(field.getElasticsearch())));      // JsonObject#getMap
    }
    return new RagSchemaDescriptor(model.getName(), version.getUuid(),
            toMap(model.getElasticsearch()), fields);
}
```

`JsonObject.getMap()` liefert die verschachtelten Objekte als `Map` und Listen als `List` —
genau die Formen, die `RagFieldSelector` erwartet; eine JSON-Abhängigkeit wandert damit nicht in
das Auswahlmodul.

Im Feld-Durchlauf (Vorlage `NodeContainerTransformer.addFields`) hängt dann alles an zwei Zeilen:

```java
RagSchemaSelection selection = selector.selectionFor(describe(container.getSchemaContainerVersion()));
if (selection.isEmpty()) {
    return null;                          // → purge(documentGroupQuery) im Handler
}
RagFieldTexts texts = new RagFieldTexts(selection);
for (FieldSchema field : schema.getFields()) {
    if (!texts.accepts(field.getName())) {
        continue;                         // spart die HTML→Markdown-Wandlung
    }
    texts.add(field.getName(), markdown(container, field));   // Micronodes/Listen: feld/index/unterfeld
}
if (texts.isEmpty()) {
    return null;
}
return new SourceContent(group, …, texts.toMarkdownByField(), …);
```

Micronodes und Listen erben die Entscheidung ihres äußeren Schemafeldes — nur dieses steht im
Schema und kann dort freigegeben werden. Der Pfad `feld/index/unterfeld` landet unverändert im
Chunk-Dokument und macht die Herkunft nachvollziehbar.

### 5.6 Ein Nebenbefund: die Feldreihenfolge war nicht deterministisch

`SourceContent` kopierte `markdownByField` mit `Map.copyOf`. Dessen Iterationsreihenfolge ist
nicht festgelegt und wechselt seit Java 9 sogar zwischen zwei JVM-Starts. Der `StructuralChunker`
zählt `chunkIndex` aber in Iterationsreihenfolge hoch, und die Chunk-ID hängt an
`fieldPath + chunkIndex`. Ein Node hätte damit nach einem Mesh-Neustart andere Chunk-IDs bekommen:
das Bulk-Upsert hätte nichts überschrieben, sondern Dubletten angelegt, und `staleChunksQuery`
hätte sie nicht gefunden, weil die Anzahl gleich geblieben wäre — der stille Fall, der erst im
Retrieval als doppelte Treffer auffällt.

`SourceContent` hält die Reihenfolge jetzt fest (`LinkedHashMap`, unveränderlich gekapselt), und
`RagFieldTexts` liefert sie in Schemareihenfolge. Die Zusage „deterministische IDs" gilt damit
auch über Neustarts hinweg.

## 6. Konfiguration und zwei Betriebsfallen

Umgesetzt als Umgebungsvariablen bzw. System-Properties (`RagSyncConfig`), **nicht** als
`MeshOptions`-Zweig: der Konfigurationsbaum von Mesh liegt im OSS-Modell, und ein
`search.rag`-Abschnitt dort wäre eine Modelländerung samt Dokumentation und Migration. Für den
Helm-Chart ist die Form ohnehin die passende.

| Umgebung | Property | Vorgabe |
| --- | --- | --- |
| `MESH_SEARCH_RAG_ENABLED` | `mesh.search.rag.enabled` | `false` |
| `MESH_SEARCH_RAG_DEFAULT_MODE` | `mesh.search.rag.defaultMode` | `NONE` |
| `MESH_SEARCH_RAG_INCLUDE_BINARIES` | `mesh.search.rag.includeBinaries` | `false` |
| `MESH_SEARCH_RAG_CHUNK_MAX_CHARS` | `mesh.search.rag.chunk.maxChars` | `1200` |
| `MESH_SEARCH_RAG_CHUNK_OVERLAP_CHARS` | `mesh.search.rag.chunk.overlapChars` | `150` |
| `MESH_SEARCH_RAG_SOURCE_URL_TEMPLATE` | `mesh.search.rag.sourceUrlTemplate` | leer |

Die ersten drei Werte bilden `RagSyncOptions`, die beiden Chunk-Werte `ChunkingOptions`. Die
Vorlage der Quellen-URL kennt die Platzhalter `{project}`, `{branch}`, `{language}`, `{path}` und
`{uuid}`; ohne sie bleibt `sourceUrl` im Chunk leer.

Ein `enabled: false` muss **beide** Handler stilllegen: der EventHandler gibt `Flowable.empty()`
zurück, `syncIndices()` ebenso. Sonst baut ein Voll-Sync Indizes auf, die niemand pflegt.

**Falle 1: `POST /search/clear` löscht die RAG-Indizes mit.**
`ElasticSearchProvider.clear()` liest `_all` und löscht alles, was mit dem Installations-Präfix
beginnt — unabhängig davon, welcher Handler den Index kennt. Mit `syncIndices()` ist das
verkraftbar (ein Voll-Sync baut wieder auf), kostet aber die komplette Neuvektorisierung. Wenn
das nicht gewollt ist, muss der RAG-Index außerhalb des Mesh-Präfixes benannt werden — was dem
`ES_INDEX_PREFIX`-Abgleich aus der README widerspricht. Ich würde es akzeptieren und
dokumentieren.

**Falle 2: der Präfix muss auf beiden Seiten identisch normalisiert sein.**
`RagIndexNaming.normalizePrefix` ergänzt ein `-`, wenn keines da ist, und schreibt alles klein.
Mesh' `installationPrefix()` tut das nicht. Bei einem Mesh-Präfix `Mesh_` heißt der Index für den
rag-service `mesh_-ragchunk-…`, für Mesh `Mesh_ragchunk-…`. Das ist ein Ein-Zeichen-Fehler, der
sich als „Service findet keine Chunks" äußert und schwer zu finden ist. Vorschlag: in
`RagChunkIndexHandler.init()` einmal prüfen, dass
`RagIndexNaming.indexName(provider.installationPrefix(), p, b, PUBLISHED)` gleich
`provider.installationPrefix() + RagIndexNaming.indexName("", p, b, PUBLISHED)` ist, und sonst mit
einer klaren Meldung starten zu verweigern.

---

## 7. Reihenfolge der Umsetzung

1. ~~Modul `content-rag/mesh-sync`, Feldauswahl mit Unit-Tests~~ — **erledigt**.
2. ~~Adapter auf die Mesh-Schema-API und `RagSourceContentBuilder`~~ — **erledigt**.
3. ~~HTML→Markdown entscheiden~~ — **erledigt**: eigene Wandlung auf jsoup (`HtmlToMarkdown`), keine neue Fremdbibliothek im Mesh-Prozess. Offen bleibt die Probe mit Beispielinhalten aus einem echten Projekt: schneidet der `StructuralChunker` darauf sinnvoll?
4. ~~Zwei Multibindings in OSS mesh, `EnterpriseSearchModule`, `RagChunkEventHandler`~~ — **erledigt**, aber mit dem Core-Team abzustimmen (Abschnitt 1).
5. ~~`RagChunkIndexHandler` mit `syncIndices()` und `filterUnknownIndices()`~~ — **erledigt**.
6. **Offen:** erster Reaktor-Build beider Repos, dann Test gegen ein echtes Projekt: Feature-Flag setzen, `POST /api/v2/search/sync`, Chunks im Index gegenprüfen, `/api/rag/status` und eine Beispielabfrage im rag-service.
7. **Offen:** Changelog-Einträge und die Dokumentation der Schema-Settings im Admin-UI.

---

## 8. Was noch zu entscheiden ist

1. **Die zwei Multibindings im OSS-Mesh** sind mit dem Mesh-Core-Team abzustimmen. Sie sind klein und generisch (kein RAG-Wissen), aber sie ändern zwei zentrale Klassen der Suche.
2. **Binaries im RAG-Index**: die Mechanik steht (`includeBinaries`, Feldfreigabe), offen bleibt die Längengrenze für extrahierten Dokumenttext — ohne sie kann ein einzelnes PDF hunderte Chunks erzeugen.
3. **Wer pflegt die Allow-List beim Kunden?** Die Settings sind über die Schema-API pflegbar; ob das Admin-UI (Konzept 5.9) dafür eine eigene Maske bekommt oder die Schema-Bearbeitung genügt, ist eine Produktentscheidung.
4. **Branch-Umbenennung.** Der Document-Group-Schlüssel ist `nodeUuid:branchName:language` — benennt jemand einen Branch um, zeigen die alten Chunks auf einen Namen, den niemand mehr bildet; sie werden erst durch einen Voll-Sync mit anschließendem Aufräumen los. Alternative wäre die Branch-UUID im Schlüssel, dann steht im Chunk-Feld `branch` aber keine Anzeigeform mehr. Vorschlag: so lassen, dokumentieren, und beim Umbenennen einen Reindex des Branches fahren.
5. **`POST /search/clear` löscht die RAG-Indizes mit** (Abschnitt 6, Falle 1) — akzeptiert und zu dokumentieren, weil ein Voll-Sync sie wieder aufbaut; kostet aber die komplette Neuvektorisierung.
