package com.gentics.mesh.core.verticle.tag;
import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.demo.DemoDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.api.common.PagingInfo;
import com.gentics.mesh.core.AbstractWebVerticle;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.project.ProjectTagFamilyVerticle;
import com.gentics.mesh.graphdb.Trx;
import com.gentics.mesh.test.AbstractRestVerticleTest;

import io.vertx.core.Future;
public class ProjectTagFamilyVerticleTest extends AbstractRestVerticleTest {

	@Autowired
	private ProjectTagFamilyVerticle tagFamilyVerticle;

	@Override
	public AbstractWebVerticle getVerticle() {
		return tagFamilyVerticle;
	}

	@Test
	public void testTagFamilyReadWithPerm() throws UnknownHostException, InterruptedException {

		TagFamily tagFamily;
		try (Trx tx = new Trx(db)) {
			tagFamily = project().getTagFamilyRoot().findAll().get(0);
			assertNotNull(tagFamily);
		}

		Future<TagFamilyResponse> future = getClient().findTagFamilyByUuid(PROJECT_NAME, tagFamily.getUuid());
		latchFor(future);
		assertSuccess(future);
		TagFamilyResponse response = future.result();

		assertNotNull(response);
		try (Trx tx = new Trx(db)) {
			assertEquals(tagFamily.getUuid(), response.getUuid());
		}
	}

	@Test
	public void testTagFamilyReadWithoutPerm() throws UnknownHostException, InterruptedException {
		String uuid;
		try (Trx tx = new Trx(db)) {
			Role role = role();
			TagFamily tagFamily = project().getTagFamilyRoot().findAll().get(0);
			uuid = tagFamily.getUuid();
			assertNotNull(tagFamily);
			role.revokePermissions(tagFamily, READ_PERM);
			tx.success();
		}

		Future<TagFamilyResponse> future = getClient().findTagFamilyByUuid(PROJECT_NAME, uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);
	}

	@Test
	public void testTagFamilyTagList() {
		String uuid;
		try (Trx tx = new Trx(db)) {
			TagFamily tagFamily = data().getTagFamilies().get("colors");
			uuid = tagFamily.getUuid();
		}
		Future<TagListResponse> future = getClient().findTagsForTagFamilies(PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);
	}

	@Test
	public void testTagFamilyListing() throws UnknownHostException, InterruptedException {
		final String noPermTagUUID;
		try (Trx tx = new Trx(db)) {
			// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
			TagFamily noPermTagFamily = project().getTagFamilyRoot().create("noPermTagFamily", user());
			noPermTagUUID = noPermTagFamily.getUuid();
			// TODO check whether the project reference should be moved from generic class into node mesh class and thus not be available for tags
			// noPermTag.addProject(project());
			assertNotNull(noPermTagFamily.getUuid());
			tx.success();
		}

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
		int totalTagFamilies = data().getTagFamilies().size();
		int totalPages = (int) Math.ceil(totalTagFamilies / (double) perPage);
		List<TagFamilyResponse> allTagFamilies = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			Future<TagFamilyListResponse> tagPageFut = getClient().findTagFamilies(PROJECT_NAME, new PagingInfo(page, perPage));
			latchFor(tagPageFut);
			assertSuccess(future);
			restResponse = tagPageFut.result();
			int expectedItemsCount = perPage;
			// Check the last page
			if (page == 1) {
				expectedItemsCount = 2;
			}
			assertEquals("The expected item count for page {" + page + "} does not match", expectedItemsCount, restResponse.getData().size());
			assertEquals(perPage, restResponse.getMetainfo().getPerPage());
			assertEquals("We requested page {" + page + "} but got a metainfo with a different page back.", page,
					restResponse.getMetainfo().getCurrentPage());
			assertEquals("The amount of total pages did not match the expected value. There are {" + totalTagFamilies + "} tags and {" + perPage
					+ "} tags per page", totalPages, restResponse.getMetainfo().getPageCount());
			assertEquals("The total tag count does not match.", totalTagFamilies, restResponse.getMetainfo().getTotalCount());

			allTagFamilies.addAll(restResponse.getData());
		}
		assertEquals("Somehow not all users were loaded when loading all pages.", totalTagFamilies, allTagFamilies.size());

