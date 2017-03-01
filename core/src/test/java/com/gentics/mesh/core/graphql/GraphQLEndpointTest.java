package com.gentics.mesh.core.graphql;

import static com.gentics.mesh.test.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.test.context.MeshTestHelper.call;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
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
		String creationDate = db().noTx(() -> content().getCreationDate());
		//JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{ tagFamilies(name: \"colors\") { name, creator {firstname, lastname}, tags(page: 1, perPage:1) {name}}, schemas(name:\"content\") {name}, nodes(uuid:\"" + contentUuid + "\"){uuid, languagePaths(linkType: FULL) {languageTag, link}, availableLanguages, project {name, rootNode {uuid}}, created, creator { username, groups { name, roles {name} } }}}"));

		JsonObject response = call(() -> client().graphql(PROJECT_NAME, getQuery("full-query")));

		System.out.println(response.encodePrettily());
//		MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + contentUuid + "', 'created': '" + creationDate + "'}}}", response);

		//		JsonObject response = call(() -> client().graphql(PROJECT_NAME, "{nodes(uuid:\"" + contentUuid + "\") {uuid, fields { ... on content { name, content }}}}"));
		//		System.out.println(response.toString());
		//		MeshJSONAssert.assertEquals("{'data':{'nodes':{'uuid':'" + contentUuid + "'}}}", response);
	}

	private String getQuery(String name) throws IOException {
		return IOUtils.toString(getClass().getResourceAsStream("/graphql/" + name));
	}

}
