package com.gentics.mesh.plugin.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.http.HttpConstants;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.SearchParameters;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.CustomQueryOptions;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.extension.CustomHandler;
import com.gentics.mesh.plugin.search.handler.SearchHandler;
import com.gentics.mesh.plugin.search.handler.custom.CustomQueryHandler;
import com.gentics.mesh.plugin.search.service.BranchService;
import com.gentics.mesh.plugin.search.service.QueryService;
import com.gentics.mesh.plugin.search.transformer.BasicHitTransformer;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.vertx.http.HttpResponseStatus;
import io.reactivex.Completable;
import io.reactivex.Single;
import io.vertx.core.http.HttpHeaders;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static com.gentics.mesh.plugin.search.util.TestUtils.mockContext;
import static com.gentics.mesh.plugin.search.util.TestUtils.mockRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.contains;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link SearchHandler}.
 */
public class SearchHandlerTest {

	/**
	 * Checks if the final search result has exactly the expected number of elements, and all default fields are present.
	 *
	 * <p>
	 * Changes to <em>.../resources/graqhQLResult.json</em> or <em>.../resources/searchResultOneBranch.json</em> must
	 * be reflected in the {@link #matches(Object) matches()} method.
	 * </p>
	 */
	private static class IsExpectedSearchResult extends BaseMatcher<String> {

		private final int expectedResults;
		private final int expectedBranches;

		IsExpectedSearchResult(int expectedResults, int expectedBranches) {
			this.expectedResults = expectedResults;
			this.expectedBranches = expectedBranches;
		}

		@Override
		public boolean matches(Object item) {
			JsonObject result = toJsonObject(item);
			JsonArray hits = result.getJsonArray("results", new JsonArray());

			if (result.getInteger("totalHits", 0) != expectedResults || hits.size() != expectedResults) {
				return false;
			}

			return hits.stream()
				.map(o -> (JsonObject) o)
				.allMatch(hit ->
					!StringUtils.isBlank(hit.getString("uuid"))
					&& !StringUtils.isBlank(hit.getString("branch"))
					&& !StringUtils.isBlank(hit.getString("language"))
					&& !StringUtils.isBlank(hit.getString("url")));
		}

		@Override
		public void describeTo(Description description) {
			String desc = String.format(
				"Search result with %d hit%s (from %d branch%s) with uuid, branch, language and url fields",
				expectedResults,
				expectedResults == 1 ? "" : "s",
				expectedBranches,
				expectedBranches == 1 ? "" : "es");

			description.appendText(desc);
		}

		private JsonObject toJsonObject(Object item) {
			try {
				return new JsonObject(item.toString());
			} catch (Exception e) {
				return new JsonObject();
			}
		}
	}

	private static final String DEFAULT_PROJECT = "dummy";
	private static final String SEARCH_QUERY = new JsonObject().put("query", "search query").encode();
	private static final String CONTENT_DYM_QUERY = new JsonObject().put("query", "dym content").encode();
	private static final String BINARY_DYM_QUERY = new JsonObject().put("query", "dym binary").encode();

	private static MeshRequest<ObjectNode> searchResult;
	private static MeshRequest<GraphQLResponse> resultQueryResponse;
	private static QueryService queryServiceMock;
	private static BranchService branchServiceMock;

	private SearchConfiguration config;

