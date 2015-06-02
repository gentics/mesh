package com.gentics.mesh.core.verticle.webroot;

import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.model.MeshNode;
import com.gentics.mesh.core.data.model.auth.PermissionType;
import com.gentics.mesh.core.rest.node.response.NodeResponse;
import com.gentics.mesh.core.verticle.WebRootVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.JsonUtils;

public class WebRootVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private WebRootVerticle webRootVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return webRootVerticle;
	}

	@Test
	public void testReadFolderByPath() throws Exception {

		MeshNode folder = data().getFolder("2015");
		String path = "/api/v1/" + PROJECT_NAME + "/webroot/News/2015";
		String response = request(info, GET, path, 200, "OK");
		NodeResponse restNode = JsonUtils.readValue(response, NodeResponse.class);
		test.assertMeshNode(folder, restNode);
		assertNull("The path {" + path + "} leads to the english version of this tag thus the german properties should not be loaded",
				restNode.getProperties("de"));
		assertNotNull("The path {" + path + "} leads to the english version of this tag thus the english properties should be loaded.",
				restNode.getProperties("en"));
	}

	@Test
	public void testReadContentByPath() throws Exception {
		String path = "/api/v1/" + PROJECT_NAME + "/webroot/Products/Concorde.en.html?lang=en,de";
		MeshNode concordeNode = data().getContent("concorde");
		String response = request(info, GET, path, 200, "OK");
		NodeResponse restNode = JsonUtils.readValue(response, NodeResponse.class);
		test.assertMeshNode(concordeNode, restNode);
		assertNotNull(restNode.getProperties("de"));
		assertNotNull(restNode.getProperties("en"));
	}

	@Test
	public void testReadFolderWithBogusPath() throws Exception {
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/blub", 404, "Not Found");
		expectMessageResponse("node_not_found_for_path", response, "blub");
	}

	@Test
	public void testReadFolderByPathWithoutPerm() throws Exception {
		String englishPath = "News/2015";
		MeshNode newsFolder = data().getFolder("2015");
		System.out.println(newsFolder.getUuid());
		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), newsFolder, PermissionType.READ);
			tx.success();
		}

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath, 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, newsFolder.getUuid());
	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		String invalidPath = "News/2015/no-valid-content.html";
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + invalidPath, 404, "Not Found");
		expectMessageResponse("node_not_found_for_path", response, invalidPath);
	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		String invalidPath = "News/no-valid-folder/no-valid-content.html";
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + invalidPath, 404, "Not Found");
		expectMessageResponse("node_not_found_for_path", response, invalidPath);
	}

}
