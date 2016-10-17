package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import org.json.JSONException;
import org.junit.Test;

import com.gentics.mesh.test.AbstractRestEndpointTest;
import com.gentics.mesh.util.MeshJSONAssert;

import io.vertx.core.json.JsonObject;

public class GraphQLEndpointTest extends AbstractRestEndpointTest {

	@Test
	public void testSimpleQuery() throws JSONException {
		JsonObject response = call(() -> getClient().graphql(PROJECT_NAME, "{me{firstname}}"));
		MeshJSONAssert.assertEquals("{'data':{'me':{'firstname':'Joe'}}}", response);
	}

}
