package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.test.TestDataProvider.PROJECT_NAME;
import static org.junit.Assert.assertEquals;
import static io.vertx.core.http.HttpMethod.*;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.rest.content.request.ContentCreateRequest;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.UserInfo;
import com.gentics.cailun.util.JsonUtils;

public class ContentVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	ContentVerticle verticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return verticle;
	}

	// Create tests

	@Test
	public void testCreateContent() throws Exception {

		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.CREATE);

		ContentCreateRequest request = new ContentCreateRequest();
		request.setSchemaName("content");
		request.addProperty("english", "filename", "new-page.html");
		request.addProperty("english", "name", "english content name");
		request.addProperty("english", "content", "Blessed mealtime again!");
		request.setTagUuid(data().getLevel1a().getUuid());

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 200, "OK", JsonUtils.toJson(request));
		String responseJson = "ok";
		assertEqualsSanitizedJson("The response json did not match the expected one", responseJson, response);
	}

	@Test
	public void testCreateContentWithMissingTagUuid() throws Exception {

		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.CREATE);

		ContentCreateRequest request = new ContentCreateRequest();
		request.setSchemaName("content");
		request.addProperty("english", "filename", "new-page.html");
		request.addProperty("english", "name", "english content name");
		request.addProperty("english", "content", "Blessed mealtime again!");

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 200, "OK", JsonUtils.toJson(request));
		String responseJson = "ok";
		assertEqualsSanitizedJson("The response json did not match the expected one", responseJson, response);
	}

	@Test
	public void testCreateContentWithMissingPermission() throws Exception {

		// Add all perms except create
		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.READ);
		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.DELETE);

		ContentCreateRequest request = new ContentCreateRequest();
		request.setSchemaName("content");
		request.addProperty("english", "filename", "new-page.html");
		request.addProperty("english", "name", "english content name");
		request.addProperty("english", "content", "Blessed mealtime again!");
		request.setTagUuid(data().getLevel1a().getUuid());

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 200, "OK", JsonUtils.toJson(request));
		String responseJson = "ok";
		assertEqualsSanitizedJson("The response json did not match the expected one", responseJson, response);
	}

	// Read tests

	@Test
	public void testReadContentByUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		Content content = data().getContentLevel1A1();
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + content.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"filename\":\"test_1.en.html\",\"name\":\"test_1 english\",\"content\":\"Blessed Mealtime 1!\"},\"type\":\"content\",\"language\":\"en_US\"}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);
	}

	@Test
	public void testReadContentByBogusUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/bogusUUID", 404, "Not Found");
		String json = "{\"message\":\"Content not found for path {bogusUUID}\"}";
		assertEquals(json, response);
	}

	@Test
	public void testReadContentByInvalidUUID() throws Exception {
		UserInfo info = data().getUserInfo();
		String json = "{\"message\":\"Content not found for uuid {dde8ba06bb7211e4897631a9ce2772f5}\"}";
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/dde8ba06bb7211e4897631a9ce2772f5", 404, "Not Found");
		assertEquals(json, response);
	}

}
