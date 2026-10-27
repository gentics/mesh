package com.gentics.mesh.contentrag.service.es;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

/**
 * Schmaler Wrapper um den Low-Level-RestClient. Bewusst minimal: der Service liest Chunks und
 * legt beim Start die drei Cluster-Objekte an (Inference-Endpunkt, Ingest-Pipeline,
 * Index-Template). Chunk-Dokumente schreibt er nicht – das tut Mesh ueber seinen Standard-Sync.
 *
 * <p>Die Query-Konstruktion bleibt bewusst explizit und damit versionsrobust gegenueber
 * Aenderungen der High-Level-Clients.
 */
@Component
public class ElasticsearchGateway {

    private final RestClient client;
    private final ObjectMapper mapper;

    /**
     * @param client Low-Level-RestClient
     * @param mapper Jackson-ObjectMapper fuer JSON-Bau und -Parsing
     */
    public ElasticsearchGateway(RestClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /** @return der von diesem Gateway genutzte ObjectMapper (zum Bauen von Query-JSON) */
    public ObjectMapper mapper() {
        return mapper;
    }

    /**
     * Fuehrt eine Suche aus.
     *
     * @param indexExpr Index, Komma-Liste oder Wildcard-Ausdruck
     * @param body      Query-Body
     * @return Antwort; ein leeres Objekt, wenn keiner der Indizes existiert
     */
    public JsonNode search(String indexExpr, JsonNode body) {
        Request request = new Request("POST", "/" + indexExpr + "/_search");
        request.addParameter("ignore_unavailable", "true");
        request.setJsonEntity(toJson(body));
        JsonNode response = perform(request, "search " + indexExpr, true);
        return response == null ? mapper.createObjectNode() : response;
    }

    /**
     * Zaehlt Dokumente.
     *
     * @param indexExpr Index, Komma-Liste oder Wildcard-Ausdruck
     * @param query     Query oder {@code null} fuer alle Dokumente
     * @return Trefferzahl; 0, wenn keiner der Indizes existiert
     */
    public long count(String indexExpr, JsonNode query) {
        Request request = new Request("POST", "/" + indexExpr + "/_count");
        request.addParameter("ignore_unavailable", "true");
        if (query != null) {
            ObjectNode body = mapper.createObjectNode();
            body.set("query", query);
            request.setJsonEntity(toJson(body));
        }
        JsonNode response = perform(request, "count " + indexExpr, true);
        return response == null ? 0L : response.path("count").asLong(0L);
    }

    /**
     * Prueft, ob unter dem Pfad ein Cluster-Objekt existiert.
     *
     * @param path absoluter API-Pfad, z. B. {@code /_ingest/pipeline/xy}
     * @return {@code true}, wenn der Abruf einen Treffer liefert
     */
    public boolean existsAt(String path) {
        Request request = new Request("GET", path);
        return perform(request, "GET " + path, true) != null;
    }

    /**
     * Legt ein Cluster-Objekt an oder aktualisiert es.
     *
     * @param path absoluter API-Pfad
     * @param body Definition
     */
    public void put(String path, JsonNode body) {
        Request request = new Request("PUT", path);
        request.setJsonEntity(toJson(body));
        perform(request, "PUT " + path, false);
    }

    private JsonNode perform(Request request, String what, boolean ignoreNotFound) {
        try {
            Response response = client.performRequest(request);
            return parse(response);
        } catch (ResponseException e) {
            int code = e.getResponse().getStatusLine().getStatusCode();
            if (ignoreNotFound && code == 404) {
                return null;
            }
            throw new EsException(what + " fehlgeschlagen (HTTP " + code + ")", e);
        } catch (IOException e) {
            throw new EsException(what + " fehlgeschlagen", e);
        }
    }

    private JsonNode parse(Response response) {
        try (InputStream in = response.getEntity().getContent()) {
            return mapper.readTree(in);
        } catch (IOException e) {
            throw new EsException("Antwort konnte nicht gelesen werden", e);
        }
    }

    private String toJson(JsonNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (IOException e) {
            throw new EsException("JSON-Serialisierung fehlgeschlagen", e);
        }
    }
}
