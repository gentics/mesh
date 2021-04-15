package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.assertj.MeshAssertions.assertThat;
import static com.gentics.mesh.test.ClientHelper.call;
import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.core.rest.graphql.GraphQLRequest;
import com.gentics.mesh.core.rest.graphql.GraphQLResponse;
import com.gentics.mesh.parameter.VersioningParameters;
import com.gentics.mesh.parameter.client.VersioningParametersImpl;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.TestSize;

import io.vertx.core.json.JsonObject;

/**
 * This test will verify that the inheritance mechanism for the node type argument works as expected.
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = TestSize.FULL, startServer = true)
public class GraphQLNodeVersionArgInheritance extends AbstractGraphQLNodeTest {

	private String version;

	public GraphQLNodeVersionArgInheritance(String version) {
		this.version = version;
	}

	@Parameters(name = "params={0}")
	public static Collection<Object[]> paramData() {
		List<Object[]> data = new ArrayList<>();
		data.add(new Object[] { "none" });
		data.add(new Object[] { "draft" });
		data.add(new Object[] { "published" });
		return data;
	}

	@Before
	public void setupContent() {
		setupContents(true);
	}

	@Test
	public void testPermissions() throws IOException {
		String queryName = "node/version-arg";
		GraphQLRequest request = new GraphQLRequest();
		request.setQuery(getGraphQLQuery(queryName));
		GraphQLResponse response = null;
		switch (version) {
		case "none":
			response = call(() -> client().graphql(PROJECT_NAME, request));
			break;
		case "draft":
		case "published":
			VersioningParameters params = new VersioningParametersImpl();
			params.setVersion(version);
			response = call(() -> client().graphql(PROJECT_NAME, request, params));
			break;
		}
		JsonObject jsonResponse = new JsonObject(response.toJson());
		System.out.println(jsonResponse.encodePrettily());
		assertThat(jsonResponse).compliesToAssertions(queryName);

		switch (version) {
		case "none":
			// We expect the default type to be draft
			assertThat(jsonResponse).compliesTo("$.data.noType.version=1.1");
			assertThat(jsonResponse).compliesTo("$.data.noType.child.version=1.1");
			assertThat(jsonResponse).compliesTo("$.data.noType.child.child.version=1.1");
			break;
		case "draft":
			// We expect draft versions
			assertThat(jsonResponse).compliesTo("$.data.noType.version=1.1");
			assertThat(jsonResponse).compliesTo("$.data.noType.child.version=1.1");
			assertThat(jsonResponse).compliesTo("$.data.noType.child.child.version=1.1");
			break;
		case "published":
			// We expect the default to be overruled by the parameter and published contents to be returned.
			assertThat(jsonResponse).compliesTo("$.data.noType.version=1.0");
			assertThat(jsonResponse).compliesTo("$.data.noType.child.version=1.0");
			assertThat(jsonResponse).compliesTo("$.data.noType.child.child.version=1.1");
			break;
		}
	}
}
