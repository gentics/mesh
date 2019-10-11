
package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.ElasticsearchTestMode.CONTAINER_ES6;

import org.junit.Test;

import com.gentics.mesh.FieldUtil;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.schema.impl.SchemaCreateRequest;
import com.gentics.mesh.core.rest.schema.impl.SchemaResponse;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;
@MeshTestSetting(elasticsearch = CONTAINER_ES6, testSize = TestSize.FULL, startServer = true)
public class GraphQLNodeSearchEndpointTest extends AbstractMeshTest {

	@Test
	public void testNodeQueryWithManySchemas() throws Exception {
		recreateIndices();
		for (int i = 0; i < 45; i++) {
			SchemaCreateRequest request = new SchemaCreateRequest();
			request.setName("dummy" + i);
			request.addField(FieldUtil.createHtmlFieldSchema("content"));
			SchemaResponse response = call(() -> client().createSchema(request));
			call(() -> client().assignSchemaToProject(PROJECT_NAME, response.getUuid()));
		}
		waitForSearchIdleEvent();
		String queryName = "node-elasticsearch-query";
		GraphQLResponse response = call(() -> client().graphqlQuery(PROJECT_NAME, getGraphQLQuery(queryName)));
		JsonObject json = new JsonObject(response.toJson());
		System.out.println(json.encodePrettily());
		assertThat(json).compliesToAssertions(queryName);
	}

}
