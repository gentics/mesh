package com.gentics.mesh.contentrag.meshsync;

import static com.gentics.mesh.core.rest.common.ContainerType.PUBLISHED;

import com.gentics.mesh.contentrag.index.RagIndexNaming;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Bucket;
import com.gentics.mesh.core.data.HibBucketableElement;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.schema.HibSchemaVersion;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.data.search.index.IndexInfo;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.search.EntityMetrics;
import com.gentics.mesh.handler.DataHolderContext;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.index.MappingProvider;
import com.gentics.mesh.search.index.Transformer;
import com.gentics.mesh.search.index.metric.SyncMeters;
import com.gentics.mesh.search.index.metric.SyncMetersFactory;

import io.reactivex.Completable;
import io.reactivex.Flowable;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Lebenszyklus des RAG-Chunk-Index: Voll-Sync, Aufraeumen verwaister Indizes und die Anzeige in
 * {@code /search/status}.
 *
 * <p>Warum es diesen Handler zusaetzlich zum {@link RagChunkEventHandler} gibt: Voll-Sync
 * ({@code POST /api/v2/search/sync}) und der periodische Index-Korrektheits-Check laufen
 * ausschliesslich ueber {@code IndexHandlerRegistry#getHandlers()}. Wer dort nicht steht, wird von
 * beiden nicht gesehen – und die Zusage aus dem Konzept, dass Voll-Sync und Check auch fuer die
 * RAG-Indizes gelten, waere nicht eingeloest.
 *
 * <p>Bewusst {@link IndexHandler} direkt implementiert und <strong>nicht</strong>
 * {@code AbstractIndexHandler} geerbt: dessen {@code diffAndSync}/{@code loadVersionsFromIndex}
 * gehen von einem Dokument je Element mit {@code uuid}- und {@code version}-Feld aus. Ein
 * Chunk-Dokument hat beides nicht – es gibt viele Chunks je Container.
 */
@Singleton
public class RagChunkIndexHandler implements IndexHandler<HibNodeFieldContainer> {

    private static final Logger log = LoggerFactory.getLogger(RagChunkIndexHandler.class);

    private final Database db;
    private final SearchProvider searchProvider;
    private final BucketManager bucketManager;
    private final RagSyncOptions options;
    private final RagChunkWriter writer;
    private final RagIndexReadiness readiness;
    private final SyncMeters meters;

    /**
     * @param db                 Datenbankzugriff
     * @param searchProvider     fuer den Installations-Praefix
     * @param bucketManager      Bucket-Aufteilung des Voll-Syncs
     * @param syncMetersFactory  Metriken fuer {@code /search/status}
     * @param options            Betriebsparameter
     * @param writer             gemeinsame Schreiblogik mit dem Event-Pfad
     * @param readiness          Wachposten fuer das Index-Template des rag-service
     */
    @Inject
    public RagChunkIndexHandler(Database db, SearchProvider searchProvider, BucketManager bucketManager,
                                SyncMetersFactory syncMetersFactory, RagSyncOptions options, RagChunkWriter writer,
                                RagIndexReadiness readiness) {
        this.db = db;
        this.searchProvider = searchProvider;
        this.bucketManager = bucketManager;
        this.options = options;
        this.writer = writer;
        this.readiness = readiness;
        this.meters = syncMetersFactory.createSyncMetric(RagIndexNaming.INFIX);
    }

    @Override
    public String getType() {
        return RagIndexNaming.INFIX;
    }

    /**
     * Prueft den Indexnamen und fragt danach einmal nach dem Index-Template.
     *
     * <p>{@code RagIndexNaming} ergaenzt ein fehlendes {@code -} und schreibt alles klein, Mesh'
     * {@code installationPrefix()} tut das nicht. Bei einem Praefix wie {@code Mesh_} heisst der
     * Index fuer den rag-service {@code mesh_-ragchunk-…}, fuer Mesh {@code Mesh_ragchunk-…} – ein
     * Ein-Zeichen-Fehler, der sich als „der Service findet keine Chunks" aeussert und schwer zu
     * finden ist. Lieber hier scheitern.
     *
     * <p>Der Index selbst entsteht implizit beim ersten Schreiben ueber das Index-Template, das
     * der rag-service anlegt. Ob dieses Template schon da ist, klaert
     * {@link RagIndexReadiness#probe()} - fehlt es, startet Mesh trotzdem, der RAG-Sync haelt sich
     * nur zurueck. Der Praefix-Guard darunter scheitert dagegen hart: ein falscher Praefix ist eine
     * Fehlkonfiguration, die sich nicht von selbst aufloest, ein fehlendes Template schon.
     */
    @Override
    public Completable init() {
        if (!options.enabled()) {
            log.info("Der RAG-Sync ist ausgeschaltet (MESH_SEARCH_RAG_ENABLED).");
            return Completable.complete();
        }
        String prefix = searchProvider.installationPrefix();
        String withPrefix = RagIndexNaming.indexName(prefix, "project", "branch", RagIndexNaming.PUBLISHED);
        String prefixed = prefix + RagIndexNaming.indexName("", "project", "branch", RagIndexNaming.PUBLISHED);
        if (!withPrefix.equals(prefixed)) {
            return Completable.error(new IllegalStateException(
                    "Der Elasticsearch-Praefix '" + prefix + "' wird von Mesh und vom rag-service"
                            + " unterschiedlich normalisiert (" + prefixed + " vs. " + withPrefix
                            + "). Bitte einen kleingeschriebenen Praefix verwenden, der auf '-' endet."));
        }
        log.info("Der RAG-Sync ist eingeschaltet, Praefix {}, Vorgabe-Auswahl {}.", prefix, options.defaultMode());
        return readiness.probe();
    }

    /**
     * Mapping und Pipeline kommen aus dem Index-Template des rag-service, nicht aus Mesh - hier
     * wird deshalb nichts angelegt und nichts angeglichen.
     *
     * <p>Geprueft wird stattdessen zweierlei: ob das Template inzwischen existiert (dann nimmt der
     * Sync den Betrieb auf) und ob ein Index vor dem Template entstanden ist und deshalb ohne
     * Embeddings dasteht. Der zweite Fall faellt sonst nirgends auf - der Index nimmt Dokumente an
     * und meldet keinen Fehler, nur das Retrieval liefert nichts.
     */
    @Override
    public Completable check() {
        if (!options.enabled()) {
            return Completable.complete();
        }
        return readiness.probe().andThen(readiness.reportBrokenIndices());
    }

    /**
     * Leer – und das ist die Absicht: {@code init()} und {@code check()} der Basisklasse iterieren
     * ueber diese Map. Ein Eintrag mit {@code Optional.empty()} wuerde ausdruecklich ein
     * {@code deleteIndex(…)} bedeuten.
     */
    @Override
    public Map<String, Optional<IndexInfo>> getIndices() {
        return Map.of();
    }

    /** Die RAG-Suche laeuft ueber den rag-service, nicht ueber {@code /search}. */
    @Override
    public Set<String> getIndicesForSearch(InternalActionContext ac) {
        return Set.of();
    }

    /**
     * Immer {@code false}: {@code IndexHandlerRegistry#getForClass()} liefert den ersten Handler,
     * der zusagt. Wuerde dieser Handler zu {@code HibNodeFieldContainer} zusagen, koennte er je
     * nach Reihenfolge {@code NodeIndexHandlerImpl} verdecken.
     */
    @Override
    public boolean accepts(Class<?> clazzOfElement) {
        return false;
    }

    @Override
    public Class<? extends HibBucketableElement> getElementClass() {
        return HibNodeFieldContainer.class;
    }

    @Override
    public Function<String, HibNodeFieldContainer> elementLoader() {
        // Wird nur ueber den Diff-Sync der Basisklasse gebraucht, den dieser Handler nicht nutzt.
        return uuid -> null;
    }

    @Override
    public Function<Collection<String>, Stream<Pair<String, HibNodeFieldContainer>>> elementsLoader() {
        return uuids -> Stream.empty();
    }

    @Override
    public Collection<? extends HibNodeFieldContainer> loadAllElements(Bucket bucket, DataHolderContext dc) {
        return List.of();
    }

    @Override
    public String generateVersion(HibNodeFieldContainer element, DataHolderContext dhc) {
        return element.getElementVersion();
    }

    /** Kein Transformer: das Chunk-Dokument entsteht ueber {@link RagChunkWriter}. */
    @Override
    @SuppressWarnings("rawtypes")
    public Transformer getTransformer() {
        return null;
    }

    /** Kein Mapping-Provider: das Mapping liegt im Index-Template des rag-service. */
    @Override
    public MappingProvider getMappingProvider() {
        return null;
    }

    @Override
    public EntityMetrics getMetrics() {
        return meters.createSnapshot();
    }

    @Override
    public long getTotalCountFromGraph() {
        // Block statt Ausdruck: ein Ausdruckslambda waere hier auch void-kompatibel, und
        // TxFactory#tx(TxAction) und tx(TxAction2) wuerden beide passen.
        return db.tx(tx -> {
            return tx.contentDao().globalCount();
        });
    }

    /**
     * Voll-Sync: schreibt alle veroeffentlichten Container aller Projekte und Branches neu.
     *
     * <p>Der bucketbasierte Diff der Basisklasse passt hier nicht – er vergleicht Versionen je
     * Dokument, und ein Container hat viele Chunk-Dokumente. Stattdessen wird stumpf upserted: die
     * deterministischen IDs ueberschreiben die vorherige Fassung, {@code staleChunksQuery} raeumt
     * den Rest.
     */
    @Override
    public Flowable<SearchRequest> syncIndices(Optional<Pattern> indexPattern) {
        if (!options.enabled()) {
            return Flowable.empty();
        }
        if (!readiness.isReady()) {
            log.warn("Voll-Sync des RAG-Index uebersprungen: das Index-Template {} fehlt. Ohne das"
                    + " Template entstuende ein Index ohne Embeddings, und der liesse sich"
                    + " nachtraeglich nicht mehr geradebiegen.", readiness.templateName());
            return Flowable.empty();
        }
        return Flowable.defer(() -> db.tx(tx -> {
            // Einmal je Lauf statt einmal je Schemaversion: getTotalCountFromGraph() wuerde eine
            // Transaktion innerhalb der hier laufenden oeffnen, und die Zahl aendert sich waehrend
            // eines Laufs ohnehin nicht sinnvoll.
            long total = tx.contentDao().globalCount();
            List<Flowable<SearchRequest>> parts = new ArrayList<>();
            int branches = 0;
            for (HibProject project : tx.projectDao().findAll()) {
                for (HibBranch branch : tx.branchDao().findAll(project)) {
                    String index = writer.indexName(project.getUuid(), branch.getUuid());
                    if (indexPattern.isPresent() && !indexPattern.get().matcher(index).matches()) {
                        continue;
                    }
                    branches++;
                    for (HibSchemaVersion version : branch.findActiveSchemaVersions()) {
                        parts.add(syncVersion(project, branch, version, total));
                    }
                }
            }
            log.info("Voll-Sync des RAG-Index: {} Branches, {} Schemaversionen, {} Container insgesamt.",
                    branches, parts.size(), total);
            return Flowable.merge(parts);
        }));
    }

    private Flowable<SearchRequest> syncVersion(HibProject project, HibBranch branch, HibSchemaVersion version,
                                                long total) {
        return bucketManager.getBuckets(total)
                .concatMap(bucket -> Flowable.defer(() -> db.tx(tx -> {
                    List<SearchRequest> requests = new ArrayList<>();
                    tx.schemaDao().getFieldContainers(version, branch.getUuid(), bucket, Optional.of(PUBLISHED))
                            .forEach(container -> {
                                requests.addAll(writer.upsert(container, project, branch, PUBLISHED));
                                // Erst anmelden, dann abhaken: SyncMeter zieht bei synced() von
                                // pending ab, ohne addPending liefe der Zaehler ins Negative.
                                meters.getInsertMeter().addPending(1L);
                                meters.getInsertMeter().synced();
                            });
                    if (log.isDebugEnabled()) {
                        log.debug("RAG-Voll-Sync {}/{}, Version {}, Bucket {}: {} Requests.",
                                project.getName(), branch.getName(), version.getName(), bucket, requests.size());
                    }
                    return Flowable.fromIterable(requests);
                })));
    }

    /**
     * Meldet RAG-Indizes, zu denen es kein Projekt und keinen Branch mehr gibt.
     *
     * <p>Ohne diese Methode bleiben sie liegen: jeder andere Handler filtert mit
     * {@code startsWith(getType())} auf seinen eigenen Typ und sieht die RAG-Indizes nicht.
     */
    @Override
    public Set<String> filterUnknownIndices(Set<String> indices) {
        Set<String> active = db.tx(tx -> {
            Set<String> names = new LinkedHashSet<>();
            for (HibProject project : tx.projectDao().findAll()) {
                for (HibBranch branch : tx.branchDao().findAll(project)) {
                    names.add(writer.indexName(project.getUuid(), branch.getUuid()));
                }
            }
            return names;
        });
        return indices.stream()
                .filter(index -> index.startsWith(getType()))
                .filter(index -> !active.contains(index))
                .collect(Collectors.toSet());
    }
}
