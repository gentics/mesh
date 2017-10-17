package com.gentics.mesh.search.index;

import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.elasticsearch.action.ActionListener;
import org.elasticsearch.action.search.SearchRequestBuilder;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.action.search.SearchType;
import org.elasticsearch.client.Client;
import org.elasticsearch.search.SearchHit;

import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.MeshCoreVertex;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.page.Page;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.search.IndexHandler;
import com.gentics.mesh.core.rest.error.GenericRestException;
import com.gentics.mesh.error.MeshConfigurationException;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.parameter.PagingParameters;
import com.gentics.mesh.search.SearchHandler;
import com.gentics.mesh.search.SearchProvider;
import com.syncleus.ferma.tx.Tx;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Abstract implementation for a mesh search handler.
 *
 * @param <T>
 */
public abstract class AbstractSearchHandler<T extends MeshCoreVertex<?, T>> implements SearchHandler<T> {

	private static final Logger log = LoggerFactory.getLogger(AbstractSearchHandler.class);

	protected Database db;

	protected SearchProvider searchProvider;

	protected IndexHandler<T> indexHandler;

	/**
	 * Create a new search handler.
	 * 
	 * @param db
	 * @param searchProvider
	 * @param indexHandler
	 */
	public AbstractSearchHandler(Database db, SearchProvider searchProvider, IndexHandler<T> indexHandler) {
		this.db = db;
		this.searchProvider = searchProvider;
		this.indexHandler = indexHandler;
	}

	/**
	 * Prepare the initial search query and inject the permission script dependency.
	 * 
	 * @param ac
	 * @param searchQuery
	 * @return
	 */
	protected JsonObject prepareSearchQuery(InternalActionContext ac, String searchQuery) {
		JsonObject json = new JsonObject(searchQuery);
		/**
		 * Note that from + size can not be more than the index.max_result_window index setting which defaults to 10,000. See the Scroll API for more efficient
		 * ways to do deep scrolling.
		 */
		json.put("from", 0);
		json.put("size", Integer.MAX_VALUE);

		// Permission script
		JsonObject scriptParams = new JsonObject();

		JsonArray roleUuids = new JsonArray();
		try (Tx tx = db.tx()) {
			for (Role role : ac.getUser().getRoles()) {
				roleUuids.add(role.getUuid());
			}
		}
		scriptParams.put("userRoleUuids", roleUuids);

		JsonObject scriptInfo = new JsonObject();
		scriptInfo.put("script", "hasPermission");
		scriptInfo.put("lang", "native");
		scriptInfo.put("params", scriptParams);

		JsonObject scriptFields = new JsonObject();
		scriptFields.put("meshscript.hasPermission", scriptInfo);
		json.put("script_fields", scriptFields);
		return json;
	}

	@Override
	public Page<? extends T> query(InternalActionContext ac, String query, PagingParameters pagingInfo, GraphPermission... permissions)
			throws MeshConfigurationException, InterruptedException, ExecutionException, TimeoutException {
		org.elasticsearch.node.Node esNode = null;
		if (searchProvider.getNode() instanceof org.elasticsearch.node.Node) {
			esNode = (org.elasticsearch.node.Node) searchProvider.getNode();
		} else {
			throw new MeshConfigurationException("Unable to get elasticsearch instance from search provider got {" + searchProvider.getNode() + "}");
		}

		Client client = esNode.client();
		if (log.isDebugEnabled()) {
			log.debug("Invoking search with query {" + query + "} for {" + indexHandler.getElementClass().getName() + "}");
		}

		SearchRequestBuilder builder = null;
		try {
			JsonObject queryJson = prepareSearchQuery(ac, query);
			Set<String> indices = indexHandler.getSelectedIndices(ac);
			builder = client.prepareSearch(indices.toArray(new String[indices.size()])).setSource(queryJson.toString());
		} catch (Exception e) {
			throw new GenericRestException(BAD_REQUEST, "search_query_not_parsable", e);
		}
		CompletableFuture<Page<? extends T>> future = new CompletableFuture<>();
		builder.setSearchType(SearchType.DFS_QUERY_THEN_FETCH);
		builder.execute().addListener(new ActionListener<SearchResponse>() {

			@Override
			public void onResponse(SearchResponse response) {
				Page<? extends T> page = db.tx(() -> {
					List<T> elementList = new ArrayList<T>();
					for (SearchHit hit : response.getHits()) {

						String id = hit.getId();
						int pos = id.indexOf("-");
						String uuid = pos > 0 ? id.substring(0, pos) : id;

						// Locate the node
						T element = indexHandler.getRootVertex().findByUuid(uuid);
						if (element != null) {
							elementList.add(element);
						}
					}
					Page<? extends T> elementPage = Page.applyPaging(elementList, pagingInfo);
					return elementPage;
				});
				future.complete(page);
			}

			@Override
			public void onFailure(Throwable e) {
				log.error("Search query failed", e);
				future.completeExceptionally(e);
			}
		});
		return future.get(60, TimeUnit.SECONDS);
	}

	public IndexHandler<T> getIndexHandler() {
		return indexHandler;
	}

}
