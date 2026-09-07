package com.gentics.mesh.contentrag.meshsync;

import com.gentics.mesh.contentrag.index.RagIndexNaming;
import com.gentics.mesh.search.SearchProvider;

import com.gentics.elasticsearch.client.ElasticsearchClient;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Wacht darueber, dass Mesh erst dann in den RAG-Index schreibt, wenn der rag-service seine
 * Vorarbeit geleistet hat.
 *
 * <p><strong>Warum es diese Klasse gibt.</strong> Ein Index-Template wirkt in Elasticsearch
 * ausschliesslich im Moment der Index-Erstellung. Der RAG-Index wird nicht von Mesh angelegt –
 * {@code RagChunkIndexHandler#getIndices()} ist leer –, sondern implizit vom ersten Bulk-Write.
 * Faellt dieser erste Write in das Zeitfenster, bevor der rag-service sein Template angelegt hat,
 * entsteht der Index mit dynamischem Mapping: jedes Textfeld wird {@code text} statt
 * {@code keyword}, das Feld {@code embedding} fehlt ganz, {@code default_pipeline} ist nicht
 * gesetzt. Der Index sieht danach gefuellt und gesund aus, ist aber fuer das Retrieval wertlos –
 * und er repariert sich nie von selbst, denn das Template greift nachtraeglich nicht mehr. Genau
 * das ist am 07.09.2026 auf dem Testsystem passiert.
 *
 * <p><strong>Warum das Template der richtige Marker ist.</strong> {@code RagIndexBootstrap} legt
 * in der Reihenfolge Inference-Endpunkt, Ingest-Pipeline, Index-Template an. Das Template kommt
 * zuletzt – existiert es, existiert der Rest zwangslaeufig auch. Eine einzige Abfrage genuegt
 * also. <em>Diese Reihenfolge ist Teil des Vertrags:</em> wer sie in {@code RagIndexBootstrap}
 * umstellt, macht diese Pruefung stillschweigend unwahr.
 *
 * <p><strong>Weich statt hart.</strong> Fehlt das Template, scheitert Mesh nicht – anders als beim
 * Praefix-Guard in {@code RagChunkIndexHandler#init()}. Mesh und rag-service starten unabhaengig
 * voneinander; ein harter Fehler wuerde die gesamte Mesh-Suche vom Startzeitpunkt des rag-service
 * abhaengig machen. Stattdessen setzt der RAG-Sync das Schreiben aus, und {@code check()} fragt
 * periodisch nach. Sobald das Template da ist, nimmt der naechste Lauf den Betrieb auf.
 */
@Singleton
public class RagIndexReadiness {

    private static final Logger log = LoggerFactory.getLogger(RagIndexReadiness.class);

    private final SearchProvider searchProvider;
    private final RagSyncOptions options;

    /** Von {@link #probe()} geschrieben, von beiden Handlern gelesen. */
    private volatile boolean ready;

    /** Damit das fehlende Template nicht bei jedem Durchlauf erneut gemeldet wird. */
    private volatile boolean missingReported;

    /**
     * @param searchProvider fuer den rohen Client und den Installations-Praefix
     * @param options        Betriebsparameter; bei {@code enabled=false} passiert hier nichts
     */
    @Inject
    public RagIndexReadiness(SearchProvider searchProvider, RagSyncOptions options) {
        this.searchProvider = searchProvider;
        this.options = options;
    }

    /**
     * @return {@code true}, wenn zuletzt ein Index-Template gefunden wurde und geschrieben werden
     *         darf
     */
    public boolean isReady() {
        return ready;
    }

    /**
     * Name des Index-Templates, so wie ihn der rag-service anlegt.
     *
     * <p>Abgeleitet aus Praefix und Infix, weil der rag-service es genauso bildet. Bekommt
     * {@code RagIndexNaming} irgendwann ein eigenes {@code templateName(prefix)}, gehoert der
     * Aufruf hierher – dann steht die Ableitung nur noch an einer Stelle.
     *
     * @return Templatename, etwa {@code mesh-ragchunk}
     */
    public String templateName() {
        return searchProvider.installationPrefix() + RagIndexNaming.INFIX;
    }

    /**
     * Fragt einmal nach, ob das Index-Template existiert, und merkt sich das Ergebnis.
     *
     * <p>Schlaegt nie fehl: ein Fehler bei der Abfrage wird wie „nicht vorhanden" behandelt. Ein
     * nicht erreichbares Elasticsearch soll den Start von Mesh nicht verhindern, und beim
     * naechsten {@code check()} wird ohnehin erneut gefragt.
     *
     * @return Completable, das nach der Abfrage abschliesst
     */
    public Completable probe() {
        if (!options.enabled()) {
            ready = false;
            return Completable.complete();
        }
        ElasticsearchClient<JsonObject> client = searchProvider.getClient();
        if (client == null) {
            // DevNull- oder Tracking-Provider (Tests): dort gibt es kein Template und auch keinen
            // Index, der kaputtgehen koennte.
            ready = true;
            return Completable.complete();
        }
        String path = "_index_template/" + templateName();
        // Completable.fromSingle statt Single#ignoreElement: die RxJava-Version im Mesh-Prozess
        // kennt ignoreElement() auf Single noch nicht.
        return Completable.fromSingle(client.getBuilder(path).async()
                .map(response -> Boolean.TRUE)
                .onErrorReturn(error -> Boolean.FALSE)
                .doOnSuccess(this::update));
    }

    /**
     * Meldet vorhandene RAG-Indizes, denen {@code index.default_pipeline} fehlt.
     *
     * <p>Das ist genau die Signatur eines Index, der vor dem Template entstanden ist. Ohne diese
     * Meldung faellt so ein Index nie wieder auf: er nimmt Dokumente an, meldet keinen Fehler und
     * liefert nur beim Retrieval nichts. Repariert wird er nicht automatisch – dazu muesste er
     * geloescht und neu aufgebaut werden, und das ist eine Entscheidung fuer den Betrieb, nicht
     * fuer Mesh.
     *
     * @return Completable, das nach der Abfrage abschliesst
     */
    public Completable reportBrokenIndices() {
        if (!options.enabled()) {
            return Completable.complete();
        }
        ElasticsearchClient<JsonObject> client = searchProvider.getClient();
        if (client == null) {
            return Completable.complete();
        }
        String pattern = searchProvider.installationPrefix() + RagIndexNaming.INFIX + "-*";
        return Completable.fromSingle(client.getBuilder(pattern + "/_settings")
                .addQueryParameter("ignore_unavailable", "true")
                .async()
                .map(RagIndexReadiness::withoutPipeline)
                .onErrorReturn(error -> new ArrayList<String>())
                .doOnSuccess(RagIndexReadiness::report));
    }

    private void update(Boolean found) {
        boolean present = Boolean.TRUE.equals(found);
        boolean previous = ready;
        ready = present;
        if (present && !previous) {
            log.info("Index-Template {} gefunden – der RAG-Sync schreibt ab jetzt.", templateName());
            missingReported = false;
        } else if (!present && previous) {
            log.warn("Index-Template {} ist nicht mehr da – der RAG-Sync setzt das Schreiben aus.",
                    templateName());
            missingReported = true;
        } else if (!present && !missingReported) {
            missingReported = true;
            log.warn("Index-Template {} fehlt. Der RAG-Sync schreibt nicht, damit kein Index mit"
                    + " dynamischem Mapping entsteht. Laeuft der rag-service? Sobald das Template"
                    + " da ist, nimmt der naechste Index-Check den Betrieb auf.", templateName());
        }
    }

    /**
     * Liest aus einer {@code _settings}-Antwort die Indizes heraus, bei denen
     * {@code index.default_pipeline} fehlt.
     */
    private static List<String> withoutPipeline(JsonObject response) {
        List<String> broken = new ArrayList<>();
        for (Map.Entry<String, Object> entry : response) {
            if (!(entry.getValue() instanceof JsonObject index)) {
                continue;
            }
            JsonObject settings = index.getJsonObject("settings");
            JsonObject inner = settings == null ? null : settings.getJsonObject("index");
            if (inner == null || inner.getString("default_pipeline") == null) {
                broken.add(entry.getKey());
            }
        }
        return broken;
    }

    private static void report(List<String> broken) {
        if (broken.isEmpty()) {
            return;
        }
        log.warn("Diese RAG-Indizes sind ohne das Index-Template entstanden und haben weder"
                + " Embeddings noch ein brauchbares Mapping: {}. Sie liefern beim Retrieval nichts."
                + " Reparatur: Index loeschen und einen Voll-Sync anstossen.", String.join(", ", broken));
    }
}