	/**
	 * Setup the query store and query builder.
	 */
	@BeforeClass
	public static void setupOnce() throws IOException {
		Class<?> clazz = SearchHandlerTest.class;

		// Note that the same GraphQL result is used in all tests, even those with results from different branches
		// (which causes some warnings about missing hits).
		JsonObject result = new JsonObject(IOUtils.toString(clazz.getResourceAsStream("/results/graphQLResult.json"), StandardCharsets.UTF_8));

		resultQueryResponse = mockRequest(new GraphQLResponse().setData(result));
		queryServiceMock = mock(QueryService.class);

		when(queryServiceMock.getSearchQuery(any())).thenReturn(Single.just(new JsonObject(SEARCH_QUERY)));
		when(queryServiceMock.getSearchResultQuery(anyString(), anyString())).thenReturn(Single.just(SEARCH_QUERY));
		when(queryServiceMock.getDidYouMeanQuery(any(), eq(false))).thenReturn(Single.just(new JsonObject(CONTENT_DYM_QUERY)));
		when(queryServiceMock.getDidYouMeanQuery(any(), eq(true))).thenReturn(Single.just(new JsonObject(BINARY_DYM_QUERY)));

		branchServiceMock = mock(BranchService.class);

		for (String project : Arrays.asList("other", "dummy")) {
			String name = "branch_" + project;
			BranchResponse branch = new BranchResponse();

			branch.setName(name);
			branch.setUuid(name);
			branch.setHostname("hostname-" + project);

			when(branchServiceMock.getBranch(eq(project), eq(name))).thenReturn(Single.just(branch));
		}
	}

	/**
	 * Make sure each test starts with the default configuration.
	 */
	@Before
	public void setup() throws IOException {
		config = new SearchConfiguration();
		searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/emptyResult.json"), ObjectNode.class));
	}

	/**
	 * Test that the handler fails with a BAD REQUEST when there is no search parameter.
	 */
	@Test
	public void testFailNoQuery() {
		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "missing-q-parameter");

		new SearchHandler(config, pc, queryServiceMock, branchServiceMock, BasicHitTransformer::new, null).handle();

