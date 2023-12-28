package com.gentics.mesh.core.graphql;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.project.ProjectCreateRequest;
import com.gentics.mesh.core.rest.project.ProjectResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, inMemoryDB = true)
public class GraphQLEndpointStressTest extends AbstractGraphQLEndpointTest {

	protected final int simultaneousCalls;

	protected MeshRestClient clientV2;
	protected MeshRestClient clientV1;

	@Parameters(name = "calls={0}")
	public static Collection<Integer> paramData() {
		int total = (int) streamQueries().count();
		return List.of(1, 2, 3, 5, 8, 13, 21, total, 100, 250, 500, 1000);
	}

	public GraphQLEndpointStressTest(Integer simultaneousCalls) {
		this.simultaneousCalls = simultaneousCalls;
	}

	protected static Stream<List<Object>> streamQueries() {
		// Filter out for `prepareNodes()` single call
		return AbstractGraphQLEndpointTest.streamQueries().filter(query -> Boolean.TRUE.equals(query.get(1)) && Boolean.FALSE.equals(query.get(2)));
	}

	@Before
	public void setUp() throws Exception {
		this.clientV2 = client("v2");
		this.clientV1 = client("v1");
	}

	public void testProjectsCreation() throws InterruptedException {
		CountDownLatch latch = new CountDownLatch(simultaneousCalls);
		List<Thread> threads = IntStream.range(0, simultaneousCalls).mapToObj(number -> {
			MeshRequest<ProjectResponse> request = clientV2.createProject(new ProjectCreateRequest().setName("ProjectStress_" + number).setSchemaRef("folder"));
			return new Thread() {
				public void run() {
					ProjectResponse response = request.blockingGet();
					assertThat(response.getName()).isEqualTo("ProjectStress_" + number);
					latch.countDown();
				}
			};
		}).collect(Collectors.toList());
		threads.stream().forEach(Thread::run);
		latch.await(5, TimeUnit.MINUTES);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSimultaneousCalls() throws Exception {
		prepareNodes(clientV2, true, false);
	
		List<List<Object>> queries = streamQueries().collect(Collectors.toList());

		CountDownLatch latch = new CountDownLatch(simultaneousCalls);
		List<Thread> threads = IntStream.range(0, simultaneousCalls).mapToObj(number -> {
			List<Object> query = queries.get(number % queries.size());
			String queryName = (String) query.get(0);
			String version = (String) query.get(3);
			Consumer<JsonObject> assertion = query.size() > 4 ? (Consumer<JsonObject>) query.get(4) : null;
			MeshRequest<GraphQLResponse> request;
			try {
				request = makeTheQuery(clientV2, queryName, "v2", version);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
			return new Thread() {
				public void run() {
					GraphQLResponse response = request.blockingGet();
					try {
						testTheQuery(response, queryName, version, assertion);
					} catch (IOException e) {
						throw new RuntimeException(e);
					}
					latch.countDown();
				}
			};
		}).collect(Collectors.toList());
		threads.stream().forEach(Thread::run);
		latch.await(5, TimeUnit.MINUTES);
	}
}
