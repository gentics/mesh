package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.TestSize.FULL;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;
import static org.assertj.core.api.Assertions.assertThat;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.node.field.impl.StringFieldImpl;
import com.gentics.mesh.dagger.MeshComponent;
import com.gentics.mesh.event.MeshEventSender;
import com.gentics.mesh.mock.SearchWaitUtilMock;
import com.gentics.mesh.parameter.impl.SearchParametersImpl;
import com.gentics.mesh.search.AbstractMultiESTest;
import com.gentics.mesh.test.context.ElasticsearchTestMode;
import com.gentics.mesh.test.context.MeshTestContext;
import com.gentics.mesh.test.context.MeshTestContextOverride;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.SearchWaitUtil;
import com.gentics.mesh.util.SearchWaitUtilImpl;

import io.reactivex.Completable;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@RunWith(Parameterized.class)
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = FULL, startServer = true)
public class GraphQLWaitSearchDurationEndpointTest extends AbstractMultiESTest {

	private static final String QUERY_WITH_ES_NAME = "wait-duration-query";
	private static final String QUERY_WITHOUT_ES_NAME = "wait-duration-filter-query";
	private static final String FIELD_NAME = "slug";
	private static final String FIELD_VALUE = "wait-duration-test";

	private static final long WAIT_TIME_DURATION = 10_000;
	private static final long WAIT_TIME_MIN = WAIT_TIME_DURATION;
	private static final long WAIT_TIME_MAX = 1_000;

	private static final SearchWaitUtilMock waitUtil = new SearchWaitUtilMock()
		.setShouldWait(true)
		.setTimeout(WAIT_TIME_DURATION);
	private static final MeshTestContextOverride overrideContext = new MeshTestContextOverride()
		.setWaitUtil(waitUtil);
	private static final MeshEventSender meshSender = overrideContext.getMeshComponent().eventSender();
	private static final SearchWaitUtil originalWaitUtil = new SearchWaitUtilImpl(meshSender, overrideContext.getMeshComponent().options());

	private String createdNode;

	public GraphQLWaitSearchDurationEndpointTest(ElasticsearchTestMode elasticsearch) throws Exception {
		super(elasticsearch);
	}

	@Override
	public MeshTestContext getTestContext() {
		return overrideContext;
	}

	@Before
	public void setUp() {
		createdNode = createNode(FIELD_NAME, new StringFieldImpl().setString(FIELD_VALUE)).getUuid();
	}

	@After
	public void cleanup() {
		if (createdNode != null) {
			deleteNode(PROJECT_NAME, createdNode);
			createdNode = null;
		}
	}

	@Test
	public void queryWithWait() {
		AtomicLong start = new AtomicLong();
		GraphQLResponse response = call(() -> originalWaitUtil.waitForIdle().doOnComplete(() -> {
			start.set(System.currentTimeMillis());
		}).andThen(client().graphql(
			PROJECT_NAME,
			new GraphQLRequest()
				.setQuery(getGraphQLQuery(QUERY_WITH_ES_NAME))
				.setVariables(new JsonObject()
					.put("query", buildQuery())
				),
			new SearchParametersImpl().setWait(true)
		).toSingle()));
		long duration = System.currentTimeMillis() - start.get();
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

		AtomicLong start = new AtomicLong();
		GraphQLResponse response = call(() -> originalWaitUtil.waitForIdle().doOnComplete(() -> {
			start.set(System.currentTimeMillis());
		}).andThen(client().graphql(
			PROJECT_NAME,
			new GraphQLRequest()
				.setQuery(getGraphQLQuery(QUERY_WITH_ES_NAME))
				.setVariables(new JsonObject()
					.put("query", buildQuery())
				),
			new SearchParametersImpl().setWait(false)
		).toSingle()));
		long duration = System.currentTimeMillis() - start.get();
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json.getValue("errors")).isNull();
		JsonArray elements = json
			.getJsonObject("data", new JsonObject())
			.getJsonObject("nodes", new JsonObject())
			.getJsonArray("elements", new JsonArray());
		assertThat(elements.size()).isEqualTo(1);
		assertThat(elements.getJsonObject(0).getJsonObject("fields").getString(FIELD_NAME)).isEqualTo(FIELD_VALUE);

		// It may not have waited for ES and therefore should have returned way before.
		assertThat(duration).isBetween(1L, WAIT_TIME_MAX);
	}

	@Test
	public void filterWithWait() {
		AtomicLong start = new AtomicLong();
		GraphQLResponse response = call(() -> originalWaitUtil.waitForIdle().doOnComplete(() -> {
			start.set(System.currentTimeMillis());
		}).andThen(client().graphql(
			PROJECT_NAME,
			new GraphQLRequest()
				.setQuery(getGraphQLQuery(QUERY_WITHOUT_ES_NAME))
				.setVariables(new JsonObject()
					.put("uuid", createdNode)
				),
			new SearchParametersImpl().setWait(true)
		).toSingle()));
		long duration = System.currentTimeMillis() - start.get();
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json.getValue("errors")).isNull();
		JsonArray elements = json
			.getJsonObject("data", new JsonObject())
			.getJsonObject("nodes", new JsonObject())
			.getJsonArray("elements", new JsonArray());
		assertThat(elements.size()).isEqualTo(1);
		assertThat(elements.getJsonObject(0).getJsonObject("fields").getString(FIELD_NAME)).isEqualTo(FIELD_VALUE);

		// It may not have waited for ES and therefore should have returned way before.
		assertThat(duration).isBetween(1L, WAIT_TIME_MAX);
	}

	@Test
	public void filterWithoutWait() {
		AtomicLong start = new AtomicLong();
		GraphQLResponse response = call(() -> originalWaitUtil.waitForIdle().doOnComplete(() -> {
			start.set(System.currentTimeMillis());
		}).andThen(client().graphql(
			PROJECT_NAME,
			new GraphQLRequest()
				.setQuery(getGraphQLQuery(QUERY_WITHOUT_ES_NAME))
				.setVariables(new JsonObject()
					.put("uuid", createdNode)
				),
			new SearchParametersImpl().setWait(false)
		).toSingle()));
		long duration = System.currentTimeMillis() - start.get();
		JsonObject json = new JsonObject(response.toJson());
		assertThat(json.getValue("errors")).isNull();
		JsonArray elements = json
			.getJsonObject("data", new JsonObject())
			.getJsonObject("nodes", new JsonObject())
			.getJsonArray("elements", new JsonArray());
		assertThat(elements.size()).isEqualTo(1);
		assertThat(elements.getJsonObject(0).getJsonObject("fields").getString(FIELD_NAME)).isEqualTo(FIELD_VALUE);

		// It may not have waited for ES and therefore should have returned way before.
		assertThat(duration).isBetween(1L, WAIT_TIME_MAX);
	}

	private String buildQuery() {
		return "{\"query\":{\"term\":{\"uuid\":\"" + this.createdNode + "\"}}}";
	}
}
