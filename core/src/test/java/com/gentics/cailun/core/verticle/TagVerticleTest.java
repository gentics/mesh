package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.test.TestDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.rest.tag.request.TagCreateRequest;
import com.gentics.cailun.core.rest.tag.request.TagUpdateRequest;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.util.JsonUtils;

public class TagVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private TagVerticle tagVerticle;

	@Autowired
	private TagService tagService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return tagVerticle;
	}

	@Test
	public void testReadTagByUUID() throws Exception {

		Tag tag = data().getLevel1a();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"type\":null,\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUUIDWithSingleLanguage() throws Exception {

		Tag tag = data().getLevel1a();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"type\":null,\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"en\":{\"name\":\"subtag\"}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUUIDWithMultipleLanguages() throws Exception {

		Tag tag = data().getLevel1a();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en,de", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"type\":null,\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"de\":{\"name\":\"unterTag\"},\"en\":{\"name\":\"subtag\"}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByPath() throws Exception {

		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.READ);
		roleService.addPermission(info.getRole(), data().getLevel2a(), PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a/ebene_2_a", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"type\":null,\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"de\":{\"name\":\"ebene_2_a\"}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagWithBogusPath() throws Exception {

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/blub", 404, "Not found");
		String json = "ERROR";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUUIDWithoutPerm() throws Exception {

		Tag tag = data().getLevel1a();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 403, "Forbidden");
		String json = "ERROR";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByPathWithoutPerm() throws Exception {
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a/ebene_2_a", 403, "Forbidden");
		String json = "ERROR";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	// Create Tests

	@Test
	public void testCreateTagInPath() throws Exception {

		// Add needed permission
		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.CREATE);

		TagCreateRequest newTag = new TagCreateRequest();
		newTag.addProperty("english", "name", "new_subtag");

		String requestJson = new ObjectMapper().writeValueAsString(newTag);
		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/tags/", 200, "OK", requestJson);
		String json = "OK";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testCreateTagInPathWithoutPerm() throws Exception {
		// Add needed permission
		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.READ);
		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.DELETE);
		// Omit create permission

		TagCreateRequest newTag = new TagCreateRequest();
		newTag.addProperty("english", "name", "new_subtag");

		String requestJson = new ObjectMapper().writeValueAsString(newTag);
		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/tags/", 403, "Forbidden", requestJson);
		String json = "ERROR";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	// Update Tests
	@Test
	public void testUpdateTagInPath() throws Exception {

		Tag tag = data().getLevel1a();

		roleService.addPermission(info.getRole(), tag, PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		// Create an tag update request
		TagUpdateRequest request = new TagUpdateRequest();
		request.setUuid(tag.getUuid());
		request.addProperty("en", "name", "new Name");

		TagResponse updateTagResponse = new TagResponse();
		updateTagResponse.addProperty("english", "name", "new Name");

		String requestJson = new ObjectMapper().writeValueAsString(request);
		String response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"type\":null,\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"en\":{\"name\":\"level_1_a\"}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// read the tag again and verify that it was not changed
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a", 200, "OK", requestJson);
		TagUpdateRequest tagUpdateRequest = JsonUtils.readValue(response, TagUpdateRequest.class);
		Assert.assertEquals(request.getProperty("en", "name"), tagUpdateRequest.getProperty("en", "name"));

	}

	@Test
	public void testUpdateTagByPathWithoutPerm() throws Exception {

		Tag tag = data().getLevel1a();

		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		// Create an tag update request
		TagUpdateRequest request = new TagUpdateRequest();
		request.setUuid(tag.getUuid());
		request.addProperty("en", "name", "new Name");

		TagResponse updateTagResponse = new TagResponse();
		updateTagResponse.addProperty("en", "name", "new Name");

		String requestJson = new ObjectMapper().writeValueAsString(request);
		String response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a", 403, "Forbidden", requestJson);
		String json = "{\"message\":\"Missing permission on object {" + tag.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// read the tag again and verify that it was not changed
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a", 200, "OK", requestJson);
		TagUpdateRequest tagUpdateRequest = JsonUtils.readValue(response, TagUpdateRequest.class);
		Assert.assertEquals(tag.getName(data().getEnglish()), tagUpdateRequest.getProperty("en", "name"));
	}

	@Test
	public void testUpdateTagByUUID() throws Exception {
		Tag tag = data().getLevel1a();

		roleService.addPermission(info.getRole(), tag, PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		// Create an tag update request
		TagUpdateRequest request = new TagUpdateRequest();
		request.setUuid(tag.getUuid());
		request.addProperty("en", "name", "new Name");

		// 1. Read the current tag in english
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		TagUpdateRequest tagUpdateRequest = JsonUtils.readValue(response, TagUpdateRequest.class);
		Assert.assertEquals(tag.getName(data().getEnglish()), tagUpdateRequest.getProperty("en", "name"));

		// 2. Manipulate the request object
		final String newName = "new Name";
		tagUpdateRequest.addProperty("en", "name", newName);
		Assert.assertEquals(newName, tagUpdateRequest.getProperty("en", "name"));

		// 3. Send the request to the server
		// TODO test with no ?lang query parameter
		String requestJson = new ObjectMapper().writeValueAsString(request);
		response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"en\":{\"name\":\"new Name\"}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// 4. read the tag again and verify that it was changed
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		tagUpdateRequest = JsonUtils.readValue(response, TagUpdateRequest.class);
		Assert.assertEquals(request.getProperty("en", "name"), tagUpdateRequest.getProperty("en", "name"));
	}

	@Test
	public void testUpdateTagByUUIDWithoutPerm() throws Exception {
		Tag tag = data().getLevel1a();

		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		// Create an tag update request
		TagUpdateRequest request = new TagUpdateRequest();
		request.setUuid(tag.getUuid());
		request.addProperty("en", "name", "new Name");

		TagResponse updateTagResponse = new TagResponse();
		updateTagResponse.addProperty("english", "name", "new Name");

		String requestJson = new ObjectMapper().writeValueAsString(request);
		String response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 403, "Forbidden", requestJson);
		String json = "{\"message\":\"Missing permission on object {" + tag.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// read the tag again and verify that it was not changed
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 200, "OK");
		TagUpdateRequest tagUpdateRequest = JsonUtils.readValue(response, TagUpdateRequest.class);
		Assert.assertEquals(tag.getName(data().getEnglish()), tagUpdateRequest.getProperty("en", "name"));
	}

	// Delete Tests
	@Test
	public void testDeleteTagByUUID() throws Exception {

		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.DELETE);

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/" + data().getLevel1a().getUuid(), 200, "OK");
		String json = "OK";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The tag should have been deleted", tagService.findByUUID(data().getLevel1a().getUuid()));
	}

	@Test
	public void testDeleteTagByUUIDWithoutPerm() throws Exception {

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/" + data().getLevel1a().getUuid(), 403, "Forbidden");
		String json = "Error";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNotNull("The tag should not have been deleted", tagService.findByUUID(data().getLevel1a().getUuid()));
	}

	@Test
	public void testDeleteTagByPath() throws Exception {

		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.READ);
		roleService.addPermission(info.getRole(), data().getLevel2a(), PermissionType.DELETE);

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a/level_2_a", 200, "OK");

		String json = "OK";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The tag should have been deleted", tagService.findByUUID(data().getLevel1a().getUuid()));
	}

	@Test
	public void testDeleteTagByPathWithoutPerm() throws Exception {
		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.READ);

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/level_1_a/level_2_a", 403, "Forbidden");
		String json = "Error";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNotNull("The tag should not have been deleted", tagService.findByUUID(data().getLevel2a().getUuid()));
	}

}
