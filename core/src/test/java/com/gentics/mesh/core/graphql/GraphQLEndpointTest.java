package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;

import org.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;
import com.gentics.mesh.util.MeshJSONAssert;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(useElasticsearch = false, testSize = TestSize.FULL, startServer = true)
public class GraphQLEndpointTest extends AbstractMeshTest {

	@Test
	public void testSimpleQuery() throws JSONException {
		JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{me{firstname}}"));
		MeshJSONAssert.assertEquals("{'data':{'me':{'firstname':'Joe'}}}", response);
	}

	@Test
	public void testNodeQuery() throws JSONException {
		String contentUuid = db().noTx(() -> content().getUuid());
//		JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{nodes(uuid:\"" + contentUuid + "\"){uuid}}"));
//		MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + contentUuid + "'}}}", response);

		JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{nodes(uuid:\"" + contentUuid + "\") {uuid, fields { ... on content { name }}}}"));
		System.out.println(response.toString());
		MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + contentUuid + "'}}}", response);
	}

}
