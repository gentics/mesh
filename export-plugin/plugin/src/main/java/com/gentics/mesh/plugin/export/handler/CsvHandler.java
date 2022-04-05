package com.gentics.mesh.plugin.export.handler;

import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.impl.GenericParametersImpl;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.export.CsvConverter;
import com.gentics.mesh.plugin.export.exception.GraphQLRestException;
import com.gentics.mesh.plugin.export.model.*;
import com.gentics.mesh.plugin.export.util.MappingService;
import com.gentics.mesh.plugin.export.util.Utils;
import com.gentics.mesh.rest.client.MeshRestClient;
import graphql.GraphQLException;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.reactivex.SingleSource;
import io.reactivex.disposables.Disposable;
import io.vertx.core.Handler;
import io.vertx.core.VertxException;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Singleton
public class CsvHandler implements Handler<PluginContext> {

	private static final Logger log = LoggerFactory.getLogger(CsvHandler.class);

	private static final String VARIABLE_KEY_PAGE = "page";
	private static final String VARIABLE_KEY_PER_PAGE = "perPage";

	private static final String REFERENCE_CONTENT_CONTAINER_NAME = "__refs";
	private static final String REFERENCE_UUID_PLACEHOLDER = "%uuid%";
	private static final String REFERENCE_VALUE_PLACEHOLDER = "%value%";

	private static final String CONTENT_TYPE_KEY = "content-type";
	private static final String CONTENT_TYPE_VALUE = "text/csv;charset=UTF-8";

	@Inject
	public MeshRestClient client;

	@Inject
	public MappingService mapping;

	@Inject
	public CsvHandler() { }

	@Override
	public void handle(PluginContext rc) {
		String projectName = rc.projectName();
		log.debug("Received Request on project '" + projectName + "'!");

		CsvRequest request = rc.get(DecodeValidateHandler.CONTEXT_PROPERTY_NAME);
		request.setProjectName(projectName);

		// Write the CSV Header directly, to start the response directly which should prevent
		// that a proxy is terminating it for loading too long.
		rc.response().setChunked(true);
		rc.response().putHeader(CONTENT_TYPE_KEY, CONTENT_TYPE_VALUE);
		rc.response().write("\ufeff"); // UTF-8 BOM Header
		rc.response().write(this.createCsvHeader(request));

		Disposable subscription = this.mapping.getConfigValues(projectName)
			.doOnSuccess(request::setConfigValues)
			.flatMapObservable(config -> this.streamDataRows(rc, request))
			.subscribe(str -> {
				rc.response().write(str);
			}, (err) -> {
				if (err instanceof IllegalStateException && rc.response().closed()) {
					// Ignore, it's an "error" that the request has been canceled.
					return;
				}
				log.error(err);
				rc.response().write(Utils.mapThrowableToJsonObject(err).encodePrettily());
				rc.response().setStatusCode(500);
				rc.response().end();
			}, () -> rc.response().end());

		rc.response().exceptionHandler(err -> {
			// Connection was closed, therefore cancel the stream/subscription to
			// free the resources as it would just go into the void now.
			if (err instanceof VertxException && "Connection was closed".equals(err.getMessage())) {
				if (!subscription.isDisposed()) {
					log.debug("Request got canceled and request is still running, therefore canceling it.");
					subscription.dispose();
				}
			}
		});
	}

