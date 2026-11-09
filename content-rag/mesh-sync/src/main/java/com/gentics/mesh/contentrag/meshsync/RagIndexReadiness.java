package com.gentics.mesh.contentrag.meshsync;

import com.gentics.mesh.contentrag.index.RagIndexNaming;
import com.gentics.mesh.search.SearchProvider;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;

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
 * <p><strong>Fehlt oder antwortet nicht?</strong> Beide Faelle fuehren zur selben Entscheidung –
 * nicht schreiben –, verlangen aber verschiedene Reaktionen: ein fehlendes Template heisst
 * „rag-service starten", ein nicht erreichbares Elasticsearch heisst etwas ganz anderes. Sie
 * werden deshalb getrennt gemeldet. Beides in eine Meldung zu werfen hat bei der Inbetriebnahme
 * eine Fehlersuche in die falsche Richtung geschickt.
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

    /** Zuletzt gemeldeter Zustand – gemeldet wird nur, was sich geaendert hat. */
    private volatile State reported;

    /** Ergebnis einer Abfrage. */
    private enum State {
        /** Template vorhanden. */
        PRESENT,
        /** Elasticsearch antwortet, kennt das Template aber nicht. */
        MISSING,
        /** Elasticsearch antwortet nicht oder mit einem anderen Fehler. */
        UNREACHABLE
    }

    /**
     * @param state  Ergebnis
     * @param detail Fehlertext bei {@link State#UNREACHABLE}, sonst {@code null}
     */
    private record Probe(State state, String detail) {
    }

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
     * <p>Ueber {@link RagIndexNaming#templateName(String)} gebildet – derselbe Aufruf, den der
     * rag-service beim Anlegen verwendet. Die Ableitung steht damit an genau einer Stelle; eine
     * eigene Nachbildung hier waere ein zweiter Ort, an dem sie stillschweigend auseinanderlaufen
     * koennte.
     *
     * @return Templatename, etwa {@code mesh-ragchunk}
     */
    public String templateName() {
        return RagIndexNaming.templateName(searchProvider.installationPrefix());
    }

    /**
     * Fragt einmal nach, ob das Index-Template existiert, und merkt sich das Ergebnis.
     *
     * <p>Schlaegt nie fehl: ein Fehler bei der Abfrage laesst den Sync ebenfalls pausieren, wird
     * aber als solcher gemeldet und nicht als fehlendes Template. Ein nicht erreichbares
     * Elasticsearch soll den Start von Mesh nicht verhindern, und beim naechsten {@code check()}
     * wird ohnehin erneut gefragt.
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
                .map(response -> new Probe(State.PRESENT, null))
                .onErrorReturn(RagIndexReadiness::classify)
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
        String pattern = RagIndexNaming.allIndices(searchProvider.installationPrefix());
        return Completable.fromSingle(client.getBuilder(pattern + "/_settings")
                .addQueryParameter("ignore_unavailable", "true")
                .async()
                .map(RagIndexReadiness::withoutPipeline)
                .onErrorReturn(error -> {
                    log.debug("Settings der RAG-Indizes nicht abfragbar: {}", error.getMessage());
                    return new ArrayList<String>();
                })
                .doOnSuccess(RagIndexReadiness::report));
    }

    /**
     * Ordnet einen Fehler bei der Abfrage einem Zustand zu. Nur ein 404 heisst „Template gibt es
     * nicht"; jeder andere Fehler sagt etwas ueber Elasticsearch aus, nicht ueber das Template.
     */
    private static Probe classify(Throwable error) {
        if (error instanceof HttpErrorException http) {
            if (http.getStatusCode() == 404) {
                return new Probe(State.MISSING, null);
            }
            return new Probe(State.UNREACHABLE, "HTTP " + http.getStatusCode());
        }
        return new Probe(State.UNREACHABLE,
                error.getClass().getSimpleName() + ": " + error.getMessage());
    }

    /** Uebernimmt das Ergebnis und meldet nur, was sich gegenueber der letzten Abfrage geaendert hat. */
    private void update(Probe probe) {
        ready = probe.state() == State.PRESENT;
        if (probe.state() == reported) {
            return;
        }
        reported = probe.state();
        switch (probe.state()) {
            case PRESENT -> log.info("Index-Template {} gefunden – der RAG-Sync schreibt ab jetzt.",
                    templateName());
            case MISSING -> log.warn("Index-Template {} fehlt. Der RAG-Sync schreibt nicht, damit"
                    + " kein Index mit dynamischem Mapping entsteht. Laeuft der rag-service? Sobald"
                    + " das Template da ist, nimmt der naechste Index-Check den Betrieb auf.",
                    templateName());
            case UNREACHABLE -> log.warn("Elasticsearch beantwortet die Frage nach dem"
                    + " Index-Template {} nicht ({}). Ob es vorhanden ist, ist damit unbekannt, und"
                    + " der RAG-Sync schreibt vorerst nicht. Der naechste Index-Check fragt erneut.",
                    templateName(), probe.detail());
            default -> { }
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
