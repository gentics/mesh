package com.gentics.mesh.graphql;

import static graphql.GraphQL.newGraphQL;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.dataloader.DataLoader;
import org.dataloader.DataLoaderOptions;
import org.dataloader.DataLoaderRegistry;

import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.error.AbstractUnavailableException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.NativeQueryFiltering;
import com.gentics.mesh.graphql.context.GraphQLContext;
import com.gentics.mesh.graphql.dataloader.NodeDataLoader;
import com.gentics.mesh.graphql.type.QueryTypeProvider;
import com.gentics.mesh.graphql.type.field.FieldDefinitionProvider;
import com.gentics.mesh.metric.MetricsService;
import com.gentics.mesh.metric.SimpleMetric;

import graphql.ExceptionWhileDataFetching;
import graphql.ExecutionInput;
import graphql.ExecutionResult;
import graphql.GraphQL;
import graphql.GraphQLError;
import graphql.execution.instrumentation.dataloader.DataLoaderDispatcherInstrumentation;
import graphql.language.SourceLocation;
import io.micrometer.core.instrument.Timer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.Vertx;

/**
 * Handler for GraphQL REST endpoint operations.
 */
@Singleton
public class GraphQLHandler {
	public static final String SLOW_QUERY_LOGGER_NAME = "com.gentics.mesh.graphql.SlowQuery";

	private static final Logger log = LoggerFactory.getLogger(GraphQLHandler.class);

	private static final Logger slowQueryLog = LoggerFactory.getLogger(SLOW_QUERY_LOGGER_NAME);

	protected final QueryTypeProvider typeProvider;

	protected final Database db;

	protected final Vertx vertx;

	private Timer graphQlTimer;

	private MeshOptions options;

	@Inject
	public GraphQLHandler(MetricsService metrics, MeshOptions options, QueryTypeProvider typeProvider, Database db, Vertx vertx) {
		this.graphQlTimer = metrics.timer(SimpleMetric.GRAPHQL_TIME);
		this.options = options;
		this.typeProvider = typeProvider;
		this.db = db;
		this.vertx = vertx;
	}

