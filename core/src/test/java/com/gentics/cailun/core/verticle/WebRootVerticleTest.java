package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.Assert.*;
import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.demo.UserInfo;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.util.JsonUtils;

public class WebRootVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private WebRootVerticle webRootVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return webRootVerticle;
	}

	@Test
	public void testReadTagByPath() throws Exception {

		roleService.addPermission(info.getRole(), data().getNews(), PermissionType.READ);
		roleService.addPermission(info.getRole(), data().getNews2015(), PermissionType.READ);

		String englishPath = data().getPathForNews2015Tag(data().getEnglish());
		Tag tag = data().getNews2015();
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath, 200, "OK");
		TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);
		test.assertTag(tag, restTag);
		assertNull("The path leads to the english version of this tag thus the german properties should not be loaded", restTag.getProperties("de"));
		assertNotNull("The path leads to the english version of this tag thus the english properties should be loaded.", restTag.getProperties("en"));
	}

	@Test
	public void testReadTagWithBogusPath() throws Exception {

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/blub", 404, "Not Found");
		expectMessageResponse("tag_not_found_for_path", response, "blub");
	}

	@Test
	public void testReadTagByPathWithoutPerm() throws Exception {
		String englishPath = data().getPathForNews2015Tag(data().getEnglish());
		Tag tag = data().getNews2015();
		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), tag, PermissionType.READ);
			tx.success();
		}
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath, 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, data().getNews().getUuid());
	}

	@Test
	public void testReadContentByValidPath() throws Exception {
		UserInfo info = data().getUserInfo();
		String englishPath = data().getPathForNews2015Tag(data().getEnglish());
		Tag tag = data().getNews2015();
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath, 200, "OK");
		TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);
		test.assertTag(tag, restTag);
	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		UserInfo info = data().getUserInfo();
		String invalidPath = "subtag/subtag2/no-valid-page.html";
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + invalidPath, 404, "Not Found");
		expectMessageResponse("tag_not_found_for_path", response, invalidPath);
	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		UserInfo info = data().getUserInfo();
		String invalidPath = "subtag/subtag-no-valid-tag/no-valid-page.html";
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + invalidPath, 404, "Not Found");
		expectMessageResponse("tag_not_found_for_path", response, invalidPath);
	}

}
