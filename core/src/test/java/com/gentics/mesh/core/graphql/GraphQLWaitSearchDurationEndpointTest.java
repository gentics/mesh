package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collection;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestContextOverride;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.SearchWaitUtil;

import io.reactivex.Completable;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class GraphQLWaitSearchDurationEndpointTest extends AbstractMeshTest {

	private static final String QUERY_WITH_ES_NAME = "wait-duration-query";
	private static final String QUERY_WITHOUT_ES_NAME = "wait-duration-filter-query";
	private static final String SEARCH_UUID = "00112233445566778899aabbccddeeff";

	private static final SearchWaitUtil waitUtil = mock(SearchWaitUtil.class);

	static {
		when(waitUtil.awaitSync(any())).thenAnswer(params -> Completable.complete());
	}

	@Rule
	@ClassRule
	public static final MeshTestContextOverride testContext = new MeshTestContextOverride()
		.setWaitUtil(waitUtil);

	@Parameterized.Parameter(0)
	public boolean searchWithQuery;

	@Parameterized.Parameter(1)
	public boolean waitForSearch;

	@Override
	public MeshTestContext getTestContext() {
		return testContext;
	}

	@Parameterized.Parameters(name = "{index}: searchWithQuery={0}, waitForEach={1}")
	public static Collection<Object[]> paramData() {
		return Arrays.asList(new Object[][] {
			{ true,  true  },
			{ true,  false },
			{ false, true  },
			{ false, false },
		});
	}

	@Before
	public void setup() {
		when(waitUtil.delayRequested(any())).thenReturn(this.waitForSearch);
		clearInvocations(waitUtil);
	}

	@Test
	public void testGraphQLEndpointWaiting() {
		try {
			GraphQLResponse res = call(() -> client().graphql(
				PROJECT_NAME,
				new GraphQLRequest()
					.setQuery(getGraphQLQuery(this.searchWithQuery ? QUERY_WITH_ES_NAME : QUERY_WITHOUT_ES_NAME))
					.setVariables(this.buildVariables()),
				new SearchParametersImpl().setWait(this.waitForSearch)
			));
			assertTrue(res.getErrors().isEmpty());
		} catch (Exception e) {
			assertNull(e);
		}

		if (this.waitForSearch) {
			verify(waitUtil).awaitSync(any());
		} else {
			verify(waitUtil, never()).awaitSync(any());
		}
	}

	private JsonObject buildVariables() {
		if (this.searchWithQuery) {
			return new JsonObject().put("query", "{\"query\":{\"term\":{\"uuid\":\"" + SEARCH_UUID + "\"}}}");
		} else {
			return new JsonObject().put("uuid", SEARCH_UUID);
		}
	}
}
