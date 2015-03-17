package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.test.TestDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static org.junit.Assert.assertEquals;
import io.vertx.core.http.HttpMethod;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.test.UserInfo;

public class WebRootVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private WebRootVerticle webRootVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return webRootVerticle;
	}

	@Test
	public void testReadTagByPath() throws Exception {

		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.READ);
		roleService.addPermission(info.getRole(), data().getLevel2a(), PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/level_1_a/ebene_2_a", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"type\":null,\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"de\":{\"name\":\"ebene_2_a\"}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagWithBogusPath() throws Exception {

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/blub", 404, "Not Found");
		String json = "ERROR";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByPathWithoutPerm() throws Exception {
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/webroot/level_1_a/ebene_2_a", 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + data().getLevel2a().getUuid() + "}\"}";
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
		String json = "{\"message\":\"Content not found for path {subtag/subtag2/no-valid-page.html}\"}";
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

	// Create Tests

	// @Test
	// public void testCreateContentByPath() throws Exception {
	// UserInfo info = data().getUserInfo();
	// ContentCreateRequest request = new ContentCreateRequest();
	// request.setSchema("content");
	// request.addProperty("english", "filename", "new-page.html");
	// request.addProperty("english", "name", "english content name");
	// request.addProperty("english", "content", "Blessed mealtime again!");
	//
	// String response = request(info, HttpMethod.POST, "/api/v1/" + PROJECT_NAME + "/contents/subtag/newpage.html", 200, "OK",
	// new ObjectMapper().writeValueAsString(request));
	// String responseJson =
	// "{\"uuid\":\"uuid-value\",\"author\":null,\"properties\":{\"filename\":\"new-page.html\",\"name\":\"english content name\",\"content\":\"blessed mealtime again!\"},\"type\":\"content\",\"language\":\"en_US\"}";
	// assertEqualsSanitizedJson("The response json did not match the expected one", responseJson, response);
	// }
	//
	//
	// @Test
	// public void testCreateTagInPath() throws Exception {
	//
	// // Add needed permission
	// roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.CREATE);
	//
	// TagCreateRequest newTag = new TagCreateRequest();
	// newTag.addProperty("english", "name", "new_subtag");
	//
	// String requestJson = new ObjectMapper().writeValueAsString(newTag);
	// String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/tags/", 200, "OK", requestJson);
	// String json = "OK";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	// }
	//
	// @Test
	// public void testCreateTagInPathWithoutPerm() throws Exception {
	// // Add needed permission
	// roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.READ);
	// roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.UPDATE);
	// roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.DELETE);
	// // Omit create permission
	//
	// TagCreateRequest newTag = new TagCreateRequest();
	// newTag.addProperty("english", "name", "new_subtag");
	//
	// String requestJson = new ObjectMapper().writeValueAsString(newTag);
	// String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/tags/", 403, "Forbidden", requestJson);
	// String json = "ERROR";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	// }
	//
	// // Update Tests
	// @Test
	// public void testUpdateTagInPath() throws Exception {
	//
	// Tag tag = data().getLevel1a();
	//
	// roleService.addPermission(info.getRole(), tag, PermissionType.UPDATE);
	// roleService.addPermission(info.getRole(), tag, PermissionType.READ);
	//
	// // Create an tag update request
	// TagUpdateRequest request = new TagUpdateRequest();
	// request.setUuid(tag.getUuid());
	// request.addProperty("en", "name", "new Name");
	//
	// TagResponse updateTagResponse = new TagResponse();
	// updateTagResponse.addProperty("english", "name", "new Name");
	//
	// String requestJson = new ObjectMapper().writeValueAsString(request);
	// String response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a", 200, "OK", requestJson);
	// String json =
	// "{\"uuid\":\"uuid-value\",\"type\":null,\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"en\":{\"name\":\"level_1_a\"}}}";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	//
	// // read the tag again and verify that it was not changed
	// response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a", 200, "OK", requestJson);
	// TagUpdateRequest tagUpdateRequest = JsonUtils.readValue(response, TagUpdateRequest.class);
	// Assert.assertEquals(request.getProperty("en", "name"), tagUpdateRequest.getProperty("en", "name"));
	//
	// }
	//
	// @Test
	// public void testUpdateTagByPathWithoutPerm() throws Exception {
	//
	// Tag tag = data().getLevel1a();
	//
	// roleService.addPermission(info.getRole(), tag, PermissionType.READ);
	//
	// // Create an tag update request
	// TagUpdateRequest request = new TagUpdateRequest();
	// request.setUuid(tag.getUuid());
	// request.addProperty("en", "name", "new Name");
	//
	// TagResponse updateTagResponse = new TagResponse();
	// updateTagResponse.addProperty("en", "name", "new Name");
	//
	// String requestJson = new ObjectMapper().writeValueAsString(request);
	// String response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a", 403, "Forbidden", requestJson);
	// String json = "{\"message\":\"Missing permission on object {" + tag.getUuid() + "}\"}";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	//
	// // read the tag again and verify that it was not changed
	// response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a", 200, "OK", requestJson);
	// TagUpdateRequest tagUpdateRequest = JsonUtils.readValue(response, TagUpdateRequest.class);
	// A
	// }

	// @Test
	// public void testDeleteTagByPath() throws Exception {
	//
	// roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.READ);
	// roleService.addPermission(info.getRole(), data().getLevel2a(), PermissionType.DELETE);
	//
	// String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a/level_2_a", 200, "OK");
	//
	// String json = "OK";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	// assertNull("The tag should have been deleted", tagService.findByUUID(data().getLevel1a().getUuid()));
	// }

	// @Test
	// public void testDeleteTagByPathWithoutPerm() throws Exception {
	// roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.READ);
	//
	// String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a/level_2_a", 403, "Forbidden");
	// String json = "Error";
	// assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	// assertNotNull("The tag should not have been deleted", tagService.findByUUID(data().getLevel2a().getUuid()));
	// }

}