		// Verify that the no_perm_tag is not part of the response
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
		totalPages = (int) Math.ceil(totalTagFamilies / (double) perPage);
		pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingInfo(4242, perPage));
		latchFor(pageFuture);
		TagFamilyListResponse tagList = pageFuture.result();
		assertEquals(0, tagList.getData().size());
		assertEquals(4242, tagList.getMetainfo().getCurrentPage());
		assertEquals(25, tagList.getMetainfo().getPerPage());
		assertEquals(totalTagFamilies, tagList.getMetainfo().getTotalCount());
		assertEquals(totalPages, tagList.getMetainfo().getPageCount());

	}

	@Test
	public void testTagFamilyCreateWithConflictingName() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("colors");
		Future<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request);
		latchFor(future);
		expectException(future, CONFLICT, "tagfamily_conflicting_name", "colors");
	}

	@Test
	public void testTagFamilyCreateWithPerm() {
		String name = "newTagFamily";
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName(name);
		Future<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request);
		latchFor(future);
		assertSuccess(future);
		assertEquals(name, future.result().getName());
	}

	@Test
	public void testTagFamilyCreateWithoutPerm() {
		try (Trx tx = new Trx(db)) {
			role().revokePermissions(project().getTagFamilyRoot(), CREATE_PERM);
			tx.success();
		}
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("SuperDoll");
		Future<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request);
		latchFor(future);

		try (Trx tx = new Trx(db)) {
			expectException(future, FORBIDDEN, "error_missing_perm", project().getTagFamilyRoot().getUuid());
		}
	}

	@Test
	public void testTagFamilyCreateWithNoName() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		// Don't set the name
		Future<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "tagfamily_name_not_set");
	}

	@Test
	public void testTagFamilyDeletionWithPerm() throws UnknownHostException, InterruptedException {
		String uuid;
		try (Trx tx = new Trx(db)) {
			TagFamily basicTagFamily = tagFamily("basic");
			uuid = basicTagFamily.getUuid();
			CountDownLatch latch = new CountDownLatch(1);
			project().getTagFamilyRoot().findByUuid(uuid, rh -> {
				assertNotNull(rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}

		Future<GenericMessageResponse> future = getClient().deleteTagFamily(PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			project().getTagFamilyRoot().findByUuid(uuid, rh -> {
				assertNull(rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}
	}

	@Test
	public void testTagFamilyDeletionWithNoPerm() throws UnknownHostException, InterruptedException {
		TagFamily basicTagFamily;
		try (Trx tx = new Trx(db)) {
			basicTagFamily = tagFamily("basic");
			Role role = role();
			role.revokePermissions(basicTagFamily, DELETE_PERM);
			tx.success();
		}
		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			project().getTagFamilyRoot().findByUuid(basicTagFamily.getUuid(), rh -> {
				assertNotNull(rh.result());
				latch.countDown();
			});
			failingLatch(latch);
		}

		try (Trx tx = new Trx(db)) {
			Future<GenericMessageResponse> future = getClient().deleteTagFamily(PROJECT_NAME, basicTagFamily.getUuid());
			latchFor(future);
			expectException(future, FORBIDDEN, "error_missing_perm", basicTagFamily.getUuid());
		}

		try (Trx tx = new Trx(db)) {
			CountDownLatch latch = new CountDownLatch(1);
			project().getTagFamilyRoot().findByUuid(basicTagFamily.getUuid(), rh -> {
				assertNotNull(rh.result());
				latch.countDown();
			});
			failingLatch(latch);

		}
	}

	@Test
	public void testTagFamilyUpdateWithConflictingName() {
		String newName = "colors";
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName(newName);

		try (Trx tx = new Trx(db)) {
			Future<TagFamilyResponse> future = getClient().updateTagFamily(PROJECT_NAME, tagFamily("basic").getUuid(), request);
			latchFor(future);
			expectException(future, CONFLICT, "tagfamily_conflicting_name", newName);
		}
	}

	@Test
	public void testTagFamilyUpdateWithPerm() throws UnknownHostException, InterruptedException {

		String uuid;
		String name;
		try (Trx tx = new Trx(db)) {
			TagFamily tagFamily = tagFamily("basic");
			uuid = tagFamily.getUuid();
			name = tagFamily.getName();
		}

		// 1. Read the current tagfamily
		Future<TagFamilyResponse> readTagFut = getClient().findTagFamilyByUuid(PROJECT_NAME, uuid);
		latchFor(readTagFut);
		assertSuccess(readTagFut);
		assertNotNull("The name of the tag should be loaded.", name);
		String restName = readTagFut.result().getName();
		assertNotNull("The tag name must be set.", restName);
		assertEquals(name, restName);

		// 2. Update the tagfamily
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName("new Name");
		TagFamilyUpdateRequest tagUpdateRequest = new TagFamilyUpdateRequest();
		final String newName = "new Name";
		tagUpdateRequest.setName(newName);
		assertEquals(newName, tagUpdateRequest.getName());

		// 3. Send the request to the server
		Future<TagFamilyResponse> updatedTagFut = getClient().updateTagFamily(PROJECT_NAME, uuid, tagUpdateRequest);
		latchFor(updatedTagFut);
		assertSuccess(updatedTagFut);
		TagFamilyResponse tagFamily2 = updatedTagFut.result();
		try (Trx tx = new Trx(db)) {
			test.assertTagFamily(tagFamily("basic"), tagFamily2);
		}

		// 4. read the tag again and verify that it was changed
		Future<TagFamilyResponse> reloadedTagFut = getClient().findTagFamilyByUuid(PROJECT_NAME, uuid);
		latchFor(reloadedTagFut);
		assertSuccess(reloadedTagFut);
		TagFamilyResponse reloadedTagFamily = reloadedTagFut.result();
		assertEquals(request.getName(), reloadedTagFamily.getName());
		try (Trx tx = new Trx(db)) {
			test.assertTagFamily(tagFamily("basic"), reloadedTagFamily);
		}

	}

	@Test
	public void testTagFamilyUpdateWithNoPerm() {
		fail("not implemented");
	}

}
