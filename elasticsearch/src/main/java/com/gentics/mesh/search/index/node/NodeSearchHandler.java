package com.gentics.mesh.search.index.node;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.mapError;
import static com.gentics.mesh.search.impl.ElasticsearchErrorHelper.mapToMeshError;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.gentics.elasticsearch.client.ElasticsearchClient;
import com.gentics.elasticsearch.client.HttpErrorException;
import com.gentics.elasticsearch.client.okhttp.RequestBuilder;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.action.NodeDAOActions;
import com.gentics.mesh.core.data.HibLanguage;
import com.gentics.mesh.core.data.HibNodeFieldContainer;
import com.gentics.mesh.core.data.node.HibNode;
import com.gentics.mesh.core.data.node.NodeContent;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.page.impl.PageImpl;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.rest.common.ContainerType;
import com.gentics.mesh.core.rest.common.PagingMetaInfo;
import com.gentics.mesh.core.rest.node.NodeResponse;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.SearchProvider;
import com.gentics.mesh.search.index.AbstractSearchHandler;
import com.gentics.mesh.util.SearchWaitUtil;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Collection of handlers which are used to deal with search requests.
 */
@Singleton
public class NodeSearchHandler extends AbstractSearchHandler<HibNode, NodeResponse> {

	private static final Logger log = LoggerFactory.getLogger(NodeSearchHandler.class);

	@Inject
	public NodeSearchHandler(SearchProvider searchProvider, Database db, NodeIndexHandlerImpl nodeIndexHandler,
		MeshOptions options, NodeDAOActions actions, SearchWaitUtil waitUtil) {
		super(db, searchProvider, options, nodeIndexHandler, actions, waitUtil);
	}

