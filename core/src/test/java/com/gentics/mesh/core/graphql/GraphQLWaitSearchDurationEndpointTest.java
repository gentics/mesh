package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.mock.SearchWaitUtilMock;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.search.AbstractMultiESTest;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestContextOverride;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.SearchWaitUtil;
import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.concurrent.TimeUnit;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = FULL, startServer = true)
public class GraphQLWaitSearchDurationEndpointTest extends AbstractMultiESTest {
	private final String QUERY_NAME = "wait-query";
	private static final SearchWaitUtilMock waitUtil = new SearchWaitUtilMock();
	private static final MeshTestContext testContext = new MeshTestContextOverride()
		.setWaitUtil(waitUtil);
	private final long WAIT_TIME_DURATION = 10_000;
	private final long WAIT_TIME_MIN = WAIT_TIME_DURATION;
	private final long WAIT_TIME_MAX = 1_000;
	private final String FIELD_NAME = "slug";
	private final String FIELD_VALUE = "waittest";

	public GraphQLWaitSearchDurationEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Override
	public MeshTestContext getTestContext() {
		return testContext;
	}

	@Before
	public void setUp() {
		createNode(FIELD_NAME, new StringFieldImpl().setString(FIELD_VALUE));
		// Wait 2 sec to let ES index it properly
		Completable.timer(2, TimeUnit.SECONDS).blockingAwait();
	}

	@Test
	public void queryWithWait() {
		waitUtil.setShouldWait(true)
			.setTimeout(WAIT_TIME_DURATION);

		long start = System.currentTimeMillis();
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(QUERY_NAME),
			new SearchParametersImpl().setWait(true)));
		long duration = System.currentTimeMillis() - start;
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json.getValue("errors")).isNull();
		JsonArray elements = json
			.getJsonObject("data", new JsonObject())
			.getJsonObject("nodes", new JsonObject())
			.getJsonArray("elements", new JsonArray());
		assertThat(elements.size()).isEqualTo(1);
		assertThat(elements.getJsonObject(0).getJsonObject("fields").getString(FIELD_NAME)).isEqualTo(FIELD_VALUE);

		// ES must have waited for the sync to be done
		assertThat(duration).isGreaterThanOrEqualTo(WAIT_TIME_MIN);
	}

	@Test
	public void queryWithoutWait() {
		waitUtil.setShouldWait(false)
			.setTimeout(0);

		long start = System.currentTimeMillis();
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(QUERY_NAME),
			new SearchParametersImpl().setWait(false)));
		long duration = System.currentTimeMillis() - start;
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json.getValue("errors")).isNull();
		JsonArray elements = json
			.getJsonObject("data", new JsonObject())
			.getJsonObject("nodes", new JsonObject())
			.getJsonArray("elements", new JsonArray());
		assertThat(elements.size()).isEqualTo(1);
		assertThat(elements.getJsonObject(0).getJsonObject("fields").getString(FIELD_NAME)).isEqualTo(FIELD_VALUE);

		// It may not have waited for ES and therefore should have returned way before.
		assertThat(duration).isLessThanOrEqualTo(WAIT_TIME_MAX);
	}
}
