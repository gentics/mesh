package com.gentics.mesh.contentrag.service.es;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Schmaler Wrapper um den Low-Level-RestClient (Konzept 5.6). Baut/parst JSON via Jackson
 * und kapselt die wenigen benoetigten Elasticsearch-Operationen. Bewusst minimal gehalten,
 * damit die Query-Konstruktion explizit und versionsrobust bleibt.
 */
@Component
public class ElasticsearchGateway {

    private final RestClient client;
    private final ObjectMapper mapper;

    /**
     * @param client Low-Level-RestClient
     * @param mapper Jackson-ObjectMapper fuer JSON-Bau/-Parsing
     */
    public ElasticsearchGateway(RestClient client, ObjectMapper mapper) {
        this.client = client;
        this.mapper = mapper;
    }

    /**
     * @return der von diesem Gateway genutzte ObjectMapper (zum Bauen von Query-JSON)
     */
    public ObjectMapper mapper() {
        return mapper;
    }

    /**
     * Prueft, ob ein konkreter Index existiert.
     *
     * @param index Indexname
     * @return {@code true}, wenn der Index vorhanden ist
     */
    public boolean indexExists(String index) {
        try {
            Response r = client.performRequest(new Request("HEAD", "/" + index));
            return r.getStatusLine().getStatusCode() == 200;
        } catch (ResponseException e) {
            if (e.getResponse().getStatusLine().getStatusCode() == 404) {
                return false;
            }
            throw new EsException("indexExists fehlgeschlagen fuer " + index, e);
        } catch (IOException e) {
            throw new EsException("indexExists fehlgeschlagen fuer " + index, e);
        }
    }

    /** Legt den Index mit dem uebergebenen Mapping/Settings-Body an (No-Op, wenn bereits vorhanden). */
    public void createIndex(String index, JsonNode mappingAndSettings) {
        if (indexExists(index)) {
            return;
        }
        Request req = new Request("PUT", "/" + index);
        req.setJsonEntity(toJson(mappingAndSettings));
        perform(req, "createIndex " + index, false);
    }

    /**
     * Bulk-Index einer Liste von Dokumenten. Jede ObjectNode muss ein Feld {@code _id} enthalten,
     * das als Dokument-ID verwendet (und aus dem Source entfernt) wird.
     */
    public void bulkIndex(String index, List<ObjectNode> docs) {
        if (docs.isEmpty()) {
            return;
        }
        StringBuilder ndjson = new StringBuilder();
        for (ObjectNode doc : docs) {
            String id = doc.path("_id").asText();
            ObjectNode source = doc.deepCopy();
            source.remove("_id");
            ndjson.append("{\"index\":{\"_id\":\"").append(id).append("\"}}\n");
            ndjson.append(toJson(source)).append('\n');
        }
        Request req = new Request("POST", "/" + index + "/_bulk");
        req.addParameter("refresh", "wait_for");
        req.setEntity(new StringEntity(ndjson.toString(),
                ContentType.create("application/x-ndjson", java.nio.charset.StandardCharsets.UTF_8)));
        JsonNode resp = perform(req, "bulk " + index, false);
        if (resp != null && resp.path("errors").asBoolean(false)) {
            throw new EsException("Bulk-Index meldete Fehler: " + resp.path("items"));
        }
    }

    /**
     * Loescht alle Treffer der Query. {@code index} darf konkret oder ein Wildcard-Ausdruck sein;
     * fehlende konkrete Indizes (HTTP 404) werden ignoriert.
     */
    public void deleteByQuery(String index, JsonNode query) {
        ObjectNode body = mapper.createObjectNode();
        body.set("query", query);
        Request req = new Request("POST", "/" + index + "/_delete_by_query");
        req.addParameter("refresh", "true");
        req.addParameter("conflicts", "proceed");
        req.addParameter("ignore_unavailable", "true");
        req.setJsonEntity(toJson(body));
        perform(req, "deleteByQuery " + index, true);
    }

    /**
     * Loescht einen Index. Ein nicht vorhandener Index (HTTP 404) wird ignoriert.
     *
     * @param index Indexname
     */
    public void deleteIndex(String index) {
        Request req = new Request("DELETE", "/" + index);
        perform(req, "deleteIndex " + index, true);
    }

    /** Fuehrt eine Suche aus. {@code indexExpr} darf ein Wildcard/Komma-Ausdruck sein. */
    public JsonNode search(String indexExpr, JsonNode body) {
        Request req = new Request("POST", "/" + indexExpr + "/_search");
        req.addParameter("ignore_unavailable", "true");
        req.setJsonEntity(toJson(body));
        JsonNode resp = perform(req, "search " + indexExpr, true);
        return resp == null ? mapper.createObjectNode() : resp;
    }

    private JsonNode perform(Request req, String what, boolean ignore404) {
        try {
            Response r = client.performRequest(req);
            return parse(r);
        } catch (ResponseException e) {
            int code = e.getResponse().getStatusLine().getStatusCode();
            if (ignore404 && code == 404) {
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
