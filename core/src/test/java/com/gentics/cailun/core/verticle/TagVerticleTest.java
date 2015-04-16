package com.gentics.cailun.core.verticle;

import static com.gentics.cailun.demo.DemoDataProvider.PROJECT_NAME;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.PUT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.http.HttpMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.neo4j.graphdb.Transaction;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gentics.cailun.core.AbstractRestVerticle;
import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.auth.PermissionType;
import com.gentics.cailun.core.data.service.TagService;
import com.gentics.cailun.core.rest.tag.request.TagUpdateRequest;
import com.gentics.cailun.core.rest.tag.response.TagListResponse;
import com.gentics.cailun.core.rest.tag.response.TagResponse;
import com.gentics.cailun.test.AbstractRestVerticleTest;
import com.gentics.cailun.util.JsonUtils;
import com.gentics.cailun.util.RestAssert;

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

		// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
		Tag noPermTag = new Tag();
		try (Transaction tx = graphDb.beginTx()) {
			noPermTag = data().addTag(data().getNews(), "NoPermEN", "NoPermDE");
			// noPermTag = tagService.save(noPermTag);
			tx.success();
		}
		noPermTag = tagService.reload(noPermTag);
		assertNotNull(noPermTag.getUuid());

		// Test default paging parameters
		String response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags/", 200, "OK");
		TagListResponse restResponse = JsonUtils.readValue(response, TagListResponse.class);
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(0, restResponse.getMetainfo().getCurrentPage());
		assertEquals("The response did not contain the correct amount of items", data().getTotalTags(), restResponse.getData().size());

		int perPage = 4;
		// Extra Tags + permitted tag
		int totalTags = data().getTotalTags();
		int totalPages = (int) Math.ceil(totalTags / (double) perPage);
		List<TagResponse> allTags = new ArrayList<>();
		for (int page = 0; page < totalPages; page++) {
			response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags/?per_page=" + perPage + "&page=" + page, 200, "OK");
			restResponse = JsonUtils.readValue(response, TagListResponse.class);
			int expectedItemsCount = perPage;
			// The last page should only list 5 items
			if (page == 3) {
				expectedItemsCount = 4;
			}
			assertEquals("The expected item count for page {" + page + "} does not match", expectedItemsCount, restResponse.getData().size());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage());
			assertEquals(page, restResponse.getMetainfo().getCurrentPage());
			assertEquals("The amount of total pages did not match the expected value. There are {" + totalTags + "} tags and {" + perPage
					+ "} tags per page", totalPages, restResponse.getMetainfo().getPageCount());
			assertEquals("The total tag count does not match.", totalTags, restResponse.getMetainfo().getTotalCount());

			allTags.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all users were loaded when loading all pages.", totalTags, allTags.size());

		// Verify that the no_perm_tag is not part of the response
		final String noPermTagUUID = noPermTag.getUuid();
		List<TagResponse> filteredUserList = allTags.parallelStream().filter(restTag -> restTag.getUuid().equals(noPermTagUUID))
				.collect(Collectors.toList());
		assertTrue("The no perm tag should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags/?per_page=" + perPage + "&page=" + -1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags/?per_page=" + 0 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags/?per_page=" + -1 + "&page=" + 1, 400, "Bad Request");
		expectMessageResponse("error_invalid_paging_parameters", response);

		perPage = 25;
		totalPages = (int) Math.ceil(totalTags / (double) perPage);
		response = request(info, HttpMethod.GET, "/api/v1/" + PROJECT_NAME + "/tags/?per_page=" + perPage + "&page=" + 4242, 200, "OK");
		String json = "{\"data\":[],\"_metainfo\":{\"page\":4242,\"per_page\":25,\"page_count\":" + totalPages + ",\"total_count\":" + totalTags
				+ "}}";
		assertEqualsSanitizedJson("The json did not match the expected one.", json, response);
	}

	@Test
	public void testReadTagByUUID() throws Exception {

		Tag tag = data().getNews();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 200, "OK");
		TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);
		test.assertTag(tag, restTag);
	}

	@Test
	public void testReadTagByUUIDWithDepthParam() throws Exception {

		Tag tag = data().getNews();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?depth=3&lang=de,en", 200, "OK");
		TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);

		assertEquals(2, restTag.getTags().size());
		TagResponse childTag1 = restTag.getTags().get(0);
		assertNotNull(childTag1.getUuid());
		assertEquals("2014", childTag1.getProperty("en", "name"));

		TagResponse childTag2 = restTag.getTags().get(1);
		assertNotNull(childTag2.getUuid());
		assertEquals("2015", childTag2.getProperty("en", "name"));

		assertEquals(1, childTag1.getTags().size());
		TagResponse childTagOfChildTag = childTag1.getTags().get(0);
		assertNotNull(childTagOfChildTag.getUuid());
		assertEquals("March", childTagOfChildTag.getProperty("en", "name"));
	}

	@Test
	public void testReadTagByUUIDWithSingleLanguage() throws Exception {

		Tag tag = data().getNews();
		try (Transaction tx = graphDb.beginTx()) {
			roleService.addPermission(info.getRole(), tag, PermissionType.READ);
			tx.success();
		}

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);
		assertNull("The returned tag should not have an german name property.", restTag.getProperty("de", "name"));
		assertNotNull("The returned tag should have an english name property.", restTag.getProperty("en", "name"));
		test.assertTag(tag, restTag);
	}

	@Test
	public void testReadTagByUUIDWithMultipleLanguages() throws Exception {

		Tag tag = data().getNews();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en,de", 200, "OK");
		TagResponse restTag = JsonUtils.readValue(response, TagResponse.class);
		test.assertTag(tag, restTag);
		assertNotNull(restTag.getProperty("de", "name"));
		assertNotNull(restTag.getProperty("en", "name"));
	}

	@Test
	public void testReadTagByUUIDWithoutPerm() throws Exception {

		Tag tag = data().getNews();
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), tag, PermissionType.READ);
			tx.success();
		}

		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, tag.getUuid());
	}

	@Test
	public void testUpdateTagByUUID() throws Exception {

		Tag tag = data().getNews();

		// Create an tag update request
		TagUpdateRequest request = new TagUpdateRequest();
		request.setUuid(tag.getUuid());
		request.addProperty("en", "name", "new Name");

		// 1. Read the current tag in english
		String response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		TagResponse tagResponse = JsonUtils.readValue(response, TagResponse.class);
		try (Transaction tx = graphDb.beginTx()) {
			String name = tagService.getName(tag, data().getEnglish());
			assertEquals(name, tagResponse.getProperty("en", "name"));
			tx.success();
		}

		// 2. Setup the request object
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		final String newName = "new Name";
		tagUpdateRequest.addProperty("en", "name", newName);
		assertEquals(newName, tagUpdateRequest.getProperty("en", "name"));

		// 3. Send the request to the server
		// TODO test with no ?lang query parameter
		String requestJson = JsonUtils.toJson(request);
		response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK", requestJson);
		test.assertTag(tag, JsonUtils.readValue(response, TagResponse.class));

		// 4. read the tag again and verify that it was changed
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		tagResponse = JsonUtils.readValue(response, TagResponse.class);
		assertEquals(request.getProperty("en", "name"), tagResponse.getProperty("en", "name"));
		test.assertTag(tag, JsonUtils.readValue(response, TagResponse.class));
	}

	@Test
	public void testUpdateTagByUUIDWithoutPerm() throws Exception {
		Tag tag = data().getNews();

		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), tag, PermissionType.UPDATE);
			tx.success();
		}

		// Create an tag update request
		TagUpdateRequest request = new TagUpdateRequest();
		request.setUuid(tag.getUuid());
		request.addProperty("en", "name", "new Name");

		String requestJson = new ObjectMapper().writeValueAsString(request);
		String response = request(info, PUT, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid(), 403, "Forbidden", requestJson);
		expectMessageResponse("error_missing_perm", response, tag.getUuid());

		// read the tag again and verify that it was not changed
		response = request(info, GET, "/api/v1/" + PROJECT_NAME + "/tags/" + tag.getUuid() + "?lang=en", 200, "OK");
		TagResponse tagUpdateRequest = JsonUtils.readValue(response, TagResponse.class);

		try (Transaction tx = graphDb.beginTx()) {
			String name = tagService.getName(tag, data().getEnglish());
			assertEquals(name, tagUpdateRequest.getProperty("en", "name"));
			tx.success();
		}
	}

	// Delete Tests
	@Test
	public void testDeleteTagByUUID() throws Exception {
		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/" + data().getNews().getUuid(), 200, "OK");
		expectMessageResponse("tag_deleted", response, data().getNews().getUuid());
		assertNull("The tag should have been deleted", tagService.findByUUID(data().getNews().getUuid()));
	}

	@Test
	public void testDeleteTagByUUIDWithoutPerm() throws Exception {
		try (Transaction tx = graphDb.beginTx()) {
			roleService.revokePermission(info.getRole(), data().getNews(), PermissionType.DELETE);
			tx.success();
		}

		String response = request(info, DELETE, "/api/v1/" + PROJECT_NAME + "/tags/" + data().getNews().getUuid(), 403, "Forbidden");
		expectMessageResponse("error_missing_perm", response, data().getNews().getUuid());
		assertNotNull("The tag should not have been deleted", tagService.findByUUID(data().getNews().getUuid()));
	}

}
