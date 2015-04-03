package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.Assert.assertEquals;
import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.demo.UserInfo;
import com.gentics.cailun.test.AbstractRestVerticleTest;

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

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath, 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"schemaName\":\"tag\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"de\":{\"name\":\"ebene_2_a\"}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagWithBogusPath() throws Exception {

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/blub", 404, "Not Found");
		String json = "{\"message\":\"Could not find tag for path \\\"blub\\\".\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByPathWithoutPerm() throws Exception {
		String englishPath = data().getPathForNews2015Tag(data().getEnglish());
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/" + englishPath, 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + data().getNews().getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadContentByValidPath() throws Exception {
		UserInfo info = data().getUserInfo();
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/subtag/english.html", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@gentics.com\"},\"properties\":{\"filename\":\"english.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime!\"},\"type\":\"content\",\"language\":\"en_US\"}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);

	}

	@Test
	public void testReadContentByInvalidPath() throws Exception {
		UserInfo info = data().getUserInfo();
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/subtag/subtag2/no-valid-page.html", 404, "Not Found");
		String json = "{\"message\":\"Could not find tag for path \\\"subtag/subtag2/no-valid-page.html\\\".\"}";
		assertEquals(json, response);

	}

	@Test
	public void testReadContentByInvalidPath2() throws Exception {
		UserInfo info = data().getUserInfo();
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/webroot/subtag/subtag-no-valid-tag/no-valid-page.html", 404,
				"Not Found");
		String json = "{\"message\":\"Could not find tag for path \\\"subtag/subtag-no-valid-tag/no-valid-page.html\\\".\"}";
		assertEquals(json, response);
	}

}
