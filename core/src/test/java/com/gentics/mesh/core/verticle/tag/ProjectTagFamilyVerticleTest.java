package com.gentics.mesh.core.verticle.tag;

import static com.gentics.mesh.core.data.relationship.Permission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.Permission.READ_PERM;
import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import io.vertx.core.Future;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractRestVerticle;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.verticle.project.ProjectTagFamilyVerticle;
import com.gentics.mesh.test.AbstractRestVerticleTest;

public class ProjectTagFamilyVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectTagFamilyVerticle tagFamilyVerticle;

	@Override
	public AbstractRestVerticle getVerticle() {
		return tagFamilyVerticle;
	}

	@Test
	public void testTagFamilyReadWithPerm() throws UnknownHostException, InterruptedException {
		TagFamily tagFamily = data().getProject().getTagFamilyRoot().findAll().get(0);
		assertNotNull(tagFamily);
		Future<TagFamilyResponse> future = getClient().findTagFamilyByUuid(PROJECT_NAME, tagFamily.getUuid());
		latchFor(future);
		assertSuccess(future);
		TagFamilyResponse response = future.result();
		assertNotNull(response);
		assertEquals(tagFamily.getUuid(), response.getUuid());
	}

	@Test
	public void testTagFamilyReadWithoutPerm() throws UnknownHostException, InterruptedException {
		Role role = data().getUserInfo().getRole();
		TagFamily tagFamily = data().getProject().getTagFamilyRoot().findAll().get(0);
		assertNotNull(tagFamily);
		role.revokePermissions(tagFamily, READ_PERM);

		Future<TagFamilyResponse> future = getClient().findTagFamilyByUuid(PROJECT_NAME, tagFamily.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", tagFamily.getUuid());
	}

	@Test
	public void testTagFamilyListing() throws UnknownHostException, InterruptedException {

		// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
		TagFamily basicTagFamily = data().getTagFamily("basic");
		TagFamily noPermTagFamily = data().getProject().getTagFamilyRoot().create("noPermTagFamily");
		// TODO check whether the project reference should be moved from generic class into node mesh class and thus not be available for tags
		// noPermTag.addProject(data().getProject());
		assertNotNull(noPermTagFamily.getUuid());

		// Test default paging parameters
		Future<TagFamilyListResponse> future = getClient().findTagFamilies(PROJECT_NAME);
		latchFor(future);
		assertSuccess(future);

		TagFamilyListResponse restResponse = future.result();
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals("The response did not contain the correct amount of items", data().getTagFamilies().size(), restResponse.getData().size());

		int perPage = 4;
		// Extra Tags + permitted tag
		int totalTags = data().getTags().size();
		int totalPages = (int) Math.ceil(totalTags / (double) perPage);
		List<TagFamilyResponse> allTagFamilies = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			Future<TagFamilyListResponse> tagPageFut = getClient().findTagFamilies(PROJECT_NAME, new PagingInfo(page, perPage));
			latchFor(tagPageFut);
			assertSuccess(future);
			restResponse = tagPageFut.result();
			int expectedItemsCount = perPage;
			// Check the last page
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

			allTagFamilies.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all users were loaded when loading all pages.", totalTags, allTagFamilies.size());

		// Verify that the no_perm_tag is not part of the response
		final String noPermTagUUID = noPermTagFamily.getUuid();
		List<TagFamilyResponse> filteredUserList = allTagFamilies.parallelStream().filter(restTag -> restTag.getUuid().equals(noPermTagUUID))
				.collect(Collectors.toList());
		assertTrue("The no perm tag should not be part of the list since no permissions were added.", filteredUserList.size() == 0);

		Future<TagFamilyListResponse> pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingInfo(-1, perPage));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingInfo(0, perPage));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingInfo(1, 0));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingInfo(1, -1));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		perPage = 25;
		totalPages = (int) Math.ceil(totalTags / (double) perPage);
		pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingInfo(4242, perPage));
		latchFor(pageFuture);
		TagFamilyListResponse tagList = pageFuture.result();
		assertEquals(0, tagList.getData().size());
		assertEquals(4242, tagList.getMetainfo().getCurrentPage());
		assertEquals(25, tagList.getMetainfo().getPerPage());
		assertEquals(totalPages, tagList.getMetainfo().getTotalCount());
		assertEquals(totalPages, tagList.getMetainfo().getPageCount());

	}

	@Test
	public void testTagFamilyCreateWithPerm() {

	}

	@Test
	public void testTagFamilyCreateWithoutPerm() {

	}

	@Test
	public void testTagFamilyCreateWithNoName() {

	}

	@Test
	public void testTagFamilyDeletionWithPerm() throws UnknownHostException, InterruptedException {
		TagFamily basicTagFamily = data().getTagFamily("basic");
		String uuid = basicTagFamily.getUuid();
		data().getProject().getTagFamilyRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());	
		});

		Future<GenericMessageResponse> future = getClient().deleteTagFamily(PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);

		data().getProject().getTagFamilyRoot().findByUuid(uuid, rh -> {
			assertNull(rh.result());
		});

	}

	@Test
	public void testTagFamilyDeletionWithNoPerm() throws UnknownHostException, InterruptedException {
		TagFamily basicTagFamily = data().getTagFamily("basic");
		String uuid = basicTagFamily.getUuid();
		Role role = data().getUserInfo().getRole();
		role.revokePermissions(basicTagFamily, DELETE_PERM);

		data().getProject().getTagFamilyRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());	
		});
		Future<GenericMessageResponse> future = getClient().deleteTagFamily(PROJECT_NAME, uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", basicTagFamily.getUuid());
		data().getProject().getTagFamilyRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());	
		});
	}

	@Test
	public void testTagFamilyUpdateWithPerm() throws UnknownHostException, InterruptedException {

		TagFamily tagFamily = data().getTagFamily("basic");

		// 1. Read the current tagfamily
		Future<TagFamilyResponse> readTagFut = getClient().findTagFamilyByUuid(PROJECT_NAME, tagFamily.getUuid());
		latchFor(readTagFut);
		assertSuccess(readTagFut);
		String name = tagFamily.getName();
		assertNotNull("The name of the tag should be loaded.", name);
		String restName = readTagFut.result().getName();
		assertNotNull("The tag name must be set.", restName);
		assertEquals(name, restName);

		// 2. Update the tagfamily
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setUuid(tagFamily.getUuid());
		request.setName("new Name");
		TagFamilyUpdateRequest tagUpdateRequest = new TagFamilyUpdateRequest();
		final String newName = "new Name";
		tagUpdateRequest.setName(newName);
		assertEquals(newName, tagUpdateRequest.getName());

		// 3. Send the request to the server
		Future<TagFamilyResponse> updatedTagFut = getClient().updateTagFamily(PROJECT_NAME, tagFamily.getUuid(), tagUpdateRequest);
		latchFor(updatedTagFut);
		assertSuccess(updatedTagFut);
		TagFamilyResponse tagFamily2 = updatedTagFut.result();
		test.assertTagFamily(tagFamily, tagFamily2);

		// 4. read the tag again and verify that it was changed
		Future<TagFamilyResponse> reloadedTagFut = getClient().findTagFamilyByUuid(PROJECT_NAME, tagFamily.getUuid());
		latchFor(reloadedTagFut);
		assertSuccess(reloadedTagFut);
		TagFamilyResponse reloadedTagFamily = reloadedTagFut.result();
		assertEquals(request.getName(), reloadedTagFamily.getName());
		test.assertTagFamily(tagFamily, reloadedTagFamily);

	}

	@Test
	public void testTagFamilyUpdateWithNoPerm() {

	}

}
