package com.gentics.mesh.plugin.search;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.parameter.SearchParameters;
import com.gentics.mesh.plugin.PluginContext;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.plugin.search.handler.AutocompleteHandler;
import com.gentics.mesh.plugin.search.service.QueryService;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.vertx.http.HttpResponseStatus;
import io.reactivex.Single;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.io.IOException;
import java.util.Optional;

import static com.gentics.mesh.plugin.search.util.TestUtils.mockContext;
import static com.gentics.mesh.plugin.search.util.TestUtils.mockRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.anyVararg;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AutocompleteHandlerTest {
	private static final String DEFAULT_PROJECT = "dummy";
	private static final String AUTOCOMPLETE_QUERY = new JsonObject().put("query", "autocomplete query").encode();

	private static QueryService queryServiceMock;

	private SearchConfiguration config;

	/**
	 * Setup the query store and query builder.
	 */
	@BeforeClass
	public static void setupOnce() throws IOException {
		queryServiceMock = mock(QueryService.class);
		when(queryServiceMock.getAutocompleteQuery(any())).thenReturn(Single.just(new JsonObject(AUTOCOMPLETE_QUERY)));
	}

	/**
	 * Make sure each test starts with the default configuration.
	 */
	@Before
	public void setup() {
		config = new SearchConfiguration();
	}

	/**
	 * Test that the handler fails with a BAD REQUEST when there is no search parameter.
	 */
	@Test
	public void testFailNoQuery() {
		PluginContext pc = mockContext(DEFAULT_PROJECT, "/search", "missing-q-parameter");

		new AutocompleteHandler(config, pc, queryServiceMock, null).handle();

		verify(pc).fail(HttpResponseStatus.BAD_REQUEST.code());
	}


	@Test
	public void testSingleTerm() throws IOException {
		PluginContext pc = mockContext(DEFAULT_PROJECT, "/autocomplete", "q=test");
		AutocompleteHandler handler = new AutocompleteHandler(config, pc, queryServiceMock, null);

		MeshRequest<ObjectNode> searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/autocompleteSingleTermResult.json"), ObjectNode.class));

		when(pc.client().searchNodesRaw(anyString(), any(), anyVararg())).thenReturn(searchResult);

		handler.handle().test()
			.assertComplete();

		verify(pc.response()).end(new JsonArray().add(new JsonObject().put("name", "testing")).encode());
	}

	/**
	 * Test that the handler works with the Elasticsearch 7 result structure.
	 *
	 * <p>
	 *     The autocomplete handler does not use the "total" field, but just to be sure test that the structure
	 *     change does not affect the handler.
	 * </p>
	 */
	@Test
	public void testNewTotal() throws IOException {
		PluginContext pc = mockContext(DEFAULT_PROJECT, "/autocomplete", "q=test");
		AutocompleteHandler handler = new AutocompleteHandler(config, pc, queryServiceMock, null);

		MeshRequest<ObjectNode> searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/autocompleteNewTotal.json"), ObjectNode.class));

		when(pc.client().searchNodesRaw(anyString(), any(), anyVararg())).thenReturn(searchResult);

		handler.handle().test()
			.assertComplete();

		verify(pc.response()).end(new JsonArray().add(new JsonObject().put("name", "testing")).encode());
	}

	@Test
	public void testMultipleTerms() throws IOException {
		PluginContext pc = mockContext(DEFAULT_PROJECT, "/autocomplete", "q=foo+bar");
		AutocompleteHandler handler = new AutocompleteHandler(config, pc, queryServiceMock, null);

		MeshRequest<ObjectNode> searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/autocompleteMultipleTermsResult.json"), ObjectNode.class));

		when(pc.client().searchNodesRaw(anyString(), any(), anyVararg())).thenReturn(searchResult);

		handler.handle().test()
			.assertComplete();

		verify(pc.response()).end(new JsonArray().add(new JsonObject().put("name", "foo bar")).encode());
	}
	@Test
	public void testWaitForIdle() throws IOException {
		doTestWaitForIdle(true);
	}

	@Test
	public void testDontWaitForIdle() throws IOException {
		doTestWaitForIdle(false);
	}

	@Test
	public void testWaitForIdleDefault() throws IOException {
		doTestWaitForIdle(null);
	}

	private void doTestWaitForIdle(Boolean wait) throws IOException {
		// The did you mean queries would mess up the count of the searchNodesRaw() invocations.
		config.setDidYouMeanThreshold(0);
		config.setResultQuery(null);
		config.setWaitForIdle(wait);

		PluginContext pc = mockContext(DEFAULT_PROJECT, "/autocomplete", "q=test");
		AutocompleteHandler handler = new AutocompleteHandler(config, pc, queryServiceMock, null);
		MeshRequest<ObjectNode> searchResult = mockRequest(new ObjectMapper().readValue(getClass().getResource("/results/autocompleteMultipleTermsResult.json"), ObjectNode.class));

		when(pc.client().searchNodesRaw(anyString(), any(), anyVararg())).thenReturn(searchResult);

		handler.handle().test()
			.assertComplete();

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

}
