package com.gentics.mesh.plugin.common.util;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.rest.client.MeshRestClient;

import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;

/**
 * Paging aware data fetcher for graphql requests
 */
public class GraphQlPagingFetcher {
	/**
	 * Client to use
	 */
	private MeshRestClient client;

	/**
	 * Project name
	 */
	private String projectName;

	/**
	 * Page Size
	 */
	private int pageSize = 100;

	/**
	 * Set the client
	 * @param client client
	 * @return fluent API
	 */
	public GraphQlPagingFetcher withClient(MeshRestClient client) {
		this.client = client;
		return this;
	}

	/**
	 * Set the project
	 * @param projectName project name
	 * @return fluent API
	 */
	public GraphQlPagingFetcher withProject(String projectName) {
		this.projectName = projectName;
		return this;
	}

	/**
	 * Set the page size
	 * @param pageSize page size
	 * @return fluent API
	 */
	public GraphQlPagingFetcher withPageSize(int pageSize) {
		this.pageSize = pageSize;
		return this;
	}

	/**
	 * Fetch the data of all pages
	 * @param <T> type of the return items
	 * @param query graphql query must have the variables "page" for the page to fetch and "perPage" for the page size
	 * @param variables additional variables to set to the graphql request
	 * @param dataExtractor data extractor
	 * @param hasNextPageExtractor extractor for the "hasNextPage" flag
	 * @return observable emitting the items
	 */
	public <T> Observable<T> fetch(String query, Map<String, Object> variables,
			Function<GraphQLResponse, List<T>> dataExtractor,
			Function<GraphQLResponse, Boolean> hasNextPageExtractor) {
		return fetchPage(query, variables, dataExtractor, hasNextPageExtractor, 1);
	}

	/**
	 * Recursive method to fetch the data in pages
	 * @param <T> type of the returned items
	 * @param query graphql query
	 * @param variables additional variables
	 * @param dataExtractor data extractor
	 * @param hasNextPageExtractor extractor for the "hasNextPage" flag
	 * @param page current page
	 * @return observable emitting the items
	 */
	private <T> Observable<T> fetchPage(String query, Map<String, Object> variables,
			Function<GraphQLResponse, List<T>> dataExtractor, Function<GraphQLResponse, Boolean> hasNextPageExtractor,
			int page) {
		JsonObject variablesObject = new JsonObject();
		variables.forEach((key, value) -> variablesObject.put(key, value));
		variablesObject.put("page", page);
		variablesObject.put("perPage", pageSize);

		GraphQLRequest request = new GraphQLRequest()
				.setQuery(query)
				.setVariables(variablesObject);

		return client.graphql(projectName, request).toSingle().flatMapObservable(response -> {
			List<T> items = dataExtractor.apply(response);
			boolean hasMoreData = hasNextPageExtractor.apply(response);

			Observable<T> currentPage = Observable.fromIterable(items);

			if (hasMoreData) {
				Observable<T> nextPage = fetchPage(query, variables, dataExtractor, hasNextPageExtractor, page + 1);

				return currentPage.concatWith(nextPage);
			} else {
				return currentPage;
			}
		});
	}
}
