package com.gentics.mesh.core.verticle;

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
import com.gentics.mesh.core.rest.meshnode.response.MeshNodeResponse;
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
	public void testReadTagByPath() throws Exception {

		String englishPath = data().getPathForNews2015Tag(data().getEnglish());
		MeshNode folder = data().getFolder("news2015");
		String path = "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath;
		String response = request(info, GET, path, 200, "OK");
		MeshNodeResponse restNode = JsonUtils.readValue(response, MeshNodeResponse.class);
		test.assertMeshNode(folder, restNode);
		assertNull("The path {" + path + "} leads to the english version of this tag thus the german properties should not be loaded",
				restNode.getProperties("de"));
		assertNotNull("The path {" + path + "} leads to the english version of this tag thus the english properties should be loaded.",
				restNode.getProperties("en"));
	}

	@Test
	public void testReadContentByPath() throws Exception {
		String path = "/api/v1/" + PROJECT_NAME + "/webroot/categories/Plane/Concorde.en.html?lang=en,de";
		MeshNode concordeNode = data().getContent("concorde");
		String response = request(info, GET, path, 200, "OK");
		System.out.println(response);
		MeshNodeResponse restNode = JsonUtils.readValue(response, MeshNodeResponse.class);
		test.assertMeshNode(concordeNode, restNode);
		assertNotNull(restNode.getProperties("de"));
		assertNotNull(restNode.getProperties("en"));
	}

	@Test
	public void testReadTagWithBogusPath() throws Exception {
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/blub", 404, "Not Found");
		expectMessageResponse("tag_not_found_for_path", response, "blub");
	}

	@Test
	public void testReadTagByPathWithoutPerm() throws Exception {
		try (Transaction tx = graphDb.beginTx()) {
			String englishPath = data().getPathForNews2015Tag(data().getEnglish());
			MeshNode newsFolder = data().getFolder("News2015");
			roleService.revokePermission(info.getRole(), newsFolder, PermissionType.READ);
			String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath, 403, "Forbidden");
			expectMessageResponse("error_missing_perm", response, newsFolder.getUuid());
			tx.success();
		}
	}

	@Test
	public void testReadContentByValidPath() throws Exception {
		try (Transaction tx = graphDb.beginTx()) {
			String englishPath = data().getPathForNews2015Tag(data().getEnglish());
			MeshNode folder = data().getFolder("news2015");
			String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath, 200, "OK");
			MeshNodeResponse restNode = JsonUtils.readValue(response, MeshNodeResponse.class);
			test.assertMeshNode(folder, restNode);
			tx.success();
		}
	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		String invalidPath = "subtag/subtag2/no-valid-page.html";
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + invalidPath, 404, "Not Found");
		expectMessageResponse("tag_not_found_for_path", response, invalidPath);
	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		String invalidPath = "subtag/subtag-no-valid-tag/no-valid-page.html";
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + invalidPath, 404, "Not Found");
		expectMessageResponse("tag_not_found_for_path", response, invalidPath);
	}

}
