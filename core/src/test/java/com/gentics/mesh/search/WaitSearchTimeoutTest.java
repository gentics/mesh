package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static com.gentics.mesh.test.context.MeshTestHelper.getSimpleQuery;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.graphql.GraphQLError;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestContextOverride;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.SearchWaitUtil;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.reactivex.Completable;

/**
 * Test cases for handling the timeout when waiting for search idle in a search request
 */
@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.PROJECT_AND_NODE, startServer = true)
public class WaitSearchTimeoutTest extends AbstractMeshTest {
	/**
	 * Mocked wait util. We use spy() here, because we need the default implementation of {@link SearchWaitUtil#awaitSync(com.gentics.mesh.parameter.ParameterProviderContext)}
	 */
	private static final SearchWaitUtil waitUtil = spy(SearchWaitUtil.class);

	/**
	 * Short time in seconds, the query needs to wait until search becomes idle (shorter than the timeout)
	 */
	public final static int SHORT_WAITING_TIME = 1;

	/**
	 * Long time in seconds, the query needs to wait until search becomes idle (longer than the timeout)
	 */
	public final static int LONG_WAITING_TIME = 5;

	/**
	 * Waiting timeout in seconds
	 */
	public final static int WAIT_FOR_IDLE_TIMEOUT = 2;

	/**
	 * Test context with mocked wait util
	 */
	@Rule
	@ClassRule
	public static final MeshTestContext testContext = new MeshTestContextOverride()
		.setWaitUtil(waitUtil).setOptionChanger(options -> {
			// set the waitForIdle Timeout
			options.getSearchOptions().setWaitForIdleTimeout(WAIT_FOR_IDLE_TIMEOUT * 1_000L);
		});

	/**
	 * Test parameters
	 * @return test parameters
	 */
	@Parameters(name = "{index}: waitForIdle={0}, waitingTime={1}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean waitForIdle : Arrays.asList(true, false)) {
			for (int waitingTime : Arrays.asList(SHORT_WAITING_TIME, LONG_WAITING_TIME)) {
				data.add(new Object[] { waitForIdle, waitingTime });
			}
		}
		return data;
	}

	@Parameter(0)
	public boolean waitForIdle;

	@Parameter(1)
	public int waitingTime;

	@Override
	public MeshTestContext getTestContext() {
		return testContext;
	}

	/**
	 * Setup wait util mock
	 */
	@Before
	public void setup() {
		// let wait util return the wait timeout (in ms)
		when(waitUtil.waitTimeoutMs()).thenReturn(WAIT_FOR_IDLE_TIMEOUT * 1_000L);
		// let wait util tell, whether the caller needs to wait for the search to become idle
		when(waitUtil.delayRequested(any())).thenReturn(waitForIdle);
		// wait for the given time
		when(waitUtil.waitForIdle()).thenAnswer(params -> Completable.timer(waitingTime, TimeUnit.SECONDS));
	}

	/**
	 * Test normal search
	 */
	@Test
	public void testSearch() {
		if (waitForIdle && waitingTime > WAIT_FOR_IDLE_TIMEOUT) {
			call(() -> client().searchNodes(getSimpleQuery("fields.slug", "waittest"),
					new SearchParametersImpl().setWait(waitForIdle)), HttpResponseStatus.INTERNAL_SERVER_ERROR, "search_error_timeout");
		} else {
			call(() -> client().searchNodes(getSimpleQuery("fields.slug", "waittest"),
					new SearchParametersImpl().setWait(waitForIdle)));
		}
	}

	/**
	 * Test raw search
	 */
	@Test
	public void testRawSearch() {
		if (waitForIdle && waitingTime > WAIT_FOR_IDLE_TIMEOUT) {
			call(() -> client().searchNodesRaw(getSimpleQuery("fields.slug", "waittest"),
					new SearchParametersImpl().setWait(waitForIdle)), HttpResponseStatus.INTERNAL_SERVER_ERROR, "search_error_timeout");
		} else {
			call(() -> client().searchNodesRaw(getSimpleQuery("fields.slug", "waittest"),
					new SearchParametersImpl().setWait(waitForIdle)));
		}
	}

	/**
	 * Test graphql containing a query
	 */
	@Test
	public void testGraphQLSearch() {
		GraphQLResponse res = call(() -> client().graphql(
			PROJECT_NAME,
			new GraphQLRequest()
				.setQuery(getGraphQLQuery("wait-query")),
			new SearchParametersImpl().setWait(waitForIdle)
		));

		if (waitForIdle && waitingTime > WAIT_FOR_IDLE_TIMEOUT) {
			// expect error and no result
			assertThat(res.getErrors()).usingElementComparatorOnFields("message", "path", "type").isNotNull()
					.containsOnly(new GraphQLError()
							.setMessage("Exception while fetching data (/nodes) : java.util.concurrent.TimeoutException: Timeout after 2000 ms while waiting for elasticsearch to become idle.")
							.setPath("nodes")
							.setType("DataFetchingException"));
			assertThat(res.getData()).as("data").isNotNull();
			assertThat(res.getData().getValue("nodes")).as("data.nodes").isNull();
		} else {
			// expect no error and empty result
			assertThat(res.getErrors()).isNullOrEmpty();
			assertThat(res.getData()).as("data").isNotNull();
			assertThat(res.getData().getJsonObject("nodes")).as("data.nodes").isNotNull();
			assertThat(res.getData().getJsonObject("nodes").getJsonArray("elements")).as("data.nodes.elements").isNotNull().isEmpty();
		}
	}
}
