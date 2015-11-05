package com.gentics.mesh.core.tagfamily;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.demo.TestDataProvider.PROJECT_NAME;
import static com.gentics.mesh.util.MeshAssert.assertElement;
import static com.gentics.mesh.util.MeshAssert.assertSuccess;
import static com.gentics.mesh.util.MeshAssert.failingLatch;
import static com.gentics.mesh.util.MeshAssert.latchFor;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONFLICT;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;
import static io.netty.handler.codec.http.HttpResponseStatus.NOT_FOUND;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.stream.Collectors;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.TagFamily;
import com.gentics.mesh.core.rest.common.GenericMessageResponse;
import com.gentics.mesh.core.rest.error.HttpStatusCodeErrorException;
import com.gentics.mesh.core.rest.tag.TagFamilyCreateRequest;
import com.gentics.mesh.core.rest.tag.TagFamilyListResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyResponse;
import com.gentics.mesh.core.rest.tag.TagFamilyUpdateRequest;
import com.gentics.mesh.core.rest.tag.TagListResponse;
import com.gentics.mesh.core.verticle.tagfamily.TagFamilyVerticle;
import com.gentics.mesh.query.impl.PagingParameter;
import com.gentics.mesh.query.impl.RolePermissionParameter;
import com.gentics.mesh.test.AbstractBasicCrudVerticleTest;

import io.vertx.core.Future;

public class TagFamilyVerticleTest extends AbstractBasicCrudVerticleTest {

	@Autowired
	private TagFamilyVerticle verticle;

	@Override
	public List<AbstractSpringVerticle> getVertices() {
		List<AbstractSpringVerticle> list = new ArrayList<>();
		list.add(verticle);
		return list;
	}

	@Test
	@Override
	public void testReadByUUID() throws UnknownHostException, InterruptedException {
		TagFamily tagFamily = project().getTagFamilyRoot().findAll().get(0);
		assertNotNull(tagFamily);

		Future<TagFamilyResponse> future = getClient().findTagFamilyByUuid(PROJECT_NAME, tagFamily.getUuid());
		latchFor(future);
		assertSuccess(future);
		TagFamilyResponse response = future.result();

		assertNotNull(response);
		assertEquals(tagFamily.getUuid(), response.getUuid());
	}

	@Test
	@Override
	public void testReadByUuidWithRolePerms() {
		TagFamily tagFamily = project().getTagFamilyRoot().findAll().get(0);
		String uuid = tagFamily.getUuid();

		Future<TagFamilyResponse> future = getClient().findTagFamilyByUuid(PROJECT_NAME, uuid,
				new RolePermissionParameter().setRoleUuid(role().getUuid()));
		latchFor(future);
		assertSuccess(future);
		assertNotNull(future.result().getRolePerms());
		assertEquals(4, future.result().getRolePerms().length);

	}

