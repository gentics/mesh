package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.TestFullDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;

import org.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.graphdb.NoTx;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.MeshJSONAssert;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, useTinyDataset = false, startServer = true)
public class GraphQLEndpointTest extends AbstractMeshTest {

	@Test
	public void testSimpleQuery() throws JSONException {
		JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{me{firstname}}"));
		MeshJSONAssert.assertEquals("{'data':{'me':{'firstname':'Joe'}}}", response);
	}

	@Test
	public void testNodeQuery() throws JSONException {
		try (NoTx noTx = db().noTx()) {
			JsonObject response = call(
					() -> client().graphql(PROJECT_NAME, "{nodes(uuid:\"" + content().getUuid() + "\"){uuid}}"));
			MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + content().getUuid() + "'}}}", response);

			response = call(() -> client().graphql(PROJECT_NAME,
					"{nodes(uuid:\"" + content().getUuid() + "\") {uuid, fields { ... on content {title}}}}"));
			System.out.println(response.toString());
			MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + content().getUuid() + "'}}}", response);
		}
	}

	@Test
	public void testNodeFieldQuery() throws JSONException {
		JsonObject response = call(
				() -> client().graphql(PROJECT_NAME, "{nodes(field: name){uuid}}"));
		MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + content().getUuid() + "'}}}", response);
	}

}
