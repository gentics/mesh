package com.gentics.mesh.core.verticle.tag;

import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.UPDATE_PERM;
import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.Future;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.Tag;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.rest.tag.TagResponse;
import com.gentics.mesh.core.rest.tag.TagUpdateRequest;
import com.gentics.mesh.core.verticle.project.ProjectTagVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;
import com.gentics.mesh.util.BlueprintTransaction;

public class ProjectTagVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectTagVerticle tagVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return tagVerticle;
	}

	@Test
	public void testReadAllTags() throws Exception {

		// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
		TagFamily basicTagFamily = data().getTagFamily("basic");
		Tag noPermTag = basicTagFamily.create("noPermTag");
		// TODO check whether the project reference should be moved from generic class into node mesh class and thus not be available for tags
		data().getProject().getTagRoot().addTag(noPermTag);
		assertNotNull(noPermTag.getUuid());

		// Test default paging parameters
		Future<TagListResponse> future = getClient().findTags(PROJECT_NAME);
		latchFor(future);
		assertSuccess(future);

		TagListResponse restResponse = future.result();
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals("The response did not contain the correct amount of items", data().getTags().size(), restResponse.getData().size());

		int perPage = 4;
		// Extra Tags + permitted tag
		int totalTags = data().getTags().size();
		int totalPages = (int) Math.ceil(totalTags / (double) perPage);
		List<TagResponse> allTags = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			Future<TagListResponse> tagPageFut = getClient().findTags(PROJECT_NAME, new PagingInfo(page, perPage));
			latchFor(tagPageFut);
			assertSuccess(future);
			restResponse = tagPageFut.result();
			int expectedItemsCount = perPage;
			// The last page should only list 5 items
			if (page == 3) {
				expectedItemsCount = 4;
			}
			assertEquals("The expected item count for page {" + page + "} does not match", expectedItemsCount, restResponse.getData().size());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage());
			assertEquals("We requested page {" + page + "} but got a metainfo with a different page back.", page, restResponse.getMetainfo()
					.getCurrentPage());
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

		Future<TagListResponse> pageFuture = getClient().findTags(PROJECT_NAME, new PagingInfo(-1, perPage));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findTags(PROJECT_NAME, new PagingInfo(0, perPage));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findTags(PROJECT_NAME, new PagingInfo(1, 0));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findTags(PROJECT_NAME, new PagingInfo(1, -1));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		perPage = 25;
		totalPages = (int) Math.ceil(totalTags / (double) perPage);
		pageFuture = getClient().findTags(PROJECT_NAME, new PagingInfo(4242, perPage));
		latchFor(pageFuture);
		TagListResponse tagList = pageFuture.result();
		assertEquals(0, tagList.getData().size());
		assertEquals(4242, tagList.getMetainfo().getCurrentPage());
		assertEquals(25, tagList.getMetainfo().getPerPage());
		assertEquals(totalPages, tagList.getMetainfo().getTotalCount());
		assertEquals(totalPages, tagList.getMetainfo().getPageCount());
	}

	@Test
	public void testReadTagByUUID() throws Exception {
		Tag tag = data().getTag("red");
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
		Future<TagResponse> future = getClient().findTagByUuid(PROJECT_NAME, tag.getUuid());
		latchFor(future);
		assertSuccess(future);
		test.assertTag(tag, future.result());
	}

	@Test
	public void testReadTagByUUIDWithoutPerm() throws Exception {
		Tag tag = data().getTag("vehicle");
		assertNotNull("The UUID of the tag must not be null.", tag.getUuid());
		info.getRole().revokePermissions(tag, READ_PERM);

		Future<TagResponse> future = getClient().findTagByUuid(PROJECT_NAME, tag.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", tag.getUuid());
	}

	@Test
	public void testUpdateTagByUUID() throws Exception {

		Tag tag = data().getTag("vehicle");

		// 1. Read the current tag
		Future<TagResponse> readTagFut = getClient().findTagByUuid(PROJECT_NAME, tag.getUuid());
		latchFor(readTagFut);
		assertSuccess(readTagFut);
		String name = tag.getName();
		assertNotNull("The name of the tag should be loaded.", name);
		String restName = readTagFut.result().getFields().getName();
		assertNotNull("The tag name must be set.", restName);
		assertEquals(name, restName);

		// 2. Update the tag
		TagUpdateRequest request = new TagUpdateRequest();
		request.setUuid(tag.getUuid());
		request.getFields().setName("new Name");
		TagUpdateRequest tagUpdateRequest = new TagUpdateRequest();
		final String newName = "new Name";
		tagUpdateRequest.getFields().setName(newName);
		assertEquals(newName, tagUpdateRequest.getFields().getName());

		// 3. Send the request to the server
		Future<TagResponse> updatedTagFut = getClient().updateTag(PROJECT_NAME, tag.getUuid(), tagUpdateRequest);
		latchFor(updatedTagFut);
		assertSuccess(updatedTagFut);
		TagResponse tag2 = updatedTagFut.result();
		test.assertTag(tag, tag2);

		// 4. read the tag again and verify that it was changed
		Future<TagResponse> reloadedTagFut = getClient().findTagByUuid(PROJECT_NAME, tag.getUuid());
		latchFor(reloadedTagFut);
		assertSuccess(reloadedTagFut);
		TagResponse reloadedTag = reloadedTagFut.result();
		assertEquals(request.getFields().getName(), reloadedTag.getFields().getName());
		test.assertTag(tag, reloadedTag);
	}

	@Test
	public void testUpdateTagByUUIDWithoutPerm() throws Exception {
		Tag tag = data().getTag("vehicle");

		info.getRole().revokePermissions(tag, UPDATE_PERM);

		// Create an tag update request
		TagUpdateRequest request = new TagUpdateRequest();
		request.setUuid(tag.getUuid());
		request.getFields().setName("new Name");

		Future<TagResponse> tagUpdateFut = getClient().updateTag(PROJECT_NAME, tag.getUuid(), request);
		latchFor(tagUpdateFut);
		expectException(tagUpdateFut, FORBIDDEN, "error_missing_perm", tag.getUuid());

		// read the tag again and verify that it was not changed
		Future<TagResponse> tagReloadFut = getClient().findTagByUuid(PROJECT_NAME, tag.getUuid());
		latchFor(tagReloadFut);
		assertTrue(tagReloadFut.succeeded());
		TagResponse loadedTag = tagReloadFut.result();
		String name = tag.getName();
		assertEquals(name, loadedTag.getFields().getName());
	}

	// Delete Tests
	@Test
	public void testDeleteTagByUUID() throws Exception {
		Tag tag = data().getTag("vehicle");
		String uuid = tag.getUuid();
		Future<GenericMessageResponse> future = getClient().deleteTag(PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);
		expectMessageResponse("tag_deleted", future, uuid);
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			boot.tagRoot().findByUuid(uuid, rh -> {
				assertNull("The tag should have been deleted", rh.result());
			});
		}
	}

	@Test
	public void testDeleteTagByUUIDWithoutPerm() throws Exception {
		Tag tag = data().getTag("vehicle");
		String uuid = tag.getUuid();
		info.getRole().revokePermissions(tag, DELETE_PERM);
		Future<GenericMessageResponse> messageFut = getClient().deleteTag(PROJECT_NAME, uuid);
		latchFor(messageFut);
		expectException(messageFut, FORBIDDEN, "error_missing_perm", tag.getUuid());
		try (BlueprintTransaction tx = new BlueprintTransaction(fg)) {
			boot.tagRoot().findByUuid(tag.getUuid(), rh -> {
				assertNotNull("The tag should not have been deleted", rh.result());
			});
		}
	}

}
