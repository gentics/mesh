package com.gentics.mesh.contentrag.meshsync;

import com.gentics.mesh.contentrag.chunking.ChunkingOptions;
import com.gentics.mesh.contentrag.chunking.StructuralChunker;
import com.gentics.mesh.contentrag.common.model.Chunk;
import com.gentics.mesh.contentrag.common.model.SourceContent;
import com.gentics.mesh.contentrag.index.RagChunkDocuments;
import com.gentics.mesh.contentrag.index.RagIndexNaming;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.project.HibProject;
import com.gentics.mesh.core.data.search.Compliance;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.verticle.eventhandler.MeshHelper;

import com.gentics.elasticsearch.client.ElasticsearchClient;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Schreibt die RAG-Chunks eines Node-Containers: Chunking, Bulk-Upsert und das Aufraeumen
 * ueberzaehliger Chunks.
 *
 * <p>Beide Handler teilen sich diese Klasse – der Event-Pfad
 * ({@link RagChunkEventHandler}) und der Voll-Sync ({@link RagChunkIndexHandler}). Sonst koennten
 * Delta- und Voll-Indexierung auseinanderlaufen, und genau das waere der Fehler, den niemand
 * bemerkt, bis das Retrieval unterschiedliche Ergebnisse liefert.
 */
public class RagChunkWriter {

    private final MeshHelper helper;
    private final SearchProvider searchProvider;
    private final Compliance compliance;
    private final RagSourceContentBuilder builder;
    private final StructuralChunker chunker;

    /**
     * @param helper         Request-Factory von Mesh (setzt den Index-Praefix selbst)
     * @param searchProvider fuer den rohen Client und den Installations-Praefix
     * @param compliance     Elasticsearch-Kompatibilitaetsmodus
     * @param builder        Aufbau des Quellinhalts inklusive Feldauswahl
     * @param chunkingOptions Chunk-Groesse und Ueberlappung
     */
    public RagChunkWriter(MeshHelper helper, SearchProvider searchProvider, Compliance compliance,
                          RagSourceContentBuilder builder, ChunkingOptions chunkingOptions) {
        this.helper = helper;
        this.searchProvider = searchProvider;
        this.compliance = compliance;
        this.builder = builder;
        this.chunker = new StructuralChunker(chunkingOptions);
    }

    /**
     * Indexname ohne Praefix – {@code MeshHelper} ergaenzt ihn beim Request-Bau selbst.
     *
     * @param projectUuid Projekt
     * @param branchUuid  Branch
     * @return unpraefigierter Indexname
     */
    public String indexName(String projectUuid, String branchUuid) {
        return RagIndexNaming.indexName("", projectUuid, branchUuid, RagIndexNaming.PUBLISHED);
    }

    /**
     * Erzeugt die Requests fuer einen Container: Bulk-Upsert der aktuellen Chunks und das
     * Wegraeumen der ueberzaehligen. Liefert der Builder nichts – Schema nicht freigegeben, alle
     * Felder ausgeschlossen, kein Text –, wird die Document-Group abgeraeumt.
     *
     * <p>Muss innerhalb einer Transaktion laufen: der Builder liest den Container.
     *
     * @param container Node-Container
     * @param project   Projekt
     * @param branch    Branch
     * @param type      Container-Typ; alles ausser {@link ContainerType#PUBLISHED} ergibt eine
     *                  leere Liste (MVP)
     * @return Requests in Ausfuehrungsreihenfolge
     */
    public List<SearchRequest> upsert(HibNodeFieldContainer container, HibProject project, HibBranch branch,
                                      ContainerType type) {
        if (type != ContainerType.PUBLISHED) {
            return List.of();
        }
        String index = indexName(project.getUuid(), branch.getUuid());
        SourceContent source = builder.build(container, project, branch, type);
        if (source == null) {
            return List.of(deleteByQuery(index,
                    RagChunkDocuments.nodeLanguageQuery(nodeUuid(container), languageOf(container))));
        }
        List<Chunk> chunks = chunker.chunk(source);
        if (chunks.isEmpty()) {
            return List.of(deleteByQuery(index,
                    RagChunkDocuments.documentGroupQuery(source.groupId().asKey())));
        }
        List<SearchRequest> requests = new ArrayList<>(chunks.size() + 1);
        for (Chunk chunk : chunks) {
            requests.add(helper.createDocumentRequest(index, RagChunkDocuments.documentId(chunk),
                    new JsonObject(RagChunkDocuments.toSource(chunk)), compliance));
        }
        // Der BulkOperator flusht vor einem nicht bulkbaren Request: die Upserts sind geschrieben,
        // bevor geloescht wird. Und die Query trifft nur Chunks jenseits der neuen Anzahl.
        requests.add(deleteByQuery(index,
                RagChunkDocuments.staleChunksQuery(source.groupId().asKey(), chunks.size())));
        return requests;
    }

    /**
     * Raeumt alle Chunks eines Nodes in einem Branch ab – fuer geloeschte und verschobene Nodes.
     *
     * @param projectUuid Projekt
     * @param branchUuid  Branch
     * @param nodeUuid    Node
     * @return Delete-by-Query-Request
     */
    public SearchRequest purgeNode(String projectUuid, String branchUuid, String nodeUuid) {
        return deleteByQuery(indexName(projectUuid, branchUuid), RagChunkDocuments.nodeQuery(nodeUuid));
    }

    /**
     * Raeumt eine Sprachversion ab – fuer depublizierte oder geloeschte Sprachversionen.
     *
     * @param projectUuid Projekt
     * @param branchUuid  Branch
     * @param nodeUuid    Node
     * @param language    Sprachcode
     * @return Delete-by-Query-Request
     */
    public SearchRequest purgeLanguage(String projectUuid, String branchUuid, String nodeUuid, String language) {
        return deleteByQuery(indexName(projectUuid, branchUuid),
                RagChunkDocuments.nodeLanguageQuery(nodeUuid, language));
    }

    /**
     * Baut einen {@code _delete_by_query}-Request.
     *
     * <p>Mesh kennt dafuer keinen Request-Typ – {@code SearchProvider#getClient()} gibt aber den
     * rohen Client heraus, wie ihn {@code ElasticSearchProvider.reIndex()} und
     * {@code RoleDeletedEventHandler} ebenfalls verwenden. Hier muss der Praefix selbst davor,
     * anders als beim Weg ueber {@link MeshHelper}.
     */
    private SearchRequest deleteByQuery(String index, Map<String, Object> query) {
        return SearchRequest.create(provider -> Completable.defer(() -> {
            ElasticsearchClient<JsonObject> client = provider.getClient();
            if (client == null) {
                // DevNull- oder Tracking-Provider (Tests).
                return Completable.complete();
            }
            String fullIndex = provider.installationPrefix() + index;
            JsonObject body = new JsonObject().put("query", new JsonObject(query));
            return client.postBuilder(fullIndex + "/_delete_by_query", body)
                    .addQueryParameter("conflicts", "proceed")
                    .addQueryParameter("ignore_unavailable", "true")
                    .async()
                    .ignoreElement();
        }));
    }

    /** @return Installations-Praefix, wie ihn der rohe Client-Pfad braucht */
    String installationPrefix() {
        return searchProvider.installationPrefix();
    }

    private static String nodeUuid(HibNodeFieldContainer container) {
        return com.gentics.mesh.core.db.Tx.get().contentDao().getNode(container).getUuid();
    }

    private static String languageOf(HibNodeFieldContainer container) {
        return container.getLanguageTag();
    }
}