	public Observable<String> streamDataRows(PluginContext rc, CsvRequest request) {
		List<CsvMappingDefinition> referenceEntries = request
			.getMapping()
			.stream()
			.filter(entry -> entry.getActualType() == CsvMappingDataType.FOREIGN_REFERENCE)
			.collect(Collectors.toList());
		boolean hasReferenceField = !referenceEntries.isEmpty();
		GraphQLRequest graphRequest = request.buildGraphQLRequest();
		ConvertingOptions options = new ConvertingOptions()
			.setMapping(request.getMapping())
			.setConfigValues(request.getConfigValues())
			.setBooleanMapping(request.getBooleanMapping())
			.setDateFormatter(request.getDateFormatter())
			.setNumberFormatter(request.getNumberFormatter());

		log.debug("Has Reference Mapping Fields: " + hasReferenceField);

		// If the limit would be reached within one page, limit the page size
		if (
			request.getMaximalNodes() != null &&
				request.getMaximalNodes() > 0 &&
				graphRequest.getVariables().getInteger(VARIABLE_KEY_PER_PAGE) > request.getMaximalNodes()
		) {
			graphRequest.getVariables().put(VARIABLE_KEY_PER_PAGE, request.getMaximalNodes());
		}

		if (hasReferenceField) {
			graphRequest.getVariables().put(VARIABLE_KEY_PER_PAGE, 1);
		}

		return Observable.defer(() -> {
			AtomicBoolean hasMore = new AtomicBoolean(false);
			AtomicBoolean hasFoundElement = new AtomicBoolean(false);
			AtomicInteger totalNodes = new AtomicInteger(0);

			return this.loadData(rc, request, graphRequest)
				.flatMapObservable(response -> {
					hasMore.set(response.isHasMore());
					hasFoundElement.set(response.isFoundElements());
					totalNodes.set(totalNodes.get() + response.getElements().size());

					return Observable.fromIterable(response.getElements());
				})
				.filter(element -> request.getElementFilter() == null || request.getElementFilter().isAllowed(element))
				.flatMapSingle(element -> {
					if (!hasReferenceField) {
						return Single.just(element);
					} else {
						return this.enrichObjectsWithReferences(element, referenceEntries, request.getProjectName());
					}
				})
				.flatMap(node -> Observable.just(CsvConverter.transformToCsv(options, node)), false, 1)
				.repeatWhen(t -> t.takeWhile(n -> {
					boolean takeMore = hasFoundElement.get() && hasMore.get();
					int perPage = graphRequest.getVariables().getInteger(VARIABLE_KEY_PER_PAGE);

					if (hasMore.get() && graphRequest.getVariables().containsKey(VARIABLE_KEY_PAGE)) {
						long nextPageNr = graphRequest.getVariables().getLong(VARIABLE_KEY_PAGE) + 1;

						if (request.getMaximalNodes() != null && request.getMaximalNodes() > 0) {
							long count = totalNodes.get();
							if (count >= request.getMaximalNodes()) {
								log.debug("Maximal nodes of {} has been reached!", request.getMaximalNodes());
								takeMore = false;
							} else {
								count += perPage;
								long diff = count - request.getMaximalNodes();
								if (count > request.getMaximalNodes() && diff < perPage) {
									log.debug("Page size has to be decreased to limit results");
									graphRequest.getVariables().put(VARIABLE_KEY_PER_PAGE, perPage - diff);
								}
							}
						}

						if (takeMore) {
							log.debug("Result has next page, repeating with next page {}", nextPageNr);
							graphRequest.getVariables().put(VARIABLE_KEY_PAGE, nextPageNr);
						}
					} else {
						log.debug("Current page is the last, we don't need to fetch more pages.");
					}

					return takeMore;
				}))
				.flatMap(Observable::fromIterable);
		});
	}