	/**
	 * Handle the GraphQL query.
	 *
	 * @param gc
	 *            Context
	 * @param body
	 *            GraphQL query
	 */
	public void handleQuery(GraphQLContext gc, String body) {
		vertx.rxExecuteBlocking(promise -> {
			Timer.Sample sample = Timer.start();
			AtomicReference<String> loggableQuery = new AtomicReference<>();
			AtomicReference<Map<String, Object>> loggableVariables = new AtomicReference<>();
			try {
				db.tx(tx -> {
					JsonObject queryJson = new JsonObject(body);
					// extract query body and variables from the sent body
					String query = queryJson.getString("query");
					Map<String, Object> variables = extractVariables(queryJson);
					// store for possibly logging it later
					loggableQuery.set(query);
					loggableVariables.set(variables);
					GraphQL graphQL = newGraphQL(typeProvider.getRootSchema(gc)).instrumentation(new DataLoaderDispatcherInstrumentation()).build();

					DataLoaderRegistry dataLoaderRegistry = new DataLoaderRegistry();
					DataLoaderOptions dlOptions = DataLoaderOptions.newOptions().setBatchLoaderContextProvider(() -> gc);
					// TODO this is inefficient, but otherwise we can't easily tell if a filter is native ready without parsing each of it ahead of time.
					dlOptions.setCachingEnabled(options.getNativeQueryFiltering() == NativeQueryFiltering.NEVER);
					dataLoaderRegistry.register(NodeDataLoader.CONTENT_LOADER_KEY, DataLoader.newDataLoader(NodeDataLoader.CONTENT_LOADER, dlOptions));
					dataLoaderRegistry.register(NodeDataLoader.CHILDREN_LOADER_KEY, DataLoader.newDataLoader(NodeDataLoader.CHILDREN_LOADER, dlOptions));
					dataLoaderRegistry.register(NodeDataLoader.PATH_LOADER_KEY, DataLoader.newDataLoader(NodeDataLoader.PATH_LOADER, dlOptions));
					dataLoaderRegistry.register(NodeDataLoader.PARENT_LOADER_KEY, DataLoader.newDataLoader(NodeDataLoader.PARENT_LOADER, dlOptions));
					dataLoaderRegistry.register(NodeDataLoader.REFERENCED_BY_LOADER_KEY, DataLoader.newDataLoader(NodeDataLoader.REFERENCED_BY_LOADER, dlOptions));
					dataLoaderRegistry.register(NodeDataLoader.BREADCRUMB_LOADER_KEY, DataLoader.newDataLoader(NodeDataLoader.BREADCRUMB_LOADER, dlOptions));
					dataLoaderRegistry.register(FieldDefinitionProvider.LINK_REPLACER_DATA_LOADER_KEY, DataLoader.newDataLoader(typeProvider.getFieldDefProvider().LINK_REPLACER_LOADER, dlOptions));
					dataLoaderRegistry.register(FieldDefinitionProvider.BOOLEAN_LIST_VALUES_DATA_LOADER_KEY, DataLoader.newDataLoader(typeProvider.getFieldDefProvider().BOOLEAN_LIST_VALUE_LOADER, dlOptions));
					dataLoaderRegistry.register(FieldDefinitionProvider.DATE_LIST_VALUES_DATA_LOADER_KEY, DataLoader.newDataLoader(typeProvider.getFieldDefProvider().DATE_LIST_VALUE_LOADER, dlOptions));
					dataLoaderRegistry.register(FieldDefinitionProvider.NUMBER_LIST_VALUES_DATA_LOADER_KEY, DataLoader.newDataLoader(typeProvider.getFieldDefProvider().NUMBER_LIST_VALUE_LOADER, dlOptions));
					dataLoaderRegistry.register(FieldDefinitionProvider.HTML_LIST_VALUES_DATA_LOADER_KEY, DataLoader.newDataLoader(typeProvider.getFieldDefProvider().HTML_LIST_VALUE_LOADER, dlOptions));
					dataLoaderRegistry.register(FieldDefinitionProvider.STRING_LIST_VALUES_DATA_LOADER_KEY, DataLoader.newDataLoader(typeProvider.getFieldDefProvider().STRING_LIST_VALUE_LOADER, dlOptions));

					ExecutionInput executionInput = ExecutionInput
						.newExecutionInput()
						.dataLoaderRegistry(dataLoaderRegistry)
						.query(query)
						.context(gc)
						.variables(variables)
						.build();
					try {
						// Implementation Note: GraphQL is implemented synchronously (no implemented datafetcher returns a CompletableFuture, which is not yet completed)
						// Nonetheless, we see sometimes that the CompletableFuture is not completed (and never will be), when GraphQL joins it, which leads to endlessly
						// blocked worker threads.
						// Therefore, we use the "async approach" for calling GraphQL and wait for the result with a timeout.
						ExecutionResult result = graphQL.executeAsync(executionInput)
								.get(options.getGraphQLOptions().getAsyncWaitTimeout(), TimeUnit.MILLISECONDS);
						List<GraphQLError> errors = result.getErrors();
						JsonObject response = new JsonObject();
						if (!errors.isEmpty()) {
							addErrors(errors, response);
							if (log.isDebugEnabled()) {
								log.debug("Encountered {" + errors.size() + "} errors while executing query {" + query + "}");
								for (GraphQLError error : errors) {
									String loc = "unknown location";
									if (error.getLocations() != null) {
										loc = error.getLocations().stream().map(Object::toString).collect(Collectors.joining(","));
									}
									log.debug("Error: " + error.getErrorType() + ":" + error.getMessage() + ":" + loc);
								}
							}
						}
						if (result.getData() != null) {
							Map<String, Object> data = result.getData();
							response.put("data", new JsonObject(data));
						}
						boolean minify = gc.isMinify(options.getHttpServerOptions());
						gc.send(minify ? response.encode() : response.encodePrettily(), OK);
						promise.complete();
					} catch (TimeoutException | InterruptedException | ExecutionException e) {
						// If an error happens while "waiting" for the result, we log the GraphQL query here.
						log.error("GraphQL query failed after {} ms with {}:\n{}\nvariables: {}",
								options.getGraphQLOptions().getAsyncWaitTimeout(), e.getClass().getSimpleName(), loggableQuery.get(),
								loggableVariables.get());
						gc.fail(e);
						promise.fail(e);
					}
				});
			} catch (Exception e) {
				promise.fail(e);
			} finally {
				long duration = sample.stop(graphQlTimer);
				Long slowThreshold = options.getGraphQLOptions().getSlowThreshold();
				if (loggableQuery.get() != null && slowThreshold != null && slowThreshold.longValue() > 0) {
					long durationMs = TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS);
					if (durationMs > slowThreshold) {
						slowQueryLog.warn("GraphQL Query took {} ms:\n{}\nvariables: {}", durationMs, loggableQuery.get(), loggableVariables.get());
					}
				}
			}
		})
		.doOnError(gc::fail)
		.subscribe();
	}

	/**
	 * Extracts the variables of a query as a map. Returns empty map if no variables are found.
	 *
	 * @param request
	 *            The request body
	 *
	 * @return GraphQL variables
	 */
	private Map<String, Object> extractVariables(JsonObject request) {
		JsonObject variables = request.getJsonObject("variables");
		if (variables == null) {
			return Collections.emptyMap();
		} else {
			return variables.getMap();
		}
	}

	/**
	 * Add the listed errors to the response.
	 *
	 * @param errors
	 * @param response
	 */
	private void addErrors(List<GraphQLError> errors, JsonObject response) {
		JsonArray jsonErrors = new JsonArray();
		response.put("errors", jsonErrors);
		for (GraphQLError error : errors) {
			JsonObject jsonError = new JsonObject();
			if (error instanceof ExceptionWhileDataFetching) {
				ExceptionWhileDataFetching dataError = (ExceptionWhileDataFetching) error;
				if (dataError.getException() instanceof AbstractUnavailableException) {
					AbstractUnavailableException restException = (AbstractUnavailableException) dataError.getException();
					// TODO translate error
					// TODO add i18n parameters
					jsonError.put("message", restException.getI18nKey());
					jsonError.put("type", restException.getType());
					jsonError.put("elementId", restException.getElementId());
					jsonError.put("elementType", restException.getElementType());
				} else {
					log.error("Error while fetching data.", dataError.getException());
					jsonError.put("message", dataError.getMessage());
					jsonError.put("type", dataError.getErrorType());
				}
				addLocation(dataError, jsonError);
			} else {
				jsonError.put("message", error.getMessage());
				jsonError.put("type", error.getErrorType().toString());
				addLocation(error, jsonError);
			}
			jsonErrors.add(jsonError);
		}
	}

	private void addLocation(GraphQLError error, JsonObject jsonError) {
		if (error.getPath() != null && !error.getPath().isEmpty()) {
			String path = error.getPath()
				.stream()
				.map(e -> e.toString())
				.collect(Collectors.joining("."));
			jsonError.put("path", path);
		}
		if (error.getLocations() != null && !error.getLocations().isEmpty()) {
			JsonArray errorLocations = new JsonArray();
			jsonError.put("locations", errorLocations);
			for (SourceLocation location : error.getLocations()) {
				JsonObject errorLocation = new JsonObject();
				errorLocation.put("line", location.getLine());
				errorLocation.put("column", location.getColumn());
				errorLocations.add(errorLocation);
			}
		}
	}
}
