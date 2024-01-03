package com.gentics.mesh.core.graphql;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.rest.client.MeshRequest;
import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.reactivex.Observable;
import io.vertx.core.json.JsonObject;

@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true, inMemoryDB = true)
public class GraphQLEndpointStressTest extends AbstractGraphQLEndpointTest {

	protected final int simultaneousCalls;

	protected MeshRestClient client;

	@Parameters(name = "calls={0}")
	public static Collection<Integer> paramData() {
		int total = (int) streamQueries().count();
		return List.of(1, 2, 3, 5, 8, 13, 21, total, 100, 250);
	}

	public GraphQLEndpointStressTest(Integer simultaneousCalls) {
		this.simultaneousCalls = simultaneousCalls;
	}

	@Before
	public void setUp() throws Exception {
		this.client = client("v2");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSimultaneousCalls() throws Exception {
		prepareNodes(client, true, true);
	
		 List<List<Object>> queries = streamQueries().collect(Collectors.toList());

		Observable.range(0, simultaneousCalls).flatMapSingle(number -> {
			List<Object> query = queries.get(number % queries.size());
			String queryName = (String) query.get(0);
			String version = (String) query.get(3);
			//Consumer<JsonObject> assertion = query.size() > 4 ? (Consumer<JsonObject>) query.get(4) : null;

			try {
				return makeTheQuery(client, queryName, "v2", version).toSingle();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}).lastOrError().ignoreElement().blockingAwait();
	}
}