	private Single<ResponseData> loadData(PluginContext rc, CsvRequest csvRequest, GraphQLRequest graphRequest) {
		return Single.defer(() -> {
			log.debug("Getting Results from page: " + graphRequest.getVariables().getLong(VARIABLE_KEY_PAGE));
			ParameterProvider params = new GenericParametersImpl();
			if (rc.request().params().contains("wait")) {
				params.setParameter("wait", rc.request().params().get("wait"));
			}
			if (rc.request().params().contains("lang")) {
				params.setParameter("lang", rc.request().params().get("lang"));
			}

			return this.client
				.graphql(csvRequest.getProjectName(), graphRequest, params)
				.toSingle()
				.map(response -> {
					if (response.getErrors() != null && !response.getErrors().isEmpty()) {
						throw new GraphQLRestException(new HashSet<>(response.getErrors()));
					}
					if (response.getData() == null) {
						throw new GenericRestException(HttpResponseStatus.INTERNAL_SERVER_ERROR,
							"GraphQL Request succeeded without errors, but also without data.");
					}
					return response.getData();
				});
		}).map(data -> {
			Optional<Object> rawHasMore = Utils.getValueByPath(data, Utils.getPathAsList(csvRequest.getHasMorePath()));
			Optional<Object> rawElements = Utils.getValueByPath(data, Utils.getPathAsList(csvRequest.getElementsPath()));
			ResponseData response = new ResponseData();
			response.setElements(new ArrayList<>());
			response.setHasMore(false);
			response.setFoundElements(false);

			if (rawHasMore.isPresent()) {
				Object tmp = rawHasMore.get();
				if (tmp instanceof Boolean) {
					response.setHasMore((Boolean) tmp);
				} else {
					try {
						response.setHasMore(Boolean.parseBoolean(tmp.toString()));
					} catch (Exception ignored) {
					}
				}
			}

			if (rawElements.isPresent()) {
				response.setFoundElements(true);

				if (rawElements.get() instanceof JsonArray) {
					response.setElements(((JsonArray) rawElements.get())
						.stream()
						.filter(v -> v instanceof JsonObject)
						.map(v -> (JsonObject) v)
						.collect(Collectors.toList())
					);
				}
			}

			return response;
		});
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

	private void enrichObjectWithResult(
		JsonObject obj,
		CsvMappingDefinition entry,
		boolean isList,
		GraphQLResponse response
	) {
		List<JsonObject> elements = new ArrayList<>();
		Optional<Object> rawElements = Utils.getValueByPath(response.getData(), Utils.getPathAsList(entry.getReferenceElementsPath()));

		if (rawElements.isPresent() && rawElements.get() instanceof JsonArray) {
			elements = ((JsonArray) rawElements.get())
				.stream()
				.filter(tmp -> tmp instanceof JsonObject)
				.map(tmp -> (JsonObject) tmp)
				.collect(Collectors.toList());
		}

		// Put the found content into the element
		if (isList) {
			obj.getJsonObject(REFERENCE_CONTENT_CONTAINER_NAME).put(entry.getPath(), elements);
		} else {
			obj
				.getJsonObject(REFERENCE_CONTENT_CONTAINER_NAME)
				.put(entry.getPath(), elements.isEmpty() ? null : elements.get(0));
		}
	}

	protected SingleSource<? extends JsonObject> enrichObjectsWithReferences(
		JsonObject obj, List<CsvMappingDefinition> referenceEntries, String projectName
	) {
		String uuid = obj.getString("uuid", "<uuid-not-found>");
		obj.put(REFERENCE_CONTENT_CONTAINER_NAME, new JsonObject());

		return Observable.fromIterable(referenceEntries).flatMapCompletable(entry -> {
			log.debug("Fetching references for entry: " + entry.getLabel());
			List<String> values = new ArrayList<>();
			Optional<Object> rawValues = Utils.getValueByPath(obj, entry.getPathAsList());
			boolean isList = entry.getType() == CsvMappingDataType.LIST;

			if (rawValues.isPresent()) {
				if (rawValues.get() instanceof JsonArray) {
					values = ((JsonArray) rawValues.get())
						.stream()
						.map(Object::toString)
						.collect(Collectors.toList());
				} else {
					values.add(rawValues.get().toString());
				}
			}

			if (values.isEmpty()) {
				values.add("");
			}

			return Observable.fromIterable(values).flatMapCompletable(singleValue -> {
				// Build a graphql request based on the entry
				JsonObject variables = new JsonObject();
				variables.put("query", entry.getReferenceSearchQuery()
					.replaceAll(REFERENCE_UUID_PLACEHOLDER, uuid)
					.replaceAll(REFERENCE_VALUE_PLACEHOLDER, singleValue)
				);
				if (isList) {
					variables.put(VARIABLE_KEY_PER_PAGE, entry.getReferenceMaximalNodes());
				} else {
					variables.put(VARIABLE_KEY_PER_PAGE, 1);
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
				})
					.doOnSuccess(response -> enrichObjectWithResult(obj, entry, isList, response))
					.ignoreElement();
			});

		}).andThen(Single.just(obj));
	}
}