	@Test
	@Override
	public void testReadByUUIDWithMissingPermission() throws Exception {
		Role role = role();
		TagFamily tagFamily = project().getTagFamilyRoot().findAll().get(0);
		String uuid = tagFamily.getUuid();
		assertNotNull(tagFamily);
		role.revokePermissions(tagFamily, READ_PERM);

		Future<TagFamilyResponse> future = getClient().findTagFamilyByUuid(PROJECT_NAME, uuid);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);
	}

	@Test
	public void testReadMultiple2() {
		TagFamily tagFamily = tagFamily("colors");
		String uuid = tagFamily.getUuid();
		Future<TagListResponse> future = getClient().findTagsForTagFamilies(PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);
	}

	@Test
	@Override
	public void testReadMultiple() throws UnknownHostException, InterruptedException {
		// Don't grant permissions to the no perm tag. We want to make sure that this one will not be listed.
		TagFamily noPermTagFamily = project().getTagFamilyRoot().create("noPermTagFamily", user());
		String noPermTagUUID = noPermTagFamily.getUuid();
		// TODO check whether the project reference should be moved from generic class into node mesh class and thus not be available for tags
		// noPermTag.addProject(project());
		assertNotNull(noPermTagFamily.getUuid());

		// Test default paging parameters
		Future<TagFamilyListResponse> future = getClient().findTagFamilies(PROJECT_NAME);
		latchFor(future);
		assertSuccess(future);

		TagFamilyListResponse restResponse = future.result();
		assertEquals(25, restResponse.getMetainfo().getPerPage());
		assertEquals(1, restResponse.getMetainfo().getCurrentPage());
		assertEquals("The response did not contain the correct amount of items", tagFamilies().size(), restResponse.getData().size());

		int perPage = 4;
		// Extra Tags + permitted tag
		int totalTagFamilies = tagFamilies().size();
		int totalPages = (int) Math.ceil(totalTagFamilies / (double) perPage);
		List<TagFamilyResponse> allTagFamilies = new ArrayList<>();
		for (int page = 1; page <= totalPages; page++) {
			Future<TagFamilyListResponse> tagPageFut = getClient().findTagFamilies(PROJECT_NAME, new PagingParameter(page, perPage));
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

		Future<TagFamilyListResponse> pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingParameter(-1, perPage));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingParameter(0, perPage));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingParameter(1, -1));
		latchFor(pageFuture);
		expectException(pageFuture, BAD_REQUEST, "error_invalid_paging_parameters");

		perPage = 25;
		totalPages = (int) Math.ceil(totalTagFamilies / (double) perPage);
		pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingParameter(4242, perPage));
		latchFor(pageFuture);
		TagFamilyListResponse tagList = pageFuture.result();
		assertEquals(0, tagList.getData().size());
		assertEquals(4242, tagList.getMetainfo().getCurrentPage());
		assertEquals(25, tagList.getMetainfo().getPerPage());
		assertEquals(totalTagFamilies, tagList.getMetainfo().getTotalCount());
		assertEquals(totalPages, tagList.getMetainfo().getPageCount());

	}

	@Test
	public void testReadMetaCountOnly() {
		Future<TagFamilyListResponse> pageFuture = getClient().findTagFamilies(PROJECT_NAME, new PagingParameter(1, 0));
		latchFor(pageFuture);
		assertSuccess(pageFuture);
		assertEquals(0, pageFuture.result().getData().size());
	}

	@Test
	public void testCreateWithConflictingName() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("colors");
		Future<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request);
		latchFor(future);
		expectException(future, CONFLICT, "tagfamily_conflicting_name", "colors");
	}

	@Test
	@Override
	public void testCreate() {
		String name = "newTagFamily";
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName(name);
		Future<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request);
		latchFor(future);
		assertSuccess(future);
		assertEquals(name, future.result().getName());
	}

	@Test
	@Override
	public void testCreateReadDelete() throws Exception {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("newTagFamily");
		Future<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request);
		latchFor(future);
		assertSuccess(future);

		Future<TagFamilyResponse> readFuture = getClient().findTagFamilyByUuid(PROJECT_NAME, future.result().getUuid());
		latchFor(readFuture);
		assertSuccess(readFuture);

		Future<GenericMessageResponse> deleteFuture = getClient().deleteTagFamily(PROJECT_NAME, future.result().getUuid());
		latchFor(deleteFuture);
		assertSuccess(deleteFuture);

	}

	@Test
	public void testCreateWithoutPerm() {
		role().revokePermissions(project().getTagFamilyRoot(), CREATE_PERM);
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("SuperDoll");
		Future<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request);
		latchFor(future);

		expectException(future, FORBIDDEN, "error_missing_perm", project().getTagFamilyRoot().getUuid());
	}

	@Test
	public void testCreateWithNoName() {
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		// Don't set the name
		Future<TagFamilyResponse> future = getClient().createTagFamily(PROJECT_NAME, request);
		latchFor(future);
		expectException(future, BAD_REQUEST, "tagfamily_name_not_set");
	}

	@Test
	@Override
	public void testDeleteByUUID() throws Exception {
		String uuid;
		TagFamily basicTagFamily = tagFamily("basic");
		uuid = basicTagFamily.getUuid();
		CountDownLatch latch = new CountDownLatch(1);
		project().getTagFamilyRoot().findByUuid(uuid, rh -> {
			assertNotNull(rh.result());
			latch.countDown();
		});
		failingLatch(latch);

		Future<GenericMessageResponse> future = getClient().deleteTagFamily(PROJECT_NAME, uuid);
		latchFor(future);
		assertSuccess(future);
		assertElement(project().getTagFamilyRoot(), uuid, false);

	}

	@Test
	@Override
	public void testDeleteByUUIDWithNoPermission() throws Exception {
		TagFamily basicTagFamily = tagFamily("basic");
		Role role = role();
		role.revokePermissions(basicTagFamily, DELETE_PERM);

		assertElement(project().getTagFamilyRoot(), basicTagFamily.getUuid(), true);

		Future<GenericMessageResponse> future = getClient().deleteTagFamily(PROJECT_NAME, basicTagFamily.getUuid());
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", basicTagFamily.getUuid());

		assertElement(project().getTagFamilyRoot(), basicTagFamily.getUuid(), true);

	}

	@Test
	public void testUpdateWithConflictingName() {
		String newName = "colors";
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName(newName);

		Future<TagFamilyResponse> future = getClient().updateTagFamily(PROJECT_NAME, tagFamily("basic").getUuid(), request);
		latchFor(future);
		expectException(future, CONFLICT, "tagfamily_conflicting_name", newName);
	}

	@Test
	@Override
	public void testUpdate() throws UnknownHostException, InterruptedException {

		TagFamily tagFamily = tagFamily("basic");
		String uuid = tagFamily.getUuid();
		String name = tagFamily.getName();

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
		test.assertTagFamily(tagFamily("basic"), tagFamily2);

		// 4. read the tag again and verify that it was changed
		Future<TagFamilyResponse> reloadedTagFut = getClient().findTagFamilyByUuid(PROJECT_NAME, uuid);
		latchFor(reloadedTagFut);
		assertSuccess(reloadedTagFut);
		TagFamilyResponse reloadedTagFamily = reloadedTagFut.result();
		assertEquals(request.getName(), reloadedTagFamily.getName());
		test.assertTagFamily(tagFamily("basic"), reloadedTagFamily);

	}

	@Test
	@Override
	public void testUpdateWithBogusUuid() throws HttpStatusCodeErrorException, Exception {
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName("new Name");

		Future<TagFamilyResponse> future = getClient().updateTagFamily(PROJECT_NAME, "bogus", request);
		latchFor(future);
		expectException(future, NOT_FOUND, "object_not_found_for_uuid", "bogus");
	}

	@Test
	@Override
	public void testUpdateByUUIDWithoutPerm() {
		TagFamily tagFamily = tagFamily("basic");
		String uuid = tagFamily.getUuid();
		String name = tagFamily.getName();
		role().revokePermissions(tagFamily, UPDATE_PERM);

		// Update the tagfamily
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName("new Name");

		Future<TagFamilyResponse> future = getClient().updateTagFamily(PROJECT_NAME, uuid, request);
		latchFor(future);
		expectException(future, FORBIDDEN, "error_missing_perm", uuid);

		assertEquals(name, tagFamily.getName());
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testUpdateMultithreaded() throws Exception {
		int nJobs = 5;
		TagFamilyUpdateRequest request = new TagFamilyUpdateRequest();
		request.setName("New Name");

		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().updateTagFamily(PROJECT_NAME, tagFamily("colors").getUuid(), request));
		}
		validateSet(set, barrier);
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testReadByUuidMultithreaded() throws Exception {
		int nJobs = 10;
		String uuid = tagFamily("colors").getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findTagFamilyByUuid(PROJECT_NAME, uuid));
		}
		validateSet(set, barrier);
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testDeleteByUUIDMultithreaded() throws Exception {
		int nJobs = 3;
		String uuid = project().getUuid();
		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<GenericMessageResponse>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().deleteTagFamily(PROJECT_NAME, uuid));
		}
		validateDeletion(set, barrier);
	}

	@Test
	@Override
	@Ignore("Not yet supported")
	public void testCreateMultithreaded() throws Exception {
		int nJobs = 5;
		TagFamilyCreateRequest request = new TagFamilyCreateRequest();
		request.setName("test12345");

		CyclicBarrier barrier = prepareBarrier(nJobs);
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().createTagFamily(PROJECT_NAME, request));
		}
		validateCreation(set, barrier);
	}

	@Test
	@Override
	public void testReadByUuidMultithreadedNonBlocking() throws Exception {
		int nJobs = 200;
		Set<Future<?>> set = new HashSet<>();
		for (int i = 0; i < nJobs; i++) {
			set.add(getClient().findTagFamilyByUuid(PROJECT_NAME, tagFamily("colors").getUuid()));
		}
		for (Future<?> future : set) {
			latchFor(future);
			assertSuccess(future);
		}
	}

}
