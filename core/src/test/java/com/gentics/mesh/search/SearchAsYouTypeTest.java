package com.gentics.mesh.search;

import static com.gentics.mesh.test.ClientHelper.call;

import org.junit.Test;

import com.gentics.mesh.rest.client.MeshRestClient;
import com.gentics.mesh.rest.client.impl.MeshRestHttpClientImpl;

import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;

public class SearchAsYouTypeTest {

	@Test
	public void testSearch() {
		MeshRestClient client = new MeshRestHttpClientImpl("localhost", Vertx.vertx());

		client.setLogin("admin", "admin");
		client.login().toBlocking().value();

		JsonObject query = new JsonObject();
//		.put("analyzer", "snowball")
		query.put("query", new JsonObject().put("simple_query_string", new JsonObject().put("query", "Trabant*").put(
				"fields", new JsonArray().add("fields.description.raw")).put("default_operator", "and")));

		String queryStr = query.encodePrettily();

		System.out.println("Query:\n" + queryStr);
		JsonObject response = call(() -> client.searchNodesRaw("demo", queryStr));
		System.out.println(response.encodePrettily());

	}
}