	/**
	 * Invoke the given query and return a page of node containers.
	 * 
	 * @param ac
	 * @param query
	 *            Elasticsearch query
	 * @param pagingInfo
	 * @param type
	 * @return
	 * @throws MeshConfigurationException
	 * @throws TimeoutException
	 * @throws ExecutionException
	 * @throws InterruptedException
	 */
	@SuppressWarnings("unchecked")
	public Page<? extends NodeContent> handleContainerSearch(InternalActionContext ac, String query, PagingParameters pagingInfo, ContainerType type,
		InternalPermission... permissions) throws MeshConfigurationException, InterruptedException, ExecutionException, TimeoutException {
		ElasticsearchClient<JsonObject> client = searchProvider.getClient();
		if (client == null) {
			throw error(HttpResponseStatus.SERVICE_UNAVAILABLE, "search_error_elasticsearch_not_available");
		}
		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + query + "} for {Containers}");
		}
		Set<String> indices = getIndexHandler().getIndicesForSearch(ac, type);

		// Add permission checks to the query
		JsonObject queryJson = prepareSearchQuery(ac, query, true);

		// Apply paging
		applyPagingParams(queryJson, pagingInfo);

		// Only load the documentId we don't care about the indexed contents. The graph is our source of truth here.
		queryJson.put("_source", false);

		if (log.isDebugEnabled()) {
			log.debug("Using parsed query {" + queryJson.encodePrettily() + "}");
		}

		JsonObject queryOption = new JsonObject();
		queryOption.put("index", StringUtils.join(indices.stream().map(i -> searchProvider.installationPrefix() + i).toArray(String[]::new), ","));
		queryOption.put("search_type", "dfs_query_then_fetch");
		log.debug("Using options {" + queryOption.encodePrettily() + "}");

		try {
			RequestBuilder<JsonObject> searchRequest = client.multiSearch(queryOption, queryJson);
			JsonObject response = searchRequest.sync();
			JsonArray responses = response.getJsonArray("responses");
			JsonObject firstResponse = responses.getJsonObject(0);

			// Process the nested error
			JsonObject errorInfo = firstResponse.getJsonObject("error");
			if (errorInfo != null) {
				throw mapError(errorInfo);
			}

			JsonObject hitsInfo = firstResponse.getJsonObject("hits");

			// The scrolling iterator will wrap the current response and query ES for more data if needed.
			Page<? extends NodeContent> page = db.tx(tx -> {
				AtomicLong totalCount = new AtomicLong(extractTotalCount(hitsInfo));
				List<NodeContent> elementList = new ArrayList<>();
				JsonArray hits = hitsInfo.getJsonArray("hits");

				// uuidLangList is the list of uuid and language (as pairs) of all hits in correct order
				List<Pair<String, String>> uuidLangList = hits.stream().map(hit -> {
					JsonObject val;
					if (hit instanceof Map) {
						val = new JsonObject((Map<String,Object>) hit);
					} else {
						val = (JsonObject) hit;
					}
					String id = ((JsonObject) val).getString("_id");
					int pos = id.indexOf("-");

					//UUID / Language pair
					return Pair.of(pos > 0 ? id.substring(0, pos) : id, pos > 0 ? id.substring(pos + 1) : null);
				}).collect(Collectors.toList());

				// the map contentMap will collect all contents for the hits (identified by uuid and language as pair)
				Map<Pair<String, String>, NodeContent> contentMap = new HashMap<>();

				// langUuids is a map of language -> uuids and contains for each language the set of uuids for the hits
				Map<String, Set<String>> langUuids = uuidLangList.stream().collect(Collectors.groupingBy(Pair::getValue, Collectors.mapping(Pair::getKey, Collectors.toSet())));

				// batch-load the nodes and contents for each language and collect results in contentMap
				langUuids.entrySet().stream().forEach(entry -> {
					HibLanguage language = tx.languageDao().findByLanguageTag(tx.getProject(ac), entry.getKey());
					if (language == null) {
						log.warn("Could not find language {" + entry.getKey() + "}");
						return;
					}
					Map<String, HibNode> nodes = tx.nodeDao().findByUuids(tx.getProject(ac), entry.getValue())
							.filter(pair -> pair.getValue() != null)
							.collect(Collectors.toMap(Pair::getKey, Pair::getValue));
					Map<HibNode, HibNodeFieldContainer> containers = tx.contentDao().getFieldsContainers(nodes.values(), entry.getKey(), tx.getBranch(ac), type);
					uuidLangList.stream().forEach(uuidLang -> {
						String uuid = uuidLang.getKey();
						Optional.ofNullable(nodes.get(uuid)).flatMap(node -> Optional.ofNullable(containers.get(node))).ifPresent(container -> {
							contentMap.put(uuidLang, new NodeContent(nodes.get(uuid), container, Arrays.asList(entry.getKey()), type));
						});
					});
				});

				// for every hit, get the found content and put into the elementList. This will preserve the order of the hits
				uuidLangList.stream().forEach(uuidLang -> {
					Optional.ofNullable(contentMap.get(uuidLang)).ifPresentOrElse(content -> {
						elementList.add(content);
					}, () -> {
						totalCount.decrementAndGet();
					});
				});

				// Update the total count
				switch (complianceMode) {
				case ES_6:
					hitsInfo.put("total", totalCount.longValue());
					break;
				case ES_7:
					hitsInfo.put("total", new JsonObject().put("value", totalCount.longValue()));
					break;
				default:
					throw new RuntimeException("Unknown compliance mode {" + complianceMode + "}");

				}

				PagingMetaInfo info = extractMetaInfo(hitsInfo, pagingInfo);
				return new PageImpl<>(elementList, info.getTotalCount(), pagingInfo.getPage(), info.getPageCount(), pagingInfo.getPerPage());
			});
			return page;
		} catch (HttpErrorException e) {
			log.error("Error while processing query", e);
			throw mapToMeshError(e);
		}

	}

	@Override
	public NodeIndexHandlerImpl getIndexHandler() {
		return (NodeIndexHandlerImpl) super.getIndexHandler();
	}

}
