package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import org.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.AbstractRestEndpointTest;
import com.gentics.mesh.util.MeshJSONAssert;

import io.vertx.core.json.JsonObject;

public class GraphQLEndpointTest extends AbstractRestEndpointTest {

	@Test
	public void testSimpleQuery() throws JSONException {
		JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{me{firstname}}"));
		MeshJSONAssert.assertEquals("{'data':{'me':{'firstname':'Joe'}}}", response);
	}

	@Test
	public void testNodeQuery() throws JSONException {
		try (NoTx noTx = db.noTx()) {
			JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{nodes(uuid:\"" + content().getUuid() + "\"){uuid}}"));
			MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + content().getUuid() + "'}}}", response);

			response = call(() -> client().graphql(PROJECT_NAME, "{nodes(uuid:\"" + content().getUuid() + "\") {uuid, fields { ... on content {title}}}}"));
			System.out.println(response.toString());
			MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + content().getUuid() + "'}}}", response);
		}
	}

}
