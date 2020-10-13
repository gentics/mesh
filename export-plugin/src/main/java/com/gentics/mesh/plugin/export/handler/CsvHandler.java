package com.gentics.mesh.plugin.export.handler;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.export.CsvConverter;
import com.gentics.mesh.plugin.export.ObjectCache;
import com.gentics.mesh.plugin.export.exception.GraphQLRestException;
import com.gentics.mesh.plugin.export.model.*;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.MeshWebrootResponse;
import graphql.GraphQLException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.subjects.ReplaySubject;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public class CsvHandler implements Handler<PluginContext> {

    private static final String VARIABLE_KEY_PAGE = "page";
    private static final String VARIABLE_KEY_PER_PAGE = "perPage";

    private static final String RESPONSE_KEY_NODES = "nodes";
    private static final String RESPONSE_KEY_HAS_NEXT_PAGE = "hasNextPage";
    private static final String RESPONSE_KEY_ELEMENTS = "elements";

    private static final String REFERENCE_CONTENT_CONTAINER_NAME = "__refs";
    private static final String REFERENCE_UUID_PLACEHOLDER = "%uuid%";

    private static final String CONTENT_TYPE_KEY = "content-type";
    private static final String CONTENT_TYPE_VALUE = "text/csv;charset=UTF-8";

    private static Logger log = LoggerFactory.getLogger(CsvHandler.class);

    private MeshRestClient client;
    private PluginConfig config;
    private ObjectCache<JsonObject> configNodeCache;

    public CsvHandler(MeshRestClient client, PluginConfig config) {
        this.client = client;
        this.config = config;
        this.configNodeCache = new ObjectCache<>(config.getConfigNodesCacheTimeMs());
    }

    @Override
    public void handle(PluginContext rc) {
        String projectName = rc.projectName();
        CsvRequest request = rc.get(DecodeValidateHandler.CONTEXT_PROPERTY_NAME);
        log.debug("Received Request on project '" + projectName + "'!");
        rc.response().setChunked(true);
        AtomicBoolean wroteHeader = new AtomicBoolean(false);

        this.streamDataRows(request, projectName).subscribe(str -> {
            if (!wroteHeader.get()) {
                rc.response().putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE);
                rc.response().write("\ufeff"); // UTF-8 BOM Header
                rc.response().write(this.createCsvHeader(request));
                wroteHeader.set(true);
            }
            rc.response().write(str);
        }, (err) -> {
            if (err instanceof IllegalStateException && rc.response().closed()) {
                // Ignore, it's an "error" that the request has been canceled.
                return;
            }
            log.error(err, err.getStackTrace());
            rc.fail(err);
        }, rc.response()::end);
    }

    public Observable<String> streamDataRows(CsvRequest request, String projectName) {
        Single<JsonObject> config = this.getValueConfig(projectName);
        List<CsvMappingDefinition> referenceEntries = request
                .getMapping()
                .stream()
                .filter(entry -> entry.getType() == CsvMappingDataType.FOREIGN_REFERENCE
                        || entry.getListType() == CsvMappingDataType.FOREIGN_REFERENCE)
                .collect(Collectors.toList());
        boolean hasReferenceField = !referenceEntries.isEmpty();
        GraphQLRequest graphRequest = request.buildGraphQLRequest();
        CsvConverter converter = new CsvConverter();
        log.debug("Has Reference Mapping Fields: " + hasReferenceField);

        if (hasReferenceField) {
            graphRequest.getVariables().put(VARIABLE_KEY_PER_PAGE, 1);
        }

        return this
                .getResults(graphRequest, projectName)
                .flatMap(response -> Observable.fromIterable(response.getJsonArray(RESPONSE_KEY_ELEMENTS)))
                .flatMapSingle(element -> {
                    JsonObject obj = (JsonObject) element;
                    if (!hasReferenceField) {
                        return Single.just(obj);
                    } else {
                        return this.enrichObjectsWithReferences(obj, referenceEntries, projectName);
                    }
                })
                .concatMap(data -> converter
                        .transformToCsv(data, request, config)
                        .flatMapObservable(Observable::fromIterable));
    }

    private String createCsvHeader(CsvRequest request) {
        CsvStringWriter writer = new CsvStringWriter();
        List<String> labels = new ArrayList<>();

        for (CsvMappingDefinition entry : request.getMapping()) {
            CsvMappingDataType type = entry.getType();
            if (type == CsvMappingDataType.LIST) {
                type = entry.getListType();
            }
            switch (type) {
                case SUB_NODE:
                case FOREIGN_REFERENCE:
                    for (CsvSubMappingDefinition sub : entry.getSubMapping()) {
                        labels.add(sub.getLabel());
                    }
                    break;
                default:
                    labels.add(entry.getLabel());
                    break;
            }
        }

        writer.writeRow(labels);
        return writer.toString();
    }

    public Single<JsonObject> getValueConfig(String projectName) {
        // cache the value config into a replay subject to ensure
        // we only merge the configs once per request
        ReplaySubject<JsonObject> subject = ReplaySubject.createWithSize(1);
        Observable
                .fromIterable(this.config.getConfigPaths())
                .flatMapSingle(webRootPath -> this.configNodeCache
                        .computeIfAbsent(webRootPath, () -> this.retrieveConfigNodeFromPath(projectName, webRootPath))
                        .onErrorReturnItem(new JsonObject()))
                .collectInto(new JsonObject(), (combined, current) -> combined.mergeIn(current, true))
                .toObservable()
                .subscribe(subject);
        return subject.singleOrError();
    }

    private Single<JsonObject> retrieveConfigNodeFromPath(String projectName, String path) {
        Observable<MeshWebrootResponse> obs;
        try {
            obs = this.client.webroot(projectName, path).toObservable();
        } catch (Exception e) {
            obs = Observable.error(e);
        }
        final Single<JsonObject> objectSingle = obs
                .filter(r -> !r.isBinary())
                .firstOrError()
                .map(MeshWebrootResponse::getNodeResponse)
                .map(NodeResponse::getFields)
                .map(fieldMap -> new JsonObject(fieldMap.toJson()))
                .doOnError(err -> log.warn("Error encountered when retrieving config node", err));
        return objectSingle;
    }

    private Observable<JsonObject> getResults(GraphQLRequest graphQLRequest, String projectName) {
        log.debug("Getting Results from page: " + graphQLRequest.getVariables().getLong(VARIABLE_KEY_PAGE));
        return this.client
                .graphql(projectName, graphQLRequest)
                .toObservable()
                .map(response -> {
                    if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                        throw new GraphQLRestException(new HashSet<>(response.getErrors()));
                    }
                    if (response.getData() == null) {
                        throw new GenericRestException(HttpResponseStatus.INTERNAL_SERVER_ERROR,
                                "GraphQL Request succeeded without errors, but also without data.");
                    }
                    return response.getData();
                })
                .filter(data -> data.containsKey(RESPONSE_KEY_NODES))
                .map(data -> data.getJsonObject(RESPONSE_KEY_NODES))
                .flatMap(nodes -> {
                    if (nodes.containsKey(RESPONSE_KEY_HAS_NEXT_PAGE) && nodes.getBoolean(RESPONSE_KEY_HAS_NEXT_PAGE)
                            && graphQLRequest.getVariables().containsKey(VARIABLE_KEY_PAGE)) {
                        Long nextPageNr = graphQLRequest.getVariables().getLong(VARIABLE_KEY_PAGE) + 1;
                        log.debug("Result has next page start fetching page {}", nextPageNr);

                        GraphQLRequest nextPage = new GraphQLRequest();
                        nextPage.setOperationName(graphQLRequest.getOperationName());
                        nextPage.setQuery(graphQLRequest.getQuery());
                        nextPage.setVariables(graphQLRequest.getVariables().copy().put(VARIABLE_KEY_PAGE, nextPageNr));

                        return Observable.concat(Observable.just(nodes), this.getResults(nextPage, projectName));
                    }
                    log.debug("Current page is the last, we don't need to fetch more pages.");
                    return Observable.just(nodes);
                });
    }

    private void enrichObjectWithResult(
            JsonObject obj, CsvMappingDefinition entry, boolean isList, GraphQLResponse response
    ) {
        // Get the elements from the response
        JsonArray elements = response
                .getData()
                .getJsonObject(RESPONSE_KEY_NODES, new JsonObject())
                .getJsonArray(RESPONSE_KEY_ELEMENTS, new JsonArray());

        // Put the found content into the element
        if (isList) {
            obj.getJsonObject(REFERENCE_CONTENT_CONTAINER_NAME).put(entry.getPath(), elements);
        } else {
            obj
                    .getJsonObject(REFERENCE_CONTENT_CONTAINER_NAME)
                    .put(entry.getPath(), elements.isEmpty() ? null : elements.getValue(0));
        }
    }

    protected SingleSource<? extends JsonObject> enrichObjectsWithReferences(
            JsonObject obj, List<CsvMappingDefinition> referenceEntries, String projectName
    ) {
        String uuid = obj.getString("uuid", "<uuid-not-found>");
        obj.put(REFERENCE_CONTENT_CONTAINER_NAME, new JsonObject());

        return Observable.fromIterable(referenceEntries).flatMapCompletable(entry -> {
            log.debug("Fetching references for entry: " + entry.getLabel());
            boolean isList = entry.getType() == CsvMappingDataType.LIST;

            // Build a graphql request based on the entry
            JsonObject variables = new JsonObject();
            variables.put("query", entry.getReferenceSearchQuery().replaceAll(REFERENCE_UUID_PLACEHOLDER, uuid));
            if (isList) {
                variables.put("perPage", entry.getReferenceMaxCount());
            } else {
                variables.put("perPage", 1);
            }
            GraphQLRequest request = new GraphQLRequest();
            request.setQuery(entry.getReferenceGraphQuery());
            request.setVariables(variables);

            // Send the request and fetch the elements
            return this.client.graphql(projectName, request).toSingle().map(response -> {
                if (response.getErrors() != null && !response.getErrors().isEmpty()) {
                    throw new GraphQLException(response.getErrors().get(0).getMessage());
                }
                return response;
            }).doOnSuccess(response -> enrichObjectWithResult(obj, entry, isList, response)).ignoreElement();
        }).andThen(Single.just(obj));

    }
}
