package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;

import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
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

		String englishPath = data().getPathForNews2015Tag(data().getEnglish());
		Tag tag = data().getNews2015();
		String path = "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath;
		String response = request(info, GET, path, 200, "OK");
		TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);
		test.assertTag(tag, restTag);
		assertNull("The path {" + path + "} leads to the english version of this tag thus the german properties should not be loaded",
				restTag.getProperties("de"));
		assertNotNull("The path {" + path + "} leads to the english version of this tag thus the english properties should be loaded.",
				restTag.getProperties("en"));
	}

	@Test
	public void testReadContentByPath() throws Exception {
		String path = "/api/v1/" + PROJECT_NAME + "/webroot/categories/Plane/Concorde.en.html?lang=en,de";
		Content concordeContent = data().getConcorde();
		String response = request(info, GET, path, 200, "OK");
		System.out.println(response);
		ContentResponse restContent = JsonUtils.readValue(response, ContentResponse.class);
		test.assertContent(concordeContent, restContent);
		assertNull("The path {" + path + "} leads to the english version of this tag thus the german properties should not be loaded",
				restContent.getProperties("de"));
		assertNotNull("The path {" + path + "} leads to the english version of this tag thus the english properties should be loaded.",
				restContent.getProperties("en"));
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
			Tag tag = data().getNews2015();
			roleService.revokePermission(info.getRole(), tag, PermissionType.READ);
			String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath, 403, "Forbidden");
			expectMessageResponse("error_missing_perm", response, data().getNews().getUuid());
			tx.success();
		}
	}

	@Test
	public void testReadContentByValidPath() throws Exception {
		try (Transaction tx = graphDb.beginTx()) {
			String englishPath = data().getPathForNews2015Tag(data().getEnglish());
			Tag tag = data().getNews2015();
			String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath, 200, "OK");
			TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);
			test.assertTag(tag, restTag);
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
