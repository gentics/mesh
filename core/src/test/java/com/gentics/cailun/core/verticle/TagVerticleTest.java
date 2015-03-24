package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.test.TestDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.TagService;
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
	public void testReadAllTags() throws Exception {
		Tag tag = data().getLevel1a();
		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"schemaName\":\"tag\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadAllTagsWithPaging() {
		fail("Not yet implemented");
	}

	@Test
	public void testReadTagByUUID() throws Exception {

		Tag tag = data().getLevel1a();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"schemaName\":\"tag\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUUIDWithSingleLanguage() throws Exception {

		Tag tag = data().getLevel1a();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"schemaName\":\"tag\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"en\":{\"name\":\"level_1_a\"}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUUIDWithMultipleLanguages() throws Exception {

		Tag tag = data().getLevel1a();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		roleService.addPermission(info.getRole(), tag, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en,de", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"schemaName\":\"tag\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"de\":{\"name\":\"ebene_1_a\"},\"en\":{\"name\":\"level_1_a\"}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUUIDWithoutPerm() throws Exception {

		Tag tag = data().getLevel1a();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + tag.getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
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
		TagResponse tagResponse = JsonUtils.readValue(response, TagResponse.class);
		Assert.assertEquals(tag.getName(data().getEnglish()), tagResponse.getProperty("en", "name"));

		// 2. Setup the request object
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		final String newName = "new Name";
		tagUpdateRequest.addProperty("en", "name", newName);
		Assert.assertEquals(newName, tagUpdateRequest.getProperty("en", "name"));

		// 3. Send the request to the server
		// TODO test with no ?lang query parameter
		String requestJson = JsonUtils.toJson(request);
		response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK", requestJson);
		String json = "{\"uuid\":\"uuid-value\",\"schemaName\":\"tag\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"en\":{\"name\":\"new Name\"}}}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);

		// 4. read the tag again and verify that it was changed
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		tagResponse = JsonUtils.readValue(response, TagResponse.class);
		Assert.assertEquals(request.getProperty("en", "name"), tagResponse.getProperty("en", "name"));

		// TODO verify that only that property was changed
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
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		TagResponse tagUpdateRequest = JsonUtils.readValue(response, TagResponse.class);
		Assert.assertEquals(tag.getName(data().getEnglish()), tagUpdateRequest.getProperty("en", "name"));
	}

	// Delete Tests
	@Test
	public void testDeleteTagByUUID() throws Exception {

		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.DELETE);

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/" + data().getLevel1a().getUuid(), 200, "OK");
		String json = "{\"message\":\"Deleted tag\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNull("The tag should have been deleted", tagService.findByUUID(data().getLevel1a().getUuid()));
	}

	@Test
	public void testDeleteTagByUUIDWithoutPerm() throws Exception {

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/" + data().getLevel1a().getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + data().getLevel1a().getUuid() + "}\"}";
		assertEqualsSanitizedJson("Response json does not match the expected one.", json, response);
		assertNotNull("The tag should not have been deleted", tagService.findByUUID(data().getLevel1a().getUuid()));
	}

}
