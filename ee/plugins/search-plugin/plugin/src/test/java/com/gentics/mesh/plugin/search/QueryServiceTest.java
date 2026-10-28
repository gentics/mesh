package com.gentics.mesh.plugin.search;

import com.gentics.mesh.core.rest.schema.SchemaListResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.service.QueryService;
import com.gentics.mesh.plugin.search.service.SchemaService;
import com.gentics.mesh.plugin.search.extension.SearchQueryBuilder;
import com.gentics.mesh.plugin.search.util.TestUtils;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.stubbing.Answer;

import javax.inject.Provider;
import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link QueryService}.
 */
public class QueryServiceTest {
	private static final String DEFAULT_PROJECT = "dummy";

	private static File storagePath;

	private SearchConfiguration config;
	private MeshRestClient adminClientMock;
	private Provider<SearchQueryBuilder> queryBuilderProvider;
	private SchemaService schemaService;

	@BeforeClass
	public static void setupOnce() throws URISyntaxException {
		storagePath = new File(QueryServiceTest.class.getResource("/queries").toURI());
	}

	@Before
	public void setup() {
		config = new SearchConfiguration();
		adminClientMock = mock(MeshRestClient.class);

		SearchConfiguration configMock = mock(SearchConfiguration.class);

		// The config is mocked because we want the test to fail if it is used for anything else
		// than the cache expire time.
		when(configMock.getExpireTime()).thenReturn(60);
		schemaService = new SchemaService(configMock, adminClientMock);

		Answer<?> extractRawQuery = invocation -> new JsonObject(invocation.getArgumentAt(1, String.class));
		SearchQueryBuilder searchQueryBuilderMock = mock(SearchQueryBuilder.class);

		when(searchQueryBuilderMock.buildQuery(any(), anyString(), anyString()))
			.thenAnswer(invocation -> new JsonObject(invocation.getArgumentAt(2, String.class)));
		when(searchQueryBuilderMock.buildSearchQuery(any(), anyString())).thenAnswer(extractRawQuery);
		when(searchQueryBuilderMock.buildAutocompleteQuery(any(), anyString())).thenAnswer(extractRawQuery);
		when(searchQueryBuilderMock.buildDidYouMeanQuery(any(), anyString(), anyBoolean())).thenAnswer(extractRawQuery);

		queryBuilderProvider = () -> searchQueryBuilderMock;
	}

	/**
	 * Create a request for a {@link SchemaListResponse} containing the specified schemas.
	 *
	 * @param schemaNames The desired schema names
	 * @return A request returning a <code>SchemaListResponse</code> with the specified schemas
	 */
	private MeshRequest<SchemaListResponse> createSchemaListResponse(String ...schemaNames) {
		SchemaListResponse response = new SchemaListResponse();

		for (String schemaName : schemaNames) {
			response.getData().add(new SchemaResponse().setName(schemaName));
		}

		return TestUtils.mockRequest(response);
	}

	/**
	 * Test if the current project is searched when no searched projects are configured.
	 */
	@Test
	public void testSearchProjectNoConfig() {
		QueryService service = new QueryService(config, storagePath, schemaService, queryBuilderProvider);
		PluginContext pc = TestUtils.mockContext(DEFAULT_PROJECT, "/search", "q=test");

		config.getProjects().clear();

		service.getSearchQuery(pc)
			.test()
			.assertComplete();

		// The project from the request must be searched in any case.
		verify(queryBuilderProvider.get(), only()).buildSearchQuery(any(), anyString());
	}

	@Test
	public void testAutocompleteQuery() {
		PluginContext pc = TestUtils.mockContext(DEFAULT_PROJECT, "/search", "q=test");

		new QueryService(config, storagePath, schemaService, queryBuilderProvider)
			.getAutocompleteQuery(pc)
			.test()
			.assertComplete();

		verify(queryBuilderProvider.get(), only()).buildAutocompleteQuery(any(), anyString());
	}

