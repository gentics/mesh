package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;
import java.util.Collection;
import java.util.Vector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.madl.tx.Tx;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.parameter.impl.PublishParametersImpl;
import com.gentics.mesh.parameter.impl.VersioningParametersImpl;
import com.gentics.mesh.test.TestSize;
import com.gentics.mesh.test.context.AbstractMeshTest;
import com.gentics.mesh.test.context.MeshTestSetting;

import io.vertx.core.json.JsonObject;

@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLPermissionQueryTest extends AbstractMeshTest {

	private final String queryName = "rolePerms";

	@Test
	public void testReadPublishedNodeChildren() throws IOException {
		RoleResponse anonymousRole = client().findRoles().toSingle().blockingGet()
			.getData().stream()
			.filter(role -> role.getName().equals("anonymous"))
			.findAny().get();

		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(getGraphQLQuery(queryName));
		request.setVariables(new JsonObject().put("roleUuid", anonymousRole.getUuid()));

		// 3. Invoke the query and assert that the nodes can still be loaded (due to read published)
		GraphQLResponse response = call(() -> client().graphql(PROJECT_NAME, request));
		JsonObject json = new JsonObject(response.toJson());
		System.out.println(json.encodePrettily());
		assertThat(json).compliesToAssertions(queryName);
	}

	

}