		verify(pc).fail(HttpResponseStatus.BAD_REQUEST.code());
	}

	/**
	 * Test that the handler makes search requests to Mesh for the query itself, as well as the two
	 * did-you-mean queries per searched project
	 */
	@Test
	public void testSearchWithDidYouMeanMultipleProjects() throws IOException {
		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "q=test");
		Map<String, Set<String>> projects = new HashMap<>();

		projects.put(pc.projectName(), new HashSet<>());
		projects.get(pc.projectName()).add("foo");
		projects.get(pc.projectName()).add("bar");

		config.setProjects(projects);
		config.setResultQuery(null);

		searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/searchResultOneBranch.json"), ObjectNode.class));

		makeSearchRequest(pc, null);

		verify(pc.client(), times(1)).searchNodesRaw(eq(SEARCH_QUERY), anyVararg());

		projects.get(pc.projectName()).forEach(p -> {
			verify(pc.client(), times(1)).searchNodesRaw(eq(p), eq(CONTENT_DYM_QUERY), anyVararg());
			verify(pc.client(), times(1)).searchNodesRaw(eq(p), eq(BINARY_DYM_QUERY), anyVararg());
		});

		verify(pc.client(), never()).graphql(anyString(), any(), anyVararg());
		verify(pc.response()).end(anyString());
	}

	/**
	 * Test that the handler makes search requests to Mesh for the query itself, as well as the two
	 * did-you-mean queries.
	 */
	@Test
	public void testSearchWithDidYouMeanSingleProject() {
		config.setResultQuery(null);
		// config.validate(config);

		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "q=test");

		makeSearchRequest(pc, null);

		verify(pc.client(), times(1)).searchNodesRaw(anyString(), eq(SEARCH_QUERY), anyVararg());
		verify(pc.client(), times(1)).searchNodesRaw(anyString(), eq(CONTENT_DYM_QUERY), anyVararg());
		verify(pc.client(), times(1)).searchNodesRaw(anyString(), eq(BINARY_DYM_QUERY), anyVararg());
		verify(pc.client(), never()).graphql(anyString(), any(), anyVararg());
		verify(pc.response()).end(anyString());
	}

	/**
	 * Test that the handler does not make a GraphQL request, when no search result query is configured.
	 */
	@Test
	public void testSearchNoResultQuery() {
		// The did you mean queries would mess up the count of the searchNodesRaw() invocations.
		config.setDidYouMeanThreshold(0);
		config.setResultQuery(null);

		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "q=test");

		makeSearchRequest(pc, null);

		verify(pc.client(), times(1)).searchNodesRaw(anyString(), anyString(), anyVararg());
		verify(pc.client(), never()).graphql(anyString(), any(), anyVararg());
		verify(pc.response()).end(anyString());
	}

	/**
	 * Test that the handler performs the GraphQL query to loadValue additional data about hits.
	 */
	@Test
	public void testSearchResultQuery() throws IOException {
		searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/searchResultOneBranch.json"), ObjectNode.class));

		// The did you mean queries would mess up the count of the searchNodesRaw() invocations.
		config.setDidYouMeanThreshold(0);
		config.setResultQuery("searchResultItem");

		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "q=test");

		makeSearchRequest(pc, null);

		verify(pc.client(), times(1)).searchNodesRaw(anyString(), anyString(), anyVararg());
		verify(pc.client(), times(1)).graphql(anyString(), any(), anyVararg());
		verify(pc.response()).end(anyString());
	}

	/**
	 * Test that the handler works with the new structure of the search response.
	 */
	@Test
	public void testSearchResultNewTotal() throws IOException {
		searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/searchResultNewTotal.json"), ObjectNode.class));

		// The did you mean queries would mess up the count of the searchNodesRaw() invocations.
		config.setDidYouMeanThreshold(0);

		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "q=test");

		makeSearchRequest(pc, null);

		verify(pc.client(), times(1)).searchNodesRaw(anyString(), anyString(), anyVararg());
		verify(pc.response()).end(contains("\"totalHits\":2"));
	}

	/**
	 * Test that the handler works with the new language specific indices.
	 */
	@Test
	public void testSearchResultLanguageSpecificIndices() throws IOException {
		searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/searchResultLanguageSpecificIndex.json"), ObjectNode.class));

		// The did you mean queries would mess up the count of the searchNodesRaw() invocations.
		config.setDidYouMeanThreshold(0);
		config.setResultQuery("searchResultItem");

		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "q=test");

		makeSearchRequest(pc, null);

		verify(pc.client(), times(1)).searchNodesRaw(anyString(), anyString(), anyVararg());
		verify(pc.response()).end(contains("\"totalHits\":1"));
	}

	/**
	 * Test that branch and node UUIDs as well as the node language are added to all search results.
	 */
	@Test
	public void testDefaultSearchResultFields() throws IOException {
		searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/searchResultOneBranch.json"), ObjectNode.class));

		// The did you mean queries would mess up the count of the searchNodesRaw() invocations.
		config.setDidYouMeanThreshold(0);
		config.setResultQuery("searchResultItem");

		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "q=test");

		makeSearchRequest(pc, null);

		verify(pc.response()).end(argThat(new IsExpectedSearchResult(2, 1)));
	}

	/**
	 * Test that hits from different branches cause additional GraphQL requests.
	 */
	@Test
	public void testSearchMultipleBranches() throws IOException {
		searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/searchResultMultipleBranches.json"), ObjectNode.class));

		// The did you mean queries would mess up the count of the searchNodesRaw() invocations.
		config.setDidYouMeanThreshold(0);
		config.setResultQuery("searchResultItem");

		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "q=test");

		makeSearchRequest(pc, null);

		verify(pc.client(), times(1)).searchNodesRaw(anyString(), anyString(), anyVararg());
		verify(pc.client(), times(2)).graphql(anyString(), any(), anyVararg());
		verify(pc.response()).end(argThat(new IsExpectedSearchResult(3, 2)));
	}

	/**
	 * Test that query and result are generated by custom handler.
	 */
	@Test
	public void testCustomHandler() throws IOException {
		searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/searchResultOneBranch.json"), ObjectNode.class));
		config.setDidYouMeanThreshold(0);

		JsonObject expectedQuery = new JsonObject().put("query", "custom handler query");
		String expectedResult = new JsonObject().put("result", "custom handler result").encode();
		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "q=test");
		CustomHandler customHandler = new CustomHandler() {
			@Override
			public HandlerType type() {
				return HandlerType.SEARCH;
			}

			@Override
			public String name() {
				return "testHandler";
			}

			@Override
			public Single<JsonObject> getQuery(PluginContext context) {
				return Single.just(expectedQuery);
			}

			@Override
			public String getResultQueryName() {
				return "customResultQuery";
			}

			@Override
			public Completable processResult(PluginContext pc, Object result) {
				return Completable.fromAction(() -> pc.response().end(expectedResult));
			}
		};

		makeSearchRequest(pc, customHandler);

		verify(pc.client(), times(1)).searchNodesRaw(eq(DEFAULT_PROJECT), eq(expectedQuery.encode()), anyVararg());
		verify(pc.response()).end(expectedResult);
		verify(queryServiceMock, never()).getSearchResultQuery(anyString());
		verify(queryServiceMock).getSearchResultQuery(DEFAULT_PROJECT, customHandler.getResultQueryName());
	}

	/**
	 * Test that configured custom queries are handled correctly.
	 */
	@Test
	public void testCustomQueryHandler() throws IOException {
		String customQuery = "customQuery";

		searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/searchResultOneBranch.json"), ObjectNode.class));
		config.setDidYouMeanThreshold(0);

		JsonObject expectedQuery = new JsonObject().put("query", "custom query");

		when(queryServiceMock.getCustomQuery(any(), anyString())).thenReturn(Single.just(expectedQuery));

		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search/custom/" + customQuery, "q=test");
		CustomHandler customHandler = new CustomQueryHandler(queryServiceMock, new CustomQueryOptions(customQuery)) ;

		makeSearchRequest(pc, customHandler);

		verify(pc.client(), times(1)).searchNodesRaw(eq(DEFAULT_PROJECT), eq(expectedQuery.encode()), anyVararg());
		verify(pc.response()).putHeader(HttpHeaders.CONTENT_TYPE, HttpConstants.APPLICATION_JSON_UTF8);
		verify(pc.response()).end(anyString());
		verify(queryServiceMock, never()).getSearchResultQuery(anyString());
		verify(queryServiceMock, never()).getSearchResultQuery(anyString(), anyString());
		verify(queryServiceMock, times(1)).getCustomQuery(any(), eq(customQuery));
	}

	@Test
	public void testWaitForIdle() {
		doTestWaitForIdle(true);
	}

	@Test
	public void testDontWaitForIdle() {
		doTestWaitForIdle(false);
	}

	@Test
	public void testWaitForIdleDefault() {
		doTestWaitForIdle(null);
	}

	private void doTestWaitForIdle(Boolean wait) {
		// The did you mean queries would mess up the count of the searchNodesRaw() invocations.
		config.setDidYouMeanThreshold(0);
		config.setResultQuery(null);
		config.setWaitForIdle(wait);

		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "q=test");

		makeSearchRequest(pc, null);

		ArgumentCaptor<ParameterProvider> varArgs = ArgumentCaptor.forClass(ParameterProvider.class);

		verify(pc.client(), times(1)).searchNodesRaw(anyString(), anyString(), varArgs.capture());

		Optional<Boolean> waitParam = varArgs.getAllValues().stream()
			.filter(param -> param instanceof SearchParameters && ((SearchParameters) param).isWait().isPresent())
			.map(param -> ((SearchParameters) param).isWait().get())
			.findAny();

		if (wait == null) {
			assertThat(waitParam)
				.as("waitForIdle parameter")
				.isNotPresent();
		} else {
			assertThat(waitParam)
				.as("waitForIdle parameter")
				.isPresent()
				.contains(wait);
		}
	}

	/**
	 * Mock the necessary methods of the client, initialize and call the handler.
	 *
	 * @param customHandler The <code>CustomHandler</code> to pass to the created {@link SearchHandler}
	 */
	private void makeSearchRequest(PluginContext pc, CustomHandler customHandler) {
		SearchHandler handler = new SearchHandler(
			config,
			pc,
			queryServiceMock,
			branchServiceMock,
			BasicHitTransformer::new,
			customHandler);

		when(pc.client().searchNodesRaw(anyString(), anyVararg())).thenReturn(searchResult);
		when(pc.client().searchNodesRaw(anyString(), anyString(), anyVararg())).thenReturn(searchResult);
		when(pc.client().graphql(anyString(), any(), anyVararg())).thenReturn(resultQueryResponse);

		handler.handle().test().assertComplete();
	}
}
