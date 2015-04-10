package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Content;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.ContentService;
import com.gentics.cailun.core.rest.content.request.ContentCreateRequest;
import com.gentics.cailun.core.rest.content.response.ContentListResponse;
import com.gentics.cailun.core.rest.content.response.ContentResponse;
import com.gentics.cailun.error.HttpStatusCodeErrorException;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.util.JsonUtils;

public class ContentVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ContentVerticle verticle;

	@Autowired
	private ContentService contentService;

	@Override
	public AbstractRestVerticle getVerticle() {
		return verticle;
	}

	// Create tests

	@Test
	public void testCreateContentWithBogusLanguageCode() throws HttpStatusCodeErrorException, Exception {
		roleService.addPermission(info.getRole(), data().getNews(), PermissionType.CREATE);

		ContentCreateRequest request = new ContentCreateRequest();
		request.setSchemaName("content");
		request.addProperty("english", "filename", "new-page.html");
		request.addProperty("english", "name", "english content name");
		request.addProperty("english", "content", "Blessed mealtime again!");
		request.setTagUuid(data().getNews().getUuid());

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 400, "Bad Request", JsonUtils.toJson(request));
		expectMessageResponse("error_language_not_found", response, "english");
	}

	@Test
	public void testCreateContent() throws Exception {

		roleService.addPermission(info.getRole(), data().getNews(), PermissionType.CREATE);

		ContentCreateRequest request = new ContentCreateRequest();
		request.setSchemaName("content");
		request.addProperty("en", "filename", "new-page.html");
		request.addProperty("en", "name", "english content name");
		request.addProperty("en", "content", "Blessed mealtime again!");
		request.setTagUuid(data().getNews().getUuid());

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 200, "OK", JsonUtils.toJson(request));
		String responseJson = "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{\"en\":{\"filename\":\"new-page.html\",\"name\":\"english content name\",\"content\":\"Blessed mealtime again!\"}},\"schemaName\":\"content\",\"perms\":[],\"tags\":[],\"order\":0}";
		assertEqualsSanitizedJson("The response json did not match the expected one", responseJson, response);
	}

	@Test
	public void testCreateContentWithMissingTagUuid() throws Exception {

		roleService.addPermission(info.getRole(), data().getNews(), PermissionType.CREATE);

		ContentCreateRequest request = new ContentCreateRequest();
		request.setSchemaName("content");
		request.addProperty("en", "filename", "new-page.html");
		request.addProperty("en", "name", "english content name");
		request.addProperty("en", "content", "Blessed mealtime again!");

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 400, "Bad Request", JsonUtils.toJson(request));
		expectMessageResponse("content_missing_parenttag_field", response);

	}

	@Test
	public void testCreateContentWithMissingPermission() throws Exception {

		// Add all perms except create
		roleService.addPermission(info.getRole(), data().getNews(), PermissionType.READ);
		roleService.addPermission(info.getRole(), data().getNews(), PermissionType.UPDATE);
		roleService.addPermission(info.getRole(), data().getNews(), PermissionType.DELETE);

		ContentCreateRequest request = new ContentCreateRequest();
		request.setSchemaName("content");
		request.addProperty("english", "filename", "new-page.html");
		request.addProperty("english", "name", "english content name");
		request.addProperty("english", "content", "Blessed mealtime again!");
		request.setTagUuid(data().getNews().getUuid());

		String response = request(info, POST, "/api/v1/" + PROJECT_NAME + "/contents", 403, "Forbidden", JsonUtils.toJson(request));
		expectMessageResponse("error_missing_perm", response, data().getNews().getUuid());
	}

	// Read tests

	@Test
	public void testReadContents() throws Exception {

		roleService.addPermission(info.getRole(), data().getNews2015Content(), PermissionType.READ);

		final int nContents = 28;
		for (int i = 0; i < nContents; i++) {
			Content extraContent = new Content();
			extraContent.setCreator(info.getUser());
			extraContent = contentService.save(extraContent);
			roleService.addPermission(info.getRole(), extraContent, PermissionType.READ);
		}

		// Don't grant permissions to the no perm content. We want to make sure that this one will not be listed.
		Content noPermContent = new Content();
		noPermContent.setCreator(info.getUser());
		noPermContent = contentService.save(noPermContent);
		noPermContent = contentService.reload(noPermContent);
		assertNotNull(noPermContent.getUuid());

		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/contents/", 200, "OK");
		ContentListResponse restResponse = JsonUtils.readValue(response, ContentListResponse.class);
		Assert.assertEquals(25, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(0, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals(25, restResponse.getData().size());

		int perPage = 11;
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/contents/?per_page=" + perPage + "&page=" + 3, 200, "OK");
		restResponse = JsonUtils.readValue(response, ContentListResponse.class);
		Assert.assertEquals(perPage, restResponse.getData().size());

		// Extra Contents + permitted content
		int totalContents = nContents + data().getTotalContents();
		int totalPages = (int) Math.ceil(totalContents / (double) perPage);
		Assert.assertEquals("The response did not contain the correct amount of items", perPage, restResponse.getData().size());
		Assert.assertEquals(3, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals(totalPages, restResponse.getMetainfo().getPageCount());
		Assert.assertEquals(perPage, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(totalContents, restResponse.getMetainfo().getTotalCount());

		List<ContentResponse> allContents = new ArrayList<>();
		for (int page = 0; page < totalPages; page++) {
			response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/contents/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, ContentListResponse.class);
			allContents.addAll(restResponse.getData());
		}
		Assert.assertEquals("Somehow not all users were loaded when loading all pages.", totalContents, allContents.size());

		// Verify that the no_perm_content is not part of the response
		final String noPermContentUUID = noPermContent.getUuid();
		List<ContentResponse> filteredUserList = allContents.parallelStream().filter(restContent -> restContent.getUuid().equals(noPermContentUUID))
				.collect(Collectors.toList());
		assertTrue("The no perm content should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/contents/?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/contents/?per_page=" + 0 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/contents/?per_page=" + -1 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/contents/?per_page=" + 25 + "&page=" + 4242, 200, "OK");
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":6,\"total_count\":145}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);

	}

	@Test
	public void testReadContentsWithoutPermissions() throws Exception {
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/", 200, "OK");

		ContentListResponse restResponse = JsonUtils.readValue(response, ContentListResponse.class);

		int nElements = restResponse.getData().size();
		Assert.assertEquals("The amount of elements in the list did not match the expected count", 25, nElements);
		Assert.assertEquals(0, restResponse.getMetainfo().getCurrentPage());
		Assert.assertEquals(3, restResponse.getMetainfo().getPageCount());
		Assert.assertEquals(25, restResponse.getMetainfo().getPerPage());
		Assert.assertEquals(57, restResponse.getMetainfo().getTotalCount());
	}

	@Test
	public void testReadContentByUUID() throws Exception {
		Content content = data().getNews2015Content();
		roleService.addPermission(info.getRole(), content, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + content.getUuid(), 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{\"de\":{\"name\":\"Special News_2014 german\",\"content\":\"Neuigkeiten!\"},\"en\":{\"name\":\"Special News_2014 english\",\"content\":\"News!\"}},\"schemaName\":\"content\",\"perms\":[\"read\",\"create\",\"update\",\"delete\"],\"tags\":[],\"order\":0}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);
	}

	@Test
	public void testReadContentByUUIDWithDepthParam() throws Exception {
		Content content = data().getNews2015Content();
		roleService.addPermission(info.getRole(), content, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + content.getUuid() + "?depth=2", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{\"de\":{\"name\":\"Special News_2014 german\",\"content\":\"Neuigkeiten!\"},\"en\":{\"name\":\"Special News_2014 english\",\"content\":\"News!\"}},\"schemaName\":\"content\",\"perms\":[\"read\",\"create\",\"update\",\"delete\"],\"tags\":[{\"uuid\":\"uuid-value\",\"schemaName\":\"tag\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{},\"childTags\":[],\"perms\":[\"read\",\"create\",\"update\",\"delete\"]},{\"uuid\":\"uuid-value\",\"schemaName\":\"tag\",\"order\":0,\"creator\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{},\"childTags\":[],\"perms\":[\"read\",\"create\",\"update\",\"delete\"]}],\"order\":0}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);
	}

	@Test
	public void testReadContentByUUIDSingleLanguage() throws Exception {
		Content content = data().getNews2015Content();
		roleService.addPermission(info.getRole(), content, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + content.getUuid() + "?lang=de", 200, "OK");
		String json = "{\"uuid\":\"uuid-value\",\"author\":{\"uuid\":\"uuid-value\",\"lastname\":\"Doe\",\"firstname\":\"Joe\",\"username\":\"joe1\",\"emailAddress\":\"j.doe@spam.gentics.com\",\"groups\":[\"joe1_group\"],\"perms\":[]},\"properties\":{\"de\":{\"name\":\"Special News_2014 german\",\"content\":\"Neuigkeiten!\"}},\"schemaName\":\"content\",\"perms\":[\"read\",\"create\",\"update\",\"delete\"],\"tags\":[],\"order\":0}";
		assertEqualsSanitizedJson("The response json did not match the expected one", json, response);
	}

	@Test
	public void testReadContentWithBogusLanguageCode() throws Exception {

		Content content = data().getNews2015Content();
		roleService.addPermission(info.getRole(), content, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + content.getUuid() + "?lang=blabla,edgsdg", 400, "Bad Request");
		expectMessageResponse("error_language_not_found", response, "blabla");

	}

	@Test
	public void testReadContentByUUIDWithoutPermission() throws Exception {
		Content content = data().getNews2015Content();
		roleService.revokePermission(info.getRole(), content, PermissionType.READ);

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + content.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, content.getUuid());
	}

	@Test
	public void testReadContentByBogusUUID() throws Exception {
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/bogusUUID", 404, "Not Found");
		expectMessageResponse("object_not_found_for_uuid", response, "bogusUUID");
	}

	@Test
	public void testReadContentByInvalidUUID() throws Exception {
		String uuid = "dde8ba06bb7211e4897631a9ce2772f5";
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/contents/" + uuid, 404, "Not Found");
		expectMessageResponse("object_not_found_for_uuid", response, uuid);
	}

}