	@Test
	public void testDidYouMeanQueries() {
		PluginContext pc = TestUtils.mockContext(DEFAULT_PROJECT, "/search", "q=test");
		QueryService service = new QueryService(config, storagePath, schemaService, queryBuilderProvider);

		service.getDidYouMeanQuery(pc, true)
			.test()
			.assertComplete();

		service.getDidYouMeanQuery(pc, false)
			.test()
			.assertComplete();

		verify(queryBuilderProvider.get(), times(2)).buildDidYouMeanQuery(any(), anyString(), anyBoolean());
	}

	/**
	 * Test loading the result GraphQL query.
	 */
	@Test
	public void testResultQuery() {
		QueryService service = new QueryService(config, storagePath, schemaService, queryBuilderProvider);

		config.getProjects().clear();
		config.setResultQuery("searchResults");


		MeshRequest<SchemaListResponse> request = createSchemaListResponse(
			DEFAULT_PROJECT + "_binary_content",
			DEFAULT_PROJECT + "_content",
			DEFAULT_PROJECT + "_folder"
		);

		when(adminClientMock.findSchemas(anyString())).thenReturn(request);

		service.getSearchResultQuery(DEFAULT_PROJECT).test().assertComplete();
	}

	@Test
	public void testCustomQuery() {
		PluginContext pc = TestUtils.mockContext(DEFAULT_PROJECT, "/search", "q=test");
		QueryService service = new QueryService(config, storagePath, schemaService, queryBuilderProvider);

		service.getCustomQuery(pc, "custom")
			.test()
			.assertComplete();
	}

	/**
	 * Test if a missing schema causes an error.
	 */
	@Test
	public void testMissingSchema() {
		QueryService service = new QueryService(config, storagePath, schemaService, queryBuilderProvider);

		config.setResultQuery("searchResults");

		MeshRequest<SchemaListResponse> request = createSchemaListResponse(
			DEFAULT_PROJECT + "_content",
			DEFAULT_PROJECT + "_folder");

		when(adminClientMock.findSchemas(anyString())).thenReturn(request);

		service.getSearchResultQuery(DEFAULT_PROJECT).test().assertNotComplete();
	}

	/**
	 * Test if ambiguous schema names cause an error.
	 */
	@Test
	public void testAmbiguousSchemas() {
		QueryService service = new QueryService(config, storagePath, schemaService, queryBuilderProvider);

		config.setResultQuery("searchResults");

		MeshRequest<SchemaListResponse> request = createSchemaListResponse(
			"superfluous_binary_content",
			DEFAULT_PROJECT + "_binary_content",
			DEFAULT_PROJECT + "_content",
			DEFAULT_PROJECT + "_folder");

		when(adminClientMock.findSchemas(anyString())).thenReturn(request);

		service.getSearchResultQuery(DEFAULT_PROJECT).test().assertComplete();
	}

	/**
	 * Test if schema names are correctly replaced.
	 */
	@Test
	public void testCorrectBranches() {
		QueryService service = new QueryService(config, storagePath, schemaService, queryBuilderProvider);

		config.setResultQuery("searchResults");

		List<String> projects = Arrays.asList("p1", "p2");
		MeshRequest<SchemaListResponse> request = null;

		for (String project : projects) {
			request = createSchemaListResponse(
				project + "_binary_content",
				project + "_content",
				project + "_folder");

			when(adminClientMock.findSchemas(project)).thenReturn(request);
		}

		// Project p3 uses the same schemas as p2.
		when(adminClientMock.findSchemas("p3")).thenReturn(request);

		String expected = null;

		for (String project : projects) {
			expected = String.format(
				"%s_binary_content,%s_content,%s_folder",
				project,
				project,
				project);

			service.getSearchResultQuery(project)
				// Get rid of possible newlines at the end of the source file.
				.map(String::trim)
				.test()
				.assertComplete()
				.assertValue(expected);
		}

		service.getSearchResultQuery("p3")
			// Get rid of possible newlines at the end of the source file.
			.map(String::trim)
			.test()
			.assertComplete()
			.assertValue(expected);
	}
}
