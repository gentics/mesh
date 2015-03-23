package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.test.TestDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.rest.content.request.ContentCreateRequest;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.test.AbstractRestVerticleTest;
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
	public void testCreateContentWithBogusLanguageCode() throws HttpStatusCodeErrorException, Exception {
		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.CREATE);

		ContentCreateRequest request = new ContentCreateRequest();
		request.setSchemaName("content");
		request.addProperty("english", "filename", "new-page.html");
		request.addProperty("english", "name", "english content name");
		request.addProperty("english", "content", "Blessed mealtime again!");
		request.setTagUuid(data().getLevel1a().getUuid());

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 400, "Bad Request", JsonUtils.toJson(request));
		String responseJson = "{\"message\":\"Could not find language {english}\"}";
		assertEqualsSanitizedJson("The response json did not match the expected one", responseJson, response);
	}

	@Test
	public void testCreateContent() throws Exception {

		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.CREATE);

		ContentCreateRequest request = new ContentCreateRequest();
		request.setSchemaName("content");
		request.addProperty("en", "filename", "new-page.html");
		request.addProperty("en", "name", "english content name");
		request.addProperty("en", "content", "Blessed mealtime again!");
		request.setTagUuid(data().getLevel1a().getUuid());

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 200, "OK", JsonUtils.toJson(request));
		String responseJson = "{\"uuid\":\"uuid-value\",\"properties\":{\"en\":{\"filename\":\"new-page.html\",\"name\":\"english content name\",\"content\":\"Blessed mealtime again!\"}},\"schemaName\":\"content\",\"order\":0}";
		assertEqualsSanitizedJson("The response json did not match the expected one", responseJson, response);
	}

	@Test
	public void testCreateContentWithMissingTagUuid() throws Exception {

		roleService.addPermission(info.getRole(), data().getLevel1a(), PermissionType.CREATE);

		ContentCreateRequest request = new ContentCreateRequest();
		request.setSchemaName("content");
		request.addProperty("en", "filename", "new-page.html");
		request.addProperty("en", "name", "english content name");
		request.addProperty("en", "content", "Blessed mealtime again!");

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 400, "Bad Request", JsonUtils.toJson(request));
		String responseJson = "{\"message\":\"No parent tag for the content was specified. Please set a parent tag uuid.\"}";
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

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 403, "Forbidden", JsonUtils.toJson(request));
		String responseJson = "{\"message\":\"Missing permission on object {" + data().getLevel1a().getUuid() + "}\"}";
		assertEqualsSanitizedJson("The response json did not match the expected one", responseJson, response);
	}

	// Read tests

	@Test
	public void testReadContents() throws Exception {
		roleService.addPermission(info.getRole(), data().getContentLevel1A1(), PermissionType.READ);
		roleService.addPermission(info.getRole(), data().getContentLevel2C1(), PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/", 200, "OK");
		String json = "{\"contents\":[{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"de\":{\"filename\":\"test_1.de.html\",\"name\":\"test_1 german\",\"content\":\"Mahlzeit 1!\"},\"en\":{\"filename\":\"test_1.en.html\",\"name\":\"test_1 english\",\"content\":\"Blessed Mealtime 1!\"}},\"schemaName\":\"content\",\"order\":0},{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"de\":{\"filename\":\"test_1.de.html\",\"name\":\"test_1 german\",\"content\":\"Mahlzeit 1!\"},\"en\":{\"filename\":\"test_1.en.html\",\"name\":\"test_1 english\",\"content\":\"Blessed Mealtime 1!\"}},\"schemaName\":\"content\",\"order\":0}],\"_metainfo\":{\"page\":0,\"per_page\":0,\"page_count\":0,\"total_count\":0,\"links\":{}}}";

		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);
	}

	@Test
	public void testReadContentsWithoutPermissions() throws Exception {
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/", 200, "OK");
		String json = "{\"_metainfo\":{\"page\":0,\"per_page\":0,\"page_count\":0,\"total_count\":0,\"links\":{}}}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);
	}

	@Test
	public void testReadContentByUUID() throws Exception {
		Content content = data().getContentLevel1A1();
		roleService.addPermission(info.getRole(), content, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + content.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"de\":{\"filename\":\"test_1.de.html\",\"name\":\"test_1 german\",\"content\":\"Mahlzeit 1!\"},\"en\":{\"filename\":\"test_1.en.html\",\"name\":\"test_1 english\",\"content\":\"Blessed Mealtime 1!\"}},\"schemaName\":\"content\",\"order\":0}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);
	}

	@Test
	public void testReadContentByUUIDSingleLanguage() throws Exception {
		Content content = data().getContentLevel1A1();
		roleService.addPermission(info.getRole(), content, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + content.getUuid() + "?lang=de", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Stark\",\"firstname\":\"Tony\",\"username\":\"dummy_user\",\"emailAddress\":\"t.stark@spam.gentics.com\",\"groups\":[\"dummy_user_group\"]},\"properties\":{\"de\":{\"filename\":\"test_1.de.html\",\"name\":\"test_1 german\",\"content\":\"Mahlzeit 1!\"}},\"schemaName\":\"content\",\"order\":0}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);
	}

	@Test
	public void testReadContentByUUIDWithoutPermission() throws Exception {
		Content content = data().getContentLevel1A1();
		roleService.addPermission(info.getRole(), content, PermissionType.DELETE);
		roleService.addPermission(info.getRole(), content, PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), content, PermissionType.CREATE);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + content.getUuid(), 403, "Forbidden");
		String json = "{\"message\":\"Missing permission on object {" + content.getUuid() + "}\"}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);
	}

	@Test
	public void testReadContentByBogusUUID() throws Exception {
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/bogusUUID", 404, "Not Found");
		String json = "{\"message\":\"Content of uuid \\\"bogusUUID\\\" could not be found.\"}";
		assertEquals(json, response);
	}

	@Test
	public void testReadContentByInvalidUUID() throws Exception {
		String uuid = "dde8ba06bb7211e4897631a9ce2772f5";
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + uuid, 404, "Not Found");
		String json = "{\"message\":\"Content of uuid \\\"" + uuid + "\\\" could not be found.\"}";
		assertEquals(json, response);
	}

}
